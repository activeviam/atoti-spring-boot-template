/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(targetNamespace = "http://www.quartetfs.com")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
public interface IDummyService {
    @WebMethod(operationName = "executeMDX")
    @WebResult(name = "cellSet")
    CellSetDTO executeMdx(@WebParam(name = "query") String var);

    @WebMethod(operationName = "executeDT")
    @WebResult(name = "cellSet")
    CellSetDTO executeDT(@WebParam(name = "varQueryDTO") VarQueryDTO varQueryDTO);
}
