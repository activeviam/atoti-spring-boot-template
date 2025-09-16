/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.cfg.source;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PFEFactory {

    public static final String MATURITY_BUCKET_LT1 = "< 1 year";
    public static final String MATURITY_BUCKET_1TO5 = "1 year to 5 years";
    public static final String MATURITY_BUCKET_GT5 = "> 5 years";

    public static final String ASSET_CLASS_FX = "Foreign exchange";
    public static final String ASSET_CLASS_COMMODITIES = "Commodity";
    public static final String ASSET_CLASS_EQUITY = "Equity";
    public static final String ASSET_CLASS_INTEREST_RATE = "Interest rate";

    public static final String PFE_SUBCATEGORY_GOLD = "Gold";
    public static final String PFE_SUBCATEGORY_PRECIOUS_METAL = "Precious metals (except gold)";

    private record PFERecord(String assetClass, String pfeSubcategory, String maturityBucket, double pfeFactor) {
        Object[] toTuple() {
            return new Object[]{assetClass,pfeSubcategory,maturityBucket,pfeFactor};
        }
    }

    static Collection<Object[]> generateTuples(){
        return List.of(
                new PFERecord(ASSET_CLASS_INTEREST_RATE,null,MATURITY_BUCKET_LT1,0d),
                new PFERecord(ASSET_CLASS_INTEREST_RATE,null,MATURITY_BUCKET_1TO5,0.005d),
                new PFERecord(ASSET_CLASS_INTEREST_RATE,null,MATURITY_BUCKET_GT5,0.015d),
                new PFERecord(ASSET_CLASS_FX,null,MATURITY_BUCKET_LT1,0.01d),
                new PFERecord(ASSET_CLASS_FX,null,MATURITY_BUCKET_1TO5,0.05d),
                new PFERecord(ASSET_CLASS_FX,null,MATURITY_BUCKET_GT5,0.075d),
                new PFERecord(ASSET_CLASS_COMMODITIES,PFE_SUBCATEGORY_GOLD,MATURITY_BUCKET_LT1,0.01d),
                new PFERecord(ASSET_CLASS_COMMODITIES,PFE_SUBCATEGORY_GOLD,MATURITY_BUCKET_1TO5,0.05d),
                new PFERecord(ASSET_CLASS_COMMODITIES,PFE_SUBCATEGORY_GOLD,MATURITY_BUCKET_GT5,0.075d),
                new PFERecord(ASSET_CLASS_COMMODITIES,PFE_SUBCATEGORY_PRECIOUS_METAL,MATURITY_BUCKET_LT1,0.07d),
                new PFERecord(ASSET_CLASS_COMMODITIES,PFE_SUBCATEGORY_PRECIOUS_METAL,MATURITY_BUCKET_1TO5,0.07d),
                new PFERecord(ASSET_CLASS_COMMODITIES,PFE_SUBCATEGORY_PRECIOUS_METAL,MATURITY_BUCKET_GT5,0.1d),
                new PFERecord(ASSET_CLASS_COMMODITIES,null,MATURITY_BUCKET_LT1,0.1d),
                new PFERecord(ASSET_CLASS_COMMODITIES,null,MATURITY_BUCKET_1TO5,0.12d),
                new PFERecord(ASSET_CLASS_COMMODITIES,null,MATURITY_BUCKET_GT5,0.15d),
                new PFERecord(ASSET_CLASS_EQUITY,null,MATURITY_BUCKET_LT1,0.06d),
                new PFERecord(ASSET_CLASS_EQUITY,null,MATURITY_BUCKET_1TO5,0.08d),
                new PFERecord(ASSET_CLASS_EQUITY,null,MATURITY_BUCKET_GT5,0.1d))
                .stream()
                .map(r -> r.toTuple())
                .toList();
    }
}
