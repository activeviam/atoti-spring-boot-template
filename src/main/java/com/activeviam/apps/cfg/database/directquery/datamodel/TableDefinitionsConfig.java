/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery.datamodel;

import static com.activeviam.apps.cfg.database.datastore.datamodel.StoreDefinitionsConfig.referenceName;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.SHIFT_COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.SHIFT_COB_DATE_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;
import static com.activeviam.database.api.types.ILiteralType.LOCAL_DATE;

import java.util.Set;

import org.springframework.context.annotation.Bean;

import com.activeviam.apps.cfg.database.directquery.DremioConfigurationProperties;
import com.activeviam.database.api.schema.ITableJoin;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;
import com.activeviam.database.sql.api.schema.SqlTableId;
import com.activeviam.database.sql.internal.jdbc.JdbcDiscoverer;
import com.activeviam.database.sql.internal.jdbc.connection.IJdbcConfiguration;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.discoverer.IDirectQueryTableDiscoverer;
import com.activeviam.directquery.api.schema.JoinDescription;
import com.activeviam.directquery.api.schema.TableDescription;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TableDefinitionsConfig {
    private final IDirectQueryTableDiscoverer directQueryTableDiscoverer;

    private final DremioConfigurationProperties dremioConfigurationProperties;

    private static final String SCHEMA = "nessie";

    private SqlTableId sqlTableId(String schema, String tableName) {
        return new SqlTableId(dremioConfigurationProperties.getDatabase(), schema, tableName);
    }

    public TableDefinitionsConfig(
            DirectQueryConnector<?> directQueryConnector,
            DremioConfigurationProperties dremioConfigurationProperties,
            IJdbcConfiguration jdbcConfiguration) {
        JdbcDiscoverer.listTables(jdbcConfiguration).forEach((catalog, schemas) -> {
            log.info("Catalog: {}", catalog);
            schemas.forEach((schema, tables) -> {
                log.info("\tSchema: {}", schema);
                tables.forEach(table -> {
                    log.info("\t\tTable: {}", table.getTableName());
                });
            });
        });
        directQueryTableDiscoverer = directQueryConnector.getDiscoverer();
        this.dremioConfigurationProperties = dremioConfigurationProperties;
    }

    @Bean
    TableDescription tradesTableDescription() {
        return directQueryTableDiscoverer.discoverTable(sqlTableId(SCHEMA, TRADES_STORE_NAME)).toBuilder()
                .clusteringFieldNames(Set.of(COB_DATE, TRADE_ID))
                .build();
    }

    @Bean
    TableDescription tradeAttributesTableDescription() {
        return directQueryTableDiscoverer.discoverTable(sqlTableId(SCHEMA, TRADE_ATTRIBUTES_STORE_NAME));
    }

    @Bean
    TableDescription counterpartyTableDescription() {
        return directQueryTableDiscoverer.discoverTable(sqlTableId(SCHEMA, COUNTERPARTIES_STORE_NAME));
    }

    @Bean
    JoinDescription tradesToAttributesJoinDescription() {
        return JoinDescription.builder()
                .sourceTableName(TRADES_STORE_NAME)
                .targetTableName(TRADE_ATTRIBUTES_STORE_NAME)
                .name(referenceName(TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME))
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(COB_DATE, COB_DATE),
                        new ITableJoin.FieldMapping(TRADE_ID, TRADE_ID)))
                .build();
    }

    @Bean
    JoinDescription tradesToCounterpartyJoinDescription() {
        return JoinDescription.builder()
                .sourceTableName(TRADE_ATTRIBUTES_STORE_NAME)
                .targetTableName(COUNTERPARTIES_STORE_NAME)
                .name(referenceName(TRADE_ATTRIBUTES_STORE_NAME, COUNTERPARTIES_STORE_NAME))
                .fieldMappings(Set.of(new ITableJoin.FieldMapping(COUNTERPARTY_ID, COUNTERPARTY_ID)))
                .build();
    }

    @Bean
    public IStoreDescription shiftCobDateStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(SHIFT_COB_DATE_STORE_NAME)
                .withField(SHIFT_COB_DATE, LOCAL_DATE)
                .asKeyField()
                .build();
    }
}
