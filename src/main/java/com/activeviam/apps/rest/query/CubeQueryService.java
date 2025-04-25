/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest.query;

import static com.activeviam.activepivot.server.json.api.dataexport.IJsonOutputConfiguration.FORMAT_PROPERTY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.metadata.HierarchyIdentifier;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.server.intf.api.dataexport.IDataExportService;
import com.activeviam.activepivot.server.json.api.dataexport.JsonCsvPivotTableOutputConfiguration;
import com.activeviam.activepivot.server.json.api.dataexport.JsonDataExportOrder;
import com.activeviam.activepivot.server.json.api.query.JsonMdxQuery;

@Service
public class CubeQueryService {

    private final Map<String, Set<HierarchyIdentifier>> slicingHierarchiesByCube = new HashMap<>();
    private final IDataExportService dataExportService;
    private final Map<String, Set<LevelIdentifier>> dateLevelsForFiltering = new HashMap<>();
    private final Map<String, LevelsConverter> levelsConverters = new HashMap<>();

    public CubeQueryService(
            IDataExportService dataExportService,
            IActivePivotManager activePivotManager,
            CubeQueryProperties cubeQueryProperties) {
        this.dataExportService = dataExportService;
        activePivotManager.getActivePivots().forEach((cube, pivot) -> {
            var slicing = pivot.getDescription().getAxisDimensions().getValues().stream()
                    .flatMap(dimension -> dimension.getHierarchies().stream())
                    .filter(hierarchy -> !hierarchy.isAllMembersEnabled())
                    .map(hierarchy -> HierarchyIdentifier.simple(hierarchy.getName()))
                    .collect(Collectors.toSet());
            slicingHierarchiesByCube.put(cube, slicing);
        });
        cubeQueryProperties.getCubeDefaults().forEach((cube, defaults) -> {
            var levelConverter = new SingleDimensionLevelsConverter(defaults.getDefaultDimension());
            levelsConverters.put(cube, levelConverter);
            dateLevelsForFiltering
                    .computeIfAbsent(cube, key -> new HashSet<>())
                    .addAll(defaults.getDateFilterLevels().stream()
                            .map(levelConverter::stringToLevelIdentifier)
                            .collect(Collectors.toSet()));
        });
    }

    public StreamingResponseBody runQuery(String cube, CubeQueryDTO dto) {
        Map<String, Object> config = Map.of(FORMAT_PROPERTY, JsonCsvPivotTableOutputConfiguration.PLUGIN_KEY);
        var cubeQuery = CubeQuery.fromDTO(dto, levelsConverters.get(cube));
        var dataExportOrder = new JsonDataExportOrder(
                new JsonMdxQuery(buildMdxQuery(cube, cubeQuery), buildQueryContext(cubeQuery)), config);
        return dataExportService.streamMdxQuery(dataExportOrder);
    }

    private Map<String, String> buildQueryContext(CubeQuery cubeQuery) {
        var context = new HashMap<String, String>();
        var hiddenLevels = cubeQuery.getLevels().stream()
                .map(CubeQueryService::levelIdentifierToMdxLevel)
                .collect(Collectors.joining(","));
        context.put("mdx.hiddensubtotals", hiddenLevels);
        cubeQuery
                .getMetrics()
                .forEach(metric -> context.put(
                        String.format("mdx.formatters.[Measures].[%s]", metric.getMetricName()), "DOUBLE[####.##]"));
        return context;
    }

    public String buildMdxQuery(String cube, CubeQueryDTO dto) {
        var cubeQuery = CubeQuery.fromDTO(dto, levelsConverters.get(cube));
        return buildMdxQuery(cube, cubeQuery);
    }

    private String buildMdxQuery(String cube, CubeQuery cubeQuery) {
        var query = new StringBuilder();
        var sortBy = cubeQuery.getSortBy();
        var sortByCalculatedMember = ObjectUtils.isEmpty(sortBy)
                ? null
                : sortBy.stream()
                        .filter(CubeQueryService::sortRequiresCalculatedMember)
                        .toList();
        var partitionedBy = cubeQuery.getPartitionedBy();
        var topRank = cubeQuery.getTopRank();
        // If any of these conditions is true, we need to add the calculated Members
        if (!ObjectUtils.isEmpty(sortByCalculatedMember)
                || !ObjectUtils.isEmpty(partitionedBy)
                || Objects.nonNull(topRank)) {
            query.append("WITH ");
            query.append(System.lineSeparator());

            // Sort
            if (!ObjectUtils.isEmpty(sortByCalculatedMember)) {
                query.append(sortByCalculatedMember.stream()
                        .map(CubeQueryService::sortingCalculatedMeasure)
                        .collect(Collectors.joining(",")));
                query.append(System.lineSeparator());
            }

            // Partitioning
            if (!ObjectUtils.isEmpty(partitionedBy)) {
                query.append(partitionedBy.stream()
                        .map(CubeQueryService::partitioningCalculatedMeasureMdx)
                        .collect(Collectors.joining(",")));
                query.append(System.lineSeparator());
            }

            // Top Rank
            if (!ObjectUtils.isEmpty(topRank)) {
                query.append(topRankCalculatedMeasureMdx(topRank));
                query.append(System.lineSeparator());
            }
        }

        var levels = cubeQuery.getLevels();
        query.append("SELECT ");
        // Levels and top rank
        if (!ObjectUtils.isEmpty(levels)) {
            query.append(hierarchizedLevelsWithTopRank(
                    levels,
                    topRank,
                    levels.stream()
                            .map(level -> sortedHierarchizedLevels(
                                    level, levelSorting(level, sortBy), isSlicingHierarchy(cube, level.getHierarchy())))
                            .collect(Collectors.joining(","))));
            query.append(System.lineSeparator());
        }

        // Metrics
        var metrics = cubeQuery.getMetrics();
        if (!ObjectUtils.isEmpty(metrics)) {
            query.append(String.format(
                    "{%s} ON COLUMNS",
                    metrics.stream().map(CubeQueryService::metricToMdx).collect(Collectors.joining(","))));
            query.append(System.lineSeparator());
        }

        // Filters
        var filters = cubeQuery.getFilters().stream()
                .filter(filter -> isNotDateFilter(filter, cube))
                .toList();
        var dateFilters = cubeQuery.getFilters().stream()
                .filter(filter -> isDateFilter(filter, cube))
                .toList();
        if (!ObjectUtils.isEmpty(filters)) {
            // Top count
            if (!ObjectUtils.isEmpty(cubeQuery.getTopCounts()) && !ObjectUtils.isEmpty(levels)) {
                query.append(topCountToMdx(cubeQuery.getTopCounts()));
                query.append(System.lineSeparator());
            }
            // Other filters
            filters.forEach(filter -> {
                query.append(filterToMdx(filter));
                query.append(System.lineSeparator());
            });
            // From Cube
            query.append(fromCubeWithDateFilterMdx(cube, dateFilters));
            query.append(System.lineSeparator());
            // FIXME: where do wen open this?
            query.append(")");
        } else {
            // FROM CUBE
            query.append(fromCubeWithDateFilterMdx(cube, dateFilters));
        }
        return query.toString();
    }

    private String fromCubeWithDateFilterMdx(String cube, List<CubeQuery.Filter> dateFilters) {
        var fromCube = new StringBuilder();
        fromCube.append(String.format("FROM [%s]", cube));
        // Date filter
        dateFilters.stream().findFirst().ifPresent(dateFilter -> fromCube.append(dateFilterToMdx(dateFilter)));
        return fromCube.toString();
    }

    private boolean isSlicingHierarchy(String cube, HierarchyIdentifier levelIdentifier) {
        return slicingHierarchiesByCube.get(cube).contains(levelIdentifier);
    }

    private static Optional<CubeQuery.Sort> levelSorting(
            LevelIdentifier levelIdentifier, List<CubeQuery.Sort> sortByDefinition) {
        return sortByDefinition.stream()
                .filter(sort -> sort.level().equals(levelIdentifier))
                .findAny();
    }

    private static String sortedHierarchizedLevels(
            LevelIdentifier level, Optional<CubeQuery.Sort> sort, boolean isSlicingHierarchy) {
        var hierarchized = isSlicingHierarchy
                ? String.format(
                        "[%s].[%s].[%s].Members",
                        level.getDimensionName(), level.getHierarchyName(), level.getLevelName())
                : String.format(
                        "Hierarchize(Descendants({[%s].[%s].[AllMember]},1,SELF_AND_BEFORE))",
                        level.getDimensionName(), level.getHierarchyName());
        return sort.map(s -> String.format("Order(%s, %s, %s)", hierarchized, sortToMdx(s), s.sortType()))
                .orElse(hierarchized);
    }

    private static String hierarchizedLevelsWithTopRank(
            List<LevelIdentifier> levels, CubeQuery.TopRank topRankDefinition, String hierarchizedLevels) {
        var topRankTemplate = new StringBuilder();
        if (Objects.nonNull(topRankDefinition)) {
            if (levels.size() == 1) {
                topRankTemplate.append("Order(%s)");
            } else {
                topRankTemplate.append("Order(Crossjoin(%s)");
            }
            topRankTemplate
                    .append(", [Measures].[")
                    .append(topRankDefinition.metric())
                    .append("], BDESC) ON ROWS,");
        } else {
            if (levels.size() == 1) {
                topRankTemplate.append("%s");
            } else {
                topRankTemplate.append("NON EMPTY Crossjoin(%s) ");
            }
            topRankTemplate.append(" ON ROWS,");
        }
        return String.format(topRankTemplate.toString(), hierarchizedLevels);
    }

    private static String levelIdentifierToMdxLevel(LevelIdentifier levelIdentifier) {
        return String.format(
                "[%s].[%s].[%s]",
                levelIdentifier.getDimensionName(), levelIdentifier.getHierarchyName(), levelIdentifier.getLevelName());
    }

    private static String metricToMdx(CubeQuery.Metric metric) {
        return String.format("[Measures].[%s]", metric.getMetricName());
    }

    private static String filterToMdx(CubeQuery.Filter filter) {
        return String.format(
                " FROM (SELECT {%s} ON COLUMNS",
                filter.values().stream()
                        .map(value -> String.format(
                                "[%s].[%s].[%s].[%s]",
                                filter.level().getDimensionName(),
                                filter.level().getHierarchyName(),
                                filter.level().getLevelName(),
                                value))
                        .collect(Collectors.joining(",")));
    }

    private static String dateFilterToMdx(CubeQuery.Filter filter) {
        if (filter.values().size() == 1) {
            return String.format(
                    " WHERE [%s].[%s].[%s].[%s]",
                    filter.level().getDimensionName(),
                    filter.level().getHierarchyName(),
                    filter.level().getLevelName(),
                    filter.values().getFirst());
        }
        return "";
    }

    private static String topCountToMdx(CubeQuery.TopCount topCount) {

        return String.format(
                " FROM (SELECT %s(Filter([%s].[%s].Levels(1).Members, NOT IsEmpty([Measures].[%s])), %s, [Measures].[%s]) ON COLUMNS ",
                topCount.bottom() ? "BottomCount" : "TopCount",
                topCount.level().getDimensionName(),
                topCount.level().getHierarchyName(),
                topCount.metric(),
                topCount.count(),
                topCount.metric());
    }

    private static String sortingCalculatedMeasure(CubeQuery.Sort sort) {
        return String.format("[Measures].[%s_sorting]", sort.level().getLevelName());
    }

    private static boolean sortRequiresCalculatedMember(CubeQuery.Sort sort) {
        return sort.metric().equals(sort.level().getLevelName());
    }

    private static String sortToMdx(CubeQuery.Sort sort) {
        return sortRequiresCalculatedMember(sort)
                ? String.format(
                        "Member %s AS [%s].[%s].CurrentMember.MEMBER_VALUE",
                        sortingCalculatedMeasure(sort),
                        sort.level().getDimensionName(),
                        sort.level().getHierarchyName())
                : String.format("[Measures].[%s]", sort.metric());
    }

    private static String topRankCalculatedMeasureMdx(CubeQuery.TopRank topRank) {
        return String.format(
                "Set OrderedL1 AS Order([%s].[%s].[%s].members,[Measures].[%s],BDESC) Member [Measures].[top_rank] AS Rank([%s].[%s].CurrentMember, OrderedL1)",
                topRank.level().getDimensionName(),
                topRank.level().getHierarchyName(),
                topRank.level().getLevelName(),
                topRank.metric(),
                topRank.level().getDimensionName(),
                topRank.level().getHierarchyName());
    }

    private static String partitioningCalculatedMeasureMdx(CubeQuery.Partitioning partitioning) {
        return String.format(
                "Member [Measures].[%s] AS ([%s].[%s].CurrentMember.Parent, [Measures].[%s])",
                partitioning.newMetric(),
                partitioning.level().getDimensionName(),
                partitioning.level().getHierarchyName(),
                partitioning.metric());
    }

    private boolean isDateFilter(CubeQuery.Filter filter, String cube) {
        return dateLevelsForFiltering.get(cube).contains(filter.level());
    }

    private boolean isNotDateFilter(CubeQuery.Filter filter, String cube) {
        return !isDateFilter(filter, cube);
    }
}
