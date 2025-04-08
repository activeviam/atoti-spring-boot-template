/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.io.dlc.impl.description.topic.CsvTopicDescription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CsvSourceConfig {

    private static final String TRADES_FILE_PATTERN = "glob:**trades.csv";

    @Bean
    CsvTopicDescription tradesCsvTopic() {
        return CsvTopicDescription.builder(TRADES_STORE_NAME, TRADES_FILE_PATTERN)
                .build();
    }
}
