/*
 * Copyright (C) ActiveViam 2023
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@EnableGlobalAuthentication
@EnableWebSecurity(debug = true)
@Configuration
@RequiredArgsConstructor
@Slf4j
public class GlobalSecurityConfig {

    @Bean
    public AuthenticationManager globalAuthenticationManager(
            final List<AuthenticationProvider> authenticationProviders) {
        final var authenticationManager = new ProviderManager(authenticationProviders);
        authenticationManager.setEraseCredentialsAfterAuthentication(false);
        return authenticationManager;
    }


}
