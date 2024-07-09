/*
 * Copyright (C) ActiveViam 2023
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.content;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.hikaricp.internal.HikariConfigurationUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.activeviam.activepivot.server.spring.private_.config.TracingConfig;
import com.activeviam.tech.contentserver.spring.api.config.ActiveUIContentServiceUtil;
import com.activeviam.tech.contentserver.storage.api.IContentService;
import com.activeviam.tech.contentserver.storage.private_.AHibernateContentService;
import com.activeviam.tech.contentserver.storage.private_.HibernateContentService;
import com.activeviam.activepivot.server.intf.api.entitlements.IActivePivotContentService;
import com.activeviam.activepivot.server.spring.api.content.ActivePivotContentServiceBuilder;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotContentServiceConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LocalContentServiceConfig implements IActivePivotContentServiceConfig {
    private final EmbeddedContentServerHibernateProperties embeddedContentServerHibernateProperties;
    private final ContentServiceSecurityProperties contentServiceSecurityProperties;

    @Override
    @Bean
    @DependsOn(TracingConfig.TRACING_BEAN)
    public IContentService contentService() {
        var contentService = new HibernateContentService(getHibernateConfiguration());
        // initialize the ActiveUI structure required on the ContentService side
        ActiveUIContentServiceUtil.initialize(contentService);
        return contentService;
    }

    private org.hibernate.cfg.Configuration getHibernateConfiguration() {
        log.info(
                "Will be connecting to Content Server DB located at: {}",
                embeddedContentServerHibernateProperties.getProperty(
                        HikariConfigurationUtil.CONFIG_PREFIX + "dataSource.url"));
        var configuration =
                new org.hibernate.cfg.Configuration().addProperties(embeddedContentServerHibernateProperties);
        configuration
                .getProperties()
                .putIfAbsent(AvailableSettings.STATEMENT_BATCH_SIZE, AHibernateContentService.INSERT_BATCH_SIZE);
        return configuration;
    }

    @Override
    @Bean(destroyMethod = "close")
    public IActivePivotContentService activePivotContentService() {
        var cs = contentService();
        return new ActivePivotContentServiceBuilder()
                .with(cs)
                .withCacheForEntitlements(contentServiceSecurityProperties
                        .getCacheEntitlementsTtl()
                        .toSeconds())
                .needInitialization(
                        contentServiceSecurityProperties.getCalculatedMemberRole(),
                        contentServiceSecurityProperties.getKpiRole())
                .build();
    }
}
