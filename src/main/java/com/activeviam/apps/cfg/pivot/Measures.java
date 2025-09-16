/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConstants.ASSET_CLASS_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.AS_OF_DATE_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.CCR_METHOD_2;
import static com.activeviam.apps.cfg.pivot.CubeConstants.CCR_METHOD_3;
import static com.activeviam.apps.cfg.pivot.CubeConstants.CURRENCY_DIMENSION;
import static com.activeviam.apps.cfg.pivot.CubeConstants.DISPLAY_CCY;
import static com.activeviam.apps.cfg.pivot.CubeConstants.DISPLAY_CCY_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.DISPLAY_CURRENCY_HIERARCHY;
import static com.activeviam.apps.cfg.pivot.CubeConstants.DOUBLE_FORMATTER;
import static com.activeviam.apps.cfg.pivot.CubeConstants.INTERNAL_MEASURES_FOLDER;
import static com.activeviam.apps.cfg.pivot.CubeConstants.INT_FORMATTER;
import static com.activeviam.apps.cfg.pivot.CubeConstants.MATURITY_BUCKET_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.MATURITY_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.NETTING_CCY_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.NETTING_SET_ID_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.NET_POSITIVE_EXPOSURE;
import static com.activeviam.apps.cfg.pivot.CubeConstants.ORIGINAL_CCY_SUFFIX;
import static com.activeviam.apps.cfg.pivot.CubeConstants.PCT_FORMATTER;
import static com.activeviam.apps.cfg.pivot.CubeConstants.PFE_ADD_ON;
import static com.activeviam.apps.cfg.pivot.CubeConstants.PFE_DIMENSION;
import static com.activeviam.apps.cfg.pivot.CubeConstants.PFE_SUBCATEGORY_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.SUM_SUFFIX;
import static com.activeviam.apps.cfg.pivot.CubeConstants.TRADE_CCY_COPPER_LEVEL;
import static com.activeviam.apps.cfg.pivot.CubeConstants.TRADE_ID_COPPER_LEVEL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASSET_CLASS;
import static com.activeviam.apps.constants.StoreAndFieldConstants.AS_OF_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.BASE_CCY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COLLATERAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTER_CCY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.FX_RATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.FX_RATES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.MARKET_VALUE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.MATURITY_BUCKET;
import static com.activeviam.apps.constants.StoreAndFieldConstants.MTA;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_ADD_ON_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_FACTOR;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_SUBCATEGORY;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.copper.api.UnlinkedCopperStore;
import com.activeviam.activepivot.core.intf.api.copper.ICopperContext;
import com.activeviam.activepivot.core.intf.api.description.builder.ICanStartBuildingMeasures;
import com.activeviam.activepivot.core.intf.api.description.builder.IHasAtLeastOneMeasure;
import com.activeviam.apps.cfg.source.PFEFactory;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.tech.aggregation.api.IAggregationFunction;

public class Measures {

    public static IHasAtLeastOneMeasure build(ICanStartBuildingMeasures builder) {
        return builder
                .withContributorsCount()
                    .withinFolder(INTERNAL_MEASURES_FOLDER)
                    .withFormatter(INT_FORMATTER)
                .withUpdateTimestamp()
                    .withinFolder(INTERNAL_MEASURES_FOLDER)
                .withCalculations(Measures::measures);
    }

    protected static void measures(ICopperContext context) {
        Stream.of(NOTIONAL, MARKET_VALUE)
                .forEach( measure -> Copper
                        .sum(measure)
                        .per(TRADE_CCY_COPPER_LEVEL)
                        .doNotAggregateAbove()
                        .as(measure+SUM_SUFFIX+ORIGINAL_CCY_SUFFIX)
                        .withFormatter(DOUBLE_FORMATTER)
                        .withinFolder(INTERNAL_MEASURES_FOLDER)
                        .publish(context));

        Copper
                .agg(COLLATERAL,IAggregationFunction.SINGLE_VALUE_PLUGIN_KEY)
                .per(NETTING_SET_ID_COPPER_LEVEL)
                .sum()
                .per(NETTING_CCY_COPPER_LEVEL)
                .doNotAggregateAbove()
                .as(COLLATERAL+SUM_SUFFIX+ORIGINAL_CCY_SUFFIX)
                .withFormatter(DOUBLE_FORMATTER)
                .withinFolder(INTERNAL_MEASURES_FOLDER)
                .publish(context);

        Copper
                .agg(MTA, IAggregationFunction.SINGLE_VALUE_PLUGIN_KEY)
                .per(NETTING_SET_ID_COPPER_LEVEL)
                .doNotAggregateAbove()
                .as(MTA+ORIGINAL_CCY_SUFFIX)
                .withFormatter(DOUBLE_FORMATTER)
                .withinFolder(INTERNAL_MEASURES_FOLDER)
                .publish(context);

        var forexJoinForTrades =
                Copper.store(FX_RATES_STORE_NAME)
                        .joinToCube(UnlinkedCopperStore.JoinType.LEFT)
                        .withMapping(AS_OF_DATE, AS_OF_DATE_COPPER_LEVEL)
                        .withMapping(BASE_CCY,TRADE_CCY_COPPER_LEVEL)
                        .withDefaultValue(FieldPath.of(FX_RATE), Double.NEGATIVE_INFINITY);

        Copper.newHierarchy(CURRENCY_DIMENSION,DISPLAY_CURRENCY_HIERARCHY)
                .fromField(forexJoinForTrades.field(COUNTER_CCY))
                .slicing()
                .withLevelOfSameName()
                .withFirstObjects(DISPLAY_CCY)
                .publish(context);

        var fxRateAtTrades = Copper.newLookupMeasure(forexJoinForTrades.field(FX_RATE));

        Stream.of(NOTIONAL+SUM_SUFFIX, MARKET_VALUE+SUM_SUFFIX)
                .forEach( measureName -> Copper
                        .measure(measureName+ORIGINAL_CCY_SUFFIX)
                        .multiply(fxRateAtTrades)
                        .per(TRADE_CCY_COPPER_LEVEL)
                        .sum()
                        .as(measureName)
                        .withFormatter(DOUBLE_FORMATTER)
                        .publish(context));

        var forexJoinForNettings =
                Copper.store(FX_RATES_STORE_NAME)
                        .joinToCube(UnlinkedCopperStore.JoinType.LEFT)
                        .withMapping(AS_OF_DATE, AS_OF_DATE_COPPER_LEVEL)
                        .withMapping(BASE_CCY,NETTING_CCY_COPPER_LEVEL)
                        .withMapping(COUNTER_CCY,DISPLAY_CCY_COPPER_LEVEL);

        var fxRateAtNettings = Copper
                .newLookupMeasure(forexJoinForNettings.field(FX_RATE));

        Copper
                .measure(COLLATERAL+SUM_SUFFIX+ORIGINAL_CCY_SUFFIX)
                .multiply(fxRateAtNettings)
                .per(NETTING_CCY_COPPER_LEVEL)
                .sum()
                .as(COLLATERAL+SUM_SUFFIX)
                .withFormatter(DOUBLE_FORMATTER)
                .publish(context);

        Copper
                .measure(MTA+ORIGINAL_CCY_SUFFIX)
                .multiply(fxRateAtNettings)
                .per(NETTING_CCY_COPPER_LEVEL)
                .sum()
                .as(MTA)
                .withFormatter(DOUBLE_FORMATTER)
                .publish(context);

       Copper
                .agg(MARKET_VALUE, IAggregationFunction.LONG_FUNCTION_PLUGIN_KEY)
                .multiply(fxRateAtTrades)
                .per(TRADE_CCY_COPPER_LEVEL)
                .sum()
                .as(NET_POSITIVE_EXPOSURE)
                .withFormatter(DOUBLE_FORMATTER)
                .publish(context);

        Copper.measure(MARKET_VALUE+SUM_SUFFIX)
                .per(TRADE_ID_COPPER_LEVEL)
                .longSum()
                .as(NET_POSITIVE_EXPOSURE+"V2")
                .withFormatter(DOUBLE_FORMATTER)
                .withinFolder(INTERNAL_MEASURES_FOLDER)
                .publish(context);

        Copper
                .measure(NET_POSITIVE_EXPOSURE)
                .minus(Copper.measure(COLLATERAL+SUM_SUFFIX))
                .per(NETTING_SET_ID_COPPER_LEVEL)
                .longSum()
                .as(CCR_METHOD_2)
                .withFormatter(DOUBLE_FORMATTER)
                .publish(context);

        var maturityBucketValues = Copper
                .combine(AS_OF_DATE_COPPER_LEVEL, MATURITY_COPPER_LEVEL)
                .mapToObject(r -> {
                        var fullYearsToMaturity = ChronoUnit.YEARS.between((LocalDate) r.read(0),(LocalDate) r.read(1));
                        if(fullYearsToMaturity < 1)
                            return PFEFactory.MATURITY_BUCKET_LT1;
                        else if(fullYearsToMaturity<5)
                            return PFEFactory.MATURITY_BUCKET_1TO5;
                        else
                            return PFEFactory.MATURITY_BUCKET_GT5;
                    });

        Copper
                .newHierarchy(PFE_DIMENSION,MATURITY_BUCKET)
                .fromValues(maturityBucketValues)
                .withLevelOfSameName()
                .withFirstObjects(PFEFactory.MATURITY_BUCKET_LT1,PFEFactory.MATURITY_BUCKET_1TO5)
                .publish(context);

        var pfeJoin = Copper
                .store(PFE_ADD_ON_STORE_NAME)
                .joinToCube(UnlinkedCopperStore.JoinType.LEFT)
                .withMapping(ASSET_CLASS,ASSET_CLASS_COPPER_LEVEL)
                .withMapping(MATURITY_BUCKET,MATURITY_BUCKET_COPPER_LEVEL)
                .withMapping(PFE_SUBCATEGORY,PFE_SUBCATEGORY_COPPER_LEVEL);

        var pfeFactor = Copper
                .newLookupMeasure(pfeJoin.field(PFE_FACTOR))
                .as(PFE_FACTOR)
                .withFormatter(PCT_FORMATTER)
                .publish(context);

        Copper
                .measure(NOTIONAL+SUM_SUFFIX)
                .multiply(pfeFactor)
                .per(ASSET_CLASS_COPPER_LEVEL,MATURITY_BUCKET_COPPER_LEVEL,PFE_SUBCATEGORY_COPPER_LEVEL)
                .sum()
                .as(PFE_ADD_ON)
                .withFormatter(DOUBLE_FORMATTER)
                .publish(context);

        Copper
                .measure(NET_POSITIVE_EXPOSURE)
                .minus(Copper.measure(COLLATERAL+SUM_SUFFIX))
                .plus(Copper.measure(PFE_ADD_ON))
                .per(NETTING_SET_ID_COPPER_LEVEL)
                .longSum()
                .as(CCR_METHOD_3)
                .withFormatter(DOUBLE_FORMATTER)
                .publish(context);
    }
}
