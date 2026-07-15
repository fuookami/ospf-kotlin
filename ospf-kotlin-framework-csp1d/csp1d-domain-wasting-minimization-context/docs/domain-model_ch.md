# 浪费最小化上下文 领域模型

[toc]

## 一、概述

浪费最小化上下文负责分析和量化切割方案集合中的各种浪费（余宽、余料、超产面积），并通过惩罚项引导求解器最小化浪费。

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

### 2. 物料（Material）

分切生产的原材料，描述可供切割的母卷规格。

**$\text{id}_{m}$** ：物料 $m$ 的唯一标识符。
**$\text{name}_{m}$** ：物料 $m$ 的名称。
**$\text{widthRange}_{m}$** ：物料 $m$ 的可用幅宽范围，包含上下界和步进值。
**$\text{length}_{m}$** ：物料 $m$ 的卷长。
**$\text{unitWeight}_{m}$** ：物料 $m$ 的单位重量。
**$\text{machineId}_{m}$** ：物料 $m$ 绑定的设备标识，null 表示不限设备。
**$\text{availableBatches}_{m}$** ：物料 $m$ 的可用批次数，主问题按方案使用量求和建模。

### 3. 切割方案（CuttingPlan）

描述一次完整切割操作，包含物料、切片和需求贡献。

**$\text{id}_{p}$** ：方案 $p$ 的唯一标识符。
**$\text{material}_{p}$** ：方案 $p$ 使用的物料。
**$\text{machineId}_{p}$** ：方案 $p$ 使用的设备标识。
**$\text{slices}_{p}$** ：方案 $p$ 的切片列表。
**$\text{demandContributions}_{p}$** ：方案 $p$ 对需求的贡献列表。
**$\text{capacityConsumption}_{p}$** ：方案 $p$ 单次使用的设备产能消耗。

### 4. 产品需求（ProductDemand）

描述对特定产品的生产需求量。

**$\text{product}_{d}$** ：需求 $d$ 对应的产品。
**$\text{quantity}_{d}$** ：需求 $d$ 的需求值（带物理单位）。
**$\text{mode}_{d}$** ：需求 $d$ 的口径标签（Roll/Weight/Sheet）。

---

## 三、本上下文实体

### 1. 余宽浪费记录（RestWidthWaste）

记录切割方案的剩余幅宽浪费。

**$\text{plan}_{w}$** ：记录 $w$ 对应的切割方案。
**$\text{restWidth}_{w}$** ：记录 $w$ 的剩余幅宽（带物理单位）。

### 2. 余料浪费记录（RestMaterialWaste）

记录切割方案的余料面积代理浪费。

**$\text{plan}_{m}$** ：记录 $m$ 对应的切割方案。
**$\text{restMaterial}_{m}$** ：记录 $m$ 的余料面积代理（带物理单位）。

### 3. 超产面积浪费（OverProductionAreaWaste）

记录产品的超产面积浪费。

**$\text{product}_{a}$** ：记录 $a$ 对应的产品。
**$\text{wasteArea}_{a}$** ：记录 $a$ 的浪费面积代理（带物理单位）。

### 4. 浪费分析结果（WasteAnalysis）

切割方案集合的浪费分析汇总。

**$\text{restWidthWastes}_{a}$** ：分析 $a$ 的余宽浪费列表。
**$\text{restMaterialWastes}_{a}$** ：分析 $a$ 的余料浪费列表。
**$\text{totalRestWidth}_{a}$** ：分析 $a$ 的总余宽。
**$\text{totalRestMaterial}_{a}$** ：分析 $a$ 的总余料面积代理。

---

## 四、变量

### 1. 决策变量

本上下文不直接定义决策变量，决策变量由产出上下文管理。

### 2. 辅助变量

本上下文不直接定义辅助变量。

---

## 五、谓词

### 1. 浪费谓词

**hasRestWidth** ：切割方案是否有剩余幅宽浪费。
**hasRestMaterial** ：切割方案是否有余料浪费。

---

## 六、集合

### 1. 浪费方案集合

**$S^{waste}$** ：有剩余幅宽浪费的方案子集。

---

## 七、中间值

### 1. 余宽

**描述**：切割方案中物料上界幅宽减去已使用幅宽。

$$
\text{restWidth}_{j} = \text{material}_{j}.\text{widthRange}.\text{upperBound} - \sum_{s \in \text{slices}_{j}} \text{width}_{s} \times \text{amount}_{s}
$$

### 2. 余料面积代理

**描述**：余宽乘以物料长度，作为余料面积的代理值。

$$
\text{restMaterial}_{j} = \text{restWidth}_{j} \times \text{material}_{j}.\text{length}
$$

### 3. 总余宽

**描述**：所有选中方案的余宽按使用车次累加。

$$
\text{totalRestWidth} = \sum_{j \in S} \text{restWidth}_{j} \times x_{j}
$$

### 4. 总余料面积代理

**描述**：所有选中方案的余料面积代理按使用车次累加。

$$
\text{totalRestMaterial} = \sum_{j \in S} \text{restMaterial}_{j} \times x_{j}
$$

### 5. 超产面积代理

**描述**：超产量乘以产品最大宽度，作为超产面积的代理值。

$$
\text{overArea}_{i} = \text{over}_{i} \times \max(\text{width}_{i})
$$

---

## 八、断言

### 1. 余宽非负

**描述**：切割方案的剩余幅宽必须非负。

$$
\forall j \in S \; (\text{restWidth}_{j} \geq 0)
$$

---

## 九、约束

### 1. 需求平衡约束

**[英]**：Demand Balance Constraint
**描述**：每个需求的总贡献量必须满足需求（与产出上下文共享）。

$$
s.t. \quad \text{demandQuantity}_{i} - \text{over}_{i} + \text{under}_{i} = \text{demand}_{i}, \; \forall i \in D
$$

---

## 十、目标函数

**描述**：最小化各类浪费的加权和。

$$
\min \sum_{j \in S} (\text{restWidth}_{j} \times \text{trimPenalty} + \text{restMaterial}_{j} \times \text{restPenalty} + \text{costPenalty}_{j}) \times x_{j} + \sum_{i \in D} \text{overArea}_{i} \times \text{overAreaPenalty}
$$

---

## 十一、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 余宽计算 | - | 第六章 | 从切割方案计算剩余幅宽 |
| 余料面积代理 | - | 第六章 | 余宽乘以物料长度的代理计算 |

---

## 十二、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 余宽 | $\text{restWidth}$ | Rest Width | 物料上界幅宽减去已使用幅宽 |
| 余料 | $\text{restMaterial}$ | Rest Material | 余宽乘以物料长度的代理值 |
| 超产面积 | $\text{overArea}$ | Over-Production Area | 超产量乘以产品最大宽度的代理值 |
| 余宽惩罚 | $\text{trimPenalty}$ | Trim Width Penalty | 余宽的惩罚权重 |
| 余料惩罚 | $\text{restPenalty}$ | Rest Material Penalty | 余料的惩罚权重 |
| 物料成本惩罚 | $\text{costPenalty}$ | Material Cost Penalty | 按物料 ID 的单位成本惩罚 |
| 超产面积惩罚 | $\text{overAreaPenalty}$ | Over-Production Area Penalty | 超产面积的惩罚权重 |

---

## 十三、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 使用代理值计算余料面积 | 精确计算 | 简化计算，足够准确 | - |
| 浪费目标项直接作用于 x 变量 | 额外 slack 变量 | 无需额外变量，简化模型 | - |
| 分离余宽和余料惩罚 | 统一惩罚 | 不同浪费类型可独立调节权重 | - |

---

## 十四、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始版本 | 基础浪费最小化功能 |
| v1.1 | 增加超产面积惩罚 | 超产浪费量化需求 |
| v1.2 | 增加物料成本惩罚 | 物料成本优化需求 |
