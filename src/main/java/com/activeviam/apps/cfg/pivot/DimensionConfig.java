/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IDimension;
import com.activeviam.activepivot.core.intf.api.cube.metadata.ILevelInfo;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.builder.ICanBuildCubeDescription;
import com.activeviam.activepivot.core.intf.api.description.builder.dimension.ICanStartBuildingDimensions;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.tech.core.api.ordering.IComparator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class DimensionConfig {

    /**
     * Adds the dimensions descriptions to the input builder.
     *
     * @param builder The cube builder
     * @return The builder for chained calls
     */
    public ICanBuildCubeDescription<IActivePivotInstanceDescription> build(ICanStartBuildingDimensions builder) {
        return builder.withSingleLevelDimensions(StoreAndFieldConstants.TRADES_TRADE_ID)
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
