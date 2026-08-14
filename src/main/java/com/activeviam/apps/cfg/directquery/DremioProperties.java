/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.directquery;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@ConfigurationProperties(prefix = "dremio")
@Profile("data-node")
@Data
@Validated
public class DremioProperties {
    /**
     * Hostname of the Dremio coordinator's Arrow Flight SQL endpoint.
     */
    @NotNull
    private String host;

    /**
     * Port of the Dremio coordinator's Arrow Flight SQL endpoint (32010 by default in Dremio).
     */
    @NotNull
    private Integer port;

    /**
     * Port of the Dremio coordinator's REST API (9047 by default in Dremio), used for the administrative
     * SQL-job calls in {@link DremioRestClient} (masking exercise's delete/restore of backend data) rather
     * than the Arrow Flight SQL port used for regular queries.
     */
    @NotNull
    private Integer restPort;

    @NotNull
    private String username;

    @NotNull
    private String password;

    /**
     * Dremio space or folder path containing the Trades/TradeAttributes datasets.
     */
    @NotNull
    private String space;

    private boolean useEncryption = false;
}
