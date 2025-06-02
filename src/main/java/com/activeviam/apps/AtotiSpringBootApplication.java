/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps;

import static com.activeviam.tech.core.api.properties.ActiveViamProperty.THROW_ON_DIFFERENT_MEASURE_NAMES_IN_DATA_NODES;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.activeviam.apps.annotations.ConditionalOnOtelJavaAgent;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

@SpringBootApplication
@EnableWebMvc
@ConfigurationPropertiesScan
public class AtotiSpringBootApplication {

    static {
        System.setProperty(THROW_ON_DIFFERENT_MEASURE_NAMES_IN_DATA_NODES.getKey(), "false");
    }

    public static void main(String[] args) {
        SpringApplication.run(AtotiSpringBootApplication.class, args);
    }

    // FIXME https://activeviam.atlassian.net/browse/PIVOT-10624
    @ConditionalOnOtelJavaAgent
    @Bean
    @Order(Integer.MIN_VALUE)
    OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }
}
