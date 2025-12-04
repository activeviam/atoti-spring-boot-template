/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap.dto.core.context;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Setter;

@XmlRootElement(name = "contexts")
@Setter
public class ContextsWrapper implements Serializable {
    /** Serialization. */
    @Serial
    private static final long serialVersionUID = 8609795293528167785L;

    private List<IContextValueDTO> contexts = new ArrayList<>();

    /** @return context values */
    @XmlElementRef
    public List<IContextValueDTO> getContexts() {
        return contexts;
    }
}
