/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.directquery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.directquery.api.schema.SchemaDescription;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile("data-node")
@RequiredArgsConstructor
public class DremioSelectionConfig {
    private final SchemaDescription dremioSchemaDescription;
    private final DremioProperties dremioProperties;

    @Bean
    public ISelectionDescription datastoreSelectionDescription() {
        return StartBuilding.selection(dremioSchemaDescription)
                .fromBaseStore(dremioProperties.getTradesTableName())
                .withAllReachableFields()
                .build();
    }
}
