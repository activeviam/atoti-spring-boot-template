/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery.datamodel;

import static com.activeviam.apps.constants.FieldConstants.AS_OF_DATE;
import static com.activeviam.apps.constants.FieldConstants.HOLDING_ID;
import static com.activeviam.apps.constants.FieldConstants.PORTFOLIO;

import java.util.Set;

import org.springframework.context.annotation.Bean;

import com.activeviam.apps.cfg.database.DatabaseProperties;
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.apps.cfg.database.directquery.ClickHouseConfigurationProperties;
import com.activeviam.database.api.schema.ITableJoin;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.directquery.api.DirectQueryConnector;
import com.activeviam.directquery.api.discoverer.IDirectQueryTableDiscoverer;
import com.activeviam.directquery.api.schema.JoinDescription;
import com.activeviam.directquery.api.schema.TableDescription;
import com.activeviam.directquery.clickhouse.api.ClickhouseTableId;

public class TableDefinitionsConfig implements AggUpTableFactory {

    public static final String SCALED_VECTORS_TABLE = "ScaledVectorsView";

    public static final String SCALED_VECTORS_TO_HOLDING = "ScaledVectorsToHolding";

    public static final String MTM_VECTOR = "MtMVector";

    private final IDirectQueryTableDiscoverer directQueryTableDiscoverer;

    private final ClickHouseConfigurationProperties clickHouseConfigurationProperties;

    private final DatabaseProperties databaseProperties;

    private ClickhouseTableId tableId(String tableName) {
        return new ClickhouseTableId(clickHouseConfigurationProperties.getDatabase(), tableName);
    }

    public TableDefinitionsConfig(
            DirectQueryConnector<?> directQueryConnector,
            ClickHouseConfigurationProperties clickHouseConfigurationProperties,
            DatabaseProperties databaseProperties) {
        directQueryTableDiscoverer = directQueryConnector.getDiscoverer();
        this.clickHouseConfigurationProperties = clickHouseConfigurationProperties;
        this.databaseProperties = databaseProperties;
    }

    @Bean
    public TableDescription scaledVectorsTable() {
        return directQueryTableDiscoverer.discoverTable(tableId(SCALED_VECTORS_TABLE));
    }

    @Override
    @Bean
    public TableDescription asOfDateTable() {
        return directQueryTableDiscoverer.discoverTable(tableId(DatastoreConstants.AsOfDateStore.STORE_NAME));
    }

    @Override
    @Bean
    public TableDescription holdingTable() {
        return directQueryTableDiscoverer.discoverTable(tableId(DatastoreConstants.HoldingStore.STORE_NAME));
    }

    @Override
    @Bean
    public TableDescription holdingDetailTable() {
        return directQueryTableDiscoverer.discoverTable(tableId(DatastoreConstants.HoldingDetailStore.STORE_NAME));
    }

    @Override
    @Bean
    public TableDescription scaledStatResultTable() {
        return directQueryTableDiscoverer.discoverTable(tableId(DatastoreConstants.ScaledStatResultStore.STORE_NAME));
    }

    @Override
    @Bean
    public TableDescription securityTable() {
        return directQueryTableDiscoverer.discoverTable(tableId(DatastoreConstants.SecurityStore.STORE_NAME));
    }

    @Override
    @Bean
    public TableDescription positionDetailTable() {
        return directQueryTableDiscoverer.discoverTable(tableId(DatastoreConstants.PositionDetailStore.STORE_NAME));
    }

    @Override
    public IStoreDescription simReturnsStore() {
        return DatastoreConstants.SimReturnsStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    public IStoreDescription historicalSimReturnDatesStore() {
        return DatastoreConstants.HistoricalSimReturnDatesStore.storeDescription(
                databaseProperties.getInMemoryStores());
    }

    @Override
    public IStoreDescription statResultsStore() {
        return DatastoreConstants.StatResultsStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    public IStoreDescription statResultLookupStore() {
        return DatastoreConstants.StatResultLookupStore.storeDescription();
    }

    @Override
    public IStoreDescription fxResultsStore() {
        return DatastoreConstants.FxResultsStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    public IStoreDescription fxEquivalentsStore() {
        return DatastoreConstants.FxEquivalentsStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    public IStoreDescription fxEquivalentsLookupStore() {
        return DatastoreConstants.FxEquivalentsLookupStore.storeDescription();
    }

    @Override
    public IStoreDescription statisticBaseCurrencyStore() {
        return DatastoreConstants.StatisticBaseCurrencyStore.storeDescription();
    }

    @Override
    public IStoreDescription engineDimensionStore() {
        return DatastoreConstants.EngineDimensionStore.storeDescription();
    }

    @Override
    public IStoreDescription currenciesStore() {
        return DatastoreConstants.CurrencyStore.storeDescription();
    }

    @Override
    public IStoreDescription statFxAttributesStore() {
        return DatastoreConstants.StatFxAttributesStore.storeDescription();
    }

    @Override
    public IStoreDescription dimensionLevelAttributeStore() {
        return DatastoreConstants.DimensionLevelAttributeStore.storeDescription();
    }

    @Override
    public IStoreDescription fundLookThroughSecurityStore() {
        return DatastoreConstants.FundLookThroughSecurityStore.storeDescription();
    }

    @Override
    public IStoreDescription equityLookThroughSecurityStore() {
        return DatastoreConstants.EquityLookThroughSecurityStore.storeDescription();
    }

    @Override
    public IStoreDescription equityFuturesLookThroughSecurityStore() {
        return DatastoreConstants.EquityFuturesLookThroughSecurityStore.storeDescription();
    }

    @Bean
    public JoinDescription scaledVectorsToHoldingJoin() {
        return JoinDescription.builder()
                .sourceTableName(SCALED_VECTORS_TABLE)
                .targetTableName(DatastoreConstants.HoldingStore.STORE_NAME)
                .name(SCALED_VECTORS_TO_HOLDING)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(HOLDING_ID, DatastoreConstants.HoldingStore.Fields.HOLDING_ID),
                        new ITableJoin.FieldMapping(AS_OF_DATE, DatastoreConstants.HoldingStore.Fields.AS_OF_DATE),
                        new ITableJoin.FieldMapping(PORTFOLIO, DatastoreConstants.HoldingStore.Fields.PORTFOLIO)))
                .build();
    }

    @Override
    @Bean
    public JoinDescription holdingToHoldingDetailsJoin() {
        return JoinDescription.builder()
                .sourceTableName(DatastoreConstants.HoldingStore.STORE_NAME)
                .targetTableName(DatastoreConstants.HoldingDetailStore.STORE_NAME)
                .name(DatastoreConstants.References.HOLDING_TO_HOLDINGDETAIL)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingStore.Fields.BASE_HOLDING_ID,
                                DatastoreConstants.HoldingDetailStore.Fields.BASE_HOLDING_ID),
                        // FIXME review if we need partitioning key
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingStore.Fields.PARTITION_KEY,
                                DatastoreConstants.HoldingDetailStore.Fields.PARTITION_KEY),
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                                DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE)))
                .build();
    }

    @Override
    @Bean
    public JoinDescription holdingToScaledStatResultJoin() {
        return JoinDescription.builder()
                .sourceTableName(DatastoreConstants.HoldingStore.STORE_NAME)
                .targetTableName(DatastoreConstants.ScaledStatResultStore.STORE_NAME)
                .name(DatastoreConstants.References.HOLDING_TO_SCALEDSTATRESULT)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingStore.Fields.HOLDING_ID,
                                DatastoreConstants.ScaledStatResultStore.Fields.HOLDING_ID),
                        // FIXME review if we need partitioning key
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingStore.Fields.PARTITION_KEY,
                                DatastoreConstants.ScaledStatResultStore.Fields.PARTITION_KEY),
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                                DatastoreConstants.ScaledStatResultStore.Fields.AS_OF_DATE)))
                .build();
    }

    @Override
    @Bean
    public JoinDescription holdingDetailToSecurityJoin() {
        return JoinDescription.builder()
                .sourceTableName(DatastoreConstants.HoldingDetailStore.STORE_NAME)
                .targetTableName(DatastoreConstants.SecurityStore.STORE_NAME)
                .name(DatastoreConstants.References.HOLDINGDETAIL_TO_SECURITY)
                .fieldMappings(Set.of(
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingDetailStore.Fields.SECURITY,
                                DatastoreConstants.SecurityStore.Fields.SECURITY_NAME),
                        // FIXME review if we need partitioning key
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingDetailStore.Fields.PARTITION_KEY,
                                DatastoreConstants.SecurityStore.Fields.PARTITION_KEY),
                        new ITableJoin.FieldMapping(
                                DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE,
                                DatastoreConstants.SecurityStore.Fields.AS_OF_DATE)))
                .build();
    }

    @Override
    @Bean
    public JoinDescription holdingDetailToPositionDetailJoin() {
        return JoinDescription.builder()
                .sourceTableName(DatastoreConstants.HoldingDetailStore.STORE_NAME)
                .targetTableName(DatastoreConstants.PositionDetailStore.STORE_NAME)
                .name(DatastoreConstants.References.HOLDINGDETAIL_TO_POSITIONDETAIL)
                .fieldMappings(Set.of(new ITableJoin.FieldMapping(
                        DatastoreConstants.HoldingDetailStore.Fields.SECURITY,
                        DatastoreConstants.PositionDetailStore.Fields.SECURITY_NAME)))
                .build();
    }

    @Override
    @Bean
    public JoinDescription holdingToAsOfDateJoin() {
        return JoinDescription.builder()
                .sourceTableName(DatastoreConstants.HoldingStore.STORE_NAME)
                .targetTableName(DatastoreConstants.AsOfDateStore.STORE_NAME)
                .name(DatastoreConstants.References.HOLDING_TO_ASOFDATE)
                .fieldMappings(Set.of(new ITableJoin.FieldMapping(
                        DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                        DatastoreConstants.AsOfDateStore.Fields.AS_OF_DATE)))
                .build();
    }
}
