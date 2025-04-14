/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database;

import static com.activeviam.apps.constants.PropertyConstants.DATABASE_PROPERTIES_PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;

public class DatabaseConfig {

    @ConfigurationProperties(prefix = DATABASE_PROPERTIES_PREFIX)
    DatabaseProperties databaseProperties() {
        return new DatabaseProperties();
    }
}
