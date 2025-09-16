/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.cfg.source;

import static com.activeviam.apps.constants.StoreAndFieldConstants.AS_OF_DATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.BASE_CCY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.COUNTER_CCY;
import static com.activeviam.apps.constants.StoreAndFieldConstants.FX_RATE;
import static com.activeviam.apps.constants.StoreAndFieldConstants.FX_RATES_STORE_NAME;

import com.activeviam.database.datastore.api.IDatastore;
import com.activeviam.source.common.api.IStoreMessage;
import com.activeviam.source.common.api.ITuplePublisher;
import com.activeviam.source.csv.api.IFileInfo;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class FXRateTuplePublisher implements ITuplePublisher<IFileInfo<Path>> {

    protected final IDatastore datastore;
    protected static String BASE_CURRENCY = "EUR";

    public FXRateTuplePublisher(IDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void publish(IStoreMessage<? extends IFileInfo<Path>, ?> message, List<Object[]> tuples) {

        HashMap<String, FxRate> ratesFromBase = tuples.stream()
                .filter(t -> BASE_CURRENCY.equals(message.read(BASE_CCY,t)))
                .collect(Collectors.toMap(
                        t -> (String) message.read(COUNTER_CCY,t),
                        t -> FxRate.fromTuple(t,message.getColumnIndexes()),
                        (a, b) -> a,           // merge function (if needed)
                        HashMap::new
                ));

        HashSet<Object[]> fxRatesGenerated = new HashSet<>();
        for (String newBaseCcy : ratesFromBase.keySet()) {
            double rateNewBaseToOldBase = 1./ratesFromBase.get(newBaseCcy).rate;
            for(FxRate origFxRate : ratesFromBase.values()){
                fxRatesGenerated.add(new FxRate(
                        newBaseCcy,
                        origFxRate.counterCcy,
                        rateNewBaseToOldBase*origFxRate.rate,
                        origFxRate.asOfDate).toTuple(message.getColumnIndexes()));
            }
        }
        datastore.getTransactionManager().addAll(FX_RATES_STORE_NAME, fxRatesGenerated);
    }

    @Override
    public Collection<String> getTargetStores() {
        return Collections.singleton(FX_RATES_STORE_NAME);
    }

    private record FxRate(String baseCcy, String counterCcy, double rate, LocalDate asOfDate) {

        static FxRate fromTuple(Object[] tuple,Map<String,Integer> columnIndices) {
            var baseCcy = (String) tuple[columnIndices.get(BASE_CCY)];
            var counterCcy = (String) tuple[columnIndices.get(COUNTER_CCY)];
            var rate = (double) tuple[columnIndices.get(FX_RATE)];
            var asOfDate = (LocalDate) tuple[columnIndices.get(AS_OF_DATE)];
            return new FxRate(baseCcy,counterCcy,rate,asOfDate);
        }

        Object[] toTuple(Map<String,Integer> columnIndices) {
            var newTuple = new Object[4];
            newTuple[columnIndices.get(BASE_CCY)] = this.baseCcy;
            newTuple[columnIndices.get(COUNTER_CCY)] = this.counterCcy;
            newTuple[columnIndices.get(FX_RATE)] = this.rate;
            newTuple[columnIndices.get(AS_OF_DATE)] = this.asOfDate;
            return newTuple;
        }
    }
}