# 批次编译 领域模型

:us: [English](domain-model.md) | :cn: 简体中文

[toc]

## 一、概述

将航班任务束编译到列生成优化模型中，管理任务时间、流量、车队平衡、航班链接和航班容量约束的注册与增量列添加。

### 1. 依赖上下文

1. **task**（航班任务）
2. **rule**（规则）
3. **framework (gantt_scheduling)**

---

## 二、概念/实体

### 1. 编译（Compilation）

列生成中航班任务束的决策变量集合，特化为 `BunchCompilation<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>`。

**$x_{b}^{(k)}$** ：第 $k$ 次迭代中束 $b$ 的决策变量，取值为 0 或 1，表示束 $b$ 是否被选中。
**$y_{i}$** ：航班任务 $i$ 的辅助决策变量，用于链接和车队平衡约束。
**$z_{a}$** ：飞机 $a$ 的辅助决策变量，用于车队平衡约束。

### 2. 航班链接（FlightLink）

表示两个连续未恢复航段之间的连接关系，具有分割成本。

**$\text{prevTask}_{l}$** ：链接 $l$ 的前驱任务。
**$\text{succTask}_{l}$** ：链接 $l$ 的后继任务。
**$\text{splitCost}_{l}$** ：链接 $l$ 的分割成本。

### 3. 车队平衡检查点（FleetBalance.CheckPoint）

表示机场和飞机子机型的组合，用于跟踪飞机在各机场的分布。

**$\text{airport}_{c}$** ：检查点 $c$ 的机场。
**$\text{aircraftMinorType}_{c}$** ：检查点 $c$ 的飞机子机型。

### 4. 航班容量（FlightCapacity）

跟踪航班任务束的乘客和货物容量表达式。

**$\text{passenger}_{i,cls}$** ：航班任务 $i$ 在舱位 $cls$ 上的乘客容量表达式。
**$\text{cargo}_{i}$** ：航班任务 $i$ 的货物容量表达式。

---

## 三、变量

### 1. 决策变量

**$x_{b}^{(k)}$** ：第 $k$ 次迭代中束 $b$ 的选择变量，无量纲量，取值范围为 $\{0, 1\}$，表示是否选择束 $b$ 执行恢复计划，$\forall b \in B^{(k)}$ 。

**$y_{i}$** ：任务 $i$ 的链接辅助变量，无量纲量，取值范围为 $\{0, 1\}$，表示任务 $i$ 是否被包含在选中的束中，$\forall i \in I$ 。

**$z_{a}$** ：飞机 $a$ 的车队平衡辅助变量，无量纲量，取值范围为 $\{0, 1\}$，表示飞机 $a$ 是否被使用，$\forall a \in A$ 。

### 2. 辅助变量

**$\text{link\_slack}_{l}$** ：链接 $l$ 的松弛变量，取值范围为 $[0, +\infty)$ ，用于惩罚不被任何选中束覆盖的链接，$\forall l \in L$ 。

**$\text{fleet\_slack}_{c}$** ：检查点 $c$ 的车队平衡松弛变量，取值范围为 $[0, +\infty)$ ，用于惩罚飞机分布偏差，$\forall c \in C$ 。

---

## 四、谓词

### 1. 任务类型

**isFlight** ：任务 $i$ 是航班类型（Flight 或 VirtualFlight）。
**isRecoveryNeeded** ：任务 $i$ 在恢复时间窗口内需要恢复。

### 2. 容量类型

**hasPassenger** ：航班任务 $i$ 的飞机具有乘客容量。
**hasCargo** ：航班任务 $i$ 的飞机具有货物容量。

---

## 五、集合

### 1. 束

**$B$** ：所有已生成的航班任务束全集。

**$B^{(k)}$** ：第 $k$ 次迭代生成的束子集。
**$B_{a}$** ：分配给飞机 $a$ 的束子集，$\forall a \in A$ 。
**$B_{i}$** ：包含任务 $i$ 的束子集，$\forall i \in I$ 。

### 2. 任务

**$I$** ：所有航班任务全集。

**$I^{R}$** ：需要恢复的任务子集。
**$I^{F}$** ：航班类型的任务子集。

### 3. 链接

**$L$** ：所有航班链接全集。

**$L^{C}$** ：连接链接子集。
**$L^{S}$** ：经停链接子集。
**$L^{I}$** ：忽略连接时间的链接子集。

### 4. 检查点

**$C$** ：所有车队平衡检查点全集（机场 × 子机型组合）。

---

## 六、中间值

### 1. 链接表达式

**描述**：链接 $l$ 被选中束覆盖的数量。

$$
\text{link}_{l} = \sum_{b \in B : b \supset l} x_{b}^{(k)}, \; \forall l \in L
$$

### 2. 车队平衡表达式

**描述**：到达检查点 $c$ 的飞机数量。

$$
\text{fleet}_{c} = \sum_{a \in A_{c}} z_{a}, \; \forall c \in C
$$

### 3. 乘客容量表达式

**描述**：航班任务 $i$ 在舱位 $cls$ 上的总乘客容量。

$$
\text{passenger\_capacity}_{i,cls} = \sum_{b \in B_{i}} \text{cap}(b, i, cls) \cdot x_{b}^{(k)}, \; \forall i \in I^{F}, \forall cls \in CLS
$$

### 4. 货物容量表达式

**描述**：航班任务 $i$ 的总货物容量。

$$
\text{cargo\_capacity}_{i} = \sum_{b \in B_{i}} \text{cap}(b, i) \cdot x_{b}^{(k)}, \; \forall i \in I^{F}
$$

---

## 七、断言

### 1. 链接覆盖一致性

**描述**：每个链接的覆盖数量应与包含该链接的任务决策变量一致。

$$
\forall l \in L \; (\text{link}_{l} = \sum_{b \in B : b \supset l} x_{b})
$$

### 2. 车队平衡一致性

**描述**：每个检查点的飞机数量应与原始计划一致。

$$
\forall c \in C \; (\text{fleet}_{c} = \text{expected\_amount}_{c})
$$

---

## 八、约束

### 1. 任务覆盖约束

**[英]**：Task Coverage Constraint
**描述**：每个需要恢复的航班任务必须被恰好一个选中的束覆盖。

$$
s.t. \quad \sum_{b \in B_{i}} x_{b} = 1, \; \forall i \in I^{R}
$$

### 2. 链接松弛约束

**[英]**：Link Slack Constraint
**描述**：链接的覆盖数量加上松弛变量应大于等于阈值。

$$
s.t. \quad \text{link}_{l} + \text{link\_slack}_{l} \geq 1, \; \forall l \in L
$$

### 3. 车队平衡约束

**[英]**：Fleet Balance Constraint
**描述**：到达每个检查点的飞机数量加上松弛变量应等于预期数量。

$$
s.t. \quad \text{fleet}_{c} + \text{fleet\_slack}_{c} = \text{expected\_amount}_{c}, \; \forall c \in C
$$

---

## 九、目标函数

**描述**：最小化恢复总成本，包括束成本和松弛惩罚。

$$
\min \sum_{b \in B} \text{cost}(b) \cdot x_{b} + \sum_{l \in L} \lambda_{l} \cdot \text{link\_slack}_{l} + \sum_{c \in C} \mu_{c} \cdot \text{fleet\_slack}_{c}
$$

---

## 十、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 阈值松弛 | `exampleThresholdSlack` | 第三章辅助变量 | 链接和车队平衡的阈值松弛函数 |

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 束 | $B$ | Bunch | 分配给单架飞机的航班任务有序序列 |
| 编译 | Compilation | Compilation | 列生成决策变量的集合 |
| 检查点 | $C$ | CheckPoint | 机场和飞机子机型的组合 |
| 链接 | $L$ | Link | 两个连续航段间的连接关系 |
| 分割成本 | splitCost | Split Cost | 链接的成本分摊 |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 使用阈值松弛而非硬约束 | 硬约束、线性松弛 | 允许不可行解并给予惩罚，提高求解灵活性 | - |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1 | 初始实现 | 基础列生成编译 |
