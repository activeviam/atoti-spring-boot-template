/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import com.activeviam.activepivot.core.intf.api.cube.metadata.HierarchyIdentifier;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SingleDimensionLevelsConverter implements LevelsConverter {

    private final String defaultDimension;

    @Override
    public LevelIdentifier stringToLevelIdentifier(String level) {
        return new LevelIdentifier(defaultDimension, level, level);
    }

    @Override
    public HierarchyIdentifier stringToHierarchyIdentifier(String hierarchy) {
        return new HierarchyIdentifier(defaultDimension, hierarchy);
    }
}
