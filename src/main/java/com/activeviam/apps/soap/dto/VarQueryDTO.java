/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap.dto;

import com.activeviam.apps.soap.dto.core.query.DrillthroughQueryDTO;

import lombok.Data;

@Data
public class VarQueryDTO {
    private DrillthroughQueryDTO dtQuery; // used as wrapper to retrieve the locations & the contextValues
    private double confidenceLevel; // used by varDrillthrough
    private int from; // used by varDataExtract
    private int to; // used by varDataExtract	
    private String dtHeader; // drill through headers use pipe separated
}
