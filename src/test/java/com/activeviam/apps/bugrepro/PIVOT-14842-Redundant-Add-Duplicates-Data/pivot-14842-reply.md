Thanks for the detailed analysis — confirmed, and closing this on our end as not a defect.

We'd actually reached the same conclusion independently before your reply came in, via the same three-way
discipline we've used on prior tickets from this rehearsal:

1. **Traced `IncrementalAggViewRefreshPlanner#handleAdd`** (`sql-database-6.2.0-sources.jar`) and found it
   plans a bare `IncrementalAddAggViewOperation` with no preceding remove-where — by design, since
   `ADD_ROWS`'s contract assumes the scope only ever covers genuinely new data.
2. **Checked `incremental-refresh-advanced.mdx` directly** rather than take the explanation on faith —
   "Partial or Wrong change description" covers exactly this case, and the doc's own worked example says
   the plainly wrong thing we were doing: a scope that re-covers already-loaded rows gets the first row
   counted twice.
3. **Confirmed empirically**: scoping the redundant refresh to only the genuinely new row (not the whole
   date) gives the correct count, no doubling.

We hadn't identified `ADD_ROWS_WITH_DIRTY_CONDITION` as the intended fix, though — thanks for naming the
actual escalation path (`MIXED_CHANGES` → `removeWhere`+re-add) rather than leaving us to hand-roll a
row-level scope ourselves. That's a cleaner fix than what we'd been about to propose (rewriting
`DataMaintenanceController`'s condition-building to key by row instead of by date), since it doesn't
require us to track which rows are "genuinely new" on our own side at all.

One thing worth flagging for anyone else hitting this: our `hasProblematicOptionalJoin`/`fullRefreshOperations`
read matches yours — under `OPTIONAL` the same dirty-scope misuse gets silently masked by the
full-refresh escalation, so a caller doing this wrong under `OPTIONAL` sees no symptom at all, only a
performance cost. `MANDATORY` is what actually surfaced the misuse, not because `MANDATORY` behaves worse,
but because it's the one path that trusts the caller's scope literally.

We'll switch `DataMaintenanceController#loadFactOnly`/`#load`/`#loadDimensionOnly` to
`ADD_ROWS_WITH_DIRTY_CONDITION` wherever a call might re-touch an already-loaded date, and retract this
ticket. Appreciate the fast, precise turnaround — and the regression tests, which we'll compare against our
own four before deciding whether to keep both sets or consolidate.
