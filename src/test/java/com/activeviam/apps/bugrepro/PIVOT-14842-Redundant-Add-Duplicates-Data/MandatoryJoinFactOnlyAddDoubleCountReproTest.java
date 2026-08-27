/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.bugrepro;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IDimension;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IMeasureHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.api.schema.ITableJoin;
import com.activeviam.database.api.schema.RelationshipOptionality;
import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.database.jdbc.api.GenericJdbcProperties;
import com.activeviam.database.jdbc.api.SqlDialect;
import com.activeviam.database.sql.api.schema.SqlTableId;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.discoverer.IDirectQueryTableDiscoverer;
import com.activeviam.directquery.api.schema.JoinDescription;
import com.activeviam.directquery.api.schema.SchemaDescription;
import com.activeviam.directquery.api.schema.TableDescription;
import com.activeviam.directquery.application.api.Application;
import com.activeviam.directquery.application.api.refresh.ChangeDescription;
import com.activeviam.directquery.application.api.refresh.ChangeType;
import com.activeviam.directquery.application.api.refresh.TableUpdateDetail;
import com.activeviam.directquery.application.api.refresh.condition.ConditionFactory;
import com.activeviam.directquery.jdbc.api.GenericJdbcClientSettings;
import com.activeviam.directquery.jdbc.api.GenericJdbcConnectorMetaFactory;

/**
 * Reproduction of bug-4, reported to ActiveViam as <a
 * href="https://activeviam.atlassian.net/browse/PIVOT-14842">PIVOT-14842</a> (see also {@code bug-4.md} in
 * this directory).
 *
 * <p><b>The real finding is broader than originally suspected.</b> Live rehearsal (27 Aug 2026, see {@code
 * project_scenario4_mandatory_factdim_rehearsal_2026_08_27} in this repo's HA-rehearsal memory) found that
 * under {@code RelationshipOptionality.MANDATORY} with the {@code SINGLE} (unpartitioned) aggregate-provider
 * mode - {@code DataCubeConfig.AggregateProviderMode.SINGLE}, this application's Scenario 4 configuration -
 * refreshing the fact table ({@code Trades}) alone via {@code ADD_ROWS} for an already-loaded date - i.e.
 * {@code DataMaintenanceController#loadFactOnly}, the {@code POST .../load/fact} endpoint - doubled that
 * date's point count. Writing this repro test found the actual root cause is more general: {@link
 * #redundantAddRowsRefreshOverAlreadyLoadedDataDuplicatesItUnderMandatoryJoinWithNoUnmatchedRowInvolved}
 * shows that <b>any</b> {@code ADD_ROWS} refresh over a date that is already fully and correctly loaded
 * duplicates it under {@code MANDATORY} - no unmatched row, no fact-only distinction required at all. The
 * duplication is cumulative, not a one-time miscalculation: a second redundant refresh takes the count from
 * 2x to 3x, confirming the provider's per-date rebuild result is being <em>appended onto</em> its existing
 * stored value at merge time instead of <em>overwriting</em> it. {@link
 * #redundantAddRowsRefreshOverAlreadyLoadedDataStaysCorrectUnderOptionalJoin} confirms this is genuinely
 * {@code MANDATORY}-specific: the identical redundant-refresh scenario under {@code OPTIONAL} stays
 * correct. The original fact-only-add scenario ({@link
 * #factOnlyAddOfAnUnmatchedRowUnderMandatoryJoinDoublesTheRealDatesCount}) is kept as its own test because
 * it is what production actually ran and it is the exact shape {@code DataMaintenanceController#loadFactOnly}
 * exercises, but it is a special case of the general bug, not a distinct one: fact-only-add against an
 * already-loaded date is itself a redundant refresh of the already-matched rows, and the new unmatched row
 * is correctly excluded by {@code MANDATORY} (no phantom-date fallback the way {@code OPTIONAL} has) rather
 * than contributing to the count itself.
 *
 * <p>This is the same {@code ADD_ROWS}-scoped-refresh code path as
 * {@code PIVOT-14825-Add-Rows-Corrupt-Date/OptionalJoinFactOnlyAddMisrouteReproTest}, but under {@code
 * MANDATORY} rather than {@code OPTIONAL} it is a non-idempotency bug, not a misrouting/NPE one - a
 * genuinely different mechanism, not just a different symptom of the same one.
 */
class MandatoryJoinFactOnlyAddDoubleCountReproTest {

    private static final String TRADES_TABLE = "Trades";
    private static final String TRADE_ATTRIBUTES_TABLE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeID";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyID";
    private static final String CUBE_NAME = "Cube";
    private static final String AGGREGATE_PROVIDER_NAME = "ByAsOfDate";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 2, 2);

    @Test
    void factOnlyAddOfAnUnmatchedRowUnderMandatoryJoinDoublesTheRealDatesCount() throws Exception {
        final String jdbcUrl = "jdbc:h2:mem:bug4_mandatory_fact_only_double;DB_CLOSE_DELAY=-1";
        try (Connection setupConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = setupConnection.createStatement()) {
            statement.execute("CREATE TABLE \"" + TRADES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \"" + TRADE_ID
                    + "\" VARCHAR(50), \"" + NOTIONAL + "\" DOUBLE, PRIMARY KEY (\"" + AS_OF_DATE + "\", \""
                    + TRADE_ID + "\"))");
            statement.execute("CREATE TABLE \"" + TRADE_ATTRIBUTES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \""
                    + TRADE_ID + "\" VARCHAR(50), \"" + COUNTERPARTY_ID + "\" VARCHAR(50), PRIMARY KEY (\""
                    + AS_OF_DATE + "\", \"" + TRADE_ID + "\"))");
            // Two matched trades - the baseline every real rehearsal load starts from, mirroring the
            // MANDATORY join's own contract that every Trades row has a TradeAttributes counterpart.
            for (int i = 1; i <= 2; i++) {
                statement.execute("INSERT INTO \"" + TRADES_TABLE + "\" VALUES ('2019-02-02', 'T" + i + "', "
                        + (100.0 * i) + ")");
                statement.execute("INSERT INTO \"" + TRADE_ATTRIBUTES_TABLE + "\" VALUES ('2019-02-02', 'T" + i
                        + "', 'Cpty" + i + "')");
            }
        }

        final GenericJdbcProperties properties = GenericJdbcProperties.builder()
                .connectionString(jdbcUrl)
                .additionalOption("user", "sa")
                .additionalOption("password", "")
                .build();
        final GenericJdbcClientSettings clientSettings =
                GenericJdbcClientSettings.builder().properties(properties).build();
        final DirectQueryConnector<GenericJdbcDatabaseSettings> connector =
                GenericJdbcConnectorMetaFactory.createConnectorFactory(
                                SqlDialect.builder().build())
                        .createConnector(clientSettings);

        final IDirectQueryTableDiscoverer discoverer = connector.getDiscoverer();
        final TableDescription tradesTable = discoverer.discoverTable(new SqlTableId("", "PUBLIC", TRADES_TABLE));
        final TableDescription tradeAttributesTable =
                discoverer.discoverTable(new SqlTableId("", "PUBLIC", TRADE_ATTRIBUTES_TABLE));

        // Same join shape as DremioSchemaConfig's current (27 Aug 2026) configuration: Trades ->
        // TradeAttributes, keyed on AsOfDate+TradeID, targetOptionality MANDATORY.
        final JoinDescription join = JoinDescription.builder()
                .name(String.format("%s_to_%s", TRADES_TABLE, TRADE_ATTRIBUTES_TABLE))
                .sourceTableName(TRADES_TABLE)
                .targetTableName(TRADE_ATTRIBUTES_TABLE)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(AS_OF_DATE, AS_OF_DATE),
                        new ITableJoin.FieldMapping(TRADE_ID, TRADE_ID)))
                .targetOptionality(RelationshipOptionality.MANDATORY)
                .build();

        final SchemaDescription schema = SchemaDescription.builder()
                .externalTables(List.of(tradesTable, tradeAttributesTable))
                .externalJoins(List.of(join))
                .build();

        final ISelectionDescription selection = StartBuilding.selection(schema)
                .fromBaseStore(TRADES_TABLE)
                .withAllReachableFields()
                .build();

        final LevelIdentifier asOfDateLevel = LevelIdentifier.simple(AS_OF_DATE);

        // Same SINGLE-mode shape as DataCubeConfig.AggregateProviderMode.SINGLE: one unpartitioned bitmap
        // provider covering every date - no filteredOn, no withValuePartitioningOn. This is Scenario 4's
        // real provider mode.
        final IActivePivotInstanceDescription cube = StartBuilding.cube(CUBE_NAME)
                .withContributorsCount()
                .withDimensions(b -> b.withDimension(AS_OF_DATE)
                        .withType(IDimension.DimensionType.TIME)
                        .withHierarchy(AS_OF_DATE)
                        .withLevelOfSameName())
                .withAggregateProvider()
                .jit()
                .withPartialProvider()
                .withName(AGGREGATE_PROVIDER_NAME)
                .bitmap()
                .includingOnlyLevels(asOfDateLevel)
                .build();

        final IActivePivotManagerDescription manager = StartBuilding.managerDescription("Manager")
                .withCatalog("Catalog")
                .containingAllCubes()
                .withSchema("Schema")
                .withSelection(selection)
                .withCube(cube)
                .build();

        try (Application application = Application.builder(connector)
                .schema(schema)
                .managerDescription(manager)
                .build()) {
            application.start();

            // application.start() itself does an implicit initial pull of whatever is already in the
            // source (standard DirectQuery startup discovery, not the bug under test) - the count is
            // already 2 here, before any explicit refresh() call at all.
            final CubeTester cubeTester = CubeTester.from(application.getManager());
            assertEquals(
                    2L,
                    pointCount(cubeTester, DATE_1),
                    "application.start() should have already pulled the 2 pre-existing matched rows");

            // A new trade lands in Trades only - its TradeAttributes counterpart hasn't arrived yet. Mirrors
            // an external feed inserting directly into the upstream source between refreshes, and exactly
            // what the live rehearsal's diagnostic row modeled.
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO \"" + TRADES_TABLE + "\" VALUES ('2019-02-02', 'T3', 999.0)");
            }

            // The actual repro: fact-only refresh, exactly like DataMaintenanceController#loadFactOnly -
            // TradeAttributes is never touched by this ChangeDescription. Unlike the dimension-only case
            // (which the engine rejects outright as "no impact on the base table"), this call is accepted
            // without throwing - the corruption only shows up in the query result afterward.
            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(TableUpdateDetail.create(
                    TRADES_TABLE, ChangeType.ADD_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            // bug-4 reproduction: the real date's own count doubles (2 -> 4) instead of correctly staying at
            // 2 (T3 is unmatched, so under MANDATORY it cannot itself contribute - there is no phantom-date
            // fallback the way there is under OPTIONAL) or moving to 3 (if the engine were lenient about the
            // still-missing attribute row). Matches the live rehearsal exactly: 1,000,000 -> 2,000,000, not
            // 1,000,001 and not 1,000,000.
            assertEquals(
                    4L,
                    pointCount(cubeTester, DATE_1),
                    "bug-4 reproduction: fact-only refresh under MANDATORY should double-count the real date "
                            + "(4) instead of correctly showing 2");

            // Append-vs-replace test: call the identical fact-only refresh a second time, with no new source
            // data at all (T3 is still the only unmatched row, already picked up above). If the provider's
            // per-date rebuild result is being appended onto its existing stored value instead of replacing
            // it, this second call should push the count to 6 (a third copy of the real 2 rows). If the bug
            // is instead a one-time miscalculation tied to a specific state transition (e.g. only the first
            // refresh after an unmatched row appears), the count should stay at 4.
            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(TableUpdateDetail.create(
                    TRADES_TABLE, ChangeType.ADD_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            final long countAfterSecondRefresh = pointCount(cubeTester, DATE_1);
            assertEquals(
                    6L,
                    countAfterSecondRefresh,
                    "append-vs-replace probe: a second fact-only refresh with no new source data pushed the "
                            + "count from 4 to "
                            + countAfterSecondRefresh
                            + " - 6 confirms the provider is appending each rebuild onto its existing value "
                            + "rather than replacing it; if this assertion fails with 4 instead, the count was "
                            + "idempotent on the second call and the append-not-replace theory is refuted, not "
                            + "confirmed - update bug-4.md accordingly rather than assuming 6.");
        }
    }

    /**
     * Minimal control, no unmatched row involved at all: does a plain, redundant {@code ADD_ROWS} refresh
     * over a date that is already fully (and correctly) loaded - both tables, every row matched - still
     * duplicate it under {@code MANDATORY}? If yes, this shows the bug is not specific to the fact-only-add
     * case at all: it is a general non-idempotency in {@code MANDATORY}'s incremental refresh whenever a
     * refresh call re-touches data that is already present, and fact-only-add (the case that surfaced it in
     * production) is just one way to trigger a redundant refresh over an already-loaded date.
     */
    @Test
    void redundantAddRowsRefreshOverAlreadyLoadedDataDuplicatesItUnderMandatoryJoinWithNoUnmatchedRowInvolved()
            throws Exception {
        final String jdbcUrl = "jdbc:h2:mem:bug4_mandatory_redundant_refresh;DB_CLOSE_DELAY=-1";
        try (Connection setupConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = setupConnection.createStatement()) {
            statement.execute("CREATE TABLE \"" + TRADES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \"" + TRADE_ID
                    + "\" VARCHAR(50), \"" + NOTIONAL + "\" DOUBLE, PRIMARY KEY (\"" + AS_OF_DATE + "\", \""
                    + TRADE_ID + "\"))");
            statement.execute("CREATE TABLE \"" + TRADE_ATTRIBUTES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \""
                    + TRADE_ID + "\" VARCHAR(50), \"" + COUNTERPARTY_ID + "\" VARCHAR(50), PRIMARY KEY (\""
                    + AS_OF_DATE + "\", \"" + TRADE_ID + "\"))");
            for (int i = 1; i <= 2; i++) {
                statement.execute("INSERT INTO \"" + TRADES_TABLE + "\" VALUES ('2019-02-02', 'T" + i + "', "
                        + (100.0 * i) + ")");
                statement.execute("INSERT INTO \"" + TRADE_ATTRIBUTES_TABLE + "\" VALUES ('2019-02-02', 'T" + i
                        + "', 'Cpty" + i + "')");
            }
        }

        final GenericJdbcProperties properties = GenericJdbcProperties.builder()
                .connectionString(jdbcUrl)
                .additionalOption("user", "sa")
                .additionalOption("password", "")
                .build();
        final GenericJdbcClientSettings clientSettings =
                GenericJdbcClientSettings.builder().properties(properties).build();
        final DirectQueryConnector<GenericJdbcDatabaseSettings> connector =
                GenericJdbcConnectorMetaFactory.createConnectorFactory(
                                SqlDialect.builder().build())
                        .createConnector(clientSettings);

        final IDirectQueryTableDiscoverer discoverer = connector.getDiscoverer();
        final TableDescription tradesTable = discoverer.discoverTable(new SqlTableId("", "PUBLIC", TRADES_TABLE));
        final TableDescription tradeAttributesTable =
                discoverer.discoverTable(new SqlTableId("", "PUBLIC", TRADE_ATTRIBUTES_TABLE));

        final JoinDescription join = JoinDescription.builder()
                .name(String.format("%s_to_%s", TRADES_TABLE, TRADE_ATTRIBUTES_TABLE))
                .sourceTableName(TRADES_TABLE)
                .targetTableName(TRADE_ATTRIBUTES_TABLE)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(AS_OF_DATE, AS_OF_DATE),
                        new ITableJoin.FieldMapping(TRADE_ID, TRADE_ID)))
                .targetOptionality(RelationshipOptionality.MANDATORY)
                .build();

        final SchemaDescription schema = SchemaDescription.builder()
                .externalTables(List.of(tradesTable, tradeAttributesTable))
                .externalJoins(List.of(join))
                .build();

        final ISelectionDescription selection = StartBuilding.selection(schema)
                .fromBaseStore(TRADES_TABLE)
                .withAllReachableFields()
                .build();

        final LevelIdentifier asOfDateLevel = LevelIdentifier.simple(AS_OF_DATE);

        final IActivePivotInstanceDescription cube = StartBuilding.cube(CUBE_NAME)
                .withContributorsCount()
                .withDimensions(b -> b.withDimension(AS_OF_DATE)
                        .withType(IDimension.DimensionType.TIME)
                        .withHierarchy(AS_OF_DATE)
                        .withLevelOfSameName())
                .withAggregateProvider()
                .jit()
                .withPartialProvider()
                .withName(AGGREGATE_PROVIDER_NAME)
                .bitmap()
                .includingOnlyLevels(asOfDateLevel)
                .build();

        final IActivePivotManagerDescription manager = StartBuilding.managerDescription("Manager")
                .withCatalog("Catalog")
                .containingAllCubes()
                .withSchema("Schema")
                .withSelection(selection)
                .withCube(cube)
                .build();

        try (Application application = Application.builder(connector)
                .schema(schema)
                .managerDescription(manager)
                .build()) {
            application.start();

            final CubeTester cubeTester = CubeTester.from(application.getManager());
            assertEquals(2L, pointCount(cubeTester, DATE_1), "start() should have pulled the 2 matched rows");

            // No source-data mutation at all here - both tables are untouched. A redundant, combined
            // ADD_ROWS refresh (mirroring DataMaintenanceController#load) over the same, already-correctly-
            // loaded date should be a safe no-op.
            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(
                    TableUpdateDetail.create(
                            TRADES_TABLE, ChangeType.ADD_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1)),
                    TableUpdateDetail.create(
                            TRADE_ATTRIBUTES_TABLE,
                            ChangeType.ADD_ROWS,
                            ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            assertEquals(
                    4L,
                    pointCount(cubeTester, DATE_1),
                    "bug-4, minimal form: a redundant ADD_ROWS refresh over already-loaded, fully-matched "
                            + "data (no unmatched row, no fact-only distinction) still doubles the count under "
                            + "MANDATORY - this is not specific to the fact-only-add case.");

            // Confirms the append pattern is cumulative, not a one-time transition: a third redundant
            // refresh should push it to 6.
            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(
                    TableUpdateDetail.create(
                            TRADES_TABLE, ChangeType.ADD_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1)),
                    TableUpdateDetail.create(
                            TRADE_ATTRIBUTES_TABLE,
                            ChangeType.ADD_ROWS,
                            ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            assertEquals(
                    6L,
                    pointCount(cubeTester, DATE_1),
                    "a third redundant refresh should push the count to 6, confirming linear, cumulative "
                            + "duplication rather than a one-time doubling");
        }
    }

    /**
     * Control: the exact same minimal redundant-refresh scenario as {@link
     * #redundantAddRowsRefreshOverAlreadyLoadedDataDuplicatesItUnderMandatoryJoinWithNoUnmatchedRowInvolved},
     * with only the join's {@code targetOptionality} changed to {@code OPTIONAL}. Confirms whether the
     * non-idempotency is specific to {@code MANDATORY}, or a general property of this refresh mechanism
     * regardless of join optionality.
     */
    @Test
    void redundantAddRowsRefreshOverAlreadyLoadedDataStaysCorrectUnderOptionalJoin() throws Exception {
        final String jdbcUrl = "jdbc:h2:mem:bug4_optional_redundant_refresh_control;DB_CLOSE_DELAY=-1";
        try (Connection setupConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = setupConnection.createStatement()) {
            statement.execute("CREATE TABLE \"" + TRADES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \"" + TRADE_ID
                    + "\" VARCHAR(50), \"" + NOTIONAL + "\" DOUBLE, PRIMARY KEY (\"" + AS_OF_DATE + "\", \""
                    + TRADE_ID + "\"))");
            statement.execute("CREATE TABLE \"" + TRADE_ATTRIBUTES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \""
                    + TRADE_ID + "\" VARCHAR(50), \"" + COUNTERPARTY_ID + "\" VARCHAR(50), PRIMARY KEY (\""
                    + AS_OF_DATE + "\", \"" + TRADE_ID + "\"))");
            for (int i = 1; i <= 2; i++) {
                statement.execute("INSERT INTO \"" + TRADES_TABLE + "\" VALUES ('2019-02-02', 'T" + i + "', "
                        + (100.0 * i) + ")");
                statement.execute("INSERT INTO \"" + TRADE_ATTRIBUTES_TABLE + "\" VALUES ('2019-02-02', 'T" + i
                        + "', 'Cpty" + i + "')");
            }
        }

        final GenericJdbcProperties properties = GenericJdbcProperties.builder()
                .connectionString(jdbcUrl)
                .additionalOption("user", "sa")
                .additionalOption("password", "")
                .build();
        final GenericJdbcClientSettings clientSettings =
                GenericJdbcClientSettings.builder().properties(properties).build();
        final DirectQueryConnector<GenericJdbcDatabaseSettings> connector =
                GenericJdbcConnectorMetaFactory.createConnectorFactory(
                                SqlDialect.builder().build())
                        .createConnector(clientSettings);

        final IDirectQueryTableDiscoverer discoverer = connector.getDiscoverer();
        final TableDescription tradesTable = discoverer.discoverTable(new SqlTableId("", "PUBLIC", TRADES_TABLE));
        final TableDescription tradeAttributesTable =
                discoverer.discoverTable(new SqlTableId("", "PUBLIC", TRADE_ATTRIBUTES_TABLE));

        // Only difference from the MANDATORY minimal-redundant-refresh test above: OPTIONAL here.
        final JoinDescription join = JoinDescription.builder()
                .name(String.format("%s_to_%s", TRADES_TABLE, TRADE_ATTRIBUTES_TABLE))
                .sourceTableName(TRADES_TABLE)
                .targetTableName(TRADE_ATTRIBUTES_TABLE)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(AS_OF_DATE, AS_OF_DATE),
                        new ITableJoin.FieldMapping(TRADE_ID, TRADE_ID)))
                .targetOptionality(RelationshipOptionality.OPTIONAL)
                .build();

        final SchemaDescription schema = SchemaDescription.builder()
                .externalTables(List.of(tradesTable, tradeAttributesTable))
                .externalJoins(List.of(join))
                .build();

        final ISelectionDescription selection = StartBuilding.selection(schema)
                .fromBaseStore(TRADES_TABLE)
                .withAllReachableFields()
                .build();

        final LevelIdentifier asOfDateLevel = LevelIdentifier.simple(AS_OF_DATE);

        final IActivePivotInstanceDescription cube = StartBuilding.cube(CUBE_NAME)
                .withContributorsCount()
                .withDimensions(b -> b.withDimension(AS_OF_DATE)
                        .withType(IDimension.DimensionType.TIME)
                        .withHierarchy(AS_OF_DATE)
                        .withLevelOfSameName())
                .withAggregateProvider()
                .jit()
                .withPartialProvider()
                .withName(AGGREGATE_PROVIDER_NAME)
                .bitmap()
                .includingOnlyLevels(asOfDateLevel)
                .build();

        final IActivePivotManagerDescription manager = StartBuilding.managerDescription("Manager")
                .withCatalog("Catalog")
                .containingAllCubes()
                .withSchema("Schema")
                .withSelection(selection)
                .withCube(cube)
                .build();

        try (Application application = Application.builder(connector)
                .schema(schema)
                .managerDescription(manager)
                .build()) {
            application.start();

            final CubeTester cubeTester = CubeTester.from(application.getManager());
            assertEquals(2L, pointCount(cubeTester, DATE_1), "start() should have pulled the 2 matched rows");

            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(
                    TableUpdateDetail.create(
                            TRADES_TABLE, ChangeType.ADD_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1)),
                    TableUpdateDetail.create(
                            TRADE_ATTRIBUTES_TABLE,
                            ChangeType.ADD_ROWS,
                            ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            final long countAfterRedundantRefresh = pointCount(cubeTester, DATE_1);
            assertEquals(
                    2L,
                    countAfterRedundantRefresh,
                    "control: under OPTIONAL, a redundant ADD_ROWS refresh over already-loaded data should "
                            + "stay correct at 2 - if this fails, the non-idempotency is not MANDATORY-specific "
                            + "and bug-4.md's scoping needs correcting");
        }
    }

    private static long pointCount(final CubeTester cubeTester, final LocalDate date) {
        final Object value = cubeTester
                .mdxQuery()
                .withMdx(String.format(
                        "SELECT {[Measures].[%s]} ON COLUMNS FROM [%s] WHERE ([%s].[%s].[%s])",
                        IMeasureHierarchy.COUNT_ID, CUBE_NAME, AS_OF_DATE, AS_OF_DATE, date))
                .run()
                .getTester()
                .hasOnlyOneCell()
                .getValue();
        return ((Number) value).longValue();
    }
}
