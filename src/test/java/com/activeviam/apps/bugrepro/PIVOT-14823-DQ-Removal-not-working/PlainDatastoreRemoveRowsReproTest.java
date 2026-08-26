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
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.api.conditions.BaseConditions;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.DatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.impl.ReferenceDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;

/**
 * Answers the "is this specific to DirectQuery?" question raised while filing <a
 * href="https://activeviam.atlassian.net/browse/PIVOT-14823">PIVOT-14823</a> (see also {@code bug-1.md}):
 * drives the plain in-memory {@code IDatastore} transaction API - <b>no DirectQuery, no
 * Dremio, no SQL, no Spring context</b> - through the same shape {@link
 * OptionalJoinRemoveRowsNonRemovalReproTest} uses (two joined stores, a {@code PARTIAL_BY_DATE}-shaped
 * cube, a scoped removal of both stores for one date in a single transaction) and checks whether the
 * removal actually takes effect.
 *
 * <p>One structural difference from the DirectQuery version cannot be reproduced here at all: {@link
 * IReferenceDescription}/{@code ReferenceDescription} (the plain in-memory join) has no {@code
 * RelationshipOptionality} concept whatsoever - only DirectQuery's {@code ITableJoin}/{@code SqlJoin} do.
 * There is nothing to configure as "OPTIONAL" on this path.
 *
 * <p><b>Real, run result (26 Aug 2026, this exact file, {@code mvn -o test
 * -Dtest=PlainDatastoreRemoveRowsReproTest}): the removal completes correctly - the date's contributor
 * count drops from 3 to 0, unlike DirectQuery's 3 staying 3.</b> This, combined with the source-level root
 * cause in {@code bug-1.md} (the defect lives entirely in {@code
 * AIncrementalViewRefreshPlanner#hasProblematicOptionalJoin}/{@code
 * IncrementalAggViewRefreshPlanner#fullRefreshOperations()}, both in {@code sql-database}'s SQL-refresh-
 * planning code, which only runs when reconciling against an external SQL source) and the observation
 * above that the plain datastore has no {@code RelationshipOptionality} setting to even trigger the bug's
 * precondition, confirms bug-1 is specific to DirectQuery and cannot occur on a plain in-memory node.
 */
class PlainDatastoreRemoveRowsReproTest {

    private static final String TRADES_STORE = "Trades";
    private static final String TRADE_ATTRIBUTES_STORE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeId";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyId";
    private static final String CUBE_NAME = "Cube";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 1, 6);

    @Test
    void removingFromTwoJoinedStoresOnPlainDatastoreActuallyRemoves() {
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

            // Scoped removal on both stores in one transaction - the plain-datastore analog of
            // DataMaintenanceController#delete.
            app.getDatastore().edit(t -> {
                t.removeWhere(TRADES_STORE, BaseConditions.equal(FieldPath.of(AS_OF_DATE), DATE_1));
                t.removeWhere(TRADE_ATTRIBUTES_STORE, BaseConditions.equal(FieldPath.of(AS_OF_DATE), DATE_1));
            });

            assertEquals(
                    0L,
                    pointCount(cubeTester),
                    "unlike DirectQuery's OPTIONAL-join bug, a plain-datastore scoped removal actually removes"
                            + " the date");
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
        // Unlike DirectQuery (where a removed member stays registered in the dictionary and this query
        // returns a real cell with value 0), a genuinely-successful plain-datastore removal evicts the
        // member entirely, so the WHERE clause resolves to an empty cell set instead of a zero-valued cell.
        if (tester.isEmpty()) {
            return 0L;
        }
        return ((Number) tester.hasOnlyOneCell().getValue()).longValue();
    }
}
