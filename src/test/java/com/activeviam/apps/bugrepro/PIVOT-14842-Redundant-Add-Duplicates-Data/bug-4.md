# Bug report: `ADD_ROWS` refresh over already-loaded data duplicates it under a `MANDATORY` join

**Jira:** [PIVOT-14842](https://activeviam.atlassian.net/browse/PIVOT-14842) - **CONFIRMED not an engine
defect by ActiveViam's own triage, 27 Aug 2026. Retraction reply posted (`pivot-14842-reply.md`, same
directory), ticket moved to Triage. Fix implemented in `DataMaintenanceController` and live-verified
against the real 10M-row cluster - see the RETRACTION UPDATE below.**

## RETRACTION UPDATE (same day, later): ActiveViam's triage confirms - not a defect, `ADD_ROWS_WITH_DIRTY_CONDITION` is the correct change type

ActiveViam's triage responded, independently reaching the same conclusion as the self-identified UPDATE
below, and adding the concrete fix we hadn't named:

> Not a bug: redundant `ADD_ROWS` refresh double-counts because the scope is dirty; `ADD_ROWS_WITH_DIRTY_CONDITION`
> is the correct change type... `IncrementalAggViewRefreshPlanner.handleAdd` plans a pure
> `IncrementalAddAggViewOperation` (no remove), so a dirty scope is re-added on every call - linear, silent
> growth... Already documented in `incremental-refresh-advanced.mdx` ("Partial or Wrong change
>
>> description": "The first row will be counted twice in the aggregate provider")... No engine fix: callers
>> that may re-touch loaded data must use `ADD_ROWS_WITH_DIRTY_CONDITION` (maps to `MIXED_CHANGES` ->
>> `removeWhere`+add, verified idempotent).

This matches our own independent trace (below) almost exactly, and names the actual intended fix
(`ADD_ROWS_WITH_DIRTY_CONDITION`, which maps to `MIXED_CHANGES` - `removeWhere` then re-add, genuinely
idempotent) rather than requiring us to hand-roll row-level scoping ourselves. A reply is drafted
(`pivot-14842-reply.md`, this directory) acknowledging the diagnosis and committing to switch
`DataMaintenanceController`'s `ADD_ROWS` calls to `ADD_ROWS_WITH_DIRTY_CONDITION` wherever a call might
re-touch an already-loaded date - **not yet posted to Jira, not yet implemented in code**, pending user
review, per the same process PIVOT-14823's actual retraction followed.

## UPDATE (same day, 27 Aug 2026): very likely NOT an engine defect - self-identified, same class as PIVOT-14823

Asked to trace the root cause into ActiveViam's actual source rather than rely on the empirical
characterization below. Found the real mechanism, then verified it three ways - the same discipline used
for PIVOT-14823's retraction - **before** waiting for any response from ActiveViam.

1. **Traced into real ActiveViam source** (`sql-database-6.2.0-sources.jar`,
   `com.activeviam.database.sql.internal.refresh.incremental.plan`):
   `IncrementalAggViewRefreshPlanner#handleAdd` always emits a single `IncrementalAddAggViewOperation`
   with **no preceding `IncrementalRemoveWhereOperation`** - unlike `handleReplace` (for
   `UPDATE_ROWS`/`MIXED_CHANGES`), which correctly does remove-then-add. The "new data query" is scoped
   purely by the `ADD_ROWS` call's own condition (`AIncrementalViewRefreshPlanner#computeNewFilter`,
   effectively just the impact condition when the provider's base query is unrestricted) - it recomputes
   the **full current aggregate for that condition's scope** from source, not an actual delta, and adds it
   on top of whatever the provider already holds for that scope. This is correct and safe **only** if the
   scope has never been reflected before; if the same scope is redescribed a second time, its freshly
   recomputed aggregate gets summed onto the existing one.
2. **Checked the docs directly** (`engine/java-sdk/6.2/directquery/incremental-refresh.mdx`) rather than
   assume the source trace explains everything on its own. It states outright, for `ADD_ROWS`: *"The
   provided scope must describe ALL the added rows and ONLY the added rows."* And more generally: *"Scope
   provided should capture the entirety of the data change. If not, cube components... will be
   de-synchronized. This could lead to inconsistent results."* This application's own
   `DataMaintenanceController#loadFactOnly`/`#load`/`#loadDimensionOnly` all scope every `ADD_ROWS` call to
   `AsOfDate = date` - correct for a date's first-ever load (every row in that scope really is new then),
   but a **wrong change description** the moment the same endpoint is called again for a date that already
   has data, since the scope then re-describes rows that are not new. This is exactly this application's
   own `POST /load/fact` usage pattern in the live rehearsal that first found this bug.
3. **Confirmed the fix empirically**: `scopingTheRedundantRefreshToOnlyTheNewRowInsteadOfTheWholeDateAvoidsTheDoubling`
   (new test, same file) repeats the minimal redundant-refresh scenario, but scopes the second refresh
   precisely to the one genuinely new row (`AsOfDate = date AND TradeID = 'T3'`) instead of the whole date -
   exactly what the docs say the scope should be. **Result: the count comes back correctly at 3, not
   doubled.** A correctly-scoped `ADD_ROWS` call, even one issued after other data already exists for that
   date, does not duplicate anything.

**This means the doubling is this application's own bug, not ActiveViam's** - `DataMaintenanceController`'s
fact-only/dimension-only/whole-day endpoints scope every `ADD_ROWS` call by date, which is fine for a
date's first load but is exactly the "wrong change description" the docs warn about the moment the same
date is touched again - which is precisely what "an intraday update lands in an already-loaded day" (the
whole *point* of the fact-only/dimension-only endpoints) requires. **Not yet decided**: whether to retract
PIVOT-14842 with ActiveViam (no response from them yet, unlike PIVOT-14823 which they flagged first), and
whether to fix `DataMaintenanceController`'s condition scoping to key on the actual new row(s) rather than
the whole date - a real, separate, actionable application-level gap. All existing tests below are still
valid and still pass; they correctly demonstrate the *application-level* defect (this app's scoping choice)
even though the underlying merge behavior they exercise turns out to be ActiveViam's documented, intended
contract rather than an engine bug.

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
(27 Aug 2026) `DremioSchemaConfig`. Four test methods, all passing (`mvn -o test
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
4. **`scopingTheRedundantRefreshToOnlyTheNewRowInsteadOfTheWholeDateAvoidsTheDoubling`** - added 27 Aug 2026
   while tracing the root cause. Repeats the minimal scenario, but scopes the second (redundant) refresh
   precisely to the one genuinely new row (`AsOfDate = date AND TradeID = 'T3'`) instead of the whole date.
   **The count comes back correctly at 3, not doubled.** This is the test that identified this as an
   application-level scoping issue, not an engine defect - see the UPDATE at the top of this file.

## Root cause

**Traced into real ActiveViam source, 27 Aug 2026** (`sql-database-6.2.0-sources.jar`,
`com.activeviam.database.sql.internal.refresh.incremental.plan`) - see the UPDATE at the top of this file
for the full account. In short: `IncrementalAggViewRefreshPlanner#handleAdd` always emits a single
`IncrementalAddAggViewOperation` with no preceding `IncrementalRemoveWhereOperation` (unlike `handleReplace`,
which correctly does remove-then-add for `UPDATE_ROWS`/`MIXED_CHANGES`). The "new data query"
(`AIncrementalViewRefreshPlanner#computeNewFilter`) is scoped purely by the `ADD_ROWS` call's own condition,
recomputing the *full current aggregate* for that scope from source - not an actual delta - and adding it
on top of whatever the provider already holds. This is by design: `ADD_ROWS`'s documented contract assumes
the scope only ever describes genuinely new data, so there is nothing to remove first. **Confirmed
empirically** (test 4 above): a correctly-scoped `ADD_ROWS` call does not duplicate anything, even when
issued after other data already exists for that date.

Under `OPTIONAL`, the equivalent redundant (over-broad-scope) refresh stays correct - plausibly because that
path's `hasProblematicOptionalJoin` check routes it to `fullRefreshOperations()` (a real remove-then-add
regardless of scope) rather than through `handleAdd`'s narrower path, though this specific branch was not
traced as deeply as the `MANDATORY`/`handleAdd` path above - flagged as a hypothesis, not confirmed by
reading that exact code path.

Dremio SQL evidence from the live production reproduction (`sys.jobs_recent`, both runs) showed the
rebuild query as an `INNER JOIN` between `Trades`/`TradeAttributes` with a `COALESCE(..., '1970-01-01')`
fallback in the join condition - the same phantom-bucket SQL shape normally associated with `OPTIONAL`.
Whether that SQL shape is itself implicated in the merge-not-replace behavior, or is unrelated boilerplate
DirectQuery always generates regardless of join optionality, is not established - and is likely moot now
that the root cause is understood to be the over-broad condition scope, not the SQL shape itself.

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

**Conclusion**: this could not be reproduced outside DirectQuery, and there is no way to construct an
equivalent test that would meaningfully settle the question either way on the plain SDK - both the
`MANDATORY` join concept and the "scoped refresh" operation itself are DirectQuery-specific by construction,
so this is unsurprising rather than diagnostic. **Superseded by the root-cause trace above**: this is not an
engine defect at all, so "is it DirectQuery-specific" is no longer the operative question - the actual
finding is that DirectQuery's `ADD_ROWS` has a documented scoping contract this application's own endpoints
don't honor, which is inherently only expressible in DirectQuery (a plain `IDatastore` has no change-
description/scope concept at all), but that is a statement about where the *concept* exists, not about a
defect being DirectQuery-only.

## Impact

**On the application, not on ActiveViam**: `DataMaintenanceController`'s fact-only/dimension-only/whole-day
endpoints all scope their `ADD_ROWS` calls to `AsOfDate = date`, which silently violates DirectQuery's
documented change-description contract the moment any of them is called a second time for an already-loaded
date - exactly the shape of an intraday update landing in an already-loaded day, which is the entire stated
purpose of the fact-only/dimension-only endpoints. The result is silent, cumulative over-counting under
`MANDATORY` with no error and no warning - a wrong number that looks plausible (a real trade count, just
doubled) rather than obviously broken. This is a real, actionable gap in this reference application's own
code, independent of whatever happens to the Jira ticket.

## Next steps

- ✅ **Posted `pivot-14842-reply.md` and retracted PIVOT-14842** - comment posted, ticket transitioned to
  `Triage` (the only forward-progress status transition available to the reporter; a full "Closed"/"Won't
  Fix" transition wasn't available via the API and likely needs ActiveViam's own team action).
- ✅ **Fixed `DataMaintenanceController`'s `ADD_ROWS` calls** (`loadFactOnly`/`loadDimensionOnly`/`load`) -
  all three now use `ADD_ROWS_WITH_DIRTY_CONDITION`. **Live-verified against the real 10M-row/3-node
  cluster**, not just H2: the original fact-only-add repro (unmatched row on an already-loaded date) now
  stays correctly at 1,000,000, and a second redundant call stays idempotent (still 1,000,000, not
  2,000,000 or 3,000,000).
- **New finding surfaced while verifying the fix**: `loadDimensionOnly`'s behavior under `MANDATORY`
  changed, not just its idempotency. Plain `ADD_ROWS` triggered an engine-side "Impossible update details"
  rejection (`AIncrementalViewRefreshPlanner#validateImpactingDetails`, gated `if
  (ChangeTypeInternal.ADD_ROWS == changeType)`) - `ADD_ROWS_WITH_DIRTY_CONDITION` maps to `MIXED_CHANGES`
  internally, bypassing that check entirely. Verified via `DirtyConditionFixVerificationTest`: the call now
  succeeds silently instead of rejecting, correctly leaving the count unaffected (an unmatched dimension-only
  row has no impact on a `Trades`-based selection either way). A behavior improvement, not a regression, but
  a real change from what Finding 2 (`project_scenario4_mandatory_factdim_rehearsal_2026_08_27`) originally
  documented and what both HA artifacts reported - needs syncing there too.
- **Compare our tests against ActiveViam's 4 committed regression tests** (dirty-add idempotency under
  `MANDATORY`, an `OPTIONAL` control, a clean-`ADD_ROWS` case, and a negative control) before deciding
  whether to keep both sets or consolidate - not yet done.
- `hasProblematicOptionalJoin`'s escalation to `fullRefreshOperations()` under `OPTIONAL` (masking the same
  dirty-scope misuse rather than handling it correctly) - confirmed by ActiveViam's own triage, not just our
  hypothesis.

