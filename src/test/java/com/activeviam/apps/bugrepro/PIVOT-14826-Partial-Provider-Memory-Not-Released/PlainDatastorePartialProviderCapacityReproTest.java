/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.bugrepro;

import static com.activeviam.database.api.types.ILiteralType.DOUBLE;
import static com.activeviam.database.api.types.ILiteralType.LOCAL_DATE;
import static com.activeviam.database.api.types.ILiteralType.STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
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
import com.activeviam.database.api.conditions.BaseConditions;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.DatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.impl.ReferenceDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;
import com.activeviam.tech.observability.api.memory.IMemoryStatistic;
import com.activeviam.tech.observability.api.memory.IStatisticAttribute;
import com.activeviam.tech.observability.internal.memory.MemoryStatisticConstants;

/**
 * Answers the "is this specific to DirectQuery?" question raised while filing <a
 * href="https://activeviam.atlassian.net/browse/PIVOT-14826">PIVOT-14826</a> (see also {@code bug-3.md}):
 * drives the plain in-memory {@code IDatastore} transaction API
 * - <b>no DirectQuery, no SQL, no Spring context</b> - through the same shape {@link
 * PartialProviderCapacityNeverShrinksReproTest} uses (a {@code PARTIAL_BY_DATE}-shaped cube, one date
 * loaded then genuinely removed via a scoped {@code removeWhere} transaction) and checks whether the
 * date's {@code PartialProvider} off-heap footprint shrinks (or at least changes) once the partition is
 * genuinely emptied.
 *
 * <p>The same {@link IMemoryAnalysisService}/{@link IMemoryStatistic} mechanism is used to measure the
 * provider's {@code retainedOffHeap}, via the {@link MemoryAnalysisServiceFactory#create} overload that
 * accepts a plain {@code IDatastore} (which, like {@code IDirectQueryDatabase}, implements {@code
 * IDatabase}).
 */
class PlainDatastorePartialProviderCapacityReproTest {

    private static final String TRADES_STORE = "Trades";
    private static final String TRADE_ATTRIBUTES_STORE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeId";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyId";
    private static final String CUBE_NAME = "Cube";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 1, 6);
    private static final String PROVIDER_NAME = "ByAsOfDate_" + DATE_1;

    @Test
    void partialProviderOffHeapDoesNotShrinkAfterAGenuineRemovalOnAPlainDatastore() {
        final IStoreDescription tradesStore = StoreDescription.builder()
                .withStoreName(TRADES_STORE)
                .withField(AS_OF_DATE, LOCAL_DATE)
                .asKeyField()
                .withField(TRADE_ID, STRING)
                .asKeyField()
                .withField(NOTIONAL, DOUBLE)
                .build();
        final IStoreDescription tradeAttributesStore = StoreDescription.builder()
                .withStoreName(TRADE_ATTRIBUTES_STORE)
                .withField(AS_OF_DATE, LOCAL_DATE)
                .asKeyField()
                .withField(TRADE_ID, STRING)
                .asKeyField()
                .withField(COUNTERPARTY_ID, STRING)
                .build();
        final IReferenceDescription tradesToAttributes = ReferenceDescription.builder()
                .fromStore(TRADES_STORE)
                .toStore(TRADE_ATTRIBUTES_STORE)
                .withName("Trades_to_TradeAttributes")
                .withMapping(AS_OF_DATE, AS_OF_DATE)
                .withMapping(TRADE_ID, TRADE_ID)
                .build();
        final IDatastoreSchemaDescription datastoreSchema =
                new DatastoreSchemaDescription(List.of(tradesStore, tradeAttributesStore), List.of(tradesToAttributes));

        final ISelectionDescription selection = StartBuilding.selection(datastoreSchema)
                .fromBaseStore(TRADES_STORE)
                .withAllReachableFields()
                .build();

        final LevelIdentifier asOfDateLevel = LevelIdentifier.simple(AS_OF_DATE);

        // Same PARTIAL_BY_DATE shape as the DirectQuery version's cube (and DataCubeConfig's
        // PARTIAL_BY_DATE mode): one independently-named bitmap provider, filteredOn this sole date.
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

        try (ApplicationWithDatastore app = StartBuilding.application()
                .withDatastore(datastoreSchema)
                .withManager(manager)
                .withoutBranchRestrictions()
                .buildAndStart()) {
            app.getDatastore().edit(t -> {
                t.addAll(
                        TRADES_STORE,
                        List.of(new Object[] {DATE_1, "T1", 100.0}, new Object[] {DATE_1, "T2", 200.0}, new Object[] {
                            DATE_1, "T3", 300.0
                        }));
                t.addAll(
                        TRADE_ATTRIBUTES_STORE,
                        List.of(
                                new Object[] {DATE_1, "T1", "Cpty1"},
                                new Object[] {DATE_1, "T2", "Cpty2"},
                                new Object[] {DATE_1, "T3", "Cpty3"}));
            });

            final CubeTester cubeTester = CubeTester.from(app.getManager());
            assertEquals(3L, pointCount(cubeTester), "baseline load should show all 3 matched trades");

            final IMemoryAnalysisService memoryAnalysisService =
                    MemoryAnalysisServiceFactory.create(app.getDatastore(), app.getManager(), Path.of("."));
            final long baselineOffHeap = retainedOffHeapOfProvider(memoryAnalysisService, PROVIDER_NAME);
            assertTrue(
                    baselineOffHeap > 0,
                    "the date's partial provider should have a non-zero off-heap footprint once loaded, was "
                            + baselineOffHeap);

            // Scoped removal on both stores in one transaction - the plain-datastore analog of the genuine
            // SQL DELETE + REMOVE_ROWS refresh the DirectQuery version issues.
            app.getDatastore().edit(t -> {
                t.removeWhere(TRADES_STORE, BaseConditions.equal(FieldPath.of(AS_OF_DATE), DATE_1));
                t.removeWhere(TRADE_ATTRIBUTES_STORE, BaseConditions.equal(FieldPath.of(AS_OF_DATE), DATE_1));
            });

            app.getDatastore().getEpochManager().discard();
            app.getDatastore().getEpochManager().forceDiscardEpochs(node -> true);

            assertEquals(
                    0L,
                    pointCount(cubeTester),
                    "a plain-datastore scoped removal actually removes the date - the member is evicted "
                            + "entirely, not merely left stale");

            final long postDeleteOffHeap = retainedOffHeapOfProvider(memoryAnalysisService, PROVIDER_NAME);

            // Real result: this reproduces here too - the plain-datastore partial provider's off-heap
            // footprint stays exactly the same after the partition is genuinely emptied, just like
            // DirectQuery's. Unlike bug-1/bug-2, this bug is NOT specific to DirectQuery.
            assertEquals(
                    baselineOffHeap,
                    postDeleteOffHeap,
                    "bug-3 also reproduces on a plain in-memory datastore: the partial provider's off-heap "
                            + "footprint does not shrink once its partition is genuinely emptied, with no "
                            + "DirectQuery, no SQL, and no Spring context involved");
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
