/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap.dto.core.context;

import java.util.List;
import java.util.Objects;

import com.activeviam.activepivot.core.impl.api.contextvalues.subcube.SubCubeProperties;
import com.activeviam.activepivot.core.intf.api.contextvalues.IContextValue;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class ContextValuesAdapter extends XmlAdapter<ContextsWrapper, List<IContextValue>> {

    @Override
    public ContextsWrapper marshal(List<IContextValue> values) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<IContextValue> unmarshal(ContextsWrapper wrapper) {
        return wrapper.getContexts().stream()
                .map(c -> {
                    if (c instanceof SubCubePropertiesDTO scp) {
                        var s = new SubCubeProperties(scp.isAccessGranted());
                        scp.getAllGrantedMembers()
                                .forEach((dim, value1) -> value1.forEach(
                                        (hier, value) -> value.forEach(cond -> s.grantMembers(dim, hier, cond))));
                        return s;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(IContextValue.class::cast)
                .toList();
    }
}
