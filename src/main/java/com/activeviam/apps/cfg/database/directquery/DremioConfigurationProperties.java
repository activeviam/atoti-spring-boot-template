/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery;

import java.util.HashMap;
import java.util.Map;

import com.activeviam.database.jdbc.api.GenericJdbcProperties;

import lombok.Data;

@Data
public class DremioConfigurationProperties {
    private String connectionString;
    private String database;
    private String schema;
    private Map<String, String> jdbcProperties = new HashMap<>();

    public GenericJdbcProperties toProperties() {
        var builder = GenericJdbcProperties.builder().connectionString(connectionString);
        if (database != null) {
            builder = builder.additionalOption("database", database);
        }
        if (schema != null) {
            builder = builder.additionalOption("schema", schema);
        }
        return builder.additionalOptions(jdbcProperties).build();
    }
}
