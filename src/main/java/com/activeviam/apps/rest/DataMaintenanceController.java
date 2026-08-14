/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.apps.cfg.directquery.DremioRestClient;
import com.activeviam.directquery.application.api.Application;
import com.activeviam.directquery.application.api.refresh.ChangeDescription;
import com.activeviam.directquery.application.api.refresh.ChangeType;
import com.activeviam.directquery.application.api.refresh.TableUpdateDetail;
import com.activeviam.directquery.application.api.refresh.condition.ConditionFactory;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;

/**
 * Deletes/restores an {@code AsOfDate} directly on Dremio, for the masking/failover rehearsal exercise
 * described at https://docs.activeviam.com/engine/java-sdk/6.1/distributed/remove_data_overlap: once a
 * date has been masked on this data node (see {@link MaskingController}), it is safe to remove that date
 * from the shared backend, and the aggregate provider keeps this node answerable in the meantime.
 *
 * <p>Operates on the Iceberg tables backing the {@code Trades}/{@code TradeAttributes} views (see the
 * Dremio-side conversion in {@code DremioSchemaConfig}'s javadoc/README), never on the views themselves,
 * since Dremio does not support DML on views. Deleted rows are copied into a same-shaped {@code
 * *_backup} table first, so {@link #restore} can put them back and make the exercise repeatable.
 *
 * <p>Since DirectQuery has no way to detect changes made directly against the external database, every
 * SQL-level change here is followed by an explicit {@link Application#refresh(ChangeDescription)} call
 * scoped to the affected {@code AsOfDate}, so this node's own hierarchies/aggregate providers stay
 * consistent with what is actually in Dremio - otherwise a later {@code unmask} would let the query node
 * ask this node for a date whose local snapshot was never updated. Note the {@code Trades}-to-{@code
 * TradeAttributes} join in {@code DremioSchemaConfig} does not declare a {@code targetOptionality}, which
 * defaults to {@code OPTIONAL}; per the DirectQuery docs, an {@code Optional} relationship can force this
 * "incremental" refresh to fall back internally to a full rebuild of every hierarchy/aggregate provider
 * even though the scope given here is exact - if that matters, mark the join {@code MANDATORY} instead
 * (only valid if every {@code Trades} row is guaranteed to have a matching {@code TradeAttributes} row).
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

    private static final String NAS_SOURCE = "local_uploads";
    private static final List<String> STORE_NAMES = List.of(TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME);

    /**
     * Columns other than {@code AsOfDate} to carry over when cloning a date's rows into a brand-new one
     * (see {@link #load}) - the key ({@code AsOfDate}+{@code TradeID}) still comes out unique per row since
     * the new {@code AsOfDate} makes it a distinct key even with the same {@code TradeID} values.
     */
    private static final Map<String, String> COLUMNS_AFTER_ASOFDATE = Map.of(
            TRADES_STORE_NAME,
            "%s, %s".formatted(TRADE_ID, NOTIONAL),
            TRADE_ATTRIBUTES_STORE_NAME,
            "%s, %s, %s".formatted(TRADE_ID, TRADE_DATE, COUNTERPARTY_ID));

    private final DremioRestClient dremioRestClient;
    private final Application directQueryApplication;

    @DeleteMapping("/{date}")
    public void delete(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        STORE_NAMES.forEach(storeName -> backupAndDelete(storeName, date));
        directQueryApplication.refresh(changeDescription(ChangeType.REMOVE_ROWS, date));
    }

    @PostMapping("/{date}/restore")
    public void restore(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        STORE_NAMES.forEach(storeName -> restoreFromBackup(storeName, date));
        directQueryApplication.refresh(changeDescription(ChangeType.ADD_ROWS, date));
    }

    /**
     * Loads a brand-new {@code AsOfDate} that was never previously present (unlike {@link #restore}, which
     * only puts back what {@link #delete} backed up) by cloning an existing date's rows under the new date.
     * Stands in for a real upstream feed, which this rehearsal setup has none of.
     */
    @PostMapping("/{date}/load")
    public void load(
            @PathVariable final LocalDate date,
            @RequestParam final LocalDate sourceDate,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        tagTestRunId(testRunId);
        STORE_NAMES.forEach(storeName -> cloneFromSourceDate(storeName, date, sourceDate));
        directQueryApplication.refresh(changeDescription(ChangeType.ADD_ROWS, date));
    }

    private static void tagTestRunId(final String testRunId) {
        if (testRunId != null) {
            Span.current().setAttribute(EndpointConstants.TEST_RUN_ID_ATTRIBUTE, testRunId);
        }
    }

    private static ChangeDescription changeDescription(final ChangeType changeType, final LocalDate date) {
        return ChangeDescription.create(STORE_NAMES.stream()
                .map(storeName ->
                        TableUpdateDetail.create(storeName, changeType, ConditionFactory.equal(ASOFDATE, date)))
                .toList());
    }

    private void backupAndDelete(final String storeName, final LocalDate date) {
        final String icebergTable = icebergTable(storeName);
        final String backupTable = backupTable(storeName);
        dremioRestClient.execute("CREATE TABLE IF NOT EXISTS %s.\"%s\" AS SELECT * FROM %s.\"%s\" WHERE 1 = 0"
                .formatted(NAS_SOURCE, backupTable, NAS_SOURCE, icebergTable));
        dremioRestClient.execute("INSERT INTO %s.\"%s\" SELECT * FROM %s.\"%s\" WHERE \"%s\" = DATE '%s'"
                .formatted(NAS_SOURCE, backupTable, NAS_SOURCE, icebergTable, ASOFDATE, date));
        dremioRestClient.execute(
                "DELETE FROM %s.\"%s\" WHERE \"%s\" = DATE '%s'".formatted(NAS_SOURCE, icebergTable, ASOFDATE, date));
    }

    private void restoreFromBackup(final String storeName, final LocalDate date) {
        final String icebergTable = icebergTable(storeName);
        final String backupTable = backupTable(storeName);
        dremioRestClient.execute("INSERT INTO %s.\"%s\" SELECT * FROM %s.\"%s\" WHERE \"%s\" = DATE '%s'"
                .formatted(NAS_SOURCE, icebergTable, NAS_SOURCE, backupTable, ASOFDATE, date));
        dremioRestClient.execute(
                "DELETE FROM %s.\"%s\" WHERE \"%s\" = DATE '%s'".formatted(NAS_SOURCE, backupTable, ASOFDATE, date));
    }

    private void cloneFromSourceDate(final String storeName, final LocalDate date, final LocalDate sourceDate) {
        final String icebergTable = icebergTable(storeName);
        dremioRestClient.execute("INSERT INTO %s.\"%s\" SELECT DATE '%s', %s FROM %s.\"%s\" WHERE \"%s\" = DATE '%s'"
                .formatted(
                        NAS_SOURCE,
                        icebergTable,
                        date,
                        COLUMNS_AFTER_ASOFDATE.get(storeName),
                        NAS_SOURCE,
                        icebergTable,
                        ASOFDATE,
                        sourceDate));
    }

    private static String icebergTable(final String storeName) {
        return storeName + "_iceberg";
    }

    private static String backupTable(final String storeName) {
        return storeName + "_backup";
    }
}
