/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.time.LocalDate;

import org.springframework.context.ApplicationEvent;

import com.activeviam.io.dlc.impl.rest.resposne.DlcUnloadResponseDTO;

import lombok.Getter;

@Getter
public class AsOfDateUnloadedEvent extends ApplicationEvent {
    private final LocalDate asOfDate;
    private final DlcUnloadResponseDTO loadResponse;

    public AsOfDateUnloadedEvent(Object source, LocalDate asOfDate, DlcUnloadResponseDTO loadResponse) {
        super(source);
        this.asOfDate = asOfDate;
        this.loadResponse = loadResponse;
    }
}
