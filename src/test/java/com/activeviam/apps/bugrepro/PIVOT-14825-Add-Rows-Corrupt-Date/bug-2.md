# Bug report: fact-only `ADD_ROWS` refresh under an `OPTIONAL` join corrupts the `AsOfDate` hierarchy

**Jira:** [PIVOT-14825](https://activeviam.atlassian.net/browse/PIVOT-14825)

## Summary

Under `RelationshipOptionality.OPTIONAL`, refreshing the fact table (`Trades`) alone via `ADD_ROWS` - i.e.
`DataMaintenanceController#loadFactOnly`, the `POST .../load/fact` endpoint - without also refreshing
`TradeAttributes` in the same `ChangeDescription`, corrupts the `AsOfDate` hierarchy for an unmatched row.
Two prior production runs each observed one isolated symptom in isolation: a phantom `1970-01-01` date at
small scale, and a point query returning double the correct count at 10M-row/3-node scale. The
pre-existing, unmodified `DataMaintenanceController#load` endpoint (both tables refreshed together) and
`#loadDimensionOnly` (dimension-only) are both confirmed clean - this bug is specific to refreshing
`Trades` alone.

## Reproduction

`OptionalJoinFactOnlyAddMisrouteReproTest` (same directory) drives the real
`Application#refresh(ChangeDescription)` API against an in-memory H2 database, with the same
`PARTIAL_BY_DATE`-shaped cube and `OPTIONAL` join as bug-1's sibling test. It loads a matched baseline (2
rows) into both tables for one date via `ADD_ROWS`, lands one more row in `Trades` only for that date (no
matching `TradeAttributes` row - an upstream feed landing a new trade before its attributes), then
refreshes only `Trades` via `ADD_ROWS`, exactly like `#loadFactOnly`.

**Real run result (26 Aug 2026, `mvn -o test -Dtest=OptionalJoinFactOnlyAddMisrouteReproTest`):**
reproduces the bug, and combines both previously-separate production symptoms in one run, plus a third,
newly-observed one:

1. A direct point query for the real date returns **4** - not the pre-refresh 2, and not the correct
   post-refresh 3 either, but exactly double the correct value - the same over-counting shape as the
   10M-row production finding.
2. A direct point query for `1970-01-01` - never inserted anywhere in this test's own data - returns **1**:
   the misrouted new row landed there instead, exactly the phantom-date symptom from the small-scale
   production finding.
3. **New finding, not previously observed in production:** a full (non-`NON EMPTY`) MDX listing of every
   `[AsOfDate].[AsOfDate]` member - a query that succeeds both before this refresh and on the clean baseline
   - throws a real `NullPointerException` (via `MdxRuntimeException`), rooted in
     `MultiVersionAxisMember.getChild` -> `ConcurrentSkipListMap.doGet` returning `null`. This suggests the
     fact-only refresh leaves the `AsOfDate` axis itself in a corrupted state, not just the aggregate values -
     neither prior production run had exercised a full member listing against the broken state, only point/
     `NON EMPTY` queries.

## Is this specific to DirectQuery?

Partially. `PlainDatastoreFactOnlyAddReproTest` (same directory) drives the identical shape - two joined
stores, a matched baseline, then one more row added to `Trades` only - through the plain in-memory
`IDatastore` API instead, with no DirectQuery, no SQL, and no Spring context involved.

**Real run result (26 Aug 2026, `mvn -o test -Dtest=PlainDatastoreFactOnlyAddReproTest`):** one of the
three anomalies above reproduces here too; the other two do not.

- **Reproduces (core engine, not DirectQuery-specific):** the misrouted row still lands on a phantom
  `1970-01-01` member (point count 1), and the real date's own count stays wrong (stuck at its pre-add
  value of 2, though not doubled the way DirectQuery's is). This also reproduces with a plain, unpartitioned
  bitmap provider (no `PARTIAL_BY_DATE`/`filteredOn` at all) - so it needs neither DirectQuery nor
  aggregate-provider partitioning. It is a general behavior of a `withAllReachableFields()` selection when a
  field name (here `AsOfDate`) exists as both a base-store key field and a field reachable through a
  reference whose target row is missing.
- **Does NOT reproduce (DirectQuery-specific):** the real date's count does not double to 4 the way it does
  under DirectQuery - it just stays stale at 2.
- **Does NOT reproduce (DirectQuery-specific):** the full `[AsOfDate].[AsOfDate].Members` listing does not
  throw - it succeeds and correctly shows both the real and phantom members.

This overturns an earlier, source-only hypothesis (never actually run against a plain datastore) that the
whole bug was caused by DirectQuery's SQL-generation-layer `COALESCE` handling
(`SqlGenerationUtil.fieldWithCoalesceIfNecessary`) for a join whose target side can be null. That mechanism
is real and still explains the DirectQuery-only anomalies, but it cannot be the sole cause, since the same
phantom-date misrouting appears with no SQL involved at all - meaning the true root cause of that part is in
ActiveViam's core reachable-field/reference-resolution machinery, not DirectQuery specifically.

## Impact

A fact-only refresh is meant to let intraday new-trade updates be applied cheaply without re-touching
reference data - but under this application's real `OPTIONAL` join configuration it instead corrupts the
`AsOfDate` hierarchy: aggregate values become wrong (double-counted, and a phantom date appears), and a
plain member-listing query can crash outright. This blocks any workflow that relies on fact-only refresh
being safe to run in isolation from the dimension table under `OPTIONAL`.
