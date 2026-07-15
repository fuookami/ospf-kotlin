# 冗余 领域模型

[toc]

## 一、概述

管理冗余和实验纵向平衡约束，用于重量分布分析和安全裕度。

### 1. 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

---

## 二、概念/实体

### 1. 冗余

重量分布安全裕度的冗余模型。

**$redundancy$** ：冗余值。

### 2. 实验纵向平衡

基于冗余计算的实验纵向平衡模型。

**$experimentalBalance$** ：实验纵向平衡值。
**$redundancy$** ：依赖的冗余值。

---

## 三、变量

本上下文复用装载分配上下文的决策变量，不定义独立决策变量。

---

## 四、约束

### 1. 冗余限制

**[英]**：Redundancy Limit
**描述**：冗余必须在可接受范围内。

$$
s.t. \quad minRedundancy \leq redundancy \leq maxRedundancy
$$

### 2. 实验纵向平衡限制

**[英]**：Experimental Longitudinal Balance Limit
**描述**：实验纵向平衡必须在范围内。

$$
s.t. \quad minBalance \leq experimentalBalance \leq maxBalance
$$

---

## 五、目标函数

本上下文不定义目标函数，仅提供约束。

---

## 六、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 冗余 | Redundancy | Redundancy | 重量分布的安全裕度 |
| 实验纵向平衡 | ExperimentalLongitudinalBalance | Experimental Longitudinal Balance | 基于冗余的纵向平衡 |

---

## 七、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 冗余建模 | 独立 vs 耦合 | 与纵向平衡耦合更符合实际 | 2024 |
