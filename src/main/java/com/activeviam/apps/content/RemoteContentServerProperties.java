/*
 * Copyright (C) ActiveViam 2023-2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.content;

import static com.activeviam.apps.content.ContentServiceProperties.CONTENT_SERVICE_PROPERTIES_PREFIX;
import static com.activeviam.apps.content.ContentServiceProperties.REMOTE_CONTENT_SERVER;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author ActiveViam
 */
@ConditionalOnRemoteContentServer
@ConfigurationProperties(RemoteContentServerProperties.REMOTE_CONTENT_SERVER_PROPERTIES)
@Data
@Validated
public class RemoteContentServerProperties {
    public static final String REMOTE_CONTENT_SERVER_PROPERTIES =
            CONTENT_SERVICE_PROPERTIES_PREFIX + "." + REMOTE_CONTENT_SERVER;

    @NotBlank
    private String url;

    @NotBlank
    private String user;
}
