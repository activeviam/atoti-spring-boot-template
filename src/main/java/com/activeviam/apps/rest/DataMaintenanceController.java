/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.apps.cfg.directquery.DremioProperties;
import com.activeviam.directquery.application.api.Application;
import com.activeviam.directquery.application.api.refresh.ChangeDescription;
import com.activeviam.directquery.application.api.refresh.ChangeType;
import com.activeviam.directquery.application.api.refresh.TableUpdateDetail;
import com.activeviam.directquery.application.api.refresh.condition.ConditionFactory;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;

/**
 * Tells this data node's own datastore/aggregate provider to pick up or drop an {@code AsOfDate}, for the
 * masking/failover rehearsal exercise described at
 * https://docs.activeviam.com/engine/java-sdk/6.1/distributed/remove_data_overlap.
 *
 * <p>Dremio is the system of record, populated independently by an upstream feed this app has no part in -
 * every {@code AsOfDate} this endpoint ever operates on is assumed to already exist there (or, for {@link
 * #delete}, to still exist there after this node stops serving it). Neither endpoint issues any DML against
 * Dremio: since DirectQuery has no way to detect changes made directly against the external database on its
 * own, both endpoints just call {@link Application#refresh(ChangeDescription)} scoped to the affected date,
 * so this node's own hierarchies/aggregate providers catch up to (or drop) what Dremio already holds. This
 * makes both endpoints safe to call independently on every replica - there is nothing to duplicate or
 * corrupt, since nothing is ever written anywhere.
 *
 * <p>Each call also accepts an optional {@value EndpointConstants#TEST_RUN_ID_HEADER} header, echoed onto
 * the request's span as {@value EndpointConstants#TEST_RUN_ID_ATTRIBUTE} - see {@link MaskingController}.
 */
@RestController
@Profile("data-node")
@RequestMapping(DataMaintenanceController.DATA_ENDPOINT)
@RequiredArgsConstructor
public class DataMaintenanceController {
    public static final String DATA_ENDPOINT = CUSTOM_REST_PATH + "/data";

    private final Application directQueryApplication;
    private final DremioProperties dremioProperties;

    /** Drops {@code date} from this node's own view. Dremio is untouched - the date may still exist there. */
    @DeleteMapping("/{date}")
    public void delete(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(changeDescription(
                ChangeType.REMOVE_ROWS,
                date,
                dremioProperties.getTradesTableName(),
                dremioProperties.getTradeAttributesTableName()));
    }

    /** Picks up {@code date} on this node, assuming it already exists in Dremio. Issues no DML of its own. */
    @PostMapping("/{date}/load")
    public void load(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(changeDescription(
                ChangeType.ADD_ROWS,
                date,
                dremioProperties.getTradesTableName(),
                dremioProperties.getTradeAttributesTableName()));
    }

    /**
     * Picks up a new row landed in the fact table ({@code Trades}) only, for a {@code date} already loaded on
     * this node - e.g. an intraday new trade. Leaves {@code TradeAttributes} untouched, so fact-only refresh
     * cost can be measured in isolation from a dimension-only refresh (see {@link #loadDimensionOnly}).
     */
    @PostMapping("/{date}/load/fact")
    public void loadFactOnly(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(
                changeDescription(ChangeType.ADD_ROWS, date, dremioProperties.getTradesTableName()));
    }

    /**
     * Picks up a new row landed in the dimension table ({@code TradeAttributes}) only, for a {@code date}
     * already loaded on this node - e.g. an intraday new counterparty/attribute record. Leaves {@code Trades}
     * untouched, so dimension-only refresh cost can be measured in isolation from a fact-only refresh (see
     * {@link #loadFactOnly}).
     */
    @PostMapping("/{date}/load/dimension")
    public void loadDimensionOnly(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(
                changeDescription(ChangeType.ADD_ROWS, date, dremioProperties.getTradeAttributesTableName()));
    }

    private static void tagTestRunId(final String testRunId) {
        if (testRunId != null) {
            Span.current().setAttribute(EndpointConstants.TEST_RUN_ID_ATTRIBUTE, testRunId);
        }
    }

    private ChangeDescription changeDescription(
            final ChangeType changeType, final LocalDate date, final String... tableNames) {
        return ChangeDescription.create(List.of(tableNames).stream()
                .map(storeName ->
                        TableUpdateDetail.create(storeName, changeType, ConditionFactory.equal(ASOFDATE, date)))
                .toList());
    }
}
