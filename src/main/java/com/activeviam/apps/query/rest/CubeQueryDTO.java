/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;

@Data
@Builder(setterPrefix = "with", toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CubeQueryDTO {

    @Singular
    private List<String> metrics;

    @Singular
    private List<String> levels;

    private String filtersExpression;

    @Valid
    private TopCountDTO topCount;

    @Singular
    private List<@Valid SortDTO> sortBys;

    @Singular
    private List<@Valid MetricDefinitionDTO> metricDefinitions;

    @Valid
    private TopRankDTO topRank;

    @Singular
    private List<@Valid PartitioningDTO> partitionedBys;

    private Boolean useContext;

    private HideTotalsDTO hideTotals;

    @Data
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricDefinitionDTO {

        @NonNull
        @NotBlank
        private String name;

        @NonNull
        @NotBlank
        private String definition;
    }

    @Data
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopCountDTO {
        @NonNull
        @NotBlank
        private String metric;

        @NonNull
        @NotBlank
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
        private String metric;

        @NonNull
        @NotBlank
        private String level;

        @Builder.Default
        private boolean ascending = false;
    }

    @Data
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopRankDTO {

        @NonNull
        @NotBlank
        private String metric;

        @NonNull
        @NotBlank
        private String level;
    }

    @Data
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PartitioningDTO {
        private String newMetric;

        @NonNull
        @NotBlank
        private String metric;

        @NonNull
        @NotBlank
        private String level;
    }

    @Data
    @Builder(setterPrefix = "with")
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HideTotalsDTO {

        @Builder.Default
        private boolean all = true;

        @Builder.Default
        private boolean grandTotal = true;

        @NonNull
        @Singular
        private List<String> levels = new ArrayList<>();
    }
}
