# ospf-kotlin-utils/math

[中文文档 (README_ch.md)](./README_ch.md)

`ospf-kotlin-utils/math` contains math utilities for OSPF Kotlin:

1. Algebra concepts and number types.
2. Ordinary math (gcd/lcm/factorization and related helpers).
3. Combinatorics (combinations/permutations/cross products).
4. Geometry (point/vector/triangle/rectangle/triangulation).
5. Symbol expression foundation and operations.

## Current Progress

The module has completed major gap-filling work against `ospf-rust-math` in:

1. Build baseline stabilization.
2. Number theory API additions.
3. Combinatorics API depth additions.
4. Algebra abstraction and geometry capability upgrades.
5. Symbol identity infrastructure upgrades.

Detailed daily records and phased plans:

- [daily.md](./daily.md)

## Benchmark (JMH)

A baseline JMH benchmark is provided:

- [MathOrdinaryBenchmark.kt](../../../../../../../../../test/fuookami/ospf/kotlin/utils/math/benchmark/MathOrdinaryBenchmark.kt)
- [BenchmarkLauncher.kt](../../../../../../../../../test/fuookami/ospf/kotlin/utils/math/benchmark/BenchmarkLauncher.kt)

Run benchmark command:

```bash
mvn -pl ospf-kotlin-utils -DskipTests=true -Pbench test-compile
```

Then run `BenchmarkLauncher.main(...)` from IDE (or custom launcher).
Optional first argument is include pattern, for example:

```text
.*MathOrdinaryBenchmark.*
```
