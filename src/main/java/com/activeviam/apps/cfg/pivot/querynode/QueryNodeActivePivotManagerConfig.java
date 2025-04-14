/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.querynode;

import static com.activeviam.apps.constants.CubeConstants.CATALOG_NAME;
import static com.activeviam.apps.constants.CubeConstants.MANAGER_NAME;

import org.springframework.context.annotation.Bean;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.IDistributedActivePivotInstanceDescription;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotManagerDescriptionConfig;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryNodeActivePivotManagerConfig implements IActivePivotManagerDescriptionConfig {

    private final IDistributedActivePivotInstanceDescription activePivotInstanceDescription;

    @Override
    @Bean
    public IActivePivotManagerDescription managerDescription() {
        return StartBuilding.managerDescription(MANAGER_NAME)
                .withCatalog(CATALOG_NAME)
                .containingAllCubes()
                .withDistributedCube(activePivotInstanceDescription)
                .build();
    }
}
