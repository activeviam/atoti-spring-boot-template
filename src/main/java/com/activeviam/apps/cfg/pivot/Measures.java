/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;

import java.util.function.Consumer;

import com.activeviam.activepivot.copper.api.Copper;
import com.activeviam.activepivot.core.impl.avinternal.foundry.core.configurator.IMeasuresConfigurator;
import com.activeviam.activepivot.core.impl.avinternal.foundry.core.measure.FoundryCopper;
import com.activeviam.database.api.schema.FieldPath;

public class Measures implements Consumer<IMeasuresConfigurator> {
    /* ********** */
    /* Formatters */
    /* ********** */
    public static final String DOUBLE_FORMATTER = "DOUBLE[#,###.##]";
    public static final String INT_FORMATTER = "INT[#,###]";
    public static final String TIMESTAMP_FORMATTER = "DATE[HH:mm:ss]";

    public static final String NATIVE_MEASURES = "Native Measures";
    @Override
    public void accept(IMeasuresConfigurator configurator) {
        // How do we rename it? What about not setting metadata here, but in the lambda? Formatters?
        configurator.addMeasure("Count", Copper.count().withinFolder(NATIVE_MEASURES));
        configurator.addMeasure(NOTIONAL, FoundryCopper.sum(FieldPath.of(NOTIONAL)));
    }
}
