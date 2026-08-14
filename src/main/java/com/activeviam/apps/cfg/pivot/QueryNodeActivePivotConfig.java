/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotConfig;

import lombok.RequiredArgsConstructor;

/**
 * The query node has no database, so it doesn't go through {@code ApplicationWithDatastore}/DirectQuery's
 * {@code Application}: it just builds a manager holding the distributed cube. {@code ApplicationManagerService}
 * (unconditional, always registered) inits/starts whichever {@code IActivePivotManager} bean is present once
 * the Spring context is fully up, same as for the data node.
 */
@Configuration
@Profile("query-node")
@RequiredArgsConstructor
public class QueryNodeActivePivotConfig implements IActivePivotConfig {
    private final ApplicationWithDatastore queryNodeApplication;

    @Bean
    public static ApplicationWithDatastore queryNodeApplication(IActivePivotManagerDescription managerDescription) {
        return StartBuilding.application()
                .withManager(managerDescription)
                .withoutBranchRestrictions()
                .build();
    }

    @Override
    @Bean
    public IActivePivotManager activePivotManager() {
        return queryNodeApplication.getManager();
    }
}
