/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.oauth;

import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_OAUTH;
import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_PROP;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_ADMIN;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_USER;
import static com.activeviam.springboot.atoti.server.starter.api.LoginLogoutUrls.LOGOUT_PAGE_URL;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.util.CollectionUtils;

import com.activeviam.apps.cfg.security.auth.SavedRequestAwareTargetUrlAuthenticationSuccessHandler;
import com.activeviam.web.spring.api.security.IAtotiServerFilters;
import com.activeviam.web.spring.api.security.dsl.AtotiServerHumanDsl;
import com.activeviam.web.spring.api.security.dsl.HumanToMachineSecurityDsl;

import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnProperty(name = MODE_PROP, havingValue = MODE_OAUTH)
@Slf4j
public class OAuthAuthenticationConfig {

    @Bean
    public UserDetailsService oauthUserDetailsService() {
        var userDetailsManager = new InMemoryUserDetailsManager();
        userDetailsManager.createUser(User.withUsername("obi@activeviam.com")
                .password(StringUtils.EMPTY)
                .authorities(ROLE_USER, ROLE_ADMIN, "ROLE_CS_ROOT")
                .build());
        return userDetailsManager;
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(UserDetailsService oauthUserDetailsService) {
        var delegate = new OidcUserService();

        return (userRequest) -> {
            // Delegate to the default implementation for loading a user
            var oidcUser = delegate.loadUser(userRequest);

            var providerDetails = userRequest.getClientRegistration().getProviderDetails();
            //            var userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
            var userNameAttributeName = "preferred_username";
            var name = (String) oidcUser.getAttributes().get(userNameAttributeName);
            Set<GrantedAuthority> authorities = new HashSet<>();
            try {
                var userDetails = oauthUserDetailsService.loadUserByUsername(name);
                var userAuthorities = userDetails.getAuthorities();
                if (!CollectionUtils.isEmpty(userAuthorities)) {
                    authorities.addAll(userAuthorities);
                }
            } catch (UsernameNotFoundException e) {
                log.error("Cannot find any user details for {}", name);
                throw new UsernameNotFoundException("Cannot find any user details for " + name, e);
            }

            oidcUser = new DefaultOidcUser(
                    authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), userNameAttributeName);
            return oidcUser;
        };
    }

    @Bean
    public HumanToMachineSecurityDsl oauthHumanToMachineSecurityDsl(
            IAtotiServerFilters filters, UserDetailsService oauthUserDetailsService) {
        return new AtotiServerHumanDsl(filters) {
            @Override
            protected void configureLoginAccess(HttpSecurity http) throws Exception {
                http.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(
                                        userInfo -> userInfo.oidcUserService(oidcUserService(oauthUserDetailsService)))
                                .successHandler(new SavedRequestAwareTargetUrlAuthenticationSuccessHandler()))
                        .logout(c -> c.logoutSuccessUrl(LOGOUT_PAGE_URL));
            }
        };
    }

    //    @Bean
    //    public MachineToMachineSecurityDsl oauthMachineToMachineSecurityDsl(
    //            IAtotiServerFilters filters, UserDetailsService oauthUserDetailsService) {
    //        return new AtotiServerMachineDsl(filters) {
    //            @Override
    //            protected void configureAuthentication(HttpSecurity http) throws Exception {
    //                http.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(
    //                        userInfo -> userInfo.oidcUserService(oidcUserService(oauthUserDetailsService))));
    //            }
    //        };
    //    }
}
