/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.cfg.database.datastore.datamodel.StoreDefinitionsConfig.DATASTORE_PARTITIONING_MODULO;
import static com.activeviam.apps.cfg.pivot.datanode.Measures.SUM;
import static com.activeviam.apps.cfg.pivot.querynode.QueryNodeActivePivotManagerConfig.DISTRIBUTING_LEVEL;
import static com.activeviam.apps.constants.CubeConstants.APPLICATION_NAME;
import static com.activeviam.apps.constants.CubeConstants.CATALOG_NAME;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.CubeConstants.MANAGER_NAME;
import static com.activeviam.apps.constants.CubeConstants.SCHEMA_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.NOTIONAL;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ID;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesResultLimit;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesTimeLimit;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotManagerDescription;
import com.activeviam.activepivot.core.intf.api.description.IDataClusterDefinition;
import com.activeviam.activepivot.core.intf.api.description.IMessengerDefinition;
import com.activeviam.activepivot.core.intf.api.description.ISelectionDescription;
import com.activeviam.apps.cfg.database.DatabaseProperties;
import com.activeviam.apps.cfg.pivot.distribution.DistributionProperties;
import com.activeviam.apps.cfg.source.CobDatesProperties;
import com.activeviam.tech.mvcc.api.policy.IEpochManagementPolicy;
import com.activeviam.tech.mvcc.api.policy.KeepLastEpochPolicy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Import({Measures.class, Dimensions.class})
public class DataNodeActivePivotManagerConfig {

    public static String DATASTORE_NODE_IDENTIFIER = "datastoreDataNode";
    public static String DIRECT_QUERY_NODE_IDENTIFIER = "directQueryDataNode";
    private final ISelectionDescription selectionDescription;

    private final Dimensions dimensions;
    private final Measures calculations;
    private final DatabaseProperties databaseProperties;
    private final CobDatesProperties cobDatesProperties;

    @Autowired(required = false)
    private final DistributionProperties distributionProperties;

    @Value("${server.port:9090}")
    private final int serverPort;

    @Bean
    public Supplier<IActivePivotManagerDescription> managerDescription() {
        return () -> StartBuilding.managerDescription(MANAGER_NAME)
                .withCatalog(CATALOG_NAME)
                .containingAllCubes()
                .withSchema(SCHEMA_NAME)
                .withSelection(selectionDescription)
                .withCube(activePivotInstanceDescription())
                .build();
    }

    @Bean
    IEpochManagementPolicy epochManagementPolicy() {
        return new KeepLastEpochPolicy();
    }

    private IActivePivotInstanceDescription activePivotInstanceDescription() {
        var isInMemory = databaseProperties.isDatastoreType();
        var builder =
                StartBuilding.cube(CUBE_NAME).withCalculations(calculations).withDimensions(dimensions);

        if (isInMemory) {
            builder = builder.withAggregateProvider()
                    .leaf()
                    .withModuloPartitioning(DATASTORE_PARTITIONING_MODULO, TRADE_ID);
        } else {
            builder = builder.withAggregateProvider()
                    .jit()
                    .withPartialProvider()
                    .leaf()
                    .includingOnlyLevels(DISTRIBUTING_LEVEL)
                    .includingOnlyMeasures(Measures.postfixMeasure(NOTIONAL, SUM))
                    .filteredOn(Map.of(DISTRIBUTING_LEVEL, List.of(cobDatesProperties.computeEndOfMonthDates())));
        }
        // Shared context values
        // Query maximum execution time (before timeout cancellation): 30s
        builder = builder.withSharedContextValue(QueriesTimeLimit.of(2, TimeUnit.MINUTES))
                .withSharedContextValue(QueriesResultLimit.withoutLimit())
                .withSharedMdxContext()
                .aggressiveFormulaEvaluation(true)
                .end()
                .withSharedDrillthroughProperties()
                .withMaxRows(10_000)
                .end();

        if (distributionProperties != null) {
            // In memory node has the highest priority (0)
            var overlapPriority = isInMemory ? 1 : Integer.MAX_VALUE;
            var address = InetAddress.getLoopbackAddress().getHostAddress();
            return builder.asDataCube()
                    .withClusterDefinition()
                    .withClusterId(distributionProperties.getClusterId())
                    .withMessengerDefinition()
                    .withNettyMessenger()
                    // Dont connect to the cluster at startup. We do this after we load the data
                    .withProperty(IMessengerDefinition.AUTO_START, Boolean.FALSE.toString())
                    .end()
                    .withProtocolPath(distributionProperties.getProtocolPath())
                    .end()
                    .withCubeIdentifierInCluster(
                            isInMemory
                                    ? String.format(
                                            "%s-%s", DATASTORE_NODE_IDENTIFIER, cobDatesProperties.getFixedCobDates())
                                    : DIRECT_QUERY_NODE_IDENTIFIER)
                    .withPort(serverPort)
                    .withAddress(address)
                    .withApplicationId(APPLICATION_NAME)
                    .withAllHierarchies()
                    .withAllMeasures()
                    .withProperty(
                            IDataClusterDefinition.DATA_NODE_PRIORITY, String.valueOf(cobDatesProperties.getPriority()))
                    .end()
                    .build();
        } else {
            return builder.build();
        }
    }
}
