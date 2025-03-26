/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.datastore.datamodel;

import static com.activeviam.database.api.types.ILiteralType.INT;
import static com.activeviam.database.api.types.ILiteralType.STRING;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.apps.constants.StoreAndFieldConstants;
import com.activeviam.database.datastore.api.description.IStoreDescription;

@Configuration
public class EmployeesStoreConfiguration {

    @Bean
    public IStoreDescription createEmployeesStoreDescription() {
        return StartBuilding.store()
                .withStoreName(StoreAndFieldConstants.EMPLOYEES_STORE_NAME)
                .withField(StoreAndFieldConstants.EMPLOYEES_EMPLOYEE_ID, INT)
                .asKeyField()
                .withField(StoreAndFieldConstants.EMPLOYEES_FIRST_NAME, STRING)
                .withField(StoreAndFieldConstants.EMPLOYEES_LAST_NAME, STRING)
                .build();
    }
}
