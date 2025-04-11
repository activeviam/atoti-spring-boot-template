/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.distribution;

import org.springframework.context.annotation.Bean;

import com.activeviam.apps.annotations.ConditionalOnDistribution;

@ConditionalOnDistribution
public class DistributionConfiguration {
    @Bean
    DistributionProperties clusterProperties() {
        return new DistributionProperties();
    }
}
