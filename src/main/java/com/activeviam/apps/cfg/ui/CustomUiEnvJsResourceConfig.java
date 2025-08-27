/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.ui;

import static com.activeviam.tech.core.api.version.Versions.VERSION;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;

import com.activeviam.springboot.atoti.admin.ui.starter.api.AtotiAdminUiProperties;
import com.activeviam.springboot.atoti.ui.starter.api.AtotiUiProperties;
import com.activeviam.tech.contentserver.spring.api.config.AdminUiEnvJs;
import com.activeviam.tech.contentserver.spring.api.config.AtotiUiContentServiceUtil;
import com.activeviam.tech.contentserver.spring.api.config.AtotiUiEnvJs;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
public class CustomUiEnvJsResourceConfig {
    // Here we are using the same env.js content for both, however in case of remote CS we would have a different
    // content.
    private static final String ENV_JS =
            """
            var baseUrl = window.location.href.split('%1$s')[0];
            var atotiVersion = "%2$s"

            window.env = {
                "jwtServer": {
                    "url": baseUrl,
                    "version": atotiVersion
                },
                "contentServer": {
                    "url": baseUrl,
                    "version": atotiVersion
                },
                // WARNING: Changing the keys of atotiServers will break previously saved widgets and dashboards.
                // If you must do it, then you also need to update each one's serverKey attribute on your content server.
                "atotiServers": {
                    "demo": {
                        "url": baseUrl,
                        "version": atotiVersion
                    },
                }
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

    @EqualsAndHashCode(callSuper = true)
    private static class EnvJsResource extends ClassPathResource {
        private final String content;

        public EnvJsResource(@NonNull String content) {
            super("classpath:/generated/env/");
            this.content = content;
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
        public boolean isReadable() {
            return true;
        }

        @Override
        public long contentLength() {
            return content.length();
        }

        @NonNull
        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
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
