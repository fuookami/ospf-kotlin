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

## 运行 Benchmark

编译 benchmark 模块：

```powershell
mvn -B -ntp -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

运行 smoke benchmark：

```powershell
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

## 结果报告

benchmark JSON 结果默认输出到 `ospf-kotlin-benchmark/target/benchmark-results/`。比较脚本会生成 Markdown 趋势报告，但不设置性能硬门禁：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir .\ospf-kotlin-benchmark\target\benchmark-results `
  -Dataset small
```

## 说明

JMH 分数对机器环境敏感。CI smoke 应验证 benchmark 可运行并保留 artifact，不应把绝对分数比较作为硬门禁。
