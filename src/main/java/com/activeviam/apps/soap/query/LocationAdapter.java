/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap.query;

import com.activeviam.activepivot.core.impl.internal.location.InternalLocationUtil;
import com.activeviam.activepivot.core.intf.api.location.ILocation;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class LocationAdapter extends XmlAdapter<String, ILocation> {
    @Override
    public String marshal(ILocation location) {
        return location.toString();
    }

    @Override
    public ILocation unmarshal(String location) {
        return InternalLocationUtil.stringLoc(location);
    }
}
