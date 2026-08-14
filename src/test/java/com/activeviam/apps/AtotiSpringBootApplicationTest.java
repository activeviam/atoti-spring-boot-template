/*
 * Copyright (C) ActiveViam 2023-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps;

import static com.activeviam.activepivot.server.intf.api.rest.ActivePivotRestServices.REST_API_URL_PREFIX;
import static com.activeviam.web.core.api.IUrlBuilder.url;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full application as a data node, which requires DirectQuery access to a live Dremio instance
 * plus the shared cluster-db Postgres for JGroups discovery; skipped unless DREMIO_USERNAME is set in the
 * environment (see the DirectQuery data source in application.yml).
 */
@EnabledIfEnvironmentVariable(named = "DREMIO_USERNAME", matches = ".+")
@ActiveProfiles("data-node")
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
    void activePivotPingReturnsPong() {
        var pingUrl = url("http://localhost:" + port, REST_API_URL_PREFIX, "ping");
        assertThat(restTemplate.getForObject(pingUrl, String.class)).contains("pong");
    }
}
