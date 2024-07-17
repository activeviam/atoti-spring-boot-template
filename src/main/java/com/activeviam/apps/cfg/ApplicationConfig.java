/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg;

import java.util.List;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.tech.core.api.agent.AgentException;
import com.activeviam.tech.core.api.registry.Registry;
import com.activeviam.tech.core.api.registry.Registry.RegistryContributions;

import lombok.RequiredArgsConstructor;

/**
 * Spring configuration of the ActivePivot Application services
 *
 * @author ActiveViam
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    /* Before anything else we statically initialize the Quartet FS Registry. */
    static {
        // TODO
        // Remember to include your package, such as `com.yourdomain`, otherwise the custom plugins from that
        Registry.initialize(RegistryContributions.builder()
                .packagesToScan(List.of("com.activeviam.apm", "com.activeviam"))
                .build());
    }

    private final IActivePivotManager activePivotManager;

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
