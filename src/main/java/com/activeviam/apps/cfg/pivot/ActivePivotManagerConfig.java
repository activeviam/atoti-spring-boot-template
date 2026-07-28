/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.tech.mvcc.api.policy.IEpochManagementPolicy;
import com.activeviam.tech.mvcc.api.policy.KeepLastEpochPolicy;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ActivePivotManagerConfig {
    /* *********************/
    /* OLAP Property names */
    /* *********************/
    public static final String MANAGER_NAME = "Manager";
    public static final String CATALOG_NAME = "Catalog";
    public static final String SCHEMA_NAME = "Schema";

    /* ********** */
    /* Formatters */
    /* ********** */
    public static final String DOUBLE_FORMATTER = "DOUBLE[#,###.##]";
    public static final String INT_FORMATTER = "INT[#,###]";
    public static final String TIMESTAMP_FORMATTER = "DATE[HH:mm:ss]";

    public static final String NATIVE_MEASURES = "Native Measures";

    private final ISelectionDescription selectionDescription;
    private final IActivePivotInstanceDescription activePivotInstanceDescription;

    @Bean
    public IActivePivotManagerDescription managerDescription() {
        return StartBuilding.managerDescription(MANAGER_NAME)
                .withCatalog(CATALOG_NAME)
                .containingAllCubes()
                .withSchema(SCHEMA_NAME)
                .withSelection(selectionDescription)
                .withCube(activePivotInstanceDescription)
                .build();
    }

    @Bean
    public IEpochManagementPolicy epochPolicy() {
        return new KeepLastEpochPolicy();
    }
}
