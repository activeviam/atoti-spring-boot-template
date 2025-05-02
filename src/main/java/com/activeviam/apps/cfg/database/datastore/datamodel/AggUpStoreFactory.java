/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.datastore.datamodel;

import com.activeviam.database.datastore.api.description.IReferenceDescription;
import com.activeviam.database.datastore.api.description.IStoreDescription;

public interface AggUpStoreFactory {
    IStoreDescription asOfDateStore();

    IStoreDescription holdingStore();

    IStoreDescription holdingDetailStore();

    IStoreDescription scaledStatResultStore();

    IStoreDescription securityStore();

    IStoreDescription simReturnsStore();

    IStoreDescription historicalSimReturnDatesStore();

    IStoreDescription statResultsStore();

    IStoreDescription statResultLookupStore();

    IStoreDescription positionDetailStore();

    IStoreDescription fxResultsStore();

    IStoreDescription fxEquivalentsStore();

    IStoreDescription fxEquivalentsLookupStore();

    IStoreDescription statisticBaseCurrencyStore();

    IStoreDescription engineDimensionStore();

    IStoreDescription currenciesStore();

    IStoreDescription statFxAttributesStore();

    IStoreDescription dimensionLevelAttributeStore();

    IStoreDescription fundLookThroughSecurityStore();

    IStoreDescription equityLookThroughSecurityStore();

    IStoreDescription equityFuturesLookThroughSecurityStore();

    IReferenceDescription holdingToHoldingDetailsReference();

    IReferenceDescription holdingToScaledStatResultReference();

    IReferenceDescription holdingDetailToSecurityReference();

    IReferenceDescription holdingDetailToPositionDetailReference();

    IReferenceDescription holdingToAsOfDateReference();
}
