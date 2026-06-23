# MAC优化 领域模型

[toc]

## 一、概述

管理 MAC 优化，包括纵向平衡（MAC 范围约束）和横向平衡约束，用于飞机重量分布。

### 1. 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）
3. 平均气动弦（mac）

---

## 二、概念/实体

### 1. MAC 范围

基于总重量定义允许的 MAC 百分比范围。

**$minMAC_{weight}$** ：给定总重量下的最小 MAC 百分比。
**$maxMAC_{weight}$** ：给定总重量下的最大 MAC 百分比。

### 2. 纵向平衡

纵向平衡约束，确保各飞行阶段 MAC 在允许范围内。

**$macRange$** ：MAC 范围。
**$torque$** ：扭矩数据。

### 3. 横向平衡

宽体飞机的横向平衡约束，确保对称装载。

**$torque$** ：横向扭矩数据。

---

## 三、变量

本上下文复用装载分配上下文的决策变量，不定义独立决策变量。

---

## 四、约束

### 1. 纵向平衡限制

**[英]**：Longitudinal Balance Limit
**描述**：MAC 百分比必须在各飞行阶段的允许范围内。

$$
s.t. \quad minMAC_{weight} \leq mac \leq maxMAC_{weight}, \; \forall phase \in FlightPhases
$$

### 2. 横向平衡限制

**[英]**：Lateral Balance Limit
**描述**：横向扭矩必须在允许范围内（仅宽体飞机）。

$$
s.t. \quad |lateralTorque| \leq maxLateralTorque
$$

### 3. 水平安定面限制

**[英]**：Horizontal Stabilizer Limit
**描述**：水平安定面位置必须与 MAC 匹配。

$$
s.t. \quad stabilizerPosition = f(mac), \; \forall phase \in FlightPhases
$$

---

## 五、目标函数

最小化 MAC 偏离目标范围的程度。

$$
\min |mac - macTarget|
$$

---

## 六、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| MAC 范围 | MACRange | MAC Range | 允许的 MAC 百分比范围 |
| 纵向平衡 | LongitudinalBalance | Longitudinal Balance | 前后方向的重量平衡 |
| 横向平衡 | LateralBalance | Lateral Balance | 左右方向的重量平衡 |
| 水平安定面 | HorizontalStabilizer | Horizontal Stabilizer | 尾翼水平安定面 |

---

## 七、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| MAC 范围建模 | 线性 vs 分段线性 | 分段线性更精确 | 2024 |
