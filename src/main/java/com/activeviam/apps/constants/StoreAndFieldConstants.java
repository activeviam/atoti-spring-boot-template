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
    public static final String NETTING_SETS_STORE_NAME = "NettingSets";
    public static final String COUNTERPARTIES_STORE_NAME = "Counterparties";
    public static final String FX_RATES_STORE_NAME = "FXRates";
    public static final String BOOKS_STORE_NAME = "Books";
    public static final String PFE_ADD_ON_STORE_NAME = "PFEAddOn";

    /********************* Stores fields ***********************/
    public static final String TRADE_ID = "TradeID";
    public static final String PRODUCT = "Product";
    public static final String NETTING_SET_ID = "NettingSetID";
    public static final String BOOK_ID = "BookID";
    public static final String DIRECTION = "Direction";
    public static final String TRADE_INPUT_CCY = "TradeInputCCY";
    public static final String INSTRUMENT = "Instrument";
    public static final String ASSET_CLASS = "AssetClass";
    public static final String SUBCLASS = "Subclass";
    public static final String OPTION_TYPE = "OptionType";
    public static final String UNDERLYING = "Underlying";
    public static final String MATURITY = "Maturity";
    public static final String NOTIONAL = "Notional";
    public static final String MARKET_VALUE = "MarketValue";
    public static final String AS_OF_DATE = "AsOfDate";

    public static final String NETTING_NAME = "NettingName";
    public static final String NETTING_TYPE = "NettingType";
    public static final String COUNTERPARTY_ID = "CounterPartyID";
    public static final String COLLATERAL = "Collateral";
    public static final String MTA = "MTA";
    public static final String NETTING_INPUT_CCY = "NettingInputCCY";


    public static final String COUNTERPARTY_NAME = "CounterPartyName";
    public static final String RATING = "Rating";
    public static final String SECTOR = "Sector";
    public static final String COUNTRY_OF_RISK = "CountryOfRisk";

    public static final String BASE_CCY = "BaseCCY";
    public static final String COUNTER_CCY = "CounterCCY";
    public static final String FX_RATE = "FXRate";

    public static final String COMPANY = "Company";
    public static final String DESK = "Desk";

    public static final String MATURITY_BUCKET = "MaturityBucket";
    public static final String PFE_SUBCATEGORY = "PFESubcategory";
    public static final String PFE_FACTOR = "PFEFactor";
}
