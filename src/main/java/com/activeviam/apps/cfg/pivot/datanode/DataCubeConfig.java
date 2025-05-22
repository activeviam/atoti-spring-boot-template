/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.datanode;

import static com.activeviam.apps.cfg.pivot.querynode.QueryCubeConfig.DISTRIBUTING_LEVEL;
import static com.activeviam.apps.constants.CubeConstants.APPLICATION_NAME;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueriesTimeLimit;
import com.activeviam.activepivot.core.intf.api.description.IActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IDataClusterDefinition;
import com.activeviam.activepivot.core.intf.api.description.IMessengerDefinition;
import com.activeviam.apps.cfg.database.DatabaseProperties;
import com.activeviam.apps.cfg.pivot.distribution.DistributionProperties;
import com.activeviam.apps.cfg.source.CobDatesProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Import({Measures.class, Dimensions.class})
@RequiredArgsConstructor
@Slf4j
public class DataCubeConfig {

    public static String DATASTORE_NODE_IDENTIFIER = "datastoreDataNode";
    public static String DIRECT_QUERY_NODE_IDENTIFIER = "directQueryDataNode";

    @Bean
    public IActivePivotInstanceDescription activePivotInstanceDescription(
            Dimensions dimensions,
            Measures calculations,
            DatabaseProperties databaseProperties,
            CobDatesProperties cobDatesProperties,
            @Autowired(required = false) DistributionProperties distributionProperties,
            @Value("${server.port:9090}") int serverPort) {

        var isInMemory = databaseProperties.isDatastoreType();
        var builder =
                StartBuilding.cube(CUBE_NAME).withCalculations(calculations).withDimensions(dimensions);

        if (isInMemory) {
            builder = builder.withAggregateProvider().leaf();
        } else {
            builder = builder.withAggregateProvider()
                    .jit()
                    .withPartialProvider()
                    .leaf()
                    .includingOnlyLevels(DISTRIBUTING_LEVEL)
                    .filteredOn(Map.of(DISTRIBUTING_LEVEL, List.of(cobDatesProperties.computeEndOfMonthDates())));
        }
        // Shared context values
        // Query maximum execution time (before timeout cancellation): 30s
        builder = builder.withSharedContextValue(QueriesTimeLimit.of(30, TimeUnit.SECONDS))
                .withSharedMdxContext()
                .aggressiveFormulaEvaluation(true)
                .end()
                .withSharedDrillthroughProperties()
                .withMaxRows(10_000)
                .end();

        if (distributionProperties != null) {
            // In memory node has the highest priority (0)
            var overlapPriority = isInMemory ? 1 : Integer.MAX_VALUE;
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
                    .withCubeIdentifierInCluster(isInMemory ? DATASTORE_NODE_IDENTIFIER : DIRECT_QUERY_NODE_IDENTIFIER)
                    .withPort(serverPort)
                    .withAddress("localhost")
                    .withApplicationId(APPLICATION_NAME)
                    .withAllHierarchies()
                    .withAllMeasures()
                    .withProperty(IDataClusterDefinition.DATA_NODE_PRIORITY, String.valueOf(overlapPriority))
                    .end()
                    .build();
        } else {
            return builder.build();
        }
    }
}
