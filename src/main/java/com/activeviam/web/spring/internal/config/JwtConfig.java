/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.web.spring.internal.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.activeviam.tech.core.api.security.IAuthorityComparator;
import com.activeviam.web.spring.api.config.IJwtConfig;
import com.activeviam.web.spring.api.jwt.IJwtService;
import com.activeviam.web.spring.api.jwt.JwtAuthenticationProvider;
import com.activeviam.web.spring.internal.jwt.JwtService;
import com.activeviam.web.spring.internal.jwt.JwtUtil;
import com.activeviam.web.spring.private_.jwt.JwtFilter;

/**
 * FIXME Remove this class once a fix has been provided for https://activeviam.atlassian.net/browse/PIVOT-11616
 * Spring declaration of the {@link IJwtService} and its associated {@link AuthenticationProvider}
 * based on RSA key pair.
 *
 * @see JwtUtil
 * @author ActiveViam
 */
@Configuration
public class JwtConfig implements IJwtConfig {

    /** Application context. */
    private final ApplicationContext context;

    private final JwtProperties properties;

    /** Spring constructor. */
    public JwtConfig(
            Environment env, ApplicationContext context, @Autowired(required = false) JwtProperties properties) {
        this.context = context;
        this.properties = Objects.requireNonNullElseGet(properties, () -> JwtProperties.createFromEnv(env));
    }

    @Bean
    @Override
    public IJwtService jwtService() {
        if (properties.enabled()) {
            return JwtService.builder()
                    .authorityComparator(getComparator())
                    .expiration(getExpiration())
                    .privateKey(getPrivateKey())
                    .build();
        } else {
            return null;
        }
    }

    /** Authentication provider to set users from JWTokens. */
    @Bean
    @Primary
    public JwtAuthenticationProvider jwtAuthenticationProvider() {
        UserDetailsService userDetailsService;
        if (properties.checkUserDetails()) {
            userDetailsService = context.getBean(UserDetailsService.class);
        } else {
            userDetailsService = null;
        }
        return JwtAuthenticationProvider.builder()
                .publicKey(getPublicKey())
                .userDetailsService(userDetailsService)
                .principalClaimKey(properties.principalClaimKey())
                .authoritiesClaimKey(properties.authorityClaimKey())
                .build();
    }

    /**
     * JwtFilter for jwt authentication.
     * FIXME hacked to pass the provider and use the ProviderManager
     */
    @Bean
    public JwtFilter jwtFilter(JwtAuthenticationProvider jwtAuthenticationProvider) {
        return new JwtFilter(
                new ProviderManager(jwtAuthenticationProvider),
                properties.principalClaimKey(),
                properties.authorityClaimKey());
    }

    /** Disabled FilterRegistrationBean to prevent Spring boot from creating a default global one. */
    @Bean
    FilterRegistrationBean<JwtFilter> jwtFilterRegistrationBean(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** Returns the public key used to verify the integrity of the tokens. */
    public final RSAPublicKey getPublicKey() {
        String public64 = properties.publicKey();
        return JwtUtil.parseRSAPublicKey(public64);
    }

    /** Returns the private key used to sign the tokens. */
    protected final RSAPrivateKey getPrivateKey() {
        String privateKey64 = properties.privateKey();
        return JwtUtil.parseRSAPrivateKey(privateKey64);
    }

    /** Returns the lifetime (in seconds) of the tokens. */
    protected final int getExpiration() {
        return Math.toIntExact(properties.expiration().toSeconds());
    }

    /**
     * Gets a comparator that indicates which authority prevails over another.
     *
     * <p><b>NOTICE - an authority coming AFTER another one prevails over this "previous"
     * authority.</b>
     */
    protected final IAuthorityComparator getComparator() {
        return context.getBean(IAuthorityComparator.class);
    }
}
