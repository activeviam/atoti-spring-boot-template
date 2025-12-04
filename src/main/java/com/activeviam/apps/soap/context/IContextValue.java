/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap.context;

import java.io.Serializable;

import com.activeviam.tech.core.api.util.IClone;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(ContextValueXmlAdapter.class)
public interface IContextValue extends Serializable, IClone<IContextValue> {

    /**
     * Returns the reference interface of the context value.
     *
     * @return The reference interface of the context value.
     */
    Class<? extends IContextValue> getContextInterface();
}
