/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.BOOKS_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.FX_RATES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_SETS_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_ADD_ON_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.activeviam.database.api.DatabasePrinter;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.io.dlc.impl.DataLoadControllerService;
import com.activeviam.io.dlc.impl.operations.request.DlcLoadRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class InitialDataLoad {
    private final DataLoadControllerService dataLoadControllerService;
    private final IDatastore datastore;

    @EventListener(value = ApplicationReadyEvent.class)
    void onApplicationReady() {
        log.info("ApplicationReadyEvent triggered");
        initialLoad();
    }

    private void initialLoad() {
        log.info("Initial data load started...");
        var before = System.nanoTime();
        try {
            dataLoadControllerService.execute(DlcLoadRequest.builder()
                    .topics(TRADES_STORE_NAME,
                            NETTING_SETS_STORE_NAME,
                            COUNTERPARTIES_STORE_NAME,
                            FX_RATES_STORE_NAME,
                            BOOKS_STORE_NAME,
                            PFE_ADD_ON_STORE_NAME)
                    .build());
            var elapsed = System.nanoTime() - before;
            log.info("Initial data load completed in [{}] ms.", elapsed / 1000000L);
            DatabasePrinter.printTableSizes(datastore.getMasterHead());
//            DatabasePrinter.printTables(datastore.getMasterHead(),10);
        } catch (Exception e) {
            log.warn("Failed to load initial data", e);
        }
    }

}
