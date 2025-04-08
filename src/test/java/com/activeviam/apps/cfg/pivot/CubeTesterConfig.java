/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.datastore.api.transaction.IOpenedTransaction;
import com.activeviam.tech.core.api.exceptions.ActiveViamRuntimeException;

@Import({ApplicationWithDatastoreConfig.class})
public class CubeTesterConfig {

    @Bean
    public CubeTester cubeTester(ApplicationWithDatastore application) {
        var manager = application.getManager();
        try {
            manager.init(null);
            manager.start();
            var datastore = application.getDatastore();
            datastore.edit(this::loadData);
            return CubeTester.from(manager);
        } catch (Exception e) {
            throw new ActiveViamRuntimeException(e);
        }
    }

    public void loadData(IOpenedTransaction transaction) {
        // Do nothing
    }
}
