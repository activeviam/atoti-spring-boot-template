/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.apps.cfg.datastore.DatastoreConfig;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;

@Configuration
@ComponentScan(basePackageClasses = {DatastoreConfig.class, ActivePivotConfig.class})
public class CubeTestConfig {
    private final CubeTester cubeTester;
    private final IDatastore datastore;

    public CubeTestConfig(
            IActivePivotManagerDescription activePivotManagerDescription,
            IDatastoreSchemaDescription datastoreSchemaDescription) {
        var application = StartBuilding.application()
                .withDatastore(datastoreSchemaDescription)
                .withManager(activePivotManagerDescription)
                .withoutBranchRestrictions()
                .buildAndStart();
        cubeTester = CubeTester.from(application.getManager());
        datastore = application.getDatastore();
    }

    @Bean
    public IDatastore datastore() {
        return datastore;
    }

    @Bean
    public CubeTester cubeTester() {
        return cubeTester;
    }
}
