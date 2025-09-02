/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import java.util.function.Supplier;

import com.activeviam.activepivot.core.impl.api.cube.CubeFeedingPromise;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.IDirectQueryDatabase;
import com.activeviam.directquery.api.schema.SchemaDescription;
import com.activeviam.directquery.application.api.Application;
import com.activeviam.directquery.application.api.IApplication;
import com.activeviam.directquery.application.api.IRefreshable;
import com.activeviam.directquery.application.api.refresh.ChangeDescription;
import com.activeviam.tech.mvcc.api.policy.IEpochManagementPolicy;
import com.activeviam.tech.mvcc.api.security.IBranchPermissionsManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DelegatingApplication implements IApplication, IRefreshable {
    private final SchemaDescription schemaDescription;
    private final Supplier<IActivePivotManagerDescription> activePivotManagerDescription;
    private final IEpochManagementPolicy epochManagementPolicy;
    private final DirectQueryConnector<GenericJdbcDatabaseSettings> directQueryConnector;
    private final GenericJdbcDatabaseSettings databaseSettings;

    private Application application;

    private Application newApplicationInstace() {
        return Application.builder(directQueryConnector)
                .managerDescription(activePivotManagerDescription.get())
                .databaseSettings(databaseSettings)
                .schema(schemaDescription)
                .epochPolicy(epochManagementPolicy)
                .build();
    }

    @Override
    public void start() {
        if (application == null) {
            application = newApplicationInstace();
        }
        application.start();
    }

    @Override
    public CubeFeedingPromise startAsync() {
        return application.startAsync();
    }

    @Override
    public IActivePivotManager getManager() {
        return application.getManager();
    }

    @Override
    public IBranchPermissionsManager getBranchPermissionManager() {
        return application.getBranchPermissionManager();
    }

    @Override
    public IDirectQueryDatabase getDatabase() {
        return application.getDatabase();
    }

    @Override
    public void refresh() {
        application.refresh();
    }

    @Override
    public void refresh(ChangeDescription changeDescription) {
        application.refresh(changeDescription);
    }

    @Override
    public void restart() {
        if (application != null) {
            try {
                log.info("Closing the current application...");
                application.close();
                application = null;
            } catch (Exception e) {
                log.error("Error while refreshing application", e);
            }
        }
        log.info("Starting the application with a new configuration...");
        start();
    }

    @Override
    public void close() throws Exception {
        application.close();
    }
}
