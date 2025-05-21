/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.rest;

import static com.activeviam.apps.rest.EndpointConstants.CUSTOM_REST_PATH;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activeviam.apps.annotations.ConditionalOnQueryNode;
import com.activeviam.apps.cfg.pivot.querynode.RolloverService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(CobDateRolloverController.ROLLOVER_ENDPOINT)
@RequiredArgsConstructor
@ConditionalOnQueryNode
public class CobDateRolloverController {

    public static final String ROLLOVER_ENDPOINT = CUSTOM_REST_PATH + "/rollover";

    private final RolloverService rolloverService;

    @PostMapping
    public String rollOver() {
        rolloverService.rolloverDates(LocalDate.now());
        return "ok";
    }

    @PostMapping("/{rolloverDate}")
    public String rollOver(@PathVariable LocalDate rolloverDate) {
        rolloverService.rolloverDates(rolloverDate);
        return "ok";
    }

    @GetMapping
    public String load() {
        return "ok";
    }
}
