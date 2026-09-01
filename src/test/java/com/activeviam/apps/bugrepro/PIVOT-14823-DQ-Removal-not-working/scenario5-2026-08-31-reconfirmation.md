# Reconfirmation: `REMOVE_ROWS` under `OPTIONAL` on Scenario 5 (star schema, unpartitioned provider)

**Status context:** PIVOT-14823 (this folder's `bug-1.md`) was retracted by ActiveViam and closed as
**not a product defect** — `REMOVE_ROWS` is a notification that an external change already happened, not
a request to make one; this app's `delete` endpoint never DMLs Dremio, so issuing `REMOVE_ROWS` without
a matching source-side delete is, by DirectQuery's own docs, an unsupported "wrong change description."
This document is **not a new bug** — it's a fresh, real-data walkthrough of the same already-understood
mechanism, this time captured end-to-end on Scenario 5's exact configuration (star schema,
`AGGREGATE_PROVIDER_MODE=SINGLE` — unpartitioned, not the `PARTIAL_BY_DATE`-shaped config the original
repro used) with the aggregate provider's own metrics tracked before/after.

**Config**: dq1 (port 9290), `DREMIO_DATASET_SIZE=LARGE` (10M rows/date), `OPTIONAL` join,
`DATA_LOAD_RANGE_START=2019-02-01`/`DATA_LOAD_RANGE_END=2019-02-05` (5 dates loaded), target date
`2019-02-01`. Full evidence log: `~/atoti-rehearsal-evidence/scenario5-optional-2026-08-31-fullseq/`.

## Step 0 — Pre-delete baseline

|                 Metric                  |                      Value                      |
|-----------------------------------------|-------------------------------------------------|
| Dates loaded                            | `2019-02-01`..`2019-02-05`, 1,000,000 rows each |
| `atoti_agg_provider_size` (Prometheus)  | 5                                               |
| Aggregate-cache point entries (Jolokia) | 5                                               |
| Heap used                               | 598,856,584 bytes (≈598.9 MB)                   |

## Step 1 — Mask

```
POST /custom/rest/masking/2019-02-01  (dq1)
→ {"successful":true, "successfulQueryCubes":["gba_Cube_QUERY_0_6356"], "failedQueryCubesWithReasons":{}}
```

Routing table (`GET /custom/rest/distribution/2019-02-01`) confirms scope: `maskedDataNodes` contains
only dq1's own node id, and a check against `2019-02-05` on the same node shows it still unmasked.

## Step 2 — The delete command

```
DELETE http://localhost:9290/custom/rest/data/2019-02-01
→ HTTP 200, wall-clock 39,409.99 ms
```

## Step 3 — What happens inside Atoti

One `ChangeDescription` with two `REMOVE_ROWS` entries (`Trades`, `TradeAttributes`), both keyed on
`AsOfDate=2019-02-01`. `TradeAttributes`'s impact only reaches the selection by crossing the `OPTIONAL`
join, tripping `AIncrementalViewRefreshPlanner#hasProblematicOptionalJoin` and escalating the **entire**
refresh to `fullRefreshOperations()`:
1. `IncrementalRemoveWhereOperation(BaseConditions.TRUE)` — wipes the whole aggregate provider (all 5
dates), not just the targeted one.
2. `IncrementalAddAggViewOperation(originalAggregateQuery)` — blindly re-runs the view's own original,
unconditioned query to rebuild the provider from scratch.

Log evidence:

```
Transaction for epoch 8 committed on ActivePivot MultiVersionDataActivePivot [id=Cube] in 37,159 ms.
```

Epoch 6 → 8 — a real, fully committed transaction.

## Step 4 — What Dremio actually receives

Because `delete` never DMLs Dremio, step 3's "re-run the original query" pulls back everything,
including the date supposedly just removed. `sys.jobs_recent` for this exact window (20:31:40–20:32:25
UTC, 31 Aug 2026) shows 5 distinct queries, **none carrying a `WHERE` clause**:

| Rows scanned |                                                                       Query                                                                       |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| 10,000,000   | `SELECT AsOfDate, COUNT(*) FROM TradeAttributesLarge GROUP BY AsOfDate`                                                                           |
| 10,000,000   | `SELECT TradeDate, COUNT(*) FROM TradeAttributesLarge GROUP BY TradeDate`                                                                         |
| 10,000,000   | `SELECT CounterpartyID, COUNT(*) FROM TradeAttributesLarge GROUP BY CounterpartyID`                                                               |
| 10,000,000   | `SELECT TradeID, COUNT(*) FROM TradeAttributesLarge GROUP BY TradeID`                                                                             |
| 20,000,000   | `SELECT AsOfDate, SUM(Notional), COUNT(*) FROM TradesLarge LEFT OUTER JOIN TradeAttributesLarge ON (AsOfDate=..., TradeID=...) GROUP BY AsOfDate` |

(Each of these 5 also appears a second time in `sys.jobs_recent` at `rows_scanned=0` with identical SQL
text — read as a separate planning/prepare-phase job Dremio logs alongside the executed one; not
investigated further.)

## Step 5 — Aggregate provider, before vs. after

|                           | Before | After  |
|---------------------------|--------|--------|
| `atoti_agg_provider_size` | **5**  | **10** |

The point count **doubled** rather than staying at 5 (in-place rebuild) or dropping to 4 (working
delete). This specific doubling behavior is observed and reproducible (same 5→10 pattern seen in an
earlier isolated run on this identical config) but not root-caused here — the working theory is that the
old version's points aren't fully evicted before the "rebuilt" ones land, so the metric reflects both
versions coexisting under Atoti's MVCC until garbage-collected, but this hasn't been confirmed from
source.

## Step 6 — Aggregate cache and heap, before vs. after

|                                     |  Before  |         After         |
|-------------------------------------|----------|-----------------------|
| Aggregate-cache point entries (dq1) | 5        | 1                     |
| Heap used (dq1)                     | 598.9 MB | 1,009.1 MB (+~410 MB) |

Cache dropped from 5 to 1, consistent with the transaction clearing the whole cache (not just the
touched date) — the single remaining entry is from the verification query issued right after.

## Step 7 — Correctness checkpoint (the actual symptom)

```
WHERE [AsOfDate].[AsOfDate].[2019-02-01] → contributors.COUNT = 1,000,000   (epoch 8)
```

Despite HTTP 200, a genuinely committed transaction, and a real ~37-second unscoped rebuild against
Dremio, `2019-02-01` still shows the full 1,000,000 rows.

## Step 8 — Cross-node behavior

From a matching isolated run under this same Scenario 5 config: dq2 (untouched) still correctly shows
1,000,000 for `2019-02-01`; the query node, with dq1 masked, correctly returns 1,000,000 by dispatching
to dq2 — not because dq1's answer happened to be right, but because masking hid dq1's now-inconsistent
local state from qn's routing.

## Net result

An operation that looks entirely successful to the caller (HTTP 200, committed epoch, real cost paid —
~37–39s wall-clock every time) is a silent no-op on the actual data, and along the way it doubles the
aggregate-provider's point count and evicts the cache. This is the same mechanism as `bug-1.md`,
reconfirmed with full before/after provider/cache/heap metrics and real Dremio SQL evidence at Scenario
5's own configuration (unpartitioned `SINGLE` provider, not `PARTIAL_BY_DATE`) — no new Jira action
implied, since the underlying issue is already understood and the ticket already closed as
not-a-defect on ActiveViam's side.
