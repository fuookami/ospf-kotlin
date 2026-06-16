# demo4 — 机组排班（甘特排程框架）

:us: [English](README.md) | :cn: 简体中文

## 简介

`demo4` 演示使用 `gantt_scheduling` 框架进行**机组排班**。它建模航班恢复场景，其中机组成员必须被分配到飞行任务，同时满足执勤时间限制、连接规则和机队平衡约束。演示还包括一个泛型数量示例，展示如何将框架类型用于各种排程维度。

## 作用范围

- 建模机组成员（飞行员、乘务员）及其职级和排班
- 定义飞行任务、航段和恢复场景
- 生成机组束（可行的执勤序列）
- 将束编译为航班链接排班
- 施加机队平衡和容量约束
- 使用泛型数量表示时间、成本、资源容量和切换

## 模块结构

| 领域上下文 | 职责 |
| --- | --- |
| `crew` | 机组领域：飞行员、乘务员、职级、排班、中转时间 |
| `task` | 飞行任务：航段、飞机、机场、旅客、恢复、维护 |
| `rule` | 排班规则：链接、锁定、流控、限制、成本计算 |
| `cargo` | 货物领域：聚合和上下文 |
| `passenger` | 旅客管理：取消、变更、容量约束 |
| `bunch_generation` | 从飞行图生成可行机组束（pricing 子问题） |
| `bunch_compilation` | 编译束：机队平衡、航班链接、航班容量（master 约束） |
| `bunch_selection` | 分支定阶算法选择束（branch-and-price 编排） |
| `infrastructure` | 求解器、DTO（输入/输出）、语义参数 |

## 架构

```
demo4/
  Application.kt              -- 入口和泛型数量示例
  infrastructure/
    Solver.kt                  -- 求解器配置
    SemanticParameter.kt       -- 语义参数定义
    Instant.kt                 -- 时间工具
    dto/
      Input.kt                 -- 输入 DTO 定义
      Output.kt                -- 输出 DTO 定义
  domain/
    crew/
      CrewContext.kt           -- 机组上下文
      Aggregation.kt           -- 机组聚合
      model/                   -- Crew, Pilot, CrewMan, 职级, 排班
    task/
      FlightTaskContext.kt     -- 飞行任务上下文
      Aggregation.kt           -- 任务聚合
      model/                   -- FlightLeg, Aircraft, Airport, Passenger 等
    rule/
      RuleContext.kt           -- 规则上下文
      Aggregation.kt           -- 规则聚合
      model/                   -- Link, Lock, FlowControl, Restriction
      service/                 -- CostCalculator, FeasibilityJudger 等
    cargo/
      CargoContext.kt          -- 货物上下文
      Aggregation.kt           -- 货物聚合
    passenger/
      PassengerContext.kt      -- 旅客上下文
      Aggregation.kt           -- 旅客聚合
      model/                   -- Passenger, PassengerAmount 等
      service/limits/          -- 取消、变更、容量约束
    bunch_generation/
      BunchGenerationContext.kt -- 束生成上下文
      Aggregation.kt           -- 束生成聚合（graphs, reverse, initialBunches）
      model/
        Graph.kt               -- 飞行图（Node, Edge, Graph）
        FlightTaskReverse.kt   -- 可逆任务对管理
      service/
        Operator.kt            -- 类型别名（RuleChecker, CostCalculator 等）
        FlightTaskFeasibilityJudger.kt -- 10 步可行性检查
        RouteGraphGenerator.kt -- BFS 路由图生成
        FlightTaskBunchGenerator.kt -- Label Setting 算法
        InitialFlightTaskBunchGenerator.kt -- 初始列生成
        AggregationInitializer.kt -- 初始化编排
    bunch_compilation/
      BunchCompilationContext.kt -- 束编译上下文
      Aggregation.kt           -- 编译聚合
      model/                   -- FlightLink, FleetBalance, FlightCapacity
      service/limits/          -- FlightLinkLimit, FleetBalanceLimit
    bunch_selection/
      BunchSelectionContext.kt -- 束选择上下文
      service/BranchAndPriceAlgorithm.kt -- 分支定阶求解器
```

## bunch_generation 详解

`bunch_generation` 是 branch-and-price 中的 **pricing 子问题**，负责生成负 reduced cost 的新列。

### 核心流程

1. **初始化**（`AggregationInitializer`）
   - 构建 `FlightTaskReverse`（可逆任务对）
   - 为每架飞机生成 `RouteGraph`（BFS）
   - 生成初始 bunch（`InitialFlightTaskBunchGenerator`）

2. **定价**（`FlightTaskBunchGenerator`）
   - Label Setting 算法
   - 拓扑排序遍历图节点
   - 沿出边扩展标签
   - 累加任务覆盖 shadow price
   - 支配剪枝（reduced cost、delay、aircraft change）
   - 输出负 reduced cost bunch

3. **输出**
   - 新 bunch 列
   - 定价诊断（迭代数、cut 数、轨迹）

### 关键概念

| 概念 | 说明 |
| --- | --- |
| `shadow price` | master 约束的对偶值，反映约束的边际成本 |
| `reduced cost` | 原始成本 - shadow price 扣减，负值表示有价值的列 |
| `initial bunch` | 每架飞机的初始可行列，包含 locked task |
| `generated bunch` | 通过 pricing 生成的新列 |
| `dominance` | 同末端节点下，时间和成本均不劣的标签保留 |

### 与 bunch_compilation 和 bunch_selection 的边界

| 模块 | 职责 | 不承担 |
| --- | --- | --- |
| `bunch_generation` | route graph、initial bunch、pricing | master 约束、fleet balance、solution 解析 |
| `bunch_compilation` | master 约束注册、fleet balance、flight link | Label Setting、route graph、reduced cost |
| `bunch_selection` | branch-and-price 编排、shadow price 提取、add columns | 具体 pricing 逻辑 |

## 泛型数量示例

`Demo4GenericQuantitySample` 对象演示如何使用框架泛型类型：

| 类型 | 说明 |
| --- | --- |
| `TimeRange` | 带起止时间的时间区间 |
| `Cost<Flt64>` | 成本数量 |
| `MaterialDemand<Flt64>` | 物料需求数量 |
| `ResourceCapacity<Flt64>` | 资源容量数量 |
| `TaskTime<Flt64>` | 任务时间数量 |
| `Switch<Flt64>` | 切换/变更数量 |
| `Makespan<Flt64>` | 完工时间数量 |

## 用法

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo4.Application

suspend fun main() {
    val app = Application()
    // ... 配置输入（机组、航班、规则）
    // val result = app(input)
}
```

泛型数量示例：

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo4.Demo4GenericQuantitySample

fun main() {
    Demo4GenericQuantitySample.run()
}
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
