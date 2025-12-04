/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap.dto.core.query;

import java.util.Collection;
import java.util.List;

import com.activeviam.activepivot.core.impl.api.query.drillthrough.DrillthroughQuery;
import com.activeviam.activepivot.core.intf.api.contextvalues.IContextValue;
import com.activeviam.activepivot.core.intf.api.location.ILocation;
import com.activeviam.activepivot.core.intf.api.query.drillthrough.IDrillthroughQuery;
import com.activeviam.apps.soap.dto.core.context.ContextValuesAdapter;
import com.activeviam.apps.soap.dto.core.loc.LocationAdapter;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Data;

@Data
public class DrillthroughQueryDTO {
    private String pivotId;
    /**
     * The requested locations.
     */
    private Collection<ILocation> locations;
    /**
     * The query context values.
     */
    private List<IContextValue> contextValues;
    /**
     * The requested measures.
     */
    private Collection<String> measures;
    /**
     * The original mdx (if any) (for monitoring).
     */
    private String mdx;
    /**
     * See {@link #getIsFormatted()}.
     */
    private boolean isFormatted = true;

    private int firstResult;
    private int maxResults;

    @XmlJavaTypeAdapter(ContextValuesAdapter.class)
    @XmlElement(name = "contextValues")
    public List<IContextValue> getContextValues() {
        return contextValues;
    }

    @XmlJavaTypeAdapter(LocationAdapter.class)
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "location")
    public Collection<ILocation> getLocations() {
        return locations;
    }

    public boolean getIsFormatted() {
        return isFormatted;
    }

    /**
     * Sets the isFormatted flag.
     *
     * @param isFormatted The new value for isFormatted flag
     */
    public void setIsFormatted(boolean isFormatted) {
        this.isFormatted = isFormatted;
    }

    public IDrillthroughQuery toCoreDrillthroughQuery() {
        return new DrillthroughQuery(
                getPivotId(),
                getLocations(),
                getMeasures(),
                getContextValues(),
                getIsFormatted(),
                getFirstResult(),
                getMaxResults());
    }
}
