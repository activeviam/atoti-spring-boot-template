/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.server.intf.api.entitlements.IActivePivotContentService;
import com.activeviam.activepivot.server.spring.private_.pivot.content.impl.DynamicActivePivotContentServiceMBean;
import com.activeviam.database.api.IDatabase;
import com.activeviam.web.spring.internal.JMXEnabler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JMXEnablerConfig {
    private final IActivePivotManager activePivotManager;
    /**
     * ActivePivot content service spring configuration
     */
    private final IActivePivotContentService activePivotContentService;

    private final IDatabase database;

    /**
     * Enable JMX Monitoring for the Datastore
     *
     * @return the {@link JMXEnabler} attached to the database
     */
    @Bean
    public JMXEnabler jmxDatastoreEnabler() {
        return new JMXEnabler(database);
    }

    /**
     * Enable JMX Monitoring for the Content Service
     *
     * @return the {@link JMXEnabler} attached to the content service.
     */
    @Bean
    public JMXEnabler jmxActivePivotContentServiceEnabler() {
        // to allow operations from the JMX bean
        return new JMXEnabler(new DynamicActivePivotContentServiceMBean(activePivotContentService, activePivotManager));
    }
}
