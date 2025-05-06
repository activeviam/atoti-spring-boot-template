/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.SECURITY_LEVEL;
import static com.activeviam.apps.cfg.pivot.datanode.Measures.identifierToLevel;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.copper.api.CopperMeasure;
import com.activeviam.database.api.types.ILiteralType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MeasuresFactory {

    public static CopperMeasure scaledVector(CopperMeasure scalingFactorMeasure, CopperMeasure vectorMeasure) {
        return Copper.combine(scalingFactorMeasure, vectorMeasure)
                .map(
                        (reader, writer) -> {
                            if (reader.isNull(0)
                                    && (reader.isNull(1) || reader.readVector(1).size() == 0)) {
                                writer.writeNull();
                            } else {
                                var scalingFactor = reader.readDouble(0);
                                var vector = reader.readVector(1);

                                var scaledVector = vector.cloneOnHeap();
                                scaledVector.scale(scalingFactor);
                                writer.write(scaledVector);
                            }
                        },
                        ILiteralType.DOUBLE_ARRAY)
                .per(identifierToLevel(SECURITY_LEVEL))
                .sum();
    }
}
