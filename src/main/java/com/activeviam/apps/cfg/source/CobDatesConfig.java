/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.PropertyConstants.COB_DATES_PROPERTIES_PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CobDatesConfig {

    @ConfigurationProperties(prefix = COB_DATES_PROPERTIES_PREFIX)
    @Bean
    CobDatesProperties cobDatesProperties() {
        return new CobDatesProperties();
    }
}
