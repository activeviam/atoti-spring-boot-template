/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg;

import java.util.List;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;

import com.activeviam.tech.core.api.registry.Registry;

/**
 * Register the project {@link Registry}
 * {@link https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.application-events-and-listeners}
 */
public class RegistryInitializationListener implements ApplicationListener<ApplicationPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        // Remember to include your package, such as `com.yourdomain`, otherwise the custom plugins from that
        Registry.initialize(Registry.RegistryContributions.builder()
                .packagesToScan(List.of("com.activeviam.apps"))
                .build());
    }
}
