/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps;

import static com.activeviam.apps.cfg.pivot.CubeConfig.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;

import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.impl.avinternal.foundry.core.configurator.ICubeManagerConfigurator;
import com.activeviam.activepivot.server.spring.avinternal.foundry.config.IModelConfigurer;
import com.activeviam.apps.cfg.datastore.datamodel.StoresConfiguration;
import com.activeviam.apps.cfg.pivot.Dimensions;
import com.activeviam.apps.cfg.pivot.Measures;
import com.activeviam.database.api.schema.IDatabaseSchema;
import com.activeviam.database.datastore.avinternal.foundry.core.configurator.IDatabaseConfigurator;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ModelConfigurer implements IModelConfigurer {

    private final Dimensions dimensions = new Dimensions();
    private final Measures measures = new Measures();

    @Override
    public void configureDatabase(IDatabaseConfigurator databaseConfigurator) {
        databaseConfigurator.addTable(StoresConfiguration.createTradesStoreDescription());
        databaseConfigurator.addTable(StoresConfiguration.createTradeAttributesStoreDescription());
        databaseConfigurator.addJoin(StoresConfiguration.tradeToAttributedReference());
    }

    @Override
    public void configureCubes(ICubeManagerConfigurator cubeManagerConfigurator, IDatabaseSchema databaseSchema) {
        var cube = cubeManagerConfigurator.addCube(CUBE_NAME, TRADES_STORE_NAME);
        dimensions.accept(cube.dimensions());
        measures.accept(cube.measures());
        // TODO?
        //                // Aggregate provider
        //                .withAggregateProvider()
        //                .jit()
        //
        //                // Shared context values
        //                // Query maximum execution time (before timeout cancellation): 30s
        //                .withSharedContextValue(QueriesTimeLimit.of(30, TimeUnit.SECONDS))
        //                .withSharedMdxContext()
        //                .aggressiveFormulaEvaluation(true)
        //                .end()
        //                .withSharedDrillthroughProperties()
        //                .withMaxRows(10_000)
        //                .end()
        //                .build();
    }
}
