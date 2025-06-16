/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.rest;

import static com.activeviam.springboot.atoti.server.starter.api.LoginLogoutUrls.LOGIN_ENTRYPOINT_URL;
import static com.activeviam.springboot.atoti.server.starter.api.LoginLogoutUrls.LOGOUT_PAGE_URL;

import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller serving the Thymeleaf resources related to the login / logout process.
 *
 * @author ActiveViam
 */
public abstract class ALoginController {
    public static final String ERROR_PARAM = "error";
    public static final String AUTH_URI_PARAM = "authUri";

    /**
     * Returns the login page, as configured by the user.
     */
    @GetMapping(LOGIN_ENTRYPOINT_URL)
    public String login(Model model, @RequestParam(name = ERROR_PARAM, required = false) String error) {
        model.addAttribute(ERROR_PARAM, error);
        model.addAttribute(AUTH_URI_PARAM, getAuthenticateUri());
        model.addAttribute("registrationIds", getRegistrationIds());
        return "login.html";
    }

    /**
     * Returns the logout page, as configured by the user.
     */
    @GetMapping(LOGOUT_PAGE_URL)
    public String logout() {
        return "logout.html";
    }

    protected abstract String getAuthenticateUri();

    protected abstract List<String> getRegistrationIds();
}
