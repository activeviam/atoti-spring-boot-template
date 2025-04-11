/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.annotations;

import static com.activeviam.apps.constants.PropertyConstants.DISTRIBUTION_PROPERTIES_PREFIX;
import static com.activeviam.apps.constants.PropertyConstants.DISTRIBUTION_TYPE_DATA;
import static com.activeviam.apps.constants.PropertyConstants.TYPE_PROPERTY;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(
        prefix = DISTRIBUTION_PROPERTIES_PREFIX,
        name = TYPE_PROPERTY,
        havingValue = DISTRIBUTION_TYPE_DATA)
public @interface ConditionalOnDataNode {}
