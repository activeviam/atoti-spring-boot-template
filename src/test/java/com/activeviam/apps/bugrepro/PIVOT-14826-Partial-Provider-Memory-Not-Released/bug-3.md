# Bug report: a `PARTIAL_BY_DATE` partial provider's off-heap footprint never shrinks after a delete

**Jira:** [PIVOT-14826](https://activeviam.atlassian.net/browse/PIVOT-14826)

## Summary

Under a `PARTIAL_BY_DATE`-shaped cube (`DataCubeConfig.AggregateProviderMode.PARTIAL_BY_DATE` in
production - one independently-named bitmap partial provider per date, built via
`.withPartialProvider().withName("ByAsOfDate_" + date).bitmap().includingOnlyLevels(level).filteredOn(
Map.of(level, date))`), a per-date partial provider's off-heap memory allocation is never released once
allocated - not when the date's data is correctly and completely deleted, not when epochs are manually
discarded, not after repeated unrelated churn cycles. A delete-then-load "date roll" pattern (delete an old
date, load a new one) therefore grows total off-heap memory monotonically with every date ever loaded
across the node's lifetime, instead of staying bounded to the dates currently live - defeating the entire
point of `PARTIAL_BY_DATE`'s per-date providers for a long-running node.

## Root cause

Unlike PIVOT-14821/14823/14825, this has **not** been traced into ActiveViam source. It is characterized
empirically only. The behavior is most consistent with a property of the partial/bitmap aggregate
provider's own chunk-based off-heap allocator: chunks allocated for a partition are not returned to the
allocator (or the OS) once the partition holds zero rows. Two pieces of evidence rule out the more benign
explanations:

- **Not an epoch-retention artifact.** The default DirectQuery epoch policy is
  `KeepLastEpochPolicy(numberEpochsToKeep=1)`. Explicitly calling `IEpochManager.discard()` and
  `forceDiscardEpochs(node -> true)` (reached via
  `((IDirectQueryDatabase) application.getDatabase()).getInMemoryDatastore().getEpochManager()`, or the
  plain-datastore equivalent `app.getDatastore().getEpochManager()`) makes no difference.
- **Not confounded by PIVOT-14823.** This bug's repro issues a real SQL `DELETE` against the external H2
  source before refreshing, so the delete is genuine and correct (the date's row count goes to an empty
  cell set, i.e. the member is evicted from the hierarchy) - independent of PIVOT-14823's separate
  no-op-delete defect.

If the ActiveViam chunk-allocator/partial-provider sources hold a documented reason (e.g. a "shrink" or
"compact" operation that must be triggered manually, or a known design limitation), it has not been located
in this session - this report should be treated as an empirical characterization to be root-caused by
ActiveViam, not a source-level diagnosis.

## Reproduction

`PartialProviderCapacityNeverShrinksReproTest` (same directory) builds the same OPTIONAL-join,
`PARTIAL_BY_DATE`-shaped, H2-backed DirectQuery application used by the other bug repros in this directory.
It loads one date (3 matched rows) via `ADD_ROWS`, measures the date's own `PartialProvider` off-heap
footprint via ActiveViam's `IMemoryAnalysisService` (`MemoryAnalysisServiceFactory.create(application
.getDatabase(), application.getManager(), Path.of("."))`, walking the returned `IMemoryStatistic` tree for
the node named `MemoryStatisticConstants.STAT_NAME_PARTIAL_PROVIDER` whose
`MemoryStatisticConstants.ATTR_NAME_PROVIDER_NAME` attribute matches the provider's name, then reading
`getRetainedOffHeap()`), issues a real SQL `DELETE` against the H2 source for both `Trades` and
`TradeAttributes`, refreshes both tables via `REMOVE_ROWS`, force-discards all epochs, confirms the MDX
point count for the date is now 0 (empty cell set), and re-measures the same provider's off-heap footprint.

**Real run result (26 Aug 2026, `mvn -o test -Dtest=PartialProviderCapacityNeverShrinksReproTest`):**
reproduces the bug, and matches the numbers already gathered earlier this session investigating the same
behavior at small and multi-date scale:

- Provider `ByAsOfDate_2019-01-06` after loading 3 matched rows: `retainedOffHeap` = **794,624 B**.
- Same provider after the genuine delete (SQL `DELETE` + `REMOVE_ROWS` refresh, MDX point count verified
  3 -> 0, empty cell set): `retainedOffHeap` = **794,624 B**, byte-for-byte unchanged.
- (From this session's earlier exploration, not re-run as a committed test but consistent with the above:
  a two-date total-footprint check showed loading `DATE_1` brings total off-heap to 1,057,280 B
  (`DATE_1`'s 794,624 B + `DATE_2`'s still-empty 262,144 B baseline); deleting `DATE_1` leaves the total
  unchanged at 1,057,280 B; loading `DATE_2` afterward then jumps the total to 1,589,760 B - `DATE_2`'s
  provider needed a full fresh allocation on top of `DATE_1`'s already-empty, never-reclaimed 794,624 B.)
- Five further unrelated load/delete churn cycles (from the same earlier exploration) produced no change -
  every measurement stayed at exactly 794,624 B, ruling out "just needs one more epoch/GC cycle to settle"
  as an explanation.

## Is this specific to DirectQuery?

**No.** `PlainDatastorePartialProviderCapacityReproTest` (same directory) drives the identical shape - the
same `PARTIAL_BY_DATE`-shaped bitmap provider, a scoped `removeWhere` transaction removing the date from
both stores - through the plain in-memory `IDatastore`/`ActivePivotManager` API instead, with no
DirectQuery, no SQL, and no Spring context involved. The same `IMemoryAnalysisService` mechanism works
identically against a plain application via the `MemoryAnalysisServiceFactory.create(IDatabase, ...)`
overload, since `IDatastore` (like `IDirectQueryDatabase`) implements `IDatabase`.

**Real run result (26 Aug 2026, `mvn -o test -Dtest=PlainDatastorePartialProviderCapacityReproTest`):**
the removal is genuinely correct (point count 3 -> 0, member evicted), and the provider's `retainedOffHeap`
is **794,624 B before and after** - identical to the DirectQuery figure, byte-for-byte. Unlike PIVOT-14823
and PIVOT-14825, this bug reproduces with no DirectQuery involved at all: it is a property of the plain
in-memory partial/bitmap aggregate provider's own off-heap chunk allocator, not of DirectQuery's refresh
mechanism, SQL generation, or external-source reconciliation. This also rules out `RelationshipOptionality`
or the `OPTIONAL` join shape as a cause, since the plain-datastore reference used here has no such concept
at all.

## Impact

The entire point of `PARTIAL_BY_DATE` - one independently-named, independently-droppable provider per date
- is to let a long-running node's memory stay bounded to the set of dates currently loaded, by discarding a
date's provider capacity when that date is deleted. This bug defeats that: every date ever loaded across
the node's lifetime permanently claims its off-heap allocation, so a date-roll workflow (delete the oldest
date, load a new one, repeat) grows total off-heap memory monotonically rather than staying flat. For a
production WCR node intended to run indefinitely while rolling dates, this is a slow, silent memory leak
that will eventually exhaust off-heap capacity - independent of, and in addition to, the separate
correctness bugs already filed as PIVOT-14823 and PIVOT-14825.
