/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps;

import static com.activeviam.activepivot.server.spring.private_.config.impl.ActivePivotRestServicesConfig.PING_SUFFIX;
import static com.activeviam.activepivot.server.spring.private_.config.impl.ActivePivotRestServicesConfig.REST_API_URL_PREFIX;
import static com.activeviam.web.core.api.IUrlBuilder.url;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(classes = AtotiSpringBootApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class AtotiSpringBootApplicationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    public void setupAuth() {
        restTemplate = restTemplate.withBasicAuth("admin", "admin");
    }

    @Test
    void contextLoads() {}

    @Test
    void activePivotPingReturnsPong() {
        var pingUrl = url("http://localhost:" + port, REST_API_URL_PREFIX, PING_SUFFIX);
        assertThat(restTemplate.getForObject(pingUrl, String.class)).contains("pong");
    }
}
