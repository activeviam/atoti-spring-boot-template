/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.querynode;

import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.COB_DATE_LEVEL;
import static com.activeviam.apps.constants.CubeConstants.APPLICATION_NAME;
import static com.activeviam.apps.constants.CubeConstants.CATALOG_NAME;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.CubeConstants.MANAGER_NAME;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesResultLimit;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesTimeLimit;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.IDistributedActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IQueryClusterDefinition;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotManagerDescriptionConfig;
import com.activeviam.apps.cfg.pivot.distribution.DistributionProperties;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryNodeActivePivotManagerConfig implements IActivePivotManagerDescriptionConfig {
    public static final LevelIdentifier DISTRIBUTING_LEVEL = COB_DATE_LEVEL;

    private final DistributionProperties distributionProperties;

    @Override
    @Bean
    public IActivePivotManagerDescription managerDescription() {
        return StartBuilding.managerDescription(MANAGER_NAME)
                .withCatalog(CATALOG_NAME)
                .containingAllCubes()
                .withDistributedCube(activePivotInstanceDescription())
                .build();
    }

    private IDistributedActivePivotInstanceDescription activePivotInstanceDescription() {
        return StartBuilding.cube(CUBE_NAME)
                .asQueryCube()
                .withClusterDefinition()
                .withClusterId(distributionProperties.getClusterId())
                .withMessengerDefinition()
                .withNettyMessenger()
                .withNoProperty()
                .withProtocolPath(distributionProperties.getProtocolPath())
                .end()
                .withApplication(APPLICATION_NAME)
                .withDistributingLevels(DISTRIBUTING_LEVEL)
                .withProperty(
                        IQueryClusterDefinition.HORIZONTAL_DATA_DUPLICATION_PROPERTY,
                        Boolean.toString(true)) // enable data node duplication
                .end()
                // Shared context values
                // Query maximum execution time (before timeout cancellation): 30s
                .withSharedContextValue(QueriesTimeLimit.of(30, TimeUnit.SECONDS))
                .withSharedContextValue(QueriesResultLimit.withoutLimit())
                .withSharedMdxContext()
                .aggressiveFormulaEvaluation(true)
                .end()
                .withSharedDrillthroughProperties()
                .withMaxRows(10_000)
                .end()
                .build();
    }
}
