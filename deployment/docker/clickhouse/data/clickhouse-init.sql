-- Delete DB
DROP
DATABASE IF EXISTS AggUp;

-- Create DB
CREATE
DATABASE IF NOT EXISTS AggUp;

-- Create table AsOfDate
CREATE TABLE IF NOT EXISTS
    AggUp.AsOfDate
(
    `RetentionType` String,
    `AsOfDate` DATE32
)
ENGINE = MergeTree
PRIMARY KEY
(
    `AsOfDate`
);

-- Create table Holding
CREATE TABLE IF NOT EXISTS
    AggUp.Holding
(
    `Portfolio` String,
    `HoldingId` String,
    `HoldingUniqueName` String,
    `BaseHoldingUniqueName` String,
    `BaseHoldingId` String,
    `Amount` DOUBLE,
    `IsRelative` BOOLEAN,
    `IsShortBase` BOOLEAN,
    `IsBlend` BOOLEAN,
    `IsFilter` BOOLEAN,
    `IsShareClassArtificial` BOOLEAN,
    `IsHoldingGroupByWeightedHolding` BOOLEAN,
    `BasePortfolio` String,
    `BenchmarkPortfolio` String,
    `BenchmarkMethod` String,
    `BaseBenchmark` String,
    `FxHedgingPercent` DOUBLE,
    `RawFxHedgingPercent` DOUBLE,
    `BlendLoadStatus` String,
    `BlendLoadMessage` String,
    `LookThroughPortfolio` String,
    `LookThroughLoadStatus` String,
    `LookThroughLoadMessage` String,
    `ProcessDate` DATE32,
    `HoldingUpdateTimestamp` String,
    `HoldingSource` String,
    `HoldingPath` String,
    `Level0` String,
    `Level1` String,
    `Level2` String,
    `Level3` String,
    `Level4` String,
    `Level5` String,
    `Level6` String,
    `Level7` String,
    `Level8` String,
    `Level9` String,
    `Level10` String,
    `FundLevel0` String,
    `FundLevel1` String,
    `FundLevel2` String,
    `FundLevel3` String,
    `FundLevel4` String,
    `FundLevel5` String,
    `FundLevel6` String,
    `FundLevel7` String,
    `FundLevel8` String,
    `FundLevel9` String,
    `FundLevel10` String,
    `PartitionKey` INTEGER,
    `AsOfDate` DATE32
)
ENGINE = MergeTree
PRIMARY KEY
(
    `AsOfDate`,
    `PartitionKey`,
    `Portfolio`,
    `HoldingId`
);

-- Create table HoldingDetail
CREATE TABLE IF NOT EXISTS
    AggUp.HoldingDetail
(
    `BaseHoldingId` String,
    `PartitionKey` INTEGER,
    `AsOfDate` DATE32,
    `HoldingName` String,
    `PricedSecurityName` String,
    `ProxySecurityName` String,
    `Security` String,
    `Haircut` DOUBLE,
    `HostName` String,
    `LoadingStatus` String,
    `LoadingMessages` String,
    `OriginalAmount` DOUBLE,
    `clientMarketValue` DOUBLE,
    `clientMarketValueCCY` String,
    `RMG_ProxySecurityName` String
)
ENGINE = MergeTree()
PRIMARY KEY
(
    `AsOfDate`,
    `BaseHoldingId`,
    `PartitionKey`
);

-- Create table ScaledStatResult
CREATE TABLE IF NOT EXISTS
    AggUp.ScaledStatResult
(
    `HoldingId` String,
    `PartitionKey` INTEGER,
    `AsOfDate` DATE32,
    `ResultValuesSum` Array(DOUBLE),
    `ResultValuesPassthrough` Array(DOUBLE),
    `AGGSVC_SIMRETURNS(1Y VS)_MONTECARLO` Array(DOUBLE)
)
ENGINE = MergeTree
    PRIMARY KEY
(
    `AsOfDate`,
    `HoldingId`,
    `PartitionKey`
);

-- Create table Security
CREATE TABLE IF NOT EXISTS
    AggUp.Security
(
    `pricedSecurityName` String,
    `PartitionKey` INTEGER,
    `AsOfDate` DATE32,
    `FxHedgingPercent` DOUBLE,
    `EnrichmentStatus` String,
    `InputModel` String,
    `OutputModel` String,
    `EnrichmentInputId` String,
    `EnrichmentInputIdType` String,
    `EnrichmentMessages` String,
    `RMG_SecurityIsProxied` BOOLEAN,
    `SecurityUpdateTimestamp` String,
    `IsFXModel` BOOLEAN
)
ENGINE = MergeTree
PRIMARY KEY
(
     `AsOfDate`,
     `pricedSecurityName`,
     `PartitionKey`
);

-- Create table PositionDetail
CREATE TABLE IF NOT EXISTS
    AggUp.PositionDetail
(
    `pricedSecurityName` String,
    `delta` String,
    `gamma` String,
    `currency` String,
    `equityDomicileCcy` String,
    `allFxCcy` String,
    `positionType` String,
    `calibratedModelType` String
)
ENGINE = MergeTree
PRIMARY KEY
(
     `pricedSecurityName`
);

-- Create table StatResults
CREATE TABLE IF NOT EXISTS
    AggUp.StatResults
(
    `SecurityName` String,
    `Valspec` String,
    `AsOfDate` DATE32,
    `ResultType` String,
    `ResultValuesSum` Array(DOUBLE),
    `ResultValuesPassthrough` Array(DOUBLE)
)
ENGINE = MergeTree
PRIMARY KEY
(
    `AsOfDate`,
    `SecurityName`,
    `Valspec`,
    `ResultType`
);

-- Create table StatResults
CREATE TABLE IF NOT EXISTS
    AggUp.StatResultLookup
(
    `StatName` String,
    `DrilldownName` String,
    `Valspec` String,
    `StatField` String,
    `StatIndex` INTEGER
)
ENGINE = MergeTree
PRIMARY KEY
(
    `StatName`,
    `DrilldownName`
);

-- Create table StatResults
CREATE TABLE IF NOT EXISTS
    AggUp.StatisticBaseCurrency
(
    `StatName` String,
    `BaseCurrency` String
)
ENGINE = MergeTree
PRIMARY KEY
(
    `StatName`
);

-- Create table SimReturns
CREATE TABLE IF NOT EXISTS
    AggUp.SimReturns
(
    `SecurityName` String,
    `DimensionsMask` String,
    `StatName` String,
    `AsOfDate` DATE32,
    `Vector` Array(DOUBLE)
)
ENGINE = MergeTree
PRIMARY KEY
(
    `SecurityName`,
    `DimensionsMask`,
    `StatName`
);

-- Create table Currency CurrencyName,SecurityName,ReportingCurrency
CREATE TABLE IF NOT EXISTS
    AggUp.Currency
(
    `CurrencyName` String,
    `SecurityName` String,
    `ReportingCurrency` BOOLEAN
)
ENGINE = MergeTree
PRIMARY KEY
(
    `CurrencyName`
);
