/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.server.spring.private_.config.TracingConfig;
import com.activeviam.activepivot.server.spring.private_.rest.database.NoSecurityDatabaseServiceConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationPropertiesScan
@SpringBootApplication
@Import({
    NoSecurityDatabaseServiceConfig.class,
    TracingConfig.class,
    // ... Any additional imports
})
public @interface ActivePivotApplication {}
