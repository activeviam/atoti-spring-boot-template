/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TEST_DECIMAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_DATE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.database.api.conditions.BaseConditions;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.io.dlc.impl.description.topic.JdbcTopicDescription;
import com.activeviam.io.dlc.impl.description.topic.UnloadTopicDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.ChannelDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.column.calc.CustomFieldDescription;
import com.activeviam.io.dlc.impl.utils.NamedEntityResolverService;
import com.activeviam.source.common.api.IColumnCalculator;
import com.activeviam.source.jdbc.api.calculator.LocalDateJdbcColumnCalculator;
import com.activeviam.source.jdbc.api.impl.ResultSetRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@ConditionalOnApplicationWithDatastore
@Slf4j
public class DremioJdbcSourceConfig {
    public static final String DREMIO_TOPICS = "DremioTopics";
    public static final String DREMIO_UNLOAD_TOPIC = "DremioUnloadTopic";

    public static final String TRADES_SQL_TOPIC = TRADES_STORE_NAME;
    public static final String TRADE_ATTRIBUTES_SQL_TOPIC = TRADE_ATTRIBUTES_STORE_NAME;

    public static final String COB_DATE_SQL_PARSER = "CobDateSqlParser";
    public static final String TEST_DECIMAL_SQL_PARSER = "TestDecimalSqlParser";

    public static final String TRADES_SQL_QUERY =
            """
            SELECT *
            FROM Trades
            WHERE CobDate = ?
            """;
    public static final String TRADE_ATTRIBUTES_SQL_QUERY =
            """
            SELECT *
            FROM TradeAttributes
            WHERE CobDate = ?
            """;

    public static final String COB_DATE_SCOPE_PARAMETER = "cobDate";

    @Bean
    CustomFieldDescription cobDateJdbcParser() {
        return CustomFieldDescription.of(COB_DATE_SQL_PARSER, scope -> new LocalDateJdbcColumnCalculator(COB_DATE));
    }

    @Bean
    CustomFieldDescription testDecimalJdbcParser() {
        return CustomFieldDescription.of(TEST_DECIMAL_SQL_PARSER, scope -> new IColumnCalculator<ResultSetRow>() {
            @Override
            public String getColumnName() {
                return TEST_DECIMAL;
            }

            @Override
            public Object compute(IColumnCalculationContext<ResultSetRow> context) {
                var decimal = context.getContext().getObject(TEST_DECIMAL);
                return Objects.nonNull(decimal) ? ((BigDecimal) decimal).intValue() : null;
            }
        });
    }

    @Bean
    JdbcTopicDescription tradesJdbcTopic(NamedEntityResolverService namedEntityResolverService) {
        return JdbcTopicDescription.builder(TRADES_SQL_TOPIC, TRADES_SQL_QUERY)
                .parameterOrder(List.of(COB_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(namedEntityResolverService.getTarget(TRADES_STORE_NAME))
                        .customFields(namedEntityResolverService.getCustomFields(
                                Set.of(COB_DATE_SQL_PARSER, TEST_DECIMAL_SQL_PARSER)))
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

    @Bean
    UnloadTopicDescription unloadTopicDescription() {
        return UnloadTopicDescription.builder()
                .name(DREMIO_UNLOAD_TOPIC)
                .stores(Set.of(TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME))
                .removalConditionFactory((storeDescription, scope) -> {
                    assert scope.containsKey(COB_DATE_SCOPE_PARAMETER);
                    return BaseConditions.equal(FieldPath.of(COB_DATE), scope.get(COB_DATE_SCOPE_PARAMETER));
                })
                .build();
    }
}
