/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.cfg.source.DlcConfig.COB_DATE_SCOPE_PARAMETER;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_DATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.io.dlc.impl.description.topic.JdbcTopicDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.ChannelDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.column.calc.CustomFieldDescription;
import com.activeviam.io.dlc.impl.utils.NamedEntityResolverService;
import com.activeviam.source.jdbc.api.calculator.LocalDateJdbcColumnCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@ConditionalOnApplicationWithDatastore
@Slf4j
public class DremioJdbcSourceConfig {
    public static final String DREMIO_TOPICS = "DremioTopics";

    public static final String TRADES_SQL_TOPIC = TRADES_STORE_NAME;
    public static final String TRADE_ATTRIBUTES_SQL_TOPIC = TRADE_ATTRIBUTES_STORE_NAME;
    public static final String COUNTERPARTIES_SQL_TOPIC = COUNTERPARTIES_STORE_NAME;

    public static final String COB_DATE_SQL_PARSER = "CobDateSqlParser";

    public static final String TRADES_SQL_QUERY =
            """
            SELECT *
            FROM Trades
            WHERE CobDate IN (?)
            """;
    public static final String TRADE_ATTRIBUTES_SQL_QUERY =
            """
            SELECT *
            FROM TradeAttributes
            WHERE CobDate IN (?)
            """;

    public static final String COUNTERPARTIES_SQL_QUERY =
            """
            SELECT *
            FROM Counterparties
            """;

    @Bean
    CustomFieldDescription cobDateJdbcParser() {
        return CustomFieldDescription.of(COB_DATE_SQL_PARSER, scope -> new LocalDateJdbcColumnCalculator(COB_DATE));
    }

    @Bean
    JdbcTopicDescription tradesJdbcTopic(NamedEntityResolverService namedEntityResolverService) {
        return JdbcTopicDescription.builder(TRADES_SQL_TOPIC, TRADES_SQL_QUERY)
                .parameterOrder(List.of(COB_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(namedEntityResolverService.getTarget(TRADES_STORE_NAME))
                        .customFields(namedEntityResolverService.getCustomFields(Set.of(COB_DATE_SQL_PARSER)))
                        .build())
                .build();
    }

    @Bean
    JdbcTopicDescription counterpartiesJdbcTopic(NamedEntityResolverService namedEntityResolverService) {
        return JdbcTopicDescription.builder(COUNTERPARTIES_SQL_TOPIC, COUNTERPARTIES_SQL_QUERY)
                .channel(ChannelDescription.builder(namedEntityResolverService.getTarget(COUNTERPARTIES_STORE_NAME))
                        .build())
                .build();
    }

    @Bean
    JdbcTopicDescription tradeAttributesJdbcTopic(NamedEntityResolverService namedEntityResolverService) {
        var customFields = new ArrayList<CustomFieldDescription>(
                namedEntityResolverService.getCustomFields(Set.of(COB_DATE_SQL_PARSER)));
        customFields.add(CustomFieldDescription.of(
                "Inline custom field", scope -> new LocalDateJdbcColumnCalculator(TRADE_DATE)));
        return JdbcTopicDescription.builder(TRADE_ATTRIBUTES_SQL_TOPIC, TRADE_ATTRIBUTES_SQL_QUERY)
                .parameterOrder(List.of(COB_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(namedEntityResolverService.getTarget(TRADE_ATTRIBUTES_STORE_NAME))
                        .customFields(customFields)
                        .build())
                .build();
    }
}
