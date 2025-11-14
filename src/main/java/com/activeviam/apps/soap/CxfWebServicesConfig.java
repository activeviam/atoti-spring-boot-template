/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.xml.ws.Endpoint;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CxfWebServicesConfig {
    private final IDummyService dummyService;

    @Bean
    public Endpoint queriesEndpoint(Bus cxfBus) {
        var endpoint = new EndpointImpl(cxfBus, dummyService);
        endpoint.publish("/DummyService");
        return endpoint;
    }
}
