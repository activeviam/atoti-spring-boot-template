/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.authentication")
public record AuthenticationProperties(Mode mode, InMemoryAuthenticationProperties inMemory) {
    public static final String PREFIX = "security.authentication";
    public static final String MODE_PROP = PREFIX + ".mode";
    public static final String MODE_SAML = "saml";
    public static final String MODE_OAUTH = "oauth";
    public static final String MODE_IN_MEMORY = "memory";

    public enum Mode {
        SAML,
        OAUTH,
        MEMORY
    }

    public record InMemoryAuthenticationProperties(Set<User> users) {

        public record User(String username, String password, Set<String> authorities) {}
    }
}
