/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.datastore;

import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.scheduling.annotation.Scheduled;

import com.activeviam.database.api.conditions.BaseConditions;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.database.datastore.api.IDatastore;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DatastoreRolloverCleanup {
    private final IDatastore datastore;

    @Scheduled(fixedDelayString = "PT10M")
    void cleanup() {
        var query = datastore
                .getMasterHead()
                .getQueryManager()
                .distinctQuery()
                .forTable(TRADES_STORE_NAME)
                .withoutCondition()
                .withTableFields(COB_DATE)
                .toQuery();
        try (var result =
                datastore.getMasterHead().getQueryRunner().distinctQuery(query).run()) {
            var baseStoreDates = StreamSupport.stream(result.spliterator(), false)
                    .map(r -> (LocalDate) r.read(COB_DATE))
                    .collect(Collectors.toSet());
            datastore.edit(t -> {
                t.removeWhere(
                        TRADE_ATTRIBUTES_STORE_NAME,
                        BaseConditions.not(BaseConditions.in(FieldPath.of(COB_DATE), baseStoreDates)));
                t.forceCommit();
            });
        }
    }
}
