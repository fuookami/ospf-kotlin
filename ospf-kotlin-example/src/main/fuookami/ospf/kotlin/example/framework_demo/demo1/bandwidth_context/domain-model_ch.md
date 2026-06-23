# 带宽分配上下文 领域模型

> [English](domain-model.md) | 中文

[toc]

## 一、概述

带宽分配上下文负责在网络拓扑中为各服务分配带宽资源，通过最小化带宽成本实现最优带宽分配。

### 1. 依赖上下文

1. 路由上下文 (Route Context) — 提供网络图结构、服务定义和路由分配变量

---

## 二、概念/实体

### 1. 边 (Edge)

网络中两个节点之间的有向连接，具有带宽容量和单位成本。

**$from_{e}$** ：边 $e$ 的起始节点。
**$to_{e}$** ：边 $e$ 的终止节点。
**$maxBandwidth_{e}$** ：边 $e$ 的最大带宽容量，物理量为无量纲整数。
**$costPerBandwidth_{e}$** ：边 $e$ 的单位带宽成本，物理量为无量纲整数。

### 2. 服务 (Service)

可通过网络路由的逻辑通道，具有容量上限和使用成本。

**$capacity_{s}$** ：服务 $s$ 的容量上限。
**$cost_{s}$** ：服务 $s$ 的每次使用成本。

### 3. 节点 (Node)

网络中的顶点，分为传输节点 (NormalNode) 和终端节点 (ClientNode)。

**$id_{n}$** ：节点 $n$ 的唯一标识符。
**$demand_{n}$** ：节点 $n$ 的带宽需求量（仅对终端节点有效）。
**$edges_{n}$** ：与节点 $n$ 相连的边集合。

---

## 三、变量

### 1. 决策变量

**$y_{e,s}$** ：边 $e$ 上分配给服务 $s$ 的带宽量，无量纲整数，取值范围为 $[0, maxBandwidth_{e}]$，表示服务 $s$ 在边 $e$ 上占用的带宽，$\forall e \in E$，$\forall s \in S$。

### 2. 辅助变量

无。

---

## 四、谓词

### 1. 节点类型

**normal** ：节点为传输节点 (NormalNode)。
**client** ：节点为终端节点 (ClientNode)。

### 2. 边类型

**from_normal** ：边的起始节点为传输节点。

---

## 五、集合

### 1. 节点

**$N$** ：网络中所有节点的集合。

**$N^{normal}$** ：满足谓词 normal 的子集，即所有传输节点。
**$N^{client}$** ：满足谓词 client 的子集，即所有终端节点。

### 2. 边

**$E$** ：网络中所有有向边的集合。

**$E^{normal}$** ：满足谓词 from_normal 的子集，即起始节点为传输节点的边。

### 3. 服务

**$S$** ：所有可用服务的集合。

---

## 六、中间值

### 1. 边总带宽 (bandwidth)

**描述**：每条普通边上所有服务分配带宽之和。

$$
bandwidth_{e} = \sum_{s \in S} y_{e,s}, \; \forall e \in E^{normal}
$$

### 2. 服务入度带宽 (inDegree/service)

**描述**：每个服务在每个节点上的入度带宽，即指向该节点的所有边上该服务的带宽之和。

$$
inDegree_{s,n} = \sum_{e \in E: to_{e}=n} y_{e,s}, \; \forall s \in S, \; \forall n \in N
$$

### 3. 服务出度带宽 (outDegree/service)

**描述**：每个服务在每个传输节点上的出度带宽，即从该节点出发的所有边上该服务的带宽之和。

$$
outDegree_{s,n} = \sum_{e \in E: from_{e}=n} y_{e,s}, \; \forall s \in S, \; \forall n \in N^{normal}
$$

### 4. 服务流出量 (outFlow/service)

**描述**：每个服务在每个传输节点上的净流出量，为出度减入度。

$$
outFlow_{s,n} = outDegree_{s,n} - inDegree_{s,n}, \; \forall s \in S, \; \forall n \in N^{normal}
$$

### 5. 节点聚合入度带宽 (inDegree/node)

**描述**：每个节点上所有服务入度带宽之和。

$$
inDegree_{n} = \sum_{s \in S} inDegree_{s,n}, \; \forall n \in N
$$

### 6. 节点聚合出度带宽 (outDegree/node)

**描述**：每个传输节点上所有服务出度带宽之和。

$$
outDegree_{n} = \sum_{s \in S} outDegree_{s,n}, \; \forall n \in N^{normal}
$$

### 7. 节点聚合流出量 (outFlow/node)

**描述**：每个传输节点上所有服务净流出量之和。

$$
outFlow_{n} = \sum_{s \in S} outFlow_{s,n}, \; \forall n \in N^{normal}
$$

### 8. 节点最大出度容量 (maxOutDegree)

**描述**：节点的最大出带宽容量，为其所有出边最大带宽之和。

$$
maxOutDegree_{n} = \sum_{e \in E: from_{e}=n} maxBandwidth_{e}, \; \forall n \in N^{normal}
$$

---

## 七、断言

### 1. 终端节点不分配带宽

**描述**：终端节点 (ClientNode) 上所有服务的带宽分配量为零。

$$
\forall n \in N^{client}, \; \forall s \in S, \; \forall e \in E: from_{e}=n \; (y_{e,s} = 0)
$$

### 2. 非普通边带宽为零

**描述**：起始节点非传输节点的边上，所有服务的带宽分配量为零。

$$
\forall e \in E \setminus E^{normal}, \; \forall s \in S \; (y_{e,s} = 0)
$$

---

## 八、约束

### 1. 边带宽约束

**[英]**：Edge Bandwidth Constraint
**描述**：当服务未分配到某边时，该边的带宽必须为零；当服务已分配时，带宽不超过边的最大容量。

$$
s.t. \quad (1 - x_{n,s}) \cdot maxBandwidth_{e} + y_{e,s} \leq maxBandwidth_{e}, \; \forall e \in E^{normal}, \; \forall s \in S
$$

其中 $x_{n,s}$ 为路由上下文中的分配变量，$n = from_{e}$。

### 2. 需求满足约束

**[英]**：Demand Constraint
**描述**：每个终端节点的入度带宽必须满足其带宽需求。

$$
s.t. \quad inDegree_{n} \geq demand_{n}, \; \forall n \in N^{client}
$$

### 3. 服务容量约束

**[英]**：Service Capacity Constraint
**描述**：当服务被分配到某节点时，该节点的服务流出量不超过服务容量。

$$
s.t. \quad capacity_{s} \cdot (1 - x_{n,s}) + outFlow_{s,n} \leq capacity_{s}, \; \forall n \in N^{normal}, \; \forall s \in S
$$

### 4. 传输节点带宽约束

**[英]**：Transfer Node Bandwidth Constraint
**描述**：当传输节点被分配时，其总流出量不超过节点的最大出度容量。

$$
s.t. \quad maxOutDegree_{n} \cdot (1 - x_{n}) + outFlow_{n} \leq maxOutDegree_{n}, \; \forall n \in N^{normal}
$$

其中 $x_{n}$ 为路由上下文中的节点分配变量。

---

## 九、目标函数

**描述**：最小化所有普通边的总带宽成本。

$$
\min \sum_{e \in E^{normal}} costPerBandwidth_{e} \cdot bandwidth_{e}
$$

---

## 十、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| DFS 路径提取 | `service/SolutionAnalyzer.kt` | 第六章 | 通过深度优先搜索从求解结果中提取服务路径 |

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 边 | $e$ | Edge | 两个节点之间的有向连接 |
| 服务 | $s$ | Service | 可通过网络路由的逻辑通道 |
| 传输节点 | $n$ | NormalNode | 可承载服务流量的中间节点 |
| 终端节点 | $n$ | ClientNode | 消耗带宽的末端节点 |
| 带宽 | $y_{e,s}$ | Bandwidth | 分配给某服务在某边上的带宽量 |
| 入度 | $inDegree$ | In-degree | 流入节点的带宽总量 |
| 出度 | $outDegree$ | Out-degree | 流出节点的带宽总量 |
| 流出量 | $outFlow$ | Out-flow | 节点的净流出带宽（出度 - 入度） |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 边带宽使用整数变量 | 连续变量 | 简化求解，符合实际带宽分配的离散特性 | 2024 |
| 中间符号使用 flatMap | 直接构建多项式 | 框架提供的声明式 API，提高代码可读性 | 2024 |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始实现 | Demo1 框架演示 |
