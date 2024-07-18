/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.filter;

import static com.activeviam.apps.constants.SecurityConstants.ROLE_USER;
import static com.activeviam.web.core.api.IUrlBuilder.url;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;

import com.activeviam.apps.rest.EndpointConstants;
import com.activeviam.web.spring.api.security.IAtotiServerFilters;
import com.activeviam.web.spring.api.security.dsl.AtotiServerHumanDsl;
import com.activeviam.web.spring.api.security.dsl.HumanToMachineSecurityDsl;
import com.activeviam.web.spring.api.security.dsl.MachineToMachineSecurityDsl;

import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
public class CustomWebSecurityFiltersConfig {
    public static final String WILDCARD = "**";

    @Bean("AtotiServerAdminUiStarterHumanToMachineDefaultSecurityDsl")
    public HumanToMachineSecurityDsl humanToMachineDsl(@Autowired(required = false) IAtotiServerFilters filters) {
        return new AtotiServerHumanDsl(filters).requestLogin("/login/index.html");
    }

    @ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
    @Bean
    @Order(4)
    public SecurityFilterChain h2ConsoleSecurityFilterChain(
            HttpSecurity http, MvcRequestMatcher.Builder mvc, H2ConsoleProperties h2ConsoleProperties)
            throws Exception {
        return http.securityMatcher(
                        mvc.servletPath(h2ConsoleProperties.getPath()).pattern(url(WILDCARD)))
                .headers(httpSecurityHeadersConfigurer ->
                        httpSecurityHeadersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(5)
    public SecurityFilterChain swaggerUiSecurityFilterChain(
            HttpSecurity http,
            MvcRequestMatcher.Builder mvc,
            HumanToMachineSecurityDsl dsl,
            SwaggerUiConfigProperties swaggerUiConfigProperties)
            throws Exception {
        return http.with(dsl, Customizer.withDefaults())
                .securityMatcher(mvc.pattern(swaggerUiConfigProperties.getPath()))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    @Bean
    @Order(6)
    public SecurityFilterChain customRestEndpointsSecurityFilterChain(
            HttpSecurity http, MvcRequestMatcher.Builder mvc, MachineToMachineSecurityDsl dsl) throws Exception {
        return http.with(dsl, Customizer.withDefaults())
                .securityMatcher(mvc.pattern(url(EndpointConstants.CUSTOM_REST_PATH, WILDCARD)))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyAuthority(ROLE_USER))
                .build();
    }
}
