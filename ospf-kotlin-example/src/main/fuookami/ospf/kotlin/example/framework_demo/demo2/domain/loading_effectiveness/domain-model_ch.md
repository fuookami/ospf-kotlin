# 装载效能 领域模型

[toc]

## 一、概述

管理装载效能约束以提升操作效率——包括拖车装载、顺序装载、转运邻接和始发/目的地分组。

### 1. 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

---

## 二、概念/实体

### 1. 建议装载

预分配模式下各位置的建议装载数量和重量。

**$adviceAmount_{j}$** ：位置 $j$ 的建议装载数量。
**$adviceWeight_{j}$** ：位置 $j$ 的建议装载重量。

### 2. 转运邻接装载

同源/同目的地邻接约束以提升转运效率。

**$adjacentPositions$** ：邻接位置对列表。
**$sources$** ：始发站列表。
**$destinations$** ：目的站列表。

### 3. 顺序装载

基于位置排序的顺序装载约束。

**$orderedPositions$** ：排序后的位置对列表。

### 4. 拖车装载

满载模式下的拖车变更和绕行约束。

**$trailers$** ：拖车列表。

---

## 三、变量

本上下文复用装载分配上下文的决策变量，不定义独立决策变量。

---

## 四、约束

### 1. 同源邻接限制

**[英]**：Same Source Adjacent Limit
**描述**：同源货物应在相邻位置装载。

$$
s.t. \quad source_i = source_k \rightarrow adjacent(x_{ij}, x_{kl}), \; \forall i, k \in Items_{sameSource}
$$

### 2. 同目的地邻接限制

**[英]**：Same Destination Adjacent Limit
**描述**：同目的地货物应在相邻位置装载。

$$
s.t. \quad dest_i = dest_k \rightarrow adjacent(x_{ij}, x_{kl}), \; \forall i, k \in Items_{sameDest}
$$

### 3. 拖车变更限制

**[英]**：Trailer Change Limit
**描述**：最小化装载过程中的拖车变更。

$$
\min \sum trailerChanges
$$

---

## 五、目标函数

最小化装载操作成本。

$$
\min loadingOperationalCost
$$

---

## 六、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 建议装载 | AdviceLoading | Advice Loading | 各位置的建议装载量 |
| 转运邻接 | TransferAdjacent | Transfer Adjacent | 转运货物的邻接要求 |
| 顺序装载 | SequentialLoading | Sequential Loading | 基于顺序的装载约束 |
| 拖车装载 | TrailerLoading | Trailer Loading | 拖车相关的装载约束 |

---

## 七、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 邻接定义 | 装载顺序 vs 物理位置 | 装载顺序更符合操作实际 | 2024 |
