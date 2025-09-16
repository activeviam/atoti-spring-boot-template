/*
 * Copyright (C) ActiveViam 2023-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConfig.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.FX_RATES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NETTING_SETS_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_ADD_ON_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.PFE_SUBCATEGORY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IMeasureHierarchy;
import com.activeviam.apps.cfg.source.PFEFactory;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.atoti.server.test.api.CubeTester;
import com.activeviam.atoti.server.test.api.ICellSetTester;
import com.activeviam.database.datastore.api.IDatastore;

@SpringJUnitConfig({CubeTestConfig.class})
class MeasuresTest {
    private static final LocalDate TEST_DATE = LocalDate.parse("2019-03-13");

    @Autowired
    CubeTester cubeTester;

    @Autowired
    IDatastore datastore;

    @BeforeEach
    public void initialLoad() {
        datastore.edit(t -> {
            t.addAll(
                    TRADES_STORE_NAME,
                    List.of(new Object[] {"TradeID1","FX Forward","NettingSetID1","Book 1","SELL","EUR","Forward","Foreign exchange",null,null,"EURUSD",LocalDate.parse("2020-03-25"),100.00,10.00,null,TEST_DATE},
                            new Object[] {"TradeID2","Commodity Cap Floor","NettingSetID1","Book 1","SELL","USD","Option","Commodity","Metals","Call","LME_AG",LocalDate.parse("2025-03-25"),200.00,-2.0,"Precious metals (except gold)",TEST_DATE},
                            new Object[] {"TradeID3","Commodity Cap Floor","NettingSetID2","Book 1","SELL","EUR","Option","Commodity","Metals","Call","LME_AG",LocalDate.parse("2025-03-25"),200.00,2.0,"Precious metals (except gold)",TEST_DATE})
            );
            t.addAll(
                    NETTING_SETS_STORE_NAME,
                    List.of(new Object[] {"NettingSetID1","NettingSetName","Margined","cpty1",50,150,"USD",TEST_DATE},
                            new Object[] {"NettingSetID2","NettingSetName","Margined","cpty1",50,150,"USD",TEST_DATE})
            );
            t.addAll(
                    FX_RATES_STORE_NAME,
                    List.of(new Object[] {"EUR","USD",10.0,TEST_DATE},
                            new Object[] {"USD","EUR",0.1,TEST_DATE},
                            new Object[] {"EUR","EUR",1.0,TEST_DATE},
                            new Object[] {"USD","USD",1.0,TEST_DATE})
            );
            t.addAll(
                    PFE_ADD_ON_STORE_NAME,
                    List.of(new Object[] {"Foreign exchange",null, PFEFactory.MATURITY_BUCKET_1TO5,0.1},
                            new Object[] {"Commodity",PFEFactory.PFE_SUBCATEGORY_PRECIOUS_METAL, PFEFactory.MATURITY_BUCKET_GT5,0.5})
            );
        });
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
    void tradeForexConversionToEURTest() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withWildcardCoordinate(StoreAndFieldConstants.TRADE_INPUT_CCY).forMeasures(
                        "Notional.SUM (original ccy)",
                        "Notional.SUM"))
                .run()
                .getTester();

        assertThat(resultCells.getCellsCount()).isEqualTo(4);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_INPUT_CCY,"EUR").measure("Notional.SUM (original ccy)").getCell().getValue()).isEqualTo(300.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_INPUT_CCY,"EUR").measure("Notional.SUM").getCell().getValue()).isEqualTo(300.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_INPUT_CCY,"USD").measure("Notional.SUM (original ccy)").getCell().getValue()).isEqualTo(200.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_INPUT_CCY,"USD").measure("Notional.SUM").getCell().getValue()).isEqualTo(20.0);
    }

    @Test
    void nettingForexConversionToEURTest() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withWildcardCoordinate(StoreAndFieldConstants.NETTING_INPUT_CCY).forMeasures(
                        "Collateral.SUM (original ccy)",
                        "Collateral.SUM"))
                .run()
                .getTester();

        assertThat(resultCells.getCellsCount()).isEqualTo(2);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.NETTING_INPUT_CCY,"USD").measure("Collateral.SUM (original ccy)").getCell().getValue()).isEqualTo(100.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.NETTING_INPUT_CCY,"USD").measure("Collateral.SUM").getCell().getValue()).isEqualTo(10.0);
    }

    @Test
    void netPositiveExposureTest() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withDefaultCoordinates().forMeasures(
                        CubeConstants.NET_POSITIVE_EXPOSURE))
                .run()
                .getTester()
                .hasOnlyOneCell();

        assertThat(resultCells.getValue()).isEqualTo(12.0);
    }

    @Test
    void netPositiveExposurePerTradeTest() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withWildcardCoordinate(StoreAndFieldConstants.TRADE_ID).forMeasures(
                        CubeConstants.NET_POSITIVE_EXPOSURE))
                .run()
                .getTester();

        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_ID,"TradeID1").measure(CubeConstants.NET_POSITIVE_EXPOSURE).getCell().getValue()).isEqualTo(10.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_ID,"TradeID2").measure(CubeConstants.NET_POSITIVE_EXPOSURE).getCell().getValue()).isEqualTo(0.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_ID,"TradeID3").measure(CubeConstants.NET_POSITIVE_EXPOSURE).getCell().getValue()).isEqualTo(2.0);
    }

    @Test
    void CCRMethod2Test() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withDefaultCoordinates().forMeasures(
                        CubeConstants.CCR_METHOD_2))
                .run()
                .getTester()
                .hasOnlyOneCell();

        assertThat(resultCells.getValue()).isEqualTo(5.0);
    }

    @Test
    void CCRMethod2PerNettingIDTest() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withWildcardCoordinate(StoreAndFieldConstants.NETTING_SET_ID).forMeasures(
                        CubeConstants.CCR_METHOD_2))
                .run()
                .getTester();

        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.NETTING_SET_ID,"NettingSetID1").measure(CubeConstants.CCR_METHOD_2).getCell().getValue()).isEqualTo(5.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.NETTING_SET_ID,"NettingSetID2").measure(CubeConstants.CCR_METHOD_2).getCell().getValue()).isEqualTo(0.0);
    }

    @Test
    void PFEAddonTest() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot
                        .withWildcardCoordinate(StoreAndFieldConstants.TRADE_ID)
                        .forMeasures(
                            CubeConstants.PFE_ADD_ON
                        ))
                .run()
                .getTester();

        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_ID,"TradeID1").measure(CubeConstants.PFE_ADD_ON).getCell().getValue()).isEqualTo(10.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_ID,"TradeID2").measure(CubeConstants.PFE_ADD_ON).getCell().getValue()).isEqualTo(10.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.TRADE_ID,"TradeID3").measure(CubeConstants.PFE_ADD_ON).getCell().getValue()).isEqualTo(100.0);
    }

    @Test
    void PFEAddonPerNettingIDTest() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withWildcardCoordinate(StoreAndFieldConstants.NETTING_SET_ID).forMeasures(
                        CubeConstants.PFE_ADD_ON))
                .run()
                .getTester();

        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.NETTING_SET_ID,"NettingSetID1").measure(CubeConstants.PFE_ADD_ON).getCell().getValue()).isEqualTo(20.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.NETTING_SET_ID,"NettingSetID2").measure(CubeConstants.PFE_ADD_ON).getCell().getValue()).isEqualTo(100.0);
    }

    @Test
    void CCRMethod3Test() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withDefaultCoordinates()
                        .forMeasures(
                                CubeConstants.CCR_METHOD_3
                        ))
                .run()
                .getTester()
                .hasOnlyOneCell();

        assertThat(resultCells.getValue()).isEqualTo(122.0);
    }

    @Test
    void CCRMethod3PerNettingIDTest() {
        var resultCells = cubeTester
                .query()
                .withQuery(pivot -> pivot.withWildcardCoordinate(StoreAndFieldConstants.NETTING_SET_ID).forMeasures(
                        CubeConstants.CCR_METHOD_3))
                .run()
                .getTester();

        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.NETTING_SET_ID,"NettingSetID1").measure(CubeConstants.CCR_METHOD_3).getCell().getValue()).isEqualTo(25.0);
        assertThat(resultCells.findCell().coordinate(StoreAndFieldConstants.NETTING_SET_ID,"NettingSetID2").measure(CubeConstants.CCR_METHOD_3).getCell().getValue()).isEqualTo(97.0);
    }


//    @Test
//    void countTestMDX() {
//        var resultCell = cubeTester
//                .mdxQuery()
//                .withMdx(String.format(
//                        "SELECT [%s].[%s] ON COLUMNS FROM [%s]", IHierarchy.MEASURES, NOTIONAL, CUBE_NAME))
//                .run()
//                .getTester()
//                .hasOnlyOneCell();
//
//        assertThat(resultCell.getValue()).isEqualTo(750.0);
//    }
}
