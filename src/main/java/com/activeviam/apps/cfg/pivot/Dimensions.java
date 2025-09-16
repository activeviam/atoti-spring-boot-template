/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConstants.BOOK_STRUCTURE_DIMENSION;
import static com.activeviam.apps.cfg.pivot.CubeConstants.COUNTERPARTIES_DIMENSION;
import static com.activeviam.apps.cfg.pivot.CubeConstants.CURRENCY_DIMENSION;
import static com.activeviam.apps.cfg.pivot.CubeConstants.NETTINGS_DIMENSION;
import static com.activeviam.apps.cfg.pivot.CubeConstants.PFE_DIMENSION;
import static com.activeviam.apps.cfg.pivot.CubeConstants.TRADE_ATTRIBUTES_DIMENSION;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASSET_CLASS;
import static com.activeviam.apps.constants.StoreAndFieldConstants.AS_OF_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.BOOK_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COMPANY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTRY_OF_RISK;
import static com.activeviam.apps.constants.StoreAndFieldConstants.DESK;
import static com.activeviam.apps.constants.StoreAndFieldConstants.DIRECTION;
import static com.activeviam.apps.constants.StoreAndFieldConstants.INSTRUMENT;
import static com.activeviam.apps.constants.StoreAndFieldConstants.MATURITY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_INPUT_CCY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_SET_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_TYPE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.OPTION_TYPE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_SUBCATEGORY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PRODUCT;
import static com.activeviam.apps.constants.StoreAndFieldConstants.RATING;
import static com.activeviam.apps.constants.StoreAndFieldConstants.SECTOR;
import static com.activeviam.apps.constants.StoreAndFieldConstants.SUBCLASS;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_INPUT_CCY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.UNDERLYING;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IDimension;
import com.activeviam.activepivot.core.intf.api.cube.metadata.ILevelInfo;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.builder.ICanBuildCubeDescription;
import com.activeviam.activepivot.core.intf.api.description.builder.dimension.ICanStartBuildingDimensions;
import com.activeviam.apps.cfg.pivot.hierarchies.DisplayCurrencyHierarchy;
import com.activeviam.tech.core.api.ordering.IComparator;

public class Dimensions {

    public static ICanBuildCubeDescription<IActivePivotInstanceDescription> build(
            final ICanStartBuildingDimensions builder) {
        return builder
                // Trade attributes dimension
                .withDimension(TRADE_ATTRIBUTES_DIMENSION)
                    .withSingleLevelHierarchies(TRADE_ID,PRODUCT,DIRECTION,UNDERLYING)
                    .withSingleLevelHierarchy(MATURITY)
                        .withType(ILevelInfo.LevelType.TIME)
                    .withHierarchy(ASSET_CLASS)
                        .withLevelOfSameName()
                        .withLevel(SUBCLASS)
                    .withHierarchy(INSTRUMENT)
                        .withLevelOfSameName()
                        .withLevel(OPTION_TYPE)
                // AsOfDate dimension
                .withDimension(AS_OF_DATE)
                    .withType(IDimension.DimensionType.TIME)
                    .withHierarchy(AS_OF_DATE)
                        .slicing()
                        .withLevelOfSameName()
                            .withType(ILevelInfo.LevelType.TIME)
                            .withComparator(IComparator.DESCENDING_NATURAL_ORDER_PLUGIN_KEY)
                // Counterparties dimension
                .withDimension(COUNTERPARTIES_DIMENSION)
                    .withSingleLevelHierarchies(COUNTERPARTY_ID,COUNTERPARTY_NAME,RATING,SECTOR,COUNTRY_OF_RISK)
                // Books dimension
                .withDimension(BOOK_STRUCTURE_DIMENSION)
                    .withSingleLevelHierarchies(BOOK_ID,COMPANY,DESK)
                // Nettings dimension
                .withDimension(NETTINGS_DIMENSION)
                    .withSingleLevelHierarchies(NETTING_NAME,NETTING_TYPE,NETTING_SET_ID)
                // Currencies dimension
                .withDimension(CURRENCY_DIMENSION)
                    .withHierarchy(TRADE_INPUT_CCY)
                        .withLevelOfSameName()
                    .withHierarchy(NETTING_INPUT_CCY)
                        .withLevelOfSameName()
//                    .withAnalysisHierarchy(
//                        DisplayCurrencyHierarchy.hierarchy(DISPLAY_CURRENCY_HIERARCHY)
//                            .slicing()
//                            .withLevel(DISPLAY_CURRENCY_HIERARCHY)
//                            .withCurrenciesOrder(List.of(CubeConstants.DISPLAY_CURRENCIES.split(",")))
//                            .build())
                // PFE dimension
                .withDimension(PFE_DIMENSION)
                    .withSingleLevelHierarchies(PFE_SUBCATEGORY);
    }
}
