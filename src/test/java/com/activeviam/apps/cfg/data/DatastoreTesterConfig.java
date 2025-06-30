/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.data;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.apps.cfg.database.datastore.DatastoreSchemaConfig;
import com.activeviam.apps.cfg.database.datastore.datamodel.StoreDefinitionsConfig;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.database.datastore.api.transaction.IOpenedTransaction;

@Import(value = {StoreDefinitionsConfig.class, DatastoreSchemaConfig.class})
public class DatastoreTesterConfig {

    @Bean
    public IDatastore datastore(DatastoreSchemaConfig schemaConfig) {
        var app = StartBuilding.application()
                .withDatastore(schemaConfig.datastoreSchemaDescription())
                .withManager(null)
                .withoutBranchRestrictions()
                .build();
        var datastore = app.getDatastore();
        datastore.edit(this::loadData);
        return datastore;
    }

    public void loadData(IOpenedTransaction transaction) {
        // Do nothing
    }
}
