/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.SHIFT_COB_DATE_STORE_NAME;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.stream.IntStream;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.activeviam.activepivot.core.datastore.api.builder.ApplicationWithDatastore;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDataActivePivot;
import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.apps.rest.CobDateLoadController;
import com.activeviam.database.api.DatabasePrinter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnApplicationWithDatastore
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialLoad {
    private final CobDateLoadController cobDateLoadController;
    private final ApplicationWithDatastore applicationWithDatastore;
    private final CobDatesProperties cobDatesProperties;

    @EventListener(value = ApplicationStartedEvent.class)
    void onApplicationReady() {
        log.info("ApplicationReadyEvent triggered");
        // Fill the SHIFT cob dates
        applicationWithDatastore.getDatastore().edit(t -> {
            t.addAll(
                    SHIFT_COB_DATE_STORE_NAME,
                    IntStream.range(1, 100)
                            .mapToObj(x -> new Object[] {LocalDate.now().minusDays(x)})
                            .toList());
        });
        startDistributionMessenger(applicationWithDatastore.getManager());
        initialInMemoryLoad();
    }

    private void initialInMemoryLoad() {
        log.info("Initial data load started...");
        var datesToLoad = new HashSet<LocalDate>(cobDatesProperties.getFixedCobDates());
        datesToLoad.addAll(cobDatesProperties.computeInMemoryDates());
        try {
            cobDateLoadController.loadCobDates(datesToLoad);
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
