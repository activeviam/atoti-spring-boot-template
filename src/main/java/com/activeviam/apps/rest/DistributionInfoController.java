/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.cfg.pivot.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.DistributionConstants.APPLICATION_ID;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.IActivePivotVersion;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.dist.impl.api.cube.IDistributedActivePivotVersion;
import com.activeviam.activepivot.dist.impl.api.distribution.IDistributionInformation;
import com.activeviam.activepivot.dist.impl.api.distribution.MemberMapping;
import com.activeviam.tech.mvcc.api.IEpoch;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;

/**
 * Exposes the query node's own live {@code AsOfDate} routing table, so a client can get actual proof that
 * masking a date on a data node (see {@link MaskingController}) changed query dispatch, rather than just
 * trusting the mask/unmask call's own success flag.
 *
 * <p>This can only live on the query node: {@link IDistributionInformation} - the member-to-data-node
 * mapping used to decide where a distributed sub-query is sent - is exposed by {@link
 * IDistributedActivePivotVersion#getDistributionInformation()}, and only the query node's cube implements
 * that interface. The data node's own cube ({@code IMultiVersionDataActivePivot}) has no such accessor;
 * it doesn't hold this routing state at all, so this check cannot be wired into {@code
 * MaskingController} directly - it has to be a separate call against the query node.
 *
 * <p>Also accepts an optional {@value EndpointConstants#TEST_RUN_ID_HEADER} header, echoed onto the
 * request's span as {@value EndpointConstants#TEST_RUN_ID_ATTRIBUTE} - see {@link MaskingController}.
 *
 * <p><b>Caveat confirmed live</b>: {@link IDistributionInformation#getMaskedMemberMapping()} only reflects
 * masks that were applied while this query node's process has been continuously running - restarting the
 * query node after a mask was already active drops it from this endpoint's {@code maskedDataNodes} (the
 * mask itself, and its effect on routing, is unaffected - only this reporting API loses visibility of it).
 */
@RestController
@Profile("query-node")
@RequestMapping(DistributionInfoController.DISTRIBUTION_ENDPOINT)
@RequiredArgsConstructor
public class DistributionInfoController {
    public static final String DISTRIBUTION_ENDPOINT = CUSTOM_REST_PATH + "/distribution";

    private final IActivePivotManager activePivotManager;

    @GetMapping("/{date}")
    public DistributionResult get(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        if (testRunId != null) {
            Span.current().setAttribute(EndpointConstants.TEST_RUN_ID_ATTRIBUTE, testRunId);
        }
        final IDistributionInformation info = distributionInformation();
        return new DistributionResult(
                date, dataNodesFor(info.getMemberMapping(), date), dataNodesFor(info.getMaskedMemberMapping(), date));
    }

    private IDistributionInformation distributionInformation() {
        final IActivePivotVersion head =
                activePivotManager.getActivePivot(CUBE_NAME).getHead(IEpoch.MASTER_BRANCH_NAME);
        return ((IDistributedActivePivotVersion) head).getDistributionInformation();
    }

    private static List<String> dataNodesFor(final MemberMapping mapping, final LocalDate date) {
        final Map<String, Collection<Object>> nodeMapping = Optional.ofNullable(
                        mapping.getApplicationMapping().get(APPLICATION_ID))
                .map(perLevel -> perLevel.get(LevelIdentifier.simple(ASOFDATE)))
                .map(MemberMapping.MemberPerDataNodeMapping::getNodeMapping)
                .orElse(Map.of());
        // DistributionInformation#formatMemberPerDataNodeMapping renders every member via
        // Object::toString before handing it back, so this side must compare against date.toString()
        // rather than the LocalDate instance itself.
        final String dateAsMemberString = date.toString();
        return nodeMapping.entrySet().stream()
                .filter(entry -> entry.getValue().contains(dateAsMemberString))
                .map(Map.Entry::getKey)
                .toList();
    }

    public record DistributionResult(LocalDate date, List<String> servingDataNodes, List<String> maskedDataNodes) {}
}
