/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.source.InitialLoad.startDistributionMessenger;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotConfig;
import com.activeviam.activepivot.server.spring.private_.config.IDatabaseConfig;
import com.activeviam.apps.annotations.ConditionalOnApplicationWithDirectQuery;
import com.activeviam.apps.cfg.database.DatabaseConfig;
import com.activeviam.apps.cfg.database.directquery.DirectQueryConfig;
import com.activeviam.apps.cfg.pivot.datanode.DataCubeConfig;
import com.activeviam.apps.cfg.pivot.datanode.DataNodeActivePivotManagerConfig;
import com.activeviam.database.api.IDatabase;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.schema.SchemaDescription;
import com.activeviam.directquery.application.api.Application;
import com.activeviam.tech.core.api.agent.AgentException;
import com.activeviam.tech.mvcc.api.policy.IEpochManagementPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnApplicationWithDirectQuery
@Configuration
@Import({DatabaseConfig.class, DirectQueryConfig.class, DataNodeActivePivotManagerConfig.class, DataCubeConfig.class})
@RequiredArgsConstructor
@Slf4j
public class ApplicationWithDirectQueryConfig implements IActivePivotConfig, IDatabaseConfig {

    static {
        System.setProperty(
                "activeviam.feature.experimental.dynamic_aggregate_provider.enabled", Boolean.TRUE.toString());
    }

    private final SchemaDescription schemaDescription;
    private final IActivePivotManagerDescription activePivotManagerDescription;
    private final IEpochManagementPolicy epochManagementPolicy;
    private final DirectQueryConnector<GenericJdbcDatabaseSettings> directQueryConnector;
    private final GenericJdbcDatabaseSettings databaseSettings;

    @Bean
    Application applicationWithDirectQuery() {
        return Application.builder(directQueryConnector)
                .managerDescription(activePivotManagerDescription)
                .databaseSettings(databaseSettings)
                .schema(schemaDescription)
                .epochPolicy(epochManagementPolicy)
                .build();
    }

    @Bean
    @Override
    public IActivePivotManager activePivotManager() {
        return applicationWithDirectQuery().getManager();
    }

    @Bean
    @Override
    public IDatabase database() {
        return applicationWithDirectQuery().getDatabase();
    }

    @Bean
    public IDatastore datastore() {
        return applicationWithDirectQuery().getDatabase().getInMemoryDatastore();
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
        var app = applicationWithDirectQuery();
        app.start();
        startDistributionMessenger(app.getManager());
    }
}
