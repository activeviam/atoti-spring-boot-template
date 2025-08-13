/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;

import org.springframework.stereotype.Component;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IDimension;
import com.activeviam.activepivot.core.intf.api.cube.metadata.ILevelInfo;
import com.activeviam.activepivot.core.intf.api.description.builder.dimension.ICanStartBuildingDimensions;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.tech.core.api.ordering.IComparator;

@Component
public class Dimensions {

    public static final String TRADE_ATTRIBUTES_DIMENSION = "Trade Attributes";

    /**
     * Adds the dimensions descriptions.
     *
     * @return The dimension adder
     */
    public ICanStartBuildingDimensions.DimensionsAdder build() {
        return b -> b.withDimension(TRADE_ATTRIBUTES_DIMENSION)
                .withSingleLevelHierarchies(TRADE_ID, COUNTERPARTY_ID)
                .withSingleLevelHierarchy(TRADE_DATE)
                .withType(ILevelInfo.LevelType.TIME)
                // Make the AsOfDate hierarchy slicing - we do not aggregate across dates
                // Also show the dates in reverse order ie most recent date first
                .withDimension(StoreAndFieldConstants.ASOFDATE)
                .withType(IDimension.DimensionType.TIME)
                .withHierarchy(StoreAndFieldConstants.ASOFDATE)
                .slicing()
                .withLevelOfSameName()
                .withType(ILevelInfo.LevelType.TIME)
                .withComparator(IComparator.DESCENDING_NATURAL_ORDER_PLUGIN_KEY);
    }
}
