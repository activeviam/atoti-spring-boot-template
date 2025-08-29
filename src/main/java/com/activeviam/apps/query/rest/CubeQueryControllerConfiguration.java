/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import static com.activeviam.apps.constants.PropertyConstants.DISTRIBUTION_TYPE_QUERY;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.apps.cfg.pivot.distribution.DistributionProperties;
import com.activeviam.apps.query.CubeQueryService;

@Configuration
public class CubeQueryControllerConfiguration {

    @Bean
    CubeQueryController cubeQueryController(
            @Autowired(required = false) DistributionProperties distributionProperties,
            IActivePivotManager activePivotManager,
            CubeQueryService cubeQueryService) {
        return Objects.nonNull(distributionProperties)
                        && distributionProperties.getType().equals(DISTRIBUTION_TYPE_QUERY)
                ? new QueryNodeCubeQueryController(cubeQueryService, activePivotManager)
                : new CubeQueryController(cubeQueryService);
    }
}
