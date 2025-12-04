/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.soap;

import com.activeviam.apps.soap.dto.VarDataExtractDTO;
import com.activeviam.apps.soap.dto.VarDrillthroughDTO;
import com.activeviam.apps.soap.dto.VarQueryDTO;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(targetNamespace = "http://www.quartetfs.com")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
public interface IVaRQueryService {

    /**
     * For a given cell, a var type (1 day or 10 days) and a confidence level we retrieve all the underlying pnl values for the underlying vectors that contributed to that cell.
     * The displayed pnl is taken from the underlying vector and that pnl value corresponds to the scenario of the var of the selected cell.
     * In addition to the pnl, we display the deal number for all the children.
     * We display once the scenario date and the scenario name of the selected cell.
     * @see VarQueryDTO, VarDrillthroughDTO and VarDealValueDTO.
     * @param varQueryDTO
     * @return VarDrillthroughDTO
     */
    @WebMethod(operationName = "varDrillthrough")
    @WebResult(name = "varDrillthroughDTO")
    VarDrillthroughDTO varDrillthrough(@WebParam(name = "varQueryDTO") VarQueryDTO varQueryDTO);

    /**
     * For a given cell and a var type (1 day or 10 days) we retrieve all the underlying vectors that contributed to that cell. Those vectors are displayed by deal number, we retrieve also the scenario dates vector that is displayed once.
     * Notice that you can extract subvector by specifying the from and to parameters in the VarQueryDTO.
     * @see VarQueryDTO, VarDataExtractDTO, VarDealVectorDTO.
     *
     * @param varQueryDTO
     * @return VarDataExtractDTO
     */
    @WebMethod(operationName = "varDataExtract")
    @WebResult(name = "varDataExtractDTO")
    VarDataExtractDTO varDataExtract(@WebParam(name = "varQueryDTO") VarQueryDTO varQueryDTO);

    @WebMethod(operationName = "testVarDataExtract")
    @WebResult(name = "varDataExtractDTO")
    VarDataExtractDTO testVarDataExtract(@WebParam(name = "varQueryDTO") VarQueryDTO varQueryDTO);
}
