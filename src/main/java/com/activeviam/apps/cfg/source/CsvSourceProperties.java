/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.cfg.source;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.activeviam.source.csv.api.CsvSourceConfiguration;

import lombok.Data;

@ConfigurationProperties(prefix = "source.csv")
@Data
public class CsvSourceProperties {

    private int parserThreads;

    private int bufferSize;

    private boolean synchronousMode;

    private final List<Topic> topics = new ArrayList<>();

    public record Topic(String storeName, String topicName, String path) {
    }

    public CsvSourceConfiguration<Path> toCsvSourceConfiguration() {
        return new CsvSourceConfiguration.CsvSourceConfigurationBuilder<Path>()
                .bufferSize(bufferSize)
                .parserThreads(parserThreads)
                .synchronousMode(synchronousMode)
                // TODO How to initialize comparator
                .build();
    }
}
