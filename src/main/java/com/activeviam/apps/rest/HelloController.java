/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(HelloController.HELLO_ENDPOINT)
@RequiredArgsConstructor
public class HelloController {
    public static final String HELLO_ENDPOINT = CUSTOM_REST_PATH + "/hello";

    @GetMapping
    public String index() {
        return "Hello from Atoti Spring Boot, time is " + LocalDateTime.now();
    }
}
