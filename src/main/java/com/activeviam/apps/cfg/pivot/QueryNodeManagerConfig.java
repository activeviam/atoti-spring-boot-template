/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConstants.CATALOG_NAME;
import static com.activeviam.apps.cfg.pivot.CubeConstants.MANAGER_NAME;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.IDistributedActivePivotInstanceDescription;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotManagerDescriptionConfig;

import lombok.RequiredArgsConstructor;

/**
 * The query node has no database of its own: its manager only holds the distributed (query) cube, which
 * forwards queries to the data node(s) in the cluster and merges the results.
 */
@Configuration
@Profile("query-node")
@RequiredArgsConstructor
public class QueryNodeManagerConfig implements IActivePivotManagerDescriptionConfig {
    private final IDistributedActivePivotInstanceDescription distributedActivePivotInstanceDescription;

    @Override
    @Bean
    public IActivePivotManagerDescription managerDescription() {
        return StartBuilding.managerDescription(MANAGER_NAME)
                .withCatalog(CATALOG_NAME)
                .containingAllCubes()
                .withDistributedCube(distributedActivePivotInstanceDescription)
                .build();
    }
}
