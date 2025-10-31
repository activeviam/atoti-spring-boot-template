/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.ActivePivotManagerConfig.DOUBLE_FORMATTER;
import static com.activeviam.apps.cfg.pivot.Dimensions.TRADE_ATTRIBUTES_DIMENSION;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.RUN_CHAINS_STORE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;

import org.springframework.stereotype.Component;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.copper.api.UnlinkedCopperStore;
import com.activeviam.activepivot.core.intf.api.copper.ICopperContext;
import com.activeviam.database.api.schema.FieldPath;

@Component
public class Measures {

    public void build(ICopperContext context) {
        Copper.sum(NOTIONAL).as(NOTIONAL).withFormatter(DOUBLE_FORMATTER).publish(context);
        var copperRunChainStore = Copper.store(RUN_CHAINS_STORE).joinToCube(UnlinkedCopperStore.JoinType.INNER)
                .withMapping("SourcedFromRunId", Copper.level(TRADE_ATTRIBUTES_DIMENSION, TRADE_ID, TRADE_ID));

        Copper.newHierarchy("Runs", "Chains").fromStore(copperRunChainStore)
                .withLevel("RunChainId", FieldPath.of("RunId"))
                .withLevel("SourceRunId", FieldPath.of("SourcedFromRunId"))
                .publish(context);
    }
}
