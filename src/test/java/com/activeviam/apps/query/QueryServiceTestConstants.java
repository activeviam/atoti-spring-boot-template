/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.activepivot.core.intf.api.cube.hierarchy.IHierarchy.ALLMEMBER;
import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.COB_DATE_LEVEL;
import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.COUNTERPARTY_LEVEL;
import static com.activeviam.apps.cfg.pivot.datanode.Dimensions.TRADE_ID_LEVEL;
import static com.activeviam.apps.cfg.pivot.datanode.Measures.MEAN;
import static com.activeviam.apps.cfg.pivot.datanode.Measures.SUM;
import static com.activeviam.apps.cfg.pivot.datanode.Measures.postfixMeasure;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTY_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;
import static com.activeviam.apps.query.CubeQueryService.CubeQuerier.levelToMdxPath;
import static com.activeviam.apps.query.CubeQueryService.OTHERS_MEMBER;
import static com.activeviam.apps.query.CubeQueryService.TOP_RANK_MEASURE;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.DateDayVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IMeasureHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.server.json.api.dataexport.JsonArrowOutputConfiguration;
import com.activeviam.activepivot.server.json.api.dataexport.JsonCsvPivotTableOutputConfiguration;
import com.activeviam.apps.query.rest.CubeQueryDTO;
import com.activeviam.atoti.server.test.api.ICellSetTester;

public class QueryServiceTestConstants {

    public static final LocalDate TEST_DATE = LocalDate.parse("2025-03-13");
    public static final LocalDate OTHER_TEST_DATE = LocalDate.parse("2025-03-12");

    public static final String CPTY_1 = "Cpty1";
    public static final String CPTY_2 = "Cpty2";
    public static final String TRADE_1 = "T1";
    public static final String TRADE_2 = "T2";
    public static final String TRADE_3 = "T3";

    public static final String COUNT_METRIC_DTO = IMeasureHierarchy.COUNT_ID;

    public static final String NOTIONAL_SUM_METRIC_DTO = postfixMeasure(NOTIONAL, SUM);

    public static final String NOTIONAL_MEAN_METRIC_DTO = postfixMeasure(NOTIONAL, MEAN);

    public static final String TOP_RANK_MEASURE_DTO = TOP_RANK_MEASURE;

    public static void assertCellValue(
            ICellSetTester.ICellByCoordinatesFinder resultCells, Map<LevelIdentifier, Object> levels, Object value) {
        var finder = resultCells;
        for (var level : levels.entrySet()) {
            finder = finder.coordinate(level.getKey(), level.getValue());
        }
        assertThat(finder.getCell().getValue()).isEqualTo(value);
    }

    public static final String CPTY_FILTER = COUNTERPARTY_ID + " IN " + "['" + CPTY_1 + "']";
    public static final String CPTY_LIKE = COUNTERPARTY_ID + " LIKE " + "'" + CPTY_1 + "'";
    public static final String TRADE_3_FILTER = TRADE_ID + " IN " + "['" + TRADE_3 + "']";
    public static final String TRADE_1_FILTER = TRADE_ID + " IN " + "['" + TRADE_1 + "']";

    public static final String COB_DATE_FILTER =
            COB_DATE + " IN [" + TEST_DATE.format(DateTimeFormatter.ISO_DATE) + "]";

    public static final Map<String, TestInputOutput> TESTS = new HashMap<>();

    static {
        TESTS.put(
                "Count",
                new TestInputOutput(
                        CubeQueryDTO.builder().withMetric(COUNT_METRIC_DTO).build(),
                        resultCells ->
                                assertThat(resultCells.getCell().getValue()).isEqualTo(3L),
                        List.of(new ResultColumn(COUNT_METRIC_DTO, new Long[] {3L}))));

        TESTS.put(
                "Notional.Sum; Levels: none, Filter: none",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withMetric(NOTIONAL_MEAN_METRIC_DTO)
                                .build(),
                        resultCells -> {
                            assertThat(resultCells
                                            .measure(NOTIONAL_SUM_METRIC_DTO)
                                            .getCell()
                                            .getValue())
                                    .isEqualTo(750d);
                            assertThat(resultCells
                                            .measure(NOTIONAL_MEAN_METRIC_DTO)
                                            .getCell()
                                            .getValue())
                                    .isEqualTo(250d);
                        },
                        null));
        TESTS.put(
                "Notional.Sum; Levels: cpty; Filter: none",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_1), 100.0);
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_2), 650.0);
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, ALLMEMBER), 750.0);
                        },
                        null));
        TESTS.put(
                "Notional.Sum; Levels: cpty; Filter: date",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .withFiltersExpression(COB_DATE_FILTER)
                                .build(),
                        resultCells -> {
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_1), 100.0);
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_2), 650.0);
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, ALLMEMBER), 750.0);
                        },
                        null));

        TESTS.put(
                "Notional.Sum; Levels: date; Filter: none",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withLevel(COB_DATE_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(resultCells, Map.of(COB_DATE_LEVEL, TEST_DATE), 750.0);
                            assertCellValue(resultCells, Map.of(COB_DATE_LEVEL, OTHER_TEST_DATE), 7500.0);
                        },
                        null));

        TESTS.put(
                "Notional.Sum; Levels: cpty; Filter: cpty",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withFiltersExpression(CPTY_FILTER)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_1), 100.0);
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, ALLMEMBER), 100.0);
                        },
                        null));

        TESTS.put(
                "Notional.Sum; Levels: trade; Filter: date,cpty,trade",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withFiltersExpression(
                                        COB_DATE_FILTER + " AND " + TRADE_1_FILTER + " AND " + CPTY_FILTER)
                                .withLevel(TRADE_ID_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(resultCells, Map.of(TRADE_ID_LEVEL, ALLMEMBER), 100.0);
                            assertCellValue(resultCells, Map.of(TRADE_ID_LEVEL, TRADE_1), 100.0);
                        },
                        List.of(
                                new ResultColumn(TRADE_ID_LEVEL, new String[] {null, TRADE_1}),
                                new ResultColumn(NOTIONAL_SUM_METRIC_DTO, new Double[] {100.0, 100.0}))));

        TESTS.put(
                "Notional.Sum; Levels: cpty; Filter: NOT cpty",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withFiltersExpression("NOT " + CPTY_FILTER)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_2), 650.0);
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, ALLMEMBER), 650.0);
                        },
                        null));
        TESTS.put(
                "Notional.Sum; Levels: cpty; Filter; date AND cpty",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withFiltersExpression(COB_DATE_FILTER + " AND " + CPTY_FILTER)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_1), 100.0);
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, ALLMEMBER), 100.0);
                        },
                        null));
        TESTS.put(
                "Notional.Sum; Levels: cpty; Filter; date AND cpty LIKE",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withFiltersExpression(COB_DATE_FILTER + " AND " + CPTY_LIKE)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_1), 100.0);
                            assertCellValue(resultCells, Map.of(COUNTERPARTY_LEVEL, ALLMEMBER), 100.0);
                        },
                        null));
        TESTS.put(
                "Notional.Sum; Levels: date,cpty; Filters: date AND cpty",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withFiltersExpression(COB_DATE_FILTER + " AND " + CPTY_FILTER)
                                .withLevel(COB_DATE_LEVEL.getLevelName())
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(
                                    resultCells, Map.of(COB_DATE_LEVEL, TEST_DATE, COUNTERPARTY_LEVEL, CPTY_1), 100.0);
                        },
                        null));
        TESTS.put(
                "Notional.Sum; Levels: date,cpty,trade; Filter: date AND (cpty OR trade)",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withFiltersExpression(
                                        COB_DATE_FILTER + " AND (" + CPTY_FILTER + " OR " + TRADE_3_FILTER + ")")
                                .withLevel(COB_DATE_LEVEL.getLevelName())
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .withLevel(TRADE_ID_LEVEL.getLevelName())
                                .build(),
                        resultCells -> {
                            assertCellValue(
                                    resultCells,
                                    Map.of(
                                            COB_DATE_LEVEL,
                                            TEST_DATE,
                                            COUNTERPARTY_LEVEL,
                                            ALLMEMBER,
                                            TRADE_ID_LEVEL,
                                            TRADE_1),
                                    100.0);
                            assertCellValue(
                                    resultCells,
                                    Map.of(
                                            COB_DATE_LEVEL,
                                            TEST_DATE,
                                            COUNTERPARTY_LEVEL,
                                            ALLMEMBER,
                                            TRADE_ID_LEVEL,
                                            TRADE_3),
                                    300.0);
                            assertCellValue(
                                    resultCells,
                                    Map.of(
                                            COB_DATE_LEVEL,
                                            TEST_DATE,
                                            COUNTERPARTY_LEVEL,
                                            CPTY_1,
                                            TRADE_ID_LEVEL,
                                            TRADE_1),
                                    100.0);
                            assertCellValue(
                                    resultCells,
                                    Map.of(
                                            COB_DATE_LEVEL,
                                            TEST_DATE,
                                            COUNTERPARTY_LEVEL,
                                            CPTY_2,
                                            TRADE_ID_LEVEL,
                                            TRADE_3),
                                    300.0);
                        },
                        List.of(
                                new ResultColumn(
                                        COB_DATE_LEVEL, new LocalDate[] {TEST_DATE, TEST_DATE, TEST_DATE, TEST_DATE}),
                                new ResultColumn(COUNTERPARTY_LEVEL, new String[] {null, null, CPTY_1, CPTY_2}),
                                new ResultColumn(TRADE_ID_LEVEL, new String[] {TRADE_1, TRADE_3, TRADE_1, TRADE_3}),
                                new ResultColumn(NOTIONAL_SUM_METRIC_DTO, new Double[] {100.0, 300.0, 100.0, 300.0}))));
        TESTS.put(
                "SORT Notional.Sum; Levels: cpty; Filters: no filter",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .withSortBy(CubeQueryDTO.SortDTO.builder()
                                        // .withMetric(postfixMeasure(NOTIONAL, SUM))
                                        .withColumn(COUNTERPARTY_LEVEL.getLevelName())
                                        .withAscending(false)
                                        .build())
                                .build(),
                        resultCells -> {
                            // we cannot check the order here with the resultCells
                        },
                        List.of(
                                new ResultColumn(COUNTERPARTY_LEVEL, new String[] {null, CPTY_2, CPTY_1}),
                                new ResultColumn(NOTIONAL_SUM_METRIC_DTO, new Double[] {750.0, 650.0, 100.0}))));
        TESTS.put(
                "PARTITION Notional.Sum; Levels: cpty; Filters: no filter",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .withPartitionedBy(CubeQueryDTO.PartitioningDTO.builder()
                                        .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                        .withMetric(postfixMeasure(NOTIONAL, SUM))
                                        .build())
                                .build(),
                        resultCells -> {
                            var calculatedMember = CubeQuery.calculatedMemberDefaultName(
                                    NOTIONAL_SUM_METRIC_DTO, COUNTERPARTY_LEVEL.getLevelName());
                            assertCellValue(
                                    resultCells.measure(NOTIONAL_SUM_METRIC_DTO),
                                    Map.of(COUNTERPARTY_LEVEL, ALLMEMBER),
                                    750.0);
                            assertCellValue(
                                    resultCells.measure(NOTIONAL_SUM_METRIC_DTO),
                                    Map.of(COUNTERPARTY_LEVEL, CPTY_1),
                                    100.0);
                            assertCellValue(
                                    resultCells.measure(NOTIONAL_SUM_METRIC_DTO),
                                    Map.of(COUNTERPARTY_LEVEL, CPTY_2),
                                    650.0);
                            assertCellValue(
                                    resultCells.measure(calculatedMember), Map.of(COUNTERPARTY_LEVEL, ALLMEMBER), null);
                            assertCellValue(
                                    resultCells.measure(calculatedMember), Map.of(COUNTERPARTY_LEVEL, CPTY_1), 750.0);
                            assertCellValue(
                                    resultCells.measure(calculatedMember), Map.of(COUNTERPARTY_LEVEL, CPTY_2), 750.0);
                        },
                        List.of(
                                new ResultColumn(COUNTERPARTY_LEVEL, new String[] {null, CPTY_1, CPTY_2}),
                                new ResultColumn(NOTIONAL_SUM_METRIC_DTO, new Double[] {750.0, 100.0, 650.0}),
                                // this is a calculated measure so return value is string
                                new ResultColumn(
                                        NOTIONAL_SUM_METRIC_DTO + "@" + COUNTERPARTY_LEVEL.getLevelName(),
                                        new String[] {null, "750.0", "750.0"}))));
        TESTS.put(
                "TOP RANK Notional.Sum; Levels: tradeId; Filters: no filter",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withMetric(TOP_RANK_MEASURE_DTO)
                                .withLevel(TRADE_ID_LEVEL.getLevelName())
                                .withTopRank(CubeQueryDTO.TopRankDTO.builder()
                                        .withLevel(TRADE_ID_LEVEL.getLevelName())
                                        .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                        .build())
                                .build(),
                        resultCells -> {
                            assertCellValue(
                                    resultCells.measure(NOTIONAL_SUM_METRIC_DTO),
                                    Map.of(TRADE_ID_LEVEL, TRADE_1),
                                    100.0);
                            assertCellValue(
                                    resultCells.measure(NOTIONAL_SUM_METRIC_DTO),
                                    Map.of(TRADE_ID_LEVEL, TRADE_2),
                                    350.0);
                            assertCellValue(
                                    resultCells.measure(NOTIONAL_SUM_METRIC_DTO),
                                    Map.of(TRADE_ID_LEVEL, TRADE_3),
                                    300.0);
                            assertCellValue(
                                    resultCells.measure(TOP_RANK_MEASURE_DTO), Map.of(TRADE_ID_LEVEL, TRADE_2), 1);
                            assertCellValue(
                                    resultCells.measure(TOP_RANK_MEASURE_DTO), Map.of(TRADE_ID_LEVEL, TRADE_3), 2);
                            assertCellValue(
                                    resultCells.measure(TOP_RANK_MEASURE_DTO), Map.of(TRADE_ID_LEVEL, TRADE_1), 3);
                        },
                        List.of(
                                new ResultColumn(TRADE_ID_LEVEL, new String[] {null, TRADE_2, TRADE_3, TRADE_1}),
                                new ResultColumn(NOTIONAL_SUM_METRIC_DTO, new Double[] {750.0, 350.0, 300.0, 100.0}),
                                new ResultColumn(TOP_RANK_MEASURE_DTO, new String[] {"0", "1", "2", "3"}))));
        TESTS.put(
                "TOP 2 Notional.Sum; Levels: cptyId,tradeId; Filters: no filter",
                new TestInputOutput(
                        CubeQueryDTO.builder()
                                .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                .withLevel(COUNTERPARTY_LEVEL.getLevelName())
                                .withLevel(TRADE_ID_LEVEL.getLevelName())
                                .withTopCount(CubeQueryDTO.TopCountDTO.builder()
                                        .withLevel(TRADE_ID_LEVEL.getLevelName())
                                        .withMetric(NOTIONAL_SUM_METRIC_DTO)
                                        .withCount(2)
                                        .withAggregateOthers(true)
                                        .build())
                                .build(),
                        resultCells -> {
                            assertCellValue(
                                    resultCells, Map.of(COUNTERPARTY_LEVEL, ALLMEMBER, TRADE_ID_LEVEL, TRADE_2), 350.0);
                            assertCellValue(
                                    resultCells, Map.of(COUNTERPARTY_LEVEL, ALLMEMBER, TRADE_ID_LEVEL, TRADE_3), 300.0);
                            assertCellValue(
                                    resultCells,
                                    Map.of(COUNTERPARTY_LEVEL, ALLMEMBER, TRADE_ID_LEVEL, OTHERS_MEMBER),
                                    100.0);
                            assertCellValue(
                                    resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_2, TRADE_ID_LEVEL, TRADE_2), 350.0);
                            assertCellValue(
                                    resultCells, Map.of(COUNTERPARTY_LEVEL, CPTY_2, TRADE_ID_LEVEL, TRADE_3), 300.0);
                            assertCellValue(
                                    resultCells,
                                    Map.of(COUNTERPARTY_LEVEL, CPTY_1, TRADE_ID_LEVEL, OTHERS_MEMBER),
                                    100.0);
                        },
                        List.of(
                                new ResultColumn(
                                        COUNTERPARTY_LEVEL, new String[] {null, null, null, CPTY_1, CPTY_2, CPTY_2}),
                                new ResultColumn(TRADE_ID_LEVEL, new String[] {
                                    TRADE_2, TRADE_3, OTHERS_MEMBER, OTHERS_MEMBER, TRADE_2, TRADE_3
                                }),
                                new ResultColumn(
                                        NOTIONAL_SUM_METRIC_DTO,
                                        new Double[] {350.0, 300.0, 100.0, 100.0, 350.0, 300.0}))));
    }

    public static VectorSchemaRoot buildExpectedResult(List<ResultColumn> expectedResults, RootAllocator allocator) {
        if (Objects.isNull(expectedResults)) {
            return null;
        }
        var vectors = expectedResults.stream()
                .map(e -> {
                    var field = e.field;
                    var data = e.data();
                    var level =
                            field instanceof LevelIdentifier ? levelToMdxPath((LevelIdentifier) field) : (String) field;
                    if (data instanceof Double[] doubles) {
                        return doubleFieldVector(allocator, doubleArrowField(level), doubles);
                    } else if (data instanceof Integer[] ints) {
                        return intFieldVector(allocator, intArrowField(level), ints);
                    } else if (data instanceof Long[] longs) {
                        return longFieldVector(allocator, longArrowField(level), longs);
                    } else if (data instanceof String[] strings) {
                        return stringFieldVector(allocator, stringArrowField(level), strings);
                    } else if (data instanceof LocalDate[] dates) {
                        return dateFieldVector(allocator, dateArrowField(level), dates);
                    }
                    throw new UnsupportedOperationException("Unsupported field type: " + field.toString());
                })
                .toList();
        return new VectorSchemaRoot(vectors);
    }

    public static Field stringArrowField(String name) {
        return new Field(name, FieldType.nullable(new ArrowType.Utf8()), null);
    }

    public static Field doubleArrowField(String measure) {
        return new Field(measure, FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null);
    }

    public static Field intArrowField(String measure) {
        return new Field(measure, FieldType.nullable(new ArrowType.Int(32, true)), null);
    }

    public static Field longArrowField(String measure) {
        return new Field(measure, FieldType.nullable(new ArrowType.Int(64, true)), null);
    }

    public static Field dateArrowField(String measure) {
        return new Field(measure, FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);
    }

    public static FieldVector doubleFieldVector(RootAllocator allocator, Field field, Double[] values) {
        var vector = new Float8Vector(field, allocator);
        vector.allocateNew(values.length);
        for (var i = 0; i < values.length; i++) {
            var value = values[i];
            if (Objects.nonNull(value)) {
                vector.set(i, values[i].doubleValue());
            }
        }
        vector.setValueCount(values.length);
        return vector;
    }

    public static FieldVector intFieldVector(RootAllocator allocator, Field field, Integer[] values) {
        var vector = new IntVector(field, allocator);
        vector.allocateNew(values.length);
        for (var i = 0; i < values.length; i++) {
            var value = values[i];
            if (Objects.nonNull(value)) {
                vector.set(i, values[i].intValue());
            }
        }
        vector.setValueCount(values.length);
        return vector;
    }

    public static FieldVector longFieldVector(RootAllocator allocator, Field field, Long[] values) {
        var vector = new BigIntVector(field, allocator);
        vector.allocateNew(values.length);
        for (var i = 0; i < values.length; i++) {
            var value = values[i];
            if (Objects.nonNull(value)) {
                vector.set(i, values[i].longValue());
            }
        }
        vector.setValueCount(values.length);
        return vector;
    }

    public static FieldVector stringFieldVector(RootAllocator allocator, Field field, String[] values) {
        var vector = new VarCharVector(field, allocator);
        vector.allocateNew(values.length);
        for (var i = 0; i < values.length; i++) {
            var value = values[i];
            if (Objects.nonNull(value)) {
                vector.set(i, values[i].getBytes());
            }
        }
        vector.setValueCount(values.length);
        return vector;
    }

    public static FieldVector dateFieldVector(RootAllocator allocator, Field field, LocalDate[] values) {
        var vector = new DateDayVector(field, allocator);
        vector.allocateNew(values.length);
        for (var i = 0; i < values.length; i++) {
            var value = values[i];
            if (Objects.nonNull(value)) {
                vector.set(i, (int) values[i].toEpochDay());
            }
        }
        vector.setValueCount(values.length);
        return vector;
    }

    public record TestInputOutput(
            CubeQueryDTO dto,
            Consumer<ICellSetTester.ICellByCoordinatesFinder> resultConsumer,
            List<ResultColumn> expectedResults) {}

    public record ResultColumn(Object field, Object[] data) {}

    public static Stream<DynamicTest> queryExporterArrowTests(
            Boolean useContext, CubeQueryService cubeQueryService, Logger logger) {
        return TESTS.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest("ArrowExporter: " + entry.getKey(), () -> {
                    var queryDto = entry.getValue().dto().toBuilder()
                            .withUseContext(useContext)
                            .build();
                    var querier = cubeQueryService.getCubeQuerier(CUBE_NAME);
                    var streamingResult = querier.runQuery(
                            querier.convertCubeQuery(queryDto), JsonArrowOutputConfiguration.PLUGIN_KEY);
                    try (var outputStream = new ByteArrayOutputStream()) {
                        outputStream.flush();
                        streamingResult.writeTo(outputStream);
                        try (var rootAllocator = new RootAllocator();
                                var reader = new ArrowStreamReader(
                                        new ByteArrayInputStream(outputStream.toByteArray()), rootAllocator);
                                var expectedSchemaRoot =
                                        buildExpectedResult(entry.getValue().expectedResults(), rootAllocator)) {
                            while (reader.loadNextBatch()) {
                                var vectorSchemaRoot = reader.getVectorSchemaRoot();
                                assertThat(vectorSchemaRoot).isNotNull();
                                logger.info(vectorSchemaRoot.contentToTSVString());
                                if (expectedSchemaRoot != null) {
                                    assertThat(vectorSchemaRoot.equals(expectedSchemaRoot))
                                            .isTrue();
                                }
                            }
                        }
                    }
                }));
    }

    public static Stream<DynamicTest> queryExporterCsvTests(
            Boolean useContext, CubeQueryService cubeQueryService, Logger logger) {
        return TESTS.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest("CSVExporter: " + entry.getKey(), () -> {
                    var queryDto = entry.getValue().dto().toBuilder()
                            .withUseContext(useContext)
                            .build();
                    var querier = cubeQueryService.getCubeQuerier(CUBE_NAME);
                    var streamingResult = querier.runQuery(
                            querier.convertCubeQuery(queryDto), JsonCsvPivotTableOutputConfiguration.PLUGIN_KEY);
                    try (var outputStream = new ByteArrayOutputStream()) {
                        outputStream.flush();
                        streamingResult.writeTo(outputStream);
                        try (var reader = new BufferedReader(new StringReader(outputStream.toString()))) {
                            logger.info("CSV:\n");
                            reader.lines().forEach(logger::info);
                        }
                    }
                }));
    }
}
