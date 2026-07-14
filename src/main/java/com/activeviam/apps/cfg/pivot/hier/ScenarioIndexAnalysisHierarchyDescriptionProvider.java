/*
 * Copyright (C) ActiveViam 2023-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.hier;

import com.activeviam.activepivot.core.impl.api.description.impl.AnalysisHierarchyDescription;
import com.activeviam.activepivot.core.impl.api.description.impl.AxisLevelDescription;
import com.activeviam.activepivot.core.intf.api.description.IAnalysisHierarchyDescription;
import com.activeviam.activepivot.core.intf.api.description.IAnalysisHierarchyDescriptionProvider;
import com.activeviam.activepivot.core.intf.api.description.IAxisLevelDescription;
import com.activeviam.tech.chunks.api.types.Types;
import com.activeviam.tech.core.api.registry.AtotiPluginValue;
import com.activeviam.tech.core.api.registry.impl.PluginValue;

@AtotiPluginValue(intf = IAnalysisHierarchyDescriptionProvider.class)
public class ScenarioIndexAnalysisHierarchyDescriptionProvider extends PluginValue
        implements IAnalysisHierarchyDescriptionProvider {
    @Override
    public String key() {
        return ScenarioIndexAnalysisHierarchy.PLUGIN_KEY;
    }

    @Override
    public IAnalysisHierarchyDescription getDescription() {
        var desc = new AnalysisHierarchyDescription(
                ScenarioIndexAnalysisHierarchy.PLUGIN_KEY, ScenarioIndexAnalysisHierarchy.LEVEL_NAME, true);
        var levelDesc = new AxisLevelDescription(ScenarioIndexAnalysisHierarchy.LEVEL_NAME, null);
        levelDesc.putProperty(IAxisLevelDescription.ANALYSIS_LEVEL_TYPE_PROPERTY, Integer.toString(Types.TYPE_INT));
        desc.addLevel(levelDesc);
        return desc;
    }
}
