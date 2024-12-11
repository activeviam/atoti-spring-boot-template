/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.datastore.datamodel;

import static com.activeviam.database.api.types.ILiteralType.DOUBLE;
import static com.activeviam.database.api.types.ILiteralType.INT;
import static com.activeviam.database.api.types.ILiteralType.LOCAL_DATE;
import static com.activeviam.database.api.types.ILiteralType.LONG;
import static com.activeviam.database.api.types.ILiteralType.STRING;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.DuplicateKeyHandlers;

@Configuration
public class OrdersStoreConfiguration {

    @Bean
    public IStoreDescription createOrdersStoreDescription() {
        return StartBuilding.store()
                .withStoreName(StoreAndFieldConstants.ORDERS_STORE_NAME)
                .withField(StoreAndFieldConstants.ORDERS_ORDER_ID, STRING)
                .asKeyField()
                .withField(StoreAndFieldConstants.ORDERS_ORDER_DATE, LOCAL_DATE)
                .asKeyField()
                .withField(StoreAndFieldConstants.ORDERS_QUANTITY_SOLD, LONG)
                .withNullableField(StoreAndFieldConstants.ORDERS_SELLING_PRICE_PER_UNIT, DOUBLE)
                .withField(StoreAndFieldConstants.ORDERS_SHIPPER_NAME, STRING)
                .withField(StoreAndFieldConstants.ORDERS_PRODUCT_ID, INT)
                .withField(StoreAndFieldConstants.ORDERS_EMPLOYEE_ID, INT)
                .withField(StoreAndFieldConstants.ORDERS_CUSTOMER_ID, STRING)
                .withField(StoreAndFieldConstants.ORDERS_SALES, DOUBLE)
                .withField(StoreAndFieldConstants.ORDERS_YEAR, INT)
                .withField(StoreAndFieldConstants.ORDERS_MONTH, STRING)
                .withField(StoreAndFieldConstants.ORDERS_QUARTER, STRING)
                .withDuplicateKeyHandler(DuplicateKeyHandlers.THROW_WITHIN_TRANSACTION)
                .build();
    }

    @Bean
    public IReferenceDescription ordersToProductsReference() {
        return StartBuilding.reference()
                .fromStore(StoreAndFieldConstants.ORDERS_STORE_NAME)
                .toStore(StoreAndFieldConstants.PRODUCTS_STORE_NAME)
                .withName(StoreAndFieldConstants.ORDERS_PRODUCTS_JOIN)
                .withMapping(StoreAndFieldConstants.ORDERS_PRODUCT_ID, StoreAndFieldConstants.PRODUCTS_PRODUCT_ID)
                .build();
    }
}
