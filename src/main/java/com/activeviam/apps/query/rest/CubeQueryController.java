/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.activeviam.apps.query.CubeQueryProperties;
import com.activeviam.apps.query.CubeQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(CubeQueryController.QUERY_ENDPOINT)
@RequiredArgsConstructor
public class CubeQueryController {
    private final CubeQueryService cubeQueryService;
    private final CubeQueryProperties cubeQueryProperties;

    public static final String QUERY_ENDPOINT = "/cube_query";

    @PostMapping("/mdx/{cube}")
    public String getMdxQueryforCube(@PathVariable String cube, @RequestBody CubeQueryDTO queryDTO) {
        return cubeQueryService.buildMdxQuery(cube, queryDTO);
    }

    @PostMapping("/mdx")
    public String getMdxQueryForDefaultCube(@RequestBody CubeQueryDTO queryDTO) {
        return cubeQueryService.buildMdxQuery(cubeQueryProperties.getDefaultCube(), queryDTO);
    }

    @PostMapping("/{cube}")
    public StreamingResponseBody runQueryOnCube(@PathVariable String cube, @RequestBody CubeQueryDTO queryDTO) {
        return cubeQueryService.runQuery(cube, queryDTO);
    }

    @PostMapping()
    public StreamingResponseBody runQueryOnDefaultCube(@RequestBody CubeQueryDTO queryDTO) {
        return cubeQueryService.runQuery(cubeQueryProperties.getDefaultCube(), queryDTO);
    }
}
