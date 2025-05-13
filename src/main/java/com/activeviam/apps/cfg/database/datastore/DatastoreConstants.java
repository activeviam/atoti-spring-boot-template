/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.apps.cfg.database.datastore;

import static com.activeviam.database.api.types.ILiteralType.BOOLEAN;
import static com.activeviam.database.api.types.ILiteralType.DOUBLE;
import static com.activeviam.database.api.types.ILiteralType.INT;
import static com.activeviam.database.api.types.ILiteralType.LOCAL_DATE;
import static com.activeviam.database.api.types.ILiteralType.STRING;

import java.util.Set;

import com.activeviam.activepivot.core.datastore.api.builder.StartBuilding;
import com.activeviam.activepivot.core.intf.api.cube.hierarchy.IAxisMember;
import com.activeviam.apps.cfg.database.DatabaseProperties;
import com.activeviam.apps.constants.FieldConstants;
import com.activeviam.database.api.schema.FieldPath;
import com.activeviam.database.datastore.api.description.IStoreDescription;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatastoreConstants {

    public static String referenceName(String from, String to) {
        return String.format("%s_to_%s", from, to);
    }

    public static final String PARTITION_KEY = "PartitionKey";
    public static final String SECURITY_NAME = "SecurityName";
    public static final String STAT_NAME = "StatName";

    public static final Set<String> STORES_WITH_AS_OF_DATE = Set.of(
            HoldingStore.STORE_NAME,
            HoldingDetailStore.STORE_NAME,
            ScaledStatResultStore.STORE_NAME,
            SecurityStore.STORE_NAME,
            StatResultsStore.STORE_NAME,
            SimReturnsStore.STORE_NAME,
            FxResultsStore.STORE_NAME,
            FxEquivalentsStore.STORE_NAME,
            AsOfDateStore.STORE_NAME,
            DimensionLevelAttributeStore.STORE_NAME,
            HistoricalSimReturnDatesStore.STORE_NAME,
            FundLookThroughSecurityStore.STORE_NAME,
            EquityLookThroughSecurityStore.STORE_NAME,
            EquityFuturesLookThroughSecurityStore.STORE_NAME,
            StatFxAttributesStore.STORE_NAME);

    public static final class References {
        private References() {}

        public static final String HOLDING_TO_HOLDINGDETAIL =
                referenceName(HoldingStore.STORE_NAME, HoldingDetailStore.STORE_NAME);
        public static final String HOLDING_TO_SCALEDSTATRESULT =
                referenceName(HoldingStore.STORE_NAME, ScaledStatResultStore.STORE_NAME);
        public static final String HOLDINGDETAIL_TO_SECURITY =
                referenceName(HoldingDetailStore.STORE_NAME, SecurityStore.STORE_NAME);
        public static final String HOLDINGDETAIL_TO_POSITIONDETAIL =
                referenceName(HoldingDetailStore.STORE_NAME, PositionDetailStore.STORE_NAME);
        public static final String HOLDING_TO_ASOFDATE =
                referenceName(HoldingStore.STORE_NAME, AsOfDateStore.STORE_NAME);
    }

    public static final class HoldingStore {
        private HoldingStore() {}

        public static IStoreDescription storeDescription(
                DatabaseProperties.InMemoryStoresProperties inMemoryStoresProperties) {
            var builderWithKeyField = StartBuilding.store()
                    .withStoreName(DatastoreConstants.HoldingStore.STORE_NAME)
                    .withField(DatastoreConstants.HoldingStore.Fields.PORTFOLIO, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.HoldingStore.Fields.HOLDING_ID, STRING)
                    .asKeyField()
                    .withNullableField(
                            DatastoreConstants.HoldingStore.Fields.HOLDING_UNIQUE_NAME, STRING) // FIXME: OBJECT
                    .withNullableField(
                            DatastoreConstants.HoldingStore.Fields.BASE_HOLDING_UNIQUE_NAME, STRING) // FIXME: OBJECT
                    .withField(DatastoreConstants.HoldingStore.Fields.BASE_HOLDING_ID, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.AMOUNT, DOUBLE)
                    .withField(DatastoreConstants.HoldingStore.Fields.IS_RELATIVE, BOOLEAN)
                    .withField(DatastoreConstants.HoldingStore.Fields.IS_SHORT_BASE, BOOLEAN)
                    .withField(DatastoreConstants.HoldingStore.Fields.IS_BLEND, BOOLEAN)
                    .withField(DatastoreConstants.HoldingStore.Fields.IS_FILTER, BOOLEAN)
                    .withField(DatastoreConstants.HoldingStore.Fields.IS_SHARECLASS_ARTIFICIAL, BOOLEAN)
                    .withField(DatastoreConstants.HoldingStore.Fields.IS_HOLDING_GROUP_BY_WEIGHTED_HOLDING, BOOLEAN)
                    .withField(DatastoreConstants.HoldingStore.Fields.BASE_PORTFOLIO, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.BENCHMARK_PORTFOLIO, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.BENCHMARK_METHOD, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.BASE_BENCHMARK, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.FX_HEDGING, DOUBLE)
                    .withField(DatastoreConstants.HoldingStore.Fields.RAW_FX_HEDGING, DOUBLE, Double.NaN)

                    //	.withField(DatastoreConstants.HoldingStore.Fields.RAW_FX_HEDGING, DOUBLE,
                    // ResultValues.EMPTY_VALUE)
                    .withField(DatastoreConstants.HoldingStore.Fields.BLEND_LOAD_STATUS, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.BLEND_LOAD_MESSAGE, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.LOOKTHROUGH_PORTFOLIO, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.LOOKTHROUGH_LOAD_STATUS, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.LOOKTHROUGH_LOAD_MESSAGE, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.PROCESS_DATE, LOCAL_DATE)
                    .withNullableField(DatastoreConstants.HoldingStore.Fields.HOLDING_UPDATE_TIMESTAMP, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.HOLDING_SOURCE, STRING)
                    .withField(DatastoreConstants.HoldingStore.Fields.HOLDING_PATH, STRING)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_0,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_1,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_2,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_3,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_4,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_5,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_6,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_7,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_8,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_9,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.LEVEL_10,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_0,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_1,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_2,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_3,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_4,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_5,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_6,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_7,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_8,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_9,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(
                            DatastoreConstants.HoldingStore.Fields.FUND_LEVEL_10,
                            STRING,
                            IAxisMember.DATA_MEMBER_DISCRIMINATOR)
                    .withField(DatastoreConstants.HoldingStore.Fields.PARTITION_KEY, INT, -1)
                    .asKeyField()
                    .withField(DatastoreConstants.HoldingStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField();

            var builderWithValuePartitioningOn =
                    builderWithKeyField.withValuePartitioningOn(DatastoreConstants.HoldingStore.Fields.PARTITION_KEY);
            builderWithValuePartitioningOn
                    .withIndexOn(DatastoreConstants.HoldingStore.Fields.AS_OF_DATE)
                    .withIndexOn(
                            DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                            DatastoreConstants.HoldingStore.Fields.PORTFOLIO)
                    .withIndexOn(
                            DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                            DatastoreConstants.HoldingStore.Fields.HOLDING_ID)
                    .withIndexOn(
                            DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                            DatastoreConstants.HoldingStore.Fields.HOLDING_UNIQUE_NAME)
                    .withIndexOn(
                            DatastoreConstants.HoldingStore.Fields.BASE_BENCHMARK,
                            DatastoreConstants.HoldingStore.Fields.AS_OF_DATE,
                            DatastoreConstants.HoldingStore.Fields.PORTFOLIO);

            if (inMemoryStoresProperties.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.HoldingStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final String STORE_NAME = "Holding";

        public static final class Fields {
            private Fields() {}

            public static final String PORTFOLIO = FieldConstants.PORTFOLIO;
            public static final String HOLDING_ID = FieldConstants.HOLDING_ID;
            public static final String HOLDING_UNIQUE_NAME = FieldConstants.HOLDING_UNIQUE_NAME;
            public static final String BASE_HOLDING_UNIQUE_NAME = FieldConstants.BASE_HOLDING_UNIQUE_NAME;
            public static final String BASE_HOLDING_ID = "BaseHoldingId";
            public static final String AMOUNT = FieldConstants.AMOUNT;
            public static final String IS_RELATIVE = FieldConstants.IS_RELATIVE;
            public static final String IS_BLEND = FieldConstants.IS_BLEND;
            public static final String IS_FILTER = FieldConstants.IS_FILTER;
            public static final String IS_SHARECLASS_ARTIFICIAL = FieldConstants.IS_SHARECLASS_ARTIFICIAL;
            public static final String IS_HOLDING_GROUP_BY_WEIGHTED_HOLDING =
                    FieldConstants.IS_HOLDING_GROUP_BY_WEIGHTED_HOLDING;
            public static final String BASE_PORTFOLIO = "BasePortfolio";
            public static final String IS_SHORT_BASE = FieldConstants.IS_SHORT_BASE;
            public static final String BENCHMARK_PORTFOLIO = "BenchmarkPortfolio";
            public static final String BENCHMARK_METHOD = "BenchmarkMethod";
            public static final String BASE_BENCHMARK = FieldConstants.BASE_BENCHMARK;
            public static final String FX_HEDGING = "FxHedgingPercent";
            public static final String RAW_FX_HEDGING = "RawFxHedgingPercent";
            public static final String BLEND_LOAD_STATUS = "BlendLoadStatus";
            public static final String BLEND_LOAD_MESSAGE = "BlendLoadMessage";

            public static final String LOOKTHROUGH_PORTFOLIO = FieldConstants.LOOKTHROUGH_PORTFOLIO;
            public static final String LOOKTHROUGH_LOAD_STATUS = FieldConstants.LOOKTHROUGH_LOAD_STATUS;
            public static final String LOOKTHROUGH_LOAD_MESSAGE = FieldConstants.LOOKTHROUGH_LOAD_MESSAGE;
            public static final String HOLDING_PATH = FieldConstants.HOLDING_PATH;
            public static final String PROCESS_DATE = "ProcessDate";
            public static final String HOLDING_UPDATE_TIMESTAMP = "HoldingUpdateTimestamp";
            public static final String HOLDING_SOURCE = FieldConstants.HOLDING_SOURCE;
            public static final String PARTITION_KEY = DatastoreConstants.PARTITION_KEY;
            public static final String FUND_LEVEL_0 = "FundLevel0";
            public static final String FUND_LEVEL_1 = "FundLevel1";
            public static final String FUND_LEVEL_2 = "FundLevel2";
            public static final String FUND_LEVEL_3 = "FundLevel3";
            public static final String FUND_LEVEL_4 = "FundLevel4";
            public static final String FUND_LEVEL_5 = "FundLevel5";
            public static final String FUND_LEVEL_6 = "FundLevel6";
            public static final String FUND_LEVEL_7 = "FundLevel7";
            public static final String FUND_LEVEL_8 = "FundLevel8";
            public static final String FUND_LEVEL_9 = "FundLevel9";
            public static final String FUND_LEVEL_10 = "FundLevel10";
            public static final String LEVEL_0 = "Level0";
            public static final String LEVEL_1 = "Level1";
            public static final String LEVEL_2 = "Level2";
            public static final String LEVEL_3 = "Level3";
            public static final String LEVEL_4 = "Level4";
            public static final String LEVEL_5 = "Level5";
            public static final String LEVEL_6 = "Level6";
            public static final String LEVEL_7 = "Level7";
            public static final String LEVEL_8 = "Level8";
            public static final String LEVEL_9 = "Level9";
            public static final String LEVEL_10 = "Level10";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath PORTFOLIO = FieldPath.of(Fields.PORTFOLIO);
            public static final FieldPath HOLDING_ID = FieldPath.of(Fields.HOLDING_ID);
            public static final FieldPath HOLDING_UNIQUE_NAME = FieldPath.of(Fields.HOLDING_UNIQUE_NAME);
            public static final FieldPath BASE_HOLDING_UNIQUE_NAME = FieldPath.of(Fields.BASE_HOLDING_UNIQUE_NAME);
            public static final FieldPath BASE_HOLDING_ID = FieldPath.of(Fields.BASE_HOLDING_ID);
            public static final FieldPath AMOUNT = FieldPath.of(Fields.AMOUNT);
            public static final FieldPath IS_RELATIVE = FieldPath.of(Fields.IS_RELATIVE);
            public static final FieldPath IS_BLEND = FieldPath.of(Fields.IS_BLEND);
            public static final FieldPath IS_FILTER = FieldPath.of(Fields.IS_FILTER);
            public static final FieldPath IS_SHARECLASS_ARTIFICIAL = FieldPath.of(Fields.IS_SHARECLASS_ARTIFICIAL);
            public static final FieldPath IS_HOLDING_GROUP_BY_WEIGHTED_HOLDING =
                    FieldPath.of(Fields.IS_HOLDING_GROUP_BY_WEIGHTED_HOLDING);
            public static final FieldPath BASE_PORTFOLIO = FieldPath.of(Fields.BASE_PORTFOLIO);
            public static final FieldPath IS_SHORT_BASE = FieldPath.of(Fields.IS_SHORT_BASE);
            public static final FieldPath BENCHMARK_PORTFOLIO = FieldPath.of(Fields.BENCHMARK_PORTFOLIO);
            public static final FieldPath BENCHMARK_METHOD = FieldPath.of(Fields.BENCHMARK_METHOD);
            public static final FieldPath BASE_BENCHMARK = FieldPath.of(Fields.BASE_BENCHMARK);
            public static final FieldPath FX_HEDGING = FieldPath.of(Fields.FX_HEDGING);
            public static final FieldPath RAW_FX_HEDGING = FieldPath.of(Fields.RAW_FX_HEDGING);
            public static final FieldPath BLEND_LOAD_STATUS = FieldPath.of(Fields.BLEND_LOAD_STATUS);
            public static final FieldPath BLEND_LOAD_MESSAGE = FieldPath.of(Fields.BLEND_LOAD_MESSAGE);

            public static final FieldPath LOOKTHROUGH_PORTFOLIO = FieldPath.of(Fields.LOOKTHROUGH_PORTFOLIO);
            public static final FieldPath LOOKTHROUGH_LOAD_STATUS = FieldPath.of(Fields.LOOKTHROUGH_LOAD_STATUS);
            public static final FieldPath LOOKTHROUGH_LOAD_MESSAGE = FieldPath.of(Fields.LOOKTHROUGH_LOAD_MESSAGE);

            public static final FieldPath PROCESS_DATE = FieldPath.of(Fields.PROCESS_DATE);
            public static final FieldPath HOLDING_UPDATE_TIMESTAMP = FieldPath.of(Fields.HOLDING_UPDATE_TIMESTAMP);
            public static final FieldPath HOLDING_SOURCE = FieldPath.of(Fields.HOLDING_SOURCE);
            public static final FieldPath PARTITION_KEY = FieldPath.of(Fields.PARTITION_KEY);
            public static final FieldPath FUND_LEVEL_0 = FieldPath.of(Fields.FUND_LEVEL_0);
            public static final FieldPath FUND_LEVEL_1 = FieldPath.of(Fields.FUND_LEVEL_1);
            public static final FieldPath FUND_LEVEL_2 = FieldPath.of(Fields.FUND_LEVEL_2);
            public static final FieldPath FUND_LEVEL_3 = FieldPath.of(Fields.FUND_LEVEL_3);
            public static final FieldPath FUND_LEVEL_4 = FieldPath.of(Fields.FUND_LEVEL_4);
            public static final FieldPath FUND_LEVEL_5 = FieldPath.of(Fields.FUND_LEVEL_5);
            public static final FieldPath FUND_LEVEL_6 = FieldPath.of(Fields.FUND_LEVEL_6);
            public static final FieldPath FUND_LEVEL_7 = FieldPath.of(Fields.FUND_LEVEL_7);
            public static final FieldPath FUND_LEVEL_8 = FieldPath.of(Fields.FUND_LEVEL_8);
            public static final FieldPath FUND_LEVEL_9 = FieldPath.of(Fields.FUND_LEVEL_9);
            public static final FieldPath FUND_LEVEL_10 = FieldPath.of(Fields.FUND_LEVEL_10);
            public static final FieldPath LEVEL_0 = FieldPath.of(Fields.LEVEL_0);
            public static final FieldPath LEVEL_1 = FieldPath.of(Fields.LEVEL_1);
            public static final FieldPath LEVEL_2 = FieldPath.of(Fields.LEVEL_2);
            public static final FieldPath LEVEL_3 = FieldPath.of(Fields.LEVEL_3);
            public static final FieldPath LEVEL_4 = FieldPath.of(Fields.LEVEL_4);
            public static final FieldPath LEVEL_5 = FieldPath.of(Fields.LEVEL_5);
            public static final FieldPath LEVEL_6 = FieldPath.of(Fields.LEVEL_6);
            public static final FieldPath LEVEL_7 = FieldPath.of(Fields.LEVEL_7);
            public static final FieldPath LEVEL_8 = FieldPath.of(Fields.LEVEL_8);
            public static final FieldPath LEVEL_9 = FieldPath.of(Fields.LEVEL_9);
            public static final FieldPath LEVEL_10 = FieldPath.of(Fields.LEVEL_10);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
            public static final FieldPath SECURITY =
                    FieldPath.of(References.HOLDING_TO_HOLDINGDETAIL, HoldingDetailStore.Fields.SECURITY);
        }
    }

    public static final class AsOfDateStore {
        private AsOfDateStore() {}

        public static final String STORE_NAME = "AsOfDate";

        public static final class Fields {
            private Fields() {}

            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
            public static final String RETENTION_TYPE = "RetentionType";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
            public static final FieldPath RETENTION_TYPE = FieldPath.of(Fields.RETENTION_TYPE);
        }

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.AsOfDateStore.STORE_NAME)
                    .withField(DatastoreConstants.AsOfDateStore.Fields.RETENTION_TYPE, STRING)
                    .withField(DatastoreConstants.AsOfDateStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withIndexOn(DatastoreConstants.AsOfDateStore.Fields.AS_OF_DATE)
                    .build();
        }
    }

    public static final class HoldingDetailStore {
        private HoldingDetailStore() {}

        public static IStoreDescription storeDescription(
                DatabaseProperties.InMemoryStoresProperties inMemoryStoresProperties) {
            var builderWithValuePartitioningOn = StartBuilding.store()
                    .withStoreName(DatastoreConstants.HoldingDetailStore.STORE_NAME)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.BASE_HOLDING_ID, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.PARTITION_KEY, INT, -1)
                    .asKeyField()
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.HOLDING_NAME, STRING)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.PRICED_SECURITY_NAME, STRING)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.PROXY_SECURITY_NAME, STRING)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.SECURITY, STRING)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.HAIRCUT, DOUBLE)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.HOSTNAME, STRING)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.LOADING_STATUS, STRING)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.LOADING_MESSAGE, STRING)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.ORIGINAL_AMOUNT, DOUBLE)
                    // FIXED VALUES FOR POC
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.CLIENT_MARKET_VALUE, DOUBLE)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.CLIENT_MARKET_VALUE_CCY, STRING)
                    .withField(DatastoreConstants.HoldingDetailStore.Fields.RMG_PROXYSECURITYNAME, STRING)
                    .withIndexOn(
                            DatastoreConstants.HoldingDetailStore.Fields.SECURITY,
                            DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE)
                    .withValuePartitioningOn(DatastoreConstants.HoldingDetailStore.Fields.PARTITION_KEY);

            if (inMemoryStoresProperties.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.HoldingDetailStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final String STORE_NAME = "HoldingDetail";

        public static final class Fields {
            private Fields() {}

            public static final String BASE_HOLDING_ID = "BaseHoldingId";
            public static final String HOLDING_NAME = FieldConstants.HOLDING_NAME;
            public static final String PRICED_SECURITY_NAME = FieldConstants.PRICED_SECURITY_NAME;
            public static final String PROXY_SECURITY_NAME = FieldConstants.PROXY_SECURITY;
            public static final String SECURITY = FieldConstants.SECURITY;
            public static final String AMOUNT = "Amount";
            public static final String ORIGINAL_AMOUNT = "OriginalAmount";
            public static final String HAIRCUT = FieldConstants.HAIRCUT;
            public static final String HOSTNAME = "HostName";
            public static final String LOADING_STATUS = FieldConstants.LOADING_STATUS;
            public static final String LOADING_MESSAGE = "LoadingMessages";
            public static final String PARTITION_KEY = DatastoreConstants.PARTITION_KEY;
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;

            // THESE ARE ADDED FOR THE POC
            public static final String CLIENT_MARKET_VALUE = "clientMarketValue";
            public static final String CLIENT_MARKET_VALUE_CCY = "clientMarketValueCCY";
            public static final String RMG_PROXYSECURITYNAME = "RMG_ProxySecurityName";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath BASE_HOLDING_ID = FieldPath.of(Fields.BASE_HOLDING_ID);
            public static final FieldPath HOLDING_NAME = FieldPath.of(Fields.HOLDING_NAME);
            public static final FieldPath PRICED_SECURITY_NAME = FieldPath.of(Fields.PRICED_SECURITY_NAME);
            public static final FieldPath PROXY_SECURITY = FieldPath.of(Fields.PROXY_SECURITY_NAME);
            public static final FieldPath SECURITY = FieldPath.of(Fields.SECURITY);
            public static final FieldPath AMOUNT = FieldPath.of(Fields.AMOUNT);
            public static final FieldPath ORIGINAL_AMOUNT = FieldPath.of(Fields.ORIGINAL_AMOUNT);
            public static final FieldPath HAIRCUT = FieldPath.of(Fields.HAIRCUT);
            public static final FieldPath HOSTNAME = FieldPath.of(Fields.HOSTNAME);
            public static final FieldPath LOADING_STATUS = FieldPath.of(Fields.LOADING_STATUS);
            public static final FieldPath LOADING_MESSAGE = FieldPath.of(Fields.LOADING_MESSAGE);
            public static final FieldPath PARTITION_KEY = FieldPath.of(Fields.PARTITION_KEY);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);

            // THESE ARE ADDED FOR THE POC
            public static final FieldPath CLIENT_MARKET_VALUE = FieldPath.of(Fields.CLIENT_MARKET_VALUE);
            public static final FieldPath CLIENT_MARKET_VALUE_CCY = FieldPath.of(Fields.CLIENT_MARKET_VALUE_CCY);
            public static final FieldPath RMG_PROXYSECURITYNAME = FieldPath.of(Fields.RMG_PROXYSECURITYNAME);
        }
    }

    public static final class DimensionLevelAttributeStore {
        private DimensionLevelAttributeStore() {}

        public static final String STORE_NAME = "DimensionLevelAttribute";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.DimensionLevelAttributeStore.STORE_NAME)
                    .withField(DatastoreConstants.DimensionLevelAttributeStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(
                            DatastoreConstants.DimensionLevelAttributeStore.Fields.LEVEL_MEMBERS,
                            STRING) // FIXME: OBJECT
                    .asKeyField()
                    .withField(DatastoreConstants.DimensionLevelAttributeStore.Fields.MEASURE_NAME, STRING)
                    .asKeyField()
                    .withField(
                            DatastoreConstants.DimensionLevelAttributeStore.Fields.MEASURE_VALUE,
                            STRING) // FIXME: OBJECT
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
            public static final String LEVEL_MEMBERS = "LevelMembers";
            public static final String MEASURE_NAME = "MeasureName";
            public static final String MEASURE_VALUE = "MeasureValue";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
            public static final FieldPath LEVEL_MEMBERS = FieldPath.of(Fields.LEVEL_MEMBERS);
            public static final FieldPath MEASURE_NAME = FieldPath.of(Fields.MEASURE_NAME);
            public static final FieldPath MEASURE_VALUE = FieldPath.of(Fields.MEASURE_VALUE);
        }
    }

    public static final class ScaledStatResultStore {
        private ScaledStatResultStore() {}

        public static final String STORE_NAME = "ScaledStatResult";

        public static IStoreDescription storeDescription(
                DatabaseProperties.InMemoryStoresProperties inMemoryStoresProperties) {
            var builderWithValuePartitioningOn = StartBuilding.store()
                    .withStoreName(DatastoreConstants.ScaledStatResultStore.STORE_NAME)
                    .withField(DatastoreConstants.ScaledStatResultStore.Fields.HOLDING_ID)
                    .asKeyField()
                    .withField(DatastoreConstants.ScaledStatResultStore.Fields.PARTITION_KEY, INT, -1)
                    .asKeyField()
                    .withField(DatastoreConstants.ScaledStatResultStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    // FIXED VALUES FOR POC: what is the vector size?
                    .withVectorField(DatastoreConstants.ScaledStatResultStore.Fields.RESULT_VALUES_SUM, DOUBLE)
                    .withVectorBlockSize(inMemoryStoresProperties.getVectorSize())
                    .withVectorField(DatastoreConstants.ScaledStatResultStore.Fields.RESULT_VALUES_PASSTHROUGH, DOUBLE)
                    .withVectorBlockSize(inMemoryStoresProperties.getVectorSize())
                    .withVectorField("AGGSVC_SIMRETURNS(1Y VS)_MONTECARLO", DOUBLE)
                    .withVectorBlockSize(inMemoryStoresProperties.getVectorSize())
                    .withIndexOn(DatastoreConstants.ScaledStatResultStore.Fields.AS_OF_DATE)
                    .withValuePartitioningOn(DatastoreConstants.ScaledStatResultStore.Fields.PARTITION_KEY);

            if (inMemoryStoresProperties.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.ScaledStatResultStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final String[] RESULT_FIELDS =
                new String[] {Fields.RESULT_VALUES_SUM, Fields.RESULT_VALUES_PASSTHROUGH};

        public static final class Fields {
            private Fields() {}

            public static final String HOLDING_ID = FieldConstants.HOLDING_ID;
            public static final String PARTITION_KEY = DatastoreConstants.PARTITION_KEY;
            public static final String RESULT_VALUES_SUM = "ResultValuesSum";
            public static final String RESULT_VALUES_PASSTHROUGH = "ResultValuesPassthrough";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath HOLDING_ID = FieldPath.of(Fields.HOLDING_ID);
            public static final FieldPath PARTITION_KEY = FieldPath.of(Fields.PARTITION_KEY);
            public static final FieldPath RESULT_VALUES_SUM = FieldPath.of(Fields.RESULT_VALUES_SUM);
            public static final FieldPath RESULT_VALUES_PASSTHROUGH = FieldPath.of(Fields.RESULT_VALUES_PASSTHROUGH);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
        }
    }

    public static final class SecurityStore {
        private SecurityStore() {}

        public static IStoreDescription storeDescription(
                DatabaseProperties.InMemoryStoresProperties inMemoryStoresProperties) {
            var builderWithValuePartitioningOn = StartBuilding.store()
                    .withStoreName(DatastoreConstants.SecurityStore.STORE_NAME)
                    .withField(DatastoreConstants.SecurityStore.Fields.SECURITY_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.SecurityStore.Fields.PARTITION_KEY, INT, -1)
                    .asKeyField()
                    .withField(DatastoreConstants.SecurityStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.SecurityStore.Fields.FX_HEDGING, DOUBLE, Double.NaN)
                    // FIXED VALUES FOR POC

                    .withField(DatastoreConstants.SecurityStore.Fields.ENRICHMENT_STATUS, STRING)
                    .withField(DatastoreConstants.SecurityStore.Fields.INPUT_MODEL, STRING)
                    .withField(DatastoreConstants.SecurityStore.Fields.OUTPUT_MODEL, STRING)
                    .withField(DatastoreConstants.SecurityStore.Fields.ENRICHMENT_INPUT_ID, STRING)
                    .withField(DatastoreConstants.SecurityStore.Fields.ENRICHMENT_INPUT_ID_TYPE, STRING)
                    .withField(DatastoreConstants.SecurityStore.Fields.ENRICHMENT_MESSAGE, STRING)
                    .withField(DatastoreConstants.SecurityStore.Fields.RMG_SECURITY_IS_PROXIED, BOOLEAN)
                    .withField(
                            DatastoreConstants.SecurityStore.Fields.SECURITY_UPDATE_TIMESTAMP,
                            STRING) // FIXME: DATE_TIME?
                    //  .withField(DatastoreConstants.SecurityStore.Fields.ENRICHED_RML, STRING)
                    .withField(DatastoreConstants.SecurityStore.Fields.IS_FX_MODEL, BOOLEAN)
                    .withIndexOn(DatastoreConstants.SecurityStore.Fields.AS_OF_DATE)
                    .updateOnlyIfDifferent()
                    .withValuePartitioningOn(DatastoreConstants.SecurityStore.Fields.PARTITION_KEY);

            if (inMemoryStoresProperties.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.SecurityStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final String STORE_NAME = "Security";

        public static final class Fields {
            private Fields() {}

            public static final String SECURITY_NAME = "pricedSecurityName";
            public static final String FX_HEDGING = "FxHedgingPercent";
            public static final String ENRICHMENT_STATUS = "EnrichmentStatus";
            public static final String INPUT_MODEL = "InputModel";
            public static final String OUTPUT_MODEL = "OutputModel";
            public static final String ENRICHMENT_INPUT_ID = "EnrichmentInputId";
            public static final String ENRICHMENT_INPUT_ID_TYPE = "EnrichmentInputIdType";
            public static final String ENRICHMENT_MESSAGE = "EnrichmentMessages";
            public static final String RMG_SECURITY_IS_PROXIED = "RMG_SecurityIsProxied";
            public static final String PARTITION_KEY = DatastoreConstants.PARTITION_KEY;
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
            public static final String SECURITY_UPDATE_TIMESTAMP = "SecurityUpdateTimestamp";
            public static final String ENRICHED_RML = "EnrichedRML";
            public static final String IS_FX_MODEL = FieldConstants.IS_FX_MODEL;

            // FOR POC
            public static final String VECTOR = "Vector";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath SECURITY_NAME = FieldPath.of(Fields.SECURITY_NAME);
            public static final FieldPath FX_HEDGING = FieldPath.of(Fields.FX_HEDGING);
            public static final FieldPath ENRICHMENT_STATUS = FieldPath.of(Fields.ENRICHMENT_STATUS);
            public static final FieldPath INPUT_MODEL = FieldPath.of(Fields.INPUT_MODEL);
            public static final FieldPath OUTPUT_MODEL = FieldPath.of(Fields.OUTPUT_MODEL);
            public static final FieldPath ENRICHMENT_INPUT_ID = FieldPath.of(Fields.ENRICHMENT_INPUT_ID);
            public static final FieldPath ENRICHMENT_INPUT_ID_TYPE = FieldPath.of(Fields.ENRICHMENT_INPUT_ID_TYPE);
            public static final FieldPath ENRICHMENT_MESSAGE = FieldPath.of(Fields.ENRICHMENT_MESSAGE);
            public static final FieldPath RMG_SECURITY_IS_PROXIED = FieldPath.of(Fields.RMG_SECURITY_IS_PROXIED);
            public static final FieldPath PARTITION_KEY = FieldPath.of(Fields.PARTITION_KEY);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
            public static final FieldPath SECURITY_UPDATE_TIMESTAMP = FieldPath.of(Fields.SECURITY_UPDATE_TIMESTAMP);
            public static final FieldPath ENRICHED_RML = FieldPath.of(Fields.ENRICHED_RML);
            public static final FieldPath IS_FX_MODEL = FieldPath.of(Fields.IS_FX_MODEL);
        }
    }

    public static final class PositionDetailStore {
        private PositionDetailStore() {}

        public static final String STORE_NAME = "PositionDetail";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.PositionDetailStore.STORE_NAME)
                    .withField(DatastoreConstants.PositionDetailStore.Fields.SECURITY_NAME, STRING)
                    .asKeyField()

                    // FIXED VALUES FOR POC
                    .withField(DatastoreConstants.PositionDetailStore.Fields.DELTA, STRING)
                    .withField(DatastoreConstants.PositionDetailStore.Fields.GAMMA, STRING)
                    .withField(DatastoreConstants.PositionDetailStore.Fields.CURRENCY, STRING)
                    .withField(DatastoreConstants.PositionDetailStore.Fields.EQUITY_DOMICILE_CURRENCY, STRING)
                    .withField(DatastoreConstants.PositionDetailStore.Fields.CALIBRATED_MODEL_CURRENCIES, STRING)
                    .withField(DatastoreConstants.PositionDetailStore.Fields.POSITION_TYPE, STRING)
                    .withField(DatastoreConstants.PositionDetailStore.Fields.CALIBRATED_MODEL_TYPE, STRING)
                    .updateOnlyIfDifferent()
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String SECURITY_NAME = SecurityStore.Fields.SECURITY_NAME;
            public static final String CURRENCY = "currency";
            public static final String POSITION_TYPE = "positionType";
            public static final String LONG_SHORT = FieldConstants.LONG_SHORT;
            public static final String EQUITY_DOMICILE_CURRENCY = "equityDomicileCcy";
            public static final String CALIBRATED_MODEL_CURRENCIES = "allFxCcy";
            public static final String CALIBRATED_MODEL_TYPE = "calibratedModelType";

            // FOR POC
            public static final String DELTA = "delta";
            public static final String GAMMA = "gamma";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath SECURITY_NAME = FieldPath.of(Fields.SECURITY_NAME);
            public static final FieldPath CURRENCY = FieldPath.of(Fields.CURRENCY);
            public static final FieldPath POSITION_TYPE = FieldPath.of(Fields.POSITION_TYPE);
            public static final FieldPath LONG_SHORT = FieldPath.of(Fields.LONG_SHORT);
            public static final FieldPath EQUITY_DOMICILE_CURRENCY = FieldPath.of(Fields.EQUITY_DOMICILE_CURRENCY);
            public static final FieldPath CALIBRATED_MODEL_CURRENCIES =
                    FieldPath.of(Fields.CALIBRATED_MODEL_CURRENCIES);
            public static final FieldPath CALIBRATED_MODEL_TYPE = FieldPath.of(Fields.CALIBRATED_MODEL_TYPE);
        }
    }

    public static final class StatResultsStore {
        private StatResultsStore() {}

        public static final String STORE_NAME = "StatResults";
        public static final String[] RESULT_FIELDS =
                new String[] {Fields.RESULT_VALUES_SUM, Fields.RESULT_VALUES_PASSTHROUGH};

        public static IStoreDescription storeDescription(DatabaseProperties.InMemoryStoresProperties inMemoryStore) {
            var builderWithValuePartitioningOn = StartBuilding.store()
                    .withStoreName(DatastoreConstants.StatResultsStore.STORE_NAME)
                    .withField(DatastoreConstants.StatResultsStore.Fields.SECURITY_NAME)
                    .asKeyField()
                    .withField(DatastoreConstants.StatResultsStore.Fields.VALSPEC)
                    .asKeyField()
                    .withField(DatastoreConstants.StatResultsStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.StatResultsStore.Fields.RESULT_TYPE)
                    .asKeyField()

                    // FIXED FIELDS FOR POC
                    .withVectorField(DatastoreConstants.StatResultsStore.Fields.RESULT_VALUES_SUM, DOUBLE)
                    .withVectorBlockSize(inMemoryStore.getVectorSize())
                    .withVectorField(DatastoreConstants.StatResultsStore.Fields.RESULT_VALUES_PASSTHROUGH, DOUBLE)
                    .withVectorBlockSize(inMemoryStore.getVectorSize())
                    .updateOnlyIfDifferent()

                    // hash partitioning will create 1 partition per core for even distribution
                    .withModuloPartitioning(partitionCount(), DatastoreConstants.StatResultsStore.Fields.SECURITY_NAME)
                    .withIndexOn(
                            DatastoreConstants.StatResultsStore.Fields.SECURITY_NAME,
                            DatastoreConstants.StatResultsStore.Fields.AS_OF_DATE);

            if (inMemoryStore.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.StatResultsStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String SECURITY_NAME = DatastoreConstants.SECURITY_NAME;
            public static final String VALSPEC = "Valspec";
            public static final String RESULT_TYPE = "ResultType";
            public static final String RESULT_VALUES_SUM = "ResultValuesSum";
            public static final String RESULT_VALUES_PASSTHROUGH = "ResultValuesPassthrough";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath SECURITY_NAME = FieldPath.of(Fields.SECURITY_NAME);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
            public static final FieldPath RESULT_TYPE = FieldPath.of(Fields.RESULT_TYPE);
        }
    }

    private static int partitionCount() {
        return 5;
    }

    public static final class SimReturnsStore {

        private SimReturnsStore() {}

        public static final String STORE_NAME = "SimReturns";

        public static IStoreDescription storeDescription(DatabaseProperties.InMemoryStoresProperties inMemoryStore) {
            var builderWithValuePartitioningOn = StartBuilding.store()
                    .withStoreName(DatastoreConstants.SimReturnsStore.STORE_NAME)
                    .withField(DatastoreConstants.SimReturnsStore.Fields.SECURITY_NAME, STRING)
                    .asKeyField()
                    .withNullableField(DatastoreConstants.SimReturnsStore.Fields.ENGINE_MASK, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.SimReturnsStore.Fields.STAT_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.SimReturnsStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()

                    // FIXED FIELDS FOR POC
                    .withVectorField(DatastoreConstants.SecurityStore.Fields.VECTOR, DOUBLE)
                    .withVectorBlockSize(inMemoryStore.getVectorSize())
                    .withIndexOn(DatastoreConstants.SimReturnsStore.Fields.SECURITY_NAME)
                    .updateOnlyIfDifferent()
                    .withModuloPartitioning(partitionCount(), DatastoreConstants.SimReturnsStore.Fields.SECURITY_NAME);

            if (inMemoryStore.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.SimReturnsStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String SECURITY_NAME = DatastoreConstants.SECURITY_NAME;
            public static final String ENGINE_MASK = "DimensionsMask";
            public static final String STAT_NAME = DatastoreConstants.STAT_NAME;
            public static final String VECTOR = "Vector";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath SECURITY_NAME = FieldPath.of(Fields.SECURITY_NAME);
            public static final FieldPath ENGINE_MASK = FieldPath.of(Fields.ENGINE_MASK);
            public static final FieldPath STAT_NAME = FieldPath.of(Fields.STAT_NAME);
            public static final FieldPath VECTOR = FieldPath.of(Fields.VECTOR);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
        }
    }

    public static final class HistoricalSimReturnDatesStore {
        private HistoricalSimReturnDatesStore() {}

        public static final String STORE_NAME = "HistoricalSimReturnDates";

        public static IStoreDescription storeDescription(DatabaseProperties.InMemoryStoresProperties inMemoryStore) {
            var builderWithValuePartitioningOn = StartBuilding.store()
                    .withStoreName(DatastoreConstants.HistoricalSimReturnDatesStore.STORE_NAME)
                    .withField(DatastoreConstants.HistoricalSimReturnDatesStore.Fields.STAT_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.HistoricalSimReturnDatesStore.Fields.HISTORICAL_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.HistoricalSimReturnDatesStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.HistoricalSimReturnDatesStore.Fields.VECTOR_INDEX, INT)
                    .updateOnlyIfDifferent()
                    .withModuloPartitioning(partitionCount(), DatastoreConstants.SimReturnsStore.Fields.STAT_NAME);

            if (inMemoryStore.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.HistoricalSimReturnDatesStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String STAT_NAME = SimReturnsStore.Fields.STAT_NAME;
            public static final String HISTORICAL_DATE = "HistoricalDate";
            public static final String VECTOR_INDEX = "VectorIndex";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath STAT_NAME = FieldPath.of(Fields.STAT_NAME);
            public static final FieldPath HISTORICAL_DATE = FieldPath.of(Fields.HISTORICAL_DATE);
            public static final FieldPath VECTOR_INDEX = FieldPath.of(Fields.VECTOR_INDEX);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
        }
    }

    public static final class FxResultsStore {
        private FxResultsStore() {}

        public static final String STORE_NAME = "FxResults";

        public static IStoreDescription storeDescription(DatabaseProperties.InMemoryStoresProperties inMemoryStore) {
            var builderWithValuePartitioningOn = StartBuilding.store()
                    .withStoreName(DatastoreConstants.FxResultsStore.STORE_NAME)
                    .withField(DatastoreConstants.FxResultsStore.Fields.SECURITY_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.FxResultsStore.Fields.CURRENCY, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.FxResultsStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    // FIXED FIELDS FOR POC: there are no extra fields

                    .updateOnlyIfDifferent()
                    .withModuloPartitioning(partitionCount(), DatastoreConstants.FxResultsStore.Fields.SECURITY_NAME);

            if (inMemoryStore.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.FxResultsStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String SECURITY_NAME = DatastoreConstants.SECURITY_NAME;
            public static final String CURRENCY = "Currency";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath SECURITY_NAME = FieldPath.of(Fields.SECURITY_NAME);
            public static final FieldPath CURRENCY = FieldPath.of(Fields.CURRENCY);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
        }
    }

    public static final class FxEquivalentsStore {
        private FxEquivalentsStore() {}

        public static final String STORE_NAME = "FxEquivalents";

        public static IStoreDescription storeDescription(DatabaseProperties.InMemoryStoresProperties inMemoryStore) {
            var builderWithValuePartitioningOn = StartBuilding.store()
                    .withStoreName(DatastoreConstants.FxEquivalentsStore.STORE_NAME)
                    .withField(DatastoreConstants.FxEquivalentsStore.Fields.SECURITY_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.FxEquivalentsStore.Fields.RISK_CURRENCY, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.FxEquivalentsStore.Fields.STAT_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.FxEquivalentsStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    // FIXED FOR POC
                    .withVectorField(DatastoreConstants.FxEquivalentsStore.Fields.DELTA_EQUIVALENTS, DOUBLE)
                    .withVectorBlockSize(inMemoryStore.getVectorSize())
                    .withIndexOn(DatastoreConstants.FxEquivalentsStore.Fields.SECURITY_NAME)
                    .updateOnlyIfDifferent()
                    .withModuloPartitioning(
                            partitionCount(), DatastoreConstants.FxEquivalentsStore.Fields.SECURITY_NAME);

            if (inMemoryStore.isPartitionAsOfDate()) {
                builderWithValuePartitioningOn = builderWithValuePartitioningOn.withValuePartitioningOn(
                        DatastoreConstants.FxEquivalentsStore.Fields.AS_OF_DATE);
            }

            return builderWithValuePartitioningOn.build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String SECURITY_NAME = DatastoreConstants.SECURITY_NAME;
            public static final String RISK_CURRENCY = "RiskCurrency";
            public static final String STAT_NAME = DatastoreConstants.STAT_NAME;
            public static final String DELTA_EQUIVALENTS = "DeltaEquivalents";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath SECURITY_NAME = FieldPath.of(Fields.SECURITY_NAME);
            public static final FieldPath RISK_CURRENCY = FieldPath.of(Fields.RISK_CURRENCY);
            public static final FieldPath STAT_NAME = FieldPath.of(Fields.STAT_NAME);
            public static final FieldPath DELTA_EQUIVALENTS = FieldPath.of(Fields.DELTA_EQUIVALENTS);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
        }
    }

    public static final class FxEquivalentsLookupStore {
        private FxEquivalentsLookupStore() {}

        public static final String STORE_NAME = "FxEquivalentsLookup";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.FxEquivalentsLookupStore.STORE_NAME)
                    .withField(DatastoreConstants.FxEquivalentsLookupStore.Fields.STAT_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.FxEquivalentsLookupStore.Fields.PRICING_CURRENCY, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.FxEquivalentsLookupStore.Fields.EQUIVALENTS_INDEX, INT, -1)
                    .updateOnlyIfDifferent()
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String STAT_NAME = DatastoreConstants.STAT_NAME;
            public static final String PRICING_CURRENCY = "PricingCurrency";
            public static final String EQUIVALENTS_INDEX = "EquivalentsIndex";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath STAT_NAME = FieldPath.of(Fields.STAT_NAME);
            public static final FieldPath PRICING_CURRENCY = FieldPath.of(Fields.PRICING_CURRENCY);
            public static final FieldPath EQUIVALENTS_INDEX = FieldPath.of(Fields.EQUIVALENTS_INDEX);
        }
    }

    public static final class StatResultLookupStore {
        private StatResultLookupStore() {}

        public static final String STORE_NAME = "StatResultLookup";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.StatResultLookupStore.STORE_NAME)
                    .withField(DatastoreConstants.StatResultLookupStore.Fields.STAT_NAME, STRING)
                    .asKeyField()
                    .withNullableField(DatastoreConstants.StatResultLookupStore.Fields.DRILLDOWN_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.StatResultLookupStore.Fields.VALSPEC, STRING)
                    .withField(DatastoreConstants.StatResultLookupStore.Fields.STAT_FIELD, STRING)
                    .withField(DatastoreConstants.StatResultLookupStore.Fields.STAT_INDEX, INT, -1)
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String STAT_NAME = DatastoreConstants.STAT_NAME;
            public static final String DRILLDOWN_NAME = "DrilldownName";
            public static final String VALSPEC = "Valspec";
            public static final String STAT_FIELD = "StatField";
            public static final String STAT_INDEX = "StatIndex";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath STAT_NAME = FieldPath.of(Fields.STAT_NAME);
            public static final FieldPath DRILLDOWN_NAME = FieldPath.of(Fields.DRILLDOWN_NAME);
            public static final FieldPath VALSPEC = FieldPath.of(Fields.VALSPEC);
            public static final FieldPath STAT_FIELD = FieldPath.of(Fields.STAT_FIELD);
            public static final FieldPath STAT_INDEX = FieldPath.of(Fields.STAT_INDEX);
        }
    }

    public static final class StatisticBaseCurrencyStore {
        private StatisticBaseCurrencyStore() {}

        public static final String STORE_NAME = "StatisticBaseCurrency";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.StatisticBaseCurrencyStore.STORE_NAME)
                    .withField(DatastoreConstants.StatisticBaseCurrencyStore.Fields.STAT_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.StatisticBaseCurrencyStore.Fields.BASE_CURRENCY, STRING)
                    .updateOnlyIfDifferent()
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String STAT_NAME = DatastoreConstants.STAT_NAME;
            public static final String BASE_CURRENCY = "BaseCurrency";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath STAT_NAME = FieldPath.of(Fields.STAT_NAME);
            public static final FieldPath BASE_CURRENCY = FieldPath.of(Fields.BASE_CURRENCY);
        }
    }

    public static final class EngineDimensionStore {
        private EngineDimensionStore() {}

        public static final String STORE_NAME = "EngineDimension";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.EngineDimensionStore.STORE_NAME)
                    .withField(DatastoreConstants.EngineDimensionStore.Fields.DRILLDOWN_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.EngineDimensionStore.Fields.VALUE, STRING)
                    .asKeyField()
                    .withIndexOn(DatastoreConstants.EngineDimensionStore.Fields.DRILLDOWN_NAME)
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String DRILLDOWN_NAME = "DrilldownName";
            public static final String VALUE = "value";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath DRILLDOWN_NAME = FieldPath.of(Fields.DRILLDOWN_NAME);
            public static final FieldPath VALUE = FieldPath.of(Fields.VALUE);
        }
    }

    public static final class CurrencyStore {
        private CurrencyStore() {}

        public static final String STORE_NAME = "Currency";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.CurrencyStore.STORE_NAME)
                    .withField(DatastoreConstants.CurrencyStore.Fields.CURRENCY_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.CurrencyStore.Fields.SECURITY_NAME, STRING)
                    .withField(DatastoreConstants.CurrencyStore.Fields.REPORTING_CURRENCY, BOOLEAN)
                    .withIndexOn(DatastoreConstants.CurrencyStore.Fields.REPORTING_CURRENCY)
                    .updateOnlyIfDifferent()
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String CURRENCY_NAME = "CurrencyName";
            public static final String SECURITY_NAME = DatastoreConstants.SECURITY_NAME;
            public static final String REPORTING_CURRENCY = "ReportingCurrency";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath CURRENCY_NAME = FieldPath.of(Fields.CURRENCY_NAME);
            public static final FieldPath SECURITY_NAME = FieldPath.of(Fields.SECURITY_NAME);
            public static final FieldPath REPORTING_CURRENCY = FieldPath.of(Fields.REPORTING_CURRENCY);
        }
    }

    public static final class FundLookThroughSecurityStore {
        private FundLookThroughSecurityStore() {}

        public static final String STORE_NAME = "FundLookThroughSecurity";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.FundLookThroughSecurityStore.STORE_NAME)
                    .withField(DatastoreConstants.FundLookThroughSecurityStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.FundLookThroughSecurityStore.Fields.SECURITY, STRING)
                    .asKeyField()
                    .withNullableField(DatastoreConstants.FundLookThroughSecurityStore.Fields.LOAD_ID, STRING)
                    .withField(DatastoreConstants.FundLookThroughSecurityStore.Fields.TYPE, STRING)
                    .withField(DatastoreConstants.FundLookThroughSecurityStore.Fields.QUANTITY, DOUBLE)
                    .withNullableField(DatastoreConstants.FundLookThroughSecurityStore.Fields.PRICE, DOUBLE)
                    .withNullableField(DatastoreConstants.FundLookThroughSecurityStore.Fields.CURRENCY, STRING)
                    .withNullableField(DatastoreConstants.FundLookThroughSecurityStore.Fields.STRIKE_PRICE, DOUBLE)
                    .withField(DatastoreConstants.FundLookThroughSecurityStore.Fields.CONSTITUENT_HOLDING_GROUP, STRING)
                    .withNullableField(
                            DatastoreConstants.FundLookThroughSecurityStore.Fields.CALIBRATION_METHOD, STRING)
                    .withNullableField(
                            DatastoreConstants.FundLookThroughSecurityStore.Fields.RECURSIVE_TAG_OVERRIDES, BOOLEAN)
                    .withNullableField(
                            DatastoreConstants.FundLookThroughSecurityStore.Fields.NO_FURTHER_LOOK_THROUGH, BOOLEAN)
                    .withNullableField(
                            DatastoreConstants.FundLookThroughSecurityStore.Fields.MODEL_LIST_SECURITIES,
                            STRING) // FIXME: OBJECT
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String SECURITY = FieldConstants.SECURITY;
            public static final String TYPE = "Type";
            public static final String QUANTITY = "Quantity";
            public static final String PRICE = "Price";
            public static final String CURRENCY = "Currency";
            public static final String STRIKE_PRICE = "StrikePrice";
            public static final String CONSTITUENT_HOLDING_GROUP = "ConstituentHoldingGroup";
            public static final String CALIBRATION_METHOD = "CalibrationMethod";
            public static final String RECURSIVE_TAG_OVERRIDES = "RecursiveTagOverrides";
            public static final String NO_FURTHER_LOOK_THROUGH = "NoFurtherLookThrough";
            public static final String MODEL_LIST_SECURITIES = "ModelListSecurities";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
            public static final String LOAD_ID = "LoadID";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath SECURITY = FieldPath.of(Fields.SECURITY);
            public static final FieldPath TYPE = FieldPath.of(Fields.TYPE);
            public static final FieldPath QUANTITY = FieldPath.of(Fields.QUANTITY);
            public static final FieldPath PRICE = FieldPath.of(Fields.PRICE);
            public static final FieldPath CURRENCY = FieldPath.of(Fields.CURRENCY);
            public static final FieldPath STRIKE_PRICE = FieldPath.of(Fields.STRIKE_PRICE);
            public static final FieldPath CONSTITUENT_HOLDING_GROUP = FieldPath.of(Fields.CONSTITUENT_HOLDING_GROUP);
            public static final FieldPath CALIBRATION_METHOD = FieldPath.of(Fields.CALIBRATION_METHOD);
            public static final FieldPath RECURSIVE_TAG_OVERRIDES = FieldPath.of(Fields.RECURSIVE_TAG_OVERRIDES);
            public static final FieldPath NO_FURTHER_LOOK_THROUGH = FieldPath.of(Fields.NO_FURTHER_LOOK_THROUGH);
            public static final FieldPath MODEL_LIST_SECURITIES = FieldPath.of(Fields.MODEL_LIST_SECURITIES);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
        }
    }

    public static final class EquityLookThroughSecurityStore {
        private EquityLookThroughSecurityStore() {}

        public static final String STORE_NAME = "EquityLookThroughSecurity";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.EquityLookThroughSecurityStore.STORE_NAME)
                    .withField(DatastoreConstants.EquityLookThroughSecurityStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.EquityLookThroughSecurityStore.Fields.SECURITY, STRING)
                    .asKeyField()
                    .withNullableField(DatastoreConstants.EquityLookThroughSecurityStore.Fields.LOAD_ID, STRING)
                    .withField(DatastoreConstants.EquityLookThroughSecurityStore.Fields.TYPE, STRING)
                    .withField(DatastoreConstants.EquityLookThroughSecurityStore.Fields.QUANTITY, DOUBLE)
                    .withNullableField(DatastoreConstants.EquityLookThroughSecurityStore.Fields.PRICE, DOUBLE)
                    .withNullableField(DatastoreConstants.EquityLookThroughSecurityStore.Fields.CURRENCY, STRING)
                    .withField(DatastoreConstants.EquityLookThroughSecurityStore.Fields.LOOK_THROUGH_NAME, STRING)
                    .withNullableField(
                            DatastoreConstants.EquityLookThroughSecurityStore.Fields.LOOK_THROUGH_SCALING_TYPE, STRING)
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String SECURITY = FieldConstants.SECURITY;
            public static final String TYPE = "Type";
            public static final String QUANTITY = "Quantity";
            public static final String PRICE = "Price";
            public static final String CURRENCY = "Currency";
            public static final String LOOK_THROUGH_NAME = "LookThroughName";
            public static final String LOOK_THROUGH_SCALING_TYPE = "LookThroughScalingType";
            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
            public static final String LOAD_ID = "LoadID";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath SECURITY = FieldPath.of(Fields.SECURITY);
            public static final FieldPath TYPE = FieldPath.of(Fields.TYPE);
            public static final FieldPath QUANTITY = FieldPath.of(Fields.QUANTITY);
            public static final FieldPath PRICE = FieldPath.of(Fields.PRICE);
            public static final FieldPath CURRENCY = FieldPath.of(Fields.CURRENCY);
            public static final FieldPath LOOK_THROUGH_NAME = FieldPath.of(Fields.LOOK_THROUGH_NAME);
            public static final FieldPath LOOK_THROUGH_SCALING_TYPE = FieldPath.of(Fields.LOOK_THROUGH_SCALING_TYPE);
            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
        }
    }

    public static final class EquityFuturesLookThroughSecurityStore {
        private EquityFuturesLookThroughSecurityStore() {}

        public static final String STORE_NAME = "EquityFuturesLookThroughSecurity";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.EquityFuturesLookThroughSecurityStore.STORE_NAME)
                    .withField(DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.SECURITY, STRING)
                    .asKeyField()
                    .withNullableField(DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.LOAD_ID, STRING)
                    .withField(DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.TYPE, STRING)
                    .withField(DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.QUANTITY, DOUBLE)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.EQUITY_INDEX_PRICE, DOUBLE)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.EQUITY_INDEX_NAME, STRING)
                    .withField(DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.CURRENCY, STRING)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.ENTRY_PRICE, DOUBLE)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.FUTURES_PRICE, DOUBLE)
                    .withField(DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.CONTRACT_SIZE, DOUBLE)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.DIVIDEND_YIELD, DOUBLE)
                    .withField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.FUTURES_EXPIRATION_DATE,
                            STRING)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.FUTURES_FINANCING_CURVE,
                            STRING)
                    .withField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.CONSTITUENT_HOLDING_GROUP,
                            STRING)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.RECURSIVE_TAG_OVERRIDES,
                            BOOLEAN)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.NO_FURTHER_LOOK_THROUGH,
                            BOOLEAN)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.IGNORE_FX_RISK, BOOLEAN)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.ALLOW_NEGATIVE_RATES,
                            BOOLEAN)
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields
                                    .HOLDING_EQUITY_FUTURE_SECURITY_MAP,
                            STRING) // FIXME: OBJECT
                    .withNullableField(
                            DatastoreConstants.EquityFuturesLookThroughSecurityStore.Fields.HOLDING_SCALING_FACTOR_MAP,
                            STRING) // FIXME: OBJECT
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
            public static final String SECURITY = FieldConstants.SECURITY;
            public static final String LOAD_ID = "LoadID";
            public static final String TYPE = "Type";
            public static final String QUANTITY = "Quantity";
            public static final String EQUITY_INDEX_PRICE = "EquityIndexPrice";
            public static final String EQUITY_INDEX_NAME = "EquityIndexName";
            public static final String CURRENCY = "Currency";
            public static final String ENTRY_PRICE = "EntryPrice";
            public static final String FUTURES_PRICE = "FuturesPrice";
            public static final String CONTRACT_SIZE = "ContractSize";
            public static final String DIVIDEND_YIELD = "DividendYield";
            public static final String FUTURES_EXPIRATION_DATE = "FuturesExpirationDate";
            public static final String FUTURES_FINANCING_CURVE = "FuturesFinancingCurve";
            public static final String CONSTITUENT_HOLDING_GROUP = "ConstituentHoldingGroup";
            public static final String RECURSIVE_TAG_OVERRIDES = "RecursiveTagOverrides";
            public static final String NO_FURTHER_LOOK_THROUGH = "NoFurtherLookThrough";
            public static final String IGNORE_FX_RISK = "IgnoreFXRisk";
            public static final String ALLOW_NEGATIVE_RATES = "AllowNegativeRates";
            public static final String HOLDING_EQUITY_FUTURE_SECURITY_MAP = "HoldingEquityFutureSecurityMap";
            public static final String HOLDING_SCALING_FACTOR_MAP = "HoldingScalingFactorMap";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
            public static final FieldPath SECURITY = FieldPath.of(Fields.SECURITY);
            public static final FieldPath TYPE = FieldPath.of(Fields.TYPE);
            public static final FieldPath QUANTITY = FieldPath.of(Fields.QUANTITY);
            public static final FieldPath EQUITY_INDEX_PRICE = FieldPath.of(Fields.EQUITY_INDEX_PRICE);
            public static final FieldPath EQUITY_INDEX_NAME = FieldPath.of(Fields.EQUITY_INDEX_NAME);
            public static final FieldPath CURRENCY = FieldPath.of(Fields.CURRENCY);
            public static final FieldPath ENTRY_PRICE = FieldPath.of(Fields.ENTRY_PRICE);
            public static final FieldPath FUTURES_PRICE = FieldPath.of(Fields.FUTURES_PRICE);
            public static final FieldPath CONTRACT_SIZE = FieldPath.of(Fields.CONTRACT_SIZE);
            public static final FieldPath DIVIDEND_YIELD = FieldPath.of(Fields.DIVIDEND_YIELD);
            public static final FieldPath FUTURES_EXPIRATION_DATE = FieldPath.of(Fields.FUTURES_EXPIRATION_DATE);
            public static final FieldPath FUTURES_FINANCING_CURVE = FieldPath.of(Fields.FUTURES_FINANCING_CURVE);
            public static final FieldPath CONSTITUENT_HOLDING_GROUP = FieldPath.of(Fields.CONSTITUENT_HOLDING_GROUP);
            public static final FieldPath RECURSIVE_TAG_OVERRIDES = FieldPath.of(Fields.RECURSIVE_TAG_OVERRIDES);
            public static final FieldPath NO_FURTHER_LOOK_THROUGH = FieldPath.of(Fields.NO_FURTHER_LOOK_THROUGH);
            public static final FieldPath IGNORE_FX_RISK = FieldPath.of(Fields.IGNORE_FX_RISK);
            public static final FieldPath ALLOW_NEGATIVE_RATES = FieldPath.of(Fields.ALLOW_NEGATIVE_RATES);
            public static final FieldPath HOLDING_EQUITY_FUTURE_SECURITY_MAP =
                    FieldPath.of(Fields.HOLDING_EQUITY_FUTURE_SECURITY_MAP);
            public static final FieldPath HOLDING_SCALING_FACTOR_MAP = FieldPath.of(Fields.HOLDING_SCALING_FACTOR_MAP);
        }
    }

    public static final class StatFxAttributesStore {
        private StatFxAttributesStore() {}

        public static final String STORE_NAME = "StatFxAttributes";

        public static IStoreDescription storeDescription() {
            return StartBuilding.store()
                    .withStoreName(DatastoreConstants.StatFxAttributesStore.STORE_NAME)
                    .withField(DatastoreConstants.StatFxAttributesStore.Fields.AS_OF_DATE, LOCAL_DATE)
                    .asKeyField()
                    .withField(DatastoreConstants.StatFxAttributesStore.Fields.STAT_NAME, STRING)
                    .asKeyField()
                    .withField(DatastoreConstants.StatFxAttributesStore.Fields.FX_TYPE, STRING)
                    .withField(DatastoreConstants.StatFxAttributesStore.Fields.FX_SHIFT_IN_PERCENT, DOUBLE, Double.NaN)
                    .withField(DatastoreConstants.StatFxAttributesStore.Fields.FX_SHIFT_IN_STD, DOUBLE, Double.NaN)
                    .withNullableField(DatastoreConstants.StatFxAttributesStore.Fields.HAS_FX_SHIFT, BOOLEAN)
                    .withNullableField(DatastoreConstants.StatFxAttributesStore.Fields.STRESS_FX_CURRENCIES, STRING)
                    .withNullableField(DatastoreConstants.StatFxAttributesStore.Fields.SUPPORT_RISK_TYPE, BOOLEAN)
                    .withNullableField(DatastoreConstants.StatFxAttributesStore.Fields.GAMMA_SHIFT, DOUBLE)
                    .withNullableField(DatastoreConstants.StatFxAttributesStore.Fields.GREEK_SENSITIVITY_TYPE, STRING)
                    .build();
        }

        public static final class Fields {
            private Fields() {}

            public static final String AS_OF_DATE = FieldConstants.AS_OF_DATE;
            public static final String STAT_NAME = DatastoreConstants.STAT_NAME;
            public static final String FX_TYPE = "fxType";
            public static final String FX_SHIFT_IN_PERCENT = "FxShiftInPercent";
            public static final String FX_SHIFT_IN_STD = "FxShiftInStd";
            public static final String HAS_FX_SHIFT = "HasFxShift";
            public static final String STRESS_FX_CURRENCIES = "StressFxCurrencies";
            public static final String FX_HEDGE_STAT = "FxHedgeStat";
            public static final String SUPPORT_RISK_TYPE = "SupportRiskType";
            public static final String GAMMA_SHIFT = "GammaShift";
            public static final String GREEK_SENSITIVITY_TYPE = "GreekSensitivityType";
        }

        public static final class FieldPaths {
            private FieldPaths() {}

            public static final FieldPath AS_OF_DATE = FieldPath.of(Fields.AS_OF_DATE);
            public static final FieldPath STAT_NAME = FieldPath.of(Fields.STAT_NAME);
            public static final FieldPath FX_TYPE = FieldPath.of(Fields.FX_TYPE);
            public static final FieldPath FX_SHIFT_IN_PERCENT = FieldPath.of(Fields.FX_SHIFT_IN_PERCENT);
            public static final FieldPath FX_SHIFT_IN_STD = FieldPath.of(Fields.FX_SHIFT_IN_STD);
            public static final FieldPath HAS_FX_SHIFT = FieldPath.of(Fields.HAS_FX_SHIFT);
            public static final FieldPath STRESS_FX_CURRENCIES = FieldPath.of(Fields.STRESS_FX_CURRENCIES);
            public static final FieldPath FX_HEDGE_STAT = FieldPath.of(Fields.FX_HEDGE_STAT);
            public static final FieldPath SUPPORT_RISK_TYPE = FieldPath.of(Fields.SUPPORT_RISK_TYPE);
            public static final FieldPath GAMMA_SHIFT = FieldPath.of(Fields.GAMMA_SHIFT);
            public static final FieldPath GREEK_SENSITIVITY_TYPE = FieldPath.of(Fields.GREEK_SENSITIVITY_TYPE);
        }
    }
}
