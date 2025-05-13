/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import java.util.List;
import java.util.Objects;

import org.springframework.util.ObjectUtils;

import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class CubeQuery {
    private final List<Metric> metrics;
    private final List<LevelIdentifier> levels;
    private final String filter;
    private final CubeQuery.TopCount topCounts;
    private final List<CubeQuery.Sort> sortBy;
    private final CubeQuery.TopRank topRank;
    private final List<CubeQuery.Partitioning> partitionedBy;

    public record Metric(String metric, String parameter) {
        public String getMetricName() {
            return ObjectUtils.isEmpty(parameter) ? metric : metric + "_" + parameter;
        }

        public static Metric fromDTO(CubeQueryDTO.MetricDTO dto) {
            return new Metric(dto.metric(), dto.parameter());
        }
    }

    public record Filter(LevelIdentifier level, List<String> values) {

        public static Filter fromDTO(String level, List<String> values, LevelsConverter levelsConverter) {
            return new Filter(levelsConverter.stringToLevelIdentifier(level), values);
        }
    }

    public record TopCount(String metric, LevelIdentifier level, int count, boolean bottom) {

        public static TopCount fromDTO(CubeQueryDTO.TopCountDTO dto, LevelsConverter levelsConverter) {
            return Objects.nonNull(dto)
                    ? new TopCount(
                            dto.metric(),
                            levelsConverter.stringToLevelIdentifier(dto.level()),
                            dto.count(),
                            dto.bottom())
                    : null;
        }
    }

    public record Sort(String metric, LevelIdentifier level, String sortType) {

        public static Sort fromDTO(CubeQueryDTO.SortDTO dto, LevelsConverter levelsConverter) {
            return new Sort(
                    ObjectUtils.isEmpty(dto.metric())
                            ? dto.level()
                            : dto.metric(), // If we only specify the level, we use level as metric as well
                    levelsConverter.stringToLevelIdentifier(dto.level()),
                    dto.ascending() ? "ASC" : "DESC");
        }
    }

    public record TopRank(String metric, LevelIdentifier level, int topN) {
        public static TopRank fromDTO(CubeQueryDTO.TopRankDTO dto, LevelsConverter levelsConverter) {
            return Objects.nonNull(dto)
                    ? new TopRank(dto.metric(), levelsConverter.stringToLevelIdentifier(dto.level()), dto.topN())
                    : null;
        }
    }

    public record Partitioning(String newMetric, String metric, LevelIdentifier level) {
        public static Partitioning fromDTO(CubeQueryDTO.PartitioningDTO dto, LevelsConverter levelsConverter) {
            return Objects.nonNull(dto)
                    ? new Partitioning(
                            dto.newMetric(), dto.metric(), levelsConverter.stringToLevelIdentifier(dto.level()))
                    : null;
        }
    }

    public static CubeQuery fromDTO(CubeQueryDTO dto, LevelsConverter levelsConverter) {
        return new CubeQuery(
                dto.getMetrics().stream().map(Metric::fromDTO).toList(),
                dto.getLevels().stream()
                        .map(levelsConverter::stringToLevelIdentifier)
                        .toList(),
                dto.getFilter(),
                TopCount.fromDTO(dto.getTopCounts(), levelsConverter),
                dto.getSortBy().stream()
                        .map(s -> Sort.fromDTO(s, levelsConverter))
                        .toList(),
                TopRank.fromDTO(dto.getTopRank(), levelsConverter),
                dto.getPartitionedBy().stream()
                        .map(p -> Partitioning.fromDTO(p, levelsConverter))
                        .toList());
    }
}
