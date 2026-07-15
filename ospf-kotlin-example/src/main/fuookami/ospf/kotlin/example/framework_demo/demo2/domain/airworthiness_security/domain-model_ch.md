# 适航安全 领域模型

[toc]

## 一、概述

执行适航和安全约束，包括线密度/面密度限制、区域载荷重量限制、累积载荷重量限制、CLIM 限制、包络线约束和载荷限制。

### 1. 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）
3. 平均气动弦（mac）

---

## 二、概念/实体

### 1. 线密度

各机身区域的线性重量密度及上下限。

**$limitZones$** ：限制区域列表。
**$density_{zone}$** ：区域线密度值。

### 2. 面密度

各区域的面重量密度及上下限。

**$limitZones$** ：限制区域列表。
**$density_{zone}$** ：区域面密度值。

### 3. 最大区域载荷重量

各机身区域的最大允许载荷重量。

**$maxWeight_{zone}$** ：区域最大载荷重量。

### 4. 最大累积载荷重量

从机头/机尾起的最大累积载荷重量。

**$maxWeight_{fromNose}$** ：从机头起的最大累积重量。
**$maxWeight_{fromTail}$** ：从机尾起的最大累积重量。

### 5. 最大非对称线密度

宽体飞机的最大允许非对称线密度。

**$maxDensity$** ：最大非对称线密度。

### 6. 最大 CLIM

宽体飞机的最大 CLIM 限制。

**$points$** ：CLIM 限制数据点。

### 7. 最小下层载荷

下层甲板所需的最小载荷量。

**$points$** ：最小载荷数据点。

### 8. 包络线

各飞行阶段的重量-重心包络线约束。

**$envelopes_{phase}$** ：各飞行阶段的包络线。

---

## 三、变量

本上下文复用装载分配上下文的决策变量，不定义独立决策变量。

---

## 四、中间值

### 1. 线密度

**描述**：各机身区域的线性重量密度。

$$
linearDensity_{zone} = \frac{\sum_{j \in zone} loadWeight_j}{zoneLength}
$$

### 2. 面密度

**描述**：各区域的面重量密度。

$$
surfaceDensity_{zone} = \frac{\sum_{j \in zone} loadWeight_j}{zoneArea}
$$

---

## 五、约束

### 1. 线密度限制

**[英]**：Linear Density Limit
**描述**：各区域的线密度必须在限制范围内。

$$
s.t. \quad linearDensityMin_{zone} \leq linearDensity_{zone} \leq linearDensityMax_{zone}, \; \forall zone \in Zones
$$

### 2. 面密度限制

**[英]**：Surface Density Limit
**描述**：各区域的面密度必须在限制范围内。

$$
s.t. \quad surfaceDensityMin_{zone} \leq surfaceDensity_{zone} \leq surfaceDensityMax_{zone}, \; \forall zone \in Zones
$$

### 3. 区域载荷重量限制

**[英]**：Zone Load Weight Limit
**描述**：区域载荷重量不得超过最大值。

$$
s.t. \quad \sum_{j \in zone} loadWeight_j \leq maxZoneLoadWeight_{zone}, \; \forall zone \in Zones
$$

### 4. 累积载荷重量限制

**[英]**：Cumulative Load Weight Limit
**描述**：从机头/机尾起的累积载荷重量不得超过最大值。

$$
s.t. \quad \sum_{j \leq k} loadWeight_j \leq maxCumulativeWeight_k, \; \forall k \in Positions
$$

### 5. 包络线限制

**[英]**：Envelope Limit
**描述**：重量-重心组合必须在各飞行阶段的包络线内。

$$
s.t. \quad (weight_{phase}, mac) \in envelope_{phase}, \; \forall phase \in FlightPhases
$$

### 6. 载荷限制

**[英]**：Payload Limit
**描述**：载荷必须在计划和最大范围内。

$$
s.t. \quad plannedPayload \leq payload \leq maxPayload
$$

### 7. 总重量限制

**[英]**：Total Weight Limit
**描述**：各飞行阶段的总重量不得超过最大值。

$$
s.t. \quad totalWeight_{phase} \leq maxTotalWeight_{phase}, \; \forall phase \in FlightPhases
$$

---

## 六、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 线密度 | LinearDensity | Linear Density | 单位长度的重量 |
| 面密度 | SurfaceDensity | Surface Density | 单位面积的重量 |
| 包络线 | Envelope | Envelope | 重量-重心可行区域 |
| CLIM | CLIM | Center of Gravity Index Moment | 重心指数力矩 |
| 区域载荷 | ZoneLoadWeight | Zone Load Weight | 机身区域的载荷重量 |
| 累积载荷 | CumulativeLoadWeight | Cumulative Load Weight | 从端点起的累积载荷 |

---

## 七、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 包络线建模 | 线性化 vs 分段线性 | 分段线性更精确 | 2024 |
| Benders 分解 | 适航约束在子问题 | 适航约束与装载强耦合 | 2024 |
