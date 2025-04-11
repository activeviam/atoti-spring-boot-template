/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.querynode;

import static com.activeviam.apps.cfg.pivot.ActivePivotManagerConfig.INT_FORMATTER;
import static com.activeviam.apps.cfg.pivot.ActivePivotManagerConfig.NATIVE_MEASURES;
import static com.activeviam.apps.cfg.pivot.ActivePivotManagerConfig.TIMESTAMP_FORMATTER;
import static com.activeviam.apps.constants.CubeConstants.APPLICATION_NAME;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesTimeLimit;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IDistributedActivePivotInstanceDescription;
import com.activeviam.apps.cfg.pivot.distribution.DistributionProperties;

public class QueryCubeConfig {

    @Bean
    public IDistributedActivePivotInstanceDescription activePivotInstanceDescription(
            DistributionProperties distributionProperties) {
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
                .asQueryCube()
                .withClusterDefinition()
                .withClusterId(distributionProperties.getClusterId())
                .withMessengerDefinition()
                .withNettyMessenger()
                .withNoProperty()
                .withProtocolPath(distributionProperties.getProtocolPath())
                .end()
                .withApplication(APPLICATION_NAME)
                .withDistributingLevels(LevelIdentifier.simple(ASOFDATE))
                .end()

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
