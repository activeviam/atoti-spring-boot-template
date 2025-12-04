/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.jws.WebService;

@WebService(
        name = "DummyService",
        serviceName = "DummyService",
        endpointInterface = "com.activeviam.apps.soap.IDummyService",
        targetNamespace = "http://www.quartetfs.com")
@Service
public class DummyService implements IDummyService {

    @Override
    public CellSetDTO executeMdx(String query) {
        return new CellSetDTO(List.of("this", "is", "a", "dummy", "response"));
    }

    @Override
    public CellSetDTO executeDT(VarQueryDTO varQueryDTO) {
        return new CellSetDTO(List.of("this", "is", "a", "dummy", "response"));
    }
}
