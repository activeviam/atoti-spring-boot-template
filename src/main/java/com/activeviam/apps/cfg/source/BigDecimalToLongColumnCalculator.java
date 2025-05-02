/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.math.BigDecimal;
import java.util.Objects;

import com.activeviam.source.common.api.IColumnCalculator;
import com.activeviam.source.jdbc.api.impl.ResultSetRow;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BigDecimalToLongColumnCalculator implements IColumnCalculator<ResultSetRow> {

    private final String column;

    @Override
    public String getColumnName() {
        return column;
    }

    @Override
    public Object compute(IColumnCalculationContext<ResultSetRow> context) {
        var decimal = context.getContext().getObject(column);
        return Objects.nonNull(decimal) ? ((BigDecimal) decimal).longValue() : null;
    }
}
