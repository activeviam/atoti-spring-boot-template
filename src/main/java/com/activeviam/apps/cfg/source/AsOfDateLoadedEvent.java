/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.source;

import java.time.LocalDate;

import org.springframework.context.ApplicationEvent;

import com.activeviam.io.dlc.impl.rest.resposne.DlcLoadResponseDTO;

import lombok.Getter;

@Getter
public class AsOfDateLoadedEvent extends ApplicationEvent {
    private final LocalDate asOfDate;
    private final DlcLoadResponseDTO loadResponse;

    public AsOfDateLoadedEvent(Object source, LocalDate asOfDate, DlcLoadResponseDTO loadResponse) {
        super(source);
        this.asOfDate = asOfDate;
        this.loadResponse = loadResponse;
    }
}
