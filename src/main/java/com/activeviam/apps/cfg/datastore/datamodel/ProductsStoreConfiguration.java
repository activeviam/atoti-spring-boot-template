/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.datastore.datamodel;

import static com.activeviam.database.api.types.ILiteralType.DOUBLE;
import static com.activeviam.database.api.types.ILiteralType.INT;
import static com.activeviam.database.api.types.ILiteralType.STRING;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.database.datastore.api.description.IStoreDescription;

@Configuration
public class ProductsStoreConfiguration {

    @Bean
    public IStoreDescription createProductsStoreDescription() {
        return StartBuilding.store()
                .withStoreName(StoreAndFieldConstants.PRODUCTS_STORE_NAME)
                .withField(StoreAndFieldConstants.PRODUCTS_PRODUCT_ID, INT)
                .asKeyField()
                .withField(StoreAndFieldConstants.PRODUCTS_PRODUCT_NAME, STRING)
                .withField(StoreAndFieldConstants.PRODUCTS_PRODUCT_CATEGORY, STRING)
                .withField(StoreAndFieldConstants.PRODUCTS_SUPPLIER, STRING)
                .withField(StoreAndFieldConstants.PRODUCTS_PURCHASING_PRICE_PER_UNIT, DOUBLE)
                .build();
    }
}
