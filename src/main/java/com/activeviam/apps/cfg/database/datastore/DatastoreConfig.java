/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.datastore;

import org.springframework.context.annotation.Import;

import com.activeviam.apps.cfg.database.DatabaseSelectionConfig;
import com.activeviam.apps.cfg.database.datastore.datamodel.StoreDefinitionsConfig;

import lombok.RequiredArgsConstructor;

@Import({StoreDefinitionsConfig.class, DatastoreSchemaConfig.class, DatabaseSelectionConfig.class})
@RequiredArgsConstructor
public class DatastoreConfig {}
