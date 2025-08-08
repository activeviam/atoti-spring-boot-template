/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.SHIFT_COB_DATE_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.io.dlc.impl.description.topic.CsvTopicDescription;
import com.activeviam.io.dlc.impl.utils.NamedEntityResolverService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@ConditionalOnApplicationWithDatastore
@Slf4j
public class CsvSourceConfig {
    public static final String CSV_TOPICS = "CsvTopics";
    public static final String TRADES_CSV_TOPIC = TRADES_STORE_NAME;
    public static final String TRADE_ATTRIBUTES_CSV_TOPIC = TRADE_ATTRIBUTES_STORE_NAME;
    public static final String COUNTERPARTIES_CSV_TOPIC = COUNTERPARTIES_STORE_NAME;

    public static final String TRADES_PATTERN = "glob:**/trades.csv";
    public static final String TRADE_ATTRIBUTES_PATTERN = "glob:**/trade_attributes.csv";

    @Bean
    CsvTopicDescription tradesCsvTopic(NamedEntityResolverService namedEntityResolverService) {
        return CsvTopicDescription.builder(TRADES_CSV_TOPIC, TRADES_PATTERN).build();
    }

    @Bean
    CsvTopicDescription tradeAttributesCsvTopic(NamedEntityResolverService namedEntityResolverService) {
        return CsvTopicDescription.builder(TRADE_ATTRIBUTES_CSV_TOPIC, TRADE_ATTRIBUTES_PATTERN)
                .build();
    }

    @Bean
    CsvTopicDescription shiftCobDatesCsvTopic(NamedEntityResolverService namedEntityResolverService) {
        return CsvTopicDescription.builder(SHIFT_COB_DATE_STORE_NAME, "TRADE_ATTRIBUTES_PATTERN")
                .build();
    }

}
