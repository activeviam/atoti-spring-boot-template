/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.bugrepro;

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
 * Verifies the actual production fix applied to {@code DataMaintenanceController} (switching every {@code
 * ADD_ROWS} call to {@code ADD_ROWS_WITH_DIRTY_CONDITION}, per ActiveViam's PIVOT-14842 triage) against all
 * three endpoint shapes ({@code load}, {@code loadFactOnly}, {@code loadDimensionOnly}), under {@code
 * MANDATORY} - the exact join setting the bug was found under.
 *
 * <p>Specifically checks a question the triage reply didn't address: {@code ADD_ROWS}'s "impossible update
 * details" validation (see {@code AIncrementalViewRefreshPlanner#validateImpactingDetails}) only fires
 * {@code if (ChangeTypeInternal.ADD_ROWS == changeType)} - {@code ADD_ROWS_WITH_DIRTY_CONDITION} maps to
 * {@code MIXED_CHANGES} internally, which routes through {@code handleReplace} instead of {@code handleAdd},
 * bypassing that check entirely. This could mean {@code loadDimensionOnly} - previously rejected outright
 * under {@code MANDATORY} as architecturally impossible (see {@code
 * project_scenario4_mandatory_factdim_rehearsal_2026_08_27}'s Finding 2) - now behaves differently after
 * the fix. Tested directly rather than assumed either way.
 */
class DirtyConditionFixVerificationTest {

    private static final String TRADES_TABLE = "Trades";
    private static final String TRADE_ATTRIBUTES_TABLE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeID";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyID";
    private static final String CUBE_NAME = "Cube";
    private static final String AGGREGATE_PROVIDER_NAME = "ByAsOfDate";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 2, 2);

    private record Harness(Application application, CubeTester cubeTester, String jdbcUrl) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            application.close();
        }
    }

    private Harness setUp(final String dbName) throws Exception {
        final String jdbcUrl = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
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

        final Application application = Application.builder(connector)
                .schema(schema)
                .managerDescription(manager)
                .build();
        application.start();

        return new Harness(application, CubeTester.from(application.getManager()), jdbcUrl);
    }

    /** Mirrors the fixed {@code DataMaintenanceController#loadFactOnly}. */
    @Test
    void fixedLoadFactOnlyIsIdempotentOverAnAlreadyLoadedDateUnderMandatory() throws Exception {
        try (Harness h = setUp("bug4_fix_verify_fact_only")) {
            assertEquals(2L, pointCount(h.cubeTester(), DATE_1), "baseline");

            try (Connection c = DriverManager.getConnection(h.jdbcUrl(), "sa", "");
                    Statement s = c.createStatement()) {
                s.execute("INSERT INTO \"" + TRADES_TABLE + "\" VALUES ('2019-02-02', 'T3', 999.0)");
            }

            // Fixed shape: ADD_ROWS_WITH_DIRTY_CONDITION, same whole-date condition as before.
            h.application()
                    .refresh(ChangeDescription.create(List.of(TableUpdateDetail.create(
                            TRADES_TABLE,
                            ChangeType.ADD_ROWS_WITH_DIRTY_CONDITION,
                            ConditionFactory.equal(AS_OF_DATE, DATE_1)))));

            assertEquals(
                    2L,
                    pointCount(h.cubeTester(), DATE_1),
                    "fixed loadFactOnly: T3 is unmatched under MANDATORY (no phantom-date fallback), so the "
                            + "count should stay at the correct 2 - not double to 4 the way plain ADD_ROWS did");

            // Second redundant call, no further data change - must stay idempotent, not compound.
            h.application()
                    .refresh(ChangeDescription.create(List.of(TableUpdateDetail.create(
                            TRADES_TABLE,
                            ChangeType.ADD_ROWS_WITH_DIRTY_CONDITION,
                            ConditionFactory.equal(AS_OF_DATE, DATE_1)))));
            assertEquals(
                    2L,
                    pointCount(h.cubeTester(), DATE_1),
                    "a second redundant fact-only refresh must stay idempotent - this is the actual fix");
        }
    }

    /** Mirrors the fixed {@code DataMaintenanceController#load} (both tables). */
    @Test
    void fixedWholeDayLoadIsIdempotentOverAnAlreadyLoadedDateUnderMandatory() throws Exception {
        try (Harness h = setUp("bug4_fix_verify_whole_day")) {
            assertEquals(2L, pointCount(h.cubeTester(), DATE_1), "baseline");

            h.application()
                    .refresh(ChangeDescription.create(List.of(
                            TableUpdateDetail.create(
                                    TRADES_TABLE,
                                    ChangeType.ADD_ROWS_WITH_DIRTY_CONDITION,
                                    ConditionFactory.equal(AS_OF_DATE, DATE_1)),
                            TableUpdateDetail.create(
                                    TRADE_ATTRIBUTES_TABLE,
                                    ChangeType.ADD_ROWS_WITH_DIRTY_CONDITION,
                                    ConditionFactory.equal(AS_OF_DATE, DATE_1)))));

            assertEquals(
                    2L,
                    pointCount(h.cubeTester(), DATE_1),
                    "fixed whole-day load: redundant call over already-loaded data must stay idempotent");
        }
    }

    /**
     * Mirrors the fixed {@code DataMaintenanceController#loadDimensionOnly}. This is the case flagged in the
     * class javadoc as needing direct verification: does switching away from {@code ADD_ROWS} change whether
     * this call is rejected under {@code MANDATORY}?
     */
    @Test
    void fixedLoadDimensionOnlyBehaviorUnderMandatory() throws Exception {
        try (Harness h = setUp("bug4_fix_verify_dimension_only")) {
            assertEquals(2L, pointCount(h.cubeTester(), DATE_1), "baseline");

            try (Connection c = DriverManager.getConnection(h.jdbcUrl(), "sa", "");
                    Statement s = c.createStatement()) {
                s.execute("INSERT INTO \"" + TRADE_ATTRIBUTES_TABLE + "\" VALUES ('2019-02-02', 'T3', 'Cpty3')");
            }

            final var dimensionOnlyRefresh = ChangeDescription.create(List.of(TableUpdateDetail.create(
                    TRADE_ATTRIBUTES_TABLE,
                    ChangeType.ADD_ROWS_WITH_DIRTY_CONDITION,
                    ConditionFactory.equal(AS_OF_DATE, DATE_1))));

            // Real result, not assumed: does this still throw the way plain ADD_ROWS did, or does the
            // dirty-condition/MIXED_CHANGES path bypass that validation and succeed instead? Observed, not
            // pre-asserted either way - see stdout for the actual outcome.
            boolean threw = false;
            try {
                h.application().refresh(dimensionOnlyRefresh);
            } catch (final Exception e) {
                threw = true;
                System.out.println(
                        "DIAGNOSTIC: loadDimensionOnly with ADD_ROWS_WITH_DIRTY_CONDITION threw: " + e.getMessage());
            }
            if (!threw) {
                System.out.println("DIAGNOSTIC: loadDimensionOnly with ADD_ROWS_WITH_DIRTY_CONDITION did NOT "
                        + "throw - succeeded silently. Count after: " + pointCount(h.cubeTester(), DATE_1));
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
}
