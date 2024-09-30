/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author ActiveViam
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentServiceConstants {
    /********************* Property names ***********************/
    public static final String CONTENT_SERVICE_PROPERTIES_PREFIX = "content-service";

    public static final String CONTENT_SERVER_PROPERTY = "server";
    public static final String EMBEDDED_CONTENT_SERVER = "embedded";
    public static final String CONTENT_SERVICE_SECURITY_PROPERTIES = "security";
    public static final String EMBEDDED_CONTENT_SERVER_HIBERNATE_PROPERTIES =
            CONTENT_SERVICE_PROPERTIES_PREFIX + "." + EMBEDDED_CONTENT_SERVER;
}
