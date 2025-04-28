/*
 * Copyright (C) ActiveViam 2018-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg;

import static com.activeviam.activepivot.server.impl.private_.rest.diagnostics.DiagnosticsRestServiceController.MEASURE_LINEAGE;
import static com.activeviam.activepivot.server.impl.private_.rest.diagnostics.DiagnosticsRestServiceController.REST_API_URL_PREFIX;
import static com.activeviam.springboot.atoti.server.starter.api.PropertyNames.DIAGNOSTICS_SERVICE_ENABLED_PROPERTY;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Atoti 6.1.4 introduced a new diagnostics endpoint to provide measure lineage information. This is used
 * for the Measure dependencies tab in Atoti Admin UI however, the version of Admin UI available at the time expects
 * these endpoints to have a different URL. This controller simply redirects requests to the new diagnostics endpoint.
 *
 * @author ActiveViam
 */
@Deprecated
@RestController
@ConditionalOnProperty(value = DIAGNOSTICS_SERVICE_ENABLED_PROPERTY, havingValue = "true")
public class DiagnosticsRedirectController {
    private static final String DIAGNOSTICS_URL = REST_API_URL_PREFIX + MEASURE_LINEAGE;

    @GetMapping(value = "/tree/cubes")
    public RedirectView getCubeNames() {
        return new RedirectView(DIAGNOSTICS_URL + "/cubes", true);
    }

    @GetMapping(value = "/tree/measures")
    public RedirectView getMeasures(@RequestParam("pivotId") String pivotId) {
        return new RedirectView(DIAGNOSTICS_URL + "/measures?pivotId=" + pivotId, true);
    }
}
