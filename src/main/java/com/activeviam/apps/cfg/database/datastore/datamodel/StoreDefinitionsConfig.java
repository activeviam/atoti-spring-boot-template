/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.datastore.datamodel;

import org.springframework.context.annotation.Bean;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.apps.cfg.database.DatabaseProperties;
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StoreDefinitionsConfig implements AggUpStoreFactory {

    private final DatabaseProperties databaseProperties;

    @Override
    @Bean
    public IStoreDescription asOfDateStore() {
        return DatastoreConstants.AsOfDateStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription holdingStore() {
        return DatastoreConstants.HoldingStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription holdingDetailStore() {

        return DatastoreConstants.HoldingDetailStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription scaledStatResultStore() {

        return DatastoreConstants.ScaledStatResultStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription securityStore() {
        return DatastoreConstants.SecurityStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription positionDetailStore() {
        return DatastoreConstants.PositionDetailStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription statResultLookupStore() {
        return DatastoreConstants.StatResultLookupStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription statResultsStore() {
        return DatastoreConstants.StatResultsStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription simReturnsStore() {
        return DatastoreConstants.SimReturnsStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription historicalSimReturnDatesStore() {
        return DatastoreConstants.HistoricalSimReturnDatesStore.storeDescription(
                databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription fxResultsStore() {
        return DatastoreConstants.FxResultsStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription fxEquivalentsStore() {
        return DatastoreConstants.FxEquivalentsStore.storeDescription(databaseProperties.getInMemoryStores());
    }

    @Override
    @Bean
    public IStoreDescription fxEquivalentsLookupStore() {
        return DatastoreConstants.FxEquivalentsLookupStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription statisticBaseCurrencyStore() {
        return DatastoreConstants.StatisticBaseCurrencyStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription engineDimensionStore() {
        return DatastoreConstants.EngineDimensionStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription currenciesStore() {
        return DatastoreConstants.CurrencyStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription dimensionLevelAttributeStore() {
        return DatastoreConstants.DimensionLevelAttributeStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription fundLookThroughSecurityStore() {
        return DatastoreConstants.FundLookThroughSecurityStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription equityLookThroughSecurityStore() {
        return DatastoreConstants.EquityLookThroughSecurityStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription equityFuturesLookThroughSecurityStore() {
        return DatastoreConstants.EquityFuturesLookThroughSecurityStore.storeDescription();
    }

    @Override
    @Bean
    public IStoreDescription statFxAttributesStore() {
        return DatastoreConstants.StatFxAttributesStore.storeDescription();
    }

    @Override
    @Bean
    public IReferenceDescription holdingToHoldingDetailsReference() {
        return StartBuilding.reference()
                .fromStore(DatastoreConstants.HoldingStore.STORE_NAME)
                .toStore(DatastoreConstants.HoldingDetailStore.STORE_NAME)
                .withName(DatastoreConstants.References.HOLDING_TO_HOLDINGDETAIL)
                .withMapping(
                        DatastoreConstants.HoldingStore.Fields.BASE_HOLDING_ID,
                        DatastoreConstants.HoldingDetailStore.Fields.BASE_HOLDING_ID)
                .withMapping(
                        DatastoreConstants.HoldingStore.Fields.PARTITION_KEY,
                        DatastoreConstants.HoldingDetailStore.Fields.PARTITION_KEY)
                .withMapping(
                        DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                        DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE)
                .build();
    }

    @Override
    @Bean
    public IReferenceDescription holdingToScaledStatResultReference() {
        return StartBuilding.reference()
                .fromStore(DatastoreConstants.HoldingStore.STORE_NAME)
                .toStore(DatastoreConstants.ScaledStatResultStore.STORE_NAME)
                .withName(DatastoreConstants.References.HOLDING_TO_SCALEDSTATRESULT)
                .withMapping(
                        DatastoreConstants.HoldingStore.Fields.HOLDING_ID,
                        DatastoreConstants.ScaledStatResultStore.Fields.HOLDING_ID)
                .withMapping(
                        DatastoreConstants.HoldingStore.Fields.PARTITION_KEY,
                        DatastoreConstants.ScaledStatResultStore.Fields.PARTITION_KEY)
                .withMapping(
                        DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                        DatastoreConstants.ScaledStatResultStore.Fields.AS_OF_DATE)
                .build();
    }

    @Override
    @Bean
    public IReferenceDescription holdingDetailToSecurityReference() {
        return StartBuilding.reference()
                .fromStore(DatastoreConstants.HoldingDetailStore.STORE_NAME)
                .toStore(DatastoreConstants.SecurityStore.STORE_NAME)
                .withName(DatastoreConstants.References.HOLDINGDETAIL_TO_SECURITY)
                .withMapping(
                        DatastoreConstants.HoldingDetailStore.Fields.SECURITY,
                        DatastoreConstants.SecurityStore.Fields.SECURITY_NAME)
                .withMapping(
                        DatastoreConstants.HoldingDetailStore.Fields.PARTITION_KEY,
                        DatastoreConstants.SecurityStore.Fields.PARTITION_KEY)
                .withMapping(
                        DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE,
                        DatastoreConstants.SecurityStore.Fields.AS_OF_DATE)
                .dontIndexOwner()
                .build();
    }

    @Override
    @Bean
    public IReferenceDescription holdingDetailToPositionDetailReference() {
        return StartBuilding.reference()
                .fromStore(DatastoreConstants.HoldingDetailStore.STORE_NAME)
                .toStore(DatastoreConstants.PositionDetailStore.STORE_NAME)
                .withName(DatastoreConstants.References.HOLDINGDETAIL_TO_POSITIONDETAIL)
                .withMapping(
                        DatastoreConstants.HoldingDetailStore.Fields.SECURITY,
                        DatastoreConstants.PositionDetailStore.Fields.SECURITY_NAME)
                .dontIndexOwner()
                .build();
    }

    @Override
    @Bean
    public IReferenceDescription holdingToAsOfDateReference() {
        return StartBuilding.reference()
                .fromStore(DatastoreConstants.HoldingStore.STORE_NAME)
                .toStore(DatastoreConstants.AsOfDateStore.STORE_NAME)
                .withName(DatastoreConstants.References.HOLDING_TO_ASOFDATE)
                .withMapping(
                        DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                        DatastoreConstants.AsOfDateStore.Fields.AS_OF_DATE)
                .build();
    }
}
