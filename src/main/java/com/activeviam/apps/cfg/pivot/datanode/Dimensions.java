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
import com.activeviam.apps.cfg.database.DatabaseProperties;
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.apps.constants.FieldConstants;
import com.activeviam.tech.core.api.ordering.IComparator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Dimensions implements ICanStartBuildingDimensions.DimensionsAdder {

    public static final String HOLDING_DETAILS_DIMENSION = "Holding details";
    public static final String SECURITY_DIMENSION = "Security details";
    public static final String STATS_DIMENSION = "Stats";

    private final DatabaseProperties databaseProperties;

    public static final LevelIdentifier AS_OF_DATE_LEVEL =
            new LevelIdentifier(HOLDING_DETAILS_DIMENSION, FieldConstants.AS_OF_DATE, FieldConstants.AS_OF_DATE);
    public static final LevelIdentifier SECURITY_LEVEL = new LevelIdentifier(
            SECURITY_DIMENSION,
            DatastoreConstants.HoldingDetailStore.Fields.SECURITY,
            DatastoreConstants.HoldingDetailStore.Fields.SECURITY);
    public static final LevelIdentifier PORTFOLIO_LEVEL =
            new LevelIdentifier(HOLDING_DETAILS_DIMENSION, FieldConstants.PORTFOLIO, FieldConstants.PORTFOLIO);
    public static final LevelIdentifier STAT_NAME_LEVEL =
            new LevelIdentifier(STATS_DIMENSION, DatastoreConstants.STAT_NAME, DatastoreConstants.STAT_NAME);
    public static final LevelIdentifier ENGINE_MASK_LEVEL = new LevelIdentifier(
            STATS_DIMENSION,
            DatastoreConstants.SimReturnsStore.Fields.ENGINE_MASK,
            DatastoreConstants.SimReturnsStore.Fields.ENGINE_MASK);

    @Override
    public ICanBuildCubeDescription<IActivePivotInstanceDescription> apply(ICanStartBuildingDimensions builder) {
        var partialBuilder = builder.withDimension(HOLDING_DETAILS_DIMENSION)
                .withSingleLevelHierarchies(FieldConstants.HOLDING_ID)
                .withHierarchy(FieldConstants.AS_OF_DATE)
                .slicing()
                .withLevelOfSameName()
                .withType(ILevelInfo.LevelType.TIME)
                .withComparator(IComparator.DESCENDING_NATURAL_ORDER_PLUGIN_KEY)
                .withSingleLevelHierarchy(FieldConstants.PORTFOLIO)
                .withDimension(SECURITY_DIMENSION)
                .withSingleLevelHierarchy(DatastoreConstants.HoldingDetailStore.Fields.SECURITY);
        // With direct query, stat name is in the base store
        if (databaseProperties.isDirectQueryType()) {
            return partialBuilder
                    .withDimension(STATS_DIMENSION)
                    .withHierarchy(DatastoreConstants.STAT_NAME)
                    .slicing()
                    .withLevelOfSameName()
                    .withHierarchy(DatastoreConstants.SimReturnsStore.Fields.ENGINE_MASK)
                    .slicing()
                    .withLevelOfSameName();
        } else {
            // With in memory, stat name is in an isolated store, we use copper join
            return partialBuilder;
        }
    }
}
