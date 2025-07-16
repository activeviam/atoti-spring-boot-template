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
public class CubeConstants {

    public static final String CUBE_NAME = "Cube";

    public static final String APPLICATION_NAME = "AtotiApplication";

    /* *********************/
    /* OLAP Property names */
    /* *********************/
    public static final String MANAGER_NAME = "Manager";
    public static final String CATALOG_NAME = "Catalog";
    public static final String SCHEMA_NAME = "Schema";

    /* ********** */
    /* Formatters */
    /* ********** */
    public static final String FORMATTER_STRING = "#0.00";
    public static final String DOUBLE_FORMATTER = "DOUBLE[#0.00]";
    public static final String INT_FORMATTER = "INT[#0]";
    public static final String TIMESTAMP_FORMATTER = "DATE[HH:mm:ss]";

    public static final String NATIVE_MEASURES = "Native Measures";
}
