/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.datastore.datamodel;

import static com.activeviam.database.api.types.ILiteralType.DOUBLE;
import static com.activeviam.database.api.types.ILiteralType.LOCAL_DATE;
import static com.activeviam.database.api.types.ILiteralType.STRING;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IAxisMember;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.DuplicateKeyHandlers;
import com.activeviam.database.datastore.api.description.impl.ReferenceDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;

@Configuration
public class StoresConfiguration {

    public static String referenceName(String from, String to) {
        return String.format("%s_to_%s", from, to);
    }

    @Bean
    public IStoreDescription createTradesStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(StoreAndFieldConstants.TRADES_STORE_NAME)
                .withField(StoreAndFieldConstants.TRADE_ID).asKeyField()
                .withField(StoreAndFieldConstants.PRODUCT)
                .withField(StoreAndFieldConstants.NETTING_SET_ID)
                .withField(StoreAndFieldConstants.BOOK_ID)
                .withField(StoreAndFieldConstants.DIRECTION)
                .withField(StoreAndFieldConstants.TRADE_INPUT_CCY)
                .withField(StoreAndFieldConstants.INSTRUMENT)
                .withField(StoreAndFieldConstants.ASSET_CLASS)
                .withField(StoreAndFieldConstants.SUBCLASS,STRING, IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                .withField(StoreAndFieldConstants.OPTION_TYPE,STRING,IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                .withField(StoreAndFieldConstants.UNDERLYING, STRING, IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                .withField(StoreAndFieldConstants.MATURITY,LOCAL_DATE)
                .withField(StoreAndFieldConstants.NOTIONAL,DOUBLE)
                .withField(StoreAndFieldConstants.MARKET_VALUE,DOUBLE)
                .withField(StoreAndFieldConstants.PFE_SUBCATEGORY,STRING)
                .withField(StoreAndFieldConstants.AS_OF_DATE, LOCAL_DATE).asKeyField()
                .withDuplicateKeyHandler(DuplicateKeyHandlers.LOG_WITHIN_TRANSACTION)
                .withModuloPartitioning(Runtime.getRuntime().availableProcessors(),StoreAndFieldConstants.TRADE_ID)
                .build();
    }

    @Bean
    public IStoreDescription createNettingSetsStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(StoreAndFieldConstants.NETTING_SETS_STORE_NAME)
                .withField(StoreAndFieldConstants.NETTING_SET_ID).asKeyField()
                .withField(StoreAndFieldConstants.NETTING_NAME)
                .withField(StoreAndFieldConstants.NETTING_TYPE)
                .withField(StoreAndFieldConstants.COUNTERPARTY_ID)
                .withField(StoreAndFieldConstants.COLLATERAL,DOUBLE)
                .withField(StoreAndFieldConstants.MTA,DOUBLE)
                .withField(StoreAndFieldConstants.NETTING_INPUT_CCY)
                .withField(StoreAndFieldConstants.AS_OF_DATE,LOCAL_DATE).asKeyField()
                .withDuplicateKeyHandler(DuplicateKeyHandlers.LOG_WITHIN_TRANSACTION)
                .build();
    }

    @Bean
    public IStoreDescription createCounterpartiesStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME)
                .withField(StoreAndFieldConstants.COUNTERPARTY_ID).asKeyField()
                .withField(StoreAndFieldConstants.COUNTERPARTY_NAME)
                .withField(StoreAndFieldConstants.RATING)
                .withField(StoreAndFieldConstants.SECTOR)
                .withField(StoreAndFieldConstants.COUNTRY_OF_RISK)
                .withDuplicateKeyHandler(DuplicateKeyHandlers.LOG_WITHIN_TRANSACTION)
                .build();
    }

    @Bean
    public IStoreDescription createFXRatesStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(StoreAndFieldConstants.FX_RATES_STORE_NAME)
                .withField(StoreAndFieldConstants.BASE_CCY).asKeyField()
                .withField(StoreAndFieldConstants.COUNTER_CCY).asKeyField()
                .withField(StoreAndFieldConstants.FX_RATE,DOUBLE)
                .withField(StoreAndFieldConstants.AS_OF_DATE,LOCAL_DATE).asKeyField()
                .withDuplicateKeyHandler(DuplicateKeyHandlers.LOG_WITHIN_TRANSACTION)
                .build();
    }

    @Bean
    public IStoreDescription createBooksStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(StoreAndFieldConstants.BOOKS_STORE_NAME)
                .withField(StoreAndFieldConstants.BOOK_ID).asKeyField()
                .withField(StoreAndFieldConstants.COMPANY)
                .withField(StoreAndFieldConstants.DESK)
                .withDuplicateKeyHandler(DuplicateKeyHandlers.LOG_WITHIN_TRANSACTION)
                .build();
    }

    @Bean
    public IStoreDescription createPREAddOnStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(StoreAndFieldConstants.PFE_ADD_ON_STORE_NAME)
                .withField(StoreAndFieldConstants.ASSET_CLASS).asKeyField()
                .withField(StoreAndFieldConstants.PFE_SUBCATEGORY).asKeyField()
                .withField(StoreAndFieldConstants.MATURITY_BUCKET).asKeyField()
                .withField(StoreAndFieldConstants.PFE_FACTOR,DOUBLE)
                .withDuplicateKeyHandler(DuplicateKeyHandlers.LOG_WITHIN_TRANSACTION)
                .build();
    }

    @Bean
    public IReferenceDescription tradeToNettingSetsReference() {
        return ReferenceDescription.builder()
                .fromStore(StoreAndFieldConstants.TRADES_STORE_NAME)
                .toStore(StoreAndFieldConstants.NETTING_SETS_STORE_NAME)
                .withName(referenceName(StoreAndFieldConstants.TRADES_STORE_NAME, StoreAndFieldConstants.NETTING_SETS_STORE_NAME))
                .withMapping(StoreAndFieldConstants.NETTING_SET_ID, StoreAndFieldConstants.NETTING_SET_ID)
                .withMapping(StoreAndFieldConstants.AS_OF_DATE,StoreAndFieldConstants.AS_OF_DATE)
                .build();
    }

    @Bean
    public IReferenceDescription nettingSetsToCounterpartiesReference() {
        return ReferenceDescription.builder()
                .fromStore(StoreAndFieldConstants.NETTING_SETS_STORE_NAME)
                .toStore(StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME)
                .withName(referenceName(StoreAndFieldConstants.NETTING_SETS_STORE_NAME, StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME))
                .withMapping(StoreAndFieldConstants.COUNTERPARTY_ID, StoreAndFieldConstants.COUNTERPARTY_ID)
                .build();
    }

    @Bean
    public IReferenceDescription tradesToBooksReference() {
        return ReferenceDescription.builder()
                .fromStore(StoreAndFieldConstants.TRADES_STORE_NAME)
                .toStore(StoreAndFieldConstants.BOOKS_STORE_NAME)
                .withName(referenceName(StoreAndFieldConstants.TRADES_STORE_NAME, StoreAndFieldConstants.BOOKS_STORE_NAME))
                .withMapping(StoreAndFieldConstants.BOOK_ID, StoreAndFieldConstants.BOOK_ID)
                .build();
    }
}
