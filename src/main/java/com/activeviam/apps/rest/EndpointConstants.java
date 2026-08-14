/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EndpointConstants {
    public static final String REST_NAMESPACE = "rest";
    public static final String CUSTOM_REST_PATH = "/custom/" + REST_NAMESPACE;

    /**
     * Client-supplied header correlating every HTTP call of a single masking/failover rehearsal run, so
     * its traces/logs/metrics across dq1/dq2/qn can all be filtered by one value afterward. Optional -
     * absent outside of a rehearsal.
     */
    public static final String TEST_RUN_ID_HEADER = "Test-Run-Id";

    public static final String TEST_RUN_ID_ATTRIBUTE = "test.run.id";
}
