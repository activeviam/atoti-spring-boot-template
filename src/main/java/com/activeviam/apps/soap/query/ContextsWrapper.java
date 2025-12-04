/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap.query;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.activeviam.apps.soap.context.IContextValue;

import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "contexts")
public class ContextsWrapper implements Serializable {
    /** Serialization. */
    @Serial
    private static final long serialVersionUID = 8609795293528167785L;

    protected List<IContextValue> imported = new ArrayList<>();

    /** @return context values */
    @XmlElementRef
    public List<IContextValue> getContexts() {
        return imported;
    }

    /** @param contexts */
    public void setContexts(List<IContextValue> contexts) {
        imported = contexts;
    }
}
