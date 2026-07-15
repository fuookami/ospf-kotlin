# ospf-kotlin-framework-gantt-scheduling

:us: [English](README.md) | :cn: 简体中文

`ospf-kotlin-framework-gantt-scheduling` 是可复用的甘特排程优化框架,基于列生成与分支定价算法。它提供高级计划与排程（APS）、主生产计划（MPS）和批次排序计划（LSP）的通用排程内核。下游请求 DTO、公式语言、项目运行参数、租户上下文、心跳逻辑和 solver 插件选择留给业务适配层。

## 范围

框架覆盖以下领域能力：

- **任务建模**：任务定义、执行者分配、时间排程、任务状态、分配策略
- **任务编译**：任务级决策变量、约束与目标
- **任务束（路线）编译**：列生成主问题 — 束选择、成本与覆盖约束
- **任务束生成**：定价问题 — 带资源约束的标签设定最短路径
- **产能排程**：基于时间槽的执行者产能分配
- **资源管理**：可消耗资源产能约束，含松弛变量
- **产出/消耗跟踪**：物料产出与消耗，含需求满足约束
- **时间基础设施**：时间范围、时间窗口、工作日历、持续时间范围

下游业务概念（如超出步进图的复杂任务依赖 DAG、复杂班次日历和自定义成本公式）暂不在 framework 中建模；只有当它们先成为通用领域实体后，才进入本包。

## 架构概览

```
┌──────────────────────────────────────────────────────────┐
│                       应用层                               │
│          APS · MPS · LSP · BranchAndPriceAlgorithm       │
└──────────┬───────────────────────────────────┬───────────┘
           │                                   │
┌──────────▼──────────┐         ┌──────────────▼──────────────┐
│    束编译             │         │    任务编译                 │
│  （主问题）           │         │  （任务级 MILP）            │
│  · BunchAggregation  │         │  · TaskCompilation         │
│  · BunchSolution     │         │  · TaskAggregation         │
└──────────┬──────────┘         └──────────────┬─────────────┘
           │                                   │
┌──────────▼──────────┐         ┌──────────────▼─────────────┐
│    束生成             │         │    产能排程                 │
│  （定价问题）         │         │  · CapacityColumn          │
│  · Graph · Label     │         │  · CapacityCompilation     │
│  · SlotBasedBunchGen │         │  · IterativeCapacityComp   │
└──────────┬──────────┘         └──────────────┬─────────────┘
           │                                   │
┌──────────▼───────────────────────────────────▼─────────────┐
│                     领域基础层                               │
│  Task Context · Resource Context · Produce Context         │
└──────────────────────┬────────────────────────────────────┘
                       │
┌──────────────────────▼────────────────────────────────────┐
│                     基础设施层                               │
│  TimeRange · TimeWindow · TimeSlot · DurationRange         │
│  WorkingCalendar · LocalDateOffset                         │
└───────────────────────────────────────────────────────────┘
```

## 核心概念

### 任务

**Task** 表示可排程的工作单元。关键属性：

- **执行者**：执行任务的资源/设备
- **排程时间**：计划开始时间
- **持续时间**：任务执行时长
- **时间窗口**：最早开始 / 最晚结束约束
- **状态**：`NotAdvance`、`NotDelay`、`NotCancel`、`NotCancelPreferred`、`NotExecutorChange`、`Parallelable`、`Divisible`

未计划任务尚未分配执行者和时间。已计划任务基于任务计划（含可选分配策略）推导排程。

### 任务束（路线）

**TaskBunch** 是分配给同一执行者的有序任务组 — 类似车辆路径问题中的"路线"。束是列生成中的列：每个束代表一个执行者的一种可行排程。

### 列生成

框架采用经典的两阶段列生成架构：

1. **主问题（RMP）**：选择束覆盖所有任务并最小化成本，满足任务覆盖、执行者产能和资源约束
2. **定价问题**：在有向图上使用标签设定算法（带资源约束的最短路径）生成负简约成本的新束

### 分支定价

`BranchAndPriceAlgorithm` 编排完整求解流程：

1. 注册初始束（列）
2. 求解限制主问题的 LP 松弛
3. 从对偶解提取影子价格
4. 通过全局和局部定价生成新列
5. 固定高价值列
6. 求解最终 MILP 获取整数解
7. 迭代直到收敛或达到时间限制

### 产能排程

产能排程处理基于时间槽的产能分配。执行者被离散化为时间槽并附加产能约束，实现规划时域内的细粒度资源分配。

### 资源与产出

- **资源**：可消耗/有限产能（物料、能源、存储），含松弛变量（`overQuantity` / `lessQuantity`）用于软约束处理
- **产出/消耗**：跟踪物料产出与消耗，含需求满足约束和松弛变量

## 子模块

| 子模块 | 职责 |
|-------|------|
| `gantt-scheduling-infrastructure` | 时间原语：TimeRange、TimeWindow、TimeSlot、DurationRange、WorkingCalendar |
| `gantt-scheduling-domain-task-context` | 核心任务模型：Task、TaskBunch、TaskPlan、Executor、AssignmentPolicy |
| `gantt-scheduling-domain-task-compilation-context` | 任务级 MILP：Compilation、TaskTime、Switch、Makespan、约束与目标 |
| `gantt-scheduling-domain-task-generation-context` | 任务生成（占位，与束生成集成） |
| `gantt-scheduling-domain-bunch-compilation-context` | 列生成主问题：BunchCompilation、BunchAggregation、SlotBasedBunch |
| `gantt-scheduling-domain-bunch-generation-context` | 定价问题：Graph、Label、SlotBasedBunchGenerator |
| `gantt-scheduling-domain-capacity-scheduling-context` | 产能排程：Capacity、CapacityColumn、CapacityCompilation |
| `gantt-scheduling-domain-resource-context` | 资源约束：ExecutionResource、StorageResource、ConnectionResource |
| `gantt-scheduling-domain-produce-context` | 产出/消耗：Produce、Consumption、ProductionTask |
| `gantt-scheduling-application` | 求解编排：APS、MPS、LSP、BranchAndPriceAlgorithm |

## 公共 API

### 业务入口

- **`APS`** — 高级计划与排程：跨完整时域的战略级规划
- **`MPS`** — 主生产计划：生产数量与时间决策
- **`LSP`** — 批次排序计划：生产批次的详细排序与排程

### 算法服务

- **`BranchAndPriceAlgorithm`**（束 / 任务两种变体）：完整分支定价，含列生成、列固定与最终 MILP
- **`ColumnGenerationAlgorithm`**（束 / 任务两种变体）：仅列生成（LP 松弛 + 最终 MILP）

### 配置

`BranchAndPriceAlgorithm` 接受的配置包括：

- `badReducedAmount`：识别不利简约成本列的阈值
- `maximumColumnAmount`：模型中最大列数
- `minimumColumnAmountPerExecutor`：每个执行者的最小列数
- `timeLimit`：算法时间限制

### 迭代跟踪

`Iteration` 跟踪收敛状态：迭代次数、LP/IP 目标值、最优率、下界、慢改进检测用于提前终止。`IterationSnapshot` 提供迭代状态的类型擦除快照。

## 建模扩展

框架遵循 context / aggregation / pipeline 架构：

- **Context**：向应用层暴露的建模入口；初始化领域聚合并注册到模型
- **Aggregation**：组合领域内的多个模型组件并协调注册顺序
- **Pipeline / Limit**：处理单一约束族、目标族、惩罚项或影子价格逻辑

下游业务约束应以 extra context / pipeline 扩展注入，而非修改 framework 核心代码。

## 泛型数值边界

公共 API 使用 `V : RealNumber<V>` 作为泛型数值抽象。内部求解器操作使用 `Flt64`。数值类型转换集中在上下文注册和结果提取边界。

领域量（时间、持续时间、产能、资源量、产出量）在适用处使用 `Quantity<V>` 或显式物理量类型。`Flt64`、`Double` 和无单位裸值仅限求解器适配、注册和提取内部使用。

## 输出

### 束级解

`BunchSolution` / `BunchSchedulingSolution` 包含：

- 选中的束（路线）及任务分配
- 取消的任务
- 执行者分配与空闲时间
- 总成本

### 任务级解

`TaskSolution` 包含：

- 已分配任务及执行者和排程时间
- 取消的任务
- 任务延迟与提前

### 产能排程解

`CapacitySchedulingSolution` 包含：

- 产能列（时间槽分配）
- 每个时间槽的生产动作

## 本地验证

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-infrastructure,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-task-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-task-compilation-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-bunch-compilation-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-bunch-generation-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-capacity-scheduling-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-resource-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-produce-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-application "-Dsurefire.failIfNoSpecifiedTests=false" test
```
