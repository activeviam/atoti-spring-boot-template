/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.saml;

import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_PROP;
import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_SAML;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_ADMIN;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_USER;
import static com.activeviam.springboot.atoti.server.starter.api.LoginLogoutUrls.LOGOUT_PAGE_URL;
import static org.springframework.security.config.Customizer.withDefaults;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.util.CollectionUtils;

import com.activeviam.apps.cfg.security.auth.SavedRequestAwareTargetUrlAuthenticationSuccessHandler;
import com.activeviam.web.spring.api.security.IAtotiServerFilters;
import com.activeviam.web.spring.api.security.dsl.AtotiServerHumanDsl;
import com.activeviam.web.spring.api.security.dsl.HumanToMachineSecurityDsl;

import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = MODE_PROP, havingValue = MODE_SAML)
@Slf4j
public class SamlAuthenticationConfig {

    @Bean
    public UserDetailsService samlUserDetailsService() {
        return username -> new User(
                username, StringUtils.EMPTY, AuthorityUtils.createAuthorityList(ROLE_USER, ROLE_ADMIN, "ROLE_CS_ROOT"));
    }

    @Bean
    OpenSaml4AuthenticationProvider samlAuthenticationProvider(UserDetailsService samlUserDetailsService) {
        var authenticationProvider = new OpenSaml4AuthenticationProvider();
        authenticationProvider.setResponseAuthenticationConverter(groupsConverter(samlUserDetailsService));
        return authenticationProvider;
    }

    @Bean
    public HumanToMachineSecurityDsl samlHumanToMachineSecurityDsl(
            IAtotiServerFilters filters, OpenSaml4AuthenticationProvider samlAuthenticationProvider) {
        return new AtotiServerHumanDsl(filters) {
            @Override
            protected void configureLoginAccess(HttpSecurity http) throws Exception {
                http.saml2Login(saml2 -> saml2.authenticationManager(new ProviderManager(samlAuthenticationProvider))
                                .successHandler(new SavedRequestAwareTargetUrlAuthenticationSuccessHandler()))
                        .saml2Logout(withDefaults())
                        .logout(c -> c.logoutSuccessUrl(LOGOUT_PAGE_URL));
            }
        };
    }

    private Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> groupsConverter(
            UserDetailsService samlUserDetailsService) {
        return responseToken -> {
            var authentication = OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter()
                    .convert(responseToken);
            var principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
            var name = principal.getName();
            var index = name.indexOf('@');
            if (index > -1) {
                name = name.substring(0, index);
                principal = new DefaultSaml2AuthenticatedPrincipal(
                        name, principal.getAttributes(), principal.getSessionIndexes());
            }
            Set<GrantedAuthority> authorities = new HashSet<>();
            try {
                var userDetails = samlUserDetailsService.loadUserByUsername(name);
                var userAuthorities = userDetails.getAuthorities();
                if (!CollectionUtils.isEmpty(userAuthorities)) {
                    authorities.addAll(userAuthorities);
                }
            } catch (UsernameNotFoundException e) {
                log.error("Cannot find any user details for {}", name);
                throw new UsernameNotFoundException("Cannot find any user details for " + name, e);
            }
            return new Saml2Authentication(principal, authentication.getSaml2Response(), authorities);
        };
    }
}
