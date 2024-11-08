/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.datastore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
import com.activeviam.activepivot.server.spring.api.config.IDatastoreConfig;
import com.activeviam.database.datastore.api.IDatastore;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DatastoreConfig implements IDatastoreConfig {
    private final ApplicationWithDatastore applicationWithDatastore;

    @Bean
    @Override
    public IDatastore database() {
        return applicationWithDatastore.getDatastore();
    }
}
