/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotConfig;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotManagerDescriptionConfig;
import com.activeviam.activepivot.server.spring.api.config.IDatastoreConfig;
import com.activeviam.activepivot.server.spring.api.config.IDatastoreSchemaDescriptionConfig;
import com.activeviam.database.datastore.api.IDatastore;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ActivePivotWithDatastoreConfig implements IDatastoreConfig, IActivePivotConfig {
    private final IActivePivotManagerDescriptionConfig apManagerConfig;
    private final IDatastoreSchemaDescriptionConfig datastoreDescriptionConfig;

    @Bean
    protected ApplicationWithDatastore applicationWithDatastore() {
        return StartBuilding.application()
                .withDatastore(datastoreDescriptionConfig.datastoreSchemaDescription())
                .withManager(apManagerConfig.managerDescription())
                .withEpochPolicy(apManagerConfig.epochManagementPolicy())
                .withoutBranchRestrictions()
                .build();
    }

    @Bean
    @Override
    public IActivePivotManager activePivotManager() {
        return applicationWithDatastore().getManager();
    }

    @Bean
    @Override
    public IDatastore database() {
        return applicationWithDatastore().getDatastore();
    }
}
