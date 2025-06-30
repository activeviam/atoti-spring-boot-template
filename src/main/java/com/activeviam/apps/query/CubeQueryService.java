/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query;

import static com.activeviam.activepivot.core.intf.api.cube.hierarchy.IHierarchy.ALLMEMBER;

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
import com.activeviam.activepivot.core.impl.api.cube.hierarchy.HierarchiesUtil;
import com.activeviam.activepivot.core.impl.internal.context.filter.QueryBasedCubeRestriction;
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

        public StreamingResponseBody runQuery(CubeQuery cubeQuery, Map<String, Object> exporterConfig) {
            assertIsReady();
            var contextValues = new ArrayList<IContextValue>();
            contextValues.add(buildMdxContext(cubeQuery));
            if (cubeQuery.getUseContext()) {
                var cubeRestrictions = buildCubeRestrictions(cubeQuery);
                contextValues.add(cubeRestrictions);
            }
            var contextSnapshot = ContextUtils.applyContextValues(activePivot.getContext(), contextValues, true);
            var mdx = buildMdxQuery(cubeQuery);
            log.info("Mdx Query: {}", mdx);
            var dataExportOrder =
                    new JsonDataExportOrder(new JsonMdxQuery(mdx, Collections.emptyMap()), exporterConfig);
            var output = dataExportService.streamMdxQuery(dataExportOrder);
            ContextUtils.replaceContextValues(activePivot.getContext(), contextSnapshot);
            return output;
        }

        IMdxContext buildMdxContext(CubeQuery cubeQuery) {
            var mdxContext = new MdxContext();
            // These context values can only be added to the mdxContext, not as pure MDX!
            mdxContext.setHiddenSubtotals(cubeQuery.getLevels());
            // FIXME: workaround, remove once https://github.com/activeviam/activepivot/pull/12984 is merged
            mdxContext.setLightCrossJoinEnabled(false);
            mdxContext.setFormatters(extractAllMetricNames(cubeQuery).stream()
                    .collect(Collectors.toMap(Function.identity(), m -> defaultDoubleFormatter)));
            if (cubeQuery.getUseContext()) {
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
            }
            return mdxContext;
        }

        private static String buildCalculatedMembersMdx(CubeQuery cubeQuery) {
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
            // If any of these conditions is true, we need to add the calculated Members
            if (!ObjectUtils.isEmpty(sortByCalculatedMember)
                    || !ObjectUtils.isEmpty(partitionedBy)
                    || !ObjectUtils.isEmpty(topRank)
                    || !ObjectUtils.isEmpty(topCount)) {
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
            }
            return query.toString();
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

        private static String calculatedMemberMdx(String memberName, String expression) {
            return String.format("Member %s AS (%s)", memberName, expression);
        }

        private static String partitioningCalculatedMeasureMdx(CubeQuery.Partitioning partitioning) {
            return calculatedMemberMdx(
                    measureToMemberMdx(partitioning.newMetric()), partitioningCalculatedMemberExpression(partitioning));
        }

        private static String topRankCalculatedMeasureMdx(CubeQuery.TopRank topRank) {
            return calculatedMemberMdx(TOP_RANK_MEASURE, topRankMemberExpression(topRank));
        }

        private static String topCountOthersCalculatedMeasureMdx(CubeQuery.TopCount topCount) {
            var measure = topNOthersMemberName(topCount);
            return calculatedMemberMdx(measure, topNOthersMemberExpression(topCount));
        }

        IQueryBasedCubeRestriction buildCubeRestrictions(CubeQuery cubeQuery) {
            return QueryBasedCubeRestriction.create(convertQueryConditionToCubeRestriction(cubeQuery.getFilter()));
        }

        private static List<String> extractAllMetricNames(CubeQuery cubeQuery) {
            var allMetrics = new ArrayList<>(cubeQuery.getMetrics());
            // Add calculated members
            allMetrics.addAll(Optional.ofNullable(cubeQuery.getPartitionedBy()).orElse(Collections.emptyList()).stream()
                    .map(CubeQuery.Partitioning::newMetric)
                    .toList());
            return allMetrics;
        }

        public String buildMdxQuery(CubeQuery cubeQuery) {
            assertIsReady();
            var query = new StringBuilder();
            var sortBys = cubeQuery.getSortBy();
            var topRank = cubeQuery.getTopRank();
            var topCount = cubeQuery.getTopCount();
            var levels = cubeQuery.getLevels();
            var fullMdxQuery = !cubeQuery.getUseContext();

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

            // If we are using a subselect to add filters, add them here
            if (!(cubeQuery.getFilter() instanceof TrueLogicalCondition) && fullMdxQuery) {
                var bottomLevel = cubeQuery.getLevels().getLast();
                query.append(subSelectWithFilter(cubeQuery.getFilter(), bottomLevel));
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
                            : levelToMdxAllMember(topCount.level()));
        }

        private static String sortedHierarchizedLevels(
                LevelIdentifier level, Optional<CubeQuery.Sort> sort, boolean isSlicingHierarchy) {
            var levelMembers = isSlicingHierarchy ? levelToMdxMembers(level) : hierarchizedDescendantsAllMember(level);
            return sort.map(s -> orderMdx(levelMembers, sortingMeasureToMdx(s), s.sortType()))
                    .orElse(levelMembers);
        }

        private static String hierarchizedMembers(LevelIdentifier level, boolean isSlicingHierarchy) {
            return isSlicingHierarchy ? levelToMdxMembers(level) : hierarchizedDescendantsMembers(level);
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

        private static String hierarchizedDescendantsMembers(LevelIdentifier level) {
            return String.format("Hierarchize(Descendants({%s},1,SELF_AND_BEFORE))", levelToMdxMembers(level));
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

        private static String sortingMeasureToMdx(CubeQuery.Sort sort) {
            return sortRequiresCalculatedMember(sort)
                    ? sortingCalculatedMeasure(sort)
                    : metricToMdxMeasure(sort.metric());
        }

        private static String sortingCalculatedMemberExpression(CubeQuery.Sort sort) {
            return String.format("%s.MEMBER_VALUE", levelToCurrentMemberMdx(sort.level()));
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

        private static String topNOthersMemberName(CubeQuery.TopCount topCount) {
            return String.format("%s.[%s]", levelToMdxAllMember(topCount.level()), OTHERS_MEMBER);
        }

        // Recursively build the cube restriction object
        private ICubeRestriction convertQueryConditionToCubeRestriction(LogicalCondition queryCondition) {
            return switch (queryCondition) {
                case AndLogicalCondition andLogicalCondition ->
                    AndCubeRestriction.create(toListOfRestrictions(andLogicalCondition.getSubConditions()));
                case OrLogicalCondition orLogicalCondition ->
                    OrCubeRestriction.create(toListOfRestrictions(orLogicalCondition.getSubConditions()));
                case NotLogicalCondition notLogicalCondition ->
                    NotCubeRestriction.create(
                            convertQueryConditionToCubeRestriction(notLogicalCondition.getCondition()));
                case InLogicalCondition<?> inLogicalCondition ->
                    InLevelRestriction.create(
                            levelsConverter.stringToLevelIdentifier(inLogicalCondition.getField()),
                            new HashSet<>(inLogicalCondition.getValues()));
                case MeasureCondition measureCondition ->
                    throw new UnsupportedOperationException(MeasureCondition.class.getSimpleName());
                case LikeLogicalCondition likeCondition -> {
                    var allMembers =
                            getMembersForLevel(levelsConverter.stringToLevelIdentifier(likeCondition.getField()));
                    var criteria = likeCondition.getMatchingCriteria();
                    var valuesToFilter = allMembers.stream()
                            .filter(m -> m.contains(criteria))
                            .toList();
                    yield InLevelRestriction.create(
                            levelsConverter.stringToLevelIdentifier(likeCondition.getField()),
                            new HashSet<>(valuesToFilter));
                }
                default -> ICubeRestriction.TRUE_INSTANCE;
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

        private String subSelectWithFilter(LogicalCondition queryCondition, LevelIdentifier bottomLevel) {
            var subSelectData = convertQueryConditionToMdxSubSelectData(queryCondition);
            if (ObjectUtils.isEmpty(subSelectData)) {
                return "";
            }
            var levels = new ArrayList<>(subSelectData.levels());
            // Add a default level to the crossjoin
            if (levels.isEmpty()) {
                levels.add(bottomLevel);
            }
            var crossJoin = String.format(
                    levels.size() == 1 ? "%s" : "Crossjoin(%s)",
                    levels.stream()
                            .map(l -> hierarchizedMembers(l, isSlicingHierarchy(l.getHierarchy())))
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
                    var mdxLevelValue = levelToCurrentMemberMdx(level) + ".MEMBER_CAPTION";
                    yield new MdxSubSelectData(
                            values.stream()
                                    .map(value -> mdxLevelValue + " = \"" + value.toString() + "\"")
                                    .collect(Collectors.joining(" OR ", "(", ")")),
                            Set.of(level));
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

        private Object[] inPathValues(String hierarchy, Collection<?> values) {
            if (isSlicingHierarchy(levelsConverter.stringToHierarchyIdentifier(hierarchy))) {
                return new Object[] {values};
            } else {
                return new Object[] {ALLMEMBER, values};
            }
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
    }

    private record MdxSubSelectData(String filterExpression, Set<LevelIdentifier> levels) {}
}
