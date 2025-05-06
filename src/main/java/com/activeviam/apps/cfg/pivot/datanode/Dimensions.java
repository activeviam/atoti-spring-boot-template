/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import com.activeviam.activepivot.core.intf.api.cube.metadata.ILevelInfo;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.builder.ICanBuildCubeDescription;
import com.activeviam.activepivot.core.intf.api.description.builder.dimension.ICanStartBuildingDimensions;
import com.activeviam.apps.constants.DatastoreConstants;
import com.activeviam.apps.constants.FieldConstants;
import com.activeviam.tech.core.api.ordering.IComparator;

public class Dimensions implements ICanStartBuildingDimensions.DimensionsAdder {

    public static final String HOLDING_DETAILS_DIMENSION = "Holding details";
    public static final String SECURITY_DIMENSION = "Security details";

    public static final LevelIdentifier SECURITY_LEVEL =
            LevelIdentifier.simple(DatastoreConstants.SecurityStore.Fields.SECURITY_NAME);
    public static final LevelIdentifier PORTFOLIO_LEVEL =
            new LevelIdentifier(HOLDING_DETAILS_DIMENSION,FieldConstants.PORTFOLIO,FieldConstants.PORTFOLIO);

    @Override
    public ICanBuildCubeDescription<IActivePivotInstanceDescription> apply(ICanStartBuildingDimensions builder) {
        return builder.withDimension(HOLDING_DETAILS_DIMENSION)
                .withSingleLevelHierarchies(FieldConstants.HOLDING_ID)
                .withSingleLevelHierarchy(FieldConstants.AS_OF_DATE)
                .withType(ILevelInfo.LevelType.TIME)
                .withComparator(IComparator.DESCENDING_NATURAL_ORDER_PLUGIN_KEY)
                .withSingleLevelHierarchy(FieldConstants.PORTFOLIO)
                .withDimension(SECURITY_DIMENSION)
                .withSingleLevelHierarchy(DatastoreConstants.SecurityStore.Fields.SECURITY_NAME);
    }
}
