/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StoreAndFieldConstants {
    /*********************** Stores names **********************/
    public static final String TRADES_STORE_NAME = "Trades";

    /********************* Stores fields ***********************/
    public static final String ASOFDATE = "AsOfDate";

    public static final String TRADES_TRADE_ID = "TradeID";
    public static final String TRADES_NOTIONAL = "Notional";
}
