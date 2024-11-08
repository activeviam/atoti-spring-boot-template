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
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;
import com.activeviam.tech.mvcc.api.policy.IEpochManagementPolicy;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ActivePivotWithDatastoreConfig {
    private final IDatastoreSchemaDescription datastoreSchemaDescription;
    private final IActivePivotManagerDescription activePivotManagerDescription;
    private final IEpochManagementPolicy epochManagementPolicy;

    @Bean
    public ApplicationWithDatastore applicationWithDatastore() {
        return StartBuilding.application()
                .withDatastore(datastoreSchemaDescription)
                .withManager(activePivotManagerDescription)
                .withEpochPolicy(epochManagementPolicy)
                .withoutBranchRestrictions()
                .build();
    }
}
