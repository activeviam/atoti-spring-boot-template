/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASSET_CLASS;
import static com.activeviam.apps.constants.StoreAndFieldConstants.AS_OF_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.MATURITY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.MATURITY_BUCKET;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_INPUT_CCY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_SET_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_SUBCATEGORY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_INPUT_CCY;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.core.intf.api.copper.CopperLevel;

public class CubeConstants {

    // Dimensions
    public static final String TRADE_ATTRIBUTES_DIMENSION = "Trade Attributes";
    public static final String COUNTERPARTIES_DIMENSION = "Counterparties";
    public static final String BOOK_STRUCTURE_DIMENSION = "Book Structure";
    public static final String NETTINGS_DIMENSION = "Nettings";
    public static final String PFE_DIMENSION = "PFE Add On";
    public static final String CURRENCY_DIMENSION = "Currency";

    // Hierarhcies
    public static final String DISPLAY_CURRENCY_HIERARCHY = "Display Currency";

    // Formatters
    public static final String DOUBLE_FORMATTER = "DOUBLE[#,###.##]";
    public static final String PCT_FORMATTER = "DOUBLE[#,###.##%]";
    public static final String INT_FORMATTER = "INT[#,###]";
    public static final String DISPLAY_CCY = "EUR";

    public static final String DISPLAY_CURRENCIES = "EUR,USD,GBP,CHF,JPY,SEK,NOK,CAD";

    // Folders
    public static final String INTERNAL_MEASURES_FOLDER = "Internal Measures";

    // Measure names
    public static final String ORIGINAL_CCY_SUFFIX = " (original ccy)";
    public static final String SUM_SUFFIX = ".SUM";
    public static final String NET_POSITIVE_EXPOSURE = "Net Positive Exposure";
    public static final String CCR_METHOD_2 = "CCR (Method 2)";
    public static final String CCR_METHOD_3 = "CCR (Method 3)";
    public static final String PFE_ADD_ON = "PFE Add-on";

    // Levels
    public static final CopperLevel TRADE_CCY_COPPER_LEVEL = Copper.level(CURRENCY_DIMENSION, TRADE_INPUT_CCY, TRADE_INPUT_CCY);
    public static final CopperLevel NETTING_CCY_COPPER_LEVEL = Copper.level(CURRENCY_DIMENSION, NETTING_INPUT_CCY, NETTING_INPUT_CCY);
    public static final CopperLevel DISPLAY_CCY_COPPER_LEVEL = Copper.level(CURRENCY_DIMENSION, DISPLAY_CURRENCY_HIERARCHY, DISPLAY_CURRENCY_HIERARCHY);

    public static final CopperLevel AS_OF_DATE_COPPER_LEVEL = Copper.level(AS_OF_DATE, AS_OF_DATE, AS_OF_DATE);

    public static final CopperLevel NETTING_SET_ID_COPPER_LEVEL = Copper.level(NETTINGS_DIMENSION, NETTING_SET_ID, NETTING_SET_ID);

    public static final CopperLevel TRADE_ID_COPPER_LEVEL = Copper.level(TRADE_ATTRIBUTES_DIMENSION, TRADE_ID, TRADE_ID);
    public static final CopperLevel MATURITY_COPPER_LEVEL = Copper.level(TRADE_ATTRIBUTES_DIMENSION, MATURITY, MATURITY);
    public static final CopperLevel ASSET_CLASS_COPPER_LEVEL = Copper.level(TRADE_ATTRIBUTES_DIMENSION, ASSET_CLASS, ASSET_CLASS);

    public static final CopperLevel MATURITY_BUCKET_COPPER_LEVEL = Copper.level(PFE_DIMENSION, MATURITY_BUCKET, MATURITY_BUCKET);
    public static final CopperLevel PFE_SUBCATEGORY_COPPER_LEVEL = Copper.level(PFE_DIMENSION, PFE_SUBCATEGORY, PFE_SUBCATEGORY);
}
