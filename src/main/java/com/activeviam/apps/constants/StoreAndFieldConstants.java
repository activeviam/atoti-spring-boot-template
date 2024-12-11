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

    public static final String ORDERS_STORE_NAME = "Orders";
    public static final String PRODUCTS_STORE_NAME = "Products";

    /*********************** Stores joins **********************/
    public static final String ORDERS_PRODUCTS_JOIN = "OrdersToProducts";

    /********************* Stores fields ***********************/
    public static final String ASOFDATE = "AsOfDate";

    public static final String TRADES_TRADE_ID = "TradeID";
    public static final String TRADES_NOTIONAL = "Notional";

    public static final String ORDERS_ORDER_ID = "OrderId";
    public static final String ORDERS_ORDER_DATE = "OrderDate";
    public static final String ORDERS_QUANTITY_SOLD = "QuantitySold";
    public static final String ORDERS_SELLING_PRICE_PER_UNIT = "SellingPricePerUnit";
    public static final String ORDERS_SHIPPER_NAME = "ShipperName";
    public static final String ORDERS_PRODUCT_ID = "ProductId";
    public static final String ORDERS_EMPLOYEE_ID = "EmployeeId";
    public static final String ORDERS_CUSTOMER_ID = "CustomerId";
    public static final String ORDERS_SALES = "Sales";
    public static final String ORDERS_YEAR = "Year";
    public static final String ORDERS_MONTH = "Month";
    public static final String ORDERS_QUARTER = "Quarter";

    public static final String PRODUCTS_PRODUCT_ID = "ProductId";
    public static final String PRODUCTS_PRODUCT_NAME = "ProductName";
    public static final String PRODUCTS_PRODUCT_CATEGORY = "ProductCategory";
    public static final String PRODUCTS_SUPPLIER = "Supplier";
    public static final String PRODUCTS_PURCHASING_PRICE_PER_UNIT = "PurchasingPricePerUnit";
}
