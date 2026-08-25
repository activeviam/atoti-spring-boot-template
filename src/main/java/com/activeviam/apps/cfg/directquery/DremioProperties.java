/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.directquery;

import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADES_STORE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.TRADE_ATTRIBUTES_STORE_NAME;

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
     * Which pair of Dremio tables this node reads from: {@code SMALL} is the original, hand-checkable
     * 5-rows/day dataset ({@code Trades}/{@code TradeAttributes}); {@code LARGE} is the realistic-scale
     * dataset ({@code TradesLarge}/{@code TradeAttributesLarge}, 1M reused TradeIDs x 10 days) generated
     * for the WCR-priority-aligned rehearsals - see {@code project_large_scale_dataset_2026_08} notes.
     */
    public enum DatasetSize {
        SMALL,
        LARGE
    }

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

    private DatasetSize datasetSize = DatasetSize.SMALL;

    public String getTradesTableName() {
        return datasetSize == DatasetSize.LARGE ? TRADES_STORE_NAME + "Large" : TRADES_STORE_NAME;
    }

    public String getTradeAttributesTableName() {
        return datasetSize == DatasetSize.LARGE ? TRADE_ATTRIBUTES_STORE_NAME + "Large" : TRADE_ATTRIBUTES_STORE_NAME;
    }
}
