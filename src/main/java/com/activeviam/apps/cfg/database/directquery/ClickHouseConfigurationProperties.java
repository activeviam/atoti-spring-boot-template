/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery;

import com.activeviam.database.clickhouse.api.ClickhouseProperties;
import com.activeviam.directquery.clickhouse.api.ClickhouseClientSettings;
import com.clickhouse.client.ClickHouseProtocol;

import lombok.Data;

@Data
public class ClickHouseConfigurationProperties {
    private String username;
    private String password;
    private String host;
    private int port;
    private String database;
    private ClickHouseProtocol protocol;

    public ClickhouseClientSettings toClientSettings() {
        var properties = ClickhouseProperties.builder()
                .host(host)
                .protocol(protocol)
                .port(port)
                .database(database)
                .userName(username)
                .password(password)
                .build();
        return ClickhouseClientSettings.builder().properties(properties).build();
    }
}
