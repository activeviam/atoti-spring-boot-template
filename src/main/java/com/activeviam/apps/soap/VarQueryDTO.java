package com.activeviam.apps.soap;

import com.activeviam.apps.soap.query.DrillthroughQuery;

import lombok.Data;

@Data
public class VarQueryDTO {
    private DrillthroughQuery dtQuery; // used as wrapper to retrieve the locations & the contextValues
    private double confidenceLevel; // used by varDrillthrough
    private int from; // used by varDataExtract
    private int to; // used by varDataExtract	
    private String dtHeader; // drill through headers use pipe separated
}
