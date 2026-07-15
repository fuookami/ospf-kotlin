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

## Running Benchmarks

Compile the benchmark module:

```powershell
mvn -B -ntp -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

Run a smoke benchmark:

```powershell
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

## Result Reports

Benchmark JSON results are written under `ospf-kotlin-benchmark/target/benchmark-results/` by default. The comparison script generates a Markdown trend report without enforcing a performance gate:

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir .\ospf-kotlin-benchmark\target\benchmark-results `
  -Dataset small
```

## Notes

JMH scores are machine-sensitive. CI smoke runs should validate benchmark executability and preserve artifacts, not compare absolute scores as hard gates.
