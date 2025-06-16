/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.rest;

import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_OAUTH;
import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_PROP;
import static org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;

import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

/**
 * Controller serving the Thymeleaf resources related to the login / logout process.
 *
 * @author ActiveViam
 */
@Controller
@ConditionalOnProperty(name = MODE_PROP, havingValue = MODE_OAUTH)
@RequiredArgsConstructor
public class OAuthLoginController extends ALoginController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Override
    protected String getAuthenticateUri() {
        return DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
    }

    @Override
    protected List<String> getRegistrationIds() {
        return StreamSupport.stream(
                        ((InMemoryClientRegistrationRepository) clientRegistrationRepository).spliterator(), false)
                .map(ClientRegistration::getRegistrationId)
                .toList();
    }
}
