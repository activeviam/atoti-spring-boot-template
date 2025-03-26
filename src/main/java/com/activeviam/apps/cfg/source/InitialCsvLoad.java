/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.activeviam.database.api.DatabasePrinter;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.io.dlc.api.IDataLoadControllerService;
import com.activeviam.io.dlc.impl.operations.request.DlcLoadRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitialCsvLoad {
    private final IDatastore datastore;
    //    private final ICsvSource<Path> csvSource;
    //    private final CsvMessageChannelFactory<Path> csvChannelFactory;
    //    private final CsvSourceProperties csvSourceProperties;
    //    private final IMessageHandler<IFileInfo<Path>> messageHandler;

    @Bean
    public ApplicationRunner initialConfigDataLoad(IDataLoadControllerService dlc) {
        return args -> {
            dlc.execute(DlcLoadRequest.builder().topics("alias").build());
        };
    }

    @EventListener(value = ApplicationReadyEvent.class)
    void onApplicationReady() {
        log.info("ApplicationReadyEvent triggered");
        initialLoad();
    }

    private void initialLoad() {
        log.info("Initial data load started.");
        //        Collection<IMessageChannel<IFileInfo<Path>, ILineReader>> csvChannels = new ArrayList<>();
        //
        //        csvSourceProperties.getTopics().stream()
        //                .map(topic -> {
        //                    var channel = csvChannelFactory.createChannel(topic.topicName(), topic.storeName());
        //                    channel.withMessageHandler(messageHandler);
        //                    return channel;
        //                })
        //                .forEach(csvChannels::add);
        //
        //        // do the transactions
        //        var before = System.nanoTime();
        //
        //        datastore.edit(t -> {
        //            csvSource.fetch(csvChannels);
        //            t.forceCommit();
        //        });
        //
        //        var elapsed = System.nanoTime() - before;
        //        log.info("Initial data load completed in {} ms.", elapsed / 1_000_000L);

        printStoreSizes();
    }

    private void printStoreSizes() {
        // print sizes
        DatabasePrinter.printTableSizes(datastore.getMasterHead());
    }
}
