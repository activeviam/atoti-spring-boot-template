/*
 * Copyright (C) ActiveViam 2023-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.apps.query.QueryServiceTestConstants.queryExporterArrowTests;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.activeviam.apps.AtotiSpringBootApplication;
import com.activeviam.apps.rest.CobDateLoadController;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.io.dlc.api.operations.response.DlcStatus;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest(classes = AtotiSpringBootApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Slf4j
@ActiveProfiles("it")
class QueryServiceIntegrationTest {

    static {
        System.setProperty("activeviam.feature.experimental.new_cube_restriction.enabled", "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CubeQueryService cubeQueryService;

    @Autowired
    private CobDateLoadController cobDateLoadController;

    @Autowired
    IDatastore datastore;

    private static final AtomicReference<LoadStatus> LOAD_STATUS = new AtomicReference<>(LoadStatus.NOT_LOADED);

    private enum LoadStatus {
        NOT_LOADED,
        LOADED
    }

    @BeforeEach
    void setupAuth() {
        restTemplate = restTemplate.withBasicAuth("admin", "admin");
    }

    @BeforeEach
    void loadDataAndWait() {
        while (LOAD_STATUS.get() != LoadStatus.LOADED) {
            if (LOAD_STATUS.get() == LoadStatus.NOT_LOADED) {
                // Load data, this is blocking
                try {
                    var result = cobDateLoadController.loadCobDates(Set.of(LocalDate.now()));
                    assertThat(result.status()).isEqualTo(DlcStatus.OK);
                    LOAD_STATUS.set(LoadStatus.LOADED);
                } catch (Exception e) {
                    Assertions.fail("Failed to load cobDates", e);
                }
            }
        }
    }

    @TestFactory
    Stream<DynamicTest> queryExporterArrowTestsWithContext() {
        return queryExporterArrowTests(true, cubeQueryService, log);
    }

    @TestFactory
    Stream<DynamicTest> queryExporterArrowTestsPureMdx() {
        return queryExporterArrowTests(false, cubeQueryService, log);
    }

    @TestFactory
    Stream<DynamicTest> queryExporterArrowTestsOptimize() {
        return queryExporterArrowTests(null, cubeQueryService, log);
    }
}
