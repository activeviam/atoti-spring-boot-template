/*
 * Copyright (C) ActiveViam 2024-2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.cfg.pivot.CubeConstants.INT_FORMATTER;
import static com.activeviam.apps.cfg.pivot.CubeConstants.NATIVE_MEASURES;
import static com.activeviam.apps.cfg.pivot.CubeConstants.TIMESTAMP_FORMATTER;
import static com.activeviam.apps.constants.DistributionConstants.APPLICATION_ID;
import static com.activeviam.apps.constants.DistributionConstants.CLUSTER_ID;
import static com.activeviam.apps.constants.DistributionConstants.JGROUPS_PROTOCOL_PATH;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesTimeLimit;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueryMonitoring;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IDataClusterDefinition;

import lombok.RequiredArgsConstructor;

/**
 * The data-node's cube: same content (measures/dimensions/aggregate provider) as a plain cube, but joined
 * to the {@link com.activeviam.apps.constants.DistributionConstants#CLUSTER_ID} cluster so a query node
 * can merge it into its topology. All of its hierarchies/measures stay visible to the query node.
 *
 * <p>Several instances of this same data node can run at once, all reading the same data from Dremio, for
 * high availability: they share the same {@code DATA_NODE_PRIORITY} (defaulted, so no per-instance
 * configuration is needed to keep them equal), which the query node's data-overlap dispatching (see {@link
 * QueryCubeConfig}) uses to pick one of them at random per query instead of double-counting their data.
 */
@Configuration
@Profile("data-node")
@RequiredArgsConstructor
public class DataCubeConfig {

    /**
     * The three aggregate-provider sub-tests used by the HA rehearsal (see {@code
     * project_directquery_dremio_migration} notes) to isolate whether a REMOVE-triggered rebuild can be
     * scoped below "the whole provider":
     *
     * <ul>
     *   <li>{@code SINGLE}: one unpartitioned {@code ByAsOfDate} bitmap provider (today's default/baseline).
     *   <li>{@code SINGLE_PARTITIONED}: the same single named provider, now internally split into per-date
     *       partitions via {@code withValuePartitioningOn}.
     *   <li>{@code PARTIAL_BY_DATE}: one separate, independently-named bitmap provider per date (via {@code
     *       filteredOn}), with no umbrella provider at all.
     * </ul>
     */
    public enum AggregateProviderMode {
        SINGLE,
        SINGLE_PARTITIONED,
        PARTIAL_BY_DATE
    }

    private static final String AGGREGATE_PROVIDER_NAME = "ByAsOfDate";

    private final Measures measures;
    private final Dimensions dimensions;

    @Value("${data-node.priority:1}")
    private final int nodePriority;

    @Value("${aggregate-provider.mode:SINGLE}")
    private final AggregateProviderMode aggregateProviderMode;

    @Value("${aggregate-provider.partition-dates:}")
    private final String partitionDatesProperty;

    private static List<LocalDate> parsePartitionDates(final String property) {
        if (property.isBlank()) {
            return List.of();
        }
        return Arrays.stream(property.split(","))
                .map(String::trim)
                .map(LocalDate::parse)
                .toList();
    }

    @Bean
    public IActivePivotInstanceDescription activePivotInstanceDescription() {
        final List<LocalDate> partitionDates = parsePartitionDates(partitionDatesProperty);
        final LevelIdentifier asOfDateLevel = LevelIdentifier.simple(ASOFDATE);

        final var afterPartialProvider = StartBuilding.cube(CUBE_NAME)
                .withContributorsCount()
                .withinFolder(NATIVE_MEASURES)
                .withAlias("Count")
                .withFormatter(INT_FORMATTER)

                // WARN: This will not be available for AggregateProvider `jit`
                .withUpdateTimestamp()
                .withinFolder(NATIVE_MEASURES)
                .withAlias("Update.Timestamp")
                .withFormatter(TIMESTAMP_FORMATTER)
                .withCalculations(measures::build)
                .withDimensions(dimensions.build())

                // Aggregate provider: pre-aggregate Notional/Count per AsOfDate in memory so queries at
                // that granularity are served without round-tripping to Dremio. Anything finer (e.g. a
                // drillthrough to individual trades) falls back to the JIT provider, which still delegates
                // straight to Dremio. See AggregateProviderMode for the three rehearsal variants this
                // switches between.
                .withAggregateProvider()
                .jit()
                .withPartialProvider();

        final var withProvider =
                switch (aggregateProviderMode) {
                    case SINGLE ->
                        afterPartialProvider
                                .withName(AGGREGATE_PROVIDER_NAME)
                                .bitmap()
                                .includingOnlyLevels(asOfDateLevel);
                    case SINGLE_PARTITIONED ->
                        afterPartialProvider
                                .withName(AGGREGATE_PROVIDER_NAME)
                                .bitmap()
                                .includingOnlyLevels(asOfDateLevel)
                                .withValuePartitioningOn(ASOFDATE);
                    case PARTIAL_BY_DATE -> {
                        var provider = afterPartialProvider
                                .withName(AGGREGATE_PROVIDER_NAME + "_" + partitionDates.getFirst())
                                .bitmap()
                                .includingOnlyLevels(asOfDateLevel)
                                .filteredOn(Map.of(asOfDateLevel, partitionDates.getFirst()));
                        for (final LocalDate date : partitionDates.subList(1, partitionDates.size())) {
                            provider = provider.withPartialProvider()
                                    .withName(AGGREGATE_PROVIDER_NAME + "_" + date)
                                    .bitmap()
                                    .includingOnlyLevels(asOfDateLevel)
                                    .filteredOn(Map.of(asOfDateLevel, date));
                        }
                        yield provider;
                    }
                };

        return withProvider
                // Shared context values
                // Query maximum execution time (before timeout cancellation): 30s
                .withSharedContextValue(QueriesTimeLimit.of(30, TimeUnit.SECONDS))
                // Print each query's execution plan and timing to this node's log (see QueryCubeConfig for
                // why this is also set on the query node)
                .withSharedContextValue(
                        new QueryMonitoring().enableExecutionPlanningPrint().enableExecutionTimingPrint())
                .withSharedMdxContext()
                .aggressiveFormulaEvaluation(true)
                .end()
                .withSharedDrillthroughProperties()
                .withMaxRows(10_000)
                .end()

                // Join the distributed cluster as a data node
                .asDataCube()
                .withClusterDefinition()
                .withClusterId(CLUSTER_ID)
                .withMessengerDefinition()
                .withNettyMessenger()
                .withNoProperty()
                .withProtocolPath(JGROUPS_PROTOCOL_PATH)
                .end()
                .withApplicationId(APPLICATION_ID)
                .withAllHierarchies()
                .withAllMeasures()
                .withConcealedBranches()
                .withProperty(IDataClusterDefinition.DATA_NODE_PRIORITY, String.valueOf(nodePriority))
                .end()
                .build();
    }
}
