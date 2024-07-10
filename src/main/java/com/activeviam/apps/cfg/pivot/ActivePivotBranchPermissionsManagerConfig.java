/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.server.impl.api.security.ContentServiceBranchPermissionsManagerBuilder;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotBranchPermissionsManagerConfig;
import com.activeviam.tech.contentserver.spring.api.config.IContentServiceConfig;
import com.activeviam.tech.mvcc.api.security.IBranchPermissionsManager;
import com.activeviam.tech.mvcc.api.security.impl.CachedBranchPermissionsManager;

import lombok.RequiredArgsConstructor;

/**
 * Default configuration class creating the manager of branch permissions.
 *
 * @author ActiveViam
 */
@Configuration
@RequiredArgsConstructor
public class ActivePivotBranchPermissionsManagerConfig
        implements IActivePivotBranchPermissionsManagerConfig {

    private final IContentServiceConfig contentServiceConfig;

    @Override
    @Bean
    public IBranchPermissionsManager branchPermissionsManager() {
        return new CachedBranchPermissionsManager(ContentServiceBranchPermissionsManagerBuilder.create().contentService(this.contentServiceConfig.contentService()).build());
    }
}