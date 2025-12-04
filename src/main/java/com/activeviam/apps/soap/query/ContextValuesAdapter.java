/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap.query;

import java.util.ArrayList;
import java.util.List;

import com.activeviam.apps.soap.context.IContextValue;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class ContextValuesAdapter extends XmlAdapter<ContextsWrapper, List<IContextValue>> {

    @Override
    public ContextsWrapper marshal(List<IContextValue> values) {
        var wrapper = new ContextsWrapper();
        wrapper.setContexts(values);
        return wrapper;
    }

    @Override
    public List<IContextValue> unmarshal(ContextsWrapper wrapper) {
        return new ArrayList<>(wrapper.getContexts());
    }
}
