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
import com.activeviam.apps.query.conditions.FilterExpressionConditionVisitor;
import com.activeviam.apps.query.conditions.LogicalCondition;
import com.activeviam.apps.query.conditions.TrueLogicalCondition;
import com.activeviam.apps.query.rest.CubeQueryDTO;

import lombok.Data;

@Data
public class CubeQuery {
    private final List<String> metrics;
    private final List<LevelIdentifier> levels;
    private final LogicalCondition filter;
    private final CubeQuery.TopCount topCount;
    private final List<CubeQuery.Sort> sortBy;
    private final CubeQuery.TopRank topRank;
    private final List<CubeQuery.Partitioning> partitionedBy;
    private final boolean useContext;
    private final HideTotals hideTotals;

    public static String calculatedMemberDefaultName(String metric, String level) {
        return metric + "@" + level;
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

    public record HideTotals(boolean all, boolean grandTotal, List<LevelIdentifier> levels) {
        public static HideTotals fromDTO(CubeQueryDTO.HideTotalsDTO dto, LevelsConverter levelsConverter) {
            return new HideTotals(
                    dto.isAll(),
                    dto.isGrandTotal(),
                    dto.getLevels().stream()
                            .map(levelsConverter::stringToLevelIdentifier)
                            .toList());
        }
    }

    public static CubeQuery fromDTO(CubeQueryDTO dto, LevelsConverter levelsConverter) {
        var filter = ObjectUtils.isEmpty(dto.getFiltersExpression())
                ? new TrueLogicalCondition()
                : FilterExpressionConditionVisitor.parseFilterExpression(dto.getFiltersExpression());
        // If we dont force the use of context or not, we always use context unless there is a measure filter
        var useContext = Optional.ofNullable(dto.getUseContext())
                .orElse(!CubeQueryService.CubeQuerier.containsMeasureFilter(filter));
        return new CubeQuery(
                Optional.ofNullable(dto.getMetrics()).orElse(Collections.emptyList()),
                Optional.ofNullable(dto.getLevels()).orElse(Collections.emptyList()).stream()
                        .map(levelsConverter::stringToLevelIdentifier)
                        .toList(),
                filter,
                TopCount.fromDTO(dto.getTopCount(), levelsConverter),
                Optional.ofNullable(dto.getSortBys()).orElse(Collections.emptyList()).stream()
                        .map(s -> Sort.fromDTO(s, levelsConverter))
                        .toList(),
                TopRank.fromDTO(dto.getTopRank(), levelsConverter),
                Optional.ofNullable(dto.getPartitionedBys()).orElse(Collections.emptyList()).stream()
                        .map(p -> Partitioning.fromDTO(p, levelsConverter))
                        .toList(),
                useContext,
                HideTotals.fromDTO(
                        Optional.ofNullable(dto.getHideTotals())
                                .orElse(CubeQueryDTO.HideTotalsDTO.builder().build()),
                        levelsConverter));
    }
}
