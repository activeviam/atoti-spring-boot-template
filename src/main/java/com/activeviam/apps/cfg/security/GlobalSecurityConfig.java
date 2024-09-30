/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.activeviam.web.spring.api.security.CompositeUserDetailsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@EnableGlobalAuthentication
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
@Slf4j
public class GlobalSecurityConfig {
    private final UserDetailsService technicalUserDetailsService;
    private final UserDetailsService inMemoryUserDetailsService;

    @Bean
    public AuthenticationManager globalAuthenticationManager(List<AuthenticationProvider> authenticationProviders) {
        var authenticationManager = new ProviderManager(authenticationProviders);
        authenticationManager.setEraseCredentialsAfterAuthentication(false);
        return authenticationManager;
    }

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return new CompositeUserDetailsService(Arrays.asList(technicalUserDetailsService, inMemoryUserDetailsService));
    }
}
