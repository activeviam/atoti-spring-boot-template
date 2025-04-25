/*
 * Copyright (C) ActiveViam 2024-2025
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

    public static final String TRADE_ATTRIBUTES_STORE_NAME = "TradeAttributes";

    public static final String COUNTERPARTIES_STORE_NAME = "Counterparties";

    /********************* Stores fields ***********************/
    public static final String COB_DATE = "CobDate";

    public static final String TRADE_ID = "TradeID";
    public static final String NOTIONAL = "Notional";

    public static final String TEST_DECIMAL = "TestDecimal";

    public static final String TRADE_DATE = "TradeDate";
    public static final String COUNTERPARTY_ID = "CounterpartyID";
    public static final String COUNTERPARTY_NAME = "CounterpartyName";
}
