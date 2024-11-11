/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.apps.cfg.datastore.DatastoreSchemaConfig;
import com.activeviam.apps.cfg.datastore.DatastoreSelectionConfig;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.datastore.api.IDatastore;

@Configuration
@Import(Measures.class)
public class CubeTestConfig {
    public static final String CUBE_NAME = "Cube";

    private final CubeTester cubeTester;
    private final IDatastore datastore;

    public CubeTestConfig() {
        var datastoreDescConfig = new DatastoreSchemaConfig();
        var datastoreSelectionDesc = new DatastoreSelectionConfig(datastoreDescConfig.datastoreSchemaDescription());
        var measures = new Measures();
        var dimensions = new Dimensions();
        var cubeDescription = StartBuilding.cube()
                .withName(CUBE_NAME)
                .withCalculations(measures::build)
                .withDimensions(dimensions::build)
                .build();

        var managerDescription = StartBuilding.managerDescription()
                .withSchema()
                .withSelection(datastoreSelectionDesc.datastoreSelectionDescription())
                .withCube(cubeDescription)
                .build();

        var applicationBuilder = StartBuilding.application()
                .withDatastore(datastoreDescConfig.datastoreSchemaDescription())
                .withManager(managerDescription)
                .withoutBranchRestrictions();

        var application = applicationBuilder.buildAndStart();
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
