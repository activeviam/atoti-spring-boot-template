/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap.context;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * JAXB Adapter to adapt the context value interface
 * with the context value base class.
 *
 * @author ActiveViam
 *
 */
public class ContextValueXmlAdapter extends XmlAdapter<AContextValue, IContextValue> {

    @Override
    public AContextValue marshal(IContextValue intf) {
        return (AContextValue) intf;
    }

    @Override
    public IContextValue unmarshal(AContextValue impl) {
        return impl;
    }
}
