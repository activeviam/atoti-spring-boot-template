/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.cfg.database.directquery.datamodel.TableDefinitionsConfig.MTM_VECTOR;
import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.AS_OF_DATE_LEVEL;
import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.ENGINE_MASK_LEVEL;
import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.SECURITY_LEVEL;
import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.STAT_NAME_LEVEL;
import static com.activeviam.apps.constants.CubeConstants.INT_FORMATTER;
import static com.activeviam.apps.constants.CubeConstants.NATIVE_MEASURES;
import static com.activeviam.apps.constants.CubeConstants.TIMESTAMP_FORMATTER;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.copper.api.CopperMeasure;
import com.activeviam.activepivot.copper.api.CopperStore;
import com.activeviam.activepivot.core.intf.api.copper.CopperHierarchy;
import com.activeviam.activepivot.core.intf.api.copper.CopperLevel;
import com.activeviam.activepivot.core.intf.api.copper.ICopperContext;
import com.activeviam.activepivot.core.intf.api.copper.Publishable;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.apps.cfg.database.DatabaseProperties;
import com.activeviam.apps.cfg.database.datastore.DatastoreConstants;
import com.activeviam.database.api.types.ILiteralType;

public class Measures implements Consumer<ICopperContext> {
    private final List<CopperMeasure> copperMeasures = new ArrayList<>();
    private final List<Publishable<CopperHierarchy>> copperHierarchies = new ArrayList<>();
    public static final String LOOKUP_MTM = "LOOKUP_MTM";

    public Measures(DatabaseProperties databaseProperties) {
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
        CopperMeasure mtm;
        if (databaseProperties.isDatastoreType()){
            var simulationsStore = simulationsStore();
          addHierarchies(Copper.newHierarchy(STAT_NAME_LEVEL.getDimensionName(),STAT_NAME_LEVEL.getHierarchyName())
                    .fromField(simulationsStore.field(DatastoreConstants.SimReturnsStore.Fields.STAT_NAME))
                    .slicing()
                    .withLevelName(STAT_NAME_LEVEL.getLevelName()),
                  Copper.newHierarchy(ENGINE_MASK_LEVEL.getDimensionName(),ENGINE_MASK_LEVEL.getHierarchyName())
                          .fromField(simulationsStore.field(DatastoreConstants.SimReturnsStore.Fields.ENGINE_MASK))
                          .slicing()
                          .withLevelName(ENGINE_MASK_LEVEL.getLevelName()));
            mtm = copperJoinMtM(simulationsStore);
        }
        else {
            mtm = preCalculatedScaledMtM();
        }


        var scaledMtM = scaledMtM(amount,mtm,databaseProperties.isDirectQueryType()).as("Scaled MtM");
        addMeasures(scaledMtM, valueAtRisk(scaledMtM, Copper.constant(95.0)).as("VaR 95"));
    }

    private void addMeasures(CopperMeasure... measure) {
        copperMeasures.addAll(List.of(measure));
    }

    private void addHierarchies(Publishable<CopperHierarchy>... hierarchies) {
        copperHierarchies.addAll(List.of(hierarchies));
    }

    @Override
    public void accept(ICopperContext context) {
        copperHierarchies.forEach(h -> h.publish(context));
        copperMeasures.forEach(m -> m.publish(context));
    }

    public static CopperLevel identifierToLevel(LevelIdentifier identifier) {
        return Copper.level(identifier.getDimensionName(), identifier.getHierarchyName(), identifier.getLevelName());
    }

    public static CopperMeasure preCalculatedScaledMtM() {
        return Copper.sum(MTM_VECTOR);
    }

    public static CopperMeasure scaledMtM(
            CopperMeasure amountMeasure, CopperMeasure vectorMeasure, boolean isPrecalculated) {
        return Copper.combine(amountMeasure, vectorMeasure)
                .map(
                        (reader, writer) -> {
                            if (reader.isNull(0)
                                    || reader.isNull(1)
                                    || reader.readVector(1).size() == 0) {
                                writer.writeNull();
                            } else {
                                var scalingFactor = reader.readDouble(0);
                                var vector = reader.readVector(1);
                                if (isPrecalculated) {
                                    writer.write(vector);
                                } else {
                                    var scaledVector = vector.cloneOnHeap();
                                    scaledVector.scale(scalingFactor);
                                    writer.write(scaledVector);
                                }
                            }
                        },
                        ILiteralType.DOUBLE_ARRAY)
                .per(Copper.level(SECURITY_LEVEL))
                .sum();
    }

    public static CopperStore simulationsStore(){
        return Copper.store(DatastoreConstants.SimReturnsStore.STORE_NAME)
                .joinToCube()
                .withMapping(DatastoreConstants.SimReturnsStore.Fields.AS_OF_DATE,Copper.level(AS_OF_DATE_LEVEL))
                .withMapping(DatastoreConstants.SimReturnsStore.Fields.SECURITY_NAME,Copper.level(SECURITY_LEVEL));
    }

    public static CopperMeasure copperJoinMtM(CopperStore simulationsStore){
            return Copper.newLookupMeasure(simulationsStore.field(DatastoreConstants.SimReturnsStore.Fields.VECTOR));
    }

//    public static CopperMeasure lookupScaledMtM(CopperMeasure scalingFactorMeasure) {
//        var lookupMtM = Copper.storeLookup(DatastoreConstants.SimReturnsStore.STORE_NAME)
//                .withMapping(
//                        Map.of(DatastoreConstants.SimReturnsStore.Fields.AS_OF_DATE,
//                        Copper.member(Copper.level(AS_OF_DATE_LEVEL)),
//                        DatastoreConstants.SimReturnsStore.Fields.SECURITY_NAME,
//                        Copper.member(Copper.level(SECURITY_LEVEL)),
//                        DatastoreConstants.SimReturnsStore.Fields.STAT_NAME,
//                        Copper.member(Copper.level(STAT_NAME_LEVEL)),
//                        DatastoreConstants.SimReturnsStore.Fields.ENGINE_MASK,
//                        Copper.member(Copper.level(ENGINE_MASK_LEVEL))))
//                .valueOf(DatastoreConstants.SimReturnsStore.Fields.VECTOR);
//        return Copper.combine(scalingFactorMeasure, lookupMtM)
//                .map(
//                        (reader, writer) -> {
//                            if (reader.isNull(0)
//                                    || reader.isNull(1)
//                                    || reader.readVector(1).size() == 0) {
//                                writer.writeNull();
//                            } else {
//                                var scalingFactor = reader.readDouble(0);
//                                var vector = reader.readVector(1);
//                                var scaledVector = vector.cloneOnHeap();
//                                scaledVector.scale(scalingFactor);
//                                writer.write(scaledVector);
//                            }
//                        },
//                        ILiteralType.DOUBLE_ARRAY)
//                .per(
//                        Copper.level(SECURITY_LEVEL),
//                        Copper.level(STAT_NAME_LEVEL),
//                        Copper.level(ENGINE_MASK_LEVEL),
//                        Copper.level(AS_OF_DATE_LEVEL))
//                .sum();
//    }

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
