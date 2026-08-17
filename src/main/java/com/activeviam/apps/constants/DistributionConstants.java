/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Shared identifiers used to join the data-node and query-node processes into the same distributed cluster.
 * Both node roles must agree on these values.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DistributionConstants {
    /**
     * Phase 4: a distinct cluster id from the two-table (Phase 3) branch's {@code
     * atoti-spring-boot-cluster}, so both setups can run simultaneously against the same shared {@code
     * cluster-db} - JGroups' {@code JDBC_PING} discovery table scopes rows by this value, so different
     * values are sufficient for isolation without a separate discovery database.
     */
    public static final String CLUSTER_ID = "atoti-spring-boot-cluster-phase4";

    /**
     * Identifier of the data-node's application within the cluster; the query node references it to know
     * which data cubes to merge into its topology.
     */
    public static final String APPLICATION_ID = "atoti-spring-boot-data-node-phase4";

    public static final String JGROUPS_PROTOCOL_PATH = "jgroups-protocols/protocol-jdbc-ping.xml";
}
