/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.constants.CubeConstants.CATALOG_NAME;
import static com.activeviam.apps.constants.CubeConstants.MANAGER_NAME;
import static com.activeviam.apps.constants.CubeConstants.SCHEMA_NAME;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.tech.mvcc.api.policy.IEpochManagementPolicy;
import com.activeviam.tech.mvcc.api.policy.KeepLastEpochPolicy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataNodeActivePivotManagerConfig {

    private final ISelectionDescription selectionDescription;
    private final Supplier<IActivePivotInstanceDescription> activePivotInstanceDescription;

    @Bean
    public Supplier<IActivePivotManagerDescription> managerDescription() {
        return () -> StartBuilding.managerDescription(MANAGER_NAME)
                .withCatalog(CATALOG_NAME)
                .containingAllCubes()
                .withSchema(SCHEMA_NAME)
                .withSelection(selectionDescription)
                .withCube(activePivotInstanceDescription.get())
                .build();
    }

    @Bean
    IEpochManagementPolicy epochManagementPolicy() {
        return new KeepLastEpochPolicy();
    }
}
