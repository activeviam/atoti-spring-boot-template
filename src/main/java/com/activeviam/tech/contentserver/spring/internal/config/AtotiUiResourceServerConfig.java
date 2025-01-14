/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.tech.contentserver.spring.internal.config;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;

import com.activeviam.tech.contentserver.spring.api.config.AtotiUiContentServiceUtil;
import com.activeviam.tech.contentserver.spring.api.config.AtotiUiEnvJs;
import com.activeviam.tech.core.internal.util.ArrayUtil;
import com.activeviam.web.spring.internal.config.ASpringResourceServerConfig;

/**
 * Spring configuration for Atoti web application.
 *
 * <p>You will need to import the Atoti UI's maven artifact in your project and create an {@code
 * static/atoti-ui/env.js} file.
 *
 * @implNote Compatible with AdminUI 5.0.13+
 * @author ActiveViam
 * @see AtotiUiContentServiceUtil to initialize the Content Service
 */
@Configuration
public class AtotiUiResourceServerConfig extends ASpringResourceServerConfig {

    private static final String JAR_RESOURCES = "classpath:META-INF/resources/webjars/atoti-ui/";
    private static final String PROJECT_RESOURCES = "classpath:/static/atoti-ui/";

    /** {@code true} to redirect queries from the root URL to Atoti UI page. */
    private final boolean redirectFromRoot;

    private final AtotiUiEnvJs envJs;

    /** Constructor. */
    @Autowired
    public AtotiUiResourceServerConfig(AtotiUiEnvJs envJs) {
        super("/ui");
        redirectFromRoot = true;
        this.envJs = envJs;
    }

    /** Gets the relative path where the UI is exposed. */
    public String getNamespace() {
        return namespace;
    }

    @Override
    protected void registerRedirections(ResourceRegistry registry) {
        super.registerRedirections(registry);
        if (redirectFromRoot) {
            registry.redirectTo(namespace + "/index.html", "/");
        }
    }

    /**
     * Registers resources to serve.
     *
     * @param registry registry to use
     */
    @Override
    protected void registerResources(ResourceRegistry registry) {
        super.registerResources(registry);

        if (redirectFromRoot) {
            // This also serves requests to the root, so that the redirection from root to ActiveUI works
            registry.serve("/").addResourceLocations("/", JAR_RESOURCES).setCacheControl(getDefaultCacheControl());
        }

        registerIndex(registry);
        registerEnvJs(registry);
        registerExtensions(registry);
    }

    private void registerIndex(ResourceRegistry registry) {
        registry.serve(namespace + "/index.html*")
                .addResourceLocations(JAR_RESOURCES)
                .setCacheControl(CacheControl.noStore());
    }

    private void registerEnvJs(ResourceRegistry registry) {
        var envConfig = registry.serve(namespace + "/env.js*");
        envConfig.addResourceLocations(envJs.getResource());
    }

    private void registerExtensions(ResourceRegistry registry) {
        registry.serve(namespace + "/extensions*.json").addResourceLocations(PROJECT_RESOURCES);
    }

    @Override
    public Set<String> getServedExtensions() {
        return ArrayUtil.mutableSet(
                // Default HTML files
                "html",
                "js",
                "css",
                "map",
                "json",
                // Image extensions
                "png",
                "jpg",
                "gif",
                "ico",
                // Font extensions
                "eot",
                "svg",
                "ttf",
                "woff",
                "woff2");
    }

    @Override
    public Set<String> getResourceLocations() {
        // Directory where the assets are in activeui.jar.
        return Set.of(JAR_RESOURCES, PROJECT_RESOURCES);
    }
}
