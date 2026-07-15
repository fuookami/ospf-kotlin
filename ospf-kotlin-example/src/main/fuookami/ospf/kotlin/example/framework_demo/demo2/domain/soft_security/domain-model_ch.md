# 软安全 领域模型

[toc]

## 一、概述

管理软安全约束，包括空载分离、主甲板舱门空载偏好和压舱物重量建议——这些约束可提升安全性，但必要时可放松。

### 1. 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

---

## 二、概念/实体

### 1. 空载分离

确保空载位置分散而非集中，以保障结构安全。

**$positions$** ：位置列表。
**$load$** ：装载数据。

---

## 三、变量

本上下文复用装载分配上下文的决策变量，不定义独立决策变量。

---

## 四、约束

### 1. 空载厌恶限制

**[英]**：Empty Hated Limit
**描述**：空载位置的惩罚（软偏好填满位置）。

$$
s.t. \quad \sum_{j \in J} empty_j \leq maxEmpty \quad (\text{soft constraint})
$$

### 2. 主甲板舱门空载限制

**[英]**：Main Deck Door Empty Limit
**描述**：主甲板舱门位置应优先空载（B757/B767）。

$$
s.t. \quad empty_j = 1, \; \forall j \in MainDeckDoorPositions \quad (\text{soft constraint})
$$

### 3. 空载分离限制

**[英]**：Divide Empty Loading Limit
**描述**：空载位置应在飞机上分散分布。

$$
s.t. \quad \text{empty positions should be distributed} \quad (\text{soft constraint})
$$

### 4. 建议压舱物重量限制

**[英]**：Advice Ballast Weight Limit
**描述**：压舱物重量应满足建议最小值。

$$
s.t. \quad ballastWeight \geq adviceBallastWeight \quad (\text{soft constraint})
$$

---

## 五、目标函数

最小化软安全违规惩罚。

$$
\min \sum penalties
$$

---

## 六、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 空载分离 | DivideEmptyLoading | Divide Empty Loading | 空载位置的分散分布 |
| 空载厌恶 | EmptyHated | Empty Hated | 对空载位置的软惩罚 |
| 压舱物建议 | AdviceBallastWeight | Advice Ballast Weight | 压舱物重量的建议值 |

---

## 七、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 约束类型 | 硬约束 vs 软约束 | 软约束允许在必要时放松 | 2024 |
