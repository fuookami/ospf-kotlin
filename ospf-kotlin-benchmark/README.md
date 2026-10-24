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

JMH scores are machine-sensitive. CI smoke runs should validate benchmark executability and preserve artifacts, not compare absolute scores as hard gates.
