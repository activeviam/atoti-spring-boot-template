/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery;

import java.util.List;

import org.springframework.context.annotation.Bean;

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
}
