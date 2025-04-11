/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.cfg.pivot.ActivePivotManagerConfig.INT_FORMATTER;
import static com.activeviam.apps.cfg.pivot.ActivePivotManagerConfig.NATIVE_MEASURES;
import static com.activeviam.apps.cfg.pivot.ActivePivotManagerConfig.TIMESTAMP_FORMATTER;
import static com.activeviam.apps.constants.CubeConstants.APPLICATION_NAME;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesTimeLimit;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.apps.cfg.pivot.distribution.DistributionProperties;

import lombok.RequiredArgsConstructor;

@Import({Measures.class, Dimensions.class})
@RequiredArgsConstructor
public class DataCubeConfig {

    @Bean
    public IActivePivotInstanceDescription activePivotInstanceDescription(
            Dimensions dimensions,
            Measures measures,
            @Autowired(required = false) DistributionProperties distributionProperties) {
        var builder = StartBuilding.cube(CUBE_NAME)
                .withContributorsCount()
                .withinFolder(NATIVE_MEASURES)
                .withAlias("Count")
                .withFormatter(INT_FORMATTER)

                // WARN: This will not be available for AggregateProvider `jit`
                .withUpdateTimestamp()
                .withinFolder(NATIVE_MEASURES)
                .withAlias("Update.Timestamp")
                .withFormatter(TIMESTAMP_FORMATTER)
                .withCalculations(measures::build)
                .withDimensions(dimensions.build())

                // Aggregate provider
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
                    .withNoProperty()
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
