/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class InMemoryAuthenticationConfig {
    private final InMemoryUserDetailsManager basicUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public InMemoryAuthenticationConfig(
            InMemoryAuthenticationProperties authenticationProperties, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        basicUserDetailsService = new InMemoryUserDetailsManager();

        var users = authenticationProperties.getUsers();
        if (!CollectionUtils.isEmpty(users)) {
            users.forEach(user -> basicUserDetailsService.createUser(User.withUsername(user.username())
                    .password(user.password())
                    .authorities(user.authorities().toArray(new String[0]))
                    .build()));
        } else {
            log.warn("No basic auth users have been configured!");
        }
    }

    @Bean
    public UserDetailsService inMemoryUserDetailsService() {
        return basicUserDetailsService;
    }

    @Bean
    public AuthenticationProvider inMemoryAuthenticationProvider() {
        var authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        authenticationProvider.setUserDetailsService(basicUserDetailsService);
        return authenticationProvider;
    }
}
