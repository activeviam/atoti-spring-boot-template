/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.COB_DATE_SCOPE_PARAMETER;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.DREMIO_TOPICS;
import static com.activeviam.apps.cfg.source.DremioJdbcSourceConfig.DREMIO_UNLOAD_TOPIC;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.database.api.DatabasePrinter;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.io.dlc.api.description.source.DlcSourceType;
import com.activeviam.io.dlc.impl.DataLoadControllerService;
import com.activeviam.io.dlc.impl.operations.request.DlcLoadRequest;
import com.activeviam.io.dlc.impl.operations.request.DlcUnloadRequest;
import com.activeviam.io.dlc.impl.operations.request.scope.DlcScope;
import com.activeviam.io.dlc.impl.rest.resposne.DlcLoadResponseDTO;
import com.activeviam.io.dlc.impl.rest.resposne.DlcUnloadResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(CobDateDataController.COB_DATE_ENDPOINT)
@RequiredArgsConstructor
public class CobDateDataController {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static final String COB_DATE_ENDPOINT = CUSTOM_REST_PATH + "/cob-date";

    private final DataLoadControllerService dataLoadControllerService;

    private final IDatastore datastore;

    @PostMapping({"/{cobDate}"})
    public DlcLoadResponseDTO loadCobDate(@PathVariable @DateTimeFormat(pattern = DATE_FORMAT) LocalDate cobDate) {
        var result = dataLoadControllerService
                .execute(DlcLoadRequest.builder()
                        .topics(DREMIO_TOPICS)
                        .scope(DlcScope.of(COB_DATE_SCOPE_PARAMETER, cobDate.format(DATE_FORMATTER))) // FIXME: do we need to format?
                        .sourceType(DlcSourceType.JDBC_SOURCE)
                        .build())
                .toDto();
        DatabasePrinter.printTableSizes(datastore.getMasterHead());
        return result;
    }

    @DeleteMapping({"/{cobDate}"})
    public DlcUnloadResponseDTO deleteCobDate(@PathVariable @DateTimeFormat(pattern = DATE_FORMAT) LocalDate cobDate) {
        var result = dataLoadControllerService
                .execute(DlcUnloadRequest.builder()
                        .topics(DREMIO_UNLOAD_TOPIC)
                        .scope(DlcScope.of(COB_DATE_SCOPE_PARAMETER, cobDate.format(DATE_FORMATTER))) // FIXME: do we need to format?
                        .build())
                .toDto();
        DatabasePrinter.printTableSizes(datastore.getMasterHead());
        return result;
    }
}
