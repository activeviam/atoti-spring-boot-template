/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.io.dlc.impl.description.AliasesDescription;

@Configuration
public class DlcConfig {

    public static final String ALL_IN_MEMORY_TOPICS = "allInMemoryTopics";

    public static final String DQ_IN_MEMORY_TOPICS = "dqInMemoryTopics";

    public static final String AS_OF_DATE_SCOPE_PARAMETER = "AS_OF_DATE";

    @Bean
    AliasesDescription aliases() {
        return new AliasesDescription(Map.of(
                ALL_IN_MEMORY_TOPICS,
                Set.of(
                        DatastoreConstants.AsOfDateStore.STORE_NAME,
                        DatastoreConstants.CurrencyStore.STORE_NAME,
                        DatastoreConstants.HoldingStore.STORE_NAME,
                        DatastoreConstants.HoldingDetailStore.STORE_NAME,
                        DatastoreConstants.PositionDetailStore.STORE_NAME,
                        DatastoreConstants.ScaledStatResultStore.STORE_NAME,
                        DatastoreConstants.SecurityStore.STORE_NAME,
                        DatastoreConstants.StatisticBaseCurrencyStore.STORE_NAME,
                        DatastoreConstants.StatResultLookupStore.STORE_NAME,
                        DatastoreConstants.StatResultsStore.STORE_NAME,
                        DatastoreConstants.SimReturnsStore.STORE_NAME
                        //                        DatastoreConstants.HistoricalSimReturnDatesStore.STORE_NAME,
                        //                        DatastoreConstants.FxResultsStore.STORE_NAME,
                        //                        DatastoreConstants.FxEquivalentsStore.STORE_NAME,
                        //                        DatastoreConstants.FxEquivalentsLookupStore.STORE_NAME,
                        //                        DatastoreConstants.EngineDimensionStore.STORE_NAME,
                        //                        DatastoreConstants.StatFxAttributesStore.STORE_NAME,
                        //                        DatastoreConstants.DimensionLevelAttributeStore.STORE_NAME,
                        //                        DatastoreConstants.FundLookThroughSecurityStore.STORE_NAME,
                        //                        DatastoreConstants.EquityLookThroughSecurityStore.STORE_NAME,
                        //                        DatastoreConstants.EquityFuturesLookThroughSecurityStore.STORE_NAME
                        )
                //                ,
                //                DQ_IN_MEMORY_TOPICS,
                //                Set.of(
                //                        DatastoreConstants.SimReturnsStore.STORE_NAME,
                //                        DatastoreConstants.HistoricalSimReturnDatesStore.STORE_NAME,
                //                        DatastoreConstants.StatResultsStore.STORE_NAME,
                //                        DatastoreConstants.StatResultLookupStore.STORE_NAME,
                //                        DatastoreConstants.FxResultsStore.STORE_NAME,
                //                        DatastoreConstants.FxEquivalentsStore.STORE_NAME,
                //                        DatastoreConstants.FxEquivalentsLookupStore.STORE_NAME,
                //                        DatastoreConstants.StatisticBaseCurrencyStore.STORE_NAME,
                //                        DatastoreConstants.EngineDimensionStore.STORE_NAME,
                //                        DatastoreConstants.CurrencyStore.STORE_NAME,
                //                        DatastoreConstants.StatFxAttributesStore.STORE_NAME,
                //                        DatastoreConstants.DimensionLevelAttributeStore.STORE_NAME,
                //                        DatastoreConstants.FundLookThroughSecurityStore.STORE_NAME,
                //                        DatastoreConstants.EquityLookThroughSecurityStore.STORE_NAME,
                //                        DatastoreConstants.EquityFuturesLookThroughSecurityStore.STORE_NAME)
                ));
    }
}
