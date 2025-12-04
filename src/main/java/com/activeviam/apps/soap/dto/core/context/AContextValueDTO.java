/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap.dto.core.context;

import java.io.Serial;

import com.activeviam.tech.core.api.exceptions.ActiveViamRuntimeException;

import jakarta.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({SubCubePropertiesDTO.class})
public abstract class AContextValueDTO implements IContextValueDTO {
    /** serialVersionUID. */
    @Serial
    private static final long serialVersionUID = -8101163655036794355L;

    @Override
    public IContextValueDTO clone() {
        try {
            return (IContextValueDTO) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ActiveViamRuntimeException("Context value implementation does not support cloning.", e);
        }
    }

    /** All Context values must implement equals method */
    @Override
    public abstract boolean equals(Object object);

    /** All Context values must implement hashcode method */
    @Override
    public abstract int hashCode();
}
