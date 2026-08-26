/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.bugrepro;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
 * Reproduction of the bug reported to ActiveViam as <a
 * href="https://activeviam.atlassian.net/browse/PIVOT-14825">PIVOT-14825</a> (see also {@code bug-2.md} in
 * this directory): under {@code RelationshipOptionality.OPTIONAL} (this application's real {@code Trades}-to-
 * {@code TradeAttributes} join configuration), refreshing the fact table ({@code Trades}) alone via {@code
 * ADD_ROWS} - i.e. {@code DataMaintenanceController#loadFactOnly}, the {@code POST .../load/fact} endpoint -
 * without also refreshing {@code TradeAttributes} in the same {@code ChangeDescription} corrupts the {@code
 * AsOfDate} hierarchy for an unmatched row. Two prior production runs (see {@code
 * project_scenario5_rehearsal_2026_08_23} in this repo's HA-rehearsal history) each saw one isolated
 * symptom: a phantom {@code 1970-01-01} date at small scale, or a point query returning double the correct
 * count at 10M-row/3-node scale. The pre-existing, unmodified {@code DataMaintenanceController#load}
 * endpoint (both tables refreshed together) and {@code #loadDimensionOnly} (dimension-only) are both
 * confirmed clean - this bug is specific to refreshing {@code Trades} alone.
 *
 * <p>This test drives the real {@code Application#refresh(ChangeDescription)} API against an in-memory H2
 * database, with the same {@code PARTIAL_BY_DATE}-shaped cube and {@code OPTIONAL} join as {@link
 * OptionalJoinRemoveRowsNonRemovalReproTest} (bug-1's sibling in this same package). It loads a matched
 * baseline into both tables for one date via {@code ADD_ROWS} (mirroring {@code
 * DataMaintenanceController#load}), then lands one more row in {@code Trades} only for that same date and
 * refreshes only {@code Trades} via {@code ADD_ROWS} (mirroring {@code #loadFactOnly} exactly).
 *
 * <p><b>Real, run result (26 Aug 2026, this exact file, {@code mvn -o test
 * -Dtest=OptionalJoinFactOnlyAddMisrouteReproTest}): reproduces the bug, and combines both previously
 * separate production symptoms in one run, plus a third, newly-observed one:</b>
 *
 * <ol>
 *   <li>A direct point query for the real date ({@code 2019-01-06}) returns <b>4</b> - not the pre-refresh
 *       {@code 2}, and not the correct post-refresh {@code 3} either, but exactly double the correct value
 *       - the same over-counting shape as the 10M-row production finding.
 *   <li>A direct point query for {@code 1970-01-01} - never inserted anywhere in this test's own data -
 *       returns <b>1</b>: the misrouted new row landed there instead, exactly the phantom-date symptom from
 *       the small-scale production finding.
 *   <li><b>New finding, not previously observed in production</b>: a full (non-{@code NON EMPTY}) MDX
 *       listing of every {@code [AsOfDate].[AsOfDate]} member - a query that succeeds both before this
 *       refresh and after every other operation in this test - throws a real {@link NullPointerException}
 *       (via {@code MdxRuntimeException}), rooted in {@code MultiVersionAxisMember.getChild} ->
 *       {@code ConcurrentSkipListMap.doGet} returning {@code null}. This suggests the fact-only refresh
 *       leaves the {@code AsOfDate} axis itself in a corrupted state, not just the aggregate values -
 *       stronger evidence than either prior production run captured on its own, since both of those only
 *       ever ran point/{@code NON EMPTY} queries against the broken state, never a full member listing.
 * </ol>
 *
 * <p>See {@link PlainDatastoreFactOnlyAddReproTest} for the same shape driven through the plain in-memory
 * {@code IDatastore} API instead. Only the phantom-{@code 1970-01-01} misrouting (anomaly 2) reproduces
 * there - it is a core ActiveViam engine behavior, not DirectQuery-specific. The doubled count (anomaly 1)
 * and the axis-corrupting crash (anomaly 3) do NOT reproduce on the plain datastore - those two are
 * DirectQuery-specific.
 */
class OptionalJoinFactOnlyAddMisrouteReproTest {

    private static final String TRADES_TABLE = "Trades";
    private static final String TRADE_ATTRIBUTES_TABLE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeID";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyID";
    private static final String CUBE_NAME = "Cube";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 1, 6);
    private static final LocalDate PHANTOM_DATE = LocalDate.of(1970, 1, 1);

    @Test
    void factOnlyAddOfAnUnmatchedRowCorruptsTheAsOfDateHierarchyUnderAnOptionalJoin() throws Exception {
        final String jdbcUrl = "jdbc:h2:mem:bug2_optional_fact_only_misroute;DB_CLOSE_DELAY=-1";
        try (Connection setupConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = setupConnection.createStatement()) {
            statement.execute("CREATE TABLE \"" + TRADES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \"" + TRADE_ID
                    + "\" VARCHAR(50), \"" + NOTIONAL + "\" DOUBLE, PRIMARY KEY (\"" + AS_OF_DATE + "\", \""
                    + TRADE_ID + "\"))");
            statement.execute("CREATE TABLE \"" + TRADE_ATTRIBUTES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \""
                    + TRADE_ID + "\" VARCHAR(50), \"" + COUNTERPARTY_ID + "\" VARCHAR(50), PRIMARY KEY (\""
                    + AS_OF_DATE + "\", \"" + TRADE_ID + "\"))");
            // Two matched trades - the baseline every real rehearsal load starts from.
            for (int i = 1; i <= 2; i++) {
                statement.execute("INSERT INTO \"" + TRADES_TABLE + "\" VALUES ('2019-01-06', 'T" + i + "', "
                        + (100.0 * i) + ")");
                statement.execute("INSERT INTO \"" + TRADE_ATTRIBUTES_TABLE + "\" VALUES ('2019-01-06', 'T" + i
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

        // Same join shape as DremioSchemaConfig: Trades -> TradeAttributes, keyed on AsOfDate+TradeID,
        // targetOptionality OPTIONAL.
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

        // Same PARTIAL_BY_DATE shape as DataCubeConfig's PARTIAL_BY_DATE mode: one independently-named
        // bitmap provider, filteredOn this sole declared date - no umbrella provider.
        final IActivePivotInstanceDescription cube = StartBuilding.cube(CUBE_NAME)
                .withContributorsCount()
                .withDimensions(b -> b.withDimension(AS_OF_DATE)
                        .withType(IDimension.DimensionType.TIME)
                        .withHierarchy(AS_OF_DATE)
                        .withLevelOfSameName())
                .withAggregateProvider()
                .jit()
                .withPartialProvider()
                .withName("ByAsOfDate_" + DATE_1)
                .bitmap()
                .includingOnlyLevels(asOfDateLevel)
                .filteredOn(Map.of(asOfDateLevel, DATE_1))
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

            // Mirrors DataMaintenanceController#load: ADD_ROWS on both tables, scoped to the date.
            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(
                    TableUpdateDetail.create(
                            TRADES_TABLE, ChangeType.ADD_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1)),
                    TableUpdateDetail.create(
                            TRADE_ATTRIBUTES_TABLE,
                            ChangeType.ADD_ROWS,
                            ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            final CubeTester cubeTester = CubeTester.from(application.getManager());
            assertEquals(2L, pointCount(cubeTester, DATE_1), "baseline load should show both matched trades");
            // A clean, correctly-partitioned baseline can always be listed in full - this is the query that
            // breaks after the fact-only refresh below.
            assertDoesNotThrow(() -> fullAsOfDateMemberCount(cubeTester), "baseline member listing must not throw");

            // A new trade lands in Trades only - its TradeAttributes counterpart hasn't arrived yet. Mirrors
            // an external feed inserting directly into the upstream source between refreshes.
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO \"" + TRADES_TABLE + "\" VALUES ('2019-01-06', 'T3', 999.0)");
            }

            // The actual repro: fact-only refresh, exactly like DataMaintenanceController#loadFactOnly -
            // TradeAttributes is never touched by this ChangeDescription. This call itself does not throw -
            // the corruption only shows up in queries issued afterward.
            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(TableUpdateDetail.create(
                    TRADES_TABLE, ChangeType.ADD_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            // Anomaly 1: the real date's own point count doubles instead of going from 2 to the correct 3.
            assertEquals(
                    4L,
                    pointCount(cubeTester, DATE_1),
                    "bug-2 reproduction: fact-only refresh should double-count the real date (4) instead of "
                            + "correctly showing 3");

            // Anomaly 2: the misrouted row lands on the join's null-key fallback date instead.
            assertEquals(
                    1L,
                    pointCount(cubeTester, PHANTOM_DATE),
                    "bug-2 reproduction: the unmatched row should be misrouted to the phantom 1970-01-01 date");

            // Anomaly 3 (new finding, not previously observed in production): the AsOfDate axis itself is
            // left corrupted - a full member listing that worked fine on the clean baseline above now
            // throws instead of just returning a wrong count.
            final Exception thrown = assertThrows(Exception.class, () -> fullAsOfDateMemberCount(cubeTester));
            Throwable cause = thrown;
            NullPointerException npeCause = null;
            while (cause != null) {
                if (cause instanceof NullPointerException npe) {
                    npeCause = npe;
                    break;
                }
                cause = cause.getCause();
            }
            if (npeCause == null) {
                fail("Expected a NullPointerException somewhere in the cause chain of: " + thrown, thrown);
            }
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

    /** Full (non-{@code NON EMPTY}) listing of every declared {@code AsOfDate} member, including any with
     * zero contributions - the only way to see a phantom date that a {@code NON EMPTY} listing would hide. */
    private static long fullAsOfDateMemberCount(final CubeTester cubeTester) {
        return cubeTester
                .mdxQuery()
                .withMdx(String.format(
                        "SELECT {[Measures].[%s]} ON COLUMNS, {[%s].[%s].Members} ON ROWS FROM [%s]",
                        IMeasureHierarchy.COUNT_ID, AS_OF_DATE, AS_OF_DATE, CUBE_NAME))
                .run()
                .getTester()
                .getCellsCount();
    }
}
