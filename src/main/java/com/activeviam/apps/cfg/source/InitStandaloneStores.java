/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.cfg.database.datastore.datamodel.StoreDefinitionsConfig.SHIFT_COB_DATES_STORE_BEAN;
import static com.activeviam.apps.constants.StoreAndFieldConstants.SHIFT_COB_DATE_STORE_NAME;

import java.time.LocalDate;
import java.util.stream.IntStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.activeviam.database.datastore.api.IDatastore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(name = SHIFT_COB_DATES_STORE_BEAN)
public class InitStandaloneStores {

    private final IDatastore datastore;

    @EventListener(value = ApplicationReadyEvent.class)
    void onApplicationReady() {
        log.info("ApplicationReadyEvent triggered");
        refreshShiftCobDateStore();
    }

    public void refreshShiftCobDateStore() {
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
