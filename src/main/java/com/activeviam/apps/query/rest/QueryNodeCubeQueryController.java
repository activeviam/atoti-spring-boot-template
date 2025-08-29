/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;
import static com.activeviam.apps.rest.RemoteRestServiceUtils.AUTH_HEADER;
import static com.activeviam.apps.rest.RemoteRestServiceUtils.datastoreRestClient;
import static com.activeviam.apps.rest.RemoteRestServiceUtils.dqRestClient;

import java.io.InputStream;
import java.util.Collections;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDistributedActivePivot;
import com.activeviam.activepivot.server.json.api.dataexport.JsonCsvPivotTableOutputConfiguration;
import com.activeviam.apps.query.CubeQueryService;
import com.activeviam.apps.rest.RemoteRestServiceUtils;
import com.activeviam.tech.core.api.exceptions.ActiveViamRuntimeException;

public class QueryNodeCubeQueryController extends CubeQueryController {

    private final IActivePivotManager activePivotManager;

    public static final String QUERY_ENDPOINT = CUSTOM_REST_PATH + "/cube_query";

    public QueryNodeCubeQueryController(CubeQueryService cubeQueryService, IActivePivotManager activePivotManager) {
        super(cubeQueryService);
        this.activePivotManager = activePivotManager;
    }

    @Override
    protected StreamingResponseBody processDTO(CubeQueryDTO queryDTO, CubeQueryService.CubeQuerier querier) {
        var query = querier.convertCubeQuery(queryDTO);
        var cube = querier.getCube();
        var datesInQuery = query.getQueryFilter().getFilteredCobDates();
        var activePivot = (IMultiVersionDistributedActivePivot) activePivotManager.getActivePivot(cube);
        var inMemoryDates = RemoteRestServiceUtils.datastoreRestClient(activePivot)
                .map(RemoteRestServiceUtils::getNodeDates)
                .orElse(Collections.emptyList());
        if (!inMemoryDates.isEmpty() && inMemoryDates.containsAll(datesInQuery)) {
            // Run the query on the inMemory node
            return datastoreRestClient(activePivot)
                            .map(client -> runQueryOnRemoteCube(queryDTO, cube, client))
                            .orElseThrow(
                                    () -> new ActiveViamRuntimeException("Could not run query on remote InMemory cube"))
                    ::transferTo;
        }
        var dqDates = RemoteRestServiceUtils.dqRestClient(activePivot)
                .map(RemoteRestServiceUtils::getNodeDates)
                .orElse(Collections.emptyList());
        if (!dqDates.isEmpty() && dqDates.containsAll(datesInQuery)) {
            // Run the query on the inMemory node
            return dqRestClient(activePivot)
                            .map(client -> runQueryOnRemoteCube(queryDTO, cube, client))
                            .orElseThrow(() -> new ActiveViamRuntimeException("Could not run query on remote DQ cube"))
                    ::transferTo;
        }
        // Run locally
        return querier.runQuery(query, JsonCsvPivotTableOutputConfiguration.PLUGIN_KEY);
    }

    private InputStream runQueryOnRemoteCube(CubeQueryDTO queryDTO, String cube, WebClient webClient) {
        return DataBufferUtils.subscriberInputStream(
                webClient
                        .post()
                        .uri(QUERY_ENDPOINT + '/' + cube)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER)
                        .accept(MediaType.APPLICATION_OCTET_STREAM)
                        .bodyValue(queryDTO)
                        .retrieve()
                        .bodyToFlux(DataBuffer.class),
                1);
    }
}
