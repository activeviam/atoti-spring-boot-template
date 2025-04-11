/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;

import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.database.api.conditions.BaseConditions;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.io.dlc.impl.description.topic.JdbcTopicDescription;
import com.activeviam.io.dlc.impl.description.topic.UnloadTopicDescription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DremioJdbcSourceConfig {
    public static final String DREMIO_TOPICS = "DremioTopics";
    public static final String DREMIO_UNLOAD_TOPIC = "DremioUnloadTopic";

    public static final String TRADES_SQL_TOPIC = TRADES_STORE_NAME;
    public static final String TRADE_ATTRIBUTES_SQL_TOPIC = TRADE_ATTRIBUTES_STORE_NAME;

    private static final String TRADES_SQL_QUERY =
            """
            SELECT *
            FROM Trades
            WHERE cobDate = ?
            """;
    private static final String TRADE_ATTRIBUTES_SQL_QUERY =
            """
            SELECT *
            FROM TradeAttributes
            WHERE cobDate = ?
            """;

    public static final String COB_DATE_SCOPE_PARAMETER = "cobDate";

    @Bean
    JdbcTopicDescription tradesJdbcTopic() {
        return JdbcTopicDescription.builder(TRADES_SQL_TOPIC, TRADES_SQL_QUERY)
                .parameterOrder(List.of(COB_DATE_SCOPE_PARAMETER))
                .build();
    }

    @Bean
    UnloadTopicDescription unloadTopicDescription() {
        return UnloadTopicDescription.builder()
                .name(DREMIO_UNLOAD_TOPIC)
                .stores(Set.of(TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME))
                .removalConditionFactory((storeDescription, scope) -> {
                    assert scope.containsKey(COB_DATE_SCOPE_PARAMETER);
                    return BaseConditions.equal(FieldPath.of(ASOFDATE), scope.get(COB_DATE_SCOPE_PARAMETER));
                })
                .build();
    }

    @Bean
    JdbcTopicDescription tradeAttributesJdbcTopic() {
        return JdbcTopicDescription.builder(TRADE_ATTRIBUTES_SQL_TOPIC, TRADE_ATTRIBUTES_SQL_QUERY)
                .parameterOrder(List.of(COB_DATE_SCOPE_PARAMETER))
                .build();
    }
}
