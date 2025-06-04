/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import static com.activeviam.activepivot.server.json.api.dataexport.IJsonOutputConfiguration.FORMAT_PROPERTY;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.activeviam.activepivot.server.json.api.dataexport.JsonCsvPivotTableOutputConfiguration;
import com.activeviam.apps.query.CubeQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(CubeQueryController.QUERY_ENDPOINT)
@RequiredArgsConstructor
public class CubeQueryController {
    private final CubeQueryService cubeQueryService;

    public static final String QUERY_ENDPOINT = "/cube_query";

    public static final Map<String, Object> CSV_OUTPUT_EXPORTER_CONFIG =
            Map.of(FORMAT_PROPERTY, JsonCsvPivotTableOutputConfiguration.PLUGIN_KEY);

    @PostMapping("/mdx/{cube}")
    public String getMdxQueryforCube(@PathVariable String cube, @RequestBody CubeQueryDTO queryDTO) {
        var querier = cubeQueryService.getCubeQuerier(cube);
        return querier.buildMdxQuery(querier.convertCubeQuery(queryDTO));
    }

    @PostMapping("/mdx")
    public String getMdxQueryForDefaultCube(@RequestBody CubeQueryDTO queryDTO) {
        var querier = cubeQueryService.getDefaultCubeQuerier();
        return querier.buildMdxQuery(querier.convertCubeQuery(queryDTO));
    }

    @PostMapping("/{cube}")
    public StreamingResponseBody runQueryOnCube(@PathVariable String cube, @RequestBody CubeQueryDTO queryDTO) {
        var querier = cubeQueryService.getCubeQuerier(cube);
        return querier.runQuery(querier.convertCubeQuery(queryDTO), CSV_OUTPUT_EXPORTER_CONFIG);
    }

    @PostMapping()
    public StreamingResponseBody runQueryOnDefaultCube(@RequestBody CubeQueryDTO queryDTO) {
        var querier = cubeQueryService.getDefaultCubeQuerier();
        return querier.runQuery(querier.convertCubeQuery(queryDTO), CSV_OUTPUT_EXPORTER_CONFIG);
    }
}
