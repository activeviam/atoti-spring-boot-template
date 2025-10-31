/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.RUN_CHAINS_STORE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;

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
    private static final String TRADE_ATTRIBUTES_FILE_PATTERN = "glob:**trade_attributes.csv";

    @Bean
    CsvTopicDescription tradesCsvTopic() {
        return CsvTopicDescription.builder(TRADES_STORE_NAME, TRADES_FILE_PATTERN)
                .build();
    }

    @Bean
    CsvTopicDescription tradeAttributesCsvTopic() {
        return CsvTopicDescription.builder(TRADE_ATTRIBUTES_STORE_NAME, TRADE_ATTRIBUTES_FILE_PATTERN)
                .build();
    }

    @Bean
    CsvTopicDescription cubeRunChainsCsvTopic() {
        return CsvTopicDescription.builder(RUN_CHAINS_STORE, "glob:**cube_run_chains.csv")
                .build();
    }
}
