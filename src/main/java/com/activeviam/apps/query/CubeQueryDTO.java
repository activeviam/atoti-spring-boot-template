/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class CubeQueryDTO {
    private final List<MetricDTO> metrics = new ArrayList<>();
    private final List<String> levels = new ArrayList<>();
    private final Map<String, List<String>> filters = new HashMap<>();
    private final String filter;
    private final TopCountDTO topCounts;
    private final List<SortDTO> sortBy = new ArrayList<>();
    private final TopRankDTO topRank;
    private final List<PartitioningDTO> partitionedBy = new ArrayList<>();

    public record MetricDTO(String metric, String parameter) {}

    public record TopCountDTO(String metric, String level, int count, boolean bottom) {}

    public record SortDTO(String metric, String level, boolean ascending) {}

    public record TopRankDTO(String metric, String level, int topN) {}

    public record PartitioningDTO(String newMetric, String metric, String level) {}
}
