# ospf-kotlin-utils/math

[English README (README.md)](./README.md)

`ospf-kotlin-utils/math` 是 OSPF Kotlin 的数学工具模块，包含：

1. 代数抽象与数值类型。
2. 基础数论与普通数学能力（gcd/lcm/分解等）。
3. 组合数学（组合/排列/笛卡尔积）。
4. 几何能力（点/向量/三角形/矩形/三角剖分）。
5. Symbol 表达式基础设施与运算能力。

## 当前进展

本模块已完成对 `ospf-rust-math` 的阶段性补齐工作：

1. 构建基线稳定化。
2. 数论 API 补齐。
3. 组合数学 API 深化。
4. 代数抽象与几何能力升级。
5. Symbol 标识体系增强。

详细阶段记录见：

- [daily.md](./daily.md)

## Benchmark（JMH）

已提供基础 JMH 基准入口：

- [MathOrdinaryBenchmark.kt](../../../../../../../../../test/fuookami/ospf/kotlin/utils/math/benchmark/MathOrdinaryBenchmark.kt)
- [BenchmarkLauncher.kt](../../../../../../../../../test/fuookami/ospf/kotlin/utils/math/benchmark/BenchmarkLauncher.kt)

编译 benchmark：

```bash
mvn -pl ospf-kotlin-utils -DskipTests=true -Pbench test-compile

# PowerShell
mvn --% -pl ospf-kotlin-utils -Dexec.classpathScope=test -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args=.*MathOrdinaryBenchmark.* org.codehaus.mojo:exec-maven-plugin:3.5.0:java

# Bash / Zsh
mvn -pl ospf-kotlin-utils -Dexec.classpathScope=test -Dexec.mainClass=fuookami.ospf.kotlin.utils.math.benchmark.BenchmarkLauncher -Dexec.args='.*MathOrdinaryBenchmark.*' org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

`BenchmarkLauncher` 第一个参数是 include pattern，第二个可选参数是 fork 数（默认 `0`，适配当前 Maven exec 路径）。
例如：

```text
.*MathTypedValueRangeBenchmark.* 0
```
