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

import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.io.dlc.impl.description.AliasesDescription;

@Configuration
public class DlcConfig {

    @Bean
    AliasesDescription aliases() {
        return new AliasesDescription(
                Map.of(DREMIO_TOPICS, Set.of(TRADES_SQL_TOPIC, TRADE_ATTRIBUTES_SQL_TOPIC, COUNTERPARTIES_SQL_TOPIC)));
    }
}
