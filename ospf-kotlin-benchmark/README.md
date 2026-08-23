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

## Running Benchmarks

Compile the benchmark module:

```powershell
mvn -B -ntp -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

Run a smoke benchmark:

```powershell
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

Run the CP benchmark fixture without a native solver:

```powershell
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*ConstraintProgrammingBenchmark.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/cp-small.json"
```

The CP fixture intentionally uses the Fake solver for portable smoke runs and also exposes
exact MIP lowering-build benchmarks for fixed, optional, and variable-duration intervals.
Native SCIP solve timings, node counts, and peak memory remain a separate integration concern
and must be recorded with the plugin integration suite. / CP 夹具的可移植 smoke 使用 Fake solver，
并提供固定、可选和可变时长 interval 的 exact MIP 降阶构建基准；SCIP 原生求解耗时、节点数和峰值内存
仍需由插件集成套件单独记录。

## Result Reports

Benchmark JSON results are written under `ospf-kotlin-benchmark/target/benchmark-results/` by default. The comparison script generates a Markdown trend report without enforcing a performance gate:

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir .\ospf-kotlin-benchmark\target\benchmark-results `
  -Dataset small
```

Benchmark correctness and replay metadata use the unified `SolveReport<V>` contract. New benchmark
fixtures should record the report's termination reason, solution presence, diagnostics, provenance,
and model/configuration fingerprints; legacy solver output views are compatibility adapters only.
The remaining plugin migration and capability scope is tracked in [`plans/solver_cp.md`](../plans/solver_cp.md).

## Notes

JMH scores are machine-sensitive. CI smoke runs should validate benchmark executability and preserve artifacts, not compare absolute scores as hard gates.
