/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import com.activeviam.tech.contentserver.spring.internal.config.ActiveUIResourceServerConfig;
import com.activeviam.tech.contentserver.spring.internal.config.AdminUIResourceServerConfig;

import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
public class CustomUiEnvJsResourceConfig {
    private static final String ATOTI_UI_ENV_JS =
            """
            var baseUrl = window.location.href.split('/ui')[0];

            window.env = {
                jwtServer: {
                    url: baseUrl,
                    version: "6.1.0-rc2"
                },
                contentServerUrl: baseUrl,
                contentServerVersion: "6.1.0-rc2",
                // WARNING: Changing the keys of activePivotServers will break previously saved widgets and dashboards.
                // If you must do it, then you also need to update each one's serverKey attribute on your content server.
                activePivotServers: {
                    "pivot-spring-boot": {
                        url: baseUrl,
                        version: "6.1.0-rc2",
                    },
                },
            };
            """;
    private static final String ADMIN_UI_ENV_JS =
            """
            var baseUrl = window.location.href.split("/admin/ui")[0];

            window.env = {
                jwtServer: {
                    url: baseUrl,
                    version: "6.1.0-rc2"
                },
                contentServerUrl: baseUrl,
                contentServerVersion: "6.1.0-rc2",
                activePivotServers: {
                    "pivot-spring-boot": {
                        url: baseUrl,
                        version: "6.1.0-rc2",
                    },
                },
            };
            """;

    @Bean
    @Qualifier(ActiveUIResourceServerConfig.ENVJS_RESOURCE_QUALIFIER)
    @Primary
    public Resource computeAtotiUiEnvJs() {
        return new ByteArrayResource(ATOTI_UI_ENV_JS.getBytes(StandardCharsets.UTF_8)) {
            @NonNull
            @Override
            public org.springframework.core.io.Resource createRelative(@NonNull String relativePath) {
                return this;
            }

            @NonNull
            @Override
            public URL getURL() throws IOException {
                return new URL("file://" + getFilename());
            }

            @Override
            public long lastModified() {
                return System.currentTimeMillis();
            }

            @Override
            @NonNull
            public String getFilename() {
                return "env.js";
            }
        };
    }

    @Bean
    @Qualifier(AdminUIResourceServerConfig.ENVJS_RESOURCE_QUALIFIER)
    @Primary
    public Resource computeAdminUiEnvJs() {
        return new ByteArrayResource(ADMIN_UI_ENV_JS.getBytes(StandardCharsets.UTF_8)) {
            @NonNull
            @Override
            public org.springframework.core.io.Resource createRelative(@NonNull String relativePath) {
                return this;
            }

            @NonNull
            @Override
            public URL getURL() throws IOException {
                return new URL("file://" + getFilename());
            }

            @Override
            public long lastModified() {
                return System.currentTimeMillis();
            }

            @Override
            @NonNull
            public String getFilename() {
                return "env.js";
            }
        };
    }
}
