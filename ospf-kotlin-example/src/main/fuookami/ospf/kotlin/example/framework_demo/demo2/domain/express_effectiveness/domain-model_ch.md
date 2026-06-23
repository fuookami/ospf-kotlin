# 快递效能 领域模型

[toc]

## 一、概述

管理快递效能约束，优化货物项优先级排序——确保高优先级货物优先装载。

### 1. 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

---

## 二、概念/实体

### 1. 绝对排序

定义预分配模式下的绝对优先级排序。

**$order_{i}$** ：货物 $i$ 的绝对优先级顺序。

### 2. 相对排序

定义满载模式下的相对优先级排序。

**$relativeOrder_{ij}$** ：货物 $i$ 相对于货物 $j$ 的优先级顺序。

### 3. 必须发运项

无论优先级如何都必须发运的货物项。

**$mustShipIndices$** ：必须发运项的索引列表。

---

## 三、变量

本上下文复用装载分配上下文的决策变量，不定义独立决策变量。

---

## 四、约束

### 1. 必须发运限制

**[英]**：Must-Ship Limit
**描述**：必须发运项必须被装载。

$$
s.t. \quad \sum_{j \in J} x_{ij} = 1, \; \forall i \in MustShip
$$

### 2. 货物优先级限制

**[英]**：Item Priority Limit
**描述**：高优先级货物应在低优先级货物之前装载。

$$
s.t. \quad priority_i < priority_j \rightarrow loaded_i \geq loaded_j, \; \forall i, j \in Items
$$

---

## 五、目标函数

最小化优先级违规成本。

$$
\min \sum_{i \in I} priorityViolationCost_i
$$

---

## 六、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 绝对排序 | AbsoluteOrder | Absolute Order | 预分配模式的优先级排序 |
| 相对排序 | RelativeOrder | Relative Order | 满载模式的优先级排序 |
| 必须发运项 | MustShip | Must-Ship Items | 必须发运的货物 |

---

## 七、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 排序模式 | 绝对 vs 相对 | 不同装载模式使用不同排序策略 | 2024 |
