/*
 * Copyright (C) ActiveViam 2019-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.content;

import static com.activeviam.apps.content.ContentServiceProperties.CONTENT_SERVER_PROPERTY;
import static com.activeviam.apps.content.ContentServiceProperties.CONTENT_SERVICE_PROPERTIES_PREFIX;
import static com.activeviam.apps.content.ContentServiceProperties.EMBEDDED_CONTENT_SERVER;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * The condition that checks whether the cube is data node.
 *
 * @author ActiveViam
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ConditionalOnProperty(
        value = CONTENT_SERVICE_PROPERTIES_PREFIX + "." + CONTENT_SERVER_PROPERTY,
        havingValue = EMBEDDED_CONTENT_SERVER)
public @interface ConditionalOnEmbeddedContentServer {}
