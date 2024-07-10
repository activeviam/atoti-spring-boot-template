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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.server.spring.private_.config.TracingConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAutoConfiguration
@ComponentScan
@ConfigurationPropertiesScan
@Configuration
@Import({

//        // Core imports
//        ActivePivotServicesConfig.class,
//
//        // Security
//        FullAccessBranchPermissionsManagerConfig.class,
//        NoSecurityDatabaseServiceConfig.class,
//
//        // REST services for ActiveUI
//        ActiveViamRestServicesConfig.class,
//        ActiveViamWebSocketServicesConfig.class,
//
//        // Internationalization
//        LocalI18nConfig.class,
//
//        MonitoringJmxConfig.class,
//        MonitoredDataLoadingConfig.class,
//        QueryPerformanceEvaluatorConfig.class,
//        ExtraLoggingConfig.class,
        TracingConfig.class,

})
public @interface ActivePivotApplication {
}
