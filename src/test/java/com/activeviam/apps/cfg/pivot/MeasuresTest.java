/*
 * Copyright (C) ActiveViam 2023-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConfig.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IMeasureHierarchy;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.database.datastore.api.transaction.IOpenedTransaction;

@SpringJUnitConfig
class MeasuresTest {
    private static final LocalDate TEST_DATE = LocalDate.parse("2019-03-13");

    @TestConfiguration
    public static class MeasuresTestConfig extends CubeTesterConfig {
        @Override
        public void loadData(IOpenedTransaction t) {
            t.addAll(
                    TRADES_STORE_NAME,
                    List.of(new Object[] {TEST_DATE, "T1", 100}, new Object[] {TEST_DATE, "T2", 350d}, new Object[] {
                        TEST_DATE, "T3", 300d
                    }));
        }
    }

    @Autowired
    CubeTester cubeTester;

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

    /**
     * Here is a measure test using MDX
     */
    @Test
    void countTestMDX() {
        var resultCell = cubeTester
                .mdxQuery()
                .withMdx(String.format(
                        "SELECT [%s].[%s] ON COLUMNS FROM [%s]", IHierarchy.MEASURES, NOTIONAL, CUBE_NAME))
                .run()
                .getTester()
                .hasOnlyOneCell();

        assertThat(resultCell.getValue()).isEqualTo(750.0);
    }
}
