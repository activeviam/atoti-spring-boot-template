/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.io.dlc.impl.description.topic.CsvTopicDescription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@ConditionalOnApplicationWithDatastore
@Slf4j
public class CsvTopicsConfig {

    public static final String HOLDING_FILE_PATTERN = "glob:Holding.csv";

    @Bean
    CsvTopicDescription holdingTopic() {
        return CsvTopicDescription.builder(DatastoreConstants.HoldingStore.STORE_NAME, HOLDING_FILE_PATTERN)
                .build();
    }

    public static final String HOLDING_DETAIL_FILE_PATTERN = "glob:HoldingDetail.csv";

    @Bean
    CsvTopicDescription holdingDetailTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.HoldingDetailStore.STORE_NAME, HOLDING_DETAIL_FILE_PATTERN)
                .build();
    }

    public static final String AS_OF_DATE_FILE_PATTERN = "glob:AsOfDate.csv";

    @Bean
    CsvTopicDescription asOfDateTopic() {
        return CsvTopicDescription.builder(DatastoreConstants.AsOfDateStore.STORE_NAME, AS_OF_DATE_FILE_PATTERN)
                .build();
    }

    public static final String SCALED_STAT_RESULT_FILE_PATTERN = "glob:ScaledStatResult.csv";

    @Bean
    CsvTopicDescription scaledStatResultTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.ScaledStatResultStore.STORE_NAME, SCALED_STAT_RESULT_FILE_PATTERN)
                .build();
    }

    public static final String SECURITY_FILE_PATTERN = "glob:Security.csv";

    @Bean
    CsvTopicDescription securityTopic() {
        return CsvTopicDescription.builder(DatastoreConstants.SecurityStore.STORE_NAME, SECURITY_FILE_PATTERN)
                .build();
    }

    public static final String SIM_RETURNS_FILE_PATTERN = "glob:SimReturns.csv";

    @Bean
    CsvTopicDescription simReturnsTopic() {
        return CsvTopicDescription.builder(DatastoreConstants.SimReturnsStore.STORE_NAME, SIM_RETURNS_FILE_PATTERN)
                .build();
    }

    public static final String HISTORICAL_SIM_RETURNS_FILE_PATTERN = "glob:HistoricalSimReturns.csv";

    @Bean
    CsvTopicDescription historicalSimReturnsTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.HistoricalSimReturnDatesStore.STORE_NAME,
                        HISTORICAL_SIM_RETURNS_FILE_PATTERN)
                .build();
    }

    public static final String STAT_RESULTS_FILE_PATTERN = "glob:StatResults.csv";

    @Bean
    CsvTopicDescription statResultsTopic() {
        return CsvTopicDescription.builder(DatastoreConstants.StatResultsStore.STORE_NAME, STAT_RESULTS_FILE_PATTERN)
                .build();
    }

    public static final String STAT_RESULT_LOOKUP_FILE_PATTERN = "glob:StatResultLookup.csv";

    @Bean
    CsvTopicDescription statResultLookupTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.StatResultLookupStore.STORE_NAME, STAT_RESULT_LOOKUP_FILE_PATTERN)
                .build();
    }

    public static final String POSITION_DELTA_FILE_PATTERN = "glob:PositionDelta.csv";

    @Bean
    CsvTopicDescription positionDeltaTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.PositionDetailStore.STORE_NAME, POSITION_DELTA_FILE_PATTERN)
                .build();
    }

    public static final String FX_RESULTS_FILE_PATTERN = "glob:FxResults.csv";

    @Bean
    CsvTopicDescription fxResultsTopic() {
        return CsvTopicDescription.builder(DatastoreConstants.FxResultsStore.STORE_NAME, FX_RESULTS_FILE_PATTERN)
                .build();
    }

    public static final String FX_EQUIVALENTS_FILE_PATTERN = "glob:FxEquivalents.csv";

    @Bean
    CsvTopicDescription fxEquivalentsTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.FxEquivalentsStore.STORE_NAME, FX_EQUIVALENTS_FILE_PATTERN)
                .build();
    }

    public static final String FX_EQUIVALENTS_LOOKUP_FILE_PATTERN = "glob:FxEquivalentsLookup.csv";

    @Bean
    CsvTopicDescription fxEquivalentsLookupTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.FxEquivalentsLookupStore.STORE_NAME, FX_EQUIVALENTS_LOOKUP_FILE_PATTERN)
                .build();
    }

    public static final String STATISTIC_BASE_CURRENCY_FILE_PATTERN = "glob:StatisticBaseCurrency.csv";

    @Bean
    CsvTopicDescription statisticBaseCurrencyTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.StatisticBaseCurrencyStore.STORE_NAME, STATISTIC_BASE_CURRENCY_FILE_PATTERN)
                .build();
    }

    public static final String ENGINE_DIMENSION_FILE_PATTERN = "glob:EngineDimension.csv";

    @Bean
    CsvTopicDescription engineDimensionTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.EngineDimensionStore.STORE_NAME, ENGINE_DIMENSION_FILE_PATTERN)
                .build();
    }

    public static final String CURRENCIES_FILE_PATTERN = "glob:Currencies.csv";

    @Bean
    CsvTopicDescription currenciesTopic() {
        return CsvTopicDescription.builder(DatastoreConstants.CurrencyStore.STORE_NAME, CURRENCIES_FILE_PATTERN)
                .build();
    }

    public static final String STAT_FX_ATTRIBUTES_FILE_PATTERN = "glob:StatFxAttributes.csv";

    @Bean
    CsvTopicDescription statFxAttributesTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.StatFxAttributesStore.STORE_NAME, STAT_FX_ATTRIBUTES_FILE_PATTERN)
                .build();
    }

    public static final String DIMENSION_LEVEL_ATTRIBUTE_FILE_PATTERN = "glob:DimensionLevelAttribute.csv";

    @Bean
    CsvTopicDescription dimensionLevelAttributeTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.DimensionLevelAttributeStore.STORE_NAME,
                        DIMENSION_LEVEL_ATTRIBUTE_FILE_PATTERN)
                .build();
    }

    public static final String FUND_LOOK_THROUGH_SECURITY_FILE_PATTERN = "glob:FundLookThroughSecurity.csv";

    @Bean
    CsvTopicDescription fundLookThroughSecurityTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.FundLookThroughSecurityStore.STORE_NAME,
                        FUND_LOOK_THROUGH_SECURITY_FILE_PATTERN)
                .build();
    }

    public static final String EQUITY_LOOK_THROUGH_SECURITY_FILE_PATTERN = "glob:EquityLookThroughSecurity.csv";

    @Bean
    CsvTopicDescription equityLookThroughSecurityTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.EquityLookThroughSecurityStore.STORE_NAME,
                        EQUITY_LOOK_THROUGH_SECURITY_FILE_PATTERN)
                .build();
    }

    public static final String EQUITY_FUTURES_LOOK_THROUGH_SECURITY_FILE_PATTERN =
            "glob:EquityFuturesLookThroughSecurity.csv";

    @Bean
    CsvTopicDescription equityFuturesLookThroughSecurityTopic() {
        return CsvTopicDescription.builder(
                        DatastoreConstants.EquityFuturesLookThroughSecurityStore.STORE_NAME,
                        EQUITY_FUTURES_LOOK_THROUGH_SECURITY_FILE_PATTERN)
                .build();
    }
}
