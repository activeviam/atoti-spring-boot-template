/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.query.conditions.TrueLogicalCondition.isTrueCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.util.ObjectUtils;

import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.apps.query.conditions.AndLogicalCondition;
import com.activeviam.apps.query.conditions.FilterExpressionConditionVisitor;
import com.activeviam.apps.query.conditions.InLogicalCondition;
import com.activeviam.apps.query.conditions.LogicalCondition;
import com.activeviam.apps.query.conditions.MeasureCondition;
import com.activeviam.apps.query.conditions.NotLogicalCondition;
import com.activeviam.apps.query.conditions.OrLogicalCondition;
import com.activeviam.apps.query.conditions.TrueLogicalCondition;
import com.activeviam.apps.query.rest.CubeQueryDTO;

import lombok.Data;

@Data
public class CubeQuery {
    private final List<String> metrics;
    private final List<LevelIdentifier> levels;
    private final SplitQueryCondition queryFilter;
    private final CubeQuery.TopCount topCount;
    private final CubeQuery.Sort<?> sortBy;
    private final CubeQuery.TopRank topRank;
    private final List<CubeQuery.Partitioning> partitionedBy;
    private final List<MetricDefinition> metricDefinitions;
    private final boolean useContext;
    private final HideTotals hideTotals;

    public static String calculatedMemberDefaultName(String metric, String level) {
        return metric + "@" + level;
    }

    public record MetricDefinition(String name, String expression) {

        public static CubeQuery.MetricDefinition fromDTO(CubeQueryDTO.MetricDefinitionDTO dto) {
            return new CubeQuery.MetricDefinition(dto.getName(), dto.getDefinition());
        }
    }

    public record TopCount(
            String metric, List<LevelIdentifier> groupBy, int count, boolean bottom, boolean aggregateOthers) {

        public static TopCount fromDTO(CubeQueryDTO.TopCountDTO dto, LevelsConverter levelsConverter) {
            return Objects.nonNull(dto)
                    ? new TopCount(
                            dto.getMetric(),
                            Optional.ofNullable(dto.getGroupBy())
                                    .map(group -> group.stream()
                                            .map(levelsConverter::stringToLevelIdentifier)
                                            .toList())
                                    .orElse(Collections.emptyList()),
                            dto.getCount(),
                            dto.isBottom(),
                            dto.isAggregateOthers())
                    : null;
        }
    }

    public record Sort<T>(T column, String sortType) {}

    public record TopRank(String metric, LevelIdentifier level) {}

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

    static SplitQueryCondition splitMeasureFilter(LogicalCondition filter) {
        var andConditions = splitAndLogicalConditions(filter);
        var measureFiltersList = andConditions.stream()
                .filter(CubeQuery::containsOnlyMeasureFilter)
                .toList();
        var cobDateFiltersList =
                andConditions.stream().filter(CubeQuery::isCobDateFilter).toList();
        if (cobDateFiltersList.size() > 1) {
            throw new IllegalArgumentException("Filter expression contais multiple CobDate filters");
        }
        var otherFiltersList = new ArrayList<>(andConditions);
        otherFiltersList.removeAll(measureFiltersList);
        otherFiltersList.removeAll(cobDateFiltersList);
        var otherFiltersCondition =
                otherFiltersList.isEmpty() ? TrueLogicalCondition.INSTANCE : new AndLogicalCondition(otherFiltersList);
        var measureFiltersCondition = measureFiltersList.isEmpty()
                ? TrueLogicalCondition.INSTANCE
                : new AndLogicalCondition(measureFiltersList);
        // Validate output
        // Make sure the otherFilters dont contains a measure filter
        // And viceversa
        if (!isTrueCondition(otherFiltersCondition) && containsMeasureFilter(otherFiltersCondition)) {
            throw new IllegalArgumentException("Filter expression contains measure filter that is not AND");
        }
        // should never happen?
        if (!(isTrueCondition(measureFiltersCondition)) && containsOtherFilter(measureFiltersCondition)) {
            throw new IllegalArgumentException("Measure filter cannot be split from other filters");
        }
        return new SplitQueryCondition(
                cobDateFiltersList.isEmpty() ? null : cobDateFiltersList.getFirst(),
                otherFiltersList,
                measureFiltersList);
    }

    static List<LogicalCondition> splitAndLogicalConditions(LogicalCondition condition) {
        return condition instanceof AndLogicalCondition andLogicalCondition
                ? andLogicalCondition.getSubConditions().stream()
                        .map(CubeQuery::splitAndLogicalConditions)
                        .flatMap(List::stream)
                        .toList()
                : List.of(condition);
    }

    public static CubeQuery fromDTO(CubeQueryDTO dto, LevelsConverter levelsConverter, Set<String> measures) {
        var filter = ObjectUtils.isEmpty(dto.getFiltersExpression())
                ? TrueLogicalCondition.INSTANCE
                : FilterExpressionConditionVisitor.parseFilterExpression(dto.getFiltersExpression());
        var splitFilter = splitMeasureFilter(filter);
        // If we dont force the use of context or not, we always use context
        var useContext = Optional.ofNullable(dto.getUseContext()).orElse(true);
        return new CubeQuery(
                Optional.ofNullable(dto.getMetrics()).orElse(Collections.emptyList()),
                Optional.ofNullable(dto.getLevels()).orElse(Collections.emptyList()).stream()
                        .map(levelsConverter::stringToLevelIdentifier)
                        .toList(),
                splitFilter,
                TopCount.fromDTO(dto.getTopCount(), levelsConverter),
                Optional.ofNullable(dto.getSortBy())
                        .map(s -> {
                            var sortType = computeSortType(s.getColumn(), s.isAscending());
                            if (measures.contains(s.getColumn())) {
                                return new Sort<String>(s.getColumn(), sortType);
                            } else {
                                return new Sort<LevelIdentifier>(
                                        levelsConverter.stringToLevelIdentifier(s.getColumn()), sortType);
                            }
                        })
                        .orElse(null),
                Optional.ofNullable(dto.getTopRank())
                        .map(t -> new TopRank(t.getMetric(), levelsConverter.stringToLevelIdentifier(t.getLevel())))
                        .orElse(null),
                Optional.ofNullable(dto.getPartitionedBys()).orElse(Collections.emptyList()).stream()
                        .map(p -> Partitioning.fromDTO(p, levelsConverter))
                        .toList(),
                Optional.ofNullable(dto.getMetricDefinitions()).orElse(Collections.emptyList()).stream()
                        .map(MetricDefinition::fromDTO)
                        .toList(),
                useContext,
                HideTotals.fromDTO(
                        Optional.ofNullable(dto.getHideTotals())
                                .orElse(CubeQueryDTO.HideTotalsDTO.builder().build()),
                        levelsConverter));
    }

    private static String computeSortType(String level, boolean isAscending) {
        // COB_DATE is sorted in reverse order so we need to reverse this
        return level.equalsIgnoreCase(COB_DATE) != isAscending ? "BASC" : "BDESC";
    }

    public static boolean isCobDateLevel(LevelIdentifier levelIdentifier) {
        return levelIdentifier.getLevelName().equals(COB_DATE);
    }

    public static boolean containsMeasureFilter(LogicalCondition queryCondition) {
        return switch (queryCondition) {
            case MeasureCondition measureCondition -> true;
            case AndLogicalCondition andLogicalCondition ->
                andLogicalCondition.getSubConditions().stream().anyMatch(CubeQuery::containsMeasureFilter);
            case OrLogicalCondition orLogicalCondition ->
                orLogicalCondition.getSubConditions().stream().anyMatch(CubeQuery::containsMeasureFilter);
            case NotLogicalCondition notLogicalCondition -> containsMeasureFilter(notLogicalCondition.getCondition());
            default -> false;
        };
    }

    public static boolean isCobDateFilter(LogicalCondition queryCondition) {
        return queryCondition instanceof InLogicalCondition<?> inLogicalCondition
                && inLogicalCondition.getField().equalsIgnoreCase(COB_DATE);
    }

    public static boolean containsOtherFilter(LogicalCondition queryCondition) {
        return switch (queryCondition) {
            case MeasureCondition measureCondition -> false;
            case AndLogicalCondition andLogicalCondition ->
                andLogicalCondition.getSubConditions().stream().anyMatch(CubeQuery::containsOtherFilter);
            case OrLogicalCondition orLogicalCondition ->
                orLogicalCondition.getSubConditions().stream().anyMatch(CubeQuery::containsOtherFilter);
            case NotLogicalCondition notLogicalCondition -> containsOtherFilter(notLogicalCondition.getCondition());
            default -> true;
        };
    }

    public static boolean containsOnlyMeasureFilter(LogicalCondition queryCondition) {
        return !containsOtherFilter(queryCondition) && containsMeasureFilter(queryCondition);
    }

    public record SplitQueryCondition(
            LogicalCondition cobDateCondition,
            List<LogicalCondition> otherConditions,
            List<LogicalCondition> measureConditions) {
        public LogicalCondition generateCobDateCondition() {
            return Optional.ofNullable(cobDateCondition).orElse(TrueLogicalCondition.INSTANCE);
        }

        public LogicalCondition generateOtherCondition() {
            return ObjectUtils.isEmpty(otherConditions)
                            || (otherConditions.size() == 1
                                    && TrueLogicalCondition.isTrueCondition(otherConditions.getFirst()))
                    ? TrueLogicalCondition.INSTANCE
                    : new AndLogicalCondition(otherConditions);
        }

        public LogicalCondition generateMeasuresCondition() {
            return ObjectUtils.isEmpty(measureConditions)
                    ? TrueLogicalCondition.INSTANCE
                    : new AndLogicalCondition(measureConditions);
        }

        public LogicalCondition generateOtherAndCobDateCondition() {
            var otherAndDateCondition = new ArrayList<LogicalCondition>(otherConditions);
            if (Objects.nonNull(cobDateCondition)) {
                otherAndDateCondition.add(cobDateCondition);
            }
            return ObjectUtils.isEmpty(otherAndDateCondition)
                    ? TrueLogicalCondition.INSTANCE
                    : new AndLogicalCondition(otherAndDateCondition);
        }
    }
}
