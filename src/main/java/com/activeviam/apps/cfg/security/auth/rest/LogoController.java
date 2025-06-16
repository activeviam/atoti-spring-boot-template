/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.rest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller serving logo resources related to the login / logout process.
 *
 * @author ActiveViam
 */
@RestController
public class LogoController {
    public static final String IMAGE_SVG_XML = "image/svg+xml";

    /**
     * Returns the login page logo, as configured by the user.
     */
    @GetMapping(value = "/logo-login", produces = IMAGE_SVG_XML)
    public Resource logo() {
        return new ClassPathResource("static/default/loading-background-dark-theme.svg");
    }

    /**
     * Returns the logout page logo, as configured by the user.
     */
    @GetMapping(value = "/logo-logout", produces = MediaType.IMAGE_PNG_VALUE)
    public Resource logoutLogo() {
        return new ClassPathResource("static/default/atoti-logout.png");
    }

    /**
     * Returns the sso logo
     */
    @GetMapping(value = "/logo-sso/{registrationId}", produces = MediaType.IMAGE_PNG_VALUE)
    public Resource ssoLogo(@PathVariable String registrationId) {
        return new ClassPathResource("static/logo/" + registrationId + ".png");
    }
}
