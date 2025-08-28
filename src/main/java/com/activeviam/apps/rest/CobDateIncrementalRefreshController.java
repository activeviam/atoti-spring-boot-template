/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTERPARTIES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.activepivot.core.impl.api.description.impl.PartialProviderDefinition;
import com.activeviam.activepivot.core.impl.api.description.impl.PartialProviderFilters;
import com.activeviam.activepivot.core.impl.api.experimental.alteration.DynamicAggregateProvider;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IAggregateProviderDefinition;
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

    private static final LocalDate EOM = LocalDate.now();

    @PostMapping
    public String refreshCobDates(@RequestBody Collection<LocalDate> cobDates) {
        var tradesUpdate = TableUpdateDetail.create(
                TRADES_STORE_NAME, ChangeType.ADD_ROWS, ConditionFactory.in(COB_DATE, Set.of(cobDates)));
        var tradeAttributesUpdate = TableUpdateDetail.create(
                TRADE_ATTRIBUTES_STORE_NAME, ChangeType.ADD_ROWS, ConditionFactory.in(COB_DATE, Set.of(cobDates)));
        var counterpartyUpdate = TableUpdateDetail.create(
                COUNTERPARTIES_STORE_NAME, ChangeType.ADD_ROWS, ConditionFactory.in(COB_DATE, Set.of(cobDates)));
        application.refresh(ChangeDescription.create(List.of(tradeAttributesUpdate, tradesUpdate, counterpartyUpdate)));
        // Re-create the shift-cob-dates!!
        initStandaloneStores.refreshShiftCobDateStore();
        // refreshAggregatesProviders(cobDates);

        return cobDates.stream()
                .map(d -> d.format(DateTimeFormatter.BASIC_ISO_DATE))
                .collect(Collectors.joining(",", "Refresh performed on CobDates: [", "]"));
    }

    private void refreshAggregatesProviders(Collection<LocalDate> cobDates) {
        // compute the EOM date, and check if we need to add it or not
        // Best option is to have 1 provider per EOM. If we have one provider with all the EOM in the filter, then
        // we would need to rebuild it everytime because we cannot update the filter.
        if (cobDates.contains(EOM)) {
            var endOfMonthProvider = new PartialProviderDefinition(
                    "EOMProvider" + EOM.format(DateTimeFormatter.BASIC_ISO_DATE),
                    IAggregateProviderDefinition.LEAF_PLUGIN_TYPE,
                    List.of(LevelIdentifier.simple("id")), // FIXME
                    List.of("value.SUM"), // FIXME
                    PartialProviderFilters.of(Map.of()), // FIXME: add the latest EOM to this filter
                    new Properties(),
                    null);
            DynamicAggregateProvider.updateAggregateProviders(application.getManager(), t -> {
                t.removePartialAggregateProvider(CUBE_NAME, "oldestEOMProvider?");
                t.addNewPartialAggregateProvider(CUBE_NAME, endOfMonthProvider);
            });
        }
    }
}
