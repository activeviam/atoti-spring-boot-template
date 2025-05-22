/*
 * Copyright (C) ActiveViam 2023-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.TRADE_ATTRIBUTES_DIMENSION;
import static com.activeviam.apps.cfg.pivot.datanode.Measures.MEAN;
import static com.activeviam.apps.cfg.pivot.datanode.Measures.SUM;
import static com.activeviam.apps.cfg.pivot.datanode.Measures.postfixMeasure;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IMeasureHierarchy;
import com.activeviam.activepivot.server.intf.api.dataexport.IDataExportService;
import com.activeviam.apps.cfg.pivot.ApplicationWithDatastoreConfig;
import com.activeviam.apps.cfg.pivot.CubeTesterConfig;
import com.activeviam.apps.query.rest.CubeQueryDTO;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.atoti.server.test.api.ICellSetTester;
import com.activeviam.database.datastore.api.transaction.IOpenedTransaction;

import lombok.extern.slf4j.Slf4j;

@SpringJUnitConfig
@Slf4j
class QueryServiceTest {
    private static final LocalDate TEST_DATE = LocalDate.parse("2019-03-13");

    @TestConfiguration
    public static class MeasuresTestConfig extends CubeTesterConfig {

        @MockitoBean
        IDataExportService dataExportService;

        @Bean
        CubeQueryService cubeQueryService(ApplicationWithDatastoreConfig application) {
            var cubeQueryProperties = new CubeQueryProperties();
            cubeQueryProperties.setDefaultCube(CUBE_NAME);
            var cubeDefaults = new CubeQueryProperties.CubeDefaults();
            cubeDefaults.setDefaultDimension(TRADE_ATTRIBUTES_DIMENSION);
            cubeDefaults.setDateFilterLevels(List.of(COB_DATE));
            cubeQueryProperties.setCubeConfiguration(Map.of(CUBE_NAME, cubeDefaults));
            return new CubeQueryService(dataExportService, application.activePivotManager(), cubeQueryProperties);
        }

        @Override
        public void loadData(IOpenedTransaction t) {
            t.addAll(
                    TRADES_STORE_NAME,
                    List.of(new Object[] {TEST_DATE, "T1", 100}, new Object[] {TEST_DATE, "T2", 350d}, new Object[] {
                        TEST_DATE, "T3", 300d
                    }));
            t.addAll(
                    TRADE_ATTRIBUTES_STORE_NAME,
                    List.of(
                            new Object[] {TEST_DATE, "T1", TEST_DATE.minusDays(3), "Cpty1"},
                            new Object[] {TEST_DATE, "T2", TEST_DATE.minusDays(5), "Cpty2"},
                            new Object[] {TEST_DATE, "T3", TEST_DATE.minusDays(7), "Cpty2"}));
        }
    }

    @Autowired
    CubeTester cubeTester;

    @Autowired
    CubeQueryService cubeQueryService;

    private static final Map<String, TestInputOutput> TESTS = Map.of(
            "Count",
            new TestInputOutput(
                    CubeQueryDTO.builder()
                            .withMetric(CubeQueryDTO.MetricDTO.builder()
                                    .withMetric(IMeasureHierarchy.COUNT_ID)
                                    .build())
                            .build(),
                    resultCells -> assertThat(resultCells.getCell().getValue()).isEqualTo(3L)),
            "Notionals",
            new TestInputOutput(
                    CubeQueryDTO.builder()
                            .withMetric(CubeQueryDTO.MetricDTO.builder()
                                    .withMetric(postfixMeasure(NOTIONAL, SUM))
                                    .build())
                            .withMetric(CubeQueryDTO.MetricDTO.builder()
                                    .withMetric(postfixMeasure(NOTIONAL, MEAN))
                                    .build())
                            .build(),
                    resultCells -> {
                        assertThat(resultCells
                                        .measure(postfixMeasure(NOTIONAL, SUM))
                                        .getCell()
                                        .getValue())
                                .isEqualTo(750d);
                        assertThat(resultCells
                                        .measure(postfixMeasure(NOTIONAL, MEAN))
                                        .getCell()
                                        .getValue())
                                .isEqualTo(250d);
                    }));

    // NOTE: this only generates the MDX query, but doesn't run it through the service!
    // To run this through the service we need an actual object of type IDataExportService
    // which is currently mocked
    @TestFactory
    Stream<DynamicTest> queryServiceMdxTests() {
        return TESTS.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest(entry.getKey() + " Measures", () -> {
                    var mdx = cubeQueryService.buildMdxQuery(
                            CUBE_NAME, entry.getValue().dto());
                    log.info("MDX:\n{}", mdx);
                    var resultCell = cubeTester
                            .mdxQuery()
                            .withMdx(mdx)
                            .run()
                            .show()
                            .getTester()
                            .findCell();
                    entry.getValue().resultConsumer().accept(resultCell);
                }));
    }

    private record TestInputOutput(
            CubeQueryDTO dto, Consumer<ICellSetTester.ICellByCoordinatesFinder> resultConsumer) {}
}
