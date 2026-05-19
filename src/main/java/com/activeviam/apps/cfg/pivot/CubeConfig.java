/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CubeConfig {



    public static final String CUBE_NAME = "Cube";

    private final Measures measures;
    private final Dimensions dimensions;

    //    @Bean
    //    public IActivePivotInstanceDescription activePivotInstanceDescription() {
    //        return StartBuilding.cube(CUBE_NAME)
    //                .withContributorsCount()
    //                .withinFolder(NATIVE_MEASURES)
    //                .withAlias("Count")
    //                .withFormatter(INT_FORMATTER)
    //
    //                // WARN: This will not be available for AggregateProvider `jit`
    //                .withUpdateTimestamp()
    //                .withinFolder(NATIVE_MEASURES)
    //                .withAlias("Update.Timestamp")
    //               .withFormatter(TIMESTAMP_FORMATTER)
    //                .withCalculations(measures::build)
    //                .withDimensions(dimensions.build())
    //
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
    //    }
}
