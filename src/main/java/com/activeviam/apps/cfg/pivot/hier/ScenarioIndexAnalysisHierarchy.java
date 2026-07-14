/*
 * Copyright (C) ActiveViam 2018-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.hier;

import java.io.Serial;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import com.activeviam.activepivot.core.ext.api.cube.hierarchy.impl.AAnalysisHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IAnalysisHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.metadata.IAnalysisHierarchyInfo;
import com.activeviam.activepivot.core.intf.api.cube.metadata.ILevelInfo;
import com.activeviam.database.api.IDatabaseVersion;
import com.activeviam.tech.core.api.registry.AtotiExtendedPluginValue;
import com.activeviam.tech.dictionaries.avinternal.IWritableDictionary;

/**
 * Analysis hierarchy used to expand the values stored in IVector measures
 */
@AtotiExtendedPluginValue(intf = IAnalysisHierarchy.class, key = ScenarioIndexAnalysisHierarchy.PLUGIN_KEY)
public class ScenarioIndexAnalysisHierarchy extends AAnalysisHierarchy {
    public static final String PLUGIN_KEY = "ScenarioIndexAnalysisHierarchy";
    public static final String LEVEL_NAME = "Expand Scenario Vector";
    public static final int MAX_NB_ELEMENTS = 10_000;

    @Serial
    private static final long serialVersionUID = 7796121743695864995L;

    private static final List<Object[]> DATA = IntStream.rangeClosed(0, MAX_NB_ELEMENTS)
            .mapToObj(i -> new Object[] {IHierarchy.ALLMEMBER, i})
            .toList();

    public ScenarioIndexAnalysisHierarchy(
            IAnalysisHierarchyInfo hierarchyInfo,
            List<ILevelInfo> levelInfos,
            IWritableDictionary<Object>[] levelDictionaries) {
        super(hierarchyInfo, levelInfos, levelDictionaries);
    }

    @Override
    protected Iterator<Object[]> buildDiscriminatorPathsIterator(IDatabaseVersion database) {
        return DATA.iterator();
    }

    @Override
    public String getType() {
        return PLUGIN_KEY;
    }
}
