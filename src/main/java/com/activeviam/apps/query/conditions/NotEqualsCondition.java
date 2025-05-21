/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.conditions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class NotEqualsCondition<T> implements LogicalCondition {
    public static final String NOT_EQ = "!=";
    private final String field;
    private final T value;

    @Override
    public String operator() {
        return NOT_EQ;
    }
}
