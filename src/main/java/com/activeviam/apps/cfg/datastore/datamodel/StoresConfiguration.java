/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.datastore.datamodel;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;
import static com.activeviam.database.api.types.ILiteralType.DOUBLE;
import static com.activeviam.database.api.types.ILiteralType.LOCAL_DATE;
import static com.activeviam.database.api.types.ILiteralType.STRING;

import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.ReferenceDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StoresConfiguration {

    public static String referenceName(String from, String to) {
        return String.format("%s_to_%s", from, to);
    }

    public static IStoreDescription createTradesStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(TRADES_STORE_NAME)
                .withField(ASOFDATE, LOCAL_DATE)
                .asKeyField()
                .withField(TRADE_ID, STRING)
                .asKeyField()
                .withField(NOTIONAL, DOUBLE)
                .build();
    }

    public static IStoreDescription createTradeAttributesStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(TRADE_ATTRIBUTES_STORE_NAME)
                .withField(ASOFDATE, LOCAL_DATE)
                .asKeyField()
                .withField(TRADE_ID, STRING)
                .asKeyField()
                .withField(TRADE_DATE, LOCAL_DATE)
                .withField(COUNTERPARTY_ID, STRING)
                .build();
    }

    public static IReferenceDescription tradeToAttributedReference() {
        return ReferenceDescription.builder()
                .fromStore(TRADES_STORE_NAME)
                .toStore(TRADE_ATTRIBUTES_STORE_NAME)
                .withName(referenceName(TRADES_STORE_NAME, TRADE_ATTRIBUTES_STORE_NAME))
                .withMapping(ASOFDATE, ASOFDATE)
                .withMapping(TRADE_ID, TRADE_ID)
                .build();
    }
}
