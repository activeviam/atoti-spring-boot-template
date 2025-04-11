/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery;

import org.springframework.context.annotation.Import;

import com.activeviam.apps.cfg.database.directquery.datamodel.TableDefinitionsConfig;

@Import(
        value = {
            DirectQuerySchemaConfig.class,
            DirectQuerySchemaConfig.class,
            DremioDirectQueryConnectorConfiguration.class,
            TableDefinitionsConfig.class
        })
public class DirectQueryConfig {}
