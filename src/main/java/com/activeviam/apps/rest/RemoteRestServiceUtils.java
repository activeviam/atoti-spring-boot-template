/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.cfg.pivot.datanode.DataCubeConfig.DATASTORE_NODE_IDENTIFIER;
import static com.activeviam.apps.cfg.pivot.datanode.DataCubeConfig.DIRECT_QUERY_NODE_IDENTIFIER;
import static com.activeviam.apps.rest.CobDateLoadController.COB_DATE_ENDPOINT;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDistributedActivePivot;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RemoteRestServiceUtils {
    public static final String AUTH_HEADER = "Basic " + Base64.getEncoder().encodeToString(("pivot:pivot").getBytes());

    public static Optional<WebClient> datastoreRestClient(IMultiVersionDistributedActivePivot activePivot) {
        var datastoreNodeAddress = activePivot.getClusterMembersRestAddresses().get(DATASTORE_NODE_IDENTIFIER);
        if (Objects.nonNull(datastoreNodeAddress)) {
            return Optional.of(WebClient.create(datastoreNodeAddress));
        }
        return Optional.empty();
    }

    public static Optional<WebClient> dqRestClient(IMultiVersionDistributedActivePivot activePivot) {
        var nodeAddress = activePivot.getClusterMembersRestAddresses().get(DIRECT_QUERY_NODE_IDENTIFIER);
        if (Objects.nonNull(nodeAddress)) {
            return Optional.of(WebClient.create(nodeAddress));
        }
        return Optional.empty();
    }

    public static Collection<LocalDate> getNodeDates(WebClient webClient) {
        return webClient
                .get()
                .uri(COB_DATE_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER)
                .retrieve()
                .bodyToMono(LocalDate[].class)
                .map(List::of)
                .log()
                .block();
    }
}
