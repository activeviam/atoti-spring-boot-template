/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConstants.CATALOG_NAME;
import static com.activeviam.apps.cfg.pivot.CubeConstants.MANAGER_NAME;
import static com.activeviam.apps.cfg.pivot.CubeConstants.SCHEMA_NAME;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotManagerDescriptionConfig;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile("data-node")
@RequiredArgsConstructor
public class DataNodeManagerConfig implements IActivePivotManagerDescriptionConfig {
    private final ISelectionDescription selectionDescription;
    private final IActivePivotInstanceDescription activePivotInstanceDescription;

    @Override
    @Bean
    public IActivePivotManagerDescription managerDescription() {
        return StartBuilding.managerDescription(MANAGER_NAME)
                .withCatalog(CATALOG_NAME)
                .containingAllCubes()
                .withSchema(SCHEMA_NAME)
                .withSelection(selectionDescription)
                .withCube(activePivotInstanceDescription)
                .build();
    }
}
