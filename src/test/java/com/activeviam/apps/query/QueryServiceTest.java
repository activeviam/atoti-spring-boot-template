/*
 * Copyright (C) ActiveViam 2023-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.TRADE_ATTRIBUTES_DIMENSION;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.query.QueryServiceTestConstants.COB_DATE_FILTER;
import static com.activeviam.apps.query.QueryServiceTestConstants.CPTY_1;
import static com.activeviam.apps.query.QueryServiceTestConstants.CPTY_2;
import static com.activeviam.apps.query.QueryServiceTestConstants.CPTY_FILTER;
import static com.activeviam.apps.query.QueryServiceTestConstants.NOTIONAL_MEAN_METRIC_DTO;
import static com.activeviam.apps.query.QueryServiceTestConstants.OTHER_TEST_DATE;
import static com.activeviam.apps.query.QueryServiceTestConstants.TESTS;
import static com.activeviam.apps.query.QueryServiceTestConstants.TEST_DATE;
import static com.activeviam.apps.query.QueryServiceTestConstants.TRADE_1;
import static com.activeviam.apps.query.QueryServiceTestConstants.TRADE_2;
import static com.activeviam.apps.query.QueryServiceTestConstants.TRADE_3;
import static com.activeviam.apps.query.QueryServiceTestConstants.TRADE_3_FILTER;
import static com.activeviam.apps.query.QueryServiceTestConstants.queryExporterArrowTests;
import static com.activeviam.apps.query.QueryServiceTestConstants.queryExporterCsvTests;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.activeviam.activepivot.core.intf.api.contextvalues.IContextValue;
import com.activeviam.activepivot.server.impl.private_.rest.dataexport.DataExportService;
import com.activeviam.apps.cfg.pivot.ApplicationWithDatastoreConfig;
import com.activeviam.apps.cfg.pivot.CubeTesterConfig;
import com.activeviam.apps.query.conditions.FilterExpressionConditionVisitor;
import com.activeviam.apps.query.rest.CubeQueryController;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.datastore.api.transaction.IOpenedTransaction;

import lombok.extern.slf4j.Slf4j;

@SpringJUnitConfig
@Slf4j
class QueryServiceTest {
    @TestConfiguration
    public static class MeasuresTestConfig extends CubeTesterConfig {

        static {
            System.setProperty("activeviam.feature.experimental.new_cube_restriction.enabled", "true");
        }

        @Bean
        CubeQueryService cubeQueryService(ApplicationWithDatastoreConfig application) {
            var cubeQueryProperties = new CubeQueryProperties();
            cubeQueryProperties.setDefaultCube(CUBE_NAME);
            var cubeDefaults = new CubeQueryProperties.CubeDefaults();
            cubeDefaults.setDefaultDimension(TRADE_ATTRIBUTES_DIMENSION);
            cubeDefaults.setDateFilterLevels(List.of(COB_DATE));
            cubeQueryProperties.setCubeConfiguration(Map.of(CUBE_NAME, cubeDefaults));
            var dataExportService = new DataExportService(application.activePivotManager(), Path.of(""));
            return new CubeQueryService(dataExportService, application.activePivotManager(), cubeQueryProperties);
        }

        @Bean
        CubeQueryController cubeQueryController(CubeQueryService cubeQueryService) {
            return new CubeQueryController(cubeQueryService);
        }

        @Override
        public void loadData(IOpenedTransaction t) {
            t.addAll(
                    TRADES_STORE_NAME,
                    List.of(
                            new Object[] {TEST_DATE, TRADE_1, 100d},
                            new Object[] {TEST_DATE, TRADE_2, 350d},
                            new Object[] {TEST_DATE, TRADE_3, 300d},
                            new Object[] {OTHER_TEST_DATE, TRADE_1, 1000d},
                            new Object[] {OTHER_TEST_DATE, TRADE_2, 3500d},
                            new Object[] {OTHER_TEST_DATE, TRADE_3, 3000d}));
            t.addAll(
                    TRADE_ATTRIBUTES_STORE_NAME,
                    List.of(
                            new Object[] {TEST_DATE, TRADE_1, TEST_DATE.minusDays(3), CPTY_1},
                            new Object[] {TEST_DATE, TRADE_2, TEST_DATE.minusDays(5), CPTY_2},
                            new Object[] {TEST_DATE, TRADE_3, TEST_DATE.minusDays(7), CPTY_2},
                            new Object[] {OTHER_TEST_DATE, TRADE_1, TEST_DATE.minusDays(3), CPTY_1},
                            new Object[] {OTHER_TEST_DATE, TRADE_2, TEST_DATE.minusDays(5), CPTY_2},
                            new Object[] {OTHER_TEST_DATE, TRADE_3, TEST_DATE.minusDays(7), CPTY_2}));
        }
    }

    @Autowired
    CubeTester cubeTester;

    @Autowired
    CubeQueryService cubeQueryService;

    // NOTE: this only generates the MDX query, but doesn't run it through the service!
    // To run this through the service we need an actual object of type IDataExportService
    // which is currently mocked
    @TestFactory
    Stream<DynamicTest> queryServiceMdxTestsWithContext() {
        return queryServiceMdxTests(true, cubeQueryService, cubeTester);
    }

    @TestFactory
    Stream<DynamicTest> queryServiceMdxTestsPureMdx() {
        return queryServiceMdxTests(false, cubeQueryService, cubeTester);
    }

    @TestFactory
    Stream<DynamicTest> queryServiceMdxTestsOptimize() {
        return queryServiceMdxTests(null, cubeQueryService, cubeTester);
    }

    private static Stream<DynamicTest> queryServiceMdxTests(
            Boolean useContext, CubeQueryService cubeQueryService, CubeTester cubeTester) {
        return TESTS.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest("QueryRunner: " + entry.getKey(), () -> {
                    // Apply useContext
                    var cubeQuerier = cubeQueryService.getCubeQuerier(CUBE_NAME);
                    var queryDto = entry.getValue().dto().toBuilder()
                            .withUseContext(useContext)
                            .build();
                    var query = cubeQuerier.convertCubeQuery(queryDto);
                    var mdxQuery = cubeQuerier.buildMdxQuery(query);
                    var contextValues = new ArrayList<IContextValue>();
                    if (query.isUseContext()) {
                        contextValues.add(cubeQuerier.buildCubeRestrictions(query));
                    }
                    contextValues.add(cubeQuerier.buildMdxContext(query));
                    var cellsTester = cubeTester
                            .mdxQuery()
                            .withMdx(mdxQuery)
                            .withContextValues(contextValues.toArray(new IContextValue[0]))
                            .run()
                            .show()
                            .getTester();
                    log.info("DTO:\n{}", queryDto);
                    log.info("MDX:\n{}", mdxQuery);
                    assertThat(cellsTester.isEmpty()).isFalse();
                    entry.getValue().resultConsumer().accept(cellsTester.findCell());
                }));
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

    // @TestFactory
    Stream<DynamicTest> queryExporterCsvTestsWithContext() {
        return queryExporterCsvTests(true, cubeQueryService, log);
    }

    // @TestFactory
    Stream<DynamicTest> queryExporterCsvTestsPureMdx() {
        return queryExporterCsvTests(false, cubeQueryService, log);
    }

    @Test
    void testContainsMeasureCondition() {
        var trueLogicalCondition = FilterExpressionConditionVisitor.parseFilterExpression(COB_DATE_FILTER + " AND ("
                + CPTY_FILTER + " OR " + TRADE_3_FILTER + ") AND " + NOTIONAL_MEAN_METRIC_DTO + " >= 1000");
        assertThat(CubeQuery.containsMeasureFilter(trueLogicalCondition)).isTrue();
        var falseLogicalCondition = FilterExpressionConditionVisitor.parseFilterExpression(
                COB_DATE_FILTER + " AND (" + CPTY_FILTER + " OR " + TRADE_3_FILTER + ")");
        assertThat(CubeQuery.containsMeasureFilter(falseLogicalCondition)).isFalse();
    }
}
