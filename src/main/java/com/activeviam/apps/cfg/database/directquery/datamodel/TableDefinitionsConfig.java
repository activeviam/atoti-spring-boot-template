/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery.datamodel;

import static com.activeviam.apps.cfg.database.datastore.datamodel.StoreDefinitionsConfig.referenceName;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;

import java.util.Set;

import org.springframework.context.annotation.Bean;

import com.activeviam.apps.cfg.database.directquery.DremioConfigurationProperties;
import com.activeviam.database.api.schema.ITableJoin;
import com.activeviam.database.sql.api.schema.SqlTableId;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.discoverer.IDirectQueryTableDiscoverer;
import com.activeviam.directquery.api.schema.JoinDescription;
import com.activeviam.directquery.api.schema.TableDescription;

public class TableDefinitionsConfig {
    private final IDirectQueryTableDiscoverer directQueryTableDiscoverer;

    private final DremioConfigurationProperties dremioConfigurationProperties;

    private SqlTableId sqlTableId(String tableName) {
        return new SqlTableId(
                dremioConfigurationProperties.getDatabase(), dremioConfigurationProperties.getSchema(), tableName);
    }

    public TableDefinitionsConfig(
            DirectQueryConnector<?> directQueryConnector, DremioConfigurationProperties dremioConfigurationProperties) {
        directQueryTableDiscoverer = directQueryConnector.getDiscoverer();
        this.dremioConfigurationProperties = dremioConfigurationProperties;
    }

    @Bean
    TableDescription tradesTableDescription() {
        return directQueryTableDiscoverer.discoverTable(sqlTableId(TRADES_STORE_NAME));
    }

    @Bean
    TableDescription tradeAttributesTableDescription() {
        return directQueryTableDiscoverer.discoverTable(sqlTableId(TRADE_ATTRIBUTES_STORE_NAME));
    }

    @Bean
    JoinDescription tradesToAttributesJoinDescription() {
        return JoinDescription.builder()
                .sourceTableName(TRADES_STORE_NAME)
                .targetTableName(TRADE_ATTRIBUTES_STORE_NAME)
                .name(referenceName(TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME))
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(ASOFDATE, ASOFDATE),
                        new ITableJoin.FieldMapping(TRADE_ID, TRADE_ID)))
                .build();
    }
}
