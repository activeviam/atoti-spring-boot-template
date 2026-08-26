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
 * href="https://activeviam.atlassian.net/browse/PIVOT-14823">PIVOT-14823</a> (see also {@code bug-1.md} in
 * this directory): under {@code RelationshipOptionality.OPTIONAL} (this application's real {@code Trades}-to-
 * {@code TradeAttributes} join configuration, see {@code DremioSchemaConfig}), a {@code REMOVE_ROWS} refresh
 * reports success (no exception, epoch genuinely advances) but never actually removes the targeted date's
 * rows - confirmed live at 10M-row/3-node scale, see {@code project_scenario5_rehearsal_2026_08_23} and
 * {@code project_delete_full_scale_redo_2026_08_26} in this repo's HA-rehearsal history.
 *
 * <p>Root cause (traced from {@code sql-database-6.2.0-sources.jar},
 * {@code com.activeviam.database.sql.internal.refresh.incremental.plan}): {@code
 * DataMaintenanceController#delete} issues one {@code ChangeDescription} with a {@code REMOVE_ROWS} entry
 * for each of {@code Trades} and {@code TradeAttributes}. The {@code TradeAttributes} update only reaches
 * the selection's view by crossing the {@code Trades -&gt; TradeAttributes} join, and {@code
 * AIncrementalViewRefreshPlanner#hasProblematicOptionalJoin} escalates the *entire* refresh to {@code
 * fullRefreshOperations()} whenever any impacting update crosses a join whose {@code targetOptionality} is
 * {@code OPTIONAL}. That full-refresh path wipes the aggregate provider, then blindly re-executes the
 * view's own original, unconditioned aggregate query against the external database - and since this app's
 * {@code delete} never issues DML against the external source (DirectQuery's source of record is untouched,
 * by design), the "full refresh" faithfully re-imports exactly the rows it was supposed to remove.
 *
 * <p>This test drives the real {@code Application#refresh(ChangeDescription)} API against an in-memory H2
 * database via {@code GenericJdbcConnectorMetaFactory} - the same generic-JDBC connector mechanism
 * production uses for Dremio - built with a {@code PARTIAL_BY_DATE}-shaped cube (one independently-named
 * bitmap provider per date, {@code filteredOn} that date, matching {@code DataCubeConfig}'s {@code
 * PARTIAL_BY_DATE} mode exactly) over two joined tables with the join's {@code targetOptionality} set to
 * {@code OPTIONAL}, exactly as {@code DremioSchemaConfig} configures it. It loads one date into both tables
 * via {@code ADD_ROWS} (mirroring {@code DataMaintenanceController#load}), confirms the baseline count, then
 * issues {@code REMOVE_ROWS} on both tables for that date (mirroring {@code DataMaintenanceController#delete}
 * exactly) and asserts the row count is unchanged afterward - the bug itself, not a crash.
 *
 * <p>See {@link PlainDatastoreRemoveRowsReproTest} for the same shape driven through the plain in-memory
 * {@code IDatastore} API instead - it confirms this bug is specific to DirectQuery (a scoped removal
 * genuinely removes the rows there).
 */
class OptionalJoinRemoveRowsNonRemovalReproTest {

    private static final String TRADES_TABLE = "Trades";
    private static final String TRADE_ATTRIBUTES_TABLE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeID";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyID";
    private static final String CUBE_NAME = "Cube";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 1, 6);

    @Test
    void removingBothTablesOnAnOptionalJoinDoesNotActuallyRemoveTheDate() throws Exception {
        final String jdbcUrl = "jdbc:h2:mem:bug1_optional_non_removal;DB_CLOSE_DELAY=-1";
        try (Connection setupConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = setupConnection.createStatement()) {
            statement.execute("CREATE TABLE \"" + TRADES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \"" + TRADE_ID
                    + "\" VARCHAR(50), \"" + NOTIONAL + "\" DOUBLE, PRIMARY KEY (\"" + AS_OF_DATE + "\", \""
                    + TRADE_ID + "\"))");
            statement.execute("CREATE TABLE \"" + TRADE_ATTRIBUTES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \""
                    + TRADE_ID + "\" VARCHAR(50), \"" + COUNTERPARTY_ID + "\" VARCHAR(50), PRIMARY KEY (\""
                    + AS_OF_DATE + "\", \"" + TRADE_ID + "\"))");
            // Three matched trades for the sole date this test ever uses - mirrors a normal, fully-joined
            // baseline load.
            for (int i = 1; i <= 3; i++) {
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
        // targetOptionality OPTIONAL - the one setting this whole bug hinges on.
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
        // bitmap provider, filteredOn this sole date - no umbrella provider.
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
            final long baselineCount = pointCount(cubeTester);
            assertEquals(3L, baselineCount, "baseline load should show all 3 matched trades for the date");

            // The actual repro: remove the sole loaded date from both tables, exactly like
            // DataMaintenanceController#delete. This must NOT throw - the bug is that it reports success
            // without actually removing anything, not that it crashes (that is bug-1's sibling NPE, filed
            // separately as PIVOT-14821).
            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(
                    TableUpdateDetail.create(
                            TRADES_TABLE, ChangeType.REMOVE_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1)),
                    TableUpdateDetail.create(
                            TRADE_ATTRIBUTES_TABLE,
                            ChangeType.REMOVE_ROWS,
                            ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            final long postDeleteCount = pointCount(cubeTester);
            // This is the bug itself: a correctly-scoped implementation would return 0 here. Instead the
            // OPTIONAL join's full-rebuild fallback re-imports every row it just claimed to remove.
            assertEquals(
                    3L,
                    postDeleteCount,
                    "bug-1 reproduction: REMOVE_ROWS under an OPTIONAL join should silently fail to remove the "
                            + "date (still 3), not actually remove it (which would be 0)");
        }
    }

    private static long pointCount(final CubeTester cubeTester) {
        final Object value = cubeTester
                .mdxQuery()
                .withMdx(String.format(
                        "SELECT {[Measures].[%s]} ON COLUMNS FROM [%s] WHERE ([%s].[%s].[%s])",
                        IMeasureHierarchy.COUNT_ID, CUBE_NAME, AS_OF_DATE, AS_OF_DATE, DATE_1))
                .run()
                .getTester()
                .hasOnlyOneCell()
                .getValue();
        return ((Number) value).longValue();
    }
}
