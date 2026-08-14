/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import static com.activeviam.apps.cfg.pivot.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.DistributionConstants.APPLICATION_ID;
import static com.activeviam.apps.constants.DistributionConstants.CLUSTER_ID;
import static com.activeviam.apps.constants.DistributionConstants.JGROUPS_PROTOCOL_PATH;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.impl.api.contextvalues.QueryMonitoring;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.core.intf.api.description.IDistributedActivePivotInstanceDescription;
import com.activeviam.activepivot.core.intf.api.description.IQueryClusterDefinition;

import lombok.NoArgsConstructor;

/**
 * The query node's cube: holds no data of its own. It joins the same cluster as {@link DataCubeConfig} and
 * merges the data node's hierarchies/measures into its own topology at runtime.
 *
 * <p>{@code AsOfDate} is declared as the distributing level and horizontal data duplication is enabled so
 * that several data nodes can carry the same (fully replicated) data for high availability: when nodes
 * share a priority, the query node picks one of them at random per query, so a node going down simply
 * stops being picked.
 */
@Configuration
@Profile("query-node")
@NoArgsConstructor
public class QueryCubeConfig {

    @Bean
    public IDistributedActivePivotInstanceDescription distributedActivePivotInstanceDescription() {
        return StartBuilding.cube(CUBE_NAME)
                .asQueryCube()
                .withClusterDefinition()
                .withClusterId(CLUSTER_ID)
                .withMessengerDefinition()
                .withNettyMessenger()
                .withNoProperty()
                .withProtocolPath(JGROUPS_PROTOCOL_PATH)
                .end()
                .withApplication(APPLICATION_ID)
                .withDistributingLevels(new LevelIdentifier(ASOFDATE, ASOFDATE, ASOFDATE))
                .withProperty(IQueryClusterDefinition.HORIZONTAL_DATA_DUPLICATION_PROPERTY, Boolean.toString(true))
                .end()

                // Print each query's distributed execution plan and per-node timing to this node's log, so
                // the masking/failover rehearsal (see MaskingController/DataMaintenanceController) has a
                // continuous trail of which data node actually served each query, without having to
                // separately call the /queryplan REST endpoint after every single request.
                .withSharedContextValue(
                        new QueryMonitoring().enableExecutionPlanningPrint().enableExecutionTimingPrint())
                .build();
    }
}
