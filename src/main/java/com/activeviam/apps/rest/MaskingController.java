/*
 * Copyright (C) ActiveViam 2026
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.cfg.pivot.CubeConstants.CUBE_NAME;
import static com.activeviam.apps.constants.StoreAndFieldConstants.ASOFDATE;
import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.activepivot.core.intf.api.cube.IActivePivotManager;
import com.activeviam.activepivot.core.intf.api.cube.metadata.LevelIdentifier;
import com.activeviam.activepivot.dist.datanode.impl.api.cube.IMultiVersionDataActivePivot;
import com.activeviam.activepivot.dist.datanode.impl.api.cube.IMultiVersionDataActivePivot.IMaskingOperationReport;
import com.activeviam.activepivot.dist.datanode.impl.api.cube.IMultiVersionDataActivePivot.LevelMembers;
import com.activeviam.tech.mvcc.api.IEpoch;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;

/**
 * Manual trigger for the data-overlap masking exercise described at
 * https://docs.activeviam.com/engine/java-sdk/6.1/distributed/remove_data_overlap: masking an
 * {@code AsOfDate} on this data node makes the query node stop routing to it for that date (falling back
 * to the other data node, which still has the data), so it can be safely deleted from the shared Dremio
 * table and later restored before unmasking.
 *
 * <p>Only meaningful on a data node: the query cube has no {@link IMultiVersionDataActivePivot} of its
 * own (see {@link com.activeviam.apps.cfg.pivot.QueryCubeConfig}).
 *
 * <p>Each call gets its own named, attributed OTEL span (rather than relying solely on the generic
 * per-HTTP-request span already provided by Spring's auto-instrumentation), so a masking action is a
 * distinct, searchable event in the trace timeline that a client can correlate against the absence of
 * that data node's spans on later distributed queries - see {@link
 * com.activeviam.apps.rest.DistributionInfoController} on the query node for the complementary
 * routing-table proof, since the live member-mapping this data node's mask/unmask affects is only
 * observable from the query node's own cube.
 *
 * <p>Each call also accepts an optional {@value EndpointConstants#TEST_RUN_ID_HEADER} header, echoed onto
 * the span as {@value EndpointConstants#TEST_RUN_ID_ATTRIBUTE}, so every call across a whole rehearsal (see
 * {@link DataMaintenanceController}/{@link DistributionInfoController} too) can be filtered by one value.
 */
@RestController
@Profile("data-node")
@RequestMapping(MaskingController.MASKING_ENDPOINT)
@RequiredArgsConstructor
public class MaskingController {
    public static final String MASKING_ENDPOINT = CUSTOM_REST_PATH + "/masking";

    private final IActivePivotManager activePivotManager;

    @PostMapping("/{date}")
    @WithSpan("masking.mask")
    public MaskingResult mask(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        final MaskingResult result = toResult(dataActivePivot()
                .maskMembers(levelMembers(date), IEpoch.MASTER_BRANCH_NAME)
                .join());
        annotateSpan(date, testRunId, result);
        return result;
    }

    @DeleteMapping("/{date}")
    @WithSpan("masking.unmask")
    public MaskingResult unmask(
            @PathVariable final LocalDate date,
            @RequestHeader(name = EndpointConstants.TEST_RUN_ID_HEADER, required = false) final String testRunId) {
        final MaskingResult result = toResult(dataActivePivot()
                .unmaskMembers(levelMembers(date), IEpoch.MASTER_BRANCH_NAME)
                .join());
        annotateSpan(date, testRunId, result);
        return result;
    }

    /**
     * Tags the current span imperatively rather than via {@code @SpanAttribute} on the method
     * parameters - that annotation-based capture does not attach anything in this app's current OTEL
     * agent configuration (confirmed by inspecting real traces: neither a {@code date} nor a
     * {@value EndpointConstants#TEST_RUN_ID_ATTRIBUTE} attribute ever appeared), unlike {@link
     * DataMaintenanceController}'s equivalent {@code tagTestRunId} helper, which uses the same
     * imperative pattern and does work.
     */
    private static void annotateSpan(final LocalDate date, final String testRunId, final MaskingResult result) {
        final Span span = Span.current()
                .setAttribute("date", date.toString())
                .setAttribute("masking.successful", result.successful())
                .setAttribute("masking.successfulQueryCubes", String.join(",", result.successfulQueryCubes()));
        if (testRunId != null) {
            span.setAttribute(EndpointConstants.TEST_RUN_ID_ATTRIBUTE, testRunId);
        }
    }

    private IMultiVersionDataActivePivot dataActivePivot() {
        return (IMultiVersionDataActivePivot) activePivotManager.getActivePivot(CUBE_NAME);
    }

    private static LevelMembers levelMembers(final LocalDate date) {
        return new LevelMembers(LevelIdentifier.simple(ASOFDATE), List.of(date));
    }

    private static MaskingResult toResult(final IMaskingOperationReport report) {
        return new MaskingResult(
                report.isSuccessful(), report.getSuccessfulQueryCubes(), report.getFailedQueryCubesWithReasons());
    }

    public record MaskingResult(
            boolean successful, List<String> successfulQueryCubes, Map<String, String> failedQueryCubesWithReasons) {}
}
