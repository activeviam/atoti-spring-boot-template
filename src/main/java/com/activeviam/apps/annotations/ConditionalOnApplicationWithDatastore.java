/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.annotations;

import static com.activeviam.apps.constants.PropertyConstants.DATABASE_PROPERTIES_PREFIX;
import static com.activeviam.apps.constants.PropertyConstants.DATABASE_TYPE_DATASTORE;
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
        prefix = DATABASE_PROPERTIES_PREFIX,
        name = TYPE_PROPERTY,
        matchIfMissing = true,
        havingValue = DATABASE_TYPE_DATASTORE)
public @interface ConditionalOnApplicationWithDatastore {}
