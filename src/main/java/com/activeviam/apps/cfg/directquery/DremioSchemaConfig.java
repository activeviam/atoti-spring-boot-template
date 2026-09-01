/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.directquery;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
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
        final String tradesTableName = dremioProperties.getTradesTableName();
        final String tradeAttributesTableName = dremioProperties.getTradeAttributesTableName();

        final TableDescription tradesTable =
                discoverer.discoverTable(new SqlTableId(NO_CATALOG, dremioProperties.getSpace(), tradesTableName));
        final TableDescription tradeAttributesTable = discoverer.discoverTable(
                new SqlTableId(NO_CATALOG, dremioProperties.getSpace(), tradeAttributesTableName));

        // RelationshipOptionality.OPTIONAL - Scenario 5 (star schema, unpartitioned provider, OPTIONAL
        // join), 31 Aug 2026: matches WCR's real production setting. Supersedes the 27 Aug MANDATORY
        // pivot for this scenario; do not flip back to MANDATORY without an explicit instruction.
        final JoinDescription join = JoinDescription.builder()
                .name(String.format("%s_to_%s", tradesTableName, tradeAttributesTableName))
                .sourceTableName(tradesTableName)
                .targetTableName(tradeAttributesTableName)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(ASOFDATE, ASOFDATE),
                        new ITableJoin.FieldMapping(TRADE_ID, TRADE_ID)))
                .targetOptionality(RelationshipOptionality.OPTIONAL)
                .build();

        return SchemaDescription.builder()
                .externalTables(List.of(tradesTable, tradeAttributesTable))
                .externalJoins(List.of(join))
                .build();
    }
}
