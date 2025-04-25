/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.activepivot.core.intf.api.description.IAxisHierarchyDescription.AUTO_CONTRIBUTE_UNKNOWN_MEMBER_NEVER;
import static com.activeviam.activepivot.core.intf.api.description.IAxisHierarchyDescription.AUTO_CONTRIBUTE_UNKNOWN_MEMBER_PROPERTY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;

import com.activeviam.activepivot.core.intf.api.cube.metadata.ILevelInfo;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.builder.ICanBuildCubeDescription;
import com.activeviam.activepivot.core.intf.api.description.builder.dimension.ICanStartBuildingDimensions;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.tech.core.api.ordering.IComparator;

public class Dimensions implements ICanStartBuildingDimensions.DimensionsAdder {

    public static final String TRADE_ATTRIBUTES_DIMENSION = "Trade Attributes";

    @Override
    public ICanBuildCubeDescription<IActivePivotInstanceDescription> apply(ICanStartBuildingDimensions builder) {
        return builder.withDimension(TRADE_ATTRIBUTES_DIMENSION)
                .withSingleLevelHierarchies(TRADE_ID, COUNTERPARTY_ID)
                .withSingleLevelHierarchy(TRADE_DATE)
                .withType(ILevelInfo.LevelType.TIME)
                // Make the AsOfDate hierarchy slicing - we do not aggregate across dates
                // Also show the dates in reverse order ie most recent date first
                //                .withDimension(StoreAndFieldConstants.COB_DATE)
                //                .withType(IDimension.DimensionType.TIME)
                .withHierarchy(StoreAndFieldConstants.COB_DATE)
                .withHierarchyProperty(AUTO_CONTRIBUTE_UNKNOWN_MEMBER_PROPERTY, AUTO_CONTRIBUTE_UNKNOWN_MEMBER_NEVER)
                .slicing()
                .withLevelOfSameName()
                .withType(ILevelInfo.LevelType.TIME)
                .withComparator(IComparator.DESCENDING_NATURAL_ORDER_PLUGIN_KEY);
    }
}
