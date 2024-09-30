/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth;

import static com.activeviam.apps.constants.SecurityConstants.ROLE_ACTUATOR;
import static com.activeviam.apps.constants.SecurityConstants.ROLE_TECH;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class TechnicalAuthenticationSecurityConfig {
    public static final String PIVOT_TECH_USER_LOGIN = "pivot";
    public static final String SBA_TECH_USER_LOGIN = "sba";

    private final InMemoryUserDetailsManager techUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public TechnicalAuthenticationSecurityConfig(
            SecurityTechUserPasswordsProperties techUserPasswordsProperties, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        techUserDetailsService = new InMemoryUserDetailsManager();

        techUserDetailsService.createUser(User.withUsername(PIVOT_TECH_USER_LOGIN)
                .password(techUserPasswordsProperties.getPivot())
                .authorities(ROLE_TECH)
                .build());

        techUserDetailsService.createUser(User.withUsername(SBA_TECH_USER_LOGIN)
                .password(techUserPasswordsProperties.getSba())
                .authorities(ROLE_ACTUATOR)
                .build());
    }

    @Bean
    public UserDetailsService technicalUserDetailsService() {
        return techUserDetailsService;
    }

    @Bean
    public AuthenticationProvider technicalAuthenticationProvider() {
        var authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        authenticationProvider.setUserDetailsService(techUserDetailsService);
        return authenticationProvider;
    }
}
