DROP VIEW IF EXISTS AggUp.ScaledVectorsView;
--
CREATE VIEW IF NOT EXISTS
AggUp.ScaledVectorsView AS
SELECT
   H.AsOfDate AS AsOfDate,
   H.Portfolio AS Portfolio,
   H.HoldingId AS HoldingId,
   SV.StatName AS StatName,
   SV.DimensionsMask AS DimensionsMask,
   SV.Vector * H.Amount AS MtMVector
FROM AggUp.Holding AS H
JOIN (
    SELECT
        HD.AsOfDate,
        HD.BaseHoldingId,
        HD.PricedSecurityName,
        SR.Vector,
        SR.DimensionsMask,
        SR.StatName
    FROM AggUp.HoldingDetail AS HD
    JOIN AggUp.SimReturns SR
    ON HD.AsOfDate = SR.AsOfDate AND HD.PricedSecurityName = SR.SecurityName) SV
ON H.AsOfDate = SV.AsOfDate AND H.HoldingId = SV.BaseHoldingId;

-- -- Create view where we create the scaled simulations
-- CREATE TABLE IF NOT EXISTS
--     AggUp.ScaledVectors
-- (
--     `AsOfDate` DATE32,
--     `Portfolio` String,
--     `HoldingId` String,
--     `SecurityName` String,
--     `DimensionMask` String,
--     `StatName` String,
--     `ScaledVector` Array(DOUBLE)
-- )
--     ENGINE = SummingMergeTree
--         PRIMARY KEY (AsOfDate,Portfolio,HoldingId,SecurityName,DimensionMask,StatName,ScaledVector);
-- --
-- CREATE MATERIALIZED VIEW AggUp.ScaledVectorsMView TO AggUp.ScaledVectors
-- AS SELECT H.AsOfDate AS AsOfDate, H.Portfolio AS Portfolio, H.HoldingId AS HoldingId, V.PricedSecurityName AS SecurityName, V.DimensionsMask AS DimensionsMask, V.StatName AS StatName, V.Vector * H.Amount AS ScaledVector
--    FROM AggUp.Holding AS H
--             JOIN (
--        SELECT  HD.AsOfDate , HD.BaseHoldingId, HD.PricedSecurityName, SR.Vector, SR.DimensionsMask, SR.StatName
--        FROM AggUp.HoldingDetail AS HD
--                 JOIN AggUp.SimReturns SR
--                      ON HD.AsOfDate = SR.AsOfDate AND HD.PricedSecurityName = SR.SecurityName) V
--                  ON H.AsOfDate = V.AsOfDate AND H.HoldingId = V.BaseHoldingId
--    GROUP BY AsOfDate, Portfolio, HoldingId, SecurityName,DimensionsMask, StatName,ScaledVector;