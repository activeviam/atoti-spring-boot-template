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

@Data
public class VarDataExtractDTO {
    private List<VarDealVectorDTO> deals = Collections.emptyList();
    private Set<String> attributesHeader = Collections.emptySet();
    private int[] scenarioIndex;
}
