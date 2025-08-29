/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.pivot.querynode;

import static com.activeviam.apps.cfg.pivot.datanode.DataCubeConfig.DATASTORE_NODE_IDENTIFIER;
import static com.activeviam.apps.cfg.pivot.datanode.DataCubeConfig.DIRECT_QUERY_NODE_IDENTIFIER;
import static com.activeviam.apps.cfg.pivot.querynode.QueryCubeConfig.DISTRIBUTING_LEVEL;
import static com.activeviam.apps.constants.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.rest.CobDateLoadController.COB_DATE_ENDPOINT;
import static com.activeviam.apps.rest.CobDateLoadController.DATE_FORMATTER;
import static com.activeviam.apps.rest.RemoteRestServiceUtils.AUTH_HEADER;
import static com.activeviam.apps.rest.RemoteRestServiceUtils.datastoreRestClient;
import static com.activeviam.apps.rest.RemoteRestServiceUtils.dqRestClient;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.dist.impl.api.cube.IMultiVersionDistributedActivePivot;
import com.activeviam.apps.cfg.source.CobDatesProperties;
import com.activeviam.apps.rest.RemoteRestServiceUtils;
import com.activeviam.tech.core.api.exceptions.ActiveViamRuntimeException;
import com.activeviam.tech.mvcc.api.IEpoch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RolloverService {

    private final IActivePivotManager activePivotManager;
    private final CobDatesProperties cobDatesProperties;

    private void loadAndRemoveDates(Collection<LocalDate> cobDatesToLoad, Collection<LocalDate> cobDatesToRemove) {
        var activePivot = (IMultiVersionDistributedActivePivot) activePivotManager.getActivePivot(CUBE_NAME);
        datastoreRestClient(activePivot)
                .ifPresentOrElse(
                        restClient -> {
                            // Load the new cob date
                            try {
                                loadRequiredDates(cobDatesToLoad, restClient);
                            } catch (Exception e) {
                                log.error("Failed to handle load request", e);
                            }
                            try {
                                // Release the old cob date
                                activePivot.unloadMembersFromDataNode(
                                        cobDatesToRemove,
                                        DISTRIBUTING_LEVEL,
                                        DATASTORE_NODE_IDENTIFIER,
                                        IEpoch.MASTER_BRANCH_NAME);
                                // delete the rest of the data
                                deleteOtherData(cobDatesToRemove, restClient);
                            } catch (ActiveViamRuntimeException e) {
                                log.error("Failed to unload dates {}", cobDatesToRemove, e);
                            }
                        },
                        () -> log.warn("Could not retrieve REST address for {}", DATASTORE_NODE_IDENTIFIER));
        dqRestClient(activePivot)
                .ifPresentOrElse(
                        restClient -> loadRequiredDates(cobDatesToLoad, restClient),
                        () -> log.warn("Could not retrieve REST address for {}", DIRECT_QUERY_NODE_IDENTIFIER));
    }

    // DQ and datastore nodes have the same API, but internally they do different things!
    private static void loadRequiredDates(Collection<LocalDate> cobDatesToLoad, WebClient restClient) {
        restClient
                .post()
                .uri(COB_DATE_ENDPOINT)
                .bodyValue(cobDatesToLoad)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER)
                .retrieve()
                // FIXME: we cannot deserialize the response
                .bodyToMono(String.class)
                .log()
                .block();
    }

    private static void deleteOtherData(Collection<LocalDate> cobDateToRemove, WebClient restClient) {
        cobDateToRemove.forEach(date -> {
            restClient
                    .delete()
                    .uri(COB_DATE_ENDPOINT + "/" + DATE_FORMATTER.format(date))
                    .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        });
    }

    public void rollover(LocalDate lastDate) {
        // Check what dates already exist
        var activePivot = (IMultiVersionDistributedActivePivot) activePivotManager.getActivePivot(CUBE_NAME);
        datastoreRestClient(activePivot)
                .ifPresentOrElse(
                        restClient -> {
                            var existingDates = RemoteRestServiceUtils.getNodeDates(restClient);
                            var requiredDates = cobDatesProperties.computeInMemoryDates(lastDate);
                            if (ObjectUtils.isEmpty(existingDates)) {
                                loadAndRemoveDates(requiredDates, Collections.emptyList());
                            } else {
                                var datesToRemove = existingDates.stream()
                                        .filter(d -> !requiredDates.contains(d))
                                        .toList();
                                var datesToLoad = requiredDates.stream()
                                        .filter(d -> !existingDates.contains(d))
                                        .toList();
                                loadAndRemoveDates(datesToLoad, datesToRemove);
                            }
                        },
                        () -> log.warn("Could not retrieve REST address for {}", COB_DATE_ENDPOINT));
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledRollover() {
        rollover(LocalDate.now());
    }
}
