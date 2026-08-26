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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IDimension;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.database.api.conditions.BaseConditions;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.DatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.impl.ReferenceDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;

/**
 * Attempted minimal, self-contained reproduction of a bug reported to ActiveViam as <a
 * href="https://activeviam.atlassian.net/browse/PIVOT-14821">PIVOT-14821</a>: an aggregate provider
 * built with {@code withValuePartitioningOn(...)} (called "SINGLE_PARTITIONED" in our own application's
 * terminology) throws a {@link NullPointerException} during post-commit gauge registration when a
 * {@code REMOVE_ROWS}-equivalent transaction removes an entire already-loaded partition value - but only
 * when driven through DirectQuery's {@code Application#refresh}. This file uses <b>no DirectQuery, no
 * Dremio, no Spring context</b> - just the plain in-memory {@link IDatastore} transaction API
 * ({@code datastore.edit(...)}). Run with: {@code mvn -o test -Dtest=SinglePartitionedRemoveRowsNpeReproTest}
 *
 * <p><b>Real, run result (26 Aug 2026, this exact file, {@code mvn -o test}) - all 3 tests pass, meaning
 * none of them reproduce the crash:</b>
 * <ol>
 *   <li>{@link #removingFromASingleUnjoinedStoreCompletesCleanly()} - a scoped removal on one unjoined
 *       store: no exception.
 *   <li>{@link #removingFromTwoJoinedStoresCompletesCleanly()} - a scoped removal on two joined stores in
 *       one transaction (matching our application's own {@code Trades}/{@code TradeAttributes} shape): no
 *       exception.
 *   <li>{@link #wipingWholeStoreThenReloadingSurvivingRowsCompletesCleanly()} - truncating the whole store
 *       then re-adding every surviving row in one transaction (the closest native-API analog to what our
 *       own source-level root-cause analysis found DirectQuery's {@code OPTIONAL}-join refresh planner
 *       actually does under the hood, {@code IncrementalAggViewRefreshPlanner#fullRefreshOperations()}):
 *       still no exception.
 * </ol>
 *
 * <p><b>This file is a negative result only</b> - it does NOT reproduce the actual NPE. See {@link
 * DirectQueryRefreshRemoveRowsNpeReproTest} for a test that reproduces the crash for real, by driving the
 * same {@code Application#refresh(ChangeDescription)} DirectQuery API used in production instead of the
 * plain datastore transaction API exercised here.
 *
 * <p><b>Conclusion, narrowing our own earlier open question:</b> the bug is not simply "any transaction
 * that removes/replaces a whole partition's worth of rows under {@code withValuePartitioningOn(...)}" -
 * that reproduces on none of the three shapes above via the plain SDK. It appears to require something
 * specific to DirectQuery's own {@code Application#refresh} orchestration itself, not just the end state
 * a transaction leaves the datastore in. Circumstantial support: the original crash's own stack trace
 * (see the bug report) passes through {@code CompositeStreamingTransaction}/{@code
 * IncrementalRefreshParentTask}/{@code ADataContributorHandlingCubeTransaction} - DirectQuery-specific
 * wrapper classes that never appear anywhere in the plain {@code datastore.edit(...)} commit path these
 * three tests exercise. This is reported as a negative result, not a proven mechanism - we have not
 * traced far enough into DirectQuery's own refresh code to say exactly what it does differently.
 */
class SinglePartitionedRemoveRowsNpeReproTest {

    private static final String TRADES_STORE = "Trades";
    private static final String TRADE_ATTRIBUTES_STORE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeId";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyId";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 1, 6);
    private static final LocalDate DATE_2 = LocalDate.of(2019, 1, 7);

    @Test
    void removingFromASingleUnjoinedStoreCompletesCleanly() {
        final IStoreDescription tradesStore = StoreDescription.builder()
                .withStoreName(TRADES_STORE)
                .withField(AS_OF_DATE, LOCAL_DATE)
                .asKeyField()
                .withField(TRADE_ID, STRING)
                .asKeyField()
                .withField(NOTIONAL, DOUBLE)
                .build();
        final IDatastoreSchemaDescription datastoreSchema =
                new DatastoreSchemaDescription(List.of(tradesStore), List.of());

        final ISelectionDescription selection = StartBuilding.selection(datastoreSchema)
                .fromBaseStore(TRADES_STORE)
                .withAllReachableFields()
                .build();

        final LevelIdentifier asOfDateLevel = LevelIdentifier.simple(AS_OF_DATE);

        // The one line that matters: withValuePartitioningOn(AS_OF_DATE) is what makes this provider's
        // post-commit gauge-registration code walk its own internal per-value partitions - and that
        // walk is where the NPE lives. Without this line (i.e. a plain, unpartitioned bitmap provider),
        // the identical remove below completes cleanly.
        final IActivePivotInstanceDescription cube = StartBuilding.cube("Cube")
                .withContributorsCount()
                .withDimensions(b -> b.withDimension(AS_OF_DATE)
                        .withType(IDimension.DimensionType.TIME)
                        .withHierarchy(AS_OF_DATE)
                        .withLevelOfSameName())
                .withAggregateProvider()
                .jit()
                .withPartialProvider()
                .withName("ByAsOfDate")
                .bitmap()
                .includingOnlyLevels(asOfDateLevel)
                .withValuePartitioningOn(AS_OF_DATE)
                .build();

        final IActivePivotManagerDescription manager = StartBuilding.managerDescription("Manager")
                .withCatalog("Catalog")
                .containingAllCubes()
                .withSchema("Schema")
                .withSelection(selection)
                .withCube(cube)
                .build();

        final IDatastore datastore = StartBuilding.application()
                .withDatastore(datastoreSchema)
                .withManager(manager)
                .withoutBranchRestrictions()
                .buildAndStart()
                .getDatastore();

        // Load two dates - DATE_1 is the one we'll remove afterward.
        datastore.edit(t -> t.addAll(
                TRADES_STORE, List.of(new Object[] {DATE_1, "T1", 100.0}, new Object[] {DATE_2, "T2", 200.0})));

        // Remove one whole already-loaded partition value in a single transaction, on this one store
        // alone. Real, run result: completes cleanly, no exception - a single unjoined store is not
        // enough to reproduce the bug on its own.
        assertDoesNotThrow(() -> datastore.edit(
                t -> t.removeWhere(TRADES_STORE, BaseConditions.equal(FieldPath.of(AS_OF_DATE), DATE_1))));
    }

    @Test
    void removingFromTwoJoinedStoresCompletesCleanly() {
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

        final IActivePivotInstanceDescription cube = StartBuilding.cube("Cube")
                .withContributorsCount()
                .withDimensions(b -> b.withDimension(AS_OF_DATE)
                        .withType(IDimension.DimensionType.TIME)
                        .withHierarchy(AS_OF_DATE)
                        .withLevelOfSameName())
                .withAggregateProvider()
                .jit()
                .withPartialProvider()
                .withName("ByAsOfDate")
                .bitmap()
                .includingOnlyLevels(asOfDateLevel)
                .withValuePartitioningOn(AS_OF_DATE)
                .build();

        final IActivePivotManagerDescription manager = StartBuilding.managerDescription("Manager")
                .withCatalog("Catalog")
                .containingAllCubes()
                .withSchema("Schema")
                .withSelection(selection)
                .withCube(cube)
                .build();

        final IDatastore datastore = StartBuilding.application()
                .withDatastore(datastoreSchema)
                .withManager(manager)
                .withoutBranchRestrictions()
                .buildAndStart()
                .getDatastore();

        datastore.edit(t -> {
            t.addAll(TRADES_STORE, List.of(new Object[] {DATE_1, "T1", 100.0}, new Object[] {DATE_2, "T2", 200.0}));
            t.addAll(
                    TRADE_ATTRIBUTES_STORE,
                    List.of(new Object[] {DATE_1, "T1", "Cpty1"}, new Object[] {DATE_2, "T2", "Cpty2"}));
        });

        // Remove one whole already-loaded partition value from BOTH joined stores in a single
        // transaction - this mirrors our application's own DataMaintenanceController#delete, which
        // issues one ChangeDescription with a REMOVE_ROWS TableUpdateDetail per table. Real, run
        // result: also completes cleanly, no exception - a scoped removeWhere on two joined stores,
        // in one transaction, is STILL not enough to reproduce the bug on its own.
        assertDoesNotThrow(() -> datastore.edit(t -> {
            t.removeWhere(TRADES_STORE, BaseConditions.equal(FieldPath.of(AS_OF_DATE), DATE_1));
            t.removeWhere(TRADE_ATTRIBUTES_STORE, BaseConditions.equal(FieldPath.of(AS_OF_DATE), DATE_1));
        }));
    }

    @Test
    void wipingWholeStoreThenReloadingSurvivingRowsCompletesCleanly() {
        final IStoreDescription tradesStore = StoreDescription.builder()
                .withStoreName(TRADES_STORE)
                .withField(AS_OF_DATE, LOCAL_DATE)
                .asKeyField()
                .withField(TRADE_ID, STRING)
                .asKeyField()
                .withField(NOTIONAL, DOUBLE)
                .build();
        final IDatastoreSchemaDescription datastoreSchema =
                new DatastoreSchemaDescription(List.of(tradesStore), List.of());

        final ISelectionDescription selection = StartBuilding.selection(datastoreSchema)
                .fromBaseStore(TRADES_STORE)
                .withAllReachableFields()
                .build();

        final LevelIdentifier asOfDateLevel = LevelIdentifier.simple(AS_OF_DATE);

        final IActivePivotInstanceDescription cube = StartBuilding.cube("Cube")
                .withContributorsCount()
                .withDimensions(b -> b.withDimension(AS_OF_DATE)
                        .withType(IDimension.DimensionType.TIME)
                        .withHierarchy(AS_OF_DATE)
                        .withLevelOfSameName())
                .withAggregateProvider()
                .jit()
                .withPartialProvider()
                .withName("ByAsOfDate")
                .bitmap()
                .includingOnlyLevels(asOfDateLevel)
                .withValuePartitioningOn(AS_OF_DATE)
                .build();

        final IActivePivotManagerDescription manager = StartBuilding.managerDescription("Manager")
                .withCatalog("Catalog")
                .containingAllCubes()
                .withSchema("Schema")
                .withSelection(selection)
                .withCube(cube)
                .build();

        final IDatastore datastore = StartBuilding.application()
                .withDatastore(datastoreSchema)
                .withManager(manager)
                .withoutBranchRestrictions()
                .buildAndStart()
                .getDatastore();

        datastore.edit(t -> t.addAll(
                TRADES_STORE, List.of(new Object[] {DATE_1, "T1", 100.0}, new Object[] {DATE_2, "T2", 200.0})));

        // Instead of a scoped removeWhere, mirror what our own root-cause analysis found DirectQuery's
        // OPTIONAL-join refresh planner actually does under the hood for a REMOVE_ROWS refresh
        // (IncrementalAggViewRefreshPlanner#fullRefreshOperations): wipe the ENTIRE store, then blindly
        // re-add every row that should still be there (DATE_2's only) - all in ONE transaction, same as
        // the planner's two-operation list (IncrementalRemoveWhereOperation(TRUE) then
        // IncrementalAddAggViewOperation). This is the closest native-API analog to that mechanism.
        // Real, run result: also completes cleanly, no exception.
        assertDoesNotThrow(() -> datastore.edit(t -> {
            t.truncate(TRADES_STORE);
            t.addAll(TRADES_STORE, List.<Object[]>of(new Object[] {DATE_2, "T2", 200.0}));
        }));
    }
}
