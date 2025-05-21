/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import lombok.Data;

@Data
public class CobDatesProperties {

    private int daysInMemory = 7;

    public List<LocalDate> computeInMemoryDates() {
        return computeInMemoryDates(LocalDate.now());
    }

    public List<LocalDate> computeInMemoryDates(LocalDate newDate) {
        return IntStream.range(0, daysInMemory).mapToObj(newDate::minusDays).toList();
    }

    public List<LocalDate> computeEndOfMonthDates() {
        // Return the last 12 end of month dates?
        var endOfMonthDates = new ArrayList<LocalDate>();
        var current = LocalDate.now().minusDays(daysInMemory + 1L);
        while (endOfMonthDates.size() < 12) {
            var previous = current.minusDays(1);
            if (!current.getMonth().equals(previous.getMonth())) {
                if (previous.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
                    endOfMonthDates.add(current.minusDays(2));
                } else if (previous.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
                    endOfMonthDates.add(current.minusDays(1));
                } else {
                    endOfMonthDates.add(previous);
                }
            }
            current = previous;
        }
        return endOfMonthDates;
    }
}
