/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg;

import static com.activeviam.apps.cfg.CsvSourceConfig.TRADES_TOPIC;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.database.datastore.internal.impl.SchemaPrinter;
import com.activeviam.source.common.api.IMessageChannel;
import com.activeviam.source.csv.api.CsvMessageChannelFactory;
import com.activeviam.source.csv.api.ICsvSource;
import com.activeviam.source.csv.api.IFileInfo;
import com.activeviam.source.csv.api.ILineReader;
import com.activeviam.tech.concurrency.internal.timing.impl.StopWatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LoadConfig {
    private final IDatastore datastore;
    private final ICsvSource<Path> csvSource;
    private final CsvMessageChannelFactory<Path> csvChannelFactory;

    @EventListener(value = ApplicationReadyEvent.class)
    void onApplicationReady() throws Exception {
        log.info("ApplicationReadyEvent triggered");
        initialLoad();
    }

    private void initialLoad() throws Exception {
        log.info("Initial data load started.");
        Collection<IMessageChannel<IFileInfo<Path>, ILineReader>> csvChannels = new ArrayList<>();
        csvChannels.add(csvChannelFactory.createChannel(TRADES_TOPIC, StoreAndFieldConstants.TRADES_STORE_NAME));

        // do the transactions
        var before = System.nanoTime();

        datastore.edit(t -> {
            csvSource.fetch(csvChannels);
            t.forceCommit();
        });

        var elapsed = System.nanoTime() - before;
        log.info("Initial data load completed in {} ms.", elapsed / 1_000_000L);

        printStoreSizes();
    }

    private void printStoreSizes() {
        // Print stop watch profiling
        var timingsOutput = new StringBuilder();
        StopWatch.get().appendTimings(timingsOutput);
        log.info(timingsOutput.toString());
        StopWatch.get().printTimingLegend();

        // print sizes
        SchemaPrinter.printStoresSizes(datastore.getMasterHead());
    }
}
