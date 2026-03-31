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
```

之后可在 IDE 或自定义启动器中运行 JMH Main 并指定 benchmark pattern。
推荐直接运行 `BenchmarkLauncher.main(...)`，可选第一个参数为 include pattern，例如：

```text
.*MathOrdinaryBenchmark.*
```
