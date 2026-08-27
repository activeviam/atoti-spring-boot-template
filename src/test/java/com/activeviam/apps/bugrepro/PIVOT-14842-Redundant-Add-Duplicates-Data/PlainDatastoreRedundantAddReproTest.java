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
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.DatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.impl.ReferenceDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;

/**
 * Answers bug-4's (<a href="https://activeviam.atlassian.net/browse/PIVOT-14842">PIVOT-14842</a>) "is this
 * specific to DirectQuery?" question (see {@code bug-4.md}), the same way
 * {@code PIVOT-14825-Add-Rows-Corrupt-Date/PlainDatastoreFactOnlyAddReproTest} answered it for PIVOT-14825.
 *
 * <p><b>Important structural caveat, established before writing this test (see
 * {@code PIVOT-14823-DQ-Removal-not-working}'s investigation): {@code IReferenceDescription} - the plain
 * {@code IDatastore} join API - has no {@code RelationshipOptionality} concept at all.</b> {@code MANDATORY}
 * cannot even be expressed here, so this test cannot literally reproduce "bug-4 under a MANDATORY join" -
 * there is no such thing on this API. What it tests instead is the closest available analog: does replaying
 * an already-applied transaction (adding rows that are already present, keyed identically) duplicate them
 * in a plain datastore's own aggregate provider, the way a redundant DirectQuery {@code ADD_ROWS} refresh
 * does under {@code MANDATORY}? A plain {@code IDatastore} store has primary-key upsert semantics -
 * {@code addAll} of a row with an already-present key is expected to update in place, not duplicate - so
 * this test is also implicitly checking whether that basic guarantee holds, which is a different (and much
 * more fundamental) question than what DirectQuery's incremental-refresh-planner merge step does.
 */
class PlainDatastoreRedundantAddReproTest {

    private static final String TRADES_STORE = "Trades";
    private static final String TRADE_ATTRIBUTES_STORE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeId";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyId";
    private static final String CUBE_NAME = "Cube";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 2, 2);

    @Test
    void replayingAnAlreadyAppliedAddTransactionDoesNotDuplicateRowsOnAPlainDatastore() {
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

        // Same SINGLE-mode shape as the MANDATORY DirectQuery test: one unpartitioned bitmap provider, no
        // filteredOn.
        final IActivePivotInstanceDescription cube = StartBuilding.cube(CUBE_NAME)
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
            final Object[] trade1 = {DATE_1, "T1", 100.0};
            final Object[] trade2 = {DATE_1, "T2", 200.0};
            final Object[] attr1 = {DATE_1, "T1", "Cpty1"};
            final Object[] attr2 = {DATE_1, "T2", "Cpty2"};

            app.getDatastore().edit(t -> {
                t.addAll(TRADES_STORE, List.of(trade1, trade2));
                t.addAll(TRADE_ATTRIBUTES_STORE, List.of(attr1, attr2));
            });

            final CubeTester cubeTester = CubeTester.from(app.getManager());
            assertEquals(2L, pointCount(cubeTester, DATE_1), "baseline load should show both matched trades");

            // The plain-datastore analog of a redundant ADD_ROWS refresh: re-submit the exact same,
            // already-present, identically-keyed rows in a second transaction. On DirectQuery under
            // MANDATORY this duplicates (bug-4); a plain IDatastore's addAll has upsert semantics on an
            // already-present key, so the expectation here is that it stays correct at 2, not 4 - this test
            // exists to confirm that empirically, not assume it.
            app.getDatastore().edit(t -> {
                t.addAll(TRADES_STORE, List.of(trade1, trade2));
                t.addAll(TRADE_ATTRIBUTES_STORE, List.of(attr1, attr2));
            });

            final long countAfterReplay = pointCount(cubeTester, DATE_1);
            assertEquals(
                    2L,
                    countAfterReplay,
                    "bug-4 does NOT reproduce on a plain IDatastore: replaying an already-applied add "
                            + "transaction correctly stays at 2 (got "
                            + countAfterReplay
                            + ") - a plain store's key-based upsert semantics prevent the duplication DirectQuery's"
                            + " MANDATORY refresh planner exhibits. If this assertion fails, that finding is wrong"
                            + " and bug-4.md needs correcting, not this comment.");

            // A third replay, to match the DirectQuery test's cumulative-growth check - confirms this isn't
            // a one-time coincidence either.
            app.getDatastore().edit(t -> {
                t.addAll(TRADES_STORE, List.of(trade1, trade2));
                t.addAll(TRADE_ATTRIBUTES_STORE, List.of(attr1, attr2));
            });
            assertEquals(
                    2L,
                    pointCount(cubeTester, DATE_1),
                    "a third replay should also stay at 2 - no cumulative growth on the plain datastore");
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
