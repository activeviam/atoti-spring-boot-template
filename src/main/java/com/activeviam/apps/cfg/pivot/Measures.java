/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.stereotype.Component;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.core.intf.api.copper.ICopperContext;
import com.activeviam.apps.constants.StoreAndFieldConstants;

@Component
public class Measures {

    public void build(ICopperContext context) {
        var quantitySold = Copper.sum(StoreAndFieldConstants.ORDERS_QUANTITY_SOLD)
                .as("Quantity Sold")
                .withFormatter("INT[#,###]")
                .withinFolder("Selling/Purchasing")
                .hidden()
                .publish(context);

        var minSellingPricePerUnit = Copper.min(StoreAndFieldConstants.ORDERS_SELLING_PRICE_PER_UNIT)
                .as("Minimum selling price per unit")
                .withFormatter("DOUBLE[#,###.##]")
                .withinFolder("Selling/Purchasing")
                .isVisible(false)
                .publish(context);

        var maxSellingPricePerUnit = Copper.max(StoreAndFieldConstants.ORDERS_SELLING_PRICE_PER_UNIT)
                .as("Maximum selling price per unit")
                .withFormatter("DOUBLE[#,###.##]")
                .withinFolder("Selling/Purchasing")
                .publish(context);

        var avgSellingPrice = Copper.avg(StoreAndFieldConstants.ORDERS_SELLING_PRICE_PER_UNIT)
                .as("Average selling price per unit")
                .withFormatter("DOUBLE[#,###.##]")
                .withinFolder("Selling/Purchasing")
                .publish(context);

        var avgPurchasingPrice = Copper.avg(StoreAndFieldConstants.PRODUCTS_PURCHASING_PRICE_PER_UNIT)
                .as("Average purchasing price per unit")
                .withFormatter("DOUBLE[#,###.##]")
                .withinFolder("Selling/Purchasing")
                .publish(context);

        var avgPurchasingPriceProductName = Copper.avg(StoreAndFieldConstants.PRODUCTS_PURCHASING_PRICE_PER_UNIT)
                .per(Copper.level("Products", "Products", "ProductName"))
                .doNotAggregateAbove()
                .as("Average purchasing price per unit at ProductName")
                .withFormatter("DOUBLE[#,###.##]")
                .withinFolder("Selling/Purchasing")
                .publish(context);

        var purchases = Copper.combine(quantitySold, avgPurchasingPrice)
                .mapToDouble(reader -> {
                    if (reader.isNull(0) || reader.isNull(1)) {
                        return 0.0d;
                    } else {
                        return reader.readLong(0) * reader.readDouble(1);
                    }
                })
                .per(Copper.level("Products", "Products", "ProductName"))
                .sum()
                .as("Purchases")
                .withFormatter("DOUBLE[#,###.##]")
                .withinFolder("Selling/Purchasing")
                .publish(context);
    }
}
