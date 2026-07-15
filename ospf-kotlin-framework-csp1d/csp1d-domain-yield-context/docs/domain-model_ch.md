# 产出偏差上下文 领域模型

[toc]

## 一、概述

产出偏差上下文负责管理需求的欠产和超产松弛变量，量化产出与需求之间的偏差，并通过惩罚项引导求解器最小化偏差。

---

## 二、基础实体

### 1. 产品（Product）

描述物料的规格属性，是切割生产的目标产出物。

**$\text{id}_{i}$** ：产品 $i$ 的唯一标识符。
**$\text{name}_{i}$** ：产品 $i$ 的名称。
**$\text{width}_{i}$** ：产品 $i$ 的宽度列表，支持多规格。
**$\text{length}_{i}$** ：产品 $i$ 的长度，动态长度产品为 null。
**$\text{unitWeight}_{i}$** ：产品 $i$ 的单位重量。
**$\text{maxOverProduceLength}_{i}$** ：产品 $i$ 的最大超产长度，用于长度分配约束。
**$\text{dynamicLength}_{i}$** ：产品 $i$ 是否为动态长度标识，若为 true 则 length 和 weight 必须为 null。

### 2. 产品需求（ProductDemand）

描述对特定产品的生产需求量。

**$\text{product}_{d}$** ：需求 $d$ 对应的产品。
**$\text{quantity}_{d}$** ：需求 $d$ 的需求值（带物理单位）。
**$\text{mode}_{d}$** ：需求 $d$ 的口径标签（Roll/Weight/Sheet）。

### 3. 切割方案（CuttingPlan）

描述一次完整切割操作，包含物料、切片和需求贡献。

**$\text{id}_{p}$** ：方案 $p$ 的唯一标识符。
**$\text{material}_{p}$** ：方案 $p$ 使用的物料。
**$\text{machineId}_{p}$** ：方案 $p$ 使用的设备标识。
**$\text{slices}_{p}$** ：方案 $p$ 的切片列表。
**$\text{demandContributions}_{p}$** ：方案 $p$ 对需求的贡献列表。
**$\text{capacityConsumption}_{p}$** ：方案 $p$ 单次使用的设备产能消耗。

### 4. 需求贡献（CuttingPlanDemandContribution）

描述切割方案对特定产品需求的贡献值。

**$\text{product}_{a}$** ：贡献 $a$ 对应的产品。
**$\text{quantity}_{a}$** ：贡献 $a$ 的贡献值（带物理单位）。

---

## 三、本上下文实体

### 1. 欠产（UnderProduction）

描述产品产出低于需求的情况。

**$\text{demand}_{u}$** ：欠产 $u$ 对应的需求。
**$\text{shortfall}_{u}$** ：欠产 $u$ 的短缺量（带物理单位）。

### 2. 超产（OverProduction）

描述产品产出超出需求的情况。

**$\text{demand}_{o}$** ：超产 $o$ 对应的需求。
**$\text{surplus}_{o}$** ：超产 $o$ 的盈余量（带物理单位）。

### 3. 产品产出汇总（ProductOutput）

描述产品的总产出量。

**$\text{product}_{p}$** ：汇总 $p$ 对应的产品。
**$\text{totalQuantity}_{p}$** ：汇总 $p$ 的总产出量（带物理单位）。
**$\text{mode}_{p}$** ：汇总 $p$ 的需求口径标签。

### 4. 产出偏差分析结果（YieldAnalysis）

产出偏差分析的完整结果。

**$\text{underProductions}_{a}$** ：分析 $a$ 的欠产列表。
**$\text{overProductions}_{a}$** ：分析 $a$ 的超产列表。
**$\text{outputs}_{a}$** ：分析 $a$ 的产品产出汇总列表。

### 5. 需求聚合键（DemandAggregationKey）

确保相同产品不同单位的产出不混算的聚合键。

**$\text{productId}_{k}$** ：聚合键 $k$ 的产品 ID。
**$\text{unit}_{k}$** ：聚合键 $k$ 的物理单位。

### 6. 产出建模配置（YieldModelingConfig）

供应用层消费的产出建模配置。

**$\text{underProductionPenalty}_{c}$** ：配置 $c$ 的欠产惩罚系数映射（按产品+单位）。
**$\text{overProductionPenalty}_{c}$** ：配置 $c$ 的超产惩罚系数映射（按产品+单位）。
**$\text{overProductionUpperBound}_{c}$** ：配置 $c$ 的超产上限映射（按产品+单位）。

### 7. 产出建模结果（YieldModelingResult）

从 solver solution 回填的建模结果。

**$\text{underProductions}_{r}$** ：结果 $r$ 的欠产变量值列表。
**$\text{overProductions}_{r}$** ：结果 $r$ 的超产变量值列表。

### 8. 模式化欠产（ModeledUnderProduction）

建模层扁平类型，记录 solver 变量值。

**$\text{productId}_{u}$** ：记录 $u$ 的产品 ID。
**$\text{unitSymbol}_{u}$** ：记录 $u$ 的需求单位符号。
**$\text{amount}_{u}$** ：记录 $u$ 的欠产量。

### 9. 模式化超产（ModeledOverProduction）

建模层扁平类型，记录 solver 变量值。

**$\text{productId}_{o}$** ：记录 $o$ 的产品 ID。
**$\text{unitSymbol}_{o}$** ：记录 $o$ 的需求单位符号。
**$\text{amount}_{o}$** ：记录 $o$ 的超产量。

---

## 四、变量

### 1. 决策变量

本上下文不直接定义决策变量，决策变量由产出上下文管理。

### 2. 辅助变量

**$\text{under\_production}_{i}$** ：需求 $i$ 的欠产松弛变量，无量纲归一化系数，取值范围为 $[0, +\infty)$ ，记录产出低于需求的部分，$\forall i \in D$ 。

**$\text{over\_production}_{i}$** ：需求 $i$ 的超产松弛变量，无量纲归一化系数，取值范围为 $[0, +\infty)$ ，记录产出超出需求的部分，$\forall i \in D$ 。

---

## 五、谓词

### 1. 需求谓词

**hasUnderPenalty** ：需求是否定义了欠产惩罚。
**hasOverPenalty** ：需求是否定义了超产惩罚。
**hasOverBound** ：需求是否定义了超产上限。
**needsOverSlackForOverArea** ：是否因超产面积惩罚需要超产 slack。

---

## 六、集合

### 1. 有欠产惩罚需求集合

**$D^{under}$** ：定义了欠产惩罚的需求子集。

### 2. 有超产惩罚需求集合

**$D^{over}$** ：定义了超产惩罚的需求子集。

### 3. 有超产上限需求集合

**$D^{bound}$** ：定义了超产上限的需求子集。

---

## 七、中间值

### 1. 总产出量

**描述**：按产品+单位聚合的总贡献量。

$$
\text{totalOutput}_{i} = \sum_{j \in S} \text{contribution}_{ij} \times x_{j}
$$

### 2. 欠产量

**描述**：需求量减去总产出量，当产出不足时。

$$
\text{shortfall}_{i} = \text{demand}_{i} - \text{totalOutput}_{i}, \; \text{if totalOutput}_{i} < \text{demand}_{i}
$$

### 3. 超产量

**描述**：总产出量减去需求量，当产出超出时。

$$
\text{surplus}_{i} = \text{totalOutput}_{i} - \text{demand}_{i}, \; \text{if totalOutput}_{i} > \text{demand}_{i}
$$

---

## 八、断言

### 1. 松弛变量非负

**描述**：欠产和超产松弛变量必须非负。

$$
\forall i \in D \; (\text{under\_production}_{i} \geq 0 \wedge \text{over\_production}_{i} \geq 0)
$$

### 2. 需求聚合键唯一性

**描述**：相同产品不同单位的需求通过聚合键区分。

$$
\forall i_1, i_2 \in D \; (i_1 \neq i_2 \rightarrow \text{key}_{i_1} \neq \text{key}_{i_2} \vee \text{unit}_{i_1} \neq \text{unit}_{i_2})
$$

---

## 九、约束

### 1. 需求平衡约束

**[英]**：Demand Balance Constraint
**描述**：每个需求的总贡献量加上欠产减去超产等于需求量。

$$
s.t. \quad \text{demandQuantity}_{i} + \text{under\_production}_{i} - \text{over\_production}_{i} = \text{demand}_{i}, \; \forall i \in D
$$

### 2. 超产上限约束

**[英]**：Over-Production Upper Bound Constraint
**描述**：超产松弛变量不得超过配置的超产上限。

$$
s.t. \quad \text{over\_production}_{i} \leq \text{overProductionUpperBound}_{i}, \; \forall i \in D^{bound}
$$

---

## 十、目标函数

**描述**：最小化欠产和超产的加权惩罚。

$$
\min \sum_{i \in D} (\text{underPenalty}_{i} \times \text{under\_production}_{i} + \text{overPenalty}_{i} \times \text{over\_production}_{i})
$$

---

## 十一、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 产出偏差分析 | - | 第六章 | 按产品+单位聚合贡献并与需求对比 |

---

## 十二、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 欠产 | $\text{under\_production}$ | Under Production | 产出低于需求的部分 |
| 超产 | $\text{over\_production}$ | Over Production | 产出超出需求的部分 |
| 欠产惩罚 | $\text{underPenalty}$ | Under-Production Penalty | 欠产的惩罚系数 |
| 超产惩罚 | $\text{overPenalty}$ | Over-Production Penalty | 超产的惩罚系数 |
| 超产上限 | $\text{overProductionUpperBound}$ | Over-Production Upper Bound | 超产的上限值 |
| 产出偏差 | - | Yield Deviation | 产出与需求之间的差异 |
| 需求聚合键 | - | Demand Aggregation Key | 确保同产品不同单位不混算的键 |

---

## 十三、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 按产品+单位聚合产出 | 仅按产品聚合 | 同产品不同单位的产出不应混算 | - |
| 分离欠产和超产松弛变量 | 单一偏差变量 | 可独立惩罚和约束 | - |
| 使用无量纲归一化系数 | 带单位物理量 | 权重由调用方负责单位换算 | - |

---

## 十四、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始版本 | 基础产出偏差管理 |
| v1.1 | 支持按产品+单位聚合 | 同产品不同单位需求的正确处理 |
| v1.2 | 增加超产上限约束 | 超产控制需求 |
