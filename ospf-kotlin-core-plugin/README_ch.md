# ospf-kotlin-core-plugin

:us: [English](README.md) | :cn: 简体中文

## 简介

ospf-kotlin-core-plugin 是 OSPF（Open Solver Platform Framework）Kotlin 项目的**求解器插件模块**。它提供了具体的求解器实现，将核心求解器抽象层桥接到商业和开源优化求解器，以及元启发式算法。

所有插件遵循 `ospf-kotlin-core` 中定义的统一接口契约，支持在不修改优化模型的情况下透明切换求解器。

## 架构概览

```
┌────────────────────────────────────────────────────────────────────────────┐
│                           用户应用层                                       │
├────────────────────────────────────────────────────────────────────────────┤
│  ospf-kotlin-core  │  求解器抽象（LinearSolver, QuadraticSolver）          │
├────────────────────────────────────────────────────────────────────────────┤
│                     ospf-kotlin-core-plugin                                │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────┐ ┌───────┐ ┌──────┐        │
│  │ Gurobi  │ │ Gurobi11 │ │  COPT  │ │CPLEX │ │Hexaly │ │ SCIP │        │
│  └─────────┘ └──────────┘ └────────┘ └──────┘ └───────┘ └──────┘        │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌────────────┐ ┌─────────────────┐  │
│  │MindOPT  │ │  MOSEK   │ │ LINGO  │ │  OPTVerse  │ │   Heuristic     │  │
│  └─────────┘ └──────────┘ └────────┘ └────────────┘ │ GA PSO GWO SCA  │  │
│                                                       │ SAA MVO ...     │  │
│                                                       └─────────────────┘  │
├────────────────────────────────────────────────────────────────────────────┤
│  ospf-kotlin-math  │  ospf-kotlin-utils  │  ospf-kotlin-core             │
└────────────────────────────────────────────────────────────────────────────┘
```

## 插件模块

### 数学规划求解器

| 模块 | 求解器 | 状态 | 线性 | 二次 | 列生成 | Benders 分解 |
|------|--------|------|------|------|--------|--------------|
| `ospf-kotlin-core-plugin-gurobi` | Gurobi 10+ | :white_check_mark: | 是 | 是 | 是 | 是 |
| `ospf-kotlin-core-plugin-gurobi11` | Gurobi 11+ | :white_check_mark: | 是 | 是 | 是 | 是 |
| `ospf-kotlin-core-plugin-copt` | COPT（杉数） | :white_check_mark: | 是 | 是 | 是 | 是 |
| `ospf-kotlin-core-plugin-cplex` | CPLEX | :white_check_mark: | 是 | 是 | 是 | 是 |
| `ospf-kotlin-core-plugin-hexaly` | Hexaly | :white_check_mark: | 是 | 是 | 是 | — |
| `ospf-kotlin-core-plugin-scip` | SCIP | :white_check_mark: | 是 | 是 | 是 | 是 |
| `ospf-kotlin-core-plugin-mindopt` | MindOPT（达摩院） | :white_check_mark: | 是 | 是 | 是 | 是 |
| `ospf-kotlin-core-plugin-mosek` | MOSEK | :construction: | 是 | — | — | — |
| `ospf-kotlin-core-plugin-lingo` | LINGO | :construction: | — | — | — | — |
| `ospf-kotlin-core-plugin-optverse` | OPTVerse | :construction: | — | — | — | — |

### 元启发式算法

| 模块 | 算法 | 状态 | 说明 |
|------|------|------|------|
| `ospf-kotlin-core-plugin-heuristic` | GA | :white_check_mark: | 遗传算法——选择、交叉、变异、迁移 |
| | PSO | :white_check_mark: | 粒子群优化——惯性权重、加速 |
| | GWO | :white_check_mark: | 灰狼优化器——alpha/beta/delta 领导层次 |
| | SCA | :white_check_mark: | 正弦余弦算法——正弦余弦位置更新 |
| | SAA | :white_check_mark: | 模拟退火算法——温度调度 |
| | MVO | :white_check_mark: | 多元宇宙优化器——白洞/虫洞穿越 |
| | EVO | :construction: | 能量谷优化器 |
| | GCO | :construction: | 生发中心优化器 |
| | HCA | :construction: | 爬山算法 |
| | HS | :construction: | 和声搜索算法 |
| | NS | :construction: | 邻域搜索算法 |
| | SOA | :construction: | 海鸥优化算法 |
| | WarSO | :construction: | 战争策略优化算法 |
| | WCA | :construction: | 水循环算法 |

## 通用插件架构

每个数学规划求解器插件遵循相同的分层结构：

```
┌──────────────────────────────────────────────────────┐
│  {Solver}LinearSolver / {Solver}QuadraticSolver     │  公共 API（实现核心求解器接口）
├──────────────────────────────────────────────────────┤
│  {Solver}ColumnGenerationSolver                      │  列生成策略
│  {Solver}BendersDecompositionSolver                  │  Benders 分解策略
├──────────────────────────────────────────────────────┤
│  {Solver}SolverCallBack                              │  回调管理（配置、解分析）
│  {Solver}Variable                                    │  变量类型映射（Binary/Integer/Continuous）
│  {Solver}Constraint                                  │  约束符号映射（适用时）
├──────────────────────────────────────────────────────┤
│  {Solver}Solver (abstract)                           │  基类——init、solve、analyzeStatus、close
│  PluginSolverAsync                                   │  异步求解协程作用域
└──────────────────────────────────────────────────────┘
```

### 求解器生命周期

每个求解器插件遵循一致的生命周期：

1. **Init** — 创建求解器环境和模型（本地或远程服务器）
2. **Dump** — 将变量、约束和目标转换为求解器原生格式
3. **Configure** — 通过回调钩子应用求解器参数
4. **Solve** — 执行优化
5. **AnalyzeStatus** — 将求解器原生状态映射为 `SolverStatus`
6. **AnalyzeSolution** — 提取变量值、目标值和界
7. **Close** — 释放求解器资源

### 回调系统

每个求解器提供回调管理器（`{Solver}SolverCallBack`），支持在求解过程的多个时机插入钩子：

| 时机 | 说明 |
|------|------|
| `AfterModeling` | 模型构建完成后、求解前 |
| `Configuration` | 求解器参数配置阶段 |
| `AnalyzingSolution` | 求解后解分析阶段 |
| `AfterFailure` | 求解失败后 |

## 启发式算法架构

启发式模块实现了**策略驱动**设计：

```
┌────────────────────────────────────────────┐
│  {Algorithm}Algorithm                      │  主算法运行器
│    ├─ policy: Abstract{Algorithm}Policy    │  策略接口
│    ├─ invoke(model, runningCallBack)       │  执行算法
│    └─ 迭代跟踪、优良解维护                   │
├────────────────────────────────────────────┤
│  {Algorithm}Policy                         │  具体策略
│    ├─ HeuristicPolicy (base)               │  迭代/时间限制
│    └─ AbstractHeuristicPolicy              │  coerceIn、update、finished
├────────────────────────────────────────────┤
│  Population / Particle / Wolf / Chromosome │  个体表示
│    └─ Individual<ObjValue, V>              │  公共接口
└────────────────────────────────────────────┘
```

所有启发式算法支持：
- **泛型值类型** — 适用于任何 `RealNumber<V>` + `NumberField<V>`
- **单目标和多目标** — 通过类型别名（`GA` / `MulObjGA`）
- **迭代和时间限制** — 可配置的停止准则
- **运行回调** — 每次迭代观察进度
- **内存压力处理** — 迭代间自动清理

## 使用

### 添加求解器插件依赖

```kotlin
// build.gradle.kts
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi:1.1.0")
```

### 使用求解器

```kotlin
val solver = GurobiLinearSolver(
    config = SolverConfig(timeLimit = 60.seconds),
    callBack = GurobiLinearSolverCallBack().configuration { status, gurobi, variables, constraints ->
        gurobi.set(GRB.IntParam.Threads, 4)
        ok
    }
)
val result = solver(model)
```

### 使用启发式算法

```kotlin
val ga = GA(
    population = listOf(
        PopulationBuilder(
            eliteAmount = UInt64(2),
            densityRange = ValueRange(UInt64(20), UInt64(50)),
            mutationRateRange = ValueRange(Flt64(0.01), Flt64(0.1)),
            parentAmountRange = ValueRange(UInt64(2), UInt64(4))
        )
    ),
    migrationPeriod = UInt64(10),
    policy = GAPolicy(...)
)
val bestSolutions = ga(model)
```

## 依赖

- `ospf-kotlin-core` — 求解器抽象接口和模型类型
- `ospf-kotlin-math` — 数学类型（Flt64、UInt64、代数概念）
- `ospf-kotlin-utils` — 函数式工具（Ret、Try、Order）

## 测试

```bash
# 测试所有插件
mvn -pl ospf-kotlin-core-plugin test -DskipITs

# 测试特定插件
mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi test -DskipITs
mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic test -DskipITs
```

## 注意事项

- **Gurobi 10 与 Gurobi 11**：`gurobi` 模块使用 `gurobi.*` 导入（Gurobi 10 API），`gurobi11` 使用 `com.gurobi.gurobi.*`（Gurobi 11+ API，重定位包）。请选择与您的 Gurobi 安装匹配的模块。
- **SCIP 原生库**：SCIP 插件通过 JNA 加载原生库。JAR 部署时使用 `ScipSolver.loadLibraryInJar()`。
- **远程求解**：Gurobi 和 COPT 插件支持带认证的远程服务器连接。
- **求解器许可证**：每个商业求解器需要各自的许可证。框架不捆绑求解器二进制文件。
