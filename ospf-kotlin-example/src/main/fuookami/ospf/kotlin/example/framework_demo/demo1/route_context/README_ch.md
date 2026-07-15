# 路由上下文 (Route Context)

> [English](README.md) | 中文

## 概述

路由上下文是 Demo1 框架演示中的基础限界上下文，负责从输入数据构建网络拓扑图，并管理服务到节点的路由分配。该上下文为带宽分配上下文提供图结构、服务定义和分配决策变量，是整个优化流程的起点。

## 职责

- 从输入数据构建网络图（节点和边）
- 定义服务（具有容量和成本属性）
- 定义服务到节点的分配决策变量
- 施加路由约束（节点分配上限、服务分配上限）
- 最小化总服务分配成本

## 依赖上下文

无上游依赖。本上下文是 Demo1 优化流程的起点。

## 目录结构

```
route_context/
├── RouteContext.kt              # 上下文入口，构建图结构并管理模型构建
├── Aggregation.kt               # 聚合图、服务和分配变量并统一注册
├── model/
│   ├── Graph.kt                 # 网络图结构（Node、Edge、Graph）
│   ├── Service.kt               # 服务定义（容量、成本）
│   └── Assignment.kt            # 分配决策变量 (x) 和中间符号
└── service/
    ├── PipelineListGenerator.kt # 生成约束和目标管线列表
    └── limits/
        ├── NodeAssignmentConstraint.kt    # 节点分配约束
        ├── ServiceAssignmentConstraint.kt # 服务分配约束
        └── ServiceCostObjective.kt        # 服务成本目标函数
```

## 核心概念

### 实体

#### 节点 (Node)

网络中的节点，分为两种类型：

- **NormalNode** — 可承载服务流量的传输节点
- **ClientNode** — 消耗带宽的终端节点，具有特定需求量 (demand)

#### 边 (Edge)

两个节点之间的有向边，具有：
- **maxBandwidth** — 最大带宽容量
- **costPerBandwidth** — 单位带宽成本

#### 服务 (Service)

可通过网络路由的服务，具有：
- **capacity** — 服务容量上限
- **cost** — 每次使用成本

### 决策变量

- **x[node, service]** — 二元变量，表示服务 `service` 是否分配到节点 `node`

### 中间符号

- **nodeAssignment[node]** — 节点 `node` 上分配的服务数量（所有服务之和）
- **serviceAssignment[service]** — 服务 `service` 被分配到的节点数量（所有节点之和）

### 约束

| 约束 | 说明 |
|------|------|
| NodeAssignmentConstraint | 每个普通节点最多分配一个服务 |
| ServiceAssignmentConstraint | 每个服务最多分配到一个节点 |

### 目标函数

最小化总服务分配成本：$\min \sum_{s \in S} cost_s \cdot serviceAssignment_s$

## 工作流程

1. **初始化 (init)** — 从输入数据构建节点、边和服务，创建网络图和分配模型
2. **注册 (register)** — 将分配决策变量和中间符号注册到优化模型
3. **构建 (construct)** — 生成约束和目标管线，注入优化模型

## 图构建规则

- 输入的每条无向边会被转换为两条有向边（双向）
- 客户端节点通过零成本边连接到其关联的普通节点
- 客户端节点的连接边带宽等于该客户端的需求量

## 使用示例

```kotlin
val routeContext = RouteContext()
routeContext.init(input)
routeContext.register(model)
routeContext.construct(model)

// 路由上下文的聚合数据可供带宽上下文使用
val aggregation = routeContext.aggregation
```
