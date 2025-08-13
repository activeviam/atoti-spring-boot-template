/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.conditions;

import lombok.Getter;

@Getter
public class MeasureCondition implements LogicalCondition {

    public MeasureCondition(String measure, String operator, Number operand) {
        this.measure = measure;
        this.operator = Operator.fromString(operator);
        this.operand = operand;
    }

    private final String measure;
    private final Operator operator;
    private final Number operand;

    public enum Operator {
        EQ("="),
        LS("<"),
        LEQ("<="),
        GT(">"),
        GEQ(">=");

        @Getter
        private final String mdxOperator;

        Operator(String operator) {
            mdxOperator = operator;
        }

        public static Operator fromString(String operator) {
            return switch (operator) {
                case "=":
                    yield EQ;
                case "<":
                    yield LS;
                case ">=":
                    yield GEQ;
                case "<=":
                    yield LEQ;
                case ">":
                    yield GT;
                default:
                    throw new IllegalArgumentException("Unknown operator: " + operator);
            };
        }
    }
}
