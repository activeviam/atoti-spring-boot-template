/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.apps.annotations.ConditionalOnApplicationWithDirectQuery;
import com.activeviam.apps.cfg.source.InitStandaloneStores;
import com.activeviam.directquery.application.api.Application;
import com.activeviam.directquery.application.api.refresh.ChangeDescription;
import com.activeviam.directquery.application.api.refresh.ChangeType;
import com.activeviam.directquery.application.api.refresh.TableUpdateDetail;
import com.activeviam.directquery.application.api.refresh.condition.ConditionFactory;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(CobDateIncrementalRefreshController.COB_DATE_ENDPOINT)
@RequiredArgsConstructor
@ConditionalOnApplicationWithDirectQuery
public class CobDateIncrementalRefreshController {

    public static final String COB_DATE_ENDPOINT = CUSTOM_REST_PATH + "/cob-date";

    private final Application application;

    private final InitStandaloneStores initStandaloneStores;

    @PostMapping
    public void loadCobDates(@RequestBody Collection<LocalDate> cobDates) {
        var tradesUpdate = TableUpdateDetail.create(
                TRADES_STORE_NAME, ChangeType.ADD_ROWS, ConditionFactory.in(COB_DATE, Set.of(cobDates)));
        var tradeAttributesUpdate = TableUpdateDetail.create(
                TRADE_ATTRIBUTES_STORE_NAME, ChangeType.ADD_ROWS, ConditionFactory.in(COB_DATE, Set.of(cobDates)));
        var counterpartyUpdate = TableUpdateDetail.create(
                COUNTERPARTIES_STORE_NAME, ChangeType.ADD_ROWS, ConditionFactory.in(COB_DATE, Set.of(cobDates)));
        application.refresh(ChangeDescription.create(List.of(tradeAttributesUpdate, tradesUpdate, counterpartyUpdate)));
        // Re-create the shift-cob-dates!!
        initStandaloneStores.refreshShiftCobDateStore();
    }
}
