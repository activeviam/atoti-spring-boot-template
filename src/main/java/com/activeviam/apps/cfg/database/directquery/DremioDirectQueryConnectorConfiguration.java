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

import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.database.jdbc.api.SqlDialect;
import com.activeviam.database.jdbc.dialect.dremio.internal.DremioSqlDialect;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.jdbc.api.GenericJdbcClientSettings;
import com.activeviam.directquery.jdbc.api.GenericJdbcConnectorMetaFactory;

public class DremioDirectQueryConnectorConfiguration {

    public static final String DREMIO_PROPERTIES_PREFIX = DATABASE_PROPERTIES_PREFIX + ".dremio";

    @Bean
    @ConfigurationProperties(prefix = DREMIO_PROPERTIES_PREFIX)
    DremioConfigurationProperties dremioJdbcProperties() {
        return new DremioConfigurationProperties();
    }

    @Bean
    SqlDialect dremioSqlDialect() {
        return DremioSqlDialect.sqlDialect();
    }

    @Bean
    DirectQueryConnector<GenericJdbcDatabaseSettings> directQueryConnector(
            SqlDialect sqlDialect, DremioConfigurationProperties jdbcProperties) {
        return GenericJdbcConnectorMetaFactory.createConnectorFactory(sqlDialect)
                .createConnector(GenericJdbcClientSettings.builder()
                        .properties(jdbcProperties.toProperties())
                        .build());
    }
}
