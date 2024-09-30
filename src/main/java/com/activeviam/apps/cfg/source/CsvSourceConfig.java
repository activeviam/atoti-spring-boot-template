/*
 * Copyright (C) ActiveViam 2024
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.nio.file.Path;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.source.csv.api.CsvMessageChannelFactory;
import com.activeviam.source.csv.api.CsvParserConfiguration;
import com.activeviam.source.csv.api.CsvSourceFactory;
import com.activeviam.source.csv.api.FileSystemCsvTopicFactory;
import com.activeviam.source.csv.api.ICsvParserConfiguration;
import com.activeviam.source.csv.api.ICsvSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CsvSourceConfig {
    private final CsvSourceProperties csvSourceProperties;
    private final IDatastore datastore;

    /**
     * Topic factory bean. Allows to create Csv topics and watch changes to directories. Autocloseable.
     *
     * @return the topic factory
     */
    @Bean
    public FileSystemCsvTopicFactory csvTopicFactory() {
        return FileSystemCsvTopicFactory.withoutWatcherService();
    }

    @Bean(destroyMethod = "close")
    public ICsvSource<Path> csvSource(FileSystemCsvTopicFactory csvTopicFactory) {
        var datastoreSchema = datastore.getCurrentSchema();
        ICsvSource<Path> csvSource = CsvSourceFactory.create();

        csvSourceProperties.getTopics().stream()
                .map(topicProperties -> {
                    var tableFields = datastoreSchema
                            .getTable(topicProperties.storeName())
                            .getFieldNames();
                    return csvTopicFactory.createTopic(
                            topicProperties.topicName(),
                            createParserConfig(tableFields.size(), tableFields),
                            topicProperties.path());
                })
                .forEach(csvSource::addTopic);

        csvSource.configure(csvSourceProperties.toCsvSourceConfiguration());
        return csvSource;
    }

    @Bean
    public CsvMessageChannelFactory<Path> csvChannelFactory(ICsvSource<Path> csvSource) {
        return new CsvMessageChannelFactory<>(csvSource, datastore);
    }

    private ICsvParserConfiguration createParserConfig(int columnCount, List<String> columns) {
        var cfg = columns == null ? new CsvParserConfiguration(columnCount) : new CsvParserConfiguration(columns);
        cfg.setNumberSkippedLines(1); // skip the first line
        return cfg;
    }
}
