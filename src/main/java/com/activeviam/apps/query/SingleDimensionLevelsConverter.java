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

/**
 * Implementation of LevelsConverter that assumes all hierarchies belong to the default dimension, and all levels
 * belong to a single level hierarchy with the same name of the level itself
 */
@RequiredArgsConstructor
public class SingleDimensionLevelsConverter implements LevelsConverter {

    private final String defaultDimension;

    @Override
    public LevelIdentifier stringToLevelIdentifier(String level) {
        var name = level.replace("'","").strip();
        return new LevelIdentifier(defaultDimension, name, name);
    }

    @Override
    public HierarchyIdentifier stringToHierarchyIdentifier(String hierarchy) {
        var name = hierarchy.replace("'","").strip();
        return new HierarchyIdentifier(defaultDimension, name);
    }
}
