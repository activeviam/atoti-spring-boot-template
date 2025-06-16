/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.cfg.source.DlcConfig.COB_DATE_UNLOAD_TOPIC;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.COUNTERPARTIES_SQL_QUERY;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.COUNTERPARTIES_SQL_TOPIC;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.TRADES_SQL_QUERY;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.TRADES_SQL_TOPIC;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.TRADE_ATTRIBUTES_SQL_QUERY;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.TRADE_ATTRIBUTES_SQL_TOPIC;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.apps.annotations.ConditionalOnApplicationWithDatastore;
import com.activeviam.apps.cfg.source.DlcConfig;
import com.activeviam.database.api.DatabasePrinter;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.io.dlc.api.operations.response.DlcStatus;
import com.activeviam.io.dlc.impl.DataLoadControllerService;
import com.activeviam.io.dlc.impl.description.topic.JdbcTopicDescription;
import com.activeviam.io.dlc.impl.operations.request.DlcLoadRequest;
import com.activeviam.io.dlc.impl.operations.request.DlcUnloadRequest;
import com.activeviam.io.dlc.impl.operations.request.scope.DlcScope;
import com.activeviam.io.dlc.impl.rest.resposne.DlcLoadResponseDTO;
import com.activeviam.io.dlc.impl.rest.resposne.DlcUnloadResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(CobDateLoadController.COB_DATE_ENDPOINT)
@RequiredArgsConstructor
@ConditionalOnApplicationWithDatastore
public class CobDateLoadController {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static final String COB_DATE_ENDPOINT = CUSTOM_REST_PATH + "/cob-date";

    private final DataLoadControllerService dataLoadControllerService;

    private final IDatastore datastore;

    public List<LocalDate> getCobDates() {
        var query = datastore
                .getQueryManager()
                .distinctQuery()
                .forTable(TRADES_STORE_NAME)
                .withoutCondition()
                .withTableFields(COB_DATE)
                .toQuery();
        return StreamSupport.stream(
                        datastore
                                .getMasterHead()
                                .getQueryRunner()
                                .distinctQuery(query)
                                .run()
                                .spliterator(),
                        false)
                .map(r -> (LocalDate) r.read(COB_DATE))
                .toList();
    }

    private static String injectCobDates(String query, Collection<LocalDate> cobDates) {
        return query.replace(
                "?",
                cobDates.stream().map(d -> "'" + d.format(DATE_FORMATTER) + "'").collect(Collectors.joining(",")));
    }

    private static JdbcTopicDescription overrideTradeTopic(Collection<LocalDate> cobDates) {
        return JdbcTopicDescription.builder(TRADES_SQL_TOPIC, injectCobDates(TRADES_SQL_QUERY, cobDates))
                .build();
    }

    private static JdbcTopicDescription overrideTradeAttributesTopic(Collection<LocalDate> cobDates) {
        return JdbcTopicDescription.builder(
                        TRADE_ATTRIBUTES_SQL_TOPIC, injectCobDates(TRADE_ATTRIBUTES_SQL_QUERY, cobDates))
                .build();
    }

    private static JdbcTopicDescription overrideCounterpartiesTopic() {
        return JdbcTopicDescription.builder(COUNTERPARTIES_SQL_TOPIC, COUNTERPARTIES_SQL_QUERY)
                .build();
    }

    //    @PostMapping({"/{cobDate}"})
    //    public DlcLoadResponseDTO loadCobDate(@PathVariable @DateTimeFormat(pattern = DATE_FORMAT) LocalDate cobDate)
    // {
    //        // Workaround: we need to override the parameterized topics because Dremio does not support
    //        // parameterized queries yet (it will from v. 26)
    //        var result = dataLoadControllerService
    //                .execute(DlcLoadRequest.builder()
    //                        // .topics(COUNTERPARTIES_SQL_TOPIC)
    //                        .topicOverrides(Set.of(
    //                                // overrideCounterpartiesTopic(),
    //                                overrideTradeTopic(List.of(cobDate)), overrideTradeAttributesTopic(cobDate)))
    //                        .build())
    //                .toDto();
    //        DatabasePrinter.printTableSizes(datastore.getMasterHead());
    //        return result;
    //    }

    @PostMapping
    public DlcLoadResponseDTO loadCobDates(@RequestBody Collection<LocalDate> cobDates) {
        // Workaround: we need to override the parameterized topics because Dremio does not support
        // parameterized queries yet (it will from v. 26)
        var existingDates = getCobDates();
        var datesToLoad =
                cobDates.stream().filter(date -> !existingDates.contains(date)).toList();

        // Load dates separately for now
        if (!datesToLoad.isEmpty()) {
            var result = dataLoadControllerService
                    .execute(DlcLoadRequest.builder()
                            // .topics(COUNTERPARTIES_SQL_TOPIC)
                            .topicOverrides(Set.of(
                                    // overrideCounterpartiesTopic(),
                                    overrideTradeTopic(datesToLoad), overrideTradeAttributesTopic(datesToLoad)))
                            .build())
                    .toDto();
            DatabasePrinter.printTableSizes(datastore.getMasterHead());
            return result;
        }
        return new DlcLoadResponseDTO(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                DlcStatus.OK);
    }

    @GetMapping
    public List<LocalDate> getLoadedDates() {
        return getCobDates();
    }

    @DeleteMapping({"/{cobDate}"})
    public DlcUnloadResponseDTO deleteCobDate(@PathVariable @DateTimeFormat(pattern = DATE_FORMAT) LocalDate cobDate) {
        var result = dataLoadControllerService
                .execute(DlcUnloadRequest.builder()
                        .topics(COB_DATE_UNLOAD_TOPIC)
                        .scope(DlcScope.of(DlcConfig.COB_DATE_SCOPE_PARAMETER, cobDate)) // FIXME: do we need to format?
                        .build())
                .toDto();
        DatabasePrinter.printTableSizes(datastore.getMasterHead());
        return result;
    }
}
