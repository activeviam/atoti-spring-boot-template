/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.tracing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {
    
    @Bean
    @ConditionalOnProperty(name = "spring.application.name")
    public Void setupTracing(@Value("${spring.application.name}") String appName) {
        TracingUtil.setDefaultTracingScopeName(appName);
        return null;
    }
    
}
