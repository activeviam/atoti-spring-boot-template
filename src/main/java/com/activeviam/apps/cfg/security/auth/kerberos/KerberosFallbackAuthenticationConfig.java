/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.kerberos;

import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_PROP;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_ADMIN;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_USER;
import static com.activeviam.springboot.atoti.server.starter.api.LoginLogoutUrls.LOGIN_PAGE_URL;
import static com.activeviam.web.core.private_.UrlUtils.url;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.GlobalSunJaasKerberosConfig;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.activeviam.web.spring.api.security.IAtotiServerFilters;
import com.activeviam.web.spring.api.security.dsl.AtotiServerHumanDsl;
import com.activeviam.web.spring.api.security.dsl.HumanToMachineSecurityDsl;
import com.activeviam.web.spring.internal.config.JwtRestServiceConfig;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = MODE_PROP, havingValue = "dumb")
@RequiredArgsConstructor
@Slf4j
public class KerberosFallbackAuthenticationConfig {
    private final KerberosSecurityProperties kerberosSecurityProperties;

    @Bean
    public UserDetailsService kerberosUserDetailsService() {
        return username -> new User(
                username, StringUtils.EMPTY, AuthorityUtils.createAuthorityList(ROLE_USER, ROLE_ADMIN, "ROLE_CS_ROOT"));
    }

    @Bean
    KerberosAuthenticationProvider kerberosAuthenticationProvider(UserDetailsService kerberosUserDetailsService) {
        var authenticationProvider = new KerberosAuthenticationProvider();
        var client = new SunJaasKerberosClient();
        client.setDebug(kerberosSecurityProperties.isDebug());
        authenticationProvider.setKerberosClient(client);
        authenticationProvider.setUserDetailsService(kerberosUserDetailsService);
        return authenticationProvider;
    }

    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint(LOGIN_PAGE_URL);
    }

    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(
            KerberosAuthenticationProvider kerberosAuthenticationProvider,
            KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider) {
        var filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(
                new ProviderManager(kerberosAuthenticationProvider, kerberosServiceAuthenticationProvider));
        return filter;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider(
            SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator,
            UserDetailsService kerberosUserDetailsService) {
        var provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(sunJaasKerberosTicketValidator);
        provider.setUserDetailsService(kerberosUserDetailsService);
        return provider;
    }

    @Bean
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
        var ticketValidator = new SunJaasKerberosTicketValidator();
        ticketValidator.setRealmName(kerberosSecurityProperties.getRealm());
        ticketValidator.setServicePrincipal(kerberosSecurityProperties.getServicePrincipal());
        ticketValidator.setKeyTabLocation(kerberosSecurityProperties.getKeytabLocation());
        ticketValidator.setDebug(kerberosSecurityProperties.isDebug());
        return ticketValidator;
    }

    @Bean
    @SneakyThrows
    public GlobalSunJaasKerberosConfig globalSunJaasKerberosConfig() {
        var kerberosConfig = new GlobalSunJaasKerberosConfig();
        kerberosConfig.setKrbConfLocation(
                kerberosSecurityProperties.getKrbConfLocation().getFile().getAbsolutePath());
        kerberosConfig.setDebug(kerberosSecurityProperties.isDebug());
        return kerberosConfig;
    }

    //    @Bean
    //    @Order(100)
    //    public SecurityFilterChain customJwtSecurityFilterChain(
    //            HttpSecurity http, HumanToMachineSecurityDsl kerberosHumanToMachineSecurityDsl) throws Exception {
    //        /* /activeviam/jwt/rest/v3 */
    //        return http.with(kerberosHumanToMachineSecurityDsl, Customizer.withDefaults())
    //                .securityMatcher(url(JwtRestServiceConfig.REST_API_URL_PREFIX, "/**"))
    //                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS)
    //                        .permitAll()
    //                        .anyRequest()
    //                        .hasAnyAuthority(ROLE_ADMIN, ROLE_USER))
    //                .build();
    //    }

    @Bean
    @Order(100)
    public SecurityFilterChain customJwtSecurityFilterChain(
            HttpSecurity http,
            SpnegoEntryPoint spnegoEntryPoint,
            SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter)
            throws Exception {
        return http.anonymous(AbstractHttpConfigurer::disable)
                .exceptionHandling(customizer -> customizer.authenticationEntryPoint(spnegoEntryPoint))
                .sessionManagement(
                        sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .addFilterBefore(spnegoAuthenticationProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .securityMatcher(url(JwtRestServiceConfig.REST_API_URL_PREFIX, "/**"))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS)
                        .permitAll()
                        .anyRequest()
                        .hasAnyAuthority(ROLE_ADMIN, ROLE_USER))
                .build();
    }

    @Bean
    public HumanToMachineSecurityDsl kerberosHumanToMachineSecurityDsl(
            IAtotiServerFilters filters,
            KerberosAuthenticationProvider kerberosAuthenticationProvider,
            KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider,
            SpnegoEntryPoint spnegoEntryPoint,
            SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter) {
        return new AtotiServerHumanDsl(filters) {
            @Override
            protected void configureLoginAccess(HttpSecurity http) throws Exception {
                super.configureLoginAccess(http);
                http.anonymous(AbstractHttpConfigurer::disable)
                        // When using Spnego, we can specify the forward url. It forwards to the core login page as
                        // expected
                        // However, as it is a forward, when we submit the login form, it fails because it tries to hit
                        // /index.html
                        // The same behaviour can be seen if we configure the core LoginUrlAuthenticationEntryPoint to
                        // use a forward instead of a redirect.
                        // .exceptionHandling(customizer -> customizer.authenticationEntryPoint(spnegoEntryPoint))
                        .authenticationProvider(kerberosAuthenticationProvider)
                        .authenticationProvider(kerberosServiceAuthenticationProvider)
                        //                        .sessionManagement(sessionManagement ->
                        //
                        // sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                        .addFilterBefore(
                                spnegoAuthenticationProcessingFilter, UsernamePasswordAuthenticationFilter.class);
            }
        };
    }
}
