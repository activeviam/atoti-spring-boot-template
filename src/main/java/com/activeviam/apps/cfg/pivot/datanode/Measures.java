/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.COB_DATE_LEVEL;
import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.SHIFT_COB_DATE_LEVEL;
import static com.activeviam.apps.constants.CubeConstants.DOUBLE_FORMATTER;
import static com.activeviam.apps.constants.CubeConstants.INT_FORMATTER;
import static com.activeviam.apps.constants.CubeConstants.NATIVE_MEASURES;
import static com.activeviam.apps.constants.CubeConstants.TIMESTAMP_FORMATTER;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.copper.api.CopperMeasure;
import com.activeviam.activepivot.core.intf.api.copper.ICopperContext;

public class Measures implements Consumer<ICopperContext> {
    private final List<CopperMeasure> copperMeasures = new ArrayList<>();

    public static String postfixMeasure(String base, String measure) {
        return String.join(".", base, measure);
    }

    public static final String SUM = "Sum";
    public static final String MEAN = "Mean";

    public Measures() {
        copperMeasures.add(
                Copper.count().withAlias("Count").withFormatter(INT_FORMATTER).withinFolder(NATIVE_MEASURES));
        copperMeasures.add(Copper.timestamp()
                .withAlias("Update.Timestamp")
                .withinFolder(NATIVE_MEASURES)
                .withFormatter(TIMESTAMP_FORMATTER));
        var notionalSum = Copper.sum(NOTIONAL).as(postfixMeasure(NOTIONAL, SUM)).withFormatter(DOUBLE_FORMATTER);
        copperMeasures.add(notionalSum);
        var notionalAvg =
                Copper.avg(NOTIONAL).as(postfixMeasure(NOTIONAL, MEAN)).withFormatter(DOUBLE_FORMATTER);
        copperMeasures.add(notionalAvg);
        copperMeasures.add(diffFromShiftDate(notionalSum).as(postfixMeasure(NOTIONAL, SUM) + " DIFF to shift date"));
        copperMeasures.add(diffFromShiftDate(notionalAvg).as(postfixMeasure(NOTIONAL, MEAN) + " DIFF to shift date"));
    }

    @Override
    public void accept(ICopperContext context) {
        copperMeasures.forEach(m -> m.publish(context));
    }

    private static CopperMeasure diffFromShiftDate(CopperMeasure underlying) {
        var previous = underlying.shift(
                Copper.levelsAt(List.of(Copper.level(COB_DATE_LEVEL), Copper.level(SHIFT_COB_DATE_LEVEL)), w -> {
                    var shiftCobDate = w.read(1);
                    w.write(0, shiftCobDate);
                }));
        return underlying.minus(previous);
    }
}
