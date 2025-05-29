/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.activepivot.core.intf.api.cube.hierarchy.IHierarchy.ALLMEMBER;
import static com.activeviam.apps.query.conditions.FilterExpressionConditionVisitor.parseFilterExpression;

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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.mdx.MdxContext;
import com.activeviam.activepivot.core.impl.internal.context.filter.QueryBasedCubeRestriction;
import com.activeviam.activepivot.core.impl.internal.context.impl.ContextUtils;
import com.activeviam.activepivot.core.intf.api.contextvalues.mdx.IMdxContext;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.IMultiVersionActivePivot;
import com.activeviam.activepivot.core.intf.api.cube.metadata.HierarchyIdentifier;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.internal.context.filter.AndCubeRestriction;
import com.activeviam.activepivot.core.intf.internal.context.filter.ICubeRestriction;
import com.activeviam.activepivot.core.intf.internal.context.filter.IQueryBasedCubeRestriction;
import com.activeviam.activepivot.core.intf.internal.context.filter.InLevelRestriction;
import com.activeviam.activepivot.core.intf.internal.context.filter.NotCubeRestriction;
import com.activeviam.activepivot.core.intf.internal.context.filter.OrCubeRestriction;
import com.activeviam.activepivot.server.intf.api.dataexport.IDataExportService;
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
            var defaults = cubeQueryProperties.getCubeConfiguration().get(cube);
            var levelsConverter = new SingleDimensionLevelsConverter(defaults.getDefaultDimension());
            var dateFilterLevels = defaults.getDateFilterLevels().stream()
                    .map(levelsConverter::stringToLevelIdentifier)
                    .collect(Collectors.toSet());
            cubeQueriers.put(
                    cube,
                    new CubeQuerier(
                            cube,
                            levelsConverter,
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

    @RequiredArgsConstructor
    public static class CubeQuerier {
        private final String cube;
        private final LevelsConverter levelsConverter;
        private final String defaultDoubleFormatter;
        private final IMultiVersionActivePivot activePivot;
        private final IDataExportService dataExportService;
        Set<HierarchyIdentifier> slicingHierarchies;

        LevelsConverter getLevelsConverter() {
            return levelsConverter;
        }

        public String buildMdxQuery(CubeQueryDTO dto) {
            return buildMdxQuery(CubeQuery.fromDTO(dto, levelsConverter));
        }

        public StreamingResponseBody runQuery(CubeQueryDTO dto, Map<String, Object> exporterConfig) {
            var cubeQuery = CubeQuery.fromDTO(dto, levelsConverter);
            var contextSnapshot = ContextUtils.applyContextValues(
                    activePivot.getContext(),
                    List.of(buildMdxContext(cubeQuery), buildCubeRestrictions(cubeQuery)),
                    true);
            var dataExportOrder = new JsonDataExportOrder(
                    new JsonMdxQuery(buildMdxQuery(cubeQuery), Collections.emptyMap()), exporterConfig);
            var output = dataExportService.streamMdxQuery(dataExportOrder);
            ContextUtils.replaceContextValues(activePivot.getContext(), contextSnapshot);
            return output;
        }

        // For tests
        IMdxContext buildMdxContext(CubeQueryDTO dto) {
            return buildMdxContext(CubeQuery.fromDTO(dto, levelsConverter));
        }

        IQueryBasedCubeRestriction buildCubeRestrictions(CubeQueryDTO dto) {
            return buildCubeRestrictions(CubeQuery.fromDTO(dto, levelsConverter));
        }

        private IMdxContext buildMdxContext(CubeQuery cubeQuery) {
            var mdxContext = new MdxContext();
            mdxContext.setHiddenSubtotals(cubeQuery.getLevels());
            // FIXME: workaround, remove once https://github.com/activeviam/activepivot/pull/12984 is merged
            mdxContext.setLightCrossJoinEnabled(false);
            mdxContext.setFormatters(extractAllMetricNames(cubeQuery).stream()
                    .collect(Collectors.toMap(Function.identity(), m -> defaultDoubleFormatter)));
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
            if (!ObjectUtils.isEmpty(cubeQuery.getTopCounts())) {
                var topCounts = cubeQuery.getTopCounts();
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
            return mdxContext;
        }

        private IQueryBasedCubeRestriction buildCubeRestrictions(CubeQuery cubeQuery) {
            return QueryBasedCubeRestriction.create(
                    filterExpressionToCubeRestrictions(cubeQuery.getFiltersExpression()));
        }

        private static List<String> extractAllMetricNames(CubeQuery cubeQuery) {
            var allMetrics = new ArrayList<>(cubeQuery.getMetrics().stream()
                    .map(CubeQuery.Metric::getMetricName)
                    .toList());
            // Add calculated members
            allMetrics.addAll(Optional.ofNullable(cubeQuery.getPartitionedBy()).orElse(Collections.emptyList()).stream()
                    .map(CubeQuery.Partitioning::newMetric)
                    .toList());
            return allMetrics;
        }

        private String buildMdxQuery(CubeQuery cubeQuery) {
            var query = new StringBuilder();
            var sortBys = cubeQuery.getSortBy();
            var topRank = cubeQuery.getTopRank();
            var topCounts = cubeQuery.getTopCounts();
            var levels = cubeQuery.getLevels();
            query.append("SELECT NON EMPTY");
            query.append(System.lineSeparator());
            // Levels and top rank
            if (!ObjectUtils.isEmpty(levels)) {
                query.append(hierarchizedLevels(levels, topRank, topCounts, sortBys));
                query.append(System.lineSeparator());
            }

            // Metrics
            var metrics = extractAllMetricNames(cubeQuery);
            if (!ObjectUtils.isEmpty(metrics)) {
                query.append(String.format(
                        "{%s} ON COLUMNS",
                        metrics.stream().map(CubeQuerier::metricToMdxMeasure).collect(Collectors.joining(","))));
                query.append(System.lineSeparator());
            }

            query.append(fromCube(cube));
            return query.toString();
        }

        private static String fromCube(String cube) {
            return String.format("FROM [%s]", cube);
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

        private String topNLevels(CubeQuery.TopCount topCount) {
            return String.format(
                    "{%s, {%s}}",
                    TOP_N_SET,
                    topCount.aggregateOthers()
                            ? topNOthersMemberName(topCount)
                            : levelToMdxAllMember(topCount.level()));
        }

        private static String sortedHierarchizedLevels(
                LevelIdentifier level, Optional<CubeQuery.Sort> sort, boolean isSlicingHierarchy) {
            var levelMembers = isSlicingHierarchy ? levelToMdxMembers(level) : hierarchizedDescendants(level);
            return sort.map(s -> String.format("Order(%s, %s, %s)", levelMembers, sortToMdx(s), s.sortType()))
                    .orElse(levelMembers);
        }

        private static String levelToMdxMembers(LevelIdentifier level) {
            return String.format("%s.Members", levelToMdxPath(level));
        }

        private static String hierarchizedDescendants(LevelIdentifier level) {
            return String.format("Hierarchize(Descendants({%s},1,SELF_AND_BEFORE))", levelToMdxAllMember(level));
        }

        private String hierarchizedLevels(
                List<LevelIdentifier> levels,
                CubeQuery.TopRank topRankDefinition,
                CubeQuery.TopCount topCount,
                List<CubeQuery.Sort> sortByDefinitions) {
            var hierarchizeTemplate = new StringBuilder();
            if (Objects.nonNull(topRankDefinition)) {
                if (levels.size() == 1) {
                    hierarchizeTemplate.append("Order(%s");
                } else {
                    hierarchizeTemplate.append("Order(Crossjoin(%s)");
                }
                hierarchizeTemplate
                        .append(", ")
                        .append(metricToMdxMeasure(topRankDefinition.metric()))
                        .append(", BDESC)");
            } else {
                if (levels.size() == 1) {
                    hierarchizeTemplate.append("%s");
                } else {
                    hierarchizeTemplate.append("Crossjoin(%s) ");
                }
            }
            return String.format(
                    hierarchizeTemplate.append(" ON ROWS,").toString(),
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

        private static String sortToMdx(CubeQuery.Sort sort) {
            return sortRequiresCalculatedMember(sort)
                    ? sortingCalculatedMeasure(sort)
                    : metricToMdxMeasure(sort.metric());
        }

        private static String sortingCalculatedMemberExpression(CubeQuery.Sort sort) {
            return String.format("%s.MEMBER_VALUE", levelToCurrentMemberMdx(sort.level()));
        }

        private static String topRankSetExpression(CubeQuery.TopRank topRank) {
            return String.format(
                    "Order(%s,%s,BDESC)", levelToMdxMembers(topRank.level()), metricToMdxMeasure(topRank.metric()));
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
            return String.format("%s.[%s]", levelToMdxAllMember(topCount.level()), OTHERS_MEMBER);
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
                    // This code works with 6.1.7
                    AndCubeRestriction.create(toListOfRestrictions(andLogicalCondition.getSubConditions()));
                // FIXME: use this code in version > 6.1.8
                // ICubeRestriction.and(toListOfRestrictions(andLogicalCondition.getSubConditions()));
                case OrLogicalCondition orLogicalCondition ->
                    // This code works with 6.1.7
                    OrCubeRestriction.create(toListOfRestrictions(orLogicalCondition.getSubConditions()));
                // FIXME: use this code in version > 6.1.8
                // ICubeRestriction.or(toListOfRestrictions(orLogicalCondition.getSubConditions()));
                case NotLogicalCondition notLogicalCondition ->
                    // This code works with 6.1.7
                    NotCubeRestriction.create(
                            convertQueryConditionToCubeRestriction(notLogicalCondition.getCondition()));
                // FIXME: use this code in version > 6.1.8
                // ICubeRestriction.not(convertQueryConditionToCubeRestriction(notLogicalCondition.getCondition()));
                case InLogicalCondition<?> inLogicalCondition ->
                    // This code works with 6.1.7
                    InLevelRestriction.create(
                            levelsConverter.stringToLevelIdentifier(inLogicalCondition.getField()),
                            new HashSet<>(inLogicalCondition.getValues()));
                // FIXME: use this code in version > 6.1.8
                // ICubeRestriction.inPath(levelsConverter.stringToHierarchyIdentifier(inLogicalCondition.getField()),
                // inPathValues(inLogicalCondition.getField(),inLogicalCondition.getValues()));
                default -> ICubeRestriction.TRUE_INSTANCE;
            };
        }

        private List<ICubeRestriction> toListOfRestrictions(Collection<LogicalCondition> conditions) {
            return conditions.stream()
                    .map(this::convertQueryConditionToCubeRestriction)
                    .toList();
        }

        private Object[] inPathValues(String hierarchy, Collection<?> values) {
            if (isSlicingHierarchy(levelsConverter.stringToHierarchyIdentifier(hierarchy))) {
                return new Object[] {values};
            } else {
                return new Object[] {ALLMEMBER, values};
            }
        }
    }
}
