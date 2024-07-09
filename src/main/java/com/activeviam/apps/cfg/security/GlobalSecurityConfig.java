/*
 * Copyright (C) ActiveViam 2023
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.UserDetailsManager;

import com.activeviam.web.spring.api.jwt.IJwtService;
import com.activeviam.web.spring.api.security.CompositeUserDetailsService;
import com.activeviam.tech.core.api.security.IAuthorityComparator;
import com.activeviam.tech.core.internal.security.AuthorityComparatorAdapter;
import com.activeviam.activepivot.server.spring.private_.pivot.security.impl.UserDetailsServiceWrapper;
import com.activeviam.tech.core.private_.ordering.CustomComparator;
import com.activeviam.tech.core.api.security.IUserDetailsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@EnableGlobalAuthentication
@Configuration
@RequiredArgsConstructor
@Slf4j
public class GlobalSecurityConfig {
    private final UserDetailsManager technicalUserDetailsService;
    private final UserDetailsManager inMemoryUserDetailsService;

    @Bean
    public IUserDetailsService avUserDetailsService() {
        return new UserDetailsServiceWrapper(userDetailsService());
    }

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return new CompositeUserDetailsService(Arrays.asList(technicalUserDetailsService, inMemoryUserDetailsService));
    }

    /**
     * NOTE: JWT authentication provider bean is provided by JwtConfig
     */
    @Bean
    public AuthenticationManager globalAuthenticationManager(List<AuthenticationProvider> authenticationProviders) {
        var authenticationManager = new ProviderManager(authenticationProviders);
        authenticationManager.setEraseCredentialsAfterAuthentication(false);
        return authenticationManager;
    }

    /**
     * [Bean] Comparator for user roles
     * <p>
     * Defines the comparator used by:
     * </p>
     * <ul>
     *   <li>com.activeviam.activepivot.server.impl.private_.security.ContextValueManager#setAuthorityComparator(IAuthorityComparator)</li>
     *   <li>{@link IJwtService}</li>
     * </ul>
     *
     * @return a comparator that indicates which authority/role prevails over another. <b>NOTICE -
     * an authority coming AFTER another one prevails over this "previous" authority.</b>
     * This authority ordering definition is essential to resolve possible ambiguity when,
     * for a given user, a context value has been defined in more than one authority
     * applicable to that user. In such case, it is what has been set for the "prevailing"
     * authority that will be effectively retained for that context value for that user.
     */
    @Bean
    public IAuthorityComparator authorityComparator() {
        var comp = new CustomComparator<String>();
        comp.setLastObjects(List.of(SecurityConstants.ROLE_USER));
        comp.setLastObjects(List.of(SecurityConstants.ROLE_ADMIN));
        return new AuthorityComparatorAdapter(comp);
    }
}
