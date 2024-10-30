/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.content;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.reactive.function.client.WebClient;

import com.activeviam.activepivot.server.intf.api.entitlements.IActivePivotContentService;
import com.activeviam.activepivot.server.spring.api.config.IActivePivotContentServiceConfig;
import com.activeviam.activepivot.server.spring.api.content.ActivePivotContentServiceBuilder;
import com.activeviam.apps.content.ConditionalOnRemoteContentServer;
import com.activeviam.apps.content.ContentServiceSecurityProperties;
import com.activeviam.apps.content.RemoteContentServerProperties;
import com.activeviam.tech.contentserver.spring.api.RemoteContentServiceBuilder;
import com.activeviam.tech.contentserver.spring.api.rest.ContentServiceRestServices;
import com.activeviam.tech.contentserver.storage.api.IContentService;
import com.activeviam.web.spring.api.config.IJwtConfig;
import com.activeviam.web.spring.api.jwt.JwtUserAuthenticator;
import com.activeviam.web.spring.api.security.JwtAuthenticator;

import lombok.RequiredArgsConstructor;

/**
 * Spring configuration of the <b>Content Service</b> backed by a remote <b>Content Server</b>.
 *
 * @author ActiveViam
 */
@ConditionalOnRemoteContentServer
@Configuration
@RequiredArgsConstructor
public class RemoteContentServerConfig implements IActivePivotContentServiceConfig {
    private final IJwtConfig jwtConfig;
    private final UserDetailsService userDetailsService;
    private final ContentServiceSecurityProperties contentServiceSecurityProperties;
    private final RemoteContentServerProperties remoteContentServerProperties;
    private final WebClient.Builder webClientBuilder;

    @Override
    @Bean
    public IContentService contentService() {
        var jwtService = jwtConfig.jwtService();
        return RemoteContentServiceBuilder.create()
                .apiRootUri(remoteContentServerProperties.getUrl() + "/" + ContentServiceRestServices.CONTENT_NAMESPACE)
                .rootAuthenticator(new JwtUserAuthenticator(
                        remoteContentServerProperties.getUser(), jwtService, userDetailsService))
                .userAuthenthicator(new JwtAuthenticator(jwtService))
                .webClientBuilder(webClientBuilder)
                .build();
    }

    /**
     * @return a content service backed by a remote content server.
     */
    @Override
    @Bean(destroyMethod = "close")
    public IActivePivotContentService activePivotContentService() {
        return new ActivePivotContentServiceBuilder()
                .with(contentService())
                .withCacheForEntitlements(contentServiceSecurityProperties
                        .getCacheEntitlementsTtl()
                        .toSeconds())
                .build();
    }
}
