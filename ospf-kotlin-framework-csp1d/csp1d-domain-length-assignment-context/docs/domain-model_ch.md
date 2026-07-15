# 长度分配上下文 领域模型

[toc]

## 一、概述

长度分配上下文负责动态卷长的分配与超长检测，为动态长度产品推导最优卷长并检测超产约束 violations。

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

---

## 三、本上下文实体

### 1. 长度分配（LengthAssignment）

描述单个产品被分配的卷长结果。

**$\text{product}_{a}$** ：分配 $a$ 对应的产品。
**$\text{assignedLength}_{a}$** ：分配 $a$ 的卷长值（带物理单位）。
**$\text{batchCount}_{a}$** ：分配 $a$ 的批次数。

### 2. 超长记录（OverLengthRecord）

记录产品实际卷长超过最大超产长度的部分。

**$\text{product}_{r}$** ：记录 $r$ 对应的产品。
**$\text{overLength}_{r}$** ：记录 $r$ 的超长量（带物理单位）。

### 3. 长度分配建模配置（LengthAssignmentModelingConfig）

供应用层消费的长度分配配置。

**$\text{dynamicProductIds}_{c}$** ：配置 $c$ 中需要动态分配卷长的产品 ID 集合。
**$\text{assignedLengthLowerBound}_{c}$** ：配置 $c$ 的已分配卷长下界映射（按产品 ID）。
**$\text{assignedLengthUpperBound}_{c}$** ：配置 $c$ 的已分配卷长上界映射（按产品 ID）。
**$\text{overLengthPenalty}_{c}$** ：配置 $c$ 的超长惩罚权重映射（按产品 ID）。
**$\text{overLengthUpperBound}_{c}$** ：配置 $c$ 的超长上限映射（按产品 ID）。
**$\text{totalLengthPenalty}_{c}$** ：配置 $c$ 的总卷长惩罚权重。
**$\text{batchMinPenalty}_{c}$** ：配置 $c$ 的批次最小化惩罚权重。

### 4. 长度分配建模结果（LengthAssignmentModelingResult）

从 solver solution 回填的建模结果。

**$\text{assignedLengths}_{r}$** ：结果 $r$ 的动态长度产品卷长分配值列表。
**$\text{overLengths}_{r}$** ：结果 $r$ 的超长产品值列表。

### 5. 模式化已分配长度（ModeledAssignedLength）

建模层扁平类型，记录 solver 变量值。

**$\text{productId}_{l}$** ：记录 $l$ 的产品 ID。
**$\text{assignedLength}_{l}$** ：记录 $l$ 的分配卷长值。

### 6. 模式化超长（ModeledOverLength）

建模层扁平类型，记录 solver 变量值。

**$\text{productId}_{o}$** ：记录 $o$ 的产品 ID。
**$\text{overLength}_{o}$** ：记录 $o$ 的超长值。

---

## 四、变量

### 1. 决策变量

**$\text{assigned\_length}_{i}$** ：产品 $i$ 的分配卷长，无量纲归一化系数，取值范围为 $[0, +\infty)$ ，动态长度产品的决策卷长，$\forall i \in P^{dyn}$ 。

### 2. 辅助变量

**$\text{over\_length}_{i}$** ：产品 $i$ 的超长松弛变量，无量纲归一化系数，取值范围为 $[0, +\infty)$ ，记录卷长超过最大超产长度的部分，$\forall i \in P^{dyn}$ 。

---

## 五、谓词

### 1. 产品谓词

**isDynamic** ：产品是否在动态产品 ID 集合中。
**hasBound** ：产品是否定义了卷长上下界。
**hasPenalty** ：产品是否定义了总卷长惩罚或超长惩罚。

---

## 六、集合

### 1. 动态产品集合

**$P^{dyn}$** ：需要动态分配卷长的产品 ID 集合，由 LengthAssignmentModelingConfig.dynamicProductIds 定义。

### 2. 有界产品集合

**$P^{bound}$** ：定义了卷长上下界的产品子集。

### 3. 有惩罚产品集合

**$P^{penalty}$** ：定义了超长惩罚或总卷长惩罚的产品子集。

---

## 七、中间值

### 1. 长度推导值

**描述**：由下游项目注入的长度推导函数计算得出的卷长值。

$$
\text{derivedLength}_{i} = \text{LengthDerivation}(\text{demandQuantity}_{i}, \text{product}_{i})
$$

其定义另参考《长度推导函数》。

---

## 八、断言

### 1. 动态产品必要条件

**描述**：只有动态长度产品才会有长度分配变量。

$$
\forall i \in P \; (\text{assigned\_length}_{i} \neq \text{null} \rightarrow \text{isDynamic}_{i})
$$

---

## 九、约束

### 1. 卷长下界约束

**[英]**：Assigned Length Lower Bound Constraint
**描述**：动态长度产品的分配卷长不得低于配置的下界。

$$
s.t. \quad \text{assigned\_length}_{i} \geq \text{lowerBound}_{i}, \; \forall i \in P^{bound}
$$

### 2. 卷长上界约束

**[英]**：Assigned Length Upper Bound Constraint
**描述**：动态长度产品的分配卷长不得超过配置的上界。

$$
s.t. \quad \text{assigned\_length}_{i} \leq \text{upperBound}_{i}, \; \forall i \in P^{bound}
$$

### 3. 超长上限约束

**[英]**：Over-Length Upper Bound Constraint
**描述**：超长松弛变量不得超过配置的超长上限。

$$
s.t. \quad \text{over\_length}_{i} \leq \text{overLengthUpperBound}_{i}, \; \forall i \in P^{dyn}
$$

### 4. 卷长-超长关联约束

**[英]**：Assigned-Over Length Link Constraint
**描述**：分配卷长减去超长松弛不得超过最大超产长度。

$$
s.t. \quad \text{assigned\_length}_{i} - \text{over\_length}_{i} \leq \text{maxOverProduceLength}_{i}, \; \forall i \in P^{dyn}
$$

---

## 十、目标函数

**描述**：最小化总卷长惩罚和超长惩罚。

$$
\min \sum_{i \in P^{dyn}} (\text{totalLengthPenalty} \times \text{assigned\_length}_{i} + \text{overLengthPenalty}_{i} \times \text{over\_length}_{i})
$$

---

## 十一、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 长度推导函数 | - | 第六章 | 从需求量和产品属性推导卷长 |

---

## 十二、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 分配卷长 | $\text{assigned\_length}$ | Assigned Length | 动态长度产品的决策卷长 |
| 超长松弛 | $\text{over\_length}$ | Over Length | 卷长超过最大超产长度的部分 |
| 超长记录 | - | OverLengthRecord | 记录产品超长量的实体 |
| 长度分配 | - | Length Assignment | 单个产品被分配的卷长结果 |
| 长度推导 | - | Length Derivation | 从需求量推导卷长的函数 |

---

## 十三、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 使用无量纲归一化系数 | 带单位物理量 | 权重由调用方负责单位换算 | - |
| 分离 assignedLength 和 overLength | 合并为单一变量 | 超长可独立惩罚和约束 | - |
| 通过 LengthDerivation 函数式接口注入推导逻辑 | 硬编码推导逻辑 | 支持下游项目按业务单位自定义 | - |

---

## 十四、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始版本 | 动态卷长分配基础功能 |
