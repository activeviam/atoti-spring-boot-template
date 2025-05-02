/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database;

import static com.activeviam.apps.constants.PropertyConstants.AS_OF_DATES_PROPERTIES_PREFIX;
import static com.activeviam.apps.constants.PropertyConstants.DATABASE_PROPERTIES_PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.activeviam.apps.cfg.source.AsOfDateProperties;

public class DatabaseConfig {

    @ConfigurationProperties(prefix = DATABASE_PROPERTIES_PREFIX)
    @Bean
    DatabaseProperties databaseProperties() {
        return new DatabaseProperties();
    }

    @ConfigurationProperties(prefix = AS_OF_DATES_PROPERTIES_PREFIX)
    @Bean
    AsOfDateProperties asOfDateDateProperties() {
        return new AsOfDateProperties();
    }
}
