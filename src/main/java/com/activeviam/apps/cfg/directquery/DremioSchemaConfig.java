/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.directquery;

import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.database.jdbc.api.GenericJdbcDatabaseSettings;
import com.activeviam.database.sql.api.schema.SqlTableId;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.discoverer.IDirectQueryTableDiscoverer;
import com.activeviam.directquery.api.schema.SchemaDescription;
import com.activeviam.directquery.api.schema.TableDescription;

import lombok.RequiredArgsConstructor;

/**
 * Phase 4: {@code Trades}/{@code TradeAttributes} merged into a single Dremio table/view ({@code
 * TradesMerged}, see the repo's Phase 4 evidence notes) - there is no {@code JoinDescription} at all here,
 * unlike the two-table model this was forked from, so none of the join-optionality/incremental-refresh
 * caveats documented there (see {@code DataMaintenanceController}'s javadoc on the two-table branch) apply.
 */
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

        final TableDescription tradesMergedTable =
                discoverer.discoverTable(new SqlTableId(NO_CATALOG, dremioProperties.getSpace(), TRADES_STORE_NAME));

        return SchemaDescription.builder()
                .externalTables(List.of(tradesMergedTable))
                .externalJoins(List.of())
                .build();
    }
}
