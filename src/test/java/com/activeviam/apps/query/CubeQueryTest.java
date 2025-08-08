/*
 * Copyright (C) ActiveViam 2023-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.activeviam.apps.query.conditions.FilterExpressionConditionVisitor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class CubeQueryTest {
    // Measure filter is always AND. There can be multiple measure filters separated as long as they are ANDs
    private static final List<String> VALID_EXPRESSIONS = List.of(
            "CobDate IN [2025-01-01] AND Notional.Sum AT Desk >= 1.0 AND NOT Desk IN ['Desk_1']",
            "CobDate IN [2025-01-01] AND Notional.Sum AT Desk >= 1.0 AND (NOT Desk IN ['Desk_1'] OR Portfolio IN ['PF_1'])",
            "CobDate IN [2025-01-01] AND (NOT Desk IN ['Desk_1'] OR Portfolio IN ['PF_1']) AND Notional.Sum AT Desk >= 1.0",
            "CobDate IN [2025-01-01] AND (NOT Desk IN ['Desk_1'] OR Portfolio IN ['PF_1']) AND Notional.Sum AT Desk >= 1.0",
            "CobDate IN [2025-01-01] AND (NOT Desk IN ['Desk_1'] OR Portfolio IN ['PF_1']) AND (Notional.Sum AT Desk >= 1.0 OR Notional.Mean AT Desk >= 1.0)",
            "CobDate IN [2025-01-01] AND Notional.Mean AT Desk >= 1.0 AND (NOT Desk IN ['Desk_1'] OR Portfolio IN ['PF_1']) AND Notional.Sum AT Desk >= 1.0",
            "Notional.Mean AT Desk >= 1.0 AND (NOT Desk IN ['Desk_1'] OR Portfolio IN ['PF_1']) AND CobDate IN [2025-01-01] AND Notional.Sum AT Desk >= 1.0");

    // Measure filter is mixed together with other filters in an OR condition
    private static final List<String> INVALID_EXPRESSIONS = List.of(
            "CobDate IN [2025-01-01] AND (Notional.Sum AT Desk >= 1.0 OR Portfolio IN ['PF_1']) AND NOT Desk IN ['Desk_1']");

    @TestFactory
    Stream<DynamicTest> testSplittingMeasureFilter() {
        return VALID_EXPRESSIONS.stream()
                .map(expression -> DynamicTest.dynamicTest(expression, () -> {
                    var filter = FilterExpressionConditionVisitor.parseFilterExpression(expression);
                    var splitFilter = CubeQuery.splitMeasureFilter(filter);
                    var measureFilter = splitFilter.generateMeasuresCondition();
                    var cobDateFilter = splitFilter.generateCobDateCondition();
                    var otherFilter = splitFilter.generateOtherCondition();
                    Assertions.assertThat(CubeQuery.containsMeasureFilter(measureFilter))
                            .isTrue();
                    Assertions.assertThat(CubeQuery.containsOtherFilter(measureFilter))
                            .isFalse();
                    Assertions.assertThat(CubeQuery.containsMeasureFilter(otherFilter))
                            .isFalse();
                    Assertions.assertThat(CubeQuery.isCobDateFilter(cobDateFilter))
                            .isTrue();
                }));
    }

    @TestFactory
    Stream<DynamicTest> testSplittingInvalidMeasureFilter() {
        return INVALID_EXPRESSIONS.stream()
                .map(expression -> DynamicTest.dynamicTest(expression, () -> {
                    var filter = FilterExpressionConditionVisitor.parseFilterExpression(expression);
                    Assertions.assertThatThrownBy(() -> CubeQuery.splitMeasureFilter(filter))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("Filter expression contains measure filter that is not AND");
                }));
    }
}
