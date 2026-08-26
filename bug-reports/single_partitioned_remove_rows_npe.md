# Bug report: `REMOVE_ROWS` refresh on a DQ node crashes with NPE under the fat provider partitioned by date

**Jira:** [PIVOT-14821](https://activeviam.atlassian.net/browse/PIVOT-14821)

## Summary

Calling `Application#refresh(ChangeDescription)` with `ChangeType.REMOVE_ROWS` against a cube whose
aggregate provider uses `withValuePartitioningOn(...)` (the "fat provider partitioned by date" —
`SINGLE_PARTITIONED` in our own `AggregateProviderMode` enum) throws a `NullPointerException` during
post-commit gauge/metrics registration, on every attempt. The transaction rolls back cleanly (no data
loss), but the removal never completes — the endpoint that triggers it always returns HTTP 500.

Confirmed on **6.1.23 and 6.2.0** (byte-for-byte identical stack trace), on a **single standalone data
node and on two-node clusters**, and at **every scale tested** (5 rows/day to 1,000,000 rows/day).

## Root cause

During `afterTransactionCommit`, `AMultiVersionPartitionedIndexedAggregateProvider.updateMeters()` →
`registerNewPartitionsMetrics()` → `registerPartitionGauges()` → `getTags()` →
`getPartitioningFieldsAndValues()` → `addEntryToTags()` → `getPartitioningFieldValue()` tries to read
the **just-removed** partition's tag value back out of the field's dictionary, to label that partition's
JMX gauge. Because the `REMOVE_ROWS` transaction already evicted that dictionary entry earlier in the
same commit, `IDictionary.read(int)` returns `null`, and the immediately-following `.toString()` call
NPEs with no null-check.

## The affected provider and trigger call

```java
// DataCubeConfig.java — cube definition, data-node profile
.withAggregateProvider().jit().withPartialProvider()
        .withName(AGGREGATE_PROVIDER_NAME)   // "ByAsOfDate"
        .bitmap()
        .includingOnlyLevels(asOfDateLevel)  // single level: AsOfDate
        .withValuePartitioningOn(ASOFDATE);  // <-- the line that causes this
```

```java
// DataMaintenanceController.java — REST DELETE endpoint, data-node profile
directQueryApplication.refresh(ChangeDescription.create(List.of(
        TableUpdateDetail.create("Trades", ChangeType.REMOVE_ROWS, ConditionFactory.equal("AsOfDate", date)),
        TableUpdateDetail.create("TradeAttributes", ChangeType.REMOVE_ROWS, ConditionFactory.equal("AsOfDate", date)))));
```

## Reproduction

Specific to DirectQuery's refresh orchestration — the identical removal via the plain in-memory
`IDatastore` transaction API (`datastore.edit(...)`, no DirectQuery involved) does **not** crash;
confirmed with `SinglePartitionedRemoveRowsNpeReproTest` in `src/test/java/com/activeviam/apps/bugrepro/`.

`DirectQueryRefreshRemoveRowsNpeReproTest` (same directory) reproduces the crash for real: it drives the
actual `Application#refresh(ChangeDescription)` API (same one production calls) against an **in-memory H2
database** instead of live Dremio, via the same generic-JDBC connector production uses — no Docker, no
live Dremio, no Spring context needed. It loads one `AsOfDate` (`ADD_ROWS`, succeeds), then removes it
(`REMOVE_ROWS`) — throwing a stack trace byte-for-byte identical (same class, method, message) to the one
below.

```
mvn -o test -Dtest=DirectQueryRefreshRemoveRowsNpeReproTest
```

## Stack trace (6.2.0, captured live — identical on 6.1.23)

```
java.lang.NullPointerException: Cannot invoke "Object.toString()" because the return value of "com.activeviam.tech.dictionaries.api.IDictionary.read(int)" is null
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.lambda$getPartitioningFieldValue$0(AMultiVersionPartitionedIndexedAggregateProvider.java:476)
	at java.base/java.util.Optional.map(Optional.java:260)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.getPartitioningFieldValue(AMultiVersionPartitionedIndexedAggregateProvider.java:476)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.addEntryToTags(AMultiVersionPartitionedIndexedAggregateProvider.java:452)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.lambda$getPartitioningFieldsAndValues$0(AMultiVersionPartitionedIndexedAggregateProvider.java:441)
	at gnu.trove.map.hash.TIntIntHashMap.forEachEntry(TIntIntHashMap.java:422)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.getPartitioningFieldsAndValues(AMultiVersionPartitionedIndexedAggregateProvider.java:438)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.getTags(AMultiVersionPartitionedIndexedAggregateProvider.java:430)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.registerGauges(AMultiVersionPartitionedIndexedAggregateProvider.java:376)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.registerPartitionGauges(AMultiVersionPartitionedIndexedAggregateProvider.java:366)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.registerNewPartitionsMetrics(AMultiVersionPartitionedIndexedAggregateProvider.java:326)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.updateMeters(AMultiVersionPartitionedIndexedAggregateProvider.java:98)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.MultiVersionPartitionedBitmapAggregateProvider.updateMeters(MultiVersionPartitionedBitmapAggregateProvider.java:96)
	at com.activeviam.activepivot.core.impl.private_.cube.provider.partition.impl.AMultiVersionPartitionedIndexedAggregateProvider.afterTransactionCommit(AMultiVersionPartitionedIndexedAggregateProvider.java:229)
	at com.activeviam.tech.mvcc.internal.impl.ATransactionalMultiVersion.commit(ATransactionalMultiVersion.java:162)
	at com.activeviam.activepivot.core.impl.internal.cube.provider.impl.AMultiVersionGlobalAggregateProvider.commit(AMultiVersionGlobalAggregateProvider.java:24)
	... (ForkJoin/transaction-commit machinery omitted)
```

The transaction rolls back cleanly — re-querying the "removed" value immediately afterward shows it
fully unchanged, at the pre-removal epoch. No data loss, but the removal never takes effect and the
endpoint always returns HTTP 500.

## Impact

Blocks any customer relying on a fat provider partitioned by date from ever removing a whole
partition's worth of data via a `REMOVE_ROWS` refresh — fails on every attempt, regardless of scale or
deployment topology.
