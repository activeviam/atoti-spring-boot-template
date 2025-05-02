/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery;

import com.activeviam.directquery.mssql.api.MsSqlClientSettings;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import lombok.Data;

@Data
public class MSSQLConfigurationProperties {
    private String username;
    private String password;
    private int port;
    private String database;
    private String schema;
    private boolean useEncryption = true;
    private boolean trustServerCertificate = true;
    private boolean sendTimeAsDatetime;
    private int connectRetryCount = 3;
    private int connectRetryInterval = 30;

    public MsSqlClientSettings toClientSettings() {
        var dataSource = new SQLServerDataSource();
        dataSource.setUser(username);
        dataSource.setPassword(password);
        // Set up the database to connect to
        dataSource.setPortNumber(port);
        dataSource.setDatabaseName(database);
        // Setup connection security parameters
        dataSource.setEncrypt(useEncryption);
        dataSource.setTrustServerCertificate(trustServerCertificate);
        dataSource.setSendTimeAsDatetime(sendTimeAsDatetime);
        // Setup number of connection attempts (first tries may fail when pool needs to warm up)
        dataSource.setConnectRetryCount(connectRetryCount);
        dataSource.setConnectRetryInterval(connectRetryInterval);
        return MsSqlClientSettings.builder().dataSource(dataSource).build();
    }
}
