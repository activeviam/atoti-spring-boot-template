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

import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.database.jdbc.api.GenericJdbcProperties;
import com.activeviam.database.jdbc.api.SqlDialect;
import com.activeviam.database.jdbc.dialect.dremio.internal.DremioSqlDialect;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.jdbc.api.GenericJdbcClientSettings;
import com.activeviam.directquery.jdbc.api.GenericJdbcConnectorMetaFactory;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile("data-node")
@RequiredArgsConstructor
public class DremioConnectorConfig {
    private final DremioProperties dremioProperties;

    @Bean
    public DirectQueryConnector<GenericJdbcDatabaseSettings> dremioConnector() {
        final SqlDialect dialect = DremioSqlDialect.sqlDialect();

        final String connectionString =
                String.format("jdbc:arrow-flight-sql://%s:%d", dremioProperties.getHost(), dremioProperties.getPort());
        final GenericJdbcProperties properties = GenericJdbcProperties.builder()
                .connectionString(connectionString)
                .additionalOption("user", dremioProperties.getUsername())
                .additionalOption("password", dremioProperties.getPassword())
                .additionalOption("useEncryption", String.valueOf(dremioProperties.isUseEncryption()))
                .build();
        final GenericJdbcClientSettings clientSettings =
                GenericJdbcClientSettings.builder().properties(properties).build();

        return GenericJdbcConnectorMetaFactory.createConnectorFactory(dialect).createConnector(clientSettings);
    }
}
