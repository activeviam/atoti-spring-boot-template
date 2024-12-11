/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.stereotype.Component;

import com.activeviam.activepivot.core.intf.api.description.builder.dimension.ICanStartBuildingDimensions;
import com.activeviam.apps.constants.StoreAndFieldConstants;

@Component
public class Dimensions {

    /**
     * Adds the dimensions descriptions.
     *
     * @return The dimension adder
     */
    public ICanStartBuildingDimensions.DimensionsAdder build() {
        return b -> b

                // Orders Hierarchies
                .withDimension(StoreAndFieldConstants.ORDERS_STORE_NAME)
                .withHierarchy(StoreAndFieldConstants.ORDERS_ORDER_DATE)
                .withLevel(StoreAndFieldConstants.ORDERS_ORDER_DATE)
                .withHierarchy(StoreAndFieldConstants.ORDERS_ORDER_ID)
                .withLevel(StoreAndFieldConstants.ORDERS_ORDER_ID)
                .withHierarchy(StoreAndFieldConstants.ORDERS_SHIPPER_NAME)
                .withLevel(StoreAndFieldConstants.ORDERS_SHIPPER_NAME)
                .withHierarchy(StoreAndFieldConstants.ORDERS_PRODUCT_ID)
                .withLevel(StoreAndFieldConstants.ORDERS_PRODUCT_ID)
                .withHierarchy(StoreAndFieldConstants.ORDERS_EMPLOYEE_ID)
                .withLevel(StoreAndFieldConstants.ORDERS_EMPLOYEE_ID)
                .withHierarchy(StoreAndFieldConstants.ORDERS_CUSTOMER_ID)
                .withLevel(StoreAndFieldConstants.ORDERS_CUSTOMER_ID)
                .withHierarchy("YearQuarterMonth")
                .withLevel(StoreAndFieldConstants.ORDERS_YEAR)
                .withLevel(StoreAndFieldConstants.ORDERS_QUARTER)
                .withLevel(StoreAndFieldConstants.ORDERS_MONTH)

                // Products Hierarchies
                .withDimension(StoreAndFieldConstants.PRODUCTS_STORE_NAME)
                .withHierarchy(StoreAndFieldConstants.PRODUCTS_PRODUCT_ID)
                .withLevel(StoreAndFieldConstants.PRODUCTS_PRODUCT_ID)
                .withHierarchy("Products")
                .withLevel(StoreAndFieldConstants.PRODUCTS_PRODUCT_CATEGORY)
                .withLevel(StoreAndFieldConstants.PRODUCTS_PRODUCT_NAME);
    }
}
