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

import com.activeviam.database.clickhouse.api.ClickhouseDatabaseSettings;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.clickhouse.api.ClickhouseConnectorFactory;

public class ClickHouseDirectQueryConnectorConfiguration {

    public static final String CLICKHOUSE_PROPERTIES_PREFIX = DATABASE_PROPERTIES_PREFIX + ".clickhouse";

    @Bean
    @ConfigurationProperties(prefix = CLICKHOUSE_PROPERTIES_PREFIX)
    ClickHouseConfigurationProperties clickhouseProperties() {
        return new ClickHouseConfigurationProperties();
    }

    @Bean
    DirectQueryConnector<ClickhouseDatabaseSettings> directQueryConnector(
            ClickHouseConfigurationProperties clickhouseProperties) {
        return ClickhouseConnectorFactory.INSTANCE.createConnector(clickhouseProperties.toClientSettings());
    }
}
