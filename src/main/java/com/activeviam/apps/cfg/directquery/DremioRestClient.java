/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.directquery;

import java.time.Duration;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.RequiredArgsConstructor;

/**
 * Runs administrative SQL statements against Dremio via its REST API (login + {@code /api/v3/sql} job
 * submission), as opposed to the Arrow Flight SQL connection ({@link DremioConnectorConfig}) that
 * DirectQuery uses for regular cube queries.
 *
 * <p>Used only by the masking exercise's data-maintenance endpoints (see {@code
 * com.activeviam.apps.rest.DataMaintenanceController}) to delete/restore an {@code AsOfDate} directly on
 * the Iceberg tables backing the {@code Trades}/{@code TradeAttributes} views - something DirectQuery
 * itself has no API for, since it treats the external database as read-only.
 */
@Component
@Profile("data-node")
@RequiredArgsConstructor
public class DremioRestClient {
    private static final Set<String> TERMINAL_JOB_STATES = Set.of("COMPLETED", "FAILED", "CANCELED");
    private static final Duration POLL_INTERVAL = Duration.ofMillis(300);
    private static final int MAX_POLL_ATTEMPTS = 100;

    private final DremioProperties dremioProperties;
    private final RestClient.Builder restClientBuilder;

    /** Runs a SQL statement to completion through Dremio's job API; throws if it fails or times out. */
    public void execute(final String sql) {
        final RestClient client = restClientBuilder
                .clone()
                .baseUrl("http://%s:%d".formatted(dremioProperties.getHost(), dremioProperties.getRestPort()))
                .build();
        final String token = login(client);
        final String jobId = submit(client, token, sql);
        final JobStatus status = awaitCompletion(client, token, jobId);
        if (!"COMPLETED".equals(status.jobState())) {
            throw new IllegalStateException("Dremio SQL job %s ended in state %s (%s) for statement: %s"
                    .formatted(jobId, status.jobState(), status.errorMessage(), sql));
        }
    }

    private String login(final RestClient client) {
        return client.post()
                .uri("/apiv2/login")
                .body(new LoginRequest(dremioProperties.getUsername(), dremioProperties.getPassword()))
                .retrieve()
                .body(LoginResponse.class)
                .token();
    }

    private String submit(final RestClient client, final String token, final String sql) {
        return client.post()
                .uri("/api/v3/sql")
                .header(HttpHeaders.AUTHORIZATION, "_dremio" + token)
                .body(new SqlRequest(sql))
                .retrieve()
                .body(SqlJobSubmitResponse.class)
                .id();
    }

    private JobStatus awaitCompletion(final RestClient client, final String token, final String jobId) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            final JobStatus status = client.get()
                    .uri("/api/v3/job/{jobId}", jobId)
                    .header(HttpHeaders.AUTHORIZATION, "_dremio" + token)
                    .retrieve()
                    .body(JobStatus.class);
            if (TERMINAL_JOB_STATES.contains(status.jobState())) {
                return status;
            }
            sleep();
        }
        throw new IllegalStateException("Dremio SQL job " + jobId + " did not complete within the polling window");
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private record LoginRequest(String userName, String password) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LoginResponse(String token) {}

    private record SqlRequest(String sql) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SqlJobSubmitResponse(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JobStatus(String jobState, String errorMessage) {}
}
