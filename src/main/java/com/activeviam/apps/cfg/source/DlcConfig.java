/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.COUNTERPARTIES_SQL_TOPIC;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.DREMIO_TOPICS;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.TRADES_SQL_TOPIC;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.TRADE_ATTRIBUTES_SQL_TOPIC;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;

import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.database.api.conditions.BaseConditions;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.io.dlc.impl.description.AliasesDescription;
import com.activeviam.io.dlc.impl.description.topic.UnloadTopicDescription;

@Configuration
public class DlcConfig {

    public static final String COB_DATE_UNLOAD_TOPIC = "CobDateUnloadTopic";

    public static final String COB_DATE_SCOPE_PARAMETER = "cobDate";

    @Bean
    AliasesDescription aliases() {
        return new AliasesDescription(
                Map.of(DREMIO_TOPICS, Set.of(TRADES_SQL_TOPIC, TRADE_ATTRIBUTES_SQL_TOPIC, COUNTERPARTIES_SQL_TOPIC)));
    }

    @Bean
    UnloadTopicDescription unloadTopicDescription() {
        return UnloadTopicDescription.builder()
                .name(COB_DATE_UNLOAD_TOPIC)
                .stores(Set.of(TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME))
                .removalConditionFactory((storeDescription, scope) -> {
                    assert scope.containsKey(COB_DATE_SCOPE_PARAMETER);
                    return BaseConditions.equal(FieldPath.of(COB_DATE), scope.get(COB_DATE_SCOPE_PARAMETER));
                })
                .build();
    }
}
