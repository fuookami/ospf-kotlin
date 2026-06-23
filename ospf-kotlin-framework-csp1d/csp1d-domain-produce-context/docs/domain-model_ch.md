# 产出上下文 领域模型

[toc]

## 一、概述

产出上下文负责管理 CSP1D 主问题的核心决策变量（切割方案使用车次）和约束中间符号，支持 MILP 注册和列生成迭代模式。

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

### 3. 设备（Machine）

分切生产设备，约束可加工的物料和产能。

**$\text{id}_{k}$** ：设备 $k$ 的唯一标识符。
**$\text{name}_{k}$** ：设备 $k$ 的名称。
**$\text{maxBatchCount}_{k}$** ：设备 $k$ 的最大批次数上限。
**$\text{maxSwitchCount}_{k}$** ：设备 $k$ 的最大换料次数，当前无序主问题不建模。
**$\text{widthRange}_{k}$** ：设备 $k$ 的可加工幅宽范围。
**$\text{capacity}_{k}$** ：设备 $k$ 的业务产能上限，仅与同单位方案产能消耗一起建模。

### 4. 切割方案（CuttingPlan）

描述一次完整切割操作，包含物料、切片和需求贡献。

**$\text{id}_{p}$** ：方案 $p$ 的唯一标识符。
**$\text{material}_{p}$** ：方案 $p$ 使用的物料。
**$\text{machineId}_{p}$** ：方案 $p$ 使用的设备标识。
**$\text{slices}_{p}$** ：方案 $p$ 的切片列表。
**$\text{demandContributions}_{p}$** ：方案 $p$ 对需求的贡献列表。
**$\text{capacityConsumption}_{p}$** ：方案 $p$ 单次使用的设备产能消耗。

### 5. 切割方案切片（CuttingPlanSlice）

描述单次切割中的产出对象及其幅宽。

**$\text{production}_{s}$** ：切片 $s$ 的产出对象（产品或配规）。
**$\text{width}_{s}$** ：切片 $s$ 的幅宽。
**$\text{amount}_{s}$** ：切片 $s$ 的份数。

### 6. 配规/副产物（Costar）

可填充切割方案剩余宽度的辅助产出物。

**$\text{id}_{c}$** ：配规 $c$ 的唯一标识符。
**$\text{name}_{c}$** ：配规 $c$ 的名称。
**$\text{width}_{c}$** ：配规 $c$ 的宽度列表。
**$\text{length}_{c}$** ：配规 $c$ 的长度。
**$\text{unitWeight}_{c}$** ：配规 $c$ 的单位重量。

### 7. 产品需求（ProductDemand）

描述对特定产品的生产需求量。

**$\text{product}_{d}$** ：需求 $d$ 对应的产品。
**$\text{quantity}_{d}$** ：需求 $d$ 的需求值（带物理单位）。
**$\text{mode}_{d}$** ：需求 $d$ 的口径标签（Roll/Weight/Sheet）。

### 8. 宽度范围（WidthRange）

支持步进和单位一致性的宽度值域。

**$\text{width}_{w}$** ：宽度范围 $w$ 的值域（QuantityRange）。
**$\text{step}_{w}$** ：宽度范围 $w$ 的步进值。

### 9. 需求贡献（CuttingPlanDemandContribution）

描述切割方案对特定产品需求的贡献值。

**$\text{product}_{a}$** ：贡献 $a$ 对应的产品。
**$\text{quantity}_{a}$** ：贡献 $a$ 的贡献值（带物理单位）。

---

## 三、本上下文实体

### 1. 切割方案使用量（CuttingPlanUsage）

描述切割方案的使用车次。

**$\text{plan}_{u}$** ：使用量 $u$ 对应的切割方案。
**$\text{amount}_{u}$** ：使用量 $u$ 的使用车次。

### 2. 物料使用量（MaterialUsage）

描述物料的使用批次数。

**$\text{material}_{m}$** ：使用量 $m$ 对应的物料。
**$\text{amount}_{m}$** ：使用量 $m$ 的使用批次数。

### 3. 设备产能使用（MachineCapacityUsage）

描述设备的产能使用情况。

**$\text{machine}_{c}$** ：使用量 $c$ 对应的设备。
**$\text{used}_{c}$** ：使用量 $c$ 的实际使用产能（由方案产能消耗聚合）。

### 4. 主问题求解产出（Produce）

主问题求解的完整产出结果。

**$\text{cuttingPlans}_{p}$** ：产出 $p$ 中选中的切割方案列表。
**$\text{materialUsages}_{p}$** ：产出 $p$ 中的物料使用统计。
**$\text{machineUsages}_{p}$** ：产出 $p$ 中的设备产能统计。
**$\text{unmetDemands}_{p}$** ：产出 $p$ 中未满足的需求。

### 5. 需求贡献聚合键（ContributionKey）

确保同产品不同单位贡献不混算的聚合键。

**$\text{productId}_{k}$** ：聚合键 $k$ 的产品 ID。
**$\text{unit}_{k}$** ：聚合键 $k$ 的物理单位。

---

## 四、变量

### 1. 决策变量

**$x_{j}$** ：切割方案 $j$ 的使用车次，整数变量，取值范围为 $\{0, 1, 2, \ldots\}$ ，表示方案 $j$ 被执行的次数，$\forall j \in S$ 。

### 2. 辅助变量

本上下文不直接定义辅助变量，辅助变量由长度分配上下文和产出偏差上下文管理。

---

## 五、谓词

### 1. 方案谓词

**hasDemandContribution** ：方案是否对某个需求有贡献。
**isAssignedToMachine** ：方案是否分配到某个设备。

---

## 六、集合

### 1. 切割方案集合

**$S$** ：所有切割方案的全集。

**$S_{m}$** ：使用物料 $m$ 的方案子集。
**$S_{k}$** ：使用设备 $k$ 的方案子集。

### 2. 迭代方案集合

**$S^{(t)}$** ：第 $t$ 次迭代新增的方案子集。

---

## 七、中间值

### 1. 需求贡献量

**描述**：每个需求的总贡献量，由方案使用量和贡献系数聚合。

$$
\text{demandQuantity}_{i} = \sum_{j \in S} \text{contribution}_{ij} \times x_{j}, \; \forall i \in D
$$

其中 $\text{contribution}_{ij}$ 是方案 $j$ 对需求 $i$ 的贡献系数。

### 2. 物料使用量

**描述**：每个物料的总使用量。

$$
\text{materialQuantity}_{m} = \sum_{j \in S_{m}} x_{j}, \; \forall m \in M
$$

### 3. 设备批次数

**描述**：每个设备的总批次数。

$$
\text{machineBatchQuantity}_{k} = \sum_{j \in S_{k}} x_{j}, \; \forall k \in K
$$

### 4. 设备产能消耗

**描述**：每个设备的总产能消耗。

$$
\text{machineCapacityQuantity}_{k} = \sum_{j \in S_{k}} \text{consumption}_{j} \times x_{j}, \; \forall k \in K
$$

---

## 八、断言

### 1. 方案唯一性

**描述**：每个切割方案有唯一标识符。

$$
\forall j_1, j_2 \in S \; (j_1 \neq j_2 \rightarrow \text{id}_{j_1} \neq \text{id}_{j_2})
$$

### 2. 列生成去重

**描述**：新增方案通过 id 和 canonicalKey 去重。

$$
\forall j \in S^{(t)} \; (\text{id}_{j} \notin \text{registeredIds} \wedge \text{canonicalKey}_{j} \notin \text{registeredKeys})
$$

---

## 九、约束

### 1. 需求平衡约束

**[英]**：Demand Balance Constraint
**描述**：每个需求的总贡献量必须满足需求（有 yield slack 时为等式，无 yield slack 时为不等式）。

有 yield slack 时：

$$
s.t. \quad \text{demandQuantity}_{i} - \text{over}_{i} + \text{under}_{i} = \text{demand}_{i}, \; \forall i \in D
$$

无 yield slack 时：

$$
s.t. \quad \text{demandQuantity}_{i} \geq \text{demand}_{i}, \; \forall i \in D
$$

### 2. 物料可用批次约束

**[英]**：Material Available Batch Constraint
**描述**：每个物料的使用总量不得超过其可用批次数。

$$
s.t. \quad \text{materialQuantity}_{m} \leq \text{availableBatches}_{m}, \; \forall m \in M
$$

### 3. 设备批次数约束

**[英]**：Machine Batch Count Constraint
**描述**：分配到每个设备的方案使用总量不得超过设备最大批次数。

$$
s.t. \quad \text{machineBatchQuantity}_{k} \leq \text{maxBatchCount}_{k}, \; \forall k \in K
$$

### 4. 设备产能约束

**[英]**：Machine Capacity Constraint
**描述**：分配到每个设备的产能消耗总量不得超过设备产能上限。

$$
s.t. \quad \text{machineCapacityQuantity}_{k} \leq \text{capacity}_{k}, \; \forall k \in K
$$

---

## 十、目标函数

**描述**：最小化总批次使用量，可通过 batchCoefficient 施加额外权重。

$$
\min \sum_{j \in S} \text{batchCoefficient} \times x_{j}
$$

---

## 十一、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 列生成 | - | 第五章 | 列生成迭代过程中的方案管理 |

---

## 十二、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 使用车次 | $x$ | Batch Count | 切割方案被执行的次数 |
| 需求贡献量 | $\text{demandQuantity}$ | Demand Quantity | 每个需求的总贡献量 |
| 物料使用量 | $\text{materialQuantity}$ | Material Quantity | 每个物料的总使用量 |
| 设备批次数 | $\text{machineBatchQuantity}$ | Machine Batch Quantity | 每个设备的总批次数 |
| 设备产能消耗 | $\text{machineCapacityQuantity}$ | Machine Capacity Quantity | 每个设备的总产能消耗 |
| 中间符号 | - | Intermediate Symbol | 约束管线引用的中间表达式 |
| 列生成 | - | Column Generation | 迭代添加新方案的求解方法 |

---

## 十三、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 使用中间符号引用约束管线 | 直接引用 x 变量 | addColumns 时只需刷新中间符号，无需刷新约束 | - |
| 使用 canonicalKey 去重 | 仅使用 id 去重 | 防止语义相同但 id 不同的方案重复添加 | - |
| 支持列生成迭代模式 | 单次注册模式 | 支持大规模问题的迭代求解 | - |

---

## 十四、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始版本 | 主问题核心变量和约束管理 |
| v1.1 | 支持列生成迭代模式 | 大规模问题求解需求 |
| v1.2 | 引入中间符号机制 | 简化 addColumns 时的约束刷新逻辑 |
