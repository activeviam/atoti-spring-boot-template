/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import java.io.Serial;

import com.activeviam.tech.core.api.ordering.IComparator;
import com.activeviam.tech.core.api.registry.AtotiExtendedPluginValue;

@AtotiExtendedPluginValue(intf = IComparator.class, key = CustomStringComparator.CUSTOM_STRING_COMPARATOR_PLUGIN_KEY)
public class CustomStringComparator implements IComparator<String> {

    public static final String CUSTOM_STRING_COMPARATOR_PLUGIN_KEY = "CustomStringComparator";

    @Serial
    private static final long serialVersionUID = -1533404513643927635L;

    @Override
    public String getType() {
        return CUSTOM_STRING_COMPARATOR_PLUGIN_KEY;
    }

    @Override
    public int compare(String o1, String o2) {
        // TODO: Logic here
        return o1.compareTo(o2);
    }
}
