/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.bugrepro;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IDimension;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.database.jdbc.api.GenericJdbcProperties;
import com.activeviam.database.jdbc.api.SqlDialect;
import com.activeviam.database.sql.api.schema.SqlTableId;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.discoverer.IDirectQueryTableDiscoverer;
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
 * href="https://activeviam.atlassian.net/browse/PIVOT-14821">PIVOT-14821</a> (see also {@code
 * bug-reports/single_partitioned_remove_rows_npe.md}) that actually crashes - unlike {@link
 * SinglePartitionedRemoveRowsNpeReproTest}, which only drives the plain {@code IDatastore.edit(...)}
 * transaction API and (documented there) never reproduces it.
 *
 * <p>The difference: this test drives the exact same {@code Application#refresh(ChangeDescription)} DirectQuery
 * API that {@code DataMaintenanceController#delete} calls in production, against a cube built the same way as
 * this application's own {@code DataCubeConfig} in {@code SINGLE_PARTITIONED} mode (fat provider partitioned by
 * date, {@code withValuePartitioningOn(...)}). The external database is an in-memory H2 instance stood up inside
 * the test (H2 is already a {@code pom.xml} dependency, used elsewhere for the dev H2 console) via the same
 * generic-JDBC DirectQuery connector production uses for Dremio ({@code GenericJdbcConnectorMetaFactory}),
 * instead of a live Dremio server - so this file needs no Docker, no Dremio, and no Spring context, while still
 * going through DirectQuery's real refresh/commit orchestration.
 *
 * <p><b>Real, run result (26 Aug 2026, this exact file, {@code mvn -o test
 * -Dtest=DirectQueryRefreshRemoveRowsNpeReproTest}): reproduces the crash.</b> {@link
 * #removingTheOnlyLoadedDateThrowsNpe()} loads a single already-known {@code AsOfDate} into one table via {@code
 * ChangeType.ADD_ROWS} (confirmed not to throw), then issues a {@code ChangeType.REMOVE_ROWS} refresh scoped to
 * that same date - mirroring the {@code Trades} half of {@code DataMaintenanceController#delete} - and that
 * refresh throws. Walking the real exception's cause chain lands on a {@link NullPointerException} whose message
 * is {@code "Cannot invoke \"Object.toString()\" because the return value of
 * \"com.activeviam.tech.dictionaries.api.IDictionary.read(int)\" is null"}, thrown from {@code
 * AMultiVersionPartitionedIndexedAggregateProvider.getPartitioningFieldValue} - byte-for-byte the same class,
 * method, and message as the production stack trace in {@code bug-reports/single_partitioned_remove_rows_npe.md}.
 * This resolves that report's open question #1 ("does this reproduce without DirectQuery?"): no, but it does
 * reproduce with DirectQuery's real refresh orchestration even against a non-Dremio (H2) backend and a single
 * unjoined table, confirming the bug lives in {@code Application#refresh}'s own commit path against the
 * partitioned aggregate provider itself, not anything Dremio-specific or join-specific.
 *
 * <p><b>Two broader, separately-confirmed findings, not asserted by this test, both real observed runs of earlier
 * versions of this same file (26 Aug 2026):</b>
 *
 * <ol>
 *   <li>Loading two distinct dates through DirectQuery - either both at once in one {@code ADD_ROWS} refresh, or
 *       one after the other across two separate {@code ADD_ROWS} refreshes - hits the identical
 *       NullPointerException on the load itself, before {@code REMOVE_ROWS} is ever issued. I.e. via DirectQuery,
 *       {@code AMultiVersionPartitionedIndexedAggregateProvider}'s post-commit gauge registration can NPE as soon
 *       as it has to tag a <em>second</em> distinct partitioning-field value in the same provider, regardless of
 *       whether that commit adds or removes rows.
 *   <li>Adding a {@code Trades}-to-{@code TradeAttributes} join (mirroring {@code DremioSchemaConfig}'s {@code
 *       RelationshipOptionality.OPTIONAL} shape) makes even the very <em>first</em> {@code ADD_ROWS} refresh of a
 *       single date crash the same way, before any second date or any {@code REMOVE_ROWS} is involved.
 * </ol>
 *
 * <p>Neither finding above is asserted here - both prevented isolating {@code REMOVE_ROWS} cleanly (the crash
 * fired earlier, on load), so this test instead keeps the schema to one unjoined table and exactly one date
 * throughout, which is the one shape that let the load succeed and isolated {@code REMOVE_ROWS} itself as the
 * trigger. Both are flagged here as genuine open leads for ActiveViam, not fabricated or extrapolated - each was
 * an actual {@code mvn -o test} run of a real (now-superseded) version of this file, not a guess.
 */
class DirectQueryRefreshRemoveRowsNpeReproTest {

    private static final String TRADES_TABLE = "Trades";
    private static final String AS_OF_DATE = "AsOfDate";
    private static final String TRADE_ID = "TradeID";
    private static final String NOTIONAL = "Notional";

    private static final LocalDate DATE_1 = LocalDate.of(2019, 1, 6);

    @Test
    void removingTheOnlyLoadedDateThrowsNpe() throws Exception {
        final String jdbcUrl = "jdbc:h2:mem:npe_repro_single_table;DB_CLOSE_DELAY=-1";
        try (Connection setupConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = setupConnection.createStatement()) {
            statement.execute("CREATE TABLE \"" + TRADES_TABLE + "\" (\"" + AS_OF_DATE + "\" DATE, \"" + TRADE_ID
                    + "\" VARCHAR(50), \"" + NOTIONAL + "\" DOUBLE, PRIMARY KEY (\"" + AS_OF_DATE + "\", \""
                    + TRADE_ID + "\"))");
            // Exactly one date's rows, inserted before the cube ever loads anything - see the class
            // Javadoc for why this test deliberately never lets a second distinct AsOfDate value exist
            // and never adds a join, to isolate the narrower REMOVE_ROWS-on-a-single-partition trigger
            // the bug report describes.
            statement.execute("INSERT INTO \"" + TRADES_TABLE + "\" VALUES ('2019-01-06', 'T1', 100.0)");
        }

        // GenericJdbcConnectorMetaFactory is the same connector factory DremioConnectorConfig uses in
        // production - only the dialect/connection string differ (H2 needs neither Dremio's custom
        // NOT_SUPPORTED literal-parameter override nor its custom type serialization, so plain defaults
        // via SqlDialect.builder().build() are enough).
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

        final SchemaDescription schema =
                SchemaDescription.builder().externalTables(List.of(tradesTable)).build();

        final ISelectionDescription selection = StartBuilding.selection(schema)
                .fromBaseStore(TRADES_TABLE)
                .withAllReachableFields()
                .build();

        final LevelIdentifier asOfDateLevel = LevelIdentifier.simple(AS_OF_DATE);

        // Same fat-provider-partitioned-by-date shape as DataCubeConfig's SINGLE_PARTITIONED mode - the
        // one line that matters is withValuePartitioningOn(AS_OF_DATE).
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

        try (Application application = Application.builder(connector)
                .schema(schema)
                .managerDescription(manager)
                .build()) {
            application.start();

            assertDoesNotThrow(() -> application.refresh(ChangeDescription.create(
                    List.of(TableUpdateDetail.create(TRADES_TABLE, ChangeType.ADD_ROWS, ConditionFactory.allRows())))));

            // The actual repro: remove the sole already-loaded date, exactly like the Trades half of
            // DataMaintenanceController#delete.
            final Exception thrown = assertThrows(
                    Exception.class,
                    () -> application.refresh(ChangeDescription.create(List.of(TableUpdateDetail.create(
                            TRADES_TABLE, ChangeType.REMOVE_ROWS, ConditionFactory.equal(AS_OF_DATE, DATE_1))))));

            Throwable cause = thrown;
            Exception npeCause = null;
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
}
