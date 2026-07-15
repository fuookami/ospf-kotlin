# 装载分配 领域模型

[toc]

## 一、概述

管理货物装载分配决策——确定各货物项装载到哪个位置——包括载荷重量计算、载荷量计算、总重量和最大装载重量。

### 1. 依赖上下文

1. 飞机（aircraft）

---

## 二、概念/实体

### 1. 货物项

货物项，含目的地、重量、ULD、位置标签、货物类型、优先级和状态。

**$id_{i}$** ：货物项唯一标识。
**$dest_{i}$** ：目的地（IATA 代码）。
**$weight_{i}$** ：货物重量，单位 kg。
**$uld_{i}$** ：所属集装器（可选）。
**$location_{i}$** ：位置标签（Main/Low/Bulk/Head/Tail）。
**$cargo_{i}$** ：货物类型和优先级。
**$status_{i}$** ：状态（Loaded/Preassigned/Optional/Reserved/AdjustmentNeeded）。
**$order_{i}$** ：排序信息（hardstand 时间、reweigh 时间、car-board 信息）。

### 2. 装载位置

装载位置，含最大装载数量（MLA）、谓词装载重量（PLW）、推荐装载重量和状态。

**$spaceName_{j}$** ：空间名称。
**$mla_{j}$** ：最大装载数量（Max Load Amount）。
**$plw_{j}$** ：谓词装载重量（Predicate Load Weight），其定义另参考《装载分配》。
**$mlw_{j}$** ：最大装载重量（Max Load Weight），其定义另参考《适航安全》。
**$coordinate_{j}$** ：坐标（纵向臂、横向臂）。
**$location_{j}$** ：位置标签集合。

### 3. 航班

航班信息。

**$flightNo$** ：航班号。
**$departure$** ：出发机场（IATA 代码）。
**$arrival$** ：到达机场（IATA 代码）。

### 4. 预约

预分配的货物-位置预约。

**$appointment$** ：货物到位置的预分配映射。

### 5. 压舱物

压舱物重量用于平衡修正。

**$minBallastWeight$** ：最小压舱物重量。

---

## 三、变量

### 1. 决策变量

**$x_{ij}$** ：货物 $i$ 是否装载到位置 $j$ ，二元变量，取值范围为 $\{0, 1\}$ ，$\forall i \in Items$ ，$\forall j \in Positions$ 。

**$y_{j}$** ：位置 $j$ 的谓词装载重量，连续变量（kg），取值范围为 $[0, MLW_j]$ ，$\forall j \in Positions$ 。

**$z_{j}$** ：位置 $j$ 的推荐装载重量，整数变量（kg），取值范围为 $[0, \lfloor MLW_j \rfloor]$ ，$\forall j \in Positions$ 。

### 2. 辅助变量

**$u_{ij}$** ：货物 $i$ 在位置 $j$ 的调整变量，二元变量，取值范围为 $\{0, 1\}$ ，$\forall i \in Items$ ，$\forall j \in Positions$ 。

---

## 四、谓词

### 1. 货物状态谓词

**stowageNeeded(item)** ：货物需要分配位置（状态为 Preassigned 或 Optional）。
**adjustmentNeeded(item)** ：货物需要调整位置（状态为 AdjustmentNeeded）。
**loaded(item)** ：货物已装载（状态为 Loaded 或 AdjustmentNeeded）。

### 2. 位置状态谓词

**stowageNeeded(position)** ：位置需要分配货物。
**available(position)** ：位置可用于装载。
**predicateWeightNeeded(position)** ：位置需要谓词重量变量。
**recommendedWeightNeeded(position)** ：位置需要推荐重量变量。

---

## 五、集合

### 1. 货物项

**$I$** ：所有货物项集合。

**$I^{pre}$** ：满足谓词 stowageNeeded 的货物子集，需要分配位置的货物。
**$I^{adj}$** ：满足谓词 adjustmentNeeded 的货物子集，需要调整位置的货物。
**$I^{opt}$** ：Optional 状态的货物子集，可选装载的货物。

### 2. 装载位置

**$J$** ：所有装载位置集合。

**$J^{avl}$** ：满足谓词 available 的位置子集，可用于装载的位置。
**$J^{pw}$** ：满足谓词 predicateWeightNeeded 的位置子集，需要谓词重量的位置。
**$J^{rw}$** ：满足谓词 recommendedWeightNeeded 的位置子集，需要推荐重量的位置。

### 3. 货物-位置对

**$IJ^{feas}$** ：可行的货物-位置分配对集合，即满足 stowageNeeded(item, position) 的 $(i, j)$ 对。

---

## 六、中间值

### 1. 装载数量

**描述**：每个位置的装载货物数量。

$$
loadAmount_j = \sum_{i \in I} stowage_{ij}, \; \forall j \in J
$$

### 2. 装载重量

**描述**：每个位置的实际装载重量。

$$
actualLoadWeight_j = \sum_{i \in I} weight_i \cdot stowage_{ij}, \; \forall j \in J
$$

### 3. 估算装载重量

**描述**：包含谓词和推荐重量的估算装载重量。

$$
estimateLoadWeight_j = \sum_{i \in I} weight_i \cdot stowage_{ij} + y_j + z_j, \; \forall j \in J
$$

---

## 七、约束

### 1. 货物分配限制

**[英]**：Item Assignment Limit
**描述**：每个需要装载的货物必须分配到恰好一个位置。

$$
s.t. \quad \sum_{j \in J} x_{ij} = 1, \; \forall i \in I^{pre}
$$

### 2. 装载数量限制

**[英]**：Load Amount Limit
**描述**：每个位置的装载数量不得超过最大装载数量（MLA）。

$$
s.t. \quad \sum_{i \in I} stowage_{ij} \leq mla_j, \; \forall j \in J
$$

### 3. 装载重量限制

**[英]**：Load Weight Limit
**描述**：每个位置的装载重量不得超过最大装载重量（MLW）。

$$
s.t. \quad actualLoadWeight_j \leq mlw_j, \; \forall j \in J
$$

### 4. 预约限制

**[英]**：Appointment Limit
**描述**：预分配的货物-位置预约必须被遵守。

$$
s.t. \quad x_{ij} = 1, \; \forall (i, j) \in Appointment
$$

---

## 八、目标函数

本上下文不定义独立目标函数，仅提供约束。

---

## 九、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 货物项 | Item | Item | 待装载的货物单元 |
| 装载位置 | Position | Position | 飞机上的货物装载位置 |
| 装载分配 | Stowage | Stowage | 货物到位置的分配决策 |
| 装载量 | Load | Load | 位置的装载重量和数量 |
| 载荷 | Payload | Payload | 飞机的总货物重量 |
| 总重量 | TotalWeight | Total Weight | 飞机各阶段的总重量 |
| 最大装载重量 | MaxLoadWeight | Max Load Weight | 位置的最大允许装载重量 |
| 压舱物 | Ballast | Ballast | 用于平衡的压舱物重量 |
| 谓词装载重量 | PLW | Predicate Load Weight | 预测的装载重量 |
| 最大装载数量 | MLA | Max Load Amount | 位置的最大装载数量 |

---

## 十、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 装载模式选择 | FullLoad / Predistribution / WeightRecommendation | 根据业务场景选择不同装载模式 | 2024 |
| Benders 分解 | 主问题/子问题分离 | 适航安全约束放入子问题，其余放入主问题 | 2024 |
