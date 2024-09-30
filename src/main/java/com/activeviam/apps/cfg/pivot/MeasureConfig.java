/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.PivotManagerConfig.DOUBLE_FORMATTER;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_NOTIONAL;

import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.core.intf.api.copper.ICopperContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class MeasureConfig {

    public void build(ICopperContext context) {
        Copper.sum(TRADES_NOTIONAL)
                .as(TRADES_NOTIONAL)
                .withFormatter(DOUBLE_FORMATTER)
                .publish(context);
    }
}
