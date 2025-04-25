/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDataActivePivot;
import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.apps.rest.CobDateDataController;
import com.activeviam.database.api.DatabasePrinter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnApplicationWithDatastore
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialLoad {
    private final CobDateDataController cobDateDataController;
    private final ApplicationWithDatastore applicationWithDatastore;
    private final CobDatesProperties cobDatesProperties;

    @EventListener(value = ApplicationStartedEvent.class)
    void onApplicationReady() {
        log.info("ApplicationReadyEvent triggered");
        initialInMemoryLoad();
    }

    private void initialInMemoryLoad() {
        log.info("Initial data load started...");
        try {
            for (var cobDate : cobDatesProperties.getInMemoryDates()) {
                cobDateDataController.loadCobDate(cobDate);
            }
            log.info("Initial data load completed");
            DatabasePrinter.printTableSizes(
                    applicationWithDatastore.getDatastore().getMasterHead());
            startDistributionMessenger(applicationWithDatastore.getManager());
        } catch (Exception e) {
            log.warn("Failed to load initial data", e);
        }
    }

    public static void startDistributionMessenger(IActivePivotManager activePivotManager) {
        try {
            for (var activePivotVersion : activePivotManager.getActivePivots().values()) {
                // start the data cube distributed messenger
                if (activePivotVersion instanceof IMultiVersionDataActivePivot multiVersionActivePivot) {
                    log.info("Starting messenger for {}.", multiVersionActivePivot.getId());
                    multiVersionActivePivot.startDistribution();
                    log.info("Messenger started for {}.", multiVersionActivePivot.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to start messenger.", e);
        }
    }
}
