/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.content;

import static com.activeviam.apps.content.ContentServiceProperties.CONTENT_SERVICE_PROPERTIES_PREFIX;
import static com.activeviam.apps.content.ContentServiceProperties.EMBEDDED_CONTENT_SERVER;
import static com.activeviam.apps.content.EmbeddedContentServerHibernateProperties.EMBEDDED_CONTENT_SERVER_HIBERNATE_PROPERTIES;

import java.io.Serial;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author ActiveViam
 */
@ConfigurationProperties(EMBEDDED_CONTENT_SERVER_HIBERNATE_PROPERTIES)
public class EmbeddedContentServerHibernateProperties extends Properties {
    public static final String EMBEDDED_CONTENT_SERVER_HIBERNATE_PROPERTIES =
            CONTENT_SERVICE_PROPERTIES_PREFIX + "." + EMBEDDED_CONTENT_SERVER;
    public static final String PERSISTENCE_ENTITY_MAPPING_PATH = "persistenceEntityMappingPath";

    @Serial
    private static final long serialVersionUID = -2772962974049022623L;

    public Configuration toHibernateConfiguration() {
        return new Configuration().addProperties(this);
    }
}
