/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.directquery;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;

import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.database.api.schema.ITableJoin;
import com.activeviam.database.api.schema.RelationshipOptionality;
import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.database.sql.api.schema.SqlTableId;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.discoverer.IDirectQueryTableDiscoverer;
import com.activeviam.directquery.api.schema.JoinDescription;
import com.activeviam.directquery.api.schema.SchemaDescription;
import com.activeviam.directquery.api.schema.TableDescription;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile("data-node")
@RequiredArgsConstructor
public class DremioSchemaConfig {
    private static final String NO_CATALOG = "";

    private final DirectQueryConnector<GenericJdbcDatabaseSettings> dremioConnector;
    private final DremioProperties dremioProperties;

    @Bean
    public SchemaDescription dremioSchemaDescription() {
        final IDirectQueryTableDiscoverer discoverer = dremioConnector.getDiscoverer();

        final TableDescription tradesTable =
                discoverer.discoverTable(new SqlTableId(NO_CATALOG, dremioProperties.getSpace(), TRADES_STORE_NAME));
        final TableDescription tradeAttributesTable = discoverer.discoverTable(
                new SqlTableId(NO_CATALOG, dremioProperties.getSpace(), TRADE_ATTRIBUTES_STORE_NAME));

        final JoinDescription join = JoinDescription.builder()
                .name(String.format("%s_to_%s", TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME))
                .sourceTableName(TRADES_STORE_NAME)
                .targetTableName(TRADE_ATTRIBUTES_STORE_NAME)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(ASOFDATE, ASOFDATE),
                        new ITableJoin.FieldMapping(TRADE_ID, TRADE_ID)))
                .targetOptionality(RelationshipOptionality.MANDATORY)
                .build();

        return SchemaDescription.builder()
                .externalTables(List.of(tradesTable, tradeAttributesTable))
                .externalJoins(List.of(join))
                .build();
    }
}
