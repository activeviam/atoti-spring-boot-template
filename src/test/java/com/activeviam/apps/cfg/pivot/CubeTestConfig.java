/*
 * Copyright (C) ActiveViam 2023-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConstants.CATALOG_NAME;
import static com.activeviam.apps.cfg.pivot.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.cfg.pivot.CubeConstants.INT_FORMATTER;
import static com.activeviam.apps.cfg.pivot.CubeConstants.MANAGER_NAME;
import static com.activeviam.apps.cfg.pivot.CubeConstants.NATIVE_MEASURES;
import static com.activeviam.apps.cfg.pivot.CubeConstants.SCHEMA_NAME;
import static com.activeviam.apps.cfg.pivot.CubeConstants.TIMESTAMP_FORMATTER;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;
import static com.activeviam.database.api.types.ILiteralType.DOUBLE;
import static com.activeviam.database.api.types.ILiteralType.LOCAL_DATE;
import static com.activeviam.database.api.types.ILiteralType.STRING;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.DatastoreSchemaDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;

/**
 * Builds a plain, non-distributed in-memory cube (no DirectQuery, no Dremio, no cluster/JGroups) mirroring
 * the production single-table {@code TradesMerged} shape (Phase 4), so {@link Measures}/{@link Dimensions}
 * can be unit-tested fast and in isolation from Stage 1 (DirectQuery) and Stage 2 (distribution) wiring.
 */
@Configuration
@Profile("!data-node & !query-node")
@ComponentScan(basePackageClasses = {Measures.class})
public class CubeTestConfig {
    private final CubeTester cubeTester;
    private final IDatastore datastore;

    public CubeTestConfig(
            IActivePivotManagerDescription activePivotManagerDescription,
            IDatastoreSchemaDescription testDatastoreSchemaDescription) {
        var application = StartBuilding.application()
                .withDatastore(testDatastoreSchemaDescription)
                .withManager(activePivotManagerDescription)
                .withoutBranchRestrictions()
                .buildAndStart();
        cubeTester = CubeTester.from(application.getManager());
        datastore = application.getDatastore();
    }

    @Bean
    public static IDatastoreSchemaDescription testDatastoreSchemaDescription() {
        final IStoreDescription tradesMergedStore = StoreDescription.builder()
                .withStoreName(TRADES_STORE_NAME)
                .withField(ASOFDATE, LOCAL_DATE)
                .asKeyField()
                .withField(TRADE_ID, STRING)
                .asKeyField()
                .withField(NOTIONAL, DOUBLE)
                .withField(TRADE_DATE, LOCAL_DATE)
                .withField(COUNTERPARTY_ID, STRING)
                .build();
        return new DatastoreSchemaDescription(List.of(tradesMergedStore), List.of());
    }

    @Bean
    public static ISelectionDescription datastoreSelectionDescription(
            IDatastoreSchemaDescription testDatastoreSchemaDescription) {
        return StartBuilding.selection(testDatastoreSchemaDescription)
                .fromBaseStore(TRADES_STORE_NAME)
                .withAllReachableFields()
                .build();
    }

    @Bean
    public static IActivePivotInstanceDescription testActivePivotInstanceDescription(
            Measures measures, Dimensions dimensions) {
        return StartBuilding.cube(CUBE_NAME)
                .withContributorsCount()
                .withinFolder(NATIVE_MEASURES)
                .withFormatter(INT_FORMATTER)
                .withUpdateTimestamp()
                .withinFolder(NATIVE_MEASURES)
                .withFormatter(TIMESTAMP_FORMATTER)
                .withCalculations(measures::build)
                .withDimensions(dimensions.build())
                .withAggregateProvider()
                .jit()
                .build();
    }

    @Bean
    public static IActivePivotManagerDescription testActivePivotManagerDescription(
            ISelectionDescription datastoreSelectionDescription,
            IActivePivotInstanceDescription testActivePivotInstanceDescription) {
        return StartBuilding.managerDescription(MANAGER_NAME)
                .withCatalog(CATALOG_NAME)
                .containingAllCubes()
                .withSchema(SCHEMA_NAME)
                .withSelection(datastoreSelectionDescription)
                .withCube(testActivePivotInstanceDescription)
                .build();
    }

    @Bean
    public IDatastore datastore() {
        return datastore;
    }

    @Bean
    public CubeTester cubeTester() {
        return cubeTester;
    }
}
