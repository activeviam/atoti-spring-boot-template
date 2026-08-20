/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.filter;

import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_ADMIN;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_USER;
import static com.activeviam.web.core.api.IUrlBuilder.url;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.utils.Constants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.activeviam.apps.rest.DataMaintenanceController;
import com.activeviam.apps.rest.DistributionInfoController;
import com.activeviam.apps.rest.EndpointConstants;
import com.activeviam.apps.rest.MaskingController;
import com.activeviam.springboot.atoti.server.starter.api.LoginLogoutUrls;
import com.activeviam.web.spring.api.security.dsl.HumanToMachineSecurityDsl;
import com.activeviam.web.spring.api.security.dsl.MachineToMachineSecurityDsl;

import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
public class CustomWebSecurityFiltersConfig {
    public static final String WILDCARD = "**";

    /**
     * Add the H2 console which is by default secured in itself. In a real project this should be exposed only for
     * a local profile as a "standard" DB would be used.
     * @param http
     * @return
     * @throws Exception
     */
    @ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
    @Bean
    @Order(4)
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher(PathRequest.toH2Console())
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(httpSecurityHeadersConfigurer ->
                        httpSecurityHeadersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(5)
    public SecurityFilterChain swaggerUiSecurityFilterChain(
            HttpSecurity http, HumanToMachineSecurityDsl dsl, SwaggerUiConfigProperties swaggerUiConfigProperties)
            throws Exception {
        return http.with(dsl, c -> c.requestLogin(LoginLogoutUrls.LOGIN_PAGE_URL))
                .securityMatcher(
                        StringUtils.defaultIfBlank(
                                swaggerUiConfigProperties.getPath(), Constants.DEFAULT_SWAGGER_UI_PATH),
                        Constants.SWAGGER_UI_PREFIX + Constants.ALL_PATTERN)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyAuthority(ROLE_ADMIN))
                .build();
    }

    /**
     * Masking/unmasking a distributing level directly impacts what a data node serves to the query node, so
     * this is admin-only, unlike the rest of the custom REST endpoints. Its matcher is a subset of {@link
     * #customRestEndpointsSecurityFilterChain}'s, so it must run first (lower {@link Order}).
     */
    @Bean
    @Order(6)
    public SecurityFilterChain maskingEndpointsSecurityFilterChain(
            HttpSecurity http, PathPatternRequestMatcher.Builder mvc, MachineToMachineSecurityDsl dsl)
            throws Exception {
        return http.with(dsl, Customizer.withDefaults())
                .securityMatcher(mvc.matcher(url(MaskingController.MASKING_ENDPOINT, WILDCARD)))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyAuthority(ROLE_ADMIN))
                .build();
    }

    /**
     * Deleting/restoring data straight on the Dremio backend is likewise admin-only; same ordering
     * constraint as {@link #maskingEndpointsSecurityFilterChain}.
     */
    @Bean
    @Order(7)
    public SecurityFilterChain dataMaintenanceEndpointsSecurityFilterChain(
            HttpSecurity http, PathPatternRequestMatcher.Builder mvc, MachineToMachineSecurityDsl dsl)
            throws Exception {
        return http.with(dsl, Customizer.withDefaults())
                .securityMatcher(mvc.matcher(url(DataMaintenanceController.DATA_ENDPOINT, WILDCARD)))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyAuthority(ROLE_ADMIN))
                .build();
    }

    /**
     * The live routing-table check ({@link DistributionInfoController}) is admin-only for the same
     * reason as masking - it's infra-level diagnostic information, not application data.
     */
    @Bean
    @Order(8)
    public SecurityFilterChain distributionInfoEndpointsSecurityFilterChain(
            HttpSecurity http, PathPatternRequestMatcher.Builder mvc, MachineToMachineSecurityDsl dsl)
            throws Exception {
        return http.with(dsl, Customizer.withDefaults())
                .securityMatcher(mvc.matcher(url(DistributionInfoController.DISTRIBUTION_ENDPOINT, WILDCARD)))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyAuthority(ROLE_ADMIN))
                .build();
    }

    @Bean
    @Order(9)
    public SecurityFilterChain customRestEndpointsSecurityFilterChain(
            HttpSecurity http, PathPatternRequestMatcher.Builder mvc, MachineToMachineSecurityDsl dsl)
            throws Exception {
        return http.with(dsl, Customizer.withDefaults())
                .securityMatcher(mvc.matcher(url(EndpointConstants.CUSTOM_REST_PATH, WILDCARD)))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyAuthority(ROLE_USER))
                .build();
    }
}
