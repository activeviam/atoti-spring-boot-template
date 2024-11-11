/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

@SpringBootApplication
@EnableWebMvc
@ConfigurationPropertiesScan
public class AtotiSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtotiSpringBootApplication.class, args);
    }

    // in case you have an OpenTelemetry agent started with your app
    @ConditionalOnOtelJavaAgent
    @Bean
    @Order(Integer.MIN_VALUE)
    OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }
}
