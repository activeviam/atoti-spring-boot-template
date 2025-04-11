/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.cfg.pivot.ActivePivotManagerConfig.DOUBLE_FORMATTER;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.core.intf.api.copper.ICopperContext;

public class Measures {

    public void build(ICopperContext context) {
        Copper.sum(NOTIONAL).as(NOTIONAL).withFormatter(DOUBLE_FORMATTER).publish(context);
    }
}
