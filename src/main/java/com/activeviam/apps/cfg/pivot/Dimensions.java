/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;

import java.util.function.Consumer;

import com.activeviam.activepivot.core.impl.avinternal.foundry.core.configurator.IDimensionsConfigurator;
import com.activeviam.activepivot.core.impl.avinternal.foundry.core.definition.ITableHierarchyDefinition;
import com.activeviam.activepivot.core.intf.api.cube.metadata.DimensionIdentifier;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.database.api.schema.FieldPath;

public class Dimensions implements Consumer<IDimensionsConfigurator> {

    public static final DimensionIdentifier TRADE_ATTRIBUTES_DIMENSION = new DimensionIdentifier("Trade Attributes");
    public static final LevelIdentifier TRADE_ID_LEVEL =
            TRADE_ATTRIBUTES_DIMENSION.hierarchy(TRADE_ID).level(TRADE_ID);
    public static final LevelIdentifier TRADE_DATE_LEVEL =
            TRADE_ATTRIBUTES_DIMENSION.hierarchy(TRADE_DATE).level(TRADE_DATE);
    public static final DimensionIdentifier AS_OF_DATE_DIMENSION = new DimensionIdentifier("AsOfDate");
    public static final LevelIdentifier AS_OF_DATE_LEVEL =
            AS_OF_DATE_DIMENSION.hierarchy(ASOFDATE).level(ASOFDATE);

    @Override
    public void accept(IDimensionsConfigurator configurator) {
        var tradeAttributesDimension =
                configurator.addDimension(TRADE_ATTRIBUTES_DIMENSION, TRADE_ATTRIBUTES_STORE_NAME);
        tradeAttributesDimension.addHierarchy(
                ITableHierarchyDefinition.singleLevel(TRADE_ID_LEVEL.getHierarchyName(), FieldPath.of(TRADE_ID)));
        tradeAttributesDimension.addHierarchy(ITableHierarchyDefinition.singleLevel(
                TRADE_DATE_LEVEL.getHierarchyName(), FieldPath.of(TRADE_DATE))); // TODO: TYPE???
        var asOfDateDimension = configurator.addDimension(AS_OF_DATE_DIMENSION, TRADES_STORE_NAME);
        asOfDateDimension.addHierarchy(ITableHierarchyDefinition.builder(AS_OF_DATE_LEVEL.getHierarchyName())
                .slicing()
                .withFieldLevel(AS_OF_DATE_LEVEL.getHierarchyName(), FieldPath.of(ASOFDATE))
                .build());
    }
}
