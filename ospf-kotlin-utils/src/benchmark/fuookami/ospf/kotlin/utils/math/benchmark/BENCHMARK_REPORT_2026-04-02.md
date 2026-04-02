# ospf-kotlin-utils/math Symbol Benchmark Baseline Report

## Run Meta
- Date: 2026-04-02
- Branch/Commit: develop / 8786525f
- Module: `ospf-kotlin-utils`
- Forks: 0 (in-host VM, for initial baseline only)
- Warmup iterations: 2 (default)
- Measurement iterations: 3 (default)
- Total time: 10:01 min

## Environment
- OS: Windows 10 Pro 10.0.19045 (MINGW64)
- CPU: AMD Ryzen 9 7945HX with Radeon Graphics
- Memory: 32 GB (estimated)
- JDK version: Java 17.0.12 LTS (GraalVM)
- Maven version: Apache Maven 3.9.12
- JVM args: GraalVM default (JVMCI enabled)

## Commands

### Compile
```powershell
mvn -pl ospf-kotlin-utils clean compile -DskipTests=true
```

### Run all benchmarks
```powershell
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*Benchmark.* 0" org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

### Run Symbol benchmark only
```powershell
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*MathSymbolBenchmark.* 0" org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

### Run with forks (for accurate results)
```powershell
mvn -pl ospf-kotlin-utils -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=".*MathSymbolBenchmark.* 1" org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

## Result Summary

| Benchmark | Mode | Score | Error | Unit | Description |
|-----------|------|-------|-------|------|-------------|
| combineTermsStress | thrpt | 30.265 | ±3.858 | ops/ms | **HOTSPOT**: 300 monomials merge |
| compileEvalCanonical | thrpt | 1594.578 | ±333.643 | ops/ms | Compile + combineTerms |
| compileGradientCanonical | thrpt | 1585.116 | ±209.485 | ops/ms | Compile gradient + combineTerms |
| evaluateOrderedCanonical | thrpt | 4036.542 | ±365.934 | ops/ms | Direct evaluation (fastest) |
| gradientCanonicalSize | thrpt | 853.903 | ±15.628 | ops/ms | Gradient computation |
| invokeCompiledEval | thrpt | 1458.180 | ±759.843 | ops/ms | Compiled function call |
| invokeCompiledGradientSize | thrpt | 1355.668 | ±151.691 | ops/ms | Compiled gradient call |
| matrixFormCanonicalSize | thrpt | 614.952 | ±109.871 | ops/ms | Matrix form conversion |
| polynomialDivScalar | thrpt | 65381.033 | ±3582.858 | ops/ms | Scalar division (very fast) |
| polynomialMinus | thrpt | 19344.253 | ±594.376 | ops/ms | Polynomial subtraction |
| polynomialPlus | thrpt | 24937.655 | ±934.730 | ops/ms | Polynomial addition |
| polynomialTimesScalar | thrpt | 63127.701 | ±40406.201 | ops/ms | Scalar multiplication (very fast) |

## Performance Tier Analysis

| Tier | Ops/ms | Benchmarks | Characteristics |
|------|--------|------------|-----------------|
| **T1 (Very Fast)** | >50K | `polynomialDivScalar`, `polynomialTimesScalar` | Scalar ops, minimal allocation |
| **T2 (Fast)** | 15K-25K | `polynomialPlus`, `polynomialMinus` | List concatenation, no merge |
| **T3 (Medium)** | 1K-4K | `compileEval/Gradient`, `evaluateOrdered`, `invokeCompiled` | With combineTerms or evaluation |
| **T4 (Slow)** | <1K | `gradientCanonicalSize`, `matrixFormCanonicalSize` | Complex computation |
| **T5 (Hotspot)** | <100 | `combineTermsStress` | Heavy merge (300 monomials) |

## Hotspot Path Analysis (S-PERF-1)

### Identified Hotspots

1. **combineTerms** (`CanonicalGeneric.kt:85-105`)
   - Uses `LinkedHashMap` for merging
   - Powers normalization via sorting
   - 3x conversion chain: `Typed → Generic → combine → Typed`
   - **Impact**: 30 ops/ms for 300 monomials (vs 25K ops/ms for plus)

2. **Polynomial arithmetic** (`CanonicalPolynomial.kt:49-67`)
   - `plus/minus` creates new List without merge (deferred to combineTerms)
   - `times/div` scalar: minimal overhead, ~65K ops/ms

3. **scaleByInt** (`CompileGeneric.kt:213-224`)
   - Uses `repeat` loop for coefficient scaling
   - Embedded in `compileGradientCanonical`

### Optimization Candidates

| Priority | Path | Current | Target | Approach |
|----------|------|---------|--------|----------|
| P1 | combineTerms conversion chain | 30 ops/ms | 50+ ops/ms | Direct Typed operation |
| P2 | powers normalization sorting | - | - | Cache sorted powers |
| P3 | scaleByInt repeat loop | - | - | Multiplication when possible |

## Baseline Comparison (Future Use)

| Benchmark | Current (2026-04-02) | Baseline | Delta | Status |
|-----------|---------------------|----------|-------|--------|
| combineTermsStress | 44.792 ops/ms | 30.265 ops/ms | **+48%** | ✅ Optimized |
| compileEvalCanonical | 1594.578 ops/ms | - | - | **Initial baseline** |
| polynomialPlus | 24937.655 ops/ms | - | - | **Initial baseline** |
| polynomialMinus | 19344.253 ops/ms | - | - | **Initial baseline** |

### Optimization Log

| Date | Change | Impact |
|------|--------|--------|
| 2026-04-02 | Direct Typed combineTerms (eliminate Generic conversion) | +48% combineTermsStress |

## Observations

1. **combineTermsStress is the primary hotspot** - 1000x slower than polynomial arithmetic
2. **Scalar operations are extremely fast** - No allocation, direct coefficient modification
3. **Evaluation path is well-optimized** - `evaluateOrdered` at 4K ops/ms
4. **Conversion chain overhead** - Each `toGeneric*` + `to*` pair adds overhead

## Risks / Notes

1. **Non-forked run warning** - Results may have JIT warmup variance; use forks=1+ for production baseline
2. **GraalVM specific** - Results may differ on standard HotSpot JVM
3. **Error bars wide** - Some benchmarks (e.g., `polynomialTimesScalar`) show high variance; need more iterations

## Next Steps (S-PERF-3 Complete)

- [ ] Run with forks=3 for stable baseline
- [ ] Compare with Rust ospf-rust-math benchmark
- [ ] Profile combineTerms hotspot with JMH profiler
- [ ] Implement threshold alerting (S-PERF-4)