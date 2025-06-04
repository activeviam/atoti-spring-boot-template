INSERT INTO AggUp.PositionDetail
FROM INFILE '/data/csv/PositionDetail.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.StatisticBaseCurrency
FROM INFILE '/data/csv/StatisticBaseCurrency.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.StatResultLookup
FROM INFILE '/data/csv/StatResultLookup.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.Currency
FROM INFILE '/data/csv/Currency.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.AsOfDate
FROM INFILE '/data/csv/2024-09-01/AsOfDate.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.Holding
FROM INFILE '/data/csv/2024-09-01/Holding.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.HoldingDetail
FROM INFILE '/data/csv/2024-09-01/HoldingDetail.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.ScaledStatResult
FROM INFILE '/data/csv/2024-09-01/ScaledStatResult.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.StatResults
FROM INFILE '/data/csv/2024-09-01/StatResults.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.Security
FROM INFILE '/data/csv/2024-09-01/Security.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.SimReturns
FROM INFILE '/data/csv/2024-09-01/SimReturns.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.AsOfDate
FROM INFILE '/data/csv/2024-09-02/AsOfDate.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.Holding
FROM INFILE '/data/csv/2024-09-02/Holding.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.HoldingDetail
FROM INFILE '/data/csv/2024-09-02/HoldingDetail.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.ScaledStatResult
FROM INFILE '/data/csv/2024-09-02/ScaledStatResult.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.StatResults
FROM INFILE '/data/csv/2024-09-02/StatResults.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.Security
FROM INFILE '/data/csv/2024-09-02/Security.csv'
FORMAT CSVWithNames;
--
INSERT INTO AggUp.SimReturns
FROM INFILE '/data/csv/2024-09-02/SimReturns.csv'
FORMAT CSVWithNames;