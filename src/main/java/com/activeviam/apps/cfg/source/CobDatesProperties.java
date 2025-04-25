/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.activeviam.activepivot.core.impl.api.condition.FactFilterConditions;
import com.activeviam.tech.core.api.filtering.ICondition;

import lombok.Data;

@Data
public class CobDatesProperties {
    private Set<LocalDate> inMemoryDates = new HashSet<>();

    public Optional<LocalDate> lastDateInMemory() {
        return inMemoryDates.stream().min(LocalDate::compareTo);
    }

    public ICondition inMemoryDatesFilterCondition() {
        return null;
        //        lastDateInMemory()
        //                .map(lastDate -> FactFilterConditions.gteq(COB_DATE, lastDate))
        //                .orElse(null);
    }

    public ICondition directQueryDatesFilterCondition() {
        return lastDateInMemory()
                .map(lastDate -> FactFilterConditions.lt(COB_DATE, lastDate))
                .orElse(null);
    }
}
