/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ActivePivotConfig {
    private final ApplicationWithDatastore applicationWithDatastore;

    @Bean
    public IActivePivotManager activePivotManager() {
        return applicationWithDatastore.getManager();
    }
}
