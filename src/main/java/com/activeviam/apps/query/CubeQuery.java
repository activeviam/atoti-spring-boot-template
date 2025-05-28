/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.util.ObjectUtils;

import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.apps.query.rest.CubeQueryDTO;

import lombok.Data;

@Data
public class CubeQuery {
    private final List<Metric> metrics;
    private final List<LevelIdentifier> levels;
    // Filters only support the AND condition
    private final List<Filter> filters;
    // FiltersExpression will replace filters to support OR
    private final String filtersExpression;
    private final CubeQuery.TopCount topCounts;
    private final List<CubeQuery.Sort> sortBy;
    private final CubeQuery.TopRank topRank;
    private final List<CubeQuery.Partitioning> partitionedBy;

    public static String calculatedMemberDefaultName(String metric, String level) {
        return metric + "@" + level;
    }

    public record Metric(String metric, String parameter) {
        public String getMetricName() {
            return ObjectUtils.isEmpty(parameter) ? metric : metric + "_" + parameter;
        }

        public static Metric fromDTO(CubeQueryDTO.MetricDTO dto) {
            return new Metric(dto.getMetric(), dto.getParameter());
        }
    }

    public record Filter(LevelIdentifier level, List<String> values, boolean exclude) {

        public static Filter fromDTO(CubeQueryDTO.FilterDTO dto, LevelsConverter levelsConverter) {
            return new Filter(
                    levelsConverter.stringToLevelIdentifier(dto.getLevel()), dto.getValues(), dto.isExclude());
        }
    }

    public record TopCount(String metric, LevelIdentifier level, int count, boolean bottom, boolean aggregateOthers) {

        public static TopCount fromDTO(CubeQueryDTO.TopCountDTO dto, LevelsConverter levelsConverter) {
            return Objects.nonNull(dto)
                    ? new TopCount(
                            dto.getMetric(),
                            levelsConverter.stringToLevelIdentifier(dto.getLevel()),
                            dto.getCount(),
                            dto.isBottom(),
                            dto.isAggregateOthers())
                    : null;
        }
    }

    public record Sort(String metric, LevelIdentifier level, String sortType) {

        public static Sort fromDTO(CubeQueryDTO.SortDTO dto, LevelsConverter levelsConverter) {
            return new Sort(
                    ObjectUtils.isEmpty(dto.getMetric())
                            ? dto.getLevel()
                            : dto.getMetric(), // If we only specify the level, we use level as metric as well
                    levelsConverter.stringToLevelIdentifier(dto.getLevel()),
                    dto.isAscending() ? "ASC" : "DESC");
        }
    }

    public record TopRank(String metric, LevelIdentifier level) {
        public static TopRank fromDTO(CubeQueryDTO.TopRankDTO dto, LevelsConverter levelsConverter) {
            return Objects.nonNull(dto)
                    ? new TopRank(dto.getMetric(), levelsConverter.stringToLevelIdentifier(dto.getLevel()))
                    : null;
        }
    }

    public record Partitioning(String newMetric, String metric, LevelIdentifier level) {
        public static Partitioning fromDTO(CubeQueryDTO.PartitioningDTO dto, LevelsConverter levelsConverter) {
            return Objects.nonNull(dto)
                    ? new Partitioning(
                            Optional.ofNullable(dto.getNewMetric())
                                    .orElse(calculatedMemberDefaultName(dto.getMetric(), dto.getLevel())),
                            dto.getMetric(),
                            levelsConverter.stringToLevelIdentifier(dto.getLevel()))
                    : null;
        }
    }

    public static CubeQuery fromDTO(CubeQueryDTO dto, LevelsConverter levelsConverter) {
        return new CubeQuery(
                Optional.ofNullable(dto.getMetrics()).orElse(Collections.emptyList()).stream()
                        .map(Metric::fromDTO)
                        .toList(),
                Optional.ofNullable(dto.getLevels()).orElse(Collections.emptyList()).stream()
                        .map(levelsConverter::stringToLevelIdentifier)
                        .toList(),
                Optional.ofNullable(dto.getFilters()).orElse(Collections.emptyList()).stream()
                        .map(filter -> Filter.fromDTO(filter, levelsConverter))
                        .toList(),
                dto.getFiltersExpression(),
                TopCount.fromDTO(dto.getTopCounts(), levelsConverter),
                Optional.ofNullable(dto.getSortBys()).orElse(Collections.emptyList()).stream()
                        .map(s -> Sort.fromDTO(s, levelsConverter))
                        .toList(),
                TopRank.fromDTO(dto.getTopRank(), levelsConverter),
                Optional.ofNullable(dto.getPartitionedBys()).orElse(Collections.emptyList()).stream()
                        .map(p -> Partitioning.fromDTO(p, levelsConverter))
                        .toList());
    }
}
