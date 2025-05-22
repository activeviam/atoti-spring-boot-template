/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder(setterPrefix = "with")
public class CubeQueryDTO {
    @Singular
    private List<MetricDTO> metrics = new ArrayList<>();

    @Singular
    private List<String> levels = new ArrayList<>();

    @Singular
    private Map<String, List<String>> filters = new HashMap<>();
    // NOTE: filtersExpression will replace filters map so we can support the OR condition
    private String filtersExpression;
    private TopCountDTO topCounts;

    @Singular
    private List<SortDTO> sortBys = new ArrayList<>();

    private TopRankDTO topRank;

    @Singular
    private List<PartitioningDTO> partitionedBys = new ArrayList<>();

    @Data
    @Builder(setterPrefix = "with")
    public static class MetricDTO {
        private String metric;

        @Builder.Default
        private String parameter = null;

        public MetricDTO(String metric, String parameter) {
            this.metric = metric;
            this.parameter = parameter;
        }

        public MetricDTO(String metric) {
            this(metric, null);
        }
    }

    public record TopCountDTO(String metric, String level, int count, boolean bottom) {}

    public record SortDTO(String metric, String level, boolean ascending) {}

    public record TopRankDTO(String metric, String level, int topN) {}

    public record PartitioningDTO(String newMetric, String metric, String level) {}
}
