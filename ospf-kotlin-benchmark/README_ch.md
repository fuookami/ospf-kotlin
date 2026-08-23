# ospf-kotlin-benchmark

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-benchmark` 包含基于 JMH 的基准入口，用于覆盖 OSPF Kotlin 中部分热点路径。它是只生成报告的 benchmark 模块，不作为库 artifact 发布。

## 作用范围

当前 benchmark 覆盖：

| 范围 | Benchmark |
| --- | --- |
| `multiarray` | block 访问和 contains 热点路径 |
| `math` | 符号组合热点路径 |
| `core` | core 模型热点路径 |
| `coreplugin` | core plugin dumping 热点路径 |
| `constraintprogramming` | CP snapshot 编码、Fake 穷举、固定/可选/可变时长排程、exact MIP 降阶和 portable checkpoint 捕获 |

## 运行 Benchmark

编译 benchmark 模块：

```powershell
mvn -B -ntp -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

运行 smoke benchmark：

```powershell
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

运行不依赖原生求解器的 CP benchmark 夹具：

```powershell
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*ConstraintProgrammingBenchmark.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/cp-small.json"
```

CP 夹具的可移植 smoke 刻意使用 Fake solver，并提供固定、可选和可变时长 interval 的 exact MIP 降阶构建基准；
SCIP 原生求解耗时、节点数和峰值内存仍需由插件集成套件单独记录。

## 结果报告

benchmark JSON 结果默认输出到 `ospf-kotlin-benchmark/target/benchmark-results/`。比较脚本会生成 Markdown 趋势报告，但不设置性能硬门禁：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir .\ospf-kotlin-benchmark\target\benchmark-results `
  -Dataset small
```

benchmark 的正确性和回放元数据统一使用 `SolveReport<V>`；新增夹具应记录终止原因、解存在性、
诊断、provenance 以及模型/配置指纹。旧 solver output 仅作为兼容适配视图保留。其余插件迁移与能力范围
见 [`plans/solver_cp.md`](../plans/solver_cp.md)。

## 说明

JMH 分数对机器环境敏感。CI smoke 应验证 benchmark 可运行并保留 artifact，不应把绝对分数比较作为硬门禁。
