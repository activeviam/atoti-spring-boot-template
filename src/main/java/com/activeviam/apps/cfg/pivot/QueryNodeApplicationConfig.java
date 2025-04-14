/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import java.util.Collections;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotConfig;
import com.activeviam.activepivot.server.spring.api.config.IDatastoreConfig;
import com.activeviam.apps.annotations.ConditionalOnQueryNode;
import com.activeviam.apps.cfg.pivot.querynode.QueryCubeConfig;
import com.activeviam.apps.cfg.pivot.querynode.QueryNodeActivePivotManagerConfig;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.database.datastore.api.description.impl.DatastoreSchemaDescription;
import com.activeviam.tech.core.api.agent.AgentException;
import com.activeviam.tech.mvcc.api.policy.IEpochManagementPolicy;
import com.activeviam.tech.mvcc.api.security.impl.BranchPermissionsManager;

@ConditionalOnQueryNode
@Configuration
@Import({QueryNodeActivePivotManagerConfig.class, QueryCubeConfig.class})
public class QueryNodeApplicationConfig implements IActivePivotConfig, IDatastoreConfig {
    private final IActivePivotManagerDescription activePivotManagerDescription;
    private final IEpochManagementPolicy epochManagementPolicy;
    private final BranchPermissionsManager branchPermissionsManager;

    public QueryNodeApplicationConfig(
            IActivePivotManagerDescription activePivotManagerDescription,
            IEpochManagementPolicy epochManagementPolicy,
            @Autowired(required = false) BranchPermissionsManager branchPermissionsManager) {
        this.activePivotManagerDescription = activePivotManagerDescription;
        this.epochManagementPolicy = epochManagementPolicy;
        this.branchPermissionsManager = branchPermissionsManager;
    }

    @Bean
    public ApplicationWithDatastore applicationWithDatastore() {
        var builder = StartBuilding.application()
                .withDatastore(new DatastoreSchemaDescription(Collections.emptyList(), Collections.emptyList()))
                .withManager(activePivotManagerDescription)
                .withEpochPolicy(epochManagementPolicy);
        if (Objects.nonNull(branchPermissionsManager)) {
            return builder.withBranchPermissionsManager(branchPermissionsManager)
                    .build();
        } else {
            return builder.withoutBranchRestrictions().build();
        }
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
    /**
     * Initialize and start the ActivePivot Manager, after performing all the injections into the ActivePivot plug-ins.
     *
     * @throws AgentException any exception that occurred during the injection, the initialization or the starting
     */
    @EventListener(ApplicationStartedEvent.class)
    public void startManager() throws AgentException {
        /* *********************************************** */
        /* Initialize the ActivePivot Manager and start it */
        /* *********************************************** */
        activePivotManager().init(null);
        activePivotManager().start();
    }
}
