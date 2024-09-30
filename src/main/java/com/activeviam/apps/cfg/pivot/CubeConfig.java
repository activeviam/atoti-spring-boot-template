/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.PivotManagerConfig.INT_FORMATTER;
import static com.activeviam.apps.cfg.pivot.PivotManagerConfig.NATIVE_MEASURES;
import static com.activeviam.apps.cfg.pivot.PivotManagerConfig.TIMESTAMP_FORMATTER;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesTimeLimit;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CubeConfig {
    public static final String CUBE_NAME = "Cube";

    private final DimensionConfig dimensionConfig;
    private final MeasureConfig measureConfig;

    public IActivePivotInstanceDescription createCubeDescription() {
        return StartBuilding.cube(CUBE_NAME)
                .withContributorsCount()
                .withinFolder(NATIVE_MEASURES)
                .withAlias("Count")
                .withFormatter(INT_FORMATTER)

                // WARN: This will not be available for AggregateProvider `jit`
                .withUpdateTimestamp()
                .withinFolder(NATIVE_MEASURES)
                .withAlias("Update.Timestamp")
                .withFormatter(TIMESTAMP_FORMATTER)
                .withCalculations(measureConfig::build)
                .withDimensions(dimensionConfig::build)

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
                .end()
                .build();
    }
}
