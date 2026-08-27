Thanks for the detailed analysis — confirmed, and closing this on our end as not a defect.

We independently verified your diagnosis two ways before responding:

1. **Checked our own repro and production code.** Our `OptionalJoinRemoveRowsNonRemovalReproTest` never issues a DELETE against the H2 source before calling `refresh(REMOVE_ROWS)` — same in production: `DataMaintenanceController#delete` only calls `Application#refresh(...)`, no DML against Dremio. That's a deliberate design in this sample app (commit `f2ecfcb`, "Stop mutating Dremio from delete/load"), made to avoid a *different* problem — calling the load endpoint's INSERT-equivalent from multiple HA replicas was duplicating rows. We hadn't connected that this made our delete's `REMOVE_ROWS` calls a "wrong change description" by DirectQuery's own contract.

2. **Checked the docs and 6.2.0 source directly** (`sql-database-6.2.0-sources.jar`, `AIncrementalViewRefreshPlanner`/`IncrementalAggViewRefreshPlanner`) rather than take the explanation on faith. All of it holds up:

   - `directquery/incremental-refresh-advanced#partial-or-wrong-change-description` — "Partial or Wrong change description are not supported," exactly the case here.
   - `hasProblematicOptionalJoin` → `fullRefreshOperations()` is a real, dedicated escalation path (`StreamPlanRationale.IMPACT_ALONG_OPTIONAL_RELATIONSHIP`), not incidental.
   - The MANDATORY path (`IncrementalAggViewRefreshPlanner#handleRemove`) confirmed as scoped local `removeWhere` with no source re-query — consistent with your point that MANDATORY only *appears* to work because it doesn't need to trust the source, not because OPTIONAL is broken.
3. **Confirmed the fix empirically.** Adding a real `DELETE` against both H2 tables before `refresh(REMOVE_ROWS)` (otherwise identical to the original repro) does genuinely remove the date — the member is purged from the hierarchy entirely, not just left at a stale value.

One thing worth a note for anyone implementing this against a real two-table source: we also tried deleting from only one of the two joined tables before calling `REMOVE_ROWS` on both (simulating a non-atomic delete across `Trades`/`TradeAttributes`). That doesn't reproduce the original non-removal *or* land cleanly at zero — it leaves a cell that exists but is null-valued, a third outcome. Not a counter to your diagnosis, just a flag that the fix on our end needs the two-table delete to be atomic, not merely present.

We'll update `DataMaintenanceController#delete` to issue the real DELETE (atomically across both tables) before the refresh call, and retract this ticket. Appreciate the thorough writeup — saved us from shipping a wrong fix upstream.
