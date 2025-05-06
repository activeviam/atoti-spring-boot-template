/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.constants.CubeConstants.APPLICATION_NAME;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesTimeLimit;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IMessengerDefinition;
import com.activeviam.apps.cfg.pivot.distribution.DistributionProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Import({Measures.class, Dimensions.class})
@RequiredArgsConstructor
@Slf4j
public class DataCubeConfig {

    @Bean
    public IActivePivotInstanceDescription activePivotInstanceDescription(
            Dimensions dimensions,
            Measures calculations,
            @Autowired(required = false) DistributionProperties distributionProperties) {
        var builder = StartBuilding.cube(CUBE_NAME)
                .withCalculations(calculations)
                .withDimensions(dimensions)
                .withAggregateProvider()
                .jit()
                // Shared context values
                // Query maximum execution time (before timeout cancellation): 30s
                .withSharedContextValue(QueriesTimeLimit.of(30, TimeUnit.SECONDS))
                .withSharedMdxContext()
                .aggressiveFormulaEvaluation(true)
                .end()
                .withSharedDrillthroughProperties()
                .withMaxRows(10_000)
                .end();

        if (distributionProperties != null) {
            return builder.asDataCube()
                    .withClusterDefinition()
                    .withClusterId(distributionProperties.getClusterId())
                    .withMessengerDefinition()
                    .withNettyMessenger()
                    // Dont connect to the cluster at startup. We do this after we load the data
                    .withProperty(IMessengerDefinition.AUTO_START, Boolean.FALSE.toString())
                    .end()
                    .withProtocolPath(distributionProperties.getProtocolPath())
                    .end()
                    .withApplicationId(APPLICATION_NAME)
                    .withAllHierarchies()
                    .withAllMeasures()
                    .end()
                    .build();
        } else {
            return builder.build();
        }
    }
}
