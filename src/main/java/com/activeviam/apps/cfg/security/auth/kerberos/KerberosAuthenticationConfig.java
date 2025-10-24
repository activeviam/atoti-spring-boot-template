/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.kerberos;

import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_KERBEROS;
import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_PROP;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_ADMIN;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_USER;
import static com.activeviam.web.core.private_.UrlUtils.url;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
import com.activeviam.web.spring.api.security.dsl.AtotiServerMachineDsl;
import com.activeviam.web.spring.api.security.dsl.HumanToMachineSecurityDsl;
import com.activeviam.web.spring.api.security.dsl.MachineToMachineSecurityDsl;
import com.activeviam.web.spring.internal.config.JwtRestServiceConfig;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = MODE_PROP, havingValue = MODE_KERBEROS)
@RequiredArgsConstructor
@Slf4j
public class KerberosAuthenticationConfig {
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
        return new SpnegoEntryPoint();
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

    @Bean
    @Order(100)
    @SneakyThrows
    public SecurityFilterChain kerberosJwtSecurityFilterChain(
            HttpSecurity http,
            SpnegoEntryPoint spnegoEntryPoint,
            SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter) {
        return http.with(
                        new AtotiServerMachineDsl(null) {
                            @Override
                            protected void configureExceptionHandling(HttpSecurity http) throws Exception {
                                http.exceptionHandling(
                                        customizer -> customizer.authenticationEntryPoint(spnegoEntryPoint));
                            }

                            @Override
                            protected void configureAuthentication(HttpSecurity http) throws Exception {
                                http.anonymous(AbstractHttpConfigurer::disable)
                                        .addFilterBefore(
                                                spnegoAuthenticationProcessingFilter,
                                                UsernamePasswordAuthenticationFilter.class);
                            }
                        },
                        d -> d.setScope(Set.of(MachineToMachineSecurityDsl.Scope.OBSERVABILITY)))
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
            KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider) {
        return new AtotiServerHumanDsl(filters) {
            @Override
            protected void configureLoginAccess(HttpSecurity http) throws Exception {
                super.configureLoginAccess(http);
                // When fallback, we need to use the following providers
                http.authenticationProvider(kerberosAuthenticationProvider)
                        .authenticationProvider(kerberosServiceAuthenticationProvider);
            }
        };
    }
}
