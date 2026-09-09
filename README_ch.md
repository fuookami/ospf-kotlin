# ospf-kotlin

[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fuookami.ospf.kotlin/ospf-kotlin)](https://mvnrepository.com/artifact/io.github.fuookami.ospf.kotlin/ospf-kotlin)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-yellow.svg?logo=kotlin)](http://kotlinlang.org)

## 介绍

ospf-kotlin 是 ospf 的 Kotlin/JVM 实现版本，更详细的介绍、文档与样例可以参考主仓库与文档页面：

ospf：https://github.com/fuookami/ospf

文档：https://fuookami.github.io/ospf/

:us: [English](README.md) | :cn: 简体中文


## 架构概览

工作区采用分层架构：

1. `utils`、`multiarray`、`math`、`quantities` 提供可复用的基础能力。
2. `core` 负责优化建模原语和求解器模型转换。
3. `framework` 添加求解器编排、流水线抽象、影子价格、持久化契约和远程求解。
4. 领域框架模块围绕 `MetaModel` 组装可复用的业务建模上下文。
5. `example` 展示当前公开流程和兼容性路径。

领域框架模块应将优化语义保持在上下文/聚合/模型组件/流水线层。应用服务负责协调求解器选择、生命周期、追踪/KPI/渲染组装和恢复边界。

## 约束规划边界

`ospf-kotlin-core` 提供整数域约束规划模型，支持不可变快照、稳定 ID、来源验证和统一的求解器报告。通用 MIP 降阶器内部使用检查算术，仅当所有整数系数、边界和生成的 Big-M 都可精确表示时才跨越现有的浮点求解器边界。

SCIP CP 入口是一个严格的有限 MIP 支持门面，并非原生 SCIP/CIP CP 后端。声明的 CP 能力范围是完整的：通用 MIP 降阶对支持的有限子集返回验证过的精确降阶结果，对 `Cumulative`、`Circuit`、`Automaton`、`Reservoir` 返回结构化的不支持错误；原始累积 FFI 是有条件的研究探针，真正的增量 CP 会话仍不支持。

## 模块文档

顶层 Maven 模块现在统一遵循双语 README 约定：英文文档使用 `README.md`，简体中文文档使用 `README_ch.md`，两个文件都包含语言互链。

| 模块 | 用途 | README |
| --- | --- | --- |
| `ospf-kotlin-dependencies` | 仓库依赖管理 BOM | [EN](ospf-kotlin-dependencies/README.md) / [中文](ospf-kotlin-dependencies/README_ch.md) |
| `ospf-kotlin-parent` | 共享 Maven 父 POM 与构建默认值 | [EN](ospf-kotlin-parent/README.md) / [中文](ospf-kotlin-parent/README_ch.md) |
| `ospf-kotlin-utils` | 通用工具和函数式结果类型 | [EN](ospf-kotlin-utils/README.md) / [中文](ospf-kotlin-utils/README_ch.md) |
| `ospf-kotlin-multiarray` | 多维数组基础 | [EN](ospf-kotlin-multiarray/README.md) / [中文](ospf-kotlin-multiarray/README_ch.md) |
| `ospf-kotlin-math` | 数学、代数、几何和符号表达式 | [EN](ospf-kotlin-math/README.md) / [中文](ospf-kotlin-math/README_ch.md) |
| `ospf-kotlin-quantities` | 物理量与单位系统 | [EN](ospf-kotlin-quantities/README.md) / [中文](ospf-kotlin-quantities/README_ch.md) |
| `ospf-kotlin-core` | 优化建模核心 | [EN](ospf-kotlin-core/README.md) / [中文](ospf-kotlin-core/README_ch.md) |
| `ospf-kotlin-framework` | 共享 framework 抽象 | [EN](ospf-kotlin-framework/README.md) / [中文](ospf-kotlin-framework/README_ch.md) |
| `ospf-kotlin-core-plugin` | 求解器后端插件 | [EN](ospf-kotlin-core-plugin/README.md) / [中文](ospf-kotlin-core-plugin/README_ch.md) |
| `ospf-kotlin-framework-plugin` | framework 基础设施插件 | [EN](ospf-kotlin-framework-plugin/README.md) / [中文](ospf-kotlin-framework-plugin/README_ch.md) |
| `ospf-kotlin-framework-bpp1d` | 规划中的一维装箱框架 | [EN](ospf-kotlin-framework-bpp1d/README.md) / [中文](ospf-kotlin-framework-bpp1d/README_ch.md) |
| `ospf-kotlin-framework-bpp2d` | 早期阶段二维装箱框架 | [EN](ospf-kotlin-framework-bpp2d/README.md) / [中文](ospf-kotlin-framework-bpp2d/README_ch.md) |
| `ospf-kotlin-framework-bpp3d` | 三维装箱框架 | [EN](ospf-kotlin-framework-bpp3d/README.md) / [中文](ospf-kotlin-framework-bpp3d/README_ch.md) |
| `ospf-kotlin-framework-csp1d` | 一维下料框架 | [EN](ospf-kotlin-framework-csp1d/README.md) / [中文](ospf-kotlin-framework-csp1d/README_ch.md) |
| `ospf-kotlin-framework-csp2d` | 规划中的二维下料框架 | [EN](ospf-kotlin-framework-csp2d/README.md) / [中文](ospf-kotlin-framework-csp2d/README_ch.md) |
| `ospf-kotlin-framework-gantt-scheduling` | 甘特调度框架 | [EN](ospf-kotlin-framework-gantt-scheduling/README.md) / [中文](ospf-kotlin-framework-gantt-scheduling/README_ch.md) |
| `ospf-kotlin-framework-network-scheduling` | 网络调度 / VRPTW 分支定价框架 | [EN](ospf-kotlin-framework-network-scheduling/README.md) / [中文](ospf-kotlin-framework-network-scheduling/README_ch.md) |
| `ospf-kotlin-starters` | starter 依赖包 | [EN](ospf-kotlin-starters/README.md) / [中文](ospf-kotlin-starters/README_ch.md) |
| `ospf-kotlin-example` | 示例和兼容性测试 | [EN](ospf-kotlin-example/README.md) / [中文](ospf-kotlin-example/README_ch.md) |
| `ospf-kotlin-benchmark` | JMH benchmark smoke 与报告 | [EN](ospf-kotlin-benchmark/README.md) / [中文](ospf-kotlin-benchmark/README_ch.md) |

新增模块文档时，请使用 [docs/README_TEMPLATE.md](docs/README_TEMPLATE.md) 和 [docs/README_TEMPLATE_ch.md](docs/README_TEMPLATE_ch.md)。

## 安装

版本要求：

* JDK: 17+ or 8+
* Maven: 3+

ospf-kotlin 已经发布到 maven 中央仓库，因此，如果你使用 maven 的话，只需要在 pom.xml 文件里面添加一个依赖即可：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

如果你需要使用一维装箱开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp1d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp1d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

如果你需要使用二维装箱开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp2d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp2d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

如果你需要使用三维装箱开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp3d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp3d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

如果你需要使用一维下料开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp1d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp1d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

如果你需要使用二维下料开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp2d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp2d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

如果你需要使用甘特图排程开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-gantt-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-gantt-scheduling-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

如果你需要使用网络流规划开发包，则在 pom.xml 文件里添加依赖：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-network-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-network-scheduling-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```


## 版本记录

完整变更记录请查看 [1.1.0 版本说明](changelog/1.1.0_ch.md)。

## Benchmark 基线

基准模块：`ospf-kotlin-benchmark`（JMH 1.37），当前覆盖 `multiarray` / `math` / `core` 以及 `core-plugin dump` 热点路径。

编译 benchmark 模块：

```bash
mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

执行 `small` 烟测（示例）：

```bash
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

执行 `core-plugin dump` 烟测（示例）：

```bash
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CorePluginDumpBenchmark.prepareVariableDumpingDataHotPath.* small 1 1 1"
```

当前支持 `small`/`medium`/`large` 数据规模。`medium`/`large` 仅用于手动趋势对比，不进入默认 CI 硬门禁。

benchmark 结果文件默认输出到：

1. 默认路径：`ospf-kotlin-benchmark/target/benchmark-results/*.json`
2. 可选自定义路径：`-Dexec.args="... json target/benchmark-results/custom.json"`

CI 会上传 `ospf-kotlin-benchmark/target/benchmark-results/` 目录作为 artifact，仅用于结果留存（包含 `ci-smoke.json`、`baseline-smoke.json`、`current-smoke.json`、`trend-smoke.md`），不作为性能硬门禁。

比较两份 JMH JSON 结果并输出 Markdown 趋势报告：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -Baseline .\ospf-kotlin-benchmark\target\benchmark-results\baseline-small.json `
  -Current .\ospf-kotlin-benchmark\target\benchmark-results\current-small.json
```

当省略 `-Output` 且命名遵循 `baseline-<dataset>.json` / `current-<dataset>.json` 时，脚本会默认在同目录输出 `trend-<dataset>.md`。

也可以使用目录模式：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir .\ospf-kotlin-benchmark\target\benchmark-results `
  -Dataset small
```

如果目录中仅存在一组可匹配的 `baseline-*.json` / `current-*.json`，可省略 `-Dataset`。

如需显式输出路径，也可传入 `-Output`：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -Baseline .\ospf-kotlin-benchmark\target\benchmark-results\baseline-small.json `
  -Current .\ospf-kotlin-benchmark\target\benchmark-results\current-small.json `
  -Output .\ospf-kotlin-benchmark\target\benchmark-results\trend-small.md
```

比较脚本只生成报告，不设置性能硬门禁，因为 JMH 分数对机器环境敏感。

benchmark 轻量 CI smoke 口径（只验证可运行，不比较绝对数值）：

```powershell
mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

P21-1 基线运行环境：

1. JDK：GraalVM JDK 17.0.12
2. Maven：Apache Maven 3.9.12
3. OS：Windows（PowerShell）
4. JVM 参数建议（缓解频繁 CodeHeap warning）：
   - PowerShell（当前会话）：
     - ``$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"``
   - Windows 持久化用户变量：
     - ``setx MAVEN_OPTS "-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"``
   - bash/zsh：
     - `export MAVEN_OPTS="-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"`

## 开源协议

ospf-kotlin 是根据 Apache 许可证 2.0 版本的条款授权的。

更多信息请查看 [LICENSE](LICENSE)。
