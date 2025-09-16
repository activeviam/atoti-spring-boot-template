/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.cfg.source.SourceConfig.DATE_FORMAT_FROM_FILE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.source.common.api.IColumnCalculator;
import com.activeviam.source.csv.api.ILineReader;

public final class DateFromFileNameCalculator implements IColumnCalculator<ILineReader> {

    private final String columnName;

    public DateFromFileNameCalculator(String columnName) {
        this.columnName = columnName;
    }

    public LocalDate compute(IColumnCalculator.IColumnCalculationContext<ILineReader> context) {
        var filename = ((ILineReader)context.getContext()).getCurrentFile().getName();
        return LocalDate.parse(filename.split("_")[1].split("\\.")[0], DateTimeFormatter.ofPattern(DATE_FORMAT_FROM_FILE));
    }

    public String getColumnName() {
        return this.columnName;
    }
}
