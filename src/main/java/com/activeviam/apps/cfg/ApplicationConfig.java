/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg;

import java.util.List;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.tech.core.api.agent.AgentException;
import com.activeviam.tech.core.api.registry.Registry;
import com.activeviam.tech.core.api.registry.Registry.RegistryContributions;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import lombok.RequiredArgsConstructor;

/**
 * Spring configuration of the ActivePivot Application services
 *
 * @author ActiveViam
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    /* Before anything else we statically initialize the Registry. */
    static {
        // TODO
        // Remember to include your package, such as `com.yourdomain`, otherwise the custom plugins from that
        Registry.initialize(RegistryContributions.builder()
                .packagesToScan(List.of("com.example"))
                .build());
    }

    private final IActivePivotManager activePivotManager;

    @Bean
    @Order(Integer.MIN_VALUE)
    OpenTelemetry openTelemetry() {
        var openTelemetry = GlobalOpenTelemetry.get();
        OpenTelemetryAppender.install(openTelemetry);
        return openTelemetry;
    }

    /**
     * Initialize and start the ActivePivot Manager, after performing all the injections into the ActivePivot plug-ins.
     *
     * @throws AgentException any exception that occurred during the injection, the initialization or the starting
     */
    @EventListener(ApplicationStartedEvent.class)
    public void startManager() throws AgentException {
        /* *********************************************** */
        /* Initialize the ActivePivot Manager and start it */
        /* *********************************************** */
        activePivotManager.init(null);
        activePivotManager.start();
    }
}
