/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.AS_OF_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.BOOKS_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.FX_RATES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_SETS_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_ADD_ON_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.io.dlc.api.operations.request.scope.IDlcScope;
import com.activeviam.io.dlc.impl.description.source.LocalCsvSourceDescription;
import com.activeviam.io.dlc.impl.description.source.TupleSourceDescription;
import com.activeviam.io.dlc.impl.description.topic.CsvTopicDescription;
import com.activeviam.io.dlc.impl.description.topic.TupleTopicDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.ChannelDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.column.calc.AnonymousCustomFieldDescription;
import com.activeviam.io.dlc.impl.description.topic.channel.publish.AnonymousTargetDescription;
import com.activeviam.io.dlc.impl.utils.NamedEntityResolverService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SourceConfig {

    private final IDatastore datastore;

    private static final String TRADES_FILE_PATTERN = "glob:**trades*.csv";
    private static final String NETTING_SETS_FILE_PATTERN = "glob:**nettingSets*.csv";
    private static final String COUNTERPARTIES_FILE_PATTERN = "glob:**counterparties_all.csv";
    private static final String FX_RATES_FILE_PATTERN = "glob:**fxrates*.csv";
    private static final String BOOKS_FILE_PATTERN = "glob:**books_all.csv";

    public static final String DATE_FORMAT_FROM_FILE = "yyyyMMdd";

    @Bean
    CsvTopicDescription tradesCsvTopic(NamedEntityResolverService namedEntityResolver) {
        return CsvTopicDescription.builder(TRADES_STORE_NAME, TRADES_FILE_PATTERN)
                .channel(ChannelDescription.builder(namedEntityResolver.getTarget(TRADES_STORE_NAME))
                        .customField(addAsOfDateFromFileName())
                        .accumulate(true)
                        .build())
                .build();
    }

    @Bean
    CsvTopicDescription nettingSetsCsvTopic(NamedEntityResolverService namedEntityResolver) {
        return CsvTopicDescription.builder(NETTING_SETS_STORE_NAME, NETTING_SETS_FILE_PATTERN)
                .channel(ChannelDescription.builder(namedEntityResolver.getTarget(NETTING_SETS_STORE_NAME))
                        .customField(addAsOfDateFromFileName())
                        .build())
                .build();
    }

    @Bean
    CsvTopicDescription counterpartiesCsvTopic() {
        return CsvTopicDescription.builder(COUNTERPARTIES_STORE_NAME, COUNTERPARTIES_FILE_PATTERN)
                .build();
    }

    @Bean
    CsvTopicDescription fxRatesCsvTopic() {
        return CsvTopicDescription.builder(FX_RATES_STORE_NAME, FX_RATES_FILE_PATTERN)
                .channel(ChannelDescription.builder(generateAllFxRates())
                        .customField(addAsOfDateFromFileName())
                        .build())
                .build();
    }

    @Bean
    CsvTopicDescription booksCsvTopic() {
        return CsvTopicDescription.builder(BOOKS_STORE_NAME, BOOKS_FILE_PATTERN)
                .build();
    }

    @Bean
    TupleTopicDescription pfeAddOnTupleTopic(){
        return TupleTopicDescription.builder(PFE_ADD_ON_STORE_NAME,generatePFETuples())
                .build();
    }

    @Bean
    LocalCsvSourceDescription csvSource() {
        return LocalCsvSourceDescription.builder("ccr_csvSource", "data-ccr")
                .build();
    }

    @Bean
    TupleSourceDescription tupleSource() {
        return TupleSourceDescription.builder("ccr_tupleSource")
                .build();
    }

    private AnonymousCustomFieldDescription addAsOfDateFromFileName(){
        return new AnonymousCustomFieldDescription(scope -> new DateFromFileNameCalculator(AS_OF_DATE));
    }

    private AnonymousTargetDescription generateAllFxRates(){
        return new AnonymousTargetDescription(FX_RATES_STORE_NAME,scope -> new FXRateTuplePublisher(datastore));
    }

    private Function<IDlcScope, Collection<Object[]>> generatePFETuples(){
        return scope -> PFEFactory.generateTuples();
    }
}
