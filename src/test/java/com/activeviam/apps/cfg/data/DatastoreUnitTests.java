/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.data;

import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.activeviam.apps.cfg.source.DlcConfig;
import com.activeviam.apps.cfg.source.DremioJdbcSourceConfig;
import com.activeviam.apps.rest.CobDateLoadController;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.database.datastore.api.transaction.IOpenedTransaction;
import com.activeviam.io.dlc.internal.config.DataLoadControllerConfig;

import lombok.extern.slf4j.Slf4j;

@SpringJUnitConfig
@Slf4j
class DatastoreUnitTests {

    @TestConfiguration
    @Import(
            value = {
                DataLoadControllerConfig.class,
                DlcConfig.class,
                CobDateLoadController.class,
                DremioJdbcSourceConfig.class
            })
    @ActiveProfiles("inmemory")
    public static class DatastoreUnitTestsConfig extends DatastoreTesterConfig {
        @Override
        public void loadData(IOpenedTransaction t) {
            //            t.addAll(
            //                    TRADES_STORE_NAME,
            //                    List.of(
            //                            new Object[] {TEST_DATE, TRADE_1, 100d},
            //                            new Object[] {TEST_DATE, TRADE_2, 350d},
            //                            new Object[] {TEST_DATE, TRADE_3, 300d},
            //                            new Object[] {OTHER_TEST_DATE, TRADE_1, 1000d},
            //                            new Object[] {OTHER_TEST_DATE, TRADE_2, 3500d},
            //                            new Object[] {OTHER_TEST_DATE, TRADE_3, 3000d}));
        }
    }

    @Autowired
    IDatastore datastore;

    @Autowired
    CobDateLoadController cobDateLoadController;

    //    @BeforeEach
    //    void loadData() {
    //        // If the date is already loaded, this doesnt do anything
    //        var result = cobDateLoadController.loadCobDates(Set.of(LocalDate.now()));
    //        assertThat(result.status()).isEqualTo(DlcStatus.OK);
    //    }

    @Test
    void measurementsTest() {
        var result = cobDateLoadController.loadCobDates(Set.of(LocalDate.now()));
        var query = datastore
                .getQueryManager()
                .distinctQuery()
                .forTable(TRADES_STORE_NAME)
                .withoutCondition()
                .withTableFields(COB_DATE)
                .toQuery();
        datastore.getMasterHead().getQueryRunner().distinctQuery(query);
    }
}
