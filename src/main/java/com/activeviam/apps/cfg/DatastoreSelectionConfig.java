/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg;

import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.apps.constants.StoreAndFieldConstants;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DatastoreSelectionConfig {
    private final DatastoreSchemaConfig datastoreSchemaConfig;

    /**
     * Creates the {@link ISelectionDescription} for Pivot Schema.
     *
     * @return The created selection description
     */
    public ISelectionDescription createSchemaSelectionDescription() {
        return StartBuilding.selection(datastoreSchemaConfig.datastoreSchemaDescription())
                .fromBaseStore(StoreAndFieldConstants.TRADES_STORE_NAME)
                .withAllFields()
                .build();
    }
}
