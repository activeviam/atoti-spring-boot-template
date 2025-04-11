/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.COB_DATE_SCOPE_PARAMETER;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.TRADES_SQL_TOPIC;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.TRADE_ATTRIBUTES_SQL_TOPIC;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.activeviam.database.api.DatabasePrinter;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.io.dlc.api.description.source.DlcSourceType;
import com.activeviam.io.dlc.impl.DataLoadControllerService;
import com.activeviam.io.dlc.impl.operations.request.DlcLoadRequest;
import com.activeviam.io.dlc.impl.operations.request.scope.DlcScope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class InitialLoad {
    private final DataLoadControllerService dataLoadControllerService;
    private final IDatastore datastore;

    @EventListener(value = ApplicationReadyEvent.class)
    void onApplicationReady() {
        log.info("ApplicationReadyEvent triggered");
        initialLoad();
    }

    private void initialLoad() {
        log.info("Initial data load started...");
        try {
            dataLoadControllerService.execute(DlcLoadRequest.builder()
                    .topics(TRADES_SQL_TOPIC, TRADE_ATTRIBUTES_SQL_TOPIC)
                    .sourceType(DlcSourceType.JDBC_SOURCE)
                    .scope(DlcScope.of(COB_DATE_SCOPE_PARAMETER, "2025-03-01"))
                    .build());
            log.info("Initial data load completed");
            DatabasePrinter.printTableSizes(datastore.getMasterHead());
        } catch (Exception e) {
            log.warn("Failed to load initial data", e);
        }
    }
}
