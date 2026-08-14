/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.directquery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.schema.SchemaDescription;
import com.activeviam.directquery.application.api.Application;
import com.activeviam.directquery.application.api.spring.ADirectQueryApplicationConfig;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile("data-node")
@RequiredArgsConstructor
public class DirectQueryApplicationConfig extends ADirectQueryApplicationConfig {
    private final DirectQueryConnector<GenericJdbcDatabaseSettings> dremioConnector;
    private final SchemaDescription dremioSchemaDescription;
    private final IActivePivotManagerDescription activePivotManagerDescription;

    @Override
    @Bean
    public Application directQueryApplication() {
        return Application.builder(dremioConnector)
                .schema(dremioSchemaDescription)
                .managerDescription(activePivotManagerDescription)
                .build();
    }
}
