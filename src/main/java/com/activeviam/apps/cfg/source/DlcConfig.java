/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.io.dlc.impl.description.AliasesDescription;
import com.activeviam.io.dlc.impl.description.source.LocalCsvSourceDescription;
import com.activeviam.io.dlc.impl.description.topic.CsvTopicDescription;

@Configuration
public class DlcConfig {

    @Bean
    LocalCsvSourceDescription tradesCsvSource() {
        return LocalCsvSourceDescription.builder("localCsvSource", "data").build();
    }

    @Bean
    public CsvTopicDescription tradesCsvTopic() {
        return CsvTopicDescription.builder("Trades", "glob:trades*.csv").build();
    }

    @Bean
    public CsvTopicDescription employeesCsvTopic() {
        return CsvTopicDescription.builder("Employees", "glob:employees*.csv").build();
    }

    @Bean
    AliasesDescription aliases() {
        return AliasesDescription.builder()
                .alias("alias", Set.of("Trades", "Employees"))
                .build();
    }
}
