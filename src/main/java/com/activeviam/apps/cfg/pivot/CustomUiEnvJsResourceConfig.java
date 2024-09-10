/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.lang.NonNull;

import com.activeviam.springboot.atoti.admin.ui.starter.api.AtotiAdminUiProperties;
import com.activeviam.springboot.atoti.ui.starter.api.AtotiUiProperties;
import com.activeviam.tech.contentserver.spring.api.config.AdminUiEnvJs;
import com.activeviam.tech.contentserver.spring.api.config.AtotiUiContentServiceUtil;
import com.activeviam.tech.contentserver.spring.api.config.AtotiUiEnvJs;

import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
public class CustomUiEnvJsResourceConfig {
    private static final String VERSION = "6.1.0";
    // Here we are using the same env.js content for both, however in case of remote CS we would have a different
    // content.
    private static final String ENV_JS =
            """
            var baseUrl = window.location.href.split('%1$s')[0];

            window.env = {
                "jwtServer": {
                    "url": baseUrl,
                    "version": "%2$s"
                },
                "contentServer": {
                    "url": baseUrl,
                    "version": "%2$s"
                },
                // WARNING: Changing the keys of atotiServers will break previously saved widgets and dashboards.
                // If you must do it, then you also need to update each one's serverKey attribute on your content server.
                "atotiServers": {
                    "atoti-spring-boot": {
                        "url": baseUrl,
                        "version": "%2$s",
                    },
                },
            };
            """;

    @Bean
    public AtotiUiEnvJs atotiUiEnvJs(AtotiUiProperties properties) {
        return () -> new EnvJsResource(String.format(ENV_JS, AtotiUiContentServiceUtil.PATH_TO_UI_FOLDER, VERSION));
    }

    @Bean
    public AdminUiEnvJs adminUiEnvJs(AtotiAdminUiProperties properties) {
        return () -> new EnvJsResource(String.format(ENV_JS, "/admin/ui", VERSION));
    }

    private static class EnvJsResource extends ByteArrayResource {
        public EnvJsResource(@NonNull String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
        }

        @NonNull
        @Override
        public org.springframework.core.io.Resource createRelative(@NonNull String relativePath) {
            return this;
        }

        @NonNull
        @Override
        public URL getURL() throws IOException {
            return URI.create("file://" + getFilename()).toURL();
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
    }
}
