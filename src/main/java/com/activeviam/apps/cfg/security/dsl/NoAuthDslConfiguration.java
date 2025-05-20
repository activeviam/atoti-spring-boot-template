/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.dsl;

import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_ADMIN;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_USER;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import com.activeviam.web.spring.api.security.IAtotiServerFilters;
import com.activeviam.web.spring.api.security.dsl.AtotiServerHumanDsl;
import com.activeviam.web.spring.api.security.dsl.AtotiServerMachineDsl;
import com.activeviam.web.spring.api.security.dsl.HumanToMachineSecurityDsl;
import com.activeviam.web.spring.api.security.dsl.MachineToMachineSecurityDsl;

@Configuration
public class NoAuthDslConfiguration {

    @Bean
    public MachineToMachineSecurityDsl noAuthMachineToMachineSecurityDsl(IAtotiServerFilters filters) {
        return new AtotiServerMachineDsl(filters) {
            @Override
            protected void configureAuthentication(HttpSecurity http) throws Exception {
                // do nothing
                http.anonymous(httpSecurityAnonymousConfigurer -> httpSecurityAnonymousConfigurer.principal("anonymous").authorities(ROLE_USER,ROLE_ADMIN));
            }
        };
    }

    @Bean
    public HumanToMachineSecurityDsl noAuthHumanToMachineSecurityDsl(IAtotiServerFilters filters) {
        return new AtotiServerHumanDsl(filters) {
            @Override
            protected void configureLoginAccess(HttpSecurity http) throws Exception {
                // do nothing
                http.anonymous(httpSecurityAnonymousConfigurer -> httpSecurityAnonymousConfigurer.principal("anonymous").authorities(ROLE_USER,ROLE_ADMIN));
            }
        };
    }
}
