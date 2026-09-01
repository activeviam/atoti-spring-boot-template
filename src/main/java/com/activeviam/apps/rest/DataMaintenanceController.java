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

    /**
     * Drops {@code date} from this node's own view, {@code Trades} only - leaves {@code TradeAttributes}
     * untouched, so fact-only removal cost/correctness can be measured in isolation from a dimension-only
     * removal (see {@link #deleteDimensionOnly}).
     *
     * <p>Under an {@code OPTIONAL} join, this takes a materially different internal path than {@link
     * #delete}: {@code Trades} is the view's base table, so a {@code Trades}-only impact never crosses any
     * join, never trips {@code AIncrementalViewRefreshPlanner#hasProblematicOptionalJoin}, and is expected to
     * resolve to a real, local, scoped {@code RemoveWhere(AsOfDate = date)} - no external re-query, unlike
     * {@link #delete}'s full-provider wipe-and-rebuild. Confirmed from
     * {@code sql-database-6.2.0-sources.jar}'s {@code IncrementalAggViewRefreshPlanner#handleRemove}, not
     * assumed - see this repo's memory notes on the 31 Aug 2026 Scenario 5 rehearsal. <b>Known, unverified
     * trade-off</b>: {@code TradeAttributes} rows for {@code date} are never removed this way - they remain
     * in memory permanently. They should be inert for aggregation (a {@code LEFT OUTER JOIN} driven from
     * {@code Trades} never surfaces an orphaned {@code TradeAttributes} row once its matching {@code Trades}
     * row is gone), but whether any hierarchy independently enumerates {@code TradeAttributes} members is not
     * yet checked.
     */
    @DeleteMapping("/{date}/delete/fact")
    public void deleteFactOnly(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(
                changeDescription(ChangeType.REMOVE_ROWS, date, dremioProperties.getTradesTableName()));
    }

    /**
     * Drops {@code date} from this node's own view, {@code TradeAttributes} only - leaves {@code Trades}
     * untouched, so dimension-only removal cost/correctness can be measured in isolation from a fact-only
     * removal (see {@link #deleteFactOnly}).
     *
     * <p>Unlike {@link #deleteFactOnly}, this impact only reaches the view by crossing the {@code
     * Trades -> TradeAttributes} join, which is {@code OPTIONAL} - expected to still trip {@code
     * hasProblematicOptionalJoin} and take the same full-provider wipe-and-rebuild path as {@link #delete},
     * just triggered by a single-table {@code ChangeDescription} this time.
     */
    @DeleteMapping("/{date}/delete/dimension")
    public void deleteDimensionOnly(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(
                changeDescription(ChangeType.REMOVE_ROWS, date, dremioProperties.getTradeAttributesTableName()));
    }

    /**
     * Picks up {@code date} on this node, assuming it already exists in Dremio. Issues no DML of its own.
     *
     * <p>Uses {@link ChangeType#ADD_ROWS_WITH_DIRTY_CONDITION}, not {@link ChangeType#ADD_ROWS} - see the
     * note on {@link #loadFactOnly} for why: this endpoint's condition ({@code AsOfDate = date}) can re-cover
     * rows already reflected in the provider if called more than once for the same date.
     */
    @PostMapping("/{date}/load")
    public void load(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(changeDescription(
                ChangeType.ADD_ROWS_WITH_DIRTY_CONDITION,
                date,
                dremioProperties.getTradesTableName(),
                dremioProperties.getTradeAttributesTableName()));
    }

    /**
     * Picks up a new row landed in the fact table ({@code Trades}) only, for a {@code date} already loaded on
     * this node - e.g. an intraday new trade. Leaves {@code TradeAttributes} untouched, so fact-only refresh
     * cost can be measured in isolation from a dimension-only refresh (see {@link #loadDimensionOnly}).
     *
     * <p>Uses {@link ChangeType#ADD_ROWS_WITH_DIRTY_CONDITION}, not {@link ChangeType#ADD_ROWS} - see <a
     * href="https://activeviam.atlassian.net/browse/PIVOT-14842">PIVOT-14842</a>. {@code ADD_ROWS} requires
     * the given condition to capture "the added rows, and only the added rows" - a condition scoped to the
     * whole {@code date} is only that precise the very first time a date is loaded; called again for a date
     * that already has data (exactly what an intraday update is), the condition is "dirty" - it re-covers
     * rows the provider already holds, and {@code ADD_ROWS}'s planner has no remove-before-add step to
     * compensate, so those rows get double-counted, cumulatively, on every redundant call.
     * {@code ADD_ROWS_WITH_DIRTY_CONDITION} tells DirectQuery the scope may overlap already-loaded data, which
     * routes it through a remove-then-add plan instead - genuinely idempotent, confirmed both by ActiveViam's
     * own regression tests and this repo's {@code bugrepro/PIVOT-14842-Redundant-Add-Duplicates-Data/}.
     */
    @PostMapping("/{date}/load/fact")
    public void loadFactOnly(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(changeDescription(
                ChangeType.ADD_ROWS_WITH_DIRTY_CONDITION, date, dremioProperties.getTradesTableName()));
    }

    /**
     * Picks up a new row landed in the dimension table ({@code TradeAttributes}) only, for a {@code date}
     * already loaded on this node - e.g. an intraday new counterparty/attribute record. Leaves {@code Trades}
     * untouched, so dimension-only refresh cost can be measured in isolation from a fact-only refresh (see
     * {@link #loadFactOnly}).
     *
     * <p>Uses {@link ChangeType#ADD_ROWS_WITH_DIRTY_CONDITION}, not {@link ChangeType#ADD_ROWS} - see the
     * note on {@link #loadFactOnly} for why. <b>This changes this endpoint's own behavior under {@code
     * MANDATORY}, not just its idempotency</b>: with plain {@code ADD_ROWS}, a dimension-only call under
     * {@code MANDATORY} was rejected outright (a real {@code ActiveViamRuntimeException}, "Impossible update
     * details... there must be an impact on the base table") - that engine-side validation only fires for
     * {@code ADD_ROWS} specifically. {@code ADD_ROWS_WITH_DIRTY_CONDITION} routes through a different
     * internal plan and bypasses that check entirely; verified (not assumed) via {@code
     * DirtyConditionFixVerificationTest#fixedLoadDimensionOnlyBehaviorUnderMandatory} that the call now
     * succeeds silently instead, correctly leaving the aggregate unchanged (an unmatched dimension-only row
     * has no impact on a {@code Trades}-based selection either way) - a behavior improvement, not a
     * regression, but a real change from what was previously documented and reported for this scenario.
     */
    @PostMapping("/{date}/load/dimension")
    public void loadDimensionOnly(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        directQueryApplication.refresh(changeDescription(
                ChangeType.ADD_ROWS_WITH_DIRTY_CONDITION, date, dremioProperties.getTradeAttributesTableName()));
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
