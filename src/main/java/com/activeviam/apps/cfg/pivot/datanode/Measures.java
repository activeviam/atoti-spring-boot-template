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
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.database.api.types.ILiteralType;

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
                .per(identifierToLevel(SECURITY_LEVEL))
                .doNotAggregateAbove()
                .as("Result Values");
        var amount = Copper.sum(DatastoreConstants.HoldingStore.Fields.AMOUNT)
                .per(identifierToLevel(SECURITY_LEVEL))
                .doNotAggregateAbove()
                .as("Amount");
        addMeasures(values, amount);
        var scaledVector = scaleVector(amount, values)
                .per(identifierToLevel(SECURITY_LEVEL))
                .sum()
                .as("Scaled Vector");
        addMeasures(scaledVector);
        addMeasures(valueAtRisk(scaledVector, Copper.constant(95.0)).as("VaR 95"));
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

    public static CopperMeasure scaleVector(CopperMeasure scalingFactorMeasure, CopperMeasure vectorMeasure) {
        return Copper.combine(scalingFactorMeasure, vectorMeasure)
                .map(
                        (reader, writer) -> {
                            if (reader.isNull(0)
                                    || reader.isNull(1)
                                    || reader.readVector(1).size() == 0) {
                                writer.writeNull();
                            } else {
                                var scalingFactor = reader.readDouble(0);
                                var vector = reader.readVector(1);
                                var scaledVector = vector.cloneOnHeap();
                                scaledVector.scale(scalingFactor);
                                writer.write(scaledVector);
                            }
                        },
                        ILiteralType.DOUBLE_ARRAY);
    }

    public static CopperMeasure valueAtRisk(CopperMeasure vectorMeasure, CopperMeasure confidenceLevelMeasure) {
        return Copper.combine(vectorMeasure, confidenceLevelMeasure)
                .map(
                        (r, w) -> {
                            if (r.isNull(0)) {
                                w.writeNull();
                            } else {
                                var vector = r.readVector(0);
                                var confidenceLevel = r.readDouble(1);
                                var quantile = 1.0 - (confidenceLevel / 100.0);
                                var rank = (int) Math.ceil(vector.size() * quantile);
                                w.writeDouble(vector.bottomK(rank).nextDouble());
                            }
                        },
                        ILiteralType.DOUBLE);
    }
}
