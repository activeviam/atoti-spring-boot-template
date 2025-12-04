/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap.dto;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VarDrillthroughDTO {
    private List<VarDealValueDTO> deals = Collections.emptyList();
    private int scenarioIndex;
    private String scenarioDate;
    private Set<String> attributesHeader = Collections.emptySet();

    public VarDrillthroughDTO(List<VarDealValueDTO> deals, int scenarioIndex) {
        this.deals = deals;
        this.scenarioIndex = scenarioIndex;
    }
}
