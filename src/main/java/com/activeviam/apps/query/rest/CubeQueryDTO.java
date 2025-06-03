/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;

@Data
@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
public class CubeQueryDTO {

    @Singular
    private List<String> metrics;

    @Singular
    private List<String> levels;

    private String filtersExpression;
    private TopCountDTO topCount;

    @Singular
    private List<SortDTO> sortBys;

    private TopRankDTO topRank;

    @Singular
    private List<PartitioningDTO> partitionedBys;

    @Builder.Default
    private boolean useContext = true;

    @Data
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopCountDTO {
        @NonNull
        private String metric;

        @NonNull
        private String level;

        @Builder.Default
        private int count = 5;

        @Builder.Default
        private boolean bottom = false;

        @Builder.Default
        private boolean aggregateOthers = true;
    }

    @Data
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
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
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopRankDTO {
        @NonNull
        private String metric;

        @NonNull
        private String level;
    }

    @Data
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PartitioningDTO {
        private String newMetric;

        @NonNull
        private String metric;

        @NonNull
        private String level;
    }
}
