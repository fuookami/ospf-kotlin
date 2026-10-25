# ospf-kotlin-benchmark

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-benchmark` contains JMH-based benchmark entry points for selected OSPF Kotlin hot paths. It is a report-only benchmark module and is not published as a library artifact.

## Scope

Current benchmark coverage includes:

| Area | Benchmark |
| --- | --- |
| `multiarray` | Block access and contains hot paths |
| `math` | Symbol combination hot paths |
| `core` | Core model hot paths |
| `coreplugin` | Core plugin dumping hot paths |
| `constraintprogramming` | CP snapshot encoding, Fake enumeration, fixed/optional/variable scheduling, exact MIP lowering, and portable checkpoint capture |
| `preparation` | Production prime-cache queries, constant arrays, cold token positions, sparse-domain membership, and checkpoint capture/restore |

`PreparationHotPathBenchmark` calls production APIs rather than copying their algorithms. The
`small`/`medium`/`large` fixtures use N/2N/4N sizes: 10k/20k/40k consecutive cold-cache prime
queries, 1024/2048/4096 array elements, and 128/256/512 checkpoint variables. Its checkpoint
fixture includes incumbent values so variable-domain validation is measured. The
token fixture uses 1024/2048/4096 tokens; sparse domains use 4096/8192/16384 values with a
fixed 128 membership queries so only domain size scales. The cold-token fixture includes
list construction, solver ordering, and first lookups for all distinct keys. The same token
sizes also cover single/batch registration and clear-then-bulk assignment with a reversed
shared owner. Existing
`LikeP19`/`LikeSolver` fixtures are algorithm illustrations, not substitutes for production-path
regression measurements. A smoke run confirms executability only, not a speedup.

## Running Benchmarks

Compile the benchmark module:

```powershell
mvn -B -ntp -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

Run a smoke benchmark:

```powershell
$result = Join-Path $env:TEMP "ospf-multiarray-smoke.json"
mvn -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java "-Dexec.args=.*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json $result"
```

Run the CP benchmark fixture without a native solver:

```powershell
$result = Join-Path $env:TEMP "ospf-cp-small.json"
mvn -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java "-Dexec.args=.*ConstraintProgrammingBenchmark.* small 1 1 1 json $result"
```

The CP fixture intentionally uses the Fake solver for portable smoke runs and also exposes
exact MIP lowering-build benchmarks for fixed, optional, and variable-duration intervals.
Native SCIP solve timings, node counts, and peak memory remain a separate integration concern
and must be recorded with the plugin integration suite. / CP 夹具的可移植 smoke 使用 Fake solver，
并提供固定、可选和可变时长 interval 的 exact MIP 降阶构建基准；SCIP 原生求解耗时、节点数和峰值内存
仍需由插件集成套件单独记录。

## Result Reports

`NetworkIndexBuildBenchmark` measures real graph creation, outgoing reachability,
bidirectional components, reverse views, filtered reachability and warm traversal.
`size=256,1024,4096` scales nodes with four arcs per node. Construction-only and reverse-only
cases deliberately leave indexes unread; cold traversals include first-read build costs.
`ProjectionIndexBuildBenchmark` uses the same sizes for decision count and half as many
three-term local rows. `consumer=snapshot,pricing,master` separates consumption surfaces;
`coldRevision` creates a new projection while `warmRevision` reuses one. Both include full
production freshness checks, not just cached map reads. Use standard `org.openjdk.jmh.Main`
with matched forks/JVM/warmup and `-prof gc`, preserving temporary JSON/log files. These
fixtures do not measure production traffic frequency, solver time or background overlap.

`ModelPreparationBenchmark` measures the production token registration path for a long
dependency chain, sparse `ColumnProjection.master` assembly and scalar local-pricing
request/model preparation. Small/medium/large scales chain length or decision count at
N/2N/4N while keeping row, block and column counts fixed. Model construction and required
freshness checks inside the benchmark calls are timed; fixture input creation is not.
There is no solver, and the scalar-pricing fixture does not measure the opt-in BPP batch
pricer. Compare identical inputs and preserve GC allocation data and confidence intervals.

`MathPreparationBenchmark` calls production canonical gradients, axis reductions,
contraction, matching-order traversal, Boolean normalization and bounded worker mapping.
Small/medium/large scales one dimension at N/2N/4N. Both ordinary shallow trees and a
shared scalar DAG are measured; `CanonicalLinearPreparationBenchmark` adds fixed-size
single-factor gradient regression sentinels. Inputs and gradient compilation are outside
timing, so these scores do not measure compilation or end-to-end solver latency.
Run both classes with standard `org.openjdk.jmh.Main`, identical harness/JVM settings,
GC allocation profiling and temporary JSON/log paths. Report confidence intervals and
regressions as well as gains. The persistence query canonicalizer disables both
deduplication and sorting and does not use the structural-key memoization path; do not
attribute normalization benchmark gains to that query configuration.

`PilePreparationBenchmark.mergePiles` calls the production BPP3D item merger with
`count=64,128,256` and `scenario=unmergeable,multiplePiles`. Input construction is outside
timing; each invocation returns a fresh production result. Run this fixture through the
standard `org.openjdk.jmh.Main` CLI (not the dataset-only convenience runner), retaining
both scenarios, GC allocation data, confidence intervals and explicit temporary result
paths. Match harness, JVM, forks and warmup between baseline and current checkouts.
The fixture measures preparation, not solver or packing quality, and does not assert
linear complexity for unavoidable pairwise feasibility checks.

`PackingGeometryPreparationBenchmark.toSchema` calls the public production packing
renderer with `count=64,256,1024` and `scenario=sparse,denseFirstConflict`. Trial setup
is excluded. The sparse success path includes geometry validation and DTO creation;
the dense failure path stops at the first conflicting pair, before DTO creation.
Use standard `org.openjdk.jmh.Main` rather than the dataset-only convenience runner.
Keep both distributions, GC allocation data and confidence intervals when comparing
baseline/current; cylinder-heavy or numerically risky inputs retain exhaustive checks.

`PatternColumnBenchmark.addColumnBatches` calls the production BPP1D pattern-assignment
context (register, addColumns, close), without solving. Dataset small/medium/large uses
256/512/1024 distinct columns; `batchSize` independently varies between 1 and 32.
Use identical datasets and batch sizes for before/after runs. Scores include required
mutable-input validation and cache invalidation, not just term appending; a smoke run
does not establish an acceleration ratio or linear total complexity.

Pass an explicit result path under the system temporary directory for repository-local work:

```powershell
$result = Join-Path $env:TEMP "ospf-preparation-small.json"
mvn -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java "-Dexec.args=.*PreparationHotPathBenchmark.* small 2 3 5 json $result"
```

Repeat with `medium` and `large` and separate filenames to inspect growth with one scaled
dimension per benchmark. Compare the same JVM, heap, threads, forks, and warmup configuration;
these measurements include API validation/serialization costs, not native solver time.

The runner's fallback path is `ospf-kotlin-benchmark/target/benchmark-results/`; always override
it with a temporary path for repository-local runs. The comparison script generates a Markdown
trend report without enforcing a performance gate:

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir $env:TEMP `
  -Dataset small
```

Benchmark correctness and replay metadata use the unified `SolveReport<V>` contract. New benchmark
fixtures should record the report's termination reason, solution presence, diagnostics, provenance,
and model/configuration fingerprints; legacy solver output views are compatibility adapters only.
The remaining plugin migration and capability scope is tracked in [`plans/solver_cp.md`](../plans/solver_cp.md).

## Cross-version verification runbook

Formal before/after timing must compare two pinned revisions, not the working tree. The repository ships two
scripts for that; all logs, JSON and analysis files they produce belong in a temporary run root outside the
repository, and none of them may be committed.

1. `scripts/prepare-cross-version-run.ps1` resolves both revisions, creates two independent clones, ports one
   measurement-only fixture patch onto the baseline, builds each side against its own Maven repository, then
   verifies classpath isolation and that both sides expose identical JMH entry sets. Run with the JDK that
   matches the repository `jvmTarget` (the build also needs `-Dgpg.skip=true` because the root POM binds GPG
   signing to `verify`, which `install` triggers).
   Two traps cost a wasted round each, so they are checked by the script or here: a full-repository build without
   `-Pbench` recompiles the benchmark module without the JMH generators, which erases
   `target/classes/META-INF/BenchmarkList`; every later `org.openjdk.jmh.Main` run then fails with
   `Unable to find the resource: /META-INF/BenchmarkList`. After any non-`bench` build, re-run
   `mvn -pl ospf-kotlin-benchmark -Pbench compile` on each side before timing, and confirm with `-l` that both
   entry lists are still non-empty and identical.
2. Time one parameter cell at a time with `org.openjdk.jmh.Main` and that side's exported classpath, alternating
   baseline/candidate order per pair, one JVM fork per invocation, never both sides concurrently and never
   concurrently with a build. Allocation (`-prof gc`), JFR and heap diagnostics are separate rounds whose scores
   must not be mixed into the time round.
3. `scripts/analyze-paired-benchmark.ps1` turns paired fork results into a ratio, a Student t interval of paired
   log ratios as the primary decision, and an exact-enumeration bootstrap interval as a robustness cross-check;
   disagreements are reported as `INCONCLUSIVE-METHODS` instead of picking a verdict. Practical equivalence band
   defaults to `[0.95, 1.05]`. Self-test: `scripts/test-analyze-paired-benchmark.ps1`.
4. `CrossVersionSemanticDump` renders shared output semantics (plan sets, required order, numeric views,
   reachability, index results, cache counters) as stable lines. Run it on both sides and diff line by line; it
   must compile on the baseline as well, which is what proves the comparison only touches entries both revisions
   expose. It covers both full-output and budget-truncated generation loads (`stop=MaxPlans` versus
   `Exhausted`); the truncated section deliberately uses `workers = 1` only. With several workers, arrival order at the
   budget limit can depend on coroutine scheduling interleaves, so order equality there is not asserted without a
   dedicated re-run protocol; single-worker truncation is compared strictly, line by line.

A single host without exclusive CPU/memory quotas cannot support small-effect claims: measure the same build
repeatedly first, then treat any effect below the observed fork-level noise floor as insufficient evidence rather
than as a speedup or a regression.

## Notes

`SharedPrimeReadBenchmark.readyRead` uses one shared production `PrimeCache` with its
initial table prepared outside timing. Run standard `org.openjdk.jmh.Main` separately
with `-t 1` and `-t 4`; compare against `PreparationHotPathBenchmark.coldPrimeSequence`
to include cold expansion costs. `GenerationCacheBuildBenchmark.generate` runs full
production DFS for `scenario=equivalent,distinct,custom` and `workers=1,4`; the custom
scenario disables template reuse. It returns the real report, including cache hits,
misses and visited nodes. These are controlled workloads, not production hit-rate or
tail-latency evidence. Keep matching JVM/forks/warmup, `-prof gc`, confidence intervals
and temporary output paths; do not run builds concurrently with measurements.

Concurrent and cross-version fixtures (measurement support only, no production logic):

- `NetworkSharedGraphBenchmark` shares one immutable graph across all JMH threads, which
  the `Scope.Thread` `NetworkIndexBuildBenchmark` cannot expose. Cold batches are timed as
  one complete operation including dispatch, barrier alignment and completion of all
  readers, with `dispatchOnly` as the empty-dispatch control under the same protocol.
  Internal concurrency is the `readers` parameter, so keep `-t 1`.
- `TokenSharedReadBenchmark` performs first concurrent reads of one shared fixed list and
  steady reads of one ready list, plus a `dispatchOnly` control. `coldBuildWork` counts
  variable-identifier reads to expose the actual number of index builds; that counter is
  diagnostic and is not a time result. Keep `-t 1`.
- `TokenInvalidateRebuildBenchmark` measures the first read of the next generation after the
  index is invalidated: each cycle adds and removes one token (which clears the cache), then
  reads a stable token to force the rebuild, so net structure change is zero. `warmReadsOnly`
  and `mutationOnly` separate read cost from structural-mutation cost. Invalidation is driven
  by the calling thread's own add/remove, so no concurrent-write contract is required. State is
  `Scope.Thread`, meaning every JMH thread owns its own list; raising `-t` multiplies memory
  rather than sharing contention, so keep `-t 1` when comparing against the single-writer claim.
- `GenerationScaleBenchmark` adds NSum and FullSum (the DFS-only fixture cannot stand in
  for them) and the `workers=1,2,4,8` gradient, separating full-output from
  budget-truncated loads. Workers are internal parallelism, so keep `-t 1`.
- `PrimeMixedExpansionBenchmark` pairs readers with one explicit writer expansion
  sequence per operation on a fresh generation, with `readyControl`, `writerOnly` and
  `readerOnly` controls so expansion cost, reader cost and scheduling cost stay separable.
  Keep `-t 1` because the pool and barrier are internal.
- `PrimeReaderLatencyBenchmark` samples reader latency while a single writer expands
  continuously across the measurement window and rebuilds the generation at the ceiling,
  so covered reads are never measured against a finished sequence. Here `-t` is the reader
  count (1,2,4,8); there is no internal pool. Each iteration prints writer steps, rebuilds
  and final bound for starvation and degradation checks.

These fixtures call only production entries shared by the compared revisions, so a
baseline checkout can compile and run them without receiving candidate implementation
code. A smoke run confirms executability only; cold/warm separation, build counts and
per-iteration writer progress belong to diagnostic rounds, not speedup conclusions.

JMH scores are machine-sensitive. CI smoke runs should validate benchmark executability and preserve artifacts, not compare absolute scores as hard gates.
