/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.activepivot.core.intf.api.cube.hierarchy.IHierarchy.ALLMEMBER;
import static com.activeviam.activepivot.server.json.api.dataexport.IJsonOutputConfiguration.FORMAT_PROPERTY;
import static com.activeviam.apps.constants.CubeConstants.FORMATTER_STRING;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COB_DATE;
import static com.activeviam.apps.query.CubeQuery.BDESC;
import static com.activeviam.apps.query.conditions.TrueLogicalCondition.isTrueCondition;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueryMonitoring;
import com.activeviam.activepivot.core.impl.api.contextvalues.mdx.MdxContext;
import com.activeviam.activepivot.core.impl.api.cube.hierarchy.HierarchiesUtil;
import com.activeviam.activepivot.core.impl.api.experimental.context.filter.QueryBasedCubeRestriction;
import com.activeviam.activepivot.core.impl.api.location.LocationUtil;
import com.activeviam.activepivot.core.impl.api.query.IActivePivotQueryRunner;
import com.activeviam.activepivot.core.impl.internal.context.impl.ContextUtils;
import com.activeviam.activepivot.core.intf.api.contextvalues.IContextValue;
import com.activeviam.activepivot.core.intf.api.contextvalues.mdx.IMdxContext;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.IMultiVersionActivePivot;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IMeasureHierarchy;
import com.activeviam.activepivot.core.intf.api.cube.metadata.HierarchyIdentifier;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.experimental.context.filter.ICubeRestriction;
import com.activeviam.activepivot.core.intf.api.experimental.context.filter.IQueryBasedCubeRestriction;
import com.activeviam.activepivot.server.intf.api.dataexport.IDataExportService;
import com.activeviam.activepivot.server.json.api.dataexport.JsonDataExportOrder;
import com.activeviam.activepivot.server.json.api.query.JsonMdxQuery;
import com.activeviam.apps.query.conditions.AndLogicalCondition;
import com.activeviam.apps.query.conditions.BetweenLogicalCondition;
import com.activeviam.apps.query.conditions.InLogicalCondition;
import com.activeviam.apps.query.conditions.LikeLogicalCondition;
import com.activeviam.apps.query.conditions.LogicalCondition;
import com.activeviam.apps.query.conditions.MeasureCondition;
import com.activeviam.apps.query.conditions.NotLogicalCondition;
import com.activeviam.apps.query.conditions.OrLogicalCondition;
import com.activeviam.apps.query.conditions.TrueLogicalCondition;
import com.activeviam.apps.query.rest.CubeQueryDTO;
import com.activeviam.tech.core.api.exceptions.ActiveViamRuntimeException;
import com.activeviam.tech.core.api.query.QueryException;

import lombok.extern.slf4j.Slf4j;

@Service
public class CubeQueryService {

    static {
        System.setProperty("activeviam.feature.experimental.new_cube_restriction.enabled", "true");
    }

    public static final String TOP_RANK_MEASURE = "top_rank";
    public static final String TOP_RANK_MEMBER = CubeQuerier.metricToMdxMeasure(TOP_RANK_MEASURE);
    public static final String TOP_RANK_SET = "OrderedL1";
    public static final String TOP_N_SET = "TopN";
    public static final String OTHERS_MEMBER = "Others";
    private final Map<String, CubeQuerier> cubeQueriers = new HashMap<>();
    private final String defaultCube;

    public CubeQueryService(
            IDataExportService dataExportService,
            IActivePivotManager activePivotManager,
            CubeQueryProperties cubeQueryProperties) {
        defaultCube = cubeQueryProperties.getDefaultCube();
        activePivotManager.getActivePivots().forEach((cube, pivot) -> {
            var cubeDefaults = cubeQueryProperties.getCubeConfiguration().get(cube);
            cubeQueriers.put(cube, new CubeQuerier(cube, cubeDefaults, FORMATTER_STRING, pivot, dataExportService));
        });
    }

    public CubeQuerier getCubeQuerier(String cube) {
        return Optional.ofNullable(cubeQueriers.get(cube))
                .orElseThrow(() -> new ActiveViamRuntimeException("Cube not found: " + cube));
    }

    public CubeQuerier getDefaultCubeQuerier() {
        return getCubeQuerier(defaultCube);
    }

    @Slf4j
    public static class CubeQuerier {
        private final String cube;
        private final LevelsConverter levelsConverter;
        private final String calculatedMeasuresFormatter;
        private final IMultiVersionActivePivot activePivot;
        private final IDataExportService dataExportService;
        private final LevelIdentifier cobDateLevelIdentifier;
        Set<HierarchyIdentifier> slicingHierarchies;
        Set<String> existingMeasures;
        Map<String, String> formatters = new HashMap<>();

        private CubeQuerier(
                String cube,
                CubeQueryProperties.CubeDefaults cubeDefaults,
                String calculatedMeasuresFormatter,
                IMultiVersionActivePivot activePivot,
                IDataExportService dataExportService) {
            this.cube = cube;
            this.calculatedMeasuresFormatter = calculatedMeasuresFormatter;
            this.activePivot = activePivot;
            this.dataExportService = dataExportService;
            levelsConverter = new SingleDimensionLevelsConverter(cubeDefaults.getDefaultDimension());
            cobDateLevelIdentifier = levelsConverter.stringToLevelIdentifier(COB_DATE);
        }

        private String getFormatter(String metric) {
            return formatters.computeIfAbsent(metric, k -> activePivot
                    .getHead()
                    .getMeasuresProvider()
                    .getMeasure(metric)
                    .getDefaultFormatter());
        }

        public CubeQuery convertCubeQuery(CubeQueryDTO dto) {
            assertIsReady();
            return CubeQuery.fromDTO(dto, levelsConverter, getExistingMeasures());
        }

        private static Map<String, Object> exporterConfig(String outputFormat) {
            var config = new HashMap<String, Object>();
            config.put(FORMAT_PROPERTY, outputFormat);
            return config;
        }

        public StreamingResponseBody runQuery(CubeQuery cubeQuery, String outputFormat) {
            assertIsReady();
            var contextValues = new ArrayList<IContextValue>();
            contextValues.add(buildMdxContext(cubeQuery));
            //            var cobDateFilter = cubeQuery.getQueryFilter().generateCobDateCondition();
            //            if (cobDateFilter instanceof InLogicalCondition<?> cobDateCondition) {
            //                var cobDateLevel = levelsConverter.stringToLevelIdentifier(cobDateCondition.getField());
            //                contextValues.add(CubeFilter.builder()
            //                        .includeMembersWithConditions(
            //
            // levelsConverter.stringToHierarchyIdentifier(cobDateLevel.getHierarchyName()),
            //                                new InCondition(cobDateCondition.getValues()))
            //                        .build());
            //            }
            if (cubeQuery.isUseContext()) {
                var cubeRestrictions = buildCubeRestrictions(cubeQuery);
                contextValues.add(cubeRestrictions);
            }
            if (cubeQuery.isExecutionPlanning()) {
                contextValues.add(new QueryMonitoring()
                        .enableQueryPlanSummary()
                        .enableExecutionPlanningPrint()
                        .enableExecutionTimingPrint());
            } else {
                contextValues.add(new QueryMonitoring().enableQueryPlanSummary());
            }
            var contextSnapshot = ContextUtils.applyContextValues(activePivot.getContext(), contextValues, true);
            var mdx = buildMdxQuery(cubeQuery);
            log.info("Mdx Query: {}", mdx);
            var exporterConfig = exporterConfig(outputFormat);
            var dataExportOrder =
                    new JsonDataExportOrder(new JsonMdxQuery(mdx, Collections.emptyMap()), exporterConfig);
            var output = dataExportService.streamMdxQuery(dataExportOrder);
            ContextUtils.replaceContextValues(activePivot.getContext(), contextSnapshot);
            return output;
        }

        private void applyHideTotals(
                CubeQuery.HideTotals hideTotals, List<LevelIdentifier> allLevels, MdxContext mdxContext) {
            // These context values can only be added to the mdxContext, not as pure MDX!
            if (hideTotals.all() || hideTotals.grandTotal()) {
                mdxContext.setHiddenGrandTotals(new int[] {1, 0});
            }
            if (hideTotals.all()) {
                mdxContext.setHiddenSubtotals(allLevels); // these are all the groupBy
            } else if (!hideTotals.levels().isEmpty()) {
                mdxContext.setHiddenSubtotals(hideTotals.levels()); // these are only
            }
        }

        private boolean isLevelTotalHidden(CubeQuery.HideTotals hideTotals, LevelIdentifier levelIdentifier) {
            return hideTotals.all() || hideTotals.levels().contains(levelIdentifier);
        }

        private void applyFullContext(CubeQuery cubeQuery, MdxContext mdxContext) {
            // Add TopRank Set and Member
            if (!ObjectUtils.isEmpty(cubeQuery.getTopRank())) {
                var topRank = cubeQuery.getTopRank();
                mdxContext.addNamedSet(StartBuilding.namedSet()
                        .withName(TOP_RANK_SET)
                        .withExpression(topRankSetExpression(topRank))
                        .build());
                mdxContext.addCalculatedMember(StartBuilding.calculatedMember()
                        .withName(TOP_RANK_MEMBER)
                        .withExpression(topRankMemberExpression(topRank))
                        .build());
            }
            if (!ObjectUtils.isEmpty(cubeQuery.getPartitionedBy())) {
                cubeQuery
                        .getPartitionedBy()
                        .forEach(p -> mdxContext.addCalculatedMember(StartBuilding.calculatedMember()
                                .withName(metricToMdxMeasure(p.newMetric()))
                                .withExpression(partitioningCalculatedMemberExpression(p))
                                .withFormatString(calculatedMeasuresFormatter)
                                .build()));
            }
            if (!ObjectUtils.isEmpty(cubeQuery.getSortBy())) {
                // Create calculated members for sorting that requires it
                var sortBy = cubeQuery.getSortBy();
                if (sortBy.column() instanceof LevelIdentifier level) {
                    mdxContext.addCalculatedMember(StartBuilding.calculatedMember()
                            .withName(sortingCalculatedMeasure(level))
                            .withExpression(sortingCalculatedMemberExpression(level))
                            .build());
                }
            }
            if (!ObjectUtils.isEmpty(cubeQuery.getTopCount())) {
                var topCounts = cubeQuery.getTopCount();
                mdxContext.addNamedSet(StartBuilding.namedSet()
                        .withName(TOP_N_SET)
                        .withExpression(topNSetExpression(topCounts, cubeQuery.getLevels()))
                        .build());
                // Create the Others member
                if (topCounts.aggregateOthers()) {
                    throw new UnsupportedOperationException("TopN others not supported.");
                    //                    mdxContext.addCalculatedMember(StartBuilding.calculatedMember()
                    //                            .withName(topNOthersMemberName(topCounts))
                    //                            .withExpression(topNOthersMemberExpression(topCounts))
                    //                            .build());
                }
            }
            if (!ObjectUtils.isEmpty(cubeQuery.getMetricDefinitions())) {
                var metricDefinitions = cubeQuery.getMetricDefinitions();
                for (var def : metricDefinitions) {
                    mdxContext.addCalculatedMember(StartBuilding.calculatedMember()
                            .withName(def.name())
                            .withExpression(calculatedMemberExpressionToMdx(def.expression()))
                            .withFormatString(calculatedMeasuresFormatter)
                            .build());
                }
            }
        }

        // Replace the metric names with the mdx metric expression
        private String calculatedMemberExpressionToMdx(String expression) {
            var convertedExpression = expression;
            for (var measure : getExistingMeasures()) {
                convertedExpression = convertedExpression.replace(measure, metricToMdxMeasure(measure));
            }
            return convertedExpression;
        }

        private Set<String> getExistingMeasures() {
            if (ObjectUtils.isEmpty(existingMeasures)) {
                // Fetch the list of existingMeasures
                existingMeasures = Set.of(activePivot
                        .getHead()
                        .getMeasuresProvider()
                        .getAllMeasureNames()
                        .toArray(String[]::new));
            }
            return existingMeasures;
        }

        //        private String getExistingMeasureFormatter() {
        //            if (ObjectUtils.isEmpty(existingMeasures)) {
        //                // Fetch the list of existingMeasures
        //                existingMeasures = Set.of(activePivot
        //                        .getHead()
        //                        .getMeasuresProvider()
        //                        .getAllMeasureNames()
        //                        .toArray(String[]::new));
        //            }
        //            return existingMeasures;
        //        }

        private void applyFormatters(CubeQuery cubeQuery, MdxContext mdxContext) {
            mdxContext.setFormatters(extractAllMetricNames(cubeQuery, false).stream()
                    .collect(Collectors.toMap(CubeQuerier::metricToMdxMeasure, this::getFormatter)));
        }

        IMdxContext buildMdxContext(CubeQuery cubeQuery) {
            var mdxContext = new MdxContext();

            // Just use the default formatter
            // applyFormatters(cubeQuery, mdxContext);

            applyHideTotals(cubeQuery.getHideTotals(), cubeQuery.getLevels(), mdxContext);

            if (cubeQuery.isUseContext()) {
                applyFullContext(cubeQuery, mdxContext);
            }

            return mdxContext;
        }

        private String buildCalculatedMembersMdx(CubeQuery cubeQuery) {
            var query = new StringBuilder();
            var sortBy = cubeQuery.getSortBy();
            LevelIdentifier sortByCalculatedMemberLevel = null;
            if (!ObjectUtils.isEmpty(sortBy) && sortBy.column() instanceof LevelIdentifier level) {
                sortByCalculatedMemberLevel = level;
            }
            var partitionedBy = cubeQuery.getPartitionedBy();
            var topRank = cubeQuery.getTopRank();
            var topCount = cubeQuery.getTopCount();
            var metricDefinitions = cubeQuery.getMetricDefinitions();
            // If any of these conditions is true, we need to add the calculated Members
            if (!ObjectUtils.isEmpty(sortByCalculatedMemberLevel)
                    || !ObjectUtils.isEmpty(partitionedBy)
                    || !ObjectUtils.isEmpty(topRank)
                    || !ObjectUtils.isEmpty(topCount)
                    || !ObjectUtils.isEmpty(metricDefinitions)) {
                query.append("WITH ");
                query.append(System.lineSeparator());

                // Sort
                if (!ObjectUtils.isEmpty(sortByCalculatedMemberLevel)) {
                    query.append(sortingCalculatedMeasureMdx(sortByCalculatedMemberLevel));
                    query.append(System.lineSeparator());
                }

                // Partitioning
                if (!ObjectUtils.isEmpty(partitionedBy)) {
                    query.append(partitionedBy.stream()
                            .map(this::partitioningCalculatedMeasureMdx)
                            .collect(Collectors.joining(",")));
                    query.append(System.lineSeparator());
                }

                // Top Rank
                if (!ObjectUtils.isEmpty(topRank)) {
                    query.append(topRankSetMdx(topRank));
                    query.append(System.lineSeparator());
                    query.append(topRankCalculatedMeasureMdx(topRank));
                }

                // Top N
                if (!ObjectUtils.isEmpty(topCount)) {
                    query.append(topCountSetMdx(topCount, cubeQuery.getLevels()));
                    // Create the Others member
                    if (topCount.aggregateOthers()) {
                        throw new UnsupportedOperationException("TopN others not supported.");
                        //                        query.append(System.lineSeparator());
                        //                        query.append(topCountOthersCalculatedMeasureMdx(topCount));
                    }
                }

                // Calculated measures
                if (!ObjectUtils.isEmpty(metricDefinitions)) {
                    for (var def : metricDefinitions) {
                        query.append(metricDefinitionMdx(def.name(), def.expression()));
                        query.append(System.lineSeparator());
                    }
                }
            }
            return query.toString();
        }

        private String metricDefinitionMdx(String name, String expression) {
            return calculatedMemberMdx(measureToMemberMdx(name), calculatedMemberExpressionToMdx(expression));
        }

        private static String setMdx(String setName, String expression) {
            return String.format("Set %s AS %s", setName, expression);
        }

        private static String topRankSetMdx(CubeQuery.TopRank topRank) {
            return setMdx(TOP_RANK_SET, topRankSetExpression(topRank));
        }

        private String topCountSetMdx(CubeQuery.TopCount topCount, List<LevelIdentifier> allLevels) {
            return setMdx(TOP_N_SET, topNSetExpression(topCount, allLevels));
        }

        private String calculatedMemberMdx(String memberName, String expression) {
            return String.format(
                    "Member %s AS (%s), FORMAT_STRING = \"%s\"", memberName, expression, calculatedMeasuresFormatter);
        }

        private String sortingCalculatedMeasureMdx(LevelIdentifier level) {
            return calculatedMemberMdx(sortingCalculatedMeasure(level), sortingCalculatedMemberExpression(level));
        }

        private String partitioningCalculatedMeasureMdx(CubeQuery.Partitioning partitioning) {
            return calculatedMemberMdx(
                    measureToMemberMdx(partitioning.newMetric()), partitioningCalculatedMemberExpression(partitioning));
        }

        private String topRankCalculatedMeasureMdx(CubeQuery.TopRank topRank) {
            return calculatedMemberMdx(TOP_RANK_MEMBER, topRankMemberExpression(topRank));
        }

        private String topCountOthersCalculatedMeasureMdx(CubeQuery.TopCount topCount) {
            return calculatedMemberMdx(topNOthersMemberName(topCount), topNOthersMemberExpression(topCount));
        }

        IQueryBasedCubeRestriction buildCubeRestrictions(CubeQuery cubeQuery) {
            // We add the date condition just in case....
            return QueryBasedCubeRestriction.create(convertQueryConditionToCubeRestriction(
                    cubeQuery.getQueryFilter().generateOtherCondition(),
                    cubeQuery.getQueryFilter().getFilteredCobDates()));
        }

        private static List<String> extractAllMetricNames(CubeQuery cubeQuery, boolean includeCalculatedMetrics) {
            var allMetrics = new ArrayList<>(cubeQuery.getMetrics());
            // Add calculated members
            if (includeCalculatedMetrics) {
                allMetrics.addAll(
                        Optional.ofNullable(cubeQuery.getPartitionedBy()).orElse(Collections.emptyList()).stream()
                                .map(CubeQuery.Partitioning::newMetric)
                                .toList());
            } else {
                cubeQuery.getMetricDefinitions().stream()
                        .map(CubeQuery.MetricDefinition::name)
                        .forEach(allMetrics::remove);
            }
            return allMetrics;
        }

        public String buildMdxQuery(CubeQuery cubeQuery) {
            assertIsReady();
            var query = new StringBuilder();
            var sortBy = cubeQuery.getSortBy();
            var topRank = cubeQuery.getTopRank();
            var topCount = cubeQuery.getTopCount();
            var levels = cubeQuery.getLevels();
            var fullMdxQuery = !cubeQuery.isUseContext();

            // If there are any calculated members and they are not added to the MDX context, add them
            // with the statement WITH
            if (fullMdxQuery) {
                var calculatedMembers = buildCalculatedMembersMdx(cubeQuery);
                if (!ObjectUtils.isEmpty(calculatedMembers)) {
                    query.append(calculatedMembers);
                }
            }

            query.append("SELECT NON EMPTY");
            query.append(System.lineSeparator());

            // Levels and top rank
            var selectItems = new ArrayList<String>();
            if (!ObjectUtils.isEmpty(levels)) {
                selectItems.add(hierarchizedLevels(levels, sortBy, topRank, topCount, cubeQuery.getHideTotals()));
            }

            // Metrics
            var metrics = extractAllMetricNames(cubeQuery, true);
            if (!ObjectUtils.isEmpty(metrics)) {
                selectItems.add(String.format(
                        "{%s} ON COLUMNS",
                        metrics.stream().map(CubeQuerier::metricToMdxMeasure).collect(Collectors.joining(","))));
            }

            query.append(String.join(System.lineSeparator() + ",", selectItems));

            var cobDateFilter = cubeQuery.getQueryFilter().generateCobDateCondition();
            // If we are not using the fullMdxQuery, then these conditions are in the cube restrictions!
            var otherFilters =
                    fullMdxQuery ? cubeQuery.getQueryFilter().generateOtherCondition() : TrueLogicalCondition.INSTANCE;
            var measureFilters = cubeQuery.getQueryFilter().generateMeasuresCondition();
            query.append(System.lineSeparator());
            // If we need to add a subselect do it here:
            // - there is a cobDate filter OR
            // - there is a measure filter OR
            // - there are other filters and we are adding all filters to the mdx query
            if (!isTrueCondition(cobDateFilter) || !isTrueCondition(measureFilters) || !isTrueCondition(otherFilters)) {
                query.append(subSelectWithFilter(cobDateFilter, otherFilters, measureFilters, cubeQuery.getLevels()));
            } else {
                query.append(fromCube(cube));
            }

            return query.toString();
        }

        private static String fromCube(String cube) {
            return String.format("FROM [%s]", cube);
        }

        private void assertIsReady() {
            var hierarchies = activePivot.getHead().getHierarchies().stream()
                    .filter(hierarchy -> !(hierarchy instanceof IMeasureHierarchy))
                    .toList();
            if (hierarchies.isEmpty()) {
                throw new ActiveViamRuntimeException("Cube has no hierarchies");
            }
        }

        private boolean isSlicingHierarchy(HierarchyIdentifier hierarchyIdentifier) {
            // Cache
            if (Objects.isNull(slicingHierarchies)) {
                var hierarchies = activePivot.getHead().getHierarchies().stream()
                        .filter(hierarchy -> !(hierarchy instanceof IMeasureHierarchy))
                        .toList();
                slicingHierarchies = hierarchies.stream()
                        .filter(HierarchiesUtil::isSlicing)
                        .map(hierarchy -> levelsConverter.stringToHierarchyIdentifier(hierarchy.getName()))
                        .collect(Collectors.toSet());
            }
            return slicingHierarchies.contains(hierarchyIdentifier);
        }

        //        private String topNLevels(CubeQuery.TopCount topCount, LevelIdentifier previousLevel, boolean
        // includeTotal) {
        //            if (includeTotal) {
        //                String topCountTotalExpression;
        //                if (topCount.aggregateOthers()) {
        //                    topCountTotalExpression = topNOthersMemberName(topCount);
        //                } else {
        //                    if (Objects.isNull(previousLevel)) {
        //                        topCountTotalExpression =
        //                                isSlicingHierarchy(topCount.level().getHierarchy())
        //                                        ? levelToMdxAll(topCount.level())
        //                                        : levelToMdxAllMember(topCount.level());
        //                    } else {
        //                        topCountTotalExpression = String.format(
        //                                "{(%s,%s)}",
        //                                levelToCurrentMemberMdx(previousLevel),
        // levelToMdxAllMember(topCount.level()));
        //                    }
        //                }
        //                return String.format("{%s, {%s}}", topCountTotalExpression, TOP_N_SET);
        //            } else {
        //                return TOP_N_SET;
        //            }
        //        }

        private static String sortedHierarchizedLevels(
                LevelIdentifier level, CubeQuery.Sort<?> sort, boolean isSlicingHierarchy) {
            var levelMembers = isSlicingHierarchy ? levelToMdxMembers(level) : hierarchizedDescendantsAllMember(level);
            return Optional.ofNullable(sort)
                    .map(s -> orderMdx(levelMembers, sortingMeasureToMdx(s), s.sortType()))
                    .orElse(levelMembers);
        }

        private static String hierarchizedMembersForFilter(LevelIdentifier level, boolean isSlicingHierarchy) {
            return isSlicingHierarchy ? levelToMdxMembers(level) : hierarchizedDescendantsMembersForFilter(level);
        }

        private static String orderMdx(String levelMembers, String measure, String sortType) {
            return String.format("Order(%s, %s, %s)", levelMembers, measure, sortType);
        }

        private static String levelToMdxMembers(LevelIdentifier level) {
            return String.format("%s.Members", levelToMdxPath(level));
        }

        private static String hierarchizedDescendantsAllMember(LevelIdentifier level) {
            return String.format("Hierarchize(Descendants({%s},1,SELF_AND_BEFORE))", levelToMdxAllMember(level));
        }

        private static String hierarchizedDescendantsMembersForFilter(LevelIdentifier level) {
            return String.format("Hierarchize(Descendants({%s}))", levelToMdxMembers(level));
        }

        private String hierarchizedLevels(
                List<LevelIdentifier> levels,
                CubeQuery.Sort<?> sortByDefinition,
                CubeQuery.TopRank topRankDefinition,
                CubeQuery.TopCount topCount,
                CubeQuery.HideTotals hideTotals) {
            if (Objects.nonNull(topCount) && !topCount.groupBy().isEmpty()) {
                throw new UnsupportedOperationException("TopN groupBy not supported.");
            }
            var hierarchizeTemplate = new StringBuilder();
            var crossJoinOrNot = crossJoinOrNot(levels);
            // Sort by topRank
            if (Objects.nonNull(topRankDefinition)) {
                hierarchizeTemplate.append(orderMdx(crossJoinOrNot, TOP_RANK_MEMBER, "BDESC"));
            } else {
                hierarchizeTemplate.append(crossJoinOrNot);
            }
            var lastLevel = Objects.nonNull(topCount) ? levels.getLast() : null;
            var hierarchized = String.format(
                    hierarchizeTemplate.toString(),
                    levels.stream()
                            .map(level -> {
                                // If we are doing TopN, we replace the last level with the TopN set
                                if (Objects.nonNull(lastLevel) && level.equals(lastLevel)) {
                                    return TOP_N_SET;
                                }
                                return isSlicingHierarchy(level.getHierarchy())
                                        ? levelToMdxMembers(level)
                                        : hierarchizedDescendantsAllMember(level);
                            })
                            .collect(Collectors.joining(",")));
            // IF there is no SortBy and we do a Top Count, we need to apply sorting on the measure used for the
            // topcount
            if (Objects.isNull(sortByDefinition)
                    && Objects.nonNull(topCount)
                    && excludeCobDateFilter(levels).size() > 1) {
                sortByDefinition = new CubeQuery.Sort<>(topCount.metric(), BDESC);
            }
            if (Objects.nonNull(sortByDefinition)) {
                // Wrap everything in the Order, ignore TopCount
                hierarchized =
                        orderMdx(hierarchized, sortingMeasureToMdx(sortByDefinition), sortByDefinition.sortType());
            }
            return hierarchized + " ON ROWS";
        }

        private static String crossJoinOrNot(Collection<LevelIdentifier> levels) {
            return levels.size() == 1 ? "%s" : "Crossjoin(%s)";
        }

        static String levelToMdxPath(LevelIdentifier levelIdentifier) {
            return String.format(
                    "[%s].[%s].[%s]",
                    levelIdentifier.getDimensionName(),
                    levelIdentifier.getHierarchyName(),
                    levelIdentifier.getLevelName());
        }

        private static String levelToMdxAll(LevelIdentifier levelIdentifier) {
            return String.format(
                    "[%s].[%s].[ALL]", levelIdentifier.getDimensionName(), levelIdentifier.getHierarchyName());
        }

        private static String levelToMdxAllMember(LevelIdentifier levelIdentifier) {
            return String.format("%s.[AllMember]", levelToMdxAll(levelIdentifier));
        }

        private static String levelToMdxAllMemberChildren(LevelIdentifier levelIdentifier) {
            return String.format("%s.Children", levelToMdxAllMember(levelIdentifier));
        }

        private static String metricToMdxMeasure(String metric) {
            return String.format("[Measures].[%s]", metric);
        }

        private static String sortingCalculatedMeasure(LevelIdentifier level) {
            return metricToMdxMeasure(String.format("%s_sorting", level.getLevelName()));
        }

        private static String sortingMeasureToMdx(CubeQuery.Sort<?> sort) {
            if (sort.column() instanceof String metric) {
                return metricToMdxMeasure(metric);
            } else if (sort.column() instanceof LevelIdentifier level) {
                return sortingCalculatedMeasure(level);
            }
            throw new ActiveViamRuntimeException("Invalid sort value: " + sort.column());
        }

        private static String sortingCalculatedMemberExpression(LevelIdentifier level) {
            return String.format("%s", levelToCurrentMemberValue(level));
        }

        private static String levelToCurrentMemberValue(LevelIdentifier level) {
            return String.format("%s.MemberValue", levelToCurrentMemberMdx(level));
        }

        private static String topRankSetExpression(CubeQuery.TopRank topRank) {
            return orderMdx(levelToMdxMembers(topRank.level()), metricToMdxMeasure(topRank.metric()), "BDESC");
        }

        private static String topRankMemberExpression(CubeQuery.TopRank topRank) {
            return String.format("Rank(%s, %s)", levelToCurrentMemberMdx(topRank.level()), TOP_RANK_SET);
        }

        private static String partitioningCalculatedMemberExpression(CubeQuery.Partitioning partitioning) {
            return String.format(
                    "(%s.Parent, %s)",
                    levelToCurrentMemberMdx(partitioning.level()), metricToMdxMeasure(partitioning.metric()));
        }

        private static String levelToCurrentMemberMdx(LevelIdentifier level) {
            return String.format("[%s].[%s].CurrentMember", level.getDimensionName(), level.getHierarchyName());
        }

        private static String levelToMemberValueMdx(LevelIdentifier level, Object member) {
            return String.format(
                    "[%s].[%s].[%s].[%s]",
                    level.getDimensionName(), level.getHierarchyName(), level.getLevelName(), member);
        }

        private static String measureToMemberMdx(String measure) {
            return String.format("[Measures].[%s]", measure);
        }

        private String topNSetExpression(CubeQuery.TopCount topCount, List<LevelIdentifier> allLevels) {
            if (topCount.groupBy().isEmpty()) {
                return topNWithoutGroupingExpression(topCount, allLevels);
            }
            throw new UnsupportedOperationException("TopN with grouping level not yet supported.");
            //            else if (topCount.groupBy().size() == 1) {
            //                return topNWithSingleGroupByLevelExpression(topCount);
            //            }
            //            else {
            //                return topNWithMultipleGroupByLevelsExpression(topCount,allLevels);
            //            }
        }

        private String topNWithSingleGroupByLevelExpression(CubeQuery.TopCount topCount) {
            return null;
            //            return String.format(
            //                    "%sCount(%s, %d, %s)",
            //                    topCount.bottom() ? "Bottom" : "Top",
            //                    levelToMdxMembers(topCount.level()),
            //                    topCount.count(),
            //                    metricToMdxMeasure(topCount.metric()));
        }

        private String topNWithMultipleGroupByLevelsExpression(
                CubeQuery.TopCount topCount, List<LevelIdentifier> allLevels) {
            return null;
        }

        //        TopCount(
        //                Filter(
        //                        [Trade Attributes].[Trade Name].Levels(
        //                        1
        //        ).Members,
        //        NOT IsEmpty(
        //          [Measures].[Delta Notional.Sum]
        //        )
        //      ),
        //              5,
        //              [Measures].[Delta Notional.Sum]
        //                )

        private String topNWithoutGroupingExpression(CubeQuery.TopCount topCount, List<LevelIdentifier> allLevels) {
            // FIXME: add totals?
            // Take the CobDate level out
            // var levelsExcludingCobDate = excludeCobDateFilter(allLevels);
            return String.format(
                    "%sCount(Filter(%s,NOT IsEmpty(%s)),%d,%s)",
                    topCount.bottom() ? "Bottom" : "Top",
                    levelToMdxMembers(allLevels.getLast()),
                    metricToMdxMeasure(topCount.metric()),
                    topCount.count(),
                    metricToMdxMeasure(topCount.metric()));
            //            return String.format(
            //                    "%sCount(NonEmpty(%s),%d,%s)",
            //                    topCount.bottom() ? "Bottom" : "Top",
            //                    crossJoinWithStar(allLevels),
            //                    topCount.count(),
            //                    metricToMdxMeasure(topCount.metric()));
        }

        private List<LevelIdentifier> excludeCobDateFilter(List<LevelIdentifier> allLevels) {
            return allLevels.stream().filter(l -> !CubeQuery.isCobDateLevel(l)).toList();
        }

        private static String topNOthersMemberExpression(CubeQuery.TopCount topCount) {
            return null;
            // return String.format("Aggregate(%s - [%s])", levelToMdxMembers(topCount.level()), TOP_N_SET);
        }

        private String topNOthersMemberName(CubeQuery.TopCount topCount) {
            return null;
            //            var level = topCount.level();
            //            var levelMembers =
            //                    isSlicingHierarchy(level.getHierarchy()) ? levelToMdxAll(level) :
            // levelToMdxAllMember(level);
            //            return String.format("%s.[%s]", levelMembers, OTHERS_MEMBER);
        }

        // Recursively build the cube restriction object
        private ICubeRestriction convertQueryConditionToCubeRestriction(
                LogicalCondition queryCondition, Collection<LocalDate> dates) {
            return switch (queryCondition) {
                case AndLogicalCondition andLogicalCondition ->
                    ICubeRestriction.and(toListOfRestrictions(andLogicalCondition.getSubConditions(), dates));
                case OrLogicalCondition orLogicalCondition ->
                    ICubeRestriction.or(toListOfRestrictions(orLogicalCondition.getSubConditions(), dates));
                case NotLogicalCondition notLogicalCondition ->
                    ICubeRestriction.not(
                            convertQueryConditionToCubeRestriction(notLogicalCondition.getCondition(), dates));
                case InLogicalCondition<?> inLogicalCondition -> {
                    var level = levelsConverter.stringToLevelIdentifier(inLogicalCondition.getField());
                    yield ICubeRestriction.inPath(
                            level.getHierarchy(), inPathValues(level, inLogicalCondition.getValues()));
                }
                case LikeLogicalCondition likeCondition -> {
                    var level = levelsConverter.stringToLevelIdentifier(likeCondition.getField());
                    var allMembers = getMembersForLevel(level, dates);
                    var pattern = Pattern.compile(likeCondition.getMatchingCriteria());
                    var valuesToFilter = allMembers.stream()
                            .filter(m -> pattern.matcher((String) m).find())
                            .toList();
                    if (valuesToFilter.isEmpty()) {
                        yield ICubeRestriction.falseRestriction();
                    }
                    yield ICubeRestriction.inPath(level.getHierarchy(), inPathValues(level, valuesToFilter));
                }
                case BetweenLogicalCondition<?> betweenLogicalCondition -> {
                    var left = betweenLogicalCondition.getLeft();
                    var right = betweenLogicalCondition.getRight();
                    var leftInclusive = betweenLogicalCondition.isLeftInclusive();
                    var rightInclusive = betweenLogicalCondition.isRightInclusive();
                    var level = levelsConverter.stringToLevelIdentifier(betweenLogicalCondition.getField());
                    var allMembers = getMembersForLevel(level, dates);
                    if (Objects.isNull(left) && Objects.isNull(right)) {
                        throw new ActiveViamRuntimeException("Left and right must not be null");
                    }
                    var membersStream = allMembers.stream();
                    if (Objects.nonNull(left)) {
                        membersStream = membersStream.filter(m -> {
                            var x = compareObjects(m, left);
                            return leftInclusive ? x >= 0 : x > 0;
                        });
                    }
                    if (Objects.nonNull(right)) {
                        membersStream = membersStream.filter(m -> {
                            var x = compareObjects(m, right);
                            return rightInclusive ? x <= 0 : x < 0;
                        });
                    }
                    var valuesToFilter = membersStream.toList();
                    if (valuesToFilter.isEmpty()) {
                        yield ICubeRestriction.falseRestriction();
                    }
                    yield ICubeRestriction.inPath(level.getHierarchy(), inPathValues(level, valuesToFilter));
                }
                case MeasureCondition measureCondition ->
                    throw new UnsupportedOperationException(
                            MeasureCondition.class.getSimpleName() + " not supported in CubeRestrictions");
                default -> ICubeRestriction.trueRestriction();
            };
        }

        private static int compareObjects(Object o1, Object o2) {
            if (o1 instanceof LocalDate date1 && o2 instanceof LocalDate date2) {
                return date1.compareTo(date2);
            }
            if (o1 instanceof String string1 && o2 instanceof String string2) {
                return string1.compareTo(string2);
            }
            if (o1 instanceof Integer int1 && o2 instanceof Integer int2) {
                return int1.compareTo(int2);
            }
            if (o1 instanceof Long long1 && o2 instanceof Long long2) {
                return long1.compareTo(long2);
            }
            if (o1 instanceof Double double1 && o2 instanceof Double double2) {
                return double1.compareTo(double2);
            }
            if (o1 instanceof Float float1 && o2 instanceof Float float2) {
                return float1.compareTo(float2);
            }
            throw new UnsupportedOperationException(
                    "DataType of " + o1.getClass().getSimpleName() + " not supported or types not matching");
        }

        private Collection<Object> getMembersForLevel(LevelIdentifier level, Collection<LocalDate> dates) {
            var gaq = dates.isEmpty()
                    ? IActivePivotQueryRunner.create()
                            .withWildcardCoordinate(level)
                            .forMeasures(IMeasureHierarchy.COUNT_ID)
                    : IActivePivotQueryRunner.create()
                            .withCoordinate(cobDateLevelIdentifier, dates)
                            .withWildcardCoordinate(level)
                            .forMeasures(IMeasureHierarchy.COUNT_ID);
            var apVersion = activePivot.getHead();
            try {
                var values = new HashSet<Object>();
                var result = gaq.run(apVersion);
                var levelInfo = HierarchiesUtil.getLevel(apVersion, level);
                result.forEachCell((location, measure, value) -> {
                    values.add(LocationUtil.getCoordinate(location, levelInfo));
                    return true;
                });
                return values;
            } catch (QueryException e) {
                throw new ActiveViamRuntimeException(e);
            }
        }

        private String subSelectWithMeasureFilter(
                LogicalCondition measureFilterCondition,
                LogicalCondition cobDateCondition,
                List<LevelIdentifier> allLevels,
                String subSelect) {
            var measureSubSelectData = convertQueryConditionToMdxSubSelectData(measureFilterCondition);
            if (ObjectUtils.isEmpty(measureSubSelectData)) {
                return subSelect;
            }

            // FIXME alternatively we need to provide a level with the measure filter
            // like we did previously (e.g. Measure AT level < 1000)
            var measureFilterLevel = allLevels.getLast();
            return String.format(
                    "FROM (SELECT FILTER(%s,%s) ON COLUMNS %s)",
                    levelToMdxMembers(measureFilterLevel), measureSubSelectData.filterExpression(), subSelect);
        }

        private String crossJoinWithStar(List<LevelIdentifier> levels) {
            return levels.stream()
                    .map(l -> isSlicingHierarchy(l.getHierarchy()) ? levelToMdxPath(l) : levelToMdxAllMemberChildren(l))
                    .collect(Collectors.joining("*"));
        }

        private String subSelectWithFilter(
                LogicalCondition cobDateCondition,
                LogicalCondition otherConditions,
                LogicalCondition measureFilterCondition,
                List<LevelIdentifier> allLevels) {
            var dateSubselect = isTrueCondition(cobDateCondition)
                    ? fromCube(cube)
                    : String.format(
                            "FROM (SELECT %s ON COLUMNS %s)",
                            inConditionOnSlicingHierarchyToMdx((InLogicalCondition<LocalDate>) cobDateCondition),
                            fromCube(cube));
            var subSelectData = convertQueryConditionToMdxSubSelectData(otherConditions);
            if (ObjectUtils.isEmpty(subSelectData)) {
                return subSelectWithMeasureFilter(measureFilterCondition, cobDateCondition, allLevels, dateSubselect);
            }

            var levels = new HashSet<>(subSelectData.levels());
            // Add a default level to the crossjoin
            if (levels.isEmpty()) {
                levels.add(allLevels.getLast());
            }
            // FIXME: if we dont have any groupBy?
            var crossJoin = String.format(
                    crossJoinOrNot(levels),
                    levels.stream()
                            .map(l -> hierarchizedMembersForFilter(l, isSlicingHierarchy(l.getHierarchy())))
                            .collect(Collectors.joining(",")));
            var subSelect = String.format(
                    "FROM (SELECT FILTER(%s,%s) ON COLUMNS %s)",
                    crossJoin, subSelectData.filterExpression(), dateSubselect);
            var subSelectWithMeasure =
                    subSelectWithMeasureFilter(measureFilterCondition, cobDateCondition, allLevels, subSelect);
            return ObjectUtils.isEmpty(subSelectWithMeasure) ? subSelect : subSelectWithMeasure;
        }

        private String inConditionOnSlicingHierarchyToMdx(InLogicalCondition<?> inCondition) {
            return inCondition.getValues().stream()
                    .map(member -> levelToMemberValueMdx(
                            levelsConverter.stringToLevelIdentifier(inCondition.getField()), member))
                    .collect(Collectors.joining(","));
        }

        // Recursively build the cube restriction object
        private MdxSubSelectData convertQueryConditionToMdxSubSelectData(LogicalCondition queryCondition) {
            return switch (queryCondition) {
                case AndLogicalCondition andLogicalCondition -> {
                    var subSelectData = andLogicalCondition.getSubConditions().stream()
                            .map(this::convertQueryConditionToMdxSubSelectData)
                            .toList();
                    yield new MdxSubSelectData(
                            subSelectData.stream()
                                    .map(MdxSubSelectData::filterExpression)
                                    .collect(Collectors.joining(" AND ", "(", ")")),
                            subSelectData.stream()
                                    .map(MdxSubSelectData::levels)
                                    .flatMap(Set::stream)
                                    .collect(Collectors.toSet()));
                }
                case OrLogicalCondition orLogicalCondition -> {
                    var subSelectData = orLogicalCondition.getSubConditions().stream()
                            .map(this::convertQueryConditionToMdxSubSelectData)
                            .toList();
                    yield new MdxSubSelectData(
                            subSelectData.stream()
                                    .map(MdxSubSelectData::filterExpression)
                                    .collect(Collectors.joining(" OR ", "(", ")")),
                            subSelectData.stream()
                                    .map(MdxSubSelectData::levels)
                                    .flatMap(Set::stream)
                                    .collect(Collectors.toSet()));
                }
                case NotLogicalCondition notLogicalCondition -> {
                    var subSelectData = convertQueryConditionToMdxSubSelectData(notLogicalCondition.getCondition());
                    yield new MdxSubSelectData("NOT " + subSelectData.filterExpression(), subSelectData.levels());
                }
                case InLogicalCondition<?> inLogicalCondition -> {
                    var values = inLogicalCondition.getValues();
                    var level = levelsConverter.stringToLevelIdentifier(inLogicalCondition.getField());
                    var mdxLevelValue = levelToCurrentMemberValue(level);
                    yield new MdxSubSelectData(
                            values.stream()
                                    .map(value -> mdxLevelValue + " = " + valueToString(value))
                                    .collect(Collectors.joining(" OR ", "(", ")")),
                            Set.of(level));
                }
                case BetweenLogicalCondition<?> betweenLogicalCondition -> {
                    var left = betweenLogicalCondition.getLeft();
                    var right = betweenLogicalCondition.getRight();
                    var leftInclusive = betweenLogicalCondition.isLeftInclusive();
                    var rightInclusive = betweenLogicalCondition.isRightInclusive();
                    var level = levelsConverter.stringToLevelIdentifier(betweenLogicalCondition.getField());
                    var mdxMemberValue = levelToCurrentMemberValue(level);
                    var subConditions = new ArrayList<String>();
                    if (Objects.isNull(left) && Objects.isNull(right)) {
                        throw new ActiveViamRuntimeException("Left and right must not be null");
                    }
                    if ((Objects.nonNull(left) && left instanceof LocalDate)
                            || (Objects.nonNull(right) && right instanceof LocalDate)) {
                        subConditions.add(String.format("IsDate(%s)", mdxMemberValue));
                    }
                    if (Objects.nonNull(left)) {
                        var compareString = leftInclusive ? ">=" : ">";
                        subConditions.add(
                                String.format("%s %s %s", mdxMemberValue, compareString, valueToString(left)));
                    }
                    if (Objects.nonNull(right)) {
                        var compareString = rightInclusive ? "<=" : "<";
                        subConditions.add(
                                String.format("%s %s %s", mdxMemberValue, compareString, valueToString(right)));
                    }
                    yield new MdxSubSelectData(
                            subConditions.stream().collect(Collectors.joining(" AND ", "(", ")")), Set.of(level));
                }
                case MeasureCondition measureCondition ->
                    new MdxSubSelectData(
                            measureToMemberMdx(measureCondition.getMeasure())
                                    + measureCondition.getOperator().getMdxOperator()
                                    + measureCondition.getOperand(),
                            Collections.emptySet());
                case LikeLogicalCondition likeCondition -> {
                    var level = levelsConverter.stringToLevelIdentifier(likeCondition.getField());
                    var inString = String.format(
                            "InStr(1,%s.MEMBER_CAPTION,\"%s\") > 0",
                            levelToCurrentMemberMdx(level), likeCondition.getMatchingCriteria());
                    yield new MdxSubSelectData(inString, Set.of(level));
                }
                default -> null;
            };
        }

        private static String valueToString(Object value) {
            if (value instanceof LocalDate localDate) {
                return formatLocalDate(localDate);
            } else if (value instanceof Number number) {
                return number.toString();
            } else {
                return String.format("\"%s\"", value.toString());
            }
        }

        private List<ICubeRestriction> toListOfRestrictions(
                Collection<LogicalCondition> conditions, Collection<LocalDate> dateCondition) {
            return conditions.stream()
                    .map(c -> this.convertQueryConditionToCubeRestriction(c, dateCondition))
                    .toList();
        }

        private Object[] inPathValues(LevelIdentifier levelIdentifier, Collection<?> values) {
            if (isSlicingHierarchy(levelIdentifier.getHierarchy())) {
                return new Object[] {values};
            } else {
                return new Object[] {ALLMEMBER, values};
            }
        }

        private static String formatLocalDate(LocalDate localDate) {
            return String.format("CDate(\"%s\")", localDate);
        }
    }

    private record MdxSubSelectData(String filterExpression, Set<LevelIdentifier> levels) {}
}
