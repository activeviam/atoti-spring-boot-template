/*
 * Copyright (C) ActiveViam 2024-2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.datastore.datamodel;

import static com.activeviam.database.api.types.ILiteralType.DOUBLE;
import static com.activeviam.database.api.types.ILiteralType.INT;
import static com.activeviam.database.api.types.ILiteralType.LOCAL_DATE;
import static com.activeviam.database.api.types.ILiteralType.STRING;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.database.datastore.api.description.impl.StoreDescription;

@Configuration
public class TradesStoreConfiguration {

    @Bean
    public IStoreDescription createTradesStoreDescription() {
        return StoreDescription.builder()
                .withStoreName(StoreAndFieldConstants.TRADES_STORE_NAME)
                .withField(StoreAndFieldConstants.ASOFDATE, LOCAL_DATE)
                .asKeyField()
                .withField(StoreAndFieldConstants.TRADES_TRADE_ID, STRING)
                .asKeyField()
                .withField(StoreAndFieldConstants.TRADES_EMPLOYEE_ID, INT)
                .withField(StoreAndFieldConstants.TRADES_NOTIONAL, DOUBLE)
                .build();
    }

    @Bean
    public IReferenceDescription tradesToEmployeesReference() {
        return StartBuilding.reference()
                .fromStore(StoreAndFieldConstants.TRADES_STORE_NAME)
                .toStore(StoreAndFieldConstants.EMPLOYEES_STORE_NAME)
                .withName(StoreAndFieldConstants.TRADES_EMPLOYEES_JOIN)
                .withMapping(StoreAndFieldConstants.TRADES_EMPLOYEE_ID, StoreAndFieldConstants.EMPLOYEES_EMPLOYEE_ID)
                .build();
    }
}
