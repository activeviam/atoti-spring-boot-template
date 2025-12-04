/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap.dto.core.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "subCubeProperties")
public class SubCubePropertiesDTO extends AContextValueDTO implements ISubCubePropertiesDTO {
    /** serialVersionUID. */
    private static final long serialVersionUID = -6838365461237781879L;
    /**
     * Boolean which indicates if the pivot instance is accessible
     */
    protected boolean accessGranted;

    /**
     * Set of explicitly granted measures. When empty,
     * all available measures are implicitly granted.
     */
    protected Set<String> grantedMeasures = new HashSet<>();

    /**
     * For each hierarchy, contains all the granted
     * member sets. A member set is defined by a path of
     * conditions.
     */
    protected Map<String, Map<String, Set<List<?>>>> grantedMembers = new HashMap<>();

    /**
     * Constructor.
     * Builds a subcube without any access.
     */
    public SubCubePropertiesDTO() {
        this(false);
    }

    /**
     * Constructor.
     *
     * @param accessGranted True to grant access to something in the cube.
     */
    public SubCubePropertiesDTO(boolean accessGranted) {
        this.accessGranted = accessGranted;
    }

    @Override
    public Class<? extends IContextValueDTO> getContextInterface() {
        return ISubCubePropertiesDTO.class;
    }

    @Override
    @XmlAttribute(name = "isAccessGranted")
    public boolean isAccessGranted() {
        return accessGranted;
    }

    /**
     * Grant global access to the underlying instance.
     * <p>
     * If this value is set to false. No results will be returned by the underlying pivot. <br>
     * <b>CAUTION: </b> the default value is false.
     *
     * @param accessGranted True to grant access, false to deny it.
     */
    public void setAccessGranted(boolean accessGranted) {
        this.accessGranted = accessGranted;
    }

    /**
     * @return set of names of granted measures
     */
    @XmlElementWrapper(name = "grantedMeasures")
    @XmlElement(name = "measure")
    public Set<String> getGrantedMeasures() {
        if (!accessGranted) {
            return Collections.emptySet();
        }
        return grantedMeasures;
    }

    /**
     * @param grantedMeasures The measures to set. Note that the passed collection will be copied,
     *        and will entirely replace the previously granted measures.
     */
    public void setGrantedMeasures(Set<String> grantedMeasures) {
        this.grantedMeasures = new HashSet<>(grantedMeasures);
    }

    /** @return all the granted members, for each hierarchy */
    @XmlElement(name = "hierarchyRestrictions")
    @XmlJavaTypeAdapter(SubCubeHierarchyAdapter.class)
    public Map<String, Map<String, Set<List<?>>>> getAllGrantedMembers() {
        return grantedMembers;
    }

    /**
     * @param grantedAxisMembers the grantedAxisMembers to set
     */
    public void setAllGrantedMembers(Map<String, Map<String, Set<List<?>>>> grantedAxisMembers) {
        grantedMembers = new HashMap<>();
        for (var dimensionEntry : grantedAxisMembers.entrySet()) {
            // Retrieve the dimension
            var dimension = dimensionEntry.getKey();
            for (var hierarchyEntry : dimensionEntry.getValue().entrySet()) {
                // Retrieve the hierarchy
                var hierarchy = hierarchyEntry.getKey();
                for (var grantedMember : hierarchyEntry.getValue()) {
                    grantMembers(dimension, hierarchy, grantedMember);
                }
            }
        }
    }

    public void grantMembers(String dimension, String hierarchy, List<?> conditions) {
        // 1: Store the conditions
        var dimensionHierarchies = grantedMembers.computeIfAbsent(dimension, k -> new HashMap<>());
        var hierarchyMembers = dimensionHierarchies.computeIfAbsent(hierarchy, k -> new HashSet<>());
        hierarchyMembers.add(conditions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (accessGranted ? 1231 : 1237);
        result = prime * result + ((grantedMeasures == null) ? 0 : grantedMeasures.hashCode());
        result = prime * result + ((grantedMembers == null) ? 0 : grantedMembers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        var other = (SubCubePropertiesDTO) obj;
        if (accessGranted != other.accessGranted) {
            return false;
        }
        if (grantedMeasures == null) {
            if (other.grantedMeasures != null) {
                return false;
            }
        } else if (!grantedMeasures.equals(other.grantedMeasures)) {
            return false;
        }
        if (grantedMembers == null) {
            return other.grantedMembers == null;
        } else {
            return grantedMembers.equals(other.grantedMembers);
        }
    }

    @Override
    public String toString() {
        return "SubCubeProperties [accessGranted="
                + accessGranted
                + ", grantedMeasures="
                + grantedMeasures
                + ", grantedMembers="
                + grantedMembers
                + "]";
    }
}
