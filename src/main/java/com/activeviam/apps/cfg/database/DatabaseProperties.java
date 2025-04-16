/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database;

import static com.activeviam.apps.constants.PropertyConstants.DATABASE_TYPE_DATASTORE;
import static com.activeviam.apps.constants.PropertyConstants.DATABASE_TYPE_DREMIO;

import java.util.Objects;

import lombok.Data;

@Data
public class DatabaseProperties {
    private String type;

    public boolean isDatastoreType() {
        return Objects.isNull(type) || DATABASE_TYPE_DATASTORE.equals(type);
    }

    public boolean isDremioType() {
        return Objects.nonNull(type) && DATABASE_TYPE_DREMIO.equals(type);
    }
}
