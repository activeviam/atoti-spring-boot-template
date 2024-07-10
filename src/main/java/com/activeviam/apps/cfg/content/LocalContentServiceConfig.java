/*
 * Copyright (C) ActiveViam 2023
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.content;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.activeviam.activepivot.server.intf.api.entitlements.IActivePivotContentService;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotContentServiceConfig;
import com.activeviam.activepivot.server.spring.api.content.ActivePivotContentServiceBuilder;
import com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties;
import com.activeviam.tech.contentserver.spring.api.config.ActiveUIContentServiceUtil;
import com.activeviam.tech.contentserver.storage.api.IContentService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class LocalContentServiceConfig implements IActivePivotContentServiceConfig {

    private static final Logger logger = LoggerFactory.getLogger(LocalContentServiceConfig.class);

    private final AtotiSecurityProperties securityProperties;
    private final Environment env;

    @ConfigurationProperties(prefix = "content")
    @Bean
    public Properties contentServiceProperties() {
        return new Properties();
    }

    @Override
    @Bean
    public IContentService contentService() {
        final var contentService = IContentService.builder().inMemory().build();
        // initialize the ActiveUI structure required on the ContentService side
        ActiveUIContentServiceUtil.initialize(contentService);
        logger.info("Initialized the contentServer with the required structure to work with ActiveUI.");
        return contentService;
    }

    @Override
    @Bean
    public IActivePivotContentService activePivotContentService() {
        final var builder =
                new ActivePivotContentServiceBuilder()
                        .with(contentService())
                        .withCacheForEntitlements(
                                // TODO Refactor CS properties, delete env
                                Long.parseLong(
                                        env.getProperty("contentServer.security.cache.entitlementsTTL", "3600")));

        securityProperties
                .getRolesAdmin()
                .forEach(adminUser -> builder.needInitialization(adminUser, adminUser));

        return builder.build();
    }
}
