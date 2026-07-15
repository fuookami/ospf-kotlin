# 带宽分配上下文 (Bandwidth Context)

> [English](README.md) | 中文

## 概述

带宽分配上下文是 Demo1 框架演示中的核心限界上下文之一，负责在网络拓扑中为各服务分配带宽资源。该上下文基于路由上下文提供的网络图和服务定义，构建带宽优化模型，通过最小化带宽成本实现最优带宽分配。

## 职责

- 定义边带宽决策变量（每条边、每个服务的带宽分配量）
- 计算服务级和节点级的带宽中间指标（入度、出度、流出量）
- 施加带宽约束（边容量、需求满足、服务容量、传输节点容量）
- 最小化总带宽成本
- 从求解结果中提取服务路径

## 依赖上下文

- **Route Context** — 提供网络图结构（节点、边）、服务定义和路由分配变量

## 目录结构

```
bandwidth_context/
├── BandwidthContext.kt          # 上下文入口，编排初始化、注册、构建和分析流程
├── Aggregation.kt               # 聚合边/服务/节点带宽模型并统一注册
├── model/
│   ├── EdgeBandwidth.kt         # 边带宽决策变量 (y) 和中间符号 (bandwidth)
│   ├── ServiceBandwidth.kt      # 服务级入度/出度/流出中间符号
│   └── NodeBandwidth.kt         # 节点级聚合入度/出度/流出中间符号
└── service/
    ├── PipelineListGenerator.kt # 生成约束和目标管线列表
    ├── SolutionAnalyzer.kt      # 通过 DFS 从求解结果提取服务路径
    └── limits/
        ├── EdgeBandwidthConstraint.kt           # 边带宽约束
        ├── DemandConstraint.kt                  # 需求满足约束
        ├── ServiceCapacityConstraint.kt         # 服务容量约束
        ├── TransferNodeBandwidthConstraint.kt   # 传输节点带宽约束
        └── BandwidthCostObjective.kt            # 带宽成本目标函数
```

## 核心概念

### 决策变量

- **y[edge, service]** — 边 `edge` 上分配给服务 `service` 的带宽量，非负整数，上限为边的最大带宽

### 中间符号

- **bandwidth[edge]** — 边的总带宽（所有服务之和）
- **inDegree/service[service, node]** — 服务 `service` 在节点 `node` 的入度带宽
- **outDegree/service[service, node]** — 服务 `service` 在节点 `node` 的出度带宽
- **outFlow/service[service, node]** — 服务 `service` 在节点 `node` 的净流出（出度 - 入度）
- **inDegree/node[node]** — 节点 `node` 的聚合入度带宽（所有服务之和）
- **outDegree/node[node]** — 节点 `node` 的聚合出度带宽
- **outFlow/node[node]** — 节点 `node` 的聚合净流出

### 约束

| 约束 | 说明 |
|------|------|
| EdgeBandwidthConstraint | 当服务未分配到某边时，该边的带宽为零 |
| DemandConstraint | 每个客户端节点的入度带宽必须满足其需求 |
| ServiceCapacityConstraint | 每个普通节点的服务流出量不超过服务容量 |
| TransferNodeBandwidthConstraint | 每个普通节点的总流出量不超过其最大带宽容量 |

### 目标函数

最小化所有普通边的总带宽成本：$\min \sum_{e \in E} cost_e \cdot bandwidth_e$

## 工作流程

1. **初始化 (init)** — 从路由上下文获取图和服务，创建边带宽、服务带宽和节点带宽模型
2. **注册 (register)** — 将决策变量和中间符号注册到优化模型
3. **构建 (construct)** — 生成约束和目标管线，注入优化模型
4. **分析 (analyze)** — 从求解结果中提取各服务的路径

## 使用示例

```kotlin
val routeContext = RouteContext()
routeContext.init(input)
routeContext.register(model)
routeContext.construct(model)

val bandwidthContext = BandwidthContext(routeContext)
bandwidthContext.init(input)
bandwidthContext.register(model)
bandwidthContext.construct(model)

// 求解后分析结果
val paths = bandwidthContext.analyze(model, solverResult)
```
