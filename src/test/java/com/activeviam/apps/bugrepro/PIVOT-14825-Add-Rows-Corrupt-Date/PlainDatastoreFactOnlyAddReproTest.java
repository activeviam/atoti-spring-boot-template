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
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.DatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.impl.ReferenceDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;

/**
 * Answers the "is this specific to DirectQuery?" question raised while filing <a
 * href="https://activeviam.atlassian.net/browse/PIVOT-14825">PIVOT-14825</a> (see also {@code bug-2.md}):
 * drives the plain in-memory {@code IDatastore} transaction API - <b>no DirectQuery, no
 * Dremio, no SQL, no Spring context</b> - through the same shape {@link
 * OptionalJoinFactOnlyAddMisrouteReproTest} uses (two joined stores, a {@code PARTIAL_BY_DATE}-shaped
 * cube, a matched baseline, then one more row added to {@code Trades} only, unmatched in {@code
 * TradeAttributes}).
 *
 * <p><b>Real, run result (26 Aug 2026, this exact file, {@code mvn -o test
 * -Dtest=PlainDatastoreFactOnlyAddReproTest}): one of the three DirectQuery symptoms reproduces here too -
 * the other two do not.</b> This overturns an earlier (source-only, never-run) hypothesis that the whole
 * bug was DirectQuery-SQL-specific (via {@code SqlGenerationUtil.fieldWithCoalesceIfNecessary}'s
 * {@code COALESCE} in generated SQL) - that mechanism cannot be the whole story, since the same phantom-date
 * symptom shows up here with no SQL involved at all:
 *
 * <ol>
 *   <li><b>Reproduces here too:</b> the unmatched row is misrouted to a phantom {@code 1970-01-01} member
 *       (point count 1), and the real date's own count stays stuck at its pre-add value ({@code 2}, not the
 *       correct {@code 3}) - confirmed with both this class's {@code PARTIAL_BY_DATE}-shaped provider and
 *       (checked interactively while isolating this, not kept as a committed file) a plain unpartitioned
 *       bitmap provider with no {@code filteredOn} at all - so this part of the bug needs neither
 *       DirectQuery nor aggregate-provider partitioning. It is a general behavior of a {@code
 *       withAllReachableFields()} selection when a field name (here {@code AsOfDate}) exists as both a
 *       base-store key field and a field reachable through a reference whose target row is missing.
 *   <li><b>Does NOT reproduce here:</b> the real date's count does not double (it stays at the pre-add 2,
 *       it does not become double the correct value of 3, i.e. not 6 either - it is simply stale/wrong in a
 *       different way than the DirectQuery version's 4).
 *   <li><b>Does NOT reproduce here:</b> a full ({@code non-NON EMPTY}) {@code [AsOfDate].[AsOfDate].Members}
 *       listing does not throw - it succeeds and correctly shows the phantom member alongside the real one.
 * </ol>
 *
 * <p>Conclusion: bug-2 is not purely a DirectQuery defect. The phantom-date misrouting itself is a core
 * ActiveViam engine behavior, reachable from the plain SDK; DirectQuery's refresh path additionally
 * introduces two extra, DirectQuery-specific failure modes on top of it (the doubled count, and the
 * axis-corrupting crash on a full member listing) that this plain-datastore path does not exhibit.
 */
class PlainDatastoreFactOnlyAddReproTest {

    private static final String TRADES_STORE = "Trades";
    private static final String TRADE_ATTRIBUTES_STORE = "TradeAttributes";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeId";
    private static final String NOTIONAL = "Notional";
    private static final String COUNTERPARTY_ID = "CounterpartyId";
    private static final String CUBE_NAME = "Cube";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 1, 6);
    private static final LocalDate PHANTOM_DATE = LocalDate.of(1970, 1, 1);

    @Test
    void factOnlyAddOfAnUnmatchedRowOnPlainDatastoreStillMisroutesToThePhantomDate() {
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
            // Two matched trades - the baseline.
            app.getDatastore().edit(t -> {
                t.addAll(TRADES_STORE, List.of(new Object[] {DATE_1, "T1", 100.0}, new Object[] {DATE_1, "T2", 200.0}));
                t.addAll(
                        TRADE_ATTRIBUTES_STORE,
                        List.of(new Object[] {DATE_1, "T1", "Cpty1"}, new Object[] {DATE_1, "T2", "Cpty2"}));
            });

            final CubeTester cubeTester = CubeTester.from(app.getManager());
            assertEquals(2L, pointCount(cubeTester, DATE_1), "baseline load should show both matched trades");

            // Fact-only add of an unmatched row - the plain-datastore analog of
            // DataMaintenanceController#loadFactOnly: only the Trades store is touched by this transaction.
            app.getDatastore().edit(t -> t.addAll(TRADES_STORE, List.<Object[]>of(new Object[] {DATE_1, "T3", 999.0})));

            // Anomaly 1 (reproduces here): the misrouted row lands on the phantom 1970-01-01 date.
            assertEquals(
                    1L,
                    pointCount(cubeTester, PHANTOM_DATE),
                    "bug-2 reproduction on plain datastore: the unmatched row is misrouted to 1970-01-01");
            // The real date's count is wrong too, but stays at its stale pre-add value rather than doubling
            // (contrast with DirectQuery's 4) - a different concrete wrongness, same underlying misrouting.
            assertEquals(
                    2L,
                    pointCount(cubeTester, DATE_1),
                    "the real date's count stays stuck at its pre-add value, not the correct 3 (and not "
                            + "DirectQuery's doubled 4 either)");
            // Anomaly 3 (does NOT reproduce here): unlike DirectQuery, the full member listing itself does
            // not throw - it succeeds and correctly reflects both members.
            assertDoesNotThrow(
                    () -> fullAsOfDateMemberCount(cubeTester),
                    "unlike DirectQuery's OPTIONAL-join bug, the AsOfDate axis itself is not corrupted here");
        }
    }

    private static long pointCount(final CubeTester cubeTester, final LocalDate date) {
        final var tester = cubeTester
                .mdxQuery()
                .withMdx(String.format(
                        "SELECT {[Measures].[%s]} ON COLUMNS FROM [%s] WHERE ([%s].[%s].[%s])",
                        IMeasureHierarchy.COUNT_ID, CUBE_NAME, AS_OF_DATE, AS_OF_DATE, date))
                .run()
                .getTester();
        if (tester.isEmpty()) {
            return 0L;
        }
        return ((Number) tester.hasOnlyOneCell().getValue()).longValue();
    }

    /** Full (non-{@code NON EMPTY}) listing of every declared {@code AsOfDate} member. */
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
