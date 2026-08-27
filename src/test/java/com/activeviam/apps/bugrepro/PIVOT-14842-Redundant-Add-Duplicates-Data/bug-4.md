# Bug report: `ADD_ROWS` refresh over already-loaded data duplicates it under a `MANDATORY` join

**Jira:** [PIVOT-14842](https://activeviam.atlassian.net/browse/PIVOT-14842)

## Summary

Under `RelationshipOptionality.MANDATORY` with the `SINGLE` (unpartitioned) aggregate-provider mode -
`DataCubeConfig.AggregateProviderMode.SINGLE`, this application's Scenario 4 configuration - an `ADD_ROWS`
refresh scoped to a date that is already fully and correctly loaded duplicates that date's point count,
instead of leaving it unchanged. The duplication is **cumulative**: a second redundant refresh takes the
count from 2x the correct value to 3x, confirming the provider's per-date rebuild result is being appended
onto its existing stored value at merge time instead of overwriting it. This reproduces both with a
fact-only refresh (`DataMaintenanceController#loadFactOnly`, the `POST .../load/fact` endpoint - the case
live production actually hit) and with a plain, combined refresh touching no unmatched data at all - the
bug is **not specific to fact-only-add**, it is a general non-idempotency in `MANDATORY`'s incremental
refresh whenever a refresh call re-touches a date that already has data present. A control test confirms
this is genuinely `MANDATORY`-specific: the identical redundant-refresh scenario under `OPTIONAL` stays
correct.

**This bug is specific to DirectQuery.** It could not be reproduced against the plain in-memory
`IDatastore` API - see "Is this specific to DirectQuery?" below for the full result and an important
caveat on how much weight that negative result actually carries.

## How this was found

Live rehearsal (27 Aug 2026, see `project_scenario4_mandatory_factdim_rehearsal_2026_08_27` in this repo's
HA-rehearsal memory) ran Scenario 4's fact-only-add test for the first time - it had never been tested
before, having been out of scope until the fact/dimension split test axis was added. A diagnostic row
(`TradeID=1000001`, unmatched to any `TradeAttributes` row) was inserted directly into the `Trades` source
table for an already-loaded date (`2019-02-02`, baseline 1,000,000 rows on a 10M-row/3-node/query-node
verified cluster), then `POST /custom/rest/data/2019-02-02/load/fact` was called on each data node
independently. Both calls reported success (HTTP 200, ~1s wall-clock). The date's own `contributors.COUNT`
came back **2,000,000** on all three nodes (dq1, dq2, and qn) - not the correct 1,000,001, and not the
pre-refresh 1,000,000, but exactly double. Reproduced identically a second time on a completely
independent fresh stack (different diagnostic row, same date, same result) before this repro test was
written.

## Reproduction

`MandatoryJoinFactOnlyAddDoubleCountReproTest` (same directory) drives the real
`Application#refresh(ChangeDescription)` API against an in-memory H2 database, with the same `SINGLE`
(unpartitioned bitmap provider, no `filteredOn`/`withValuePartitioningOn`) cube shape as
`DataCubeConfig.AggregateProviderMode.SINGLE` and a `MANDATORY` join, matching this application's current
(27 Aug 2026) `DremioSchemaConfig`. Three test methods, all passing (`mvn -o test
-Dtest=MandatoryJoinFactOnlyAddDoubleCountReproTest`):

1. **`factOnlyAddOfAnUnmatchedRowUnderMandatoryJoinDoublesTheRealDatesCount`** - mirrors the live production
   scenario exactly. Two matched trades are loaded (`application.start()` itself does an implicit initial
   pull, so the count is already 2 before any explicit `refresh()` call - confirmed directly, not assumed).
   A third, unmatched row lands in `Trades` only. A fact-only `ADD_ROWS` refresh (exactly
   `DataMaintenanceController#loadFactOnly`'s shape) doubles the count to **4**, not 2 (unchanged) or 3
   (if the new row contributed). A **second** identical fact-only refresh, with no further source-data
   change at all, pushes the count to **6** - confirming the duplication compounds with every redundant
   refresh rather than happening once.
2. **`redundantAddRowsRefreshOverAlreadyLoadedDataDuplicatesItUnderMandatoryJoinWithNoUnmatchedRowInvolved`**
   - the minimal control. No unmatched row anywhere, no fact-only distinction - a plain, combined `ADD_ROWS`
     refresh (mirroring `DataMaintenanceController#load`, both tables together) over a date that is already
     fully and correctly loaded (2 matched rows, count already 2 from `start()`) still doubles the count to
     **4**, and a third redundant refresh pushes it to **6**. This is the cleanest evidence that the bug has
     nothing to do with fact-only-add specifically - any redundant `ADD_ROWS` refresh over already-present
     data triggers it.
3. **`redundantAddRowsRefreshOverAlreadyLoadedDataStaysCorrectUnderOptionalJoin`** - the same minimal
   scenario as (2), with only `targetOptionality` changed to `OPTIONAL`. The redundant refresh correctly
   leaves the count at **2**. This confirms the non-idempotency is genuinely `MANDATORY`-specific, not a
   general property of redundant refreshes that happens to also affect `OPTIONAL`.

## Root cause

**Not traced into ActiveViam source** (unlike PIVOT-14821/14823/14825) - characterized empirically only, via
the three tests above. The evidence is consistent with: under `MANDATORY`, when an `ADD_ROWS` refresh's
scoped condition matches rows that are already present in the provider, the incremental refresh planner
re-computes and re-applies the full per-date aggregate for that scope, and the result is **added to** the
provider's existing stored value instead of **replacing** it. Under `OPTIONAL`, the equivalent redundant
refresh is correctly idempotent, so whatever code path handles this merge differs by join optionality -
plausibly the same `hasProblematicOptionalJoin` / `fullRefreshOperations()`-style escalation logic already
identified as `OPTIONAL`-specific in `PIVOT-14823-DQ-Removal-not-working`'s investigation, but this has not
been confirmed by reading the source for this specific case - flagged as a hypothesis, not a finding.

Dremio SQL evidence from the live production reproduction (`sys.jobs_recent`, both runs) showed the
rebuild query as an `INNER JOIN` between `Trades`/`TradeAttributes` with a `COALESCE(..., '1970-01-01')`
fallback in the join condition - the same phantom-bucket SQL shape normally associated with `OPTIONAL`.
Whether that SQL shape is itself implicated in the merge-not-replace behavior, or is unrelated boilerplate
DirectQuery always generates regardless of join optionality, is not established.

## Is this specific to DirectQuery?

**Yes - specific to DirectQuery, not a general engine defect.** `PlainDatastoreRedundantAddReproTest` (same
directory) is the plain `IDatastore` counterpart, mirroring `PlainDatastoreFactOnlyAddReproTest`'s
structure from PIVOT-14825, and it does not reproduce the bug.

**Structural caveat, found before writing this test, not after:** `IReferenceDescription` - the plain
`IDatastore` join API - has **no `RelationshipOptionality` concept at all** (already established during the
PIVOT-14823 investigation). `MANDATORY` cannot literally be expressed on this API, so this test cannot
reproduce "this bug under a `MANDATORY` join" in the literal sense - there is no such configuration to build.
What it tests instead is the closest available analog: replaying an already-applied transaction (adding
rows that are already present, identically keyed) against a plain datastore, the same shape as a redundant
`ADD_ROWS` refresh.

**Real run result (27 Aug 2026, `mvn -o test -Dtest=PlainDatastoreRedundantAddReproTest`): does NOT
reproduce.** Two matched trades loaded, count correctly 2. Replaying the identical, already-present rows in
a second transaction - twice - leaves the count correctly at 2 both times, no duplication, no cumulative
growth. This is expected for a different, more fundamental reason than "MANDATORY doesn't exist here": a
plain `IDatastore` store enforces primary-key upsert semantics - `addAll` of a row with an already-present
key updates in place, it cannot produce a duplicate row by construction. This test is therefore not a clean
positive/negative signal on whether this bug's *underlying mechanism* is DirectQuery-specific, because the
plain-datastore analog and DirectQuery's real mechanism aren't doing comparable work: DirectQuery's
`ADD_ROWS` refresh re-queries an external source scoped by a condition and merges the result into the
aggregate provider (a genuinely different operation from inserting rows into a local keyed store), so there
is no way to make the plain `IDatastore` API attempt the same "re-run a scoped merge over already-covered
data" operation that DirectQuery's incremental-refresh planner performs.

**Conclusion**: this bug could not be reproduced outside DirectQuery, and there is no way to construct an
equivalent test that would meaningfully settle the question either way on the plain SDK - both the
`MANDATORY` join concept and the "redundant scoped refresh" operation are DirectQuery-specific by
construction. Treat this as "no counter-evidence found, and none can be produced on this API," not as an
independent confirmation the way PIVOT-14825's plain-datastore test was (there, the same operation - adding
a genuinely new row - was directly comparable across both APIs). The merge-not-replace mechanism remains
untraced in ActiveViam's incremental-refresh-planning source (see Root Cause above); that source trace is
the only way left to settle DirectQuery-specificity with real confidence.

## Impact

Under `MANDATORY`, calling any `ADD_ROWS` refresh a second time over a date that is already loaded silently
inflates every aggregate for that date - and does so again on every subsequent redundant call, unbounded.
This is worse than the fact-only-add framing alone suggests: it means `MANDATORY` cannot safely be used with
any operational pattern that might re-trigger a refresh over an already-loaded date (a retried REST call
after a timeout, a redundant date-roll step, an at-least-once delivery guarantee on the calling side) without
silently corrupting reported aggregates - with no error, no warning, and a wrong number that looks
plausible (a real trade count, just doubled) rather than obviously broken.

## Next steps

- Trace the actual merge-not-replace code path in ActiveViam's incremental-refresh-planning source
  (`com.activeviam.database.sql.internal.refresh.incremental.plan.IncrementalRefreshPlanner` and
  neighboring classes - the same package the dimension-only "impossible update" validation lives in, see
  `project_scenario4_mandatory_factdim_rehearsal_2026_08_27`'s Finding 2) rather than relying on the
  empirical characterization above - this is now the only way left to settle DirectQuery-specificity with
  confidence, since no equivalent test can be built on the plain SDK (see above).
- Filed with ActiveViam as [PIVOT-14842](https://activeviam.atlassian.net/browse/PIVOT-14842), 27 Aug 2026.

