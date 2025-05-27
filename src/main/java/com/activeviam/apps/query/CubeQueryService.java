/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.activepivot.core.intf.api.cube.hierarchy.IHierarchy.ALLMEMBER;
import static com.activeviam.activepivot.server.json.api.dataexport.IJsonOutputConfiguration.FORMAT_PROPERTY;
import static com.activeviam.apps.query.conditions.FilterExpressionConditionVisitor.parseFilterExpression;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.activeviam.activepivot.core.impl.internal.context.filter.QueryBasedCubeRestriction;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.IMultiVersionActivePivot;
import com.activeviam.activepivot.core.intf.api.cube.metadata.HierarchyIdentifier;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.internal.context.filter.ICubeRestriction;
import com.activeviam.activepivot.core.intf.internal.context.filter.IQueryBasedCubeRestriction;
import com.activeviam.activepivot.server.intf.api.dataexport.IDataExportService;
import com.activeviam.activepivot.server.json.api.dataexport.JsonCsvPivotTableOutputConfiguration;
import com.activeviam.activepivot.server.json.api.dataexport.JsonDataExportOrder;
import com.activeviam.activepivot.server.json.api.query.JsonMdxQuery;
import com.activeviam.apps.query.conditions.AndLogicalCondition;
import com.activeviam.apps.query.conditions.InLogicalCondition;
import com.activeviam.apps.query.conditions.LogicalCondition;
import com.activeviam.apps.query.conditions.NotLogicalCondition;
import com.activeviam.apps.query.conditions.OrLogicalCondition;
import com.activeviam.apps.query.rest.CubeQueryDTO;
import com.activeviam.tech.core.api.exceptions.ActiveViamRuntimeException;

import lombok.RequiredArgsConstructor;

@Service
public class CubeQueryService {

    private final Map<String, CubeQuerier> cubeQueriers = new HashMap<>();
    private final String defaultCube;

    public CubeQueryService(
            IDataExportService dataExportService,
            IActivePivotManager activePivotManager,
            CubeQueryProperties cubeQueryProperties) {
        defaultCube = cubeQueryProperties.getDefaultCube();
        activePivotManager.getActivePivots().forEach((cube, pivot) -> {
            var defaults = cubeQueryProperties.getCubeConfiguration().get(cube);
            var levelsConverter = new SingleDimensionLevelsConverter(defaults.getDefaultDimension());
            var dateFilterLevels = defaults.getDateFilterLevels().stream()
                    .map(levelsConverter::stringToLevelIdentifier)
                    .collect(Collectors.toSet());
            cubeQueriers.put(cube, new CubeQuerier(cube, levelsConverter, dateFilterLevels, pivot, dataExportService));
        });
    }

    public CubeQuerier getCubeQuerier(String cube) {
        return Optional.ofNullable(cubeQueriers.get(cube))
                .orElseThrow(() -> new ActiveViamRuntimeException("Cube not found: " + cube));
    }

    public CubeQuerier getDefaultCubeQuerier() {
        return getCubeQuerier(defaultCube);
    }

    @RequiredArgsConstructor
    public static class CubeQuerier {
        private final String cube;
        private final LevelsConverter levelsConverter;
        private final Set<LevelIdentifier> dateLevelsForFiltering;
        private final IMultiVersionActivePivot activePivot;
        private final IDataExportService dataExportService;

        Set<HierarchyIdentifier> slicingHierarchies;

        public String buildMdxQuery(CubeQueryDTO dto) {
            var cubeQuery = CubeQuery.fromDTO(dto, levelsConverter);
            return buildMdxQuery(cubeQuery);
        }

        public StreamingResponseBody runQuery(CubeQueryDTO dto) {
            Map<String, Object> config = Map.of(FORMAT_PROPERTY, JsonCsvPivotTableOutputConfiguration.PLUGIN_KEY);
            var cubeQuery = CubeQuery.fromDTO(dto, levelsConverter);
            var dataExportOrder = new JsonDataExportOrder(
                    new JsonMdxQuery(buildMdxQuery(cubeQuery), buildQueryContext(cubeQuery)), config);
            var context = activePivot.getContext();
            var snapshot = context.getAllUser();
            try {
                var cubeRestriction = QueryBasedCubeRestriction.create(
                        filterExpressionToCubeRestrictions(cubeQuery.getFiltersExpression()));
                context.set(IQueryBasedCubeRestriction.class, cubeRestriction);
                return dataExportService.streamMdxQuery(dataExportOrder);
            } finally {
                activePivot.getContext().restore(snapshot);
            }
        }

        private Map<String, String> buildQueryContext(CubeQuery cubeQuery) {
            var context = new HashMap<String, String>();
            var hiddenLevels = cubeQuery.getLevels().stream()
                    .map(CubeQuerier::levelToMdxPath)
                    .collect(Collectors.joining(","));
            context.put("mdx.hiddensubtotals", hiddenLevels);
            cubeQuery
                    .getMetrics()
                    .forEach(metric -> context.put(
                            String.format("mdx.formatters.[Measures].[%s]", metric.getMetricName()),
                            "DOUBLE[####.##]"));
            return context;
        }

        private String buildMdxQuery(CubeQuery cubeQuery) {
            var query = new StringBuilder();
            var sortBy = cubeQuery.getSortBy();
            var sortByCalculatedMember = ObjectUtils.isEmpty(sortBy)
                    ? null
                    : sortBy.stream()
                            .filter(CubeQuerier::sortRequiresCalculatedMember)
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
                            .map(CubeQuerier::sortingCalculatedMeasure)
                            .collect(Collectors.joining(",")));
                    query.append(System.lineSeparator());
                }

                // Partitioning
                if (!ObjectUtils.isEmpty(partitionedBy)) {
                    query.append(partitionedBy.stream()
                            .map(CubeQuerier::partitioningCalculatedMeasureMdx)
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
            query.append("SELECT");
            query.append(System.lineSeparator());
            // Levels and top rank
            if (!ObjectUtils.isEmpty(levels)) {
                query.append(hierarchizedLevelsWithTopRank(
                        levels,
                        topRank,
                        levels.stream()
                                .map(level -> sortedHierarchizedLevels(
                                        level, levelSorting(level, sortBy), isSlicingHierarchy(level.getHierarchy())))
                                .collect(Collectors.joining(","))));
                query.append(System.lineSeparator());
            }

            // Metrics
            var metrics = cubeQuery.getMetrics();
            if (!ObjectUtils.isEmpty(metrics)) {
                query.append(String.format(
                        "{%s} ON COLUMNS",
                        metrics.stream().map(CubeQuerier::metricToMdx).collect(Collectors.joining(","))));
                query.append(System.lineSeparator());
            }

            // Filters
            var allFilters = cubeQuery.getFilters();
            var otherFilters = cubeQuery.getFilters().stream()
                    .filter(this::isNotDateFilter)
                    .toList();
            var dateFilters =
                    cubeQuery.getFilters().stream().filter(this::isDateFilter).toList();
            var dateLevels = cubeQuery.getLevels().stream()
                    .filter(dateLevelsForFiltering::contains)
                    .toList();

            if (!ObjectUtils.isEmpty(otherFilters)) {
                // Top count
                if (!ObjectUtils.isEmpty(cubeQuery.getTopCounts()) && !ObjectUtils.isEmpty(levels)) {
                    query.append(topCountToMdx(cubeQuery.getTopCounts()));
                    query.append(System.lineSeparator());
                }
                // all filters are the same
                if (!dateLevels.isEmpty()) {
                    addSubSelectFilters(query, cube, allFilters, Collections.emptyList());
                } else {
                    // Add WHERE as of date to every sub select
                    addSubSelectFilters(query, cube, otherFilters, dateFilters);
                }
            } else {
                // FROM CUBE
                query.append(fromCubeWithDateFilterMdx(cube, dateFilters));
            }
            return query.toString();
        }

        private static void addSubSelectFilters(
                StringBuilder query,
                String cube,
                List<CubeQuery.Filter> allFilters,
                List<CubeQuery.Filter> dateFilters) {
            allFilters.forEach(filter -> {
                query.append(filterToMdx(filter));
                query.append(System.lineSeparator());
                query.append(fromCube(cube));
                dateFilters.stream().findFirst().ifPresent(dateFilter -> {
                    query.append(System.lineSeparator());
                    query.append(where(dateFilter));
                });
                query.append(System.lineSeparator());
            });
            // Close the subselects
            allFilters.forEach(filter -> query.append(")"));
        }

        private static String fromCube(String cube) {
            return String.format("FROM [%s]", cube);
        }

        private static String where(CubeQuery.Filter filter) {
            return String.format(
                    "WHERE %s.[%s]",
                    levelToMdxPath(filter.level()), filter.values().getFirst());
        }

        private String fromCubeWithDateFilterMdx(String cube, List<CubeQuery.Filter> dateFilters) {
            var fromCube = new StringBuilder();
            // Date filter
            dateFilters.stream()
                    .findFirst()
                    .ifPresentOrElse(
                            dateFilter -> {
                                fromCube.append("FROM ( SELECT ");
                                fromCube.append(System.lineSeparator());
                                fromCube.append(dateFilterToMdx(dateFilter));
                                fromCube.append(fromCube(cube));
                                fromCube.append(")");
                            },
                            () -> fromCube.append(fromCube(cube)));
            return fromCube.toString();
        }

        private boolean isSlicingHierarchy(HierarchyIdentifier hierarchyIdentifier) {
            // Cache
            if (Objects.isNull(slicingHierarchies)) {
                slicingHierarchies = activePivot.getDescription().getAxisDimensions().getValues().stream()
                        .flatMap(dimension -> dimension.getHierarchies().stream())
                        .filter(hierarchy -> !hierarchy.isAllMembersEnabled())
                        .map(hierarchy -> levelsConverter.stringToHierarchyIdentifier(hierarchy.getName()))
                        .collect(Collectors.toSet());
            }
            return slicingHierarchies.contains(hierarchyIdentifier);
        }

        private static Optional<CubeQuery.Sort> levelSorting(
                LevelIdentifier levelIdentifier, List<CubeQuery.Sort> sortByDefinition) {
            return sortByDefinition.stream()
                    .filter(sort -> sort.level().equals(levelIdentifier))
                    .findAny();
        }

        private static String sortedHierarchizedLevels(
                LevelIdentifier level, Optional<CubeQuery.Sort> sort, boolean isSlicingHierarchy) {
            var levelMembers = isSlicingHierarchy ? members(level) : hierarchizedDescendants(level);
            return sort.map(s -> String.format("Order(%s, %s, %s)", levelMembers, sortToMdx(s), s.sortType()))
                    .orElse(levelMembers);
        }

        private static String members(LevelIdentifier level) {
            return String.format("%s.Members", levelToMdxPath(level));
        }

        private static String hierarchizedDescendants(LevelIdentifier level) {
            return String.format(
                    "Hierarchize(Descendants({[%s].[%s].[ALL].[AllMember]},1,SELF_AND_BEFORE))",
                    level.getDimensionName(), level.getHierarchyName());
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

        private static String levelToMdxPath(LevelIdentifier levelIdentifier) {
            return String.format(
                    "[%s].[%s].[%s]",
                    levelIdentifier.getDimensionName(),
                    levelIdentifier.getHierarchyName(),
                    levelIdentifier.getLevelName());
        }

        private static String metricToMdx(CubeQuery.Metric metric) {
            return String.format("[Measures].[%s]", metric.getMetricName());
        }

        private static String filterToMdx(CubeQuery.Filter filter) {
            var membersList = String.format(
                    "{%s}",
                    filter.values().stream()
                            .map(value -> String.format("%s.[%s]", levelToMdxPath(filter.level()), value))
                            .collect(Collectors.joining(",")));
            return String.format(
                    " FROM (SELECT %s ON COLUMNS",
                    filter.exclude()
                            ? String.format(
                                    "Except (%s,{%s})", levelToMdxPath(filter.level()) + ".Members", membersList)
                            : membersList);
        }

        private static String dateFilterToMdx(CubeQuery.Filter filter) {
            if (filter.values().size() == 1) {
                return String.format(
                        " %s.[%s] ON COLUMNS ",
                        levelToMdxPath(filter.level()), filter.values().getFirst());
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
                    "Set OrderedL1 AS Order(%s.members,[Measures].[%s],BDESC) Member [Measures].[top_rank] AS Rank([%s].[%s].CurrentMember, OrderedL1)",
                    levelToMdxPath(topRank.level()),
                    topRank.metric(),
                    topRank.level().getDimensionName(),
                    topRank.level().getHierarchyName());
        }

        //    private static String topRankOthersCalculatedMeasureMdx(CubeQuery.TopRank topRank) {
        //        return String.format(
        //                "Member %s AS "Others" Order([%s].[%s].[%s].members,[Measures].[%s],BDESC) Member
        // [Measures].[top_rank] AS Rank([%s].[%s].CurrentMember, OrderedL1)",
        //                topRank.level().getDimensionName(),
        //                topRank.level().getHierarchyName(),
        //                topRank.level().getLevelName(),
        //                topRank.metric(),
        //                topRank.level().getDimensionName(),
        //                topRank.level().getHierarchyName());
        //    }

        private static String partitioningCalculatedMeasureMdx(CubeQuery.Partitioning partitioning) {
            return String.format(
                    "Member [Measures].[%s] AS ([%s].[%s].CurrentMember.Parent, [Measures].[%s])",
                    partitioning.newMetric(),
                    partitioning.level().getDimensionName(),
                    partitioning.level().getHierarchyName(),
                    partitioning.metric());
        }

        private boolean isDateFilter(CubeQuery.Filter filter) {
            return dateLevelsForFiltering.contains(filter.level());
        }

        private boolean isNotDateFilter(CubeQuery.Filter filter) {
            return !isDateFilter(filter);
        }

        private ICubeRestriction filterExpressionToCubeRestrictions(String filterExpression) {
            return Optional.ofNullable(filterExpression)
                    .map(f -> convertQueryConditionToCubeRestriction(parseFilterExpression(f)))
                    .orElse(ICubeRestriction.TRUE_INSTANCE);
        }

        // Recursively build the cube restriction object
        private ICubeRestriction convertQueryConditionToCubeRestriction(LogicalCondition queryCondition) {
            return switch (queryCondition) {
                case AndLogicalCondition andLogicalCondition ->
                    ICubeRestriction.and(toListOfRestrictions(andLogicalCondition.getSubConditions()));
                case OrLogicalCondition orLogicalCondition ->
                    ICubeRestriction.or(toListOfRestrictions(orLogicalCondition.getSubConditions()));
                case NotLogicalCondition notLogicalCondition ->
                    ICubeRestriction.not(convertQueryConditionToCubeRestriction(notLogicalCondition.getCondition()));
                case InLogicalCondition<?> inLogicalCondition ->
                    ICubeRestriction.inPath(
                            levelsConverter.stringToHierarchyIdentifier(inLogicalCondition.getField()),
                            inPathValues(inLogicalCondition.getField(), inLogicalCondition.getValues()));
                default -> ICubeRestriction.TRUE_INSTANCE;
            };
        }

        private List<ICubeRestriction> toListOfRestrictions(Collection<LogicalCondition> conditions) {
            return conditions.stream()
                    .map(this::convertQueryConditionToCubeRestriction)
                    .toList();
        }

        private Object[] inPathValues(String hierarchy, Collection<?> values){
            if (isSlicingHierarchy(levelsConverter.stringToHierarchyIdentifier(hierarchy))) {
                return new Object[]{values};
            }
            else {
                return  new Object[]{ALLMEMBER, values};
            }
        }
    }
}
