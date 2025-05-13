/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database;

import org.springframework.context.annotation.Bean;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.database.api.schema.IDatabaseSchema;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DatabaseSelectionConfig {
    private final IDatabaseSchema databaseSchema;

    /**
     * Creates the {@link ISelectionDescription} for Pivot Schema.
     *
     * @return The created selection description
     */
    @Bean
    public ISelectionDescription datastoreSelectionDescription() {
        return StartBuilding.selection(databaseSchema)
                // Holding store
                .fromBaseStore(DatastoreConstants.HoldingStore.STORE_NAME)
                .withAllFields()
                // As Of Date store
                .usingReference(DatastoreConstants.References.HOLDING_TO_ASOFDATE)
                .withAllFields()
                .except(DatastoreConstants.AsOfDateStore.Fields.AS_OF_DATE)
                // Scaled results store
                .usingReference(DatastoreConstants.References.HOLDING_TO_SCALEDSTATRESULT)
                .withAllFields()
                .except(
                        DatastoreConstants.ScaledStatResultStore.Fields.AS_OF_DATE,
                        DatastoreConstants.ScaledStatResultStore.Fields.PARTITION_KEY,
                        DatastoreConstants.ScaledStatResultStore.Fields.HOLDING_ID)
                // Holding detail
                .usingReference(DatastoreConstants.References.HOLDING_TO_HOLDINGDETAIL)
                .withAllFields()
                .except(
                        DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE,
                        DatastoreConstants.HoldingDetailStore.Fields.PARTITION_KEY,
                        DatastoreConstants.HoldingDetailStore.Fields.BASE_HOLDING_ID)
                // Security store
                .usingReference(
                        DatastoreConstants.References.HOLDING_TO_HOLDINGDETAIL,
                        DatastoreConstants.References.HOLDINGDETAIL_TO_SECURITY)
                .withAllFields()
                .except(
                        DatastoreConstants.SecurityStore.Fields.SECURITY_NAME,
                        DatastoreConstants.SecurityStore.Fields.AS_OF_DATE,
                        DatastoreConstants.SecurityStore.Fields.PARTITION_KEY,
                        DatastoreConstants.SecurityStore.Fields.FX_HEDGING)
                // Position detail
                .usingReference(
                        DatastoreConstants.References.HOLDING_TO_HOLDINGDETAIL,
                        DatastoreConstants.References.HOLDINGDETAIL_TO_POSITIONDETAIL)
                .withAllFields()
                .except(DatastoreConstants.PositionDetailStore.Fields.SECURITY_NAME)
                .build();
    }
}
