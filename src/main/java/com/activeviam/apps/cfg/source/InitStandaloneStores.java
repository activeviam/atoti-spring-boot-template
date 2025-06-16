/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.SHIFT_COB_DATE_STORE_NAME;

import java.time.LocalDate;
import java.util.stream.IntStream;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.activeviam.apps.annotations.ConditionalOnDataNode;
import com.activeviam.database.datastore.api.IDatastore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnDataNode
public class InitStandaloneStores {

    private final IDatastore datastore;

    @EventListener(value = ApplicationStartedEvent.class)
    void onApplicationReady() {
        log.info("ApplicationReadyEvent triggered");
        // Fill the SHIFT cob dates
        datastore.edit(t -> {
            t.addAll(
                    SHIFT_COB_DATE_STORE_NAME,
                    IntStream.range(1, 100)
                            .mapToObj(x -> new Object[] {LocalDate.now().minusDays(x)})
                            .toList());
        });
    }
}
