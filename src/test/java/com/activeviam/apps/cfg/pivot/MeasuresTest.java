/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeTestConfig.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IMeasureHierarchy;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.datastore.api.IDatastore;

@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({CubeTestConfig.class})
class MeasuresTest {
    @Autowired
    CubeTester cubeTester;

    @Autowired
    IDatastore datastore;

    @BeforeEach
    public void initialLoad() {
        var tuples = new ArrayList<Object[]>();
        tuples.add(new Object[] {LocalDate.parse("2019-03-13"), "T1", 100});
        tuples.add(new Object[] {LocalDate.parse("2019-03-13"), "T2", 350d});
        tuples.add(new Object[] {LocalDate.parse("2019-03-13"), "T3", 300d});
        insertTradesTuples(tuples);
    }

    private void insertTradesTuples(List<Object[]> tuples) {
        datastore.edit(t -> {
            t.addAll(TRADES_STORE_NAME, tuples);
            t.forceCommit();
        });
    }

    private void insertTradesTuples(Object[] tuple) {
        var tuples = new ArrayList<Object[]>();
        tuples.add(tuple);
        insertTradesTuples(tuples);
    }

    /**
     * Here is the actual test. Check that the numbers sum up correctly
     */
    @Test
    void countTest() {
        var resultCell = cubeTester
                .query()
                .withQuery(pivot -> pivot.withDefaultCoordinates().forMeasures(IMeasureHierarchy.COUNT_ID))
                .run()
                .getTester()
                .hasOnlyOneCell();

        assertThat(resultCell.getValue()).isEqualTo(3L);
    }

    @Test
    void countTestMDX() {
        var resultCell = cubeTester
                .mdxQuery()
                .withMdx("SELECT [Measures].[contributors.COUNT] ON COLUMNS FROM [Cube]")
                .run()
                .getTester()
                .hasOnlyOneCell();

        assertThat(resultCell.getValue()).isEqualTo(3L);
    }

    /**
     * Here is the actual test. Check that the numbers sum up correctly
     */
    @Test
    void tradesNotionalTotal_test() {
        insertTradesTuples(new Object[] {LocalDate.parse("2019-03-13"), "T4", 400d});

        var result = cubeTester
                .query()
                .onPivot(CUBE_NAME)
                .withQuery(cube -> cube.withDefaultCoordinates().forMeasures(TRADES_NOTIONAL))
                .run()
                .getTester()
                .hasOnlyOneCell()
                .getValue();

        assertThat(result).isEqualTo(1150d);
    }
}
