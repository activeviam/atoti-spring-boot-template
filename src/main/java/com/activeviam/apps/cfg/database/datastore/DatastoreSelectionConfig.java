/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.datastore;

import org.springframework.context.annotation.Bean;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.database.datastore.api.description.IDatastoreSchemaDescription;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DatastoreSelectionConfig {
    private final IDatastoreSchemaDescription datastoreSchemaDescription;

    /**
     * Creates the {@link ISelectionDescription} for Pivot Schema.
     *
     * @return The created selection description
     */
    @Bean
    public ISelectionDescription datastoreSelectionDescription() {
        return StartBuilding.selection(datastoreSchemaDescription)
                .fromBaseStore(StoreAndFieldConstants.TRADES_STORE_NAME)
                //                .withAllFields()
                //                .usingReference(referenceName(TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME))
                //                .withFields(COUNTERPARTY_ID)
                .withAllReachableFields()
                .build();
    }
}
