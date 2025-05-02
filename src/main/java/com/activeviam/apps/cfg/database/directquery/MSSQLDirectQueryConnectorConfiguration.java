/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery;

import static com.activeviam.apps.constants.PropertyConstants.DATABASE_PROPERTIES_PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.activeviam.database.mssql.api.MsSqlDatabaseSettings;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.mssql.api.MsSqlConnectorFactory;

public class MSSQLDirectQueryConnectorConfiguration {

    public static final String MSSQL_PROPERTIES_PREFIX = DATABASE_PROPERTIES_PREFIX + ".mssql";

    @Bean
    @ConfigurationProperties(prefix = MSSQL_PROPERTIES_PREFIX)
    MSSQLConfigurationProperties mssqlJdbcProperties() {
        return new MSSQLConfigurationProperties();
    }

    @Bean
    DirectQueryConnector<MsSqlDatabaseSettings> directQueryConnector(MSSQLConfigurationProperties jdbcProperties) {
        return MsSqlConnectorFactory.INSTANCE.createConnector(jdbcProperties.toClientSettings());
    }
}
