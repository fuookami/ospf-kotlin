# Benchmark Result Template

## Run Meta
- Date:
- Branch/Commit:
- Module: `ospf-kotlin-utils`
- Forks:
- Warmup iterations: 2 (default)
- Measurement iterations: 3 (default)

## Environment
- OS:
- CPU:
- Memory:
- JDK version:
- Maven version:
- JVM args:

## Commands

### Compile
```powershell
mvn -pl ospf-kotlin-utils clean compile -DskipTests=true
```

### Run all benchmarks
```powershell
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*Benchmark.* 0" org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

### Run specific benchmark
```powershell
# Symbol benchmark
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*MathSymbolBenchmark.* 0" org.codehaus.mojo:exec-maven-plugin:3.5.0:java

# Geometry benchmark
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*MathGeometryBenchmark.* 0" org.codehaus.mojo:exec-maven-plugin:3.5.0:java

# TypedValueRange benchmark
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*MathTypedValueRangeBenchmark.* 0" org.codehaus.mojo:exec-maven-plugin:3.5.0:java

# ValueRange benchmark
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*MathValueRangeBenchmark.* 0" org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

### Run with forks (for accurate results)
```powershell
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*MathSymbolBenchmark.* 1" org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

## Result Summary
| Benchmark | Mode | Score | Error | Unit |
|-----------|------|-------|-------|------|
|           |      |       |       |      |

## Baseline Comparison
| Benchmark | Current | Baseline | Delta (%) | Status |
|-----------|---------|----------|-----------|--------|
|           |         |          |           |        |

## Threshold Alerting Rules (S-PERF-4)

### Performance Regression Thresholds

| Benchmark | Warning (≥) | Critical (≥) | Baseline |
|-----------|-------------|--------------|----------|
| combineTermsStress | 5% slower | 10% slower | 44.792 ops/ms |
| compileEvalCanonical | 5% slower | 10% slower | 1594.578 ops/ms |
| compileGradientCanonical | 5% slower | 10% slower | 1585.116 ops/ms |
| evaluateOrderedCanonical | 5% slower | 10% slower | 4036.542 ops/ms |
| polynomialPlus | 5% slower | 10% slower | 24937.655 ops/ms |
| polynomialMinus | 5% slower | 10% slower | 19344.253 ops/ms |
| polynomialTimesScalar | 5% slower | 10% slower | 63127.701 ops/ms |
| polynomialDivScalar | 5% slower | 10% slower | 65381.033 ops/ms |

### Status Definitions
- ✅ **OK**: Delta within ±5% of baseline
- ⚠️ **WARNING**: Delta ≥5% slower (requires investigation)
- 🔴 **CRITICAL**: Delta ≥10% slower (requires immediate action or revert)

### Weekly Update Checklist
1. Run benchmark with forks=1+ on same machine/JDK
2. Compare with previous baseline using threshold rules
3. Update baseline if improvement confirmed (commit with benchmark report)
4. Log regression in GitHub issue if WARNING/CRITICAL
5. Archive report as `BENCHMARK_REPORT_YYYY-MM-DD.md`

## Observations
-

## Risks / Notes
-