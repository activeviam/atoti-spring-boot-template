/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery;

import static com.activeviam.apps.cfg.database.directquery.datamodel.TableDefinitionsConfig.SCALED_VECTORS_TABLE;
import static com.activeviam.apps.cfg.database.directquery.datamodel.TableDefinitionsConfig.SCALED_VECTORS_TO_HOLDING;

import java.util.List;

import org.springframework.context.annotation.Bean;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.directquery.api.schema.ATableDescription;
import com.activeviam.directquery.api.schema.JoinDescription;
import com.activeviam.directquery.api.schema.SchemaDescription;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DirectQuerySchemaConfig {
    private final List<IStoreDescription> inMemoryStoreDescriptions;
    private final List<IReferenceDescription> inMemoryReferenceDescriptions;

    private final List<ATableDescription> tableDescriptions;
    private final List<JoinDescription> joinDescriptions;

    @Bean
    public SchemaDescription schemaDescription() {
        return SchemaDescription.builder()
                .externalTables(tableDescriptions)
                .externalJoins(joinDescriptions)
                .inMemoryTables(inMemoryStoreDescriptions)
                .inMemoryJoins(inMemoryReferenceDescriptions)
                .build();
    }

    /**
     * Creates the {@link ISelectionDescription} for Pivot Schema.
     *
     * @return The created selection description
     */
    @Bean
    public ISelectionDescription databaseSelectionDescription() {
        return StartBuilding.selection(schemaDescription())
                // Holding store
                .fromBaseStore(SCALED_VECTORS_TABLE)
                .withAllFields()
                .usingReference(SCALED_VECTORS_TO_HOLDING)
                .withAllFields()
                .except(
                        DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                        DatastoreConstants.HoldingStore.Fields.PORTFOLIO,
                        DatastoreConstants.HoldingStore.Fields.HOLDING_ID)
                // As Of Date store
                .usingReference(SCALED_VECTORS_TO_HOLDING, DatastoreConstants.References.HOLDING_TO_ASOFDATE)
                .withAllFields()
                .except(DatastoreConstants.AsOfDateStore.Fields.AS_OF_DATE)
                // Scaled results store
                .usingReference(SCALED_VECTORS_TO_HOLDING, DatastoreConstants.References.HOLDING_TO_SCALEDSTATRESULT)
                .withAllFields()
                .except(
                        DatastoreConstants.ScaledStatResultStore.Fields.AS_OF_DATE,
                        DatastoreConstants.ScaledStatResultStore.Fields.PARTITION_KEY,
                        DatastoreConstants.ScaledStatResultStore.Fields.HOLDING_ID)
                // Holding detail
                .usingReference(SCALED_VECTORS_TO_HOLDING, DatastoreConstants.References.HOLDING_TO_HOLDINGDETAIL)
                .withAllFields()
                .except(
                        DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE,
                        DatastoreConstants.HoldingDetailStore.Fields.PARTITION_KEY,
                        DatastoreConstants.HoldingDetailStore.Fields.BASE_HOLDING_ID)
                // Security store
                .usingReference(
                        SCALED_VECTORS_TO_HOLDING,
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
                        SCALED_VECTORS_TO_HOLDING,
                        DatastoreConstants.References.HOLDING_TO_HOLDINGDETAIL,
                        DatastoreConstants.References.HOLDINGDETAIL_TO_POSITIONDETAIL)
                .withAllFields()
                .except(DatastoreConstants.PositionDetailStore.Fields.SECURITY_NAME)
                .build();
    }
}
