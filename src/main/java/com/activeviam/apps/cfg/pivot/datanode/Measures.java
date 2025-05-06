/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.SECURITY_LEVEL;
import static com.activeviam.apps.constants.CubeConstants.INT_FORMATTER;
import static com.activeviam.apps.constants.CubeConstants.NATIVE_MEASURES;
import static com.activeviam.apps.constants.CubeConstants.TIMESTAMP_FORMATTER;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.copper.api.CopperMeasure;
import com.activeviam.activepivot.core.intf.api.copper.CopperLevel;
import com.activeviam.activepivot.core.intf.api.copper.ICopperContext;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.apps.constants.DatastoreConstants;

public class Measures implements Consumer<ICopperContext> {
    private final List<CopperMeasure> copperMeasures = new ArrayList<>();

    public Measures() {
        addMeasures(
                Copper.count().withAlias("Count").withFormatter(INT_FORMATTER).withinFolder(NATIVE_MEASURES),
                Copper.timestamp()
                        .withAlias("Update.Timestamp")
                        .withinFolder(NATIVE_MEASURES)
                        .withFormatter(TIMESTAMP_FORMATTER));
        var values = Copper.sum(DatastoreConstants.ScaledStatResultStore.Fields.RESULT_VALUES_SUM)
                .as("Result Values");
        var amount = Copper.sum(DatastoreConstants.HoldingStore.Fields.AMOUNT)
                .per(identifierToLevel(SECURITY_LEVEL))
                .doNotAggregateAbove()
                .as("Amount");
        addMeasures(values, amount);
        addMeasures(MeasuresFactory.scaledVector(values, amount).as("Scaled Vector"));
    }

    private void addMeasures(CopperMeasure... measure) {
        copperMeasures.addAll(List.of(measure));
    }

    @Override
    public void accept(ICopperContext context) {
        copperMeasures.forEach(m -> m.publish(context));
    }

    public static CopperLevel identifierToLevel(LevelIdentifier identifier) {
        return Copper.level(identifier.getDimensionName(), identifier.getHierarchyName(), identifier.getLevelName());
    }
}
