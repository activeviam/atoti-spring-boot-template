/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertyConstants {
    public static final String DATABASE_PROPERTIES_PREFIX = "database";

    public static final String TYPE_PROPERTY = "type";

    public static final String DATABASE_TYPE_DATASTORE = "datastore";
    public static final String DATABASE_TYPE_DREMIO = "dremio";

    public static final String DISTRIBUTION_PROPERTIES_PREFIX = "distribution";
    public static final String DISTRIBUTION_TYPE_DATA = "data";
    public static final String DISTRIBUTION_TYPE_QUERY = "query";
    public static final String DISTRIBUTION_TYPE_NONE = "none";
}
