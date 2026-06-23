# 推荐重量均衡 领域模型

[toc]

## 一、概述

管理推荐重量均衡——确保货物重量按优先级预约在各位置间均匀分布。

### 1. 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

---

## 二、概念/实体

### 1. 优先级预约

基于优先级的货物-位置预约及重量均衡。

**$appointment$** ：货物到位置的预约映射。
**$priority$** ：货物优先级。

---

## 三、变量

本上下文复用装载分配上下文的决策变量，不定义独立决策变量。

---

## 四、约束

### 1. 货物顺序限制

**[英]**：Item Order Limit
**描述**：货物必须按优先级顺序装载。

$$
s.t. \quad priority_i < priority_j \rightarrow order_i \leq order_j, \; \forall i, j \in Items
$$

### 2. 优先级预约限制

**[英]**：Priority Appointment Limit
**描述**：必须遵守优先级预约。

$$
s.t. \quad x_{ij} = 1, \; \forall (i, j) \in PriorityAppointment
$$

### 3. 推荐重量均衡限制

**[英]**：Recommended Weight Equalization Limit
**描述**：装载重量应在各位置间均衡分布。

$$
s.t. \quad |loadWeight_j - avgWeight| \leq tolerance, \; \forall j \in J
$$

---

## 五、目标函数

最小化重量偏离推荐值的程度。

$$
\min \sum_{j \in J} |loadWeight_j - recommendedWeight_j|
$$

---

## 六、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 优先级预约 | PriorityAppointment | Priority Appointment | 基于优先级的货物-位置预约 |
| 重量均衡 | WeightEqualization | Weight Equalization | 货物重量的均匀分布 |

---

## 七、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 均衡策略 | 绝对均衡 vs 相对均衡 | 相对均衡更灵活 | 2024 |
