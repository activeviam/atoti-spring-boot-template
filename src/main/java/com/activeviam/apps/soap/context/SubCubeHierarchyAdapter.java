/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.activeviam.activepivot.core.intf.api.location.ILocation;
import com.activeviam.tech.core.api.exceptions.ActiveViamRuntimeException;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class SubCubeHierarchyAdapter
        extends XmlAdapter<SubCubeHierarchyAdapter.SubCubeHierarchyList, Map<String, Map<String, Set<List<Object>>>>> {

    @Override
    public Map<String, Map<String, Set<List<Object>>>> unmarshal(SubCubeHierarchyList toUnmarshal) {
        Map<String, Map<String, Set<List<Object>>>> dimensions = new HashMap<>();
        if (toUnmarshal != null && toUnmarshal.getHierarchies() != null) {
            for (var subCubeHierarchy : toUnmarshal.getHierarchies()) {
                // The dimension, the hierarchy and the members
                var dim = subCubeHierarchy.dimension;
                var hier = subCubeHierarchy.name;
                // These are JAXB serializable objects that have to be deserialized
                Set<List<Object>> deserializedMemberPaths = new HashSet<>();
                for (var memberPath : subCubeHierarchy.getMemberPaths()) {
                    List<Object> deserializedMemberPath = new ArrayList<>();
                    for (var member : memberPath.getMemberPath()) {
                        if (member instanceof String) {
                            if (member.equals(ILocation.WILDCARD)) {
                                deserializedMemberPath.add(null);
                            } else {
                                deserializedMemberPath.add(member);
                            }
                        } else {
                            throw new IllegalArgumentException();
                        }
                    }
                    deserializedMemberPaths.add(deserializedMemberPath);
                }

                var hierarchies = dimensions.computeIfAbsent(dim, k -> new HashMap<>());
                hierarchies.put(hier, deserializedMemberPaths);
            }
        }
        return dimensions;
    }

    @Override
    public SubCubeHierarchyList marshal(Map<String, Map<String, Set<List<Object>>>> toMarshal) {
        throw new ActiveViamRuntimeException("Not implemented yet");
    }

    @NoArgsConstructor
    @Setter
    @XmlRootElement(name = "restrictedHierarchies")
    public static class SubCubeHierarchyList {
        /**
         * @param hierarchies the hierarchies to set
         */
        protected List<SubCubeHierarchy> hierarchies;

        /** @param subList */
        public SubCubeHierarchyList(List<SubCubeHierarchy> subList) {
            super();
            hierarchies = subList;
        }

        /** @return the hierarchies */
        @XmlElement(name = "hierarchy")
        public List<SubCubeHierarchy> getHierarchies() {
            return hierarchies;
        }
    }

    @Setter
    public static class SubCubeHierarchy {
        /** The dimension */
        protected String dimension;

        /** The hierarchy */
        protected String name;

        /** The underlying members */
        protected Set<SubCubeMemberPath> memberPaths;

        public SubCubeHierarchy() {
            super();
            memberPaths = new HashSet<>();
        }

        public SubCubeHierarchy(String dimension, String name, Set<List<Object>> memberPaths) {
            super();
            this.dimension = dimension;
            this.name = name;
            this.memberPaths = new HashSet<>();

            for (List<Object> memberPath : memberPaths) {
                this.memberPaths.add(new SubCubeMemberPath(memberPath));
            }
        }

        /**
         * @return the members
         */
        @XmlElements(value = {@XmlElement(name = "allowedMember"), @XmlElement(name = "restriction")})
        public Set<SubCubeMemberPath> getMemberPaths() {
            return memberPaths;
        }

        @XmlAttribute
        public String getName() {
            return name;
        }

        @XmlAttribute
        public String getDimension() {
            return dimension;
        }
    }

    @Setter
    public static class SubCubeMemberPath {
        protected List<Object> memberPath;

        public SubCubeMemberPath() {
            super();
            memberPath = new ArrayList<>();
        }

        public SubCubeMemberPath(List<Object> givenMemberPath) {
            super();
            memberPath = givenMemberPath;
        }

        @XmlElement(name = "memberPathPart")
        public List<Object> getMemberPath() {
            return memberPath;
        }
    }
}
