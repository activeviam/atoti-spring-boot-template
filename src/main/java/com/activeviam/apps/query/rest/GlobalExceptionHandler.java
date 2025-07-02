/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.query.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.activeviam.activepivot.core.intf.api.mdx.MdxException;
import com.activeviam.tech.core.api.exceptions.ActiveViamRuntimeException;
import com.google.common.base.Throwables;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MdxException.class)
    public ResponseEntity<Object> handleMdxQueryException(MdxException ex) {
        return buildBadRequestResponseEntity(ex);
    }

    @ExceptionHandler(ActiveViamRuntimeException.class)
    public ResponseEntity<Object> handleActiveViamRuntimeException(ActiveViamRuntimeException ex) {
        return buildBadRequestResponseEntity(ex);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Object> handleUnsupportedOperationException(UnsupportedOperationException ex) {
        return buildBadRequestResponseEntity(ex);
    }

    private static ResponseEntity<Object> buildBadRequestResponseEntity(Exception ex) {
        return new ResponseEntity<>(
                String.format(
                        "%s%nRoot cause: %s",
                        ex.getMessage(), Throwables.getRootCause(ex).getMessage()),
                HttpStatus.BAD_REQUEST);
    }
}
