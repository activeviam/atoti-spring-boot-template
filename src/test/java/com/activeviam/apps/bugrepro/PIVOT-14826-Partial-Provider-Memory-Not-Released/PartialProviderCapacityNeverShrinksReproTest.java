/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.bugrepro;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
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
import com.activeviam.activepivot.server.impl.api.observability.MemoryAnalysisServiceFactory;
import com.activeviam.activepivot.server.intf.api.observability.IMemoryAnalysisService;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.api.schema.ITableJoin;
import com.activeviam.database.api.schema.RelationshipOptionality;
import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.database.jdbc.api.GenericJdbcProperties;
import com.activeviam.database.jdbc.api.SqlDialect;
import com.activeviam.database.sql.api.schema.SqlTableId;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.IDirectQueryDatabase;
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
import com.activeviam.tech.observability.api.memory.IMemoryStatistic;
import com.activeviam.tech.observability.api.memory.IStatisticAttribute;
import com.activeviam.tech.observability.internal.memory.MemoryStatisticConstants;

/**
 * Reproduction of the bug reported to ActiveViam as <a
 * href="https://activeviam.atlassian.net/browse/PIVOT-14826">PIVOT-14826</a> (see also {@code bug-3.md} in
 * this directory), found while rehearsing the WCR date-roll pattern (delete an old date, load a new one)
 * against a {@code PARTIAL_BY_DATE}-shaped cube: under {@code DataCubeConfig.AggregateProviderMode.PARTIAL_BY_DATE} - one independently-
 * named bitmap partial provider per date, built via {@code .withPartialProvider().withName("ByAsOfDate_" +
 * date).bitmap().includingOnlyLevels(level).filteredOn(Map.of(level, date))} - a per-date partial
 * provider's off-heap memory allocation is never released once allocated, even after the date's data is
 * correctly and completely deleted.
 *
 * <p>Unlike {@code PIVOT-14823} (see {@code OptionalJoinRemoveRowsNonRemovalReproTest}), this test issues a
 * <b>real SQL {@code DELETE}</b> against the external H2 source for both {@code Trades} and {@code
 * TradeAttributes} before refreshing - so the deletion itself is genuine and correct (the post-delete MDX
 * point count for the date goes to an empty cell set, i.e. the member is evicted from the hierarchy, not
 * left stale). The bug is purely about memory: the {@code PartialProvider} statistic for the date's
 * provider (measured via ActiveViam's {@link IMemoryAnalysisService}, walking the {@link IMemoryStatistic}
 * tree for the node named {@link MemoryStatisticConstants#STAT_NAME_PARTIAL_PROVIDER} whose {@link
 * MemoryStatisticConstants#ATTR_NAME_PROVIDER_NAME} attribute matches the provider's name) reports the
 * exact same {@code retainedOffHeap} figure before and after the genuine delete.
 *
 * <p>Root cause is not diagnosed at the ActiveViam source level (unlike PIVOT-14821/14823/14825) - this is
 * an empirical characterization only, most likely a property of the partial/bitmap provider's own chunk-
 * based off-heap allocator (chunks allocated for a partition are not returned to the allocator once the
 * partition holds zero rows), independent of DirectQuery's refresh mechanism.
 *
 * <p>See {@link PlainDatastorePartialProviderCapacityReproTest} for the same shape driven through the plain
 * in-memory {@code IDatastore} API instead, answering whether this is specific to DirectQuery.
 */
class PartialProviderCapacityNeverShrinksReproTest {

    private static final String TRADES_TABLE = "Trades";
    private static final String TRADE_ATTRIBUTES_TABLE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeID";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyID";
    private static final String CUBE_NAME = "Cube";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 1, 6);
    private static final String PROVIDER_NAME = "ByAsOfDate_" + DATE_1;

    @Test
    void partialProviderOffHeapDoesNotShrinkAfterAGenuineCorrectDelete() throws Exception {
        final String jdbcUrl = "jdbc:h2:mem:bug3_partial_provider_capacity;DB_CLOSE_DELAY=-1";
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

        // Same join shape as DremioSchemaConfig / bug-1's test: Trades -> TradeAttributes, keyed on
        // AsOfDate+TradeID, targetOptionality OPTIONAL.
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
                .withName(PROVIDER_NAME)
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
            assertEquals(3L, pointCount(cubeTester), "baseline load should show all 3 matched trades for the date");

            final IMemoryAnalysisService memoryAnalysisService = MemoryAnalysisServiceFactory.create(
                    application.getDatabase(), application.getManager(), Path.of("."));
            final long baselineOffHeap = retainedOffHeapOfProvider(memoryAnalysisService, PROVIDER_NAME);
            assertTrue(
                    baselineOffHeap > 0,
                    "the date's partial provider should have a non-zero off-heap footprint once loaded, was "
                            + baselineOffHeap);

            // The genuine, correct delete: unlike bug-1's production `delete` (which never touches the
            // external source), this test issues real DML against H2, so even the OPTIONAL join's
            // full-refresh fallback (see PIVOT-14823) re-fetches a source that now genuinely has zero rows
            // for the date - the deletion is not confounded by that separate bug.
            try (Connection deleteConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
                    Statement statement = deleteConnection.createStatement()) {
                statement.execute(
                        "DELETE FROM \"" + TRADE_ATTRIBUTES_TABLE + "\" WHERE \"" + AS_OF_DATE + "\" = '2019-01-06'");
                statement.execute("DELETE FROM \"" + TRADES_TABLE + "\" WHERE \"" + AS_OF_DATE + "\" = '2019-01-06'");
            }

            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(List.of(
                    TableUpdateDetail.create(
                            TRADES_TABLE, ChangeType.REMOVE_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1)),
                    TableUpdateDetail.create(
                            TRADE_ATTRIBUTES_TABLE,
                            ChangeType.REMOVE_ROWS,
                            ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            // Rule out epoch-retention lag as an explanation before re-measuring: force-discard every
            // epoch we can, on top of the default KeepLastEpochPolicy(numberEpochsToKeep=1).
            final IDirectQueryDatabase directQueryDatabase = (IDirectQueryDatabase) application.getDatabase();
            directQueryDatabase.getInMemoryDatastore().getEpochManager().discard();
            directQueryDatabase.getInMemoryDatastore().getEpochManager().forceDiscardEpochs(node -> true);

            // Confirm the delete is genuinely correct: the date's member is evicted from the hierarchy
            // entirely (empty cell set), not merely left at a stale value the way bug-1 leaves it.
            assertEquals(
                    0L,
                    pointCount(cubeTester),
                    "the delete against the real H2 source should make the date's row count genuinely go to "
                            + "zero (member evicted), not merely appear to");

            final long postDeleteOffHeap = retainedOffHeapOfProvider(memoryAnalysisService, PROVIDER_NAME);

            // This is the bug itself: a provider whose partition now genuinely holds zero rows should not
            // retain the same off-heap footprint it had when it held 3 rows. Instead, the allocation is
            // never released - the assertion below is the repro (a green test documents the bug, matching
            // this repo's convention for bug-1/bug-2).
            assertEquals(
                    baselineOffHeap,
                    postDeleteOffHeap,
                    "bug-3 reproduction: the partial provider's off-heap footprint should shrink (or at least "
                            + "not stay identical) once its partition is genuinely emptied, but it never does");
        }
    }

    private static long pointCount(final CubeTester cubeTester) {
        final var tester = cubeTester
                .mdxQuery()
                .withMdx(String.format(
                        "SELECT {[Measures].[%s]} ON COLUMNS FROM [%s] WHERE ([%s].[%s].[%s])",
                        IMeasureHierarchy.COUNT_ID, CUBE_NAME, AS_OF_DATE, AS_OF_DATE, DATE_1))
                .run()
                .getTester();
        if (tester.isEmpty()) {
            return 0L;
        }
        return ((Number) tester.hasOnlyOneCell().getValue()).longValue();
    }

    /**
     * Walks the {@link IMemoryStatistic} tree returned by {@link
     * IMemoryAnalysisService#getApplicationMemoryStatistic()} looking for the {@code PartialProvider} node
     * whose {@code name} attribute matches {@code providerName}, and returns its {@code retainedOffHeap}.
     */
    private static long retainedOffHeapOfProvider(
            final IMemoryAnalysisService memoryAnalysisService, final String providerName) {
        final IMemoryStatistic root = memoryAnalysisService.getApplicationMemoryStatistic();
        final IMemoryStatistic providerStatistic = findProviderStatistic(root, providerName);
        assertNotNull(providerStatistic, "could not find a PartialProvider statistic named " + providerName);
        return providerStatistic.getRetainedOffHeap();
    }

    private static IMemoryStatistic findProviderStatistic(final IMemoryStatistic node, final String providerName) {
        if (MemoryStatisticConstants.STAT_NAME_PARTIAL_PROVIDER.equals(node.getName())) {
            final IStatisticAttribute nameAttribute =
                    node.getAttribute(MemoryStatisticConstants.ATTR_NAME_PROVIDER_NAME);
            if (nameAttribute != null && providerName.equals(nameAttribute.asText())) {
                return node;
            }
        }
        for (final IMemoryStatistic child : node.getChildren()) {
            final IMemoryStatistic found = findProviderStatistic(child, providerName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
