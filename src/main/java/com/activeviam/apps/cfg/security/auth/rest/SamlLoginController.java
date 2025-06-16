/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.security.auth.rest;

import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_PROP;
import static com.activeviam.apps.cfg.security.auth.AuthenticationProperties.MODE_SAML;

import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

/**
 * Controller serving the Thymeleaf resources related to the login / logout process.
 *
 * @author ActiveViam
 */
@Controller
@ConditionalOnProperty(name = MODE_PROP, havingValue = MODE_SAML)
@RequiredArgsConstructor
public class SamlLoginController extends ALoginController {
    public static final String AUTHENTICATE_URI = "/saml2/authenticate";

    private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    @Override
    protected String getAuthenticateUri() {
        return AUTHENTICATE_URI;
    }

    @Override
    protected List<String> getRegistrationIds() {
        return StreamSupport.stream(
                        ((InMemoryRelyingPartyRegistrationRepository) relyingPartyRegistrationRepository).spliterator(),
                        false)
                .map(RelyingPartyRegistration::getRegistrationId)
                .toList();
    }
}
