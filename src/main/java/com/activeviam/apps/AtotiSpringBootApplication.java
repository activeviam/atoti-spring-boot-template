/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.activeviam.tech.core.api.tracking.Tracing;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

@SpringBootApplication
@EnableWebMvc
@ConfigurationPropertiesScan
public class AtotiSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtotiSpringBootApplication.class, args);
        OpenTelemetryAppender.install(Tracing.getEffectiveOtelInstance());
    }
}
