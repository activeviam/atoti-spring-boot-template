/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.querynode;

import static com.activeviam.apps.cfg.pivot.datanode.DataCubeConfig.DATASTORE_NODE_IDENTIFIER;
import static com.activeviam.apps.cfg.pivot.querynode.QueryCubeConfig.DISTRIBUTING_LEVEL;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.rest.CobDateLoadController.COB_DATE_ENDPOINT;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDistributedActivePivot;
import com.activeviam.apps.cfg.source.CobDatesProperties;
import com.activeviam.tech.mvcc.api.IEpoch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RolloverService {

    private final IActivePivotManager activePivotManager;
    private final CobDatesProperties cobDatesProperties;

    public void removeAndLoadDates(LocalDate cobDateToRemove, Collection<LocalDate> cobDatesToLoad) {
        var activePivot = (IMultiVersionDistributedActivePivot) activePivotManager.getActivePivot(CUBE_NAME);
        var datastoreNodeAddress = activePivot.getClusterMembersRestAddresses().get(DATASTORE_NODE_IDENTIFIER);
        if (Objects.nonNull(datastoreNodeAddress)) {
            var restClient = WebClient.create(datastoreNodeAddress);
            // Load the new cob date
            try {
                restClient
                        .post()
                        .uri(COB_DATE_ENDPOINT)
                        .bodyValue(cobDatesToLoad)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Basic " + Base64.getEncoder().encodeToString(("pivot:pivot").getBytes()))
                        .retrieve()
                        // FIXME: we cannot deserialize the response
                        .bodyToMono(String.class)
                        .log()
                        .block();
            } catch (Exception e) {
                log.error("Failed to handle load request", e);
            }
            try {
                // Release the old cob date
                activePivot.unloadMembersFromDataNode(
                        List.of(cobDateToRemove),
                        DISTRIBUTING_LEVEL,
                        DATASTORE_NODE_IDENTIFIER,
                        IEpoch.MASTER_BRANCH_NAME);
            } catch (Exception e) {
                log.error("Failed to unload date {}", cobDateToRemove, e);
            }
        } else {
            log.warn("Could not retrieve REST address for {}", DATASTORE_NODE_IDENTIFIER);
        }
    }

    public void rolloverDates(LocalDate newDate) {
        var dates = cobDatesProperties.computeInMemoryDates(newDate);
        var cobDateToRemove = dates.stream().max(LocalDate::compareTo).get();
        var cobDatesToLoad =
                dates.stream().filter(date -> !date.equals(cobDateToRemove)).toList();
        removeAndLoadDates(cobDateToRemove, cobDatesToLoad);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledRollover() {
        rolloverDates(LocalDate.now());
    }
}
