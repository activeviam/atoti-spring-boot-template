/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.time.LocalDate;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.apps.annotations.ConditionalOnDataNode;
import com.activeviam.apps.rest.CobDateDataController;
import com.activeviam.database.api.DatabasePrinter;
import com.activeviam.database.datastore.api.IDatastore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnApplicationWithDatastore
@ConditionalOnDataNode
@RequiredArgsConstructor
@Slf4j
@Configuration
public class InitialLoad {
    private final CobDateDataController cobDateDataController;
    private final IDatastore datastore;

    @EventListener(value = ApplicationReadyEvent.class)
    void onApplicationReady() {
        log.info("ApplicationReadyEvent triggered");
        initialLoad();
    }

    private void initialLoad() {
        log.info("Initial data load started...");
        try {
            cobDateDataController.loadCobDate(LocalDate.of(2025, 3, 1));
            log.info("Initial data load completed");
            DatabasePrinter.printTableSizes(datastore.getMasterHead());
        } catch (Exception e) {
            log.warn("Failed to load initial data", e);
        }
    }
}
