/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

@Data
@Builder(setterPrefix = "with")
public class CubeQueryDTO {
    @Singular
    private List<MetricDTO> metrics;

    @Singular
    private List<String> levels;

    @Singular
    private List<FilterDTO> filters;
    // NOTE: filtersExpression will replace filters map so we can support the OR condition
    private String filtersExpression;
    private TopCountDTO topCounts;

    @Singular
    private List<SortDTO> sortBys;

    private TopRankDTO topRank;

    @Singular
    private List<PartitioningDTO> partitionedBys;

    @Data
    @Builder(setterPrefix = "with")
    public static class FilterDTO {
        @NonNull
        private String level;

        @NonNull
        private List<String> values;

        @Builder.Default
        private boolean exclude = false;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class MetricDTO {
        @NonNull
        private String metric;

        @Builder.Default
        private String parameter = null;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class TopCountDTO {
        @NonNull
        private String metric;

        @NonNull
        private String level;

        @Builder.Default
        private int count = 5;

        @Builder.Default
        private boolean bottom = false;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class SortDTO {
        @NonNull
        private String metric;

        @NonNull
        String level;

        @Builder.Default
        boolean ascending = false;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class TopRankDTO {
        @NonNull
        private String metric;

        @NonNull
        private String level;

        @Builder.Default
        private int topN = 5;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class PartitioningDTO {
        @NonNull
        private String newMetric;

        @NonNull
        private String metric;

        @NonNull
        private String level;
    }
}
