# Bug report: `REMOVE_ROWS` refresh under an `OPTIONAL` join silently fails to remove the date

**Jira:** [PIVOT-14823](https://activeviam.atlassian.net/browse/PIVOT-14823)

## Summary

Under `RelationshipOptionality.OPTIONAL` (this application's real `Trades`-to-`TradeAttributes` join
configuration, see `DremioSchemaConfig`), a `REMOVE_ROWS` refresh reports success - no exception, epoch
genuinely advances - but never actually removes the targeted date's rows. Confirmed live at 10M-row/3-node
scale (`dq1`/`dq2`, both replicas, query node included): `DELETE /custom/rest/data/{date}` returns HTTP 200
after a real, committed, epoch-advancing transaction, yet the date still serves its full pre-delete row
count afterward.

## Root cause

Traced from `sql-database-6.2.0-sources.jar`, package
`com.activeviam.database.sql.internal.refresh.incremental.plan`:

1. `DataMaintenanceController#delete` issues one `ChangeDescription` with a `REMOVE_ROWS` entry for each of
   `Trades` and `TradeAttributes`, both keyed on `AsOfDate = date`.
2. The `TradeAttributes` update only reaches the selection's view by crossing the
   `Trades -> TradeAttributes` join (a non-empty join path), unlike the `Trades` update which hits the base
   table directly.
3. `AIncrementalViewRefreshPlanner#hasProblematicOptionalJoin` checks every impacting update's join path
   against the join's `targetOptionality`. Since that join is `OPTIONAL`, the `TradeAttributes` impact trips
   it - escalating the **entire** refresh (not just the `TradeAttributes` half) to `fullRefreshOperations()`.
4. `fullRefreshOperations()` wipes the whole aggregate provider, then blindly re-executes the view's own
   original, unconditioned aggregate query against the external source. Since this app's `delete` never
   issues DML against the external database (it stays the system of record, untouched by design), that
   source still physically has every row for the date - so the "full refresh" faithfully re-imports exactly
   what it was supposed to remove.

`RelationshipOptionality.MANDATORY` does not hit this: no join is `OPTIONAL`, so `hasProblematicOptionalJoin`
never trips, and the correct scoped path (a real, local `IncrementalRemoveWhereOperation(condition)`
targeting just the removed rows, no re-query of the source) runs instead.

## Reproduction

`OptionalJoinRemoveRowsNonRemovalReproTest` (same directory) drives the real
`Application#refresh(ChangeDescription)` API against an in-memory H2 database via
`GenericJdbcConnectorMetaFactory` - the same generic-JDBC connector mechanism production uses for Dremio -
with a `PARTIAL_BY_DATE`-shaped cube and an `OPTIONAL` join, mirroring `DataCubeConfig`/`DremioSchemaConfig`
exactly. It loads one date into both tables via `ADD_ROWS`, confirms the baseline count (3), then issues
`REMOVE_ROWS` on both tables for that date exactly like `DataMaintenanceController#delete` - and asserts the
count is unchanged afterward (still 3, not 0).

**Real run result (26 Aug 2026, `mvn -o test -Dtest=OptionalJoinRemoveRowsNonRemovalReproTest`):**
reproduces the bug. The `REMOVE_ROWS` refresh does not throw, the transaction commits (epoch advances
6 -> 11 across the test's operations), and the post-delete point query for the date still returns 3.

## Is this specific to DirectQuery?

Yes, confirmed. `PlainDatastoreRemoveRowsReproTest` (same directory) drives the identical shape - two
joined stores, the same `PARTIAL_BY_DATE`-shaped provider, a scoped removal on both stores in one
transaction - through the plain in-memory `IDatastore` API instead, with no DirectQuery, no SQL, and no
Spring context involved. **Real run result:** the removal completes correctly - the count drops from 3
to 0 (the member is evicted entirely, unlike DirectQuery where it stays registered at its stale value).
This matches the source-level root cause above: the defect lives entirely in `sql-database`'s SQL-refresh-
planning code, which only runs when reconciling against an external SQL source, and the plain in-memory
join (`IReferenceDescription`) has no `RelationshipOptionality` concept at all - there is nothing to
configure as `OPTIONAL` on that path, so the bug's precondition cannot even be expressed there.

## Impact

A client can believe a date was removed (HTTP 200, successful commit, advancing epoch) when it was not -
this is a silent correctness failure, not a performance-only concern. Given `OPTIONAL` is this
application's real join configuration, this blocks any `REMOVE_ROWS`-based data-maintenance workflow
(masking/failover rehearsals, date rollback, corrections) under the star-schema, `PARTIAL_BY_DATE`/
`OPTIONAL` configuration this repo actually runs.
