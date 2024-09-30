/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.content;

import static com.activeviam.apps.constants.ContentServiceConstants.EMBEDDED_CONTENT_SERVER_HIBERNATE_PROPERTIES;

import java.io.Serial;
import java.util.Properties;

import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.activeviam.tech.contentserver.storage.private_.AHibernateContentService;

/**
 * @author ActiveViam
 */
@ConfigurationProperties(EMBEDDED_CONTENT_SERVER_HIBERNATE_PROPERTIES)
public class EmbeddedContentServerHibernateProperties extends Properties {
    @Serial
    private static final long serialVersionUID = -2772962974049022623L;

    public Configuration toHibernateConfiguration() {
        var configuration = new Configuration().addProperties(this);
        configuration
                .getProperties()
                .putIfAbsent(BatchSettings.STATEMENT_BATCH_SIZE, AHibernateContentService.INSERT_BATCH_SIZE);
        return configuration;
    }
}
