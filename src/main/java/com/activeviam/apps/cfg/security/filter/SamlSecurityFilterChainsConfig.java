/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.filter;

import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_PROP;
import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_SAML;
import static com.activeviam.apps.cfg.security.auth.rest.SamlLoginController.ERROR_PARAM;
import static com.activeviam.apps.cfg.security.filter.CustomWebSecurityFiltersConfig.WILDCARD;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_ADMIN;
import static com.activeviam.springboot.atoti.server.starter.api.LoginLogoutUrls.LOGIN_ENTRYPOINT_URL;
import static com.activeviam.springboot.atoti.server.starter.api.LoginLogoutUrls.LOGOUT_PAGE_URL;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.ParameterRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;
import org.springframework.web.filter.OncePerRequestFilter;

import com.activeviam.apps.cfg.security.auth.SavedRequestAwareTargetUrlAuthenticationSuccessHandler;
import com.activeviam.web.spring.api.security.dsl.HumanToMachineSecurityDsl;
import com.activeviam.web.spring.api.security.dsl.MachineToMachineSecurityDsl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@ConditionalOnProperty(name = MODE_PROP, havingValue = MODE_SAML)
@RequiredArgsConstructor
public class SamlSecurityFilterChainsConfig {

    @Bean
    @Order(-1)
    public SecurityFilterChain uiLoginFilterChain(HttpSecurity http) throws Exception {
        var requestCache =
                Optional.ofNullable(http.getSharedObject(RequestCache.class)).orElse(new HttpSessionRequestCache());
        return http.securityMatcher(UiLoginFilter.LOGIN_MATCHER)
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(UiLoginFilter.LOGIN_MATCHER).permitAll())
                .addFilterAfter(new UiLoginFilter(requestCache), Saml2WebSsoAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain staticLogoFilterChain(HttpSecurity http) throws Exception {
        var patterns = new String[] {"/logo-*", "/logo-*/" + WILDCARD};
        return http.securityMatcher(patterns)
                .authorizeHttpRequests(auth -> auth.requestMatchers(patterns).permitAll())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain uiLogoutFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher(LOGOUT_PAGE_URL)
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(LOGOUT_PAGE_URL).permitAll())
                .build();
    }

    /**
     * Give USERS access to all other services.
     *
     * @param http
     * @param m2mDsl
     * @param h2mDsl
     * @param securityContextRepository
     * @return a filter chain that allows the core's JwtFilter to be used for authentication and
     * authorizes all non-mapped requests to be accessed by anyone with `ROLE_USER`.
     * @throws Exception
     */
    @Bean
    @Order(1000)
    public SecurityFilterChain allEndpointsSecurityFilterChain(
            HttpSecurity http,
            MachineToMachineSecurityDsl m2mDsl,
            HumanToMachineSecurityDsl h2mDsl,
            @Autowired(required = false) SecurityContextRepository securityContextRepository)
            throws Exception {
        http.with(m2mDsl, d -> d.setScope(Set.of(MachineToMachineSecurityDsl.Scope.SECURITY)))
                // Add the HumanToMachine DSL to handle the SAML specific URLs
                .with(h2mDsl, Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyAuthority(ROLE_ADMIN));

        if (securityContextRepository != null) {
            http.securityContext(sc -> sc.securityContextRepository(securityContextRepository));
        }
        return http.build();
    }

    @RequiredArgsConstructor
    public static class UiLoginFilter extends OncePerRequestFilter {
        public static final RequestMatcher LOGIN_MATCHER = RequestMatchers.allOf(
                PathPatternRequestMatcher.withDefaults().matcher(LOGIN_ENTRYPOINT_URL),
                new OrRequestMatcher(
                        new ParameterRequestMatcher(
                                SavedRequestAwareTargetUrlAuthenticationSuccessHandler.TARGET_URL_PARAM),
                        new ParameterRequestMatcher(ERROR_PARAM)));
        public static final RequestMatcher REDIRECT_MATCHER = RequestMatchers.allOf(
                PathPatternRequestMatcher.withDefaults().matcher(LOGIN_ENTRYPOINT_URL),
                new ParameterRequestMatcher(SavedRequestAwareTargetUrlAuthenticationSuccessHandler.TARGET_URL_PARAM));
        private static final String SAML_AUTHENTICATE_URL = "/saml2/authenticate?registrationId=okta";

        private final RequestCache requestCache;

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (REDIRECT_MATCHER.matcher(request).isMatch()) {
                requestCache.saveRequest(request, response);
                // Auto redirect to the authentication SAML connection page
                //                    var uriBuilder =
                //                            UriComponentsBuilder.fromUriString(request.getContextPath() +
                // SAML_AUTHENTICATE_URL);
                //                    response.sendRedirect(uriBuilder.build(true).toUriString());
            }
            filterChain.doFilter(request, response);
        }
    }
}
