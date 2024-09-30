/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.content;

import static com.activeviam.apps.constants.ContentServiceConstants.CONTENT_SERVICE_PROPERTIES_PREFIX;
import static com.activeviam.apps.constants.ContentServiceConstants.CONTENT_SERVICE_SECURITY_PROPERTIES;
import static com.activeviam.springboot.atoti.server.starter.api.AtotiSecurityProperties.ROLE_USER;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * @author ActiveViam
 */
@ConfigurationProperties(CONTENT_SERVICE_PROPERTIES_PREFIX + "." + CONTENT_SERVICE_SECURITY_PROPERTIES)
@Data
public class ContentServiceSecurityProperties {
    private String calculatedMemberRole = ROLE_USER;
    private String kpiRole = ROLE_USER;
    private Duration cacheEntitlementsTtl = Duration.ofHours(1);
}
