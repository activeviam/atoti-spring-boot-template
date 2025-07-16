/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "cube-query-service")
public class CubeQueryProperties {
    private String defaultCube;
    private Map<String, CubeDefaults> cubeConfiguration;

    @Data
    public static class CubeDefaults {
        List<String> dateFilterLevels;
        String defaultDimension;
    }
}
