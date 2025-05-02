/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.directquery.datamodel;

import com.activeviam.database.datastore.api.description.IStoreDescription;
import com.activeviam.directquery.api.schema.JoinDescription;
import com.activeviam.directquery.api.schema.TableDescription;

public interface AggUpTableFactory {
    TableDescription asOfDateTable();

    TableDescription holdingTable();

    TableDescription holdingDetailTable();

    TableDescription scaledStatResultTable();

    TableDescription securityTable();

    TableDescription positionDetailTable();

    IStoreDescription simReturnsStore();

    IStoreDescription historicalSimReturnDatesStore();

    IStoreDescription statResultsStore();

    IStoreDescription statResultLookupStore();

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

    JoinDescription holdingToHoldingDetailsJoin();

    JoinDescription holdingToScaledStatResultJoin();

    JoinDescription holdingDetailToSecurityJoin();

    JoinDescription holdingDetailToPositionDetailJoin();

    JoinDescription holdingToAsOfDateJoin();
}
