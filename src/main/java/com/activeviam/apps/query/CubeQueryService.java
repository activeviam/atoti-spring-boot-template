/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.activepivot.server.json.api.dataexport.IJsonOutputConfiguration.FORMAT_PROPERTY;

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
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.mdx.MdxContext;
import com.activeviam.activepivot.core.impl.api.cube.hierarchy.HierarchiesUtil;
import com.activeviam.activepivot.core.impl.api.experimental.context.filter.QueryBasedCubeRestriction;
import com.activeviam.activepivot.core.impl.internal.context.impl.ContextUtils;
import com.activeviam.activepivot.core.impl.internal.contextvalues.subcube.CubeFilterUtil;
import com.activeviam.activepivot.core.intf.api.contextvalues.IContextValue;
import com.activeviam.activepivot.core.intf.api.contextvalues.mdx.IMdxContext;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.IMultiVersionActivePivot;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IAxisHierarchy;
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
            cubeQueriers.put(
                    cube,
                    new CubeQuerier(
                            cube,
                            cubeDefaults,
                            cubeQueryProperties.getDefaultDoubleFormatter(),
                            pivot,
                            dataExportService));
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
        private final String defaultDoubleFormatter;
        private final IMultiVersionActivePivot activePivot;
        private final IDataExportService dataExportService;
        Set<HierarchyIdentifier> slicingHierarchies;
        Set<String> existingMeasures;

        private CubeQuerier(
                String cube,
                CubeQueryProperties.CubeDefaults cubeDefaults,
                String defaultDoubleFormatter,
                IMultiVersionActivePivot activePivot,
                IDataExportService dataExportService) {
            this.cube = cube;
            this.defaultDoubleFormatter = defaultDoubleFormatter;
            this.activePivot = activePivot;
            this.dataExportService = dataExportService;
            levelsConverter = new SingleDimensionLevelsConverter(cubeDefaults.getDefaultDimension());
        }

        public CubeQuery convertCubeQuery(CubeQueryDTO dto) {
            assertIsReady();
            return CubeQuery.fromDTO(dto, levelsConverter);
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
            if (cubeQuery.isUseContext()) {
                var cubeRestrictions = buildCubeRestrictions(cubeQuery);
                contextValues.add(cubeRestrictions);
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
                mdxContext.setHiddenSubtotals(allLevels); // these are all the levels
            } else if (!hideTotals.levels().isEmpty()) {
                mdxContext.setHiddenSubtotals(hideTotals.levels()); // these are only
            }
        }

        private void applyFullContext(CubeQuery cubeQuery, MdxContext mdxContext) {
            // FIXME: workaround, remove once https://github.com/activeviam/activepivot/pull/12984 is merged
            mdxContext.setLightCrossJoinEnabled(false);
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
                                .withFormatString(defaultDoubleFormatter)
                                .build()));
            }
            if (!ObjectUtils.isEmpty(cubeQuery.getSortBy())) {
                // Create calculated members for sorting that requires it
                cubeQuery.getSortBy().stream()
                        .filter(CubeQuerier::sortRequiresCalculatedMember)
                        .forEach(sort -> mdxContext.addCalculatedMember(StartBuilding.calculatedMember()
                                .withName(sortingCalculatedMeasure(sort))
                                .withExpression(sortingCalculatedMemberExpression(sort))
                                .build()));
            }
            if (!ObjectUtils.isEmpty(cubeQuery.getTopCount())) {
                var topCounts = cubeQuery.getTopCount();
                mdxContext.addNamedSet(StartBuilding.namedSet()
                        .withName(TOP_N_SET)
                        .withExpression(topNSetExpression(topCounts))
                        .build());
                // Create the Others member
                if (topCounts.aggregateOthers()) {
                    mdxContext.addCalculatedMember(StartBuilding.calculatedMember()
                            .withName(topNOthersMemberName(topCounts))
                            .withExpression(topNOthersMemberExpression(topCounts))
                            .build());
                }
            }
            if (!ObjectUtils.isEmpty(cubeQuery.getMetricDefinitions())) {
                var metricDefinitions = cubeQuery.getMetricDefinitions();
                for (var def : metricDefinitions) {
                    mdxContext.addCalculatedMember(StartBuilding.calculatedMember()
                            .withName(def.name())
                            .withExpression(calculatedMemberExpressionToMdx(def.expression()))
                            .withFormatString(defaultDoubleFormatter)
                            .build());
                }
            }
        }

        // Replace the metric names with the mdx metric expression
        private String calculatedMemberExpressionToMdx(String expression) {
            if (ObjectUtils.isEmpty(existingMeasures)) {
                // Fetch the list of existingMeasures
                existingMeasures = Set.of(activePivot
                        .getHead()
                        .getMeasuresProvider()
                        .getAllMeasureNames()
                        .toArray(String[]::new));
            }
            var convertedExpression = expression;
            for (var measure : existingMeasures) {
                convertedExpression = convertedExpression.replace(measure, metricToMdxMeasure(measure));
            }
            return convertedExpression;
        }

        private void applyFormatters(CubeQuery cubeQuery, MdxContext mdxContext) {
            var formatter = String.format("DOUBLE[%s]", defaultDoubleFormatter);
            mdxContext.setFormatters(extractAllMetricNames(cubeQuery, false).stream()
                    .collect(Collectors.toMap(CubeQuerier::metricToMdxMeasure, m -> formatter)));
        }

        IMdxContext buildMdxContext(CubeQuery cubeQuery) {
            var mdxContext = new MdxContext();

            applyFormatters(cubeQuery, mdxContext);

            applyHideTotals(cubeQuery.getHideTotals(), cubeQuery.getLevels(), mdxContext);

            if (cubeQuery.isUseContext()) {
                applyFullContext(cubeQuery, mdxContext);
            }

            return mdxContext;
        }

        private String buildCalculatedMembersMdx(CubeQuery cubeQuery) {
            var query = new StringBuilder();
            var sortBy = cubeQuery.getSortBy();
            var sortByCalculatedMember = ObjectUtils.isEmpty(sortBy)
                    ? null
                    : sortBy.stream()
                            .filter(CubeQuerier::sortRequiresCalculatedMember)
                            .toList();
            var partitionedBy = cubeQuery.getPartitionedBy();
            var topRank = cubeQuery.getTopRank();
            var topCount = cubeQuery.getTopCount();
            var metricDefinitions = cubeQuery.getMetricDefinitions();
            // If any of these conditions is true, we need to add the calculated Members
            if (!ObjectUtils.isEmpty(sortByCalculatedMember)
                    || !ObjectUtils.isEmpty(partitionedBy)
                    || !ObjectUtils.isEmpty(topRank)
                    || !ObjectUtils.isEmpty(topCount)
                    || !ObjectUtils.isEmpty(metricDefinitions)) {
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
                    query.append(topCountSetMdx(topCount));
                    // Create the Others member
                    if (topCount.aggregateOthers()) {
                        query.append(System.lineSeparator());
                        query.append(topCountOthersCalculatedMeasureMdx(topCount));
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

        private static String topCountSetMdx(CubeQuery.TopCount topCount) {
            return setMdx(TOP_N_SET, topNSetExpression(topCount));
        }

        private String calculatedMemberMdx(String memberName, String expression) {
            return String.format(
                    "Member %s AS (%s), FORMAT_STRING = \"%s\"", memberName, expression, defaultDoubleFormatter);
        }

        private String partitioningCalculatedMeasureMdx(CubeQuery.Partitioning partitioning) {
            return calculatedMemberMdx(
                    measureToMemberMdx(partitioning.newMetric()), partitioningCalculatedMemberExpression(partitioning));
        }

        private String topRankCalculatedMeasureMdx(CubeQuery.TopRank topRank) {
            return calculatedMemberMdx(TOP_RANK_MEASURE, topRankMemberExpression(topRank));
        }

        private String topCountOthersCalculatedMeasureMdx(CubeQuery.TopCount topCount) {
            return calculatedMemberMdx(topNOthersMemberName(topCount), topNOthersMemberExpression(topCount));
        }

        IQueryBasedCubeRestriction buildCubeRestrictions(CubeQuery cubeQuery) {
            return QueryBasedCubeRestriction.create(convertQueryConditionToCubeRestriction(cubeQuery.getFilter()));
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
            var sortBys = cubeQuery.getSortBy();
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
            if (!ObjectUtils.isEmpty(levels)) {
                query.append(hierarchizedLevels(levels, topRank, topCount, sortBys));
            }

            // Metrics
            var metrics = extractAllMetricNames(cubeQuery, true);
            if (!ObjectUtils.isEmpty(metrics)) {
                query.append(",");
                query.append(System.lineSeparator());
                query.append(String.format(
                        "{%s} ON COLUMNS",
                        metrics.stream().map(CubeQuerier::metricToMdxMeasure).collect(Collectors.joining(","))));
            } else {
                query.append(System.lineSeparator());
            }

            // If we are using a subselect to add filters, add them here
            if (!(cubeQuery.getFilter() instanceof TrueLogicalCondition) && fullMdxQuery) {
                query.append(subSelectWithFilter(cubeQuery.getFilter(), cubeQuery.getLevels()));
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

        private static Optional<CubeQuery.Sort> levelSorting(
                LevelIdentifier levelIdentifier, List<CubeQuery.Sort> sortByDefinition) {
            return sortByDefinition.stream()
                    .filter(sort -> sort.level().equals(levelIdentifier))
                    .findAny();
        }

        private String topNLevels(CubeQuery.TopCount topCount) {
            return String.format(
                    "{%s, {%s}}",
                    TOP_N_SET,
                    topCount.aggregateOthers()
                            ? topNOthersMemberName(topCount)
                            : (isSlicingHierarchy(topCount.level().getHierarchy())
                                    ? levelToMdxAll(topCount.level())
                                    : levelToMdxAllMember(topCount.level())));
        }

        private static String sortedHierarchizedLevels(
                LevelIdentifier level, Optional<CubeQuery.Sort> sort, boolean isSlicingHierarchy) {
            var levelMembers = isSlicingHierarchy ? levelToMdxMembers(level) : hierarchizedDescendantsAllMember(level);
            return sort.map(s -> orderMdx(levelMembers, sortingMeasureToMdx(s), s.sortType()))
                    .orElse(levelMembers);
        }

        //        private static String hierarchizedMembers(LevelIdentifier level, boolean isSlicingHierarchy) {
        //            return isSlicingHierarchy ? levelToMdxMembers(level) : hierarchizedDescendantsMembers(level);
        //        }

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

        //        private static String hierarchizedDescendantsMembers(LevelIdentifier level) {
        //            return String.format("Hierarchize(Descendants({%s},1,SELF_AND_BEFORE))",
        // levelToMdxMembers(level));
        //        }

        private static String hierarchizedDescendantsMembersForFilter(LevelIdentifier level) {
            return String.format("Hierarchize(Descendants({%s}))", levelToMdxMembers(level));
        }

        private String hierarchizedLevels(
                List<LevelIdentifier> levels,
                CubeQuery.TopRank topRankDefinition,
                CubeQuery.TopCount topCount,
                List<CubeQuery.Sort> sortByDefinitions) {
            var hierarchizeTemplate = new StringBuilder();
            var crossJoinOrNot = levels.size() == 1 ? "%s" : "Crossjoin(%s)";
            // Sort by topRank
            if (Objects.nonNull(topRankDefinition)) {
                hierarchizeTemplate.append(
                        orderMdx(crossJoinOrNot, metricToMdxMeasure(topRankDefinition.metric()), "BDESC"));
            } else {
                hierarchizeTemplate.append(crossJoinOrNot);
            }
            return String.format(
                    hierarchizeTemplate.append(" ON ROWS").toString(),
                    levels.stream()
                            .map(level -> Objects.nonNull(topCount)
                                            && topCount.level().equals(level)
                                    ?
                                    // If level is the TopCount level, we use Top Set
                                    topNLevels(topCount)
                                    : sortedHierarchizedLevels(
                                            level,
                                            levelSorting(level, sortByDefinitions),
                                            isSlicingHierarchy(level.getHierarchy())))
                            .collect(Collectors.joining(",")));
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
            return String.format(
                    "[%s].[%s].[ALL].[AllMember]",
                    levelIdentifier.getDimensionName(), levelIdentifier.getHierarchyName());
        }

        private static String metricToMdxMeasure(String metric) {
            return String.format("[Measures].[%s]", metric);
        }

        private static String sortingCalculatedMeasure(CubeQuery.Sort sort) {
            return metricToMdxMeasure(String.format("%s_sorting", sort.level().getLevelName()));
        }

        private static boolean sortRequiresCalculatedMember(CubeQuery.Sort sort) {
            return sort.metric().equals(sort.level().getLevelName());
        }

        private static String sortingMeasureToMdx(CubeQuery.Sort sort) {
            return sortRequiresCalculatedMember(sort)
                    ? sortingCalculatedMeasure(sort)
                    : metricToMdxMeasure(sort.metric());
        }

        private static String sortingCalculatedMemberExpression(CubeQuery.Sort sort) {
            return levelToCurrentMemberValue(sort.level());
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

        private static String measureToMemberMdx(String measure) {
            return String.format("[Measures].[%s]", measure);
        }

        private static String topNSetExpression(CubeQuery.TopCount topCount) {
            return String.format(
                    "%sCount(%s, %d, %s)",
                    topCount.bottom() ? "Bottom" : "Top",
                    levelToMdxMembers(topCount.level()),
                    topCount.count(),
                    metricToMdxMeasure(topCount.metric()));
        }

        private static String topNOthersMemberExpression(CubeQuery.TopCount topCount) {
            return String.format("Aggregate(%s - [%s])", levelToMdxMembers(topCount.level()), TOP_N_SET);
        }

        private String topNOthersMemberName(CubeQuery.TopCount topCount) {
            var level = topCount.level();
            var levelMembers =
                    isSlicingHierarchy(level.getHierarchy()) ? levelToMdxAll(level) : levelToMdxAllMember(level);
            return String.format("%s.[%s]", levelMembers, OTHERS_MEMBER);
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
                case MeasureCondition measureCondition ->
                    throw new UnsupportedOperationException(MeasureCondition.class.getSimpleName());
                case LikeLogicalCondition likeCondition -> {
                    var allMembers =
                            getMembersForLevel(levelsConverter.stringToLevelIdentifier(likeCondition.getField()));
                    var criteria = likeCondition.getMatchingCriteria();
                    var valuesToFilter = allMembers.stream()
                            .filter(m -> m.contains(criteria))
                            .toList();
                    yield ICubeRestriction.inPath(
                            levelsConverter.stringToHierarchyIdentifier(likeCondition.getField()),
                            inPathValues(likeCondition.getField(), valuesToFilter));
                }
                default -> ICubeRestriction.trueRestriction();
            };
        }

        private Collection<String> getMembersForLevel(LevelIdentifier level) {
            var hierarchy = HierarchiesUtil.getHierarchy(activePivot.getHead(), level.getHierarchy());
            // NOTE: We assume this is a single level hierarchy!
            return Objects.requireNonNull(
                            CubeFilterUtil.getAll(activePivot.getContext()).getSecurityAndFilter())
                    .retrieveMembers((IAxisHierarchy) hierarchy, 1)
                    .stream()
                    .map(m -> (String) m.getDiscriminator())
                    .toList();
        }

        private String subSelectWithFilter(LogicalCondition queryCondition, List<LevelIdentifier> allLevels) {
            var subSelectData = convertQueryConditionToMdxSubSelectData(queryCondition);
            if (ObjectUtils.isEmpty(subSelectData)) {
                return "";
            }
            var levels = new HashSet<>(subSelectData.levels());
            var measureFilterLevels = getMeasureFilterLevels(queryCondition);
            if (!measureFilterLevels.isEmpty()) {
                // Add the required levels for the measure filter
                levels.addAll(measureFilterLevels.stream()
                        .map(levelsConverter::stringToLevelIdentifier)
                        .filter(l -> !levels.contains(l))
                        .toList());
            }
            // Add a default level to the crossjoin
            if (levels.isEmpty()) {
                levels.add(allLevels.getLast());
            }
            // FIXME: if we dont have any levels?

            var crossJoin = String.format(
                    levels.size() == 1 ? "%s" : "Crossjoin(%s)",
                    levels.stream()
                            .map(l -> hierarchizedMembersForFilter(l, isSlicingHierarchy(l.getHierarchy())))
                            .collect(Collectors.joining(",")));

            return String.format(
                    "FROM (SELECT FILTER(%s,%s) ON COLUMNS %s)",
                    crossJoin, subSelectData.filterExpression(), fromCube(cube));
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
                                    .map(value -> mdxLevelValue + " = "
                                            + (value instanceof LocalDate localDate
                                                    ? formatLocalDate(localDate)
                                                    : value.toString()))
                                    .collect(Collectors.joining(" OR ", "(", ")")),
                            Set.of(level));
                }
                case BetweenLogicalCondition<?> betweenDatesLogicalCondition -> {
                    var left = betweenDatesLogicalCondition.getLeft();
                    var right = betweenDatesLogicalCondition.getRight();
                    var level = levelsConverter.stringToLevelIdentifier(betweenDatesLogicalCondition.getField());
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
                        subConditions.add(String.format(
                                "%s >= %s",
                                mdxMemberValue,
                                left instanceof LocalDate localDate ? formatLocalDate(localDate) : left.toString()));
                    }
                    if (Objects.nonNull(right)) {
                        subConditions.add(String.format(
                                "%s <= %s",
                                mdxMemberValue,
                                right instanceof LocalDate ? String.format("CDate(\"%s\")", right) : right.toString()));
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

        private List<ICubeRestriction> toListOfRestrictions(Collection<LogicalCondition> conditions) {
            return conditions.stream()
                    .map(this::convertQueryConditionToCubeRestriction)
                    .toList();
        }

        // FIXME!
        private static Set<?> inPathValues(String hierarchy, Collection<?> values) {
            return new HashSet<>(values);
        }

        static boolean containsMeasureFilter(LogicalCondition queryCondition) {
            return switch (queryCondition) {
                case MeasureCondition measureCondition -> true;
                case AndLogicalCondition andLogicalCondition ->
                    andLogicalCondition.getSubConditions().stream()
                            .anyMatch(CubeQueryService.CubeQuerier::containsMeasureFilter);
                case OrLogicalCondition orLogicalCondition ->
                    orLogicalCondition.getSubConditions().stream()
                            .anyMatch(CubeQueryService.CubeQuerier::containsMeasureFilter);
                case NotLogicalCondition notLogicalCondition ->
                    containsMeasureFilter(notLogicalCondition.getCondition());
                default -> false;
            };
        }

        static Set<String> getMeasureFilterLevels(LogicalCondition queryCondition) {
            return switch (queryCondition) {
                case MeasureCondition measureCondition -> Set.of(measureCondition.getAtLevel());
                case AndLogicalCondition andLogicalCondition ->
                    andLogicalCondition.getSubConditions().stream()
                            .map(CubeQueryService.CubeQuerier::getMeasureFilterLevels)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                case OrLogicalCondition orLogicalCondition ->
                    orLogicalCondition.getSubConditions().stream()
                            .map(CubeQueryService.CubeQuerier::getMeasureFilterLevels)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                case NotLogicalCondition notLogicalCondition ->
                    getMeasureFilterLevels(notLogicalCondition.getCondition());
                default -> Collections.emptySet();
            };
        }

        private static String formatLocalDate(LocalDate localDate) {
            return String.format("CDate(\"%s\")", localDate);
        }
    }

    private record MdxSubSelectData(String filterExpression, Set<LevelIdentifier> levels) {}
}
