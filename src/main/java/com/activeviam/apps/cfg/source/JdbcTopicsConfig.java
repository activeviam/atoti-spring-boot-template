/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.cfg.source.DlcConfig.AS_OF_DATE_SCOPE_PARAMETER;
import static com.activeviam.apps.constants.FieldConstants.AS_OF_DATE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.io.dlc.api.description.topic.channel.column.calc.ICustomFieldDescription;
import com.activeviam.io.dlc.impl.description.topic.JdbcTopicDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.ChannelDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.column.calc.AnonymousCustomFieldDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.column.calc.CustomFieldDescription;
import com.activeviam.io.dlc.impl.utils.NamedEntityResolverService;
import com.activeviam.source.jdbc.api.calculator.LocalDateJdbcColumnCalculator;
import com.activeviam.source.jdbc.api.calculator.VectorColumnCalculatorFactory;
import com.activeviam.tech.chunks.api.types.ContentType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@ConditionalOnApplicationWithDatastore
@Slf4j
public class JdbcTopicsConfig {

    private static final String AS_OF_DATE_SQL_PARSER = "asOfDateSqlParser";

    @Bean
    CustomFieldDescription cobDateJdbcParser() {
        return CustomFieldDescription.of(AS_OF_DATE_SQL_PARSER, scope -> new LocalDateJdbcColumnCalculator(AS_OF_DATE));
    }

    public static final String HOLDING_FILE_SQL = "SELECT * FROM AggUp.Holding WHERE AsOfDate = ?";

    @Bean
    JdbcTopicDescription holdingTopic(NamedEntityResolverService namedEntityResolverService) {
        return JdbcTopicDescription.builder(DatastoreConstants.HoldingStore.STORE_NAME, HOLDING_FILE_SQL)
                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(
                                namedEntityResolverService.getTarget(DatastoreConstants.HoldingStore.STORE_NAME))
                        .customFields(namedEntityResolverService.getCustomFields(Set.of(AS_OF_DATE_SQL_PARSER)))
                        .build())
                .build();
    }

    public static final String HOLDING_DETAIL_SQL = "SELECT * FROM AggUp.HoldingDetail WHERE AsOfDate = ?";

    @Bean
    JdbcTopicDescription holdingDetailTopic(NamedEntityResolverService namedEntityResolverService) {
        return JdbcTopicDescription.builder(DatastoreConstants.HoldingDetailStore.STORE_NAME, HOLDING_DETAIL_SQL)
                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(
                                namedEntityResolverService.getTarget(DatastoreConstants.HoldingDetailStore.STORE_NAME))
                        .customFields(namedEntityResolverService.getCustomFields(Set.of(AS_OF_DATE_SQL_PARSER)))
                        .build())
                .build();
    }

    public static final String AS_OF_DATE_SQL = "SELECT * FROM AggUp.AsOfDate WHERE AsOfDate = ?";

    @Bean
    JdbcTopicDescription asOfDateTopic(NamedEntityResolverService namedEntityResolverService) {
        return JdbcTopicDescription.builder(DatastoreConstants.AsOfDateStore.STORE_NAME, AS_OF_DATE_SQL)
                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(
                                namedEntityResolverService.getTarget(DatastoreConstants.AsOfDateStore.STORE_NAME))
                        .customFields(namedEntityResolverService.getCustomFields(Set.of(AS_OF_DATE_SQL_PARSER)))
                        .build())
                .build();
    }

    public static final String SCALED_STAT_RESULT_SQL = "SELECT * FROM AggUp.ScaledStatResult WHERE AsOfDate = ?";

    @Bean
    JdbcTopicDescription scaledStatResultTopic(NamedEntityResolverService namedEntityResolverService) {
        var customFields = new HashSet<ICustomFieldDescription>(
                namedEntityResolverService.getCustomFields(Set.of(AS_OF_DATE_SQL_PARSER)));
        customFields.add(AnonymousCustomFieldDescription.builder()
                .columnCalculatorFactory(scope -> VectorColumnCalculatorFactory.createForNativeRows(
                        DatastoreConstants.ScaledStatResultStore.Fields.RESULT_VALUES_SUM,
                        VectorColumnCalculatorFactory.ResultSetArrayFormat.JDBC_ARRAY,
                        ContentType.DOUBLE_ARRAY))
                .build());
        customFields.add(AnonymousCustomFieldDescription.builder()
                .columnCalculatorFactory(scope -> VectorColumnCalculatorFactory.createForNativeRows(
                        DatastoreConstants.ScaledStatResultStore.Fields.RESULT_VALUES_PASSTHROUGH,
                        VectorColumnCalculatorFactory.ResultSetArrayFormat.JDBC_ARRAY,
                        ContentType.DOUBLE_ARRAY))
                .build());
        customFields.add(AnonymousCustomFieldDescription.builder()
                .columnCalculatorFactory(scope -> VectorColumnCalculatorFactory.createForNativeRows(
                        "AGGSVC_SIMRETURNS(1Y VS)_MONTECARLO",
                        VectorColumnCalculatorFactory.ResultSetArrayFormat.JDBC_ARRAY,
                        ContentType.DOUBLE_ARRAY))
                .build());
        return JdbcTopicDescription.builder(DatastoreConstants.ScaledStatResultStore.STORE_NAME, SCALED_STAT_RESULT_SQL)
                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(namedEntityResolverService.getTarget(
                                DatastoreConstants.ScaledStatResultStore.STORE_NAME))
                        .customFields(customFields)
                        .build())
                .build();
    }

    public static final String SECURITY_SQL = "SELECT * FROM AggUp.Security WHERE AsOfDate = ?";

    @Bean
    JdbcTopicDescription securityTopic(NamedEntityResolverService namedEntityResolverService) {
        return JdbcTopicDescription.builder(DatastoreConstants.SecurityStore.STORE_NAME, SECURITY_SQL)
                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(
                                namedEntityResolverService.getTarget(DatastoreConstants.SecurityStore.STORE_NAME))
                        .customFields(namedEntityResolverService.getCustomFields(Set.of(AS_OF_DATE_SQL_PARSER)))
                        .build())
                .build();
    }

    public static final String SIM_RETURNS_SQL = "SELECT * FROM AggUp.SimReturns WHERE AsOfDate = ?";

    @Bean
    JdbcTopicDescription simReturnsTopic(NamedEntityResolverService namedEntityResolverService) {
        var customFields = new HashSet<ICustomFieldDescription>(
                namedEntityResolverService.getCustomFields(Set.of(AS_OF_DATE_SQL_PARSER)));
        customFields.add(AnonymousCustomFieldDescription.builder()
                .columnCalculatorFactory(scope -> VectorColumnCalculatorFactory.createForNativeRows(
                        DatastoreConstants.SimReturnsStore.Fields.VECTOR,
                        VectorColumnCalculatorFactory.ResultSetArrayFormat.JDBC_ARRAY,
                        ContentType.DOUBLE_ARRAY))
                .build());
        return JdbcTopicDescription.builder(DatastoreConstants.SimReturnsStore.STORE_NAME, SIM_RETURNS_SQL)
                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(
                                namedEntityResolverService.getTarget(DatastoreConstants.SimReturnsStore.STORE_NAME))
                        .customFields(customFields)
                        .build())
                .build();
    }

    //    public static final String HISTORICAL_SIM_RETURNS_SQL = "SELECT * FROM HistoricalSimReturns WHERE AsOfDate =
    // ?";
    //
    //    @Bean
    //    JdbcTopicDescription historicalSimReturnsTopic() {
    //        return JdbcTopicDescription.builder(
    //                        DatastoreConstants.HistoricalSimReturnDatesStore.STORE_NAME, HISTORICAL_SIM_RETURNS_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    public static final String STAT_RESULTS_SQL = "SELECT * FROM AggUp.StatResults WHERE AsOfDate = ?";

    @Bean
    JdbcTopicDescription statResultsTopic(NamedEntityResolverService namedEntityResolverService) {
        var customFields = new HashSet<ICustomFieldDescription>(
                namedEntityResolverService.getCustomFields(Set.of(AS_OF_DATE_SQL_PARSER)));
        customFields.add(AnonymousCustomFieldDescription.builder()
                .columnCalculatorFactory(scope -> VectorColumnCalculatorFactory.createForNativeRows(
                        DatastoreConstants.StatResultsStore.Fields.RESULT_VALUES_SUM,
                        VectorColumnCalculatorFactory.ResultSetArrayFormat.JDBC_ARRAY,
                        ContentType.DOUBLE_ARRAY))
                .build());
        customFields.add(AnonymousCustomFieldDescription.builder()
                .columnCalculatorFactory(scope -> VectorColumnCalculatorFactory.createForNativeRows(
                        DatastoreConstants.StatResultsStore.Fields.RESULT_VALUES_PASSTHROUGH,
                        VectorColumnCalculatorFactory.ResultSetArrayFormat.JDBC_ARRAY,
                        ContentType.DOUBLE_ARRAY))
                .build());
        return JdbcTopicDescription.builder(DatastoreConstants.StatResultsStore.STORE_NAME, STAT_RESULTS_SQL)
                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
                .channel(ChannelDescription.builder(
                                namedEntityResolverService.getTarget(DatastoreConstants.StatResultsStore.STORE_NAME))
                        .customFields(customFields)
                        .build())
                .build();
    }

    public static final String STAT_RESULT_LOOKUP_SQL = "SELECT * FROM AggUp.StatResultLookup";

    @Bean
    JdbcTopicDescription statResultLookupTopic() {
        return JdbcTopicDescription.builder(DatastoreConstants.StatResultLookupStore.STORE_NAME, STAT_RESULT_LOOKUP_SQL)
                .build();
    }

    public static final String POSITION_DETAIL_SQL = "SELECT * FROM AggUp.PositionDetail";

    @Bean
    JdbcTopicDescription positionDetailTopic() {
        return JdbcTopicDescription.builder(DatastoreConstants.PositionDetailStore.STORE_NAME, POSITION_DETAIL_SQL)
                .build();
    }

    //    public static final String FX_RESULTS_SQL = "SELECT * FROM FXResults WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription fxResultsTopic() {
    //        return JdbcTopicDescription.builder(DatastoreConstants.FxResultsStore.STORE_NAME, FX_RESULTS_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    //    public static final String FX_EQUIVALENTS_SQL = "SELECT * FROM FXEquivalents WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription fxEquivalentsTopic() {
    //        return JdbcTopicDescription.builder(DatastoreConstants.FxEquivalentsStore.STORE_NAME, FX_EQUIVALENTS_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    //    public static final String FX_EQUIVALENTS_LOOKUP_SQL = "SELECT * FROM FXEquivalentsLookup WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription fxEquivalentsLookupTopic() {
    //        return JdbcTopicDescription.builder(
    //                        DatastoreConstants.FxEquivalentsLookupStore.STORE_NAME, FX_EQUIVALENTS_LOOKUP_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    public static final String STATISTIC_BASE_CURRENCY_SQL = "SELECT * FROM AggUp.StatisticBaseCurrency";

    @Bean
    JdbcTopicDescription statisticBaseCurrencyTopic() {
        return JdbcTopicDescription.builder(
                        DatastoreConstants.StatisticBaseCurrencyStore.STORE_NAME, STATISTIC_BASE_CURRENCY_SQL)
                .build();
    }

    //    public static final String ENGINE_DIMENSION_SQL = "SELECT * FROM EngineDimension WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription engineDimensionTopic() {
    //        return JdbcTopicDescription.builder(DatastoreConstants.EngineDimensionStore.STORE_NAME,
    // ENGINE_DIMENSION_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    public static final String CURRENCY_SQL = "SELECT * FROM AggUp.Currency";

    @Bean
    JdbcTopicDescription currencyTopic() {
        return JdbcTopicDescription.builder(DatastoreConstants.CurrencyStore.STORE_NAME, CURRENCY_SQL)
                .build();
    }

    //    public static final String STAT_FX_ATTRIBUTES_SQL = "SELECT * FROM StatFXAttributes WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription statFxAttributesTopic() {
    //        return JdbcTopicDescription.builder(DatastoreConstants.StatFxAttributesStore.STORE_NAME,
    // STAT_FX_ATTRIBUTES_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    //    public static final String DIMENSION_LEVEL_ATTRIBUTE_SQL =
    //            "SELECT * FROM DimensionLevelAttributes WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription dimensionLevelAttributeTopic() {
    //        return JdbcTopicDescription.builder(
    //                        DatastoreConstants.DimensionLevelAttributeStore.STORE_NAME, DIMENSION_LEVEL_ATTRIBUTE_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    //    public static final String FUND_LOOK_THROUGH_SECURITY_SQL =
    //            "SELECT * FROM FundLookThroughSecurity WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription fundLookThroughSecurityTopic() {
    //        return JdbcTopicDescription.builder(
    //                        DatastoreConstants.FundLookThroughSecurityStore.STORE_NAME,
    // FUND_LOOK_THROUGH_SECURITY_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    //    public static final String EQUITY_LOOK_THROUGH_SECURITY_SQL =
    //            "SELECT * FROM EquityLookThroughSecurity WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription equityLookThroughSecurityTopic() {
    //        return JdbcTopicDescription.builder(
    //                        DatastoreConstants.EquityLookThroughSecurityStore.STORE_NAME,
    // EQUITY_LOOK_THROUGH_SECURITY_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }

    //    public static final String EQUITY_FUTURES_LOOK_THROUGH_SECURITY_SQL =
    //            "SELECT * FROM EquityFuturesLookThroughSecurity WHERE AsOfDate = ?";
    //
    //    @Bean
    //    JdbcTopicDescription equityFuturesLookThroughSecurityTopic() {
    //        return JdbcTopicDescription.builder(
    //                        DatastoreConstants.EquityFuturesLookThroughSecurityStore.STORE_NAME,
    //                        EQUITY_FUTURES_LOOK_THROUGH_SECURITY_SQL)
    //                .parameterOrder(List.of(AS_OF_DATE_SCOPE_PARAMETER))
    //                .build();
    //    }
}
