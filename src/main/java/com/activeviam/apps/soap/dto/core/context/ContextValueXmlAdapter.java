/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap.dto.core.context;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * JAXB Adapter to adapt the context value interface
 * with the context value base class.
 *
 * @author ActiveViam
 *
 */
public class ContextValueXmlAdapter extends XmlAdapter<AContextValueDTO, IContextValueDTO> {

    @Override
    public AContextValueDTO marshal(IContextValueDTO intf) {
        return (AContextValueDTO) intf;
    }

    @Override
    public IContextValueDTO unmarshal(AContextValueDTO impl) {
        return impl;
    }
}
