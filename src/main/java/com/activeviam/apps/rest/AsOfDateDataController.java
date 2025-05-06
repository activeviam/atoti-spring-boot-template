/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.cfg.source.DlcConfig.AS_OF_DATE_SCOPE_PARAMETER;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.apps.cfg.source.DlcConfig;
import com.activeviam.database.api.DatabasePrinter;
import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.io.dlc.impl.DataLoadControllerService;
import com.activeviam.io.dlc.impl.operations.request.DlcLoadRequest;
import com.activeviam.io.dlc.impl.operations.request.DlcUnloadRequest;
import com.activeviam.io.dlc.impl.operations.request.scope.DlcScope;
import com.activeviam.io.dlc.impl.rest.resposne.DlcLoadResponseDTO;
import com.activeviam.io.dlc.impl.rest.resposne.DlcUnloadResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(AsOfDateDataController.AS_OF_DATE_ENDPOINT)
@RequiredArgsConstructor
public class AsOfDateDataController {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static final String AS_OF_DATE_ENDPOINT = CUSTOM_REST_PATH + "/as-of-date";

    private final DataLoadControllerService dataLoadControllerService;

    private final IDatastore datastore;

    private static DlcScope asOfDateScope(LocalDate date) {
        return DlcScope.of(AS_OF_DATE_SCOPE_PARAMETER, date.format(DATE_FORMATTER));
    }

    @PostMapping({"/{asOfDate}"})
    public DlcLoadResponseDTO loadAsOfDate(@PathVariable @DateTimeFormat(pattern = DATE_FORMAT) LocalDate asOfDate) {
        var result = dataLoadControllerService
                .execute(DlcLoadRequest.builder()
                        .topics(DlcConfig.ALL_IN_MEMORY_TOPICS)
                        .scope(asOfDateScope(asOfDate))
                        .build())
                .toDto();
        DatabasePrinter.printTableSizes(datastore.getMasterHead());
        return result;
    }

    @DeleteMapping({"/{asOfDate}"})
    public DlcUnloadResponseDTO deleteCobDate(@PathVariable @DateTimeFormat(pattern = DATE_FORMAT) LocalDate asOfDate) {
        var result = dataLoadControllerService
                .execute(DlcUnloadRequest.builder()
                        .topics(DlcConfig.ALL_IN_MEMORY_TOPICS)
                        .scope(asOfDateScope(asOfDate)) // FIXME: do we need to format?
                        .build())
                .toDto();
        DatabasePrinter.printTableSizes(datastore.getMasterHead());
        return result;
    }
}
