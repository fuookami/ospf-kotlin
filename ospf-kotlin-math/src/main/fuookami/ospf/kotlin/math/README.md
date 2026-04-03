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

# PowerShell
mvn --% -pl ospf-kotlin-utils -Dexec.classpathScope=test -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=.*MathOrdinaryBenchmark.* org.codehaus.mojo:exec-maven-plugin:3.5.0:java

# Bash / Zsh
mvn -pl ospf-kotlin-utils -Dexec.classpathScope=test -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args='.*MathOrdinaryBenchmark.*' org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

`BenchmarkLauncher` first argument is include pattern.
Second optional argument is fork count (default `0`, recommended for current Maven exec path).
For example:

```text
.*MathTypedValueRangeBenchmark.* 0
```
