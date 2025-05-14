/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database;

import static com.activeviam.apps.constants.PropertyConstants.DATABASE_TYPE_CLICKHOUSE;
import static com.activeviam.apps.constants.PropertyConstants.DATABASE_TYPE_DATASTORE;

import java.util.Objects;

import lombok.Data;

@Data
public class DatabaseProperties {
    private String type;

    private InMemoryStoresProperties inMemoryStores = new InMemoryStoresProperties();

    public boolean isDatastoreType() {
        return Objects.isNull(type) || DATABASE_TYPE_DATASTORE.equals(type);
    }

    public boolean isMSSQLType() {
        return Objects.nonNull(type) && DATABASE_TYPE_CLICKHOUSE.equals(type);
    }

    @Data
    public static class InMemoryStoresProperties {
        private boolean partitionAsOfDate = true;

        private int statsVectorSize = 3;
        private int simVectorSize = 1000;
    }
}
