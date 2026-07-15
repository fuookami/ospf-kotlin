# 资源限界上下文领域模型 — 甘特调度

[toc]

## 一、概述

资源上下文负责建模甘特调度中可消耗资源的容量约束与松弛变量。它追踪任务如何随时间消耗共享资源——执行资源在任务期间消耗、连接资源在任务转换期间消耗、存储资源在库存中平衡——并在容量范围内强制累积使用量。该上下文支持三种调度模式：任务级、批次（列生成）和产能调度。

### 1. 依赖上下文

1. **甘特调度基础设施层**（gantt-scheduling-infrastructure）— TimeRange、TimeWindow、TimeSlot
2. **甘特调度任务域**（gantt-scheduling-domain-task-context）— Executor、AssignmentPolicy、AbstractTask、AbstractTaskBunch
3. **甘特调度任务编排上下文**（gantt-scheduling-domain-task-compilation-context）— IterativeTaskCompilation
4. **甘特调度批次编排上下文**（gantt-scheduling-domain-bunch-compilation-context）— BunchCompilation
5. **甘特调度产能调度上下文**（gantt-scheduling-domain-capacity-scheduling-context）— ProductionAction、Capacity、CapacityColumn、IterativeCapacityCompilation
6. **OSPF 核心**（ospf-kotlin-core）— MetaModel、LinearIntermediateSymbols、SlackFunction
7. **OSPF 框架**（ospf-kotlin-framework）— ShadowPrice、ShadowPriceKey、Pipeline

---

## 二、概念/实体

### 1. 资源容量（ResourceCapacity\<V\>）

一个时间有界的数量范围，定义资源在某时间区间内的可用性。每个容量指定允许的使用范围、可选的松弛量及其管辖的时间段。

**$time(C)$** ：容量 $C$ 生效的时间范围，TimeRange 值。
**$quantityRangeValue(C)$** ：容量 $C$ 的允许使用范围 $[LB, UB]$，QuantityRange 值。
**$lessQuantityValue(C)$** ：容量 $C$ 允许的最大欠量，Quantity 值（可选）。
**$overQuantityValue(C)$** ：容量 $C$ 允许的最大超量，Quantity 值（可选）。
**$interval(C)$** ：容量 $C$ 的时间区间长度，Duration 值。
**$name(C)$** ：容量 $C$ 的人类可读名称。
**$lessEnabled(C)$** ：派生谓词；当 $lessQuantityValue(C)$ 已定义且非零时为真。
**$overEnabled(C)$** ：派生谓词；当 $overQuantityValue(C)$ 已定义且非零时为真。

### 2. 资源（Resource\<C, V\>）

拥有容量并被任务消耗的实体。资源是核心聚合根，拥有容量集合和初始数量值。子类特化消耗语义。

**$id(R)$** ：资源 $R$ 的唯一标识符。
**$name(R)$** ：资源 $R$ 的人类可读名称。
**$capacities(R)$** ：资源 $R$ 拥有的 ResourceCapacity 实例集合。
**$initialQuantityValue(R)$** ：资源 $R$ 在任何消耗之前的起始数量水平。

**$usedQuantityQuantity(R, B, t, u)$** ：抽象方法，计算批次 $B$ 在时间 $t$、单位 $u$ 下消耗的总数量。

### 3. 执行资源（ExecutionResource\<C, V\>）

在任务执行期间消耗的资源。使用量由任务的活跃时间区间决定。

**$usedBy(R, T, t)$** ：抽象方法，返回资源 $R$ 在时间 $t$ 被任务 $T$ 消耗的数量。

### 4. 连接资源（ConnectionResource\<C, V\>）

在任务转换期间消耗的资源。使用量取决于连续任务之间的邻接关系。

**$usedBy(R, T_{prev}, T_{next}, t)$** ：抽象方法，返回资源 $R$ 在时间 $t$ 被从 $T_{prev}$ 到 $T_{next}$ 的转换消耗的数量（任一可为 null）。

### 5. 存储资源（StorageResource\<C, V\>）

具有供给和成本平衡语义的资源，代表随时间累积的库存。

**$costBy(R, T, t)$** ：抽象方法，返回资源 $R$ 在时间 $t$ 被任务 $T$ 消耗的成本数量。
**$supplyBy(R, T, t)$** ：抽象方法，返回资源 $R$ 在时间 $t$ 被任务 $T$ 产生的供给数量。
**$fixedCostIn(R, t)$** ：具体方法，返回资源 $R$ 在时间 $t$ 的时间比例固定成本。
**$fixedSupplyIn(R, t)$** ：具体方法，返回资源 $R$ 在时间 $t$ 的时间比例固定供给。

### 6. 资源时间槽（ResourceTimeSlot\<R, C, V\>）

与特定资源和容量关联的离散化时间区间，是容量约束建模的基本单位。

**$origin(S)$** ：该资源时间槽所源自的底层 TimeSlot。
**$resource(S)$** ：该时间槽所属的资源。
**$resourceCapacity(S)$** ：管辖该时间槽的容量。
**$time(S)$** ：该时间槽覆盖的时间范围。
**$indexInRule(S)$** ：该时间槽在容量规则中的索引。

**$relatedTo(S, T_{prev}, T_{next})$** ：谓词，表示任务对 $(T_{prev}, T_{next})$ 与时间槽 $S$ 是否有非零关系。
**$relationTo(S, T_{prev}, T_{next})$** ：返回任务对与时间槽 $S$ 之间的定量关系系数。

### 7. 执行资源时间槽（ExecutionResourceTimeSlot）

针对执行资源特化的 ResourceTimeSlot。继承 ResourceTimeSlot 的所有属性和方法。

### 8. 连接资源时间槽（ConnectionResourceTimeSlot）

针对连接资源特化的 ResourceTimeSlot。继承 ResourceTimeSlot 的所有属性和方法。

### 9. 存储资源时间槽（StorageResourceTimeSlot）

针对存储资源特化的 ResourceTimeSlot。继承 ResourceTimeSlot 的所有属性和方法，并附加时间窗口引用。

**$timeWindow(S)$** ：与该存储资源时间槽关联的 TimeWindow。

### 10. 产能动作资源（CapacityActionResource\<C, V\>）

产能调度模式下使用的资源变体，消耗由生产动作而非任务驱动。

**$usedBy(R, A, t)$** ：抽象方法，返回资源 $R$ 在时间 $t$ 被生产动作 $A$ 消耗的数量。

### 11. 资源使用量（ResourceUsage\<S, R, C, V\>）

追踪跨时间槽的聚合资源消耗，是领域资源与求解器决策变量之间的桥梁。

**$name(U)$** ：该使用量实例的名称。
**$timeSlots(U)$** ：该使用量追踪的资源时间槽集合。
**$quantity(U, s)$** ：时间槽 $s$ 的总资源使用量决策变量，$\forall s \in S$。
**$overQuantity(U, s)$** ：时间槽 $s$ 的超量松弛决策变量，$\forall s \in S$（overEnabled 时生效）。
**$lessQuantity(U, s)$** ：时间槽 $s$ 的欠量松弛决策变量，$\forall s \in S$（lessEnabled 时生效）。
**$overEnabled(U)$** ：该使用量是否启用超量松弛。
**$lessEnabled(U)$** ：该使用量是否启用欠量松弛。

### 12. 资源容量影子价格键（ResourceCapacityShadowPriceKey）

影子价格键，唯一标识资源容量约束，用于列生成中的对偶变量提取。

---

## 三、变量

### 1. 决策变量

**$quantity_{s}$** ：连续变量，表示时间槽 $s$ 的总资源使用量，$\forall s \in S$。从资源初始数量初始化，累加任务/批次/动作贡献。

**$overQuantity_{s}$** ：连续非负变量，表示时间槽 $s$ 的超量松弛，$\forall s \in S$。仅当资源容量的 $overEnabled$ 为真时激活。

**$lessQuantity_{s}$** ：连续非负变量，表示时间槽 $s$ 的欠量松弛，$\forall s \in S$。仅当资源容量的 $lessEnabled$ 为真时激活。

**$supply_{r,t}$** ：连续变量，表示存储资源 $r$ 在时间 $t$ 的累积供给量，$\forall r \in R_{storage}$，$\forall t \in T$。

**$executorSupply_{e,r,t}$** ：连续变量，表示执行器 $e$ 对存储资源 $r$ 在时间 $t$ 的供给贡献，$\forall e \in E$，$\forall r \in R_{storage}$，$\forall t \in T$。

**$cost_{r,t}$** ：连续变量，表示存储资源 $r$ 在时间 $t$ 的累积成本量，$\forall r \in R_{storage}$，$\forall t \in T$。

### 2. 辅助变量

本上下文除上述松弛变量外不定义辅助变量。

---

## 四、谓词

### 1. 容量松弛谓词

**lessEnabled(C)** ：资源容量 $C$ 允许欠量松弛，即 $lessQuantityValue(C)$ 已定义且非零。
**overEnabled(C)** ：资源容量 $C$ 允许超量松弛，即 $overQuantityValue(C)$ 已定义且非零。

### 2. 时间槽关系谓词

**relatedTo(S, T_{prev}, T_{next})** ：时间槽 $S$ 与任务对 $(T_{prev}, T_{next})$ 有非零关系，即任务在该时间槽的时间范围内对资源消耗有贡献。

---

## 五、集合

### 1. 资源

**$R$** ：所有资源的全集。

**$R_{exec}$** ：执行资源子集（ExecutionResource）。
**$R_{conn}$** ：连接资源子集（ConnectionResource）。
**$R_{storage}$** ：存储资源子集（StorageResource）。
**$R_{capAction}$** ：产能动作资源子集（CapacityActionResource）。

### 2. 容量

**$C_{r}$** ：资源 $r$ 拥有的所有容量集合，$\forall r \in R$。

### 3. 时间槽

**$S$** ：所有离散化资源时间槽的全集。

**$S_{r}$** ：与资源 $r$ 关联的时间槽子集，$\forall r \in R$。
**$S_{c}$** ：由容量 $c$ 管辖的时间槽子集，$\forall c \in C_{r}$。

### 4. 执行器

**$E$** ：所有执行器集合，与存储资源供给计算相关。

### 5. 生产动作

**$A$** ：所有生产动作集合，与产能调度模式相关。

### 6. 批次

**$B$** ：列生成中所有任务批次（列）的集合。

---

## 六、中间值

### 1. 求解器下界

**描述**：资源容量数量范围的下界转换为求解器数值。

$$
solverLowerBound_{c} = quantityRangeValue(c).lowerBound.value.unwrap().toSolverValue()
$$

### 2. 求解器上界

**描述**：资源容量数量范围的上界转换为求解器数值。

$$
solverUpperBound_{c} = quantityRangeValue(c).upperBound.value.unwrap().toSolverValue()
$$

### 3. 求解器欠量

**描述**：允许的最大欠量转换为求解器数值。

$$
solverLessQuantity_{c} = lessQuantityValue(c).value.toSolverValue() \quad (\text{若为 null 则为 } 0)
$$

### 4. 求解器超量

**描述**：允许的最大超量转换为求解器数值。

$$
solverOverQuantity_{c} = overQuantityValue(c).value.toSolverValue() \quad (\text{若为 null 则为 } 0)
$$

### 5. 求解器值域

**描述**：考虑松弛量后的有效容量范围。

$$
solverValueRange_{c} = [solverLowerBound_{c} - solverLessQuantity_{c}, \; solverUpperBound_{c} + solverOverQuantity_{c}]
$$

### 6. 求解器初始数量

**描述**：资源的初始数量转换为求解器数值。

$$
solverInitialQuantity_{r} = initialQuantityValue(r).value.toSolverValue()
$$

### 7. 执行数量

**描述**：对于执行资源，时间槽的总数量为初始数量加上该时间槽时间范围内所有任务贡献之和。

$$
quantity_{s} = solverInitialQuantity_{r} + \sum_{T \in Tasks} usedBy(r, T, time(s)), \; \forall s \in S_{r}, \; r \in R_{exec}
$$

### 8. 存储数量

**描述**：对于存储资源，时间槽的总数量为初始数量加上累积供给减去累积成本。

$$
quantity_{s} = solverInitialQuantity_{r} + supply_{r,t} - cost_{r,t}, \; \forall s \in S_{r}, \; r \in R_{storage}
$$

### 9. 总供给

**描述**：存储资源的总供给由固定供给和各执行器贡献组成。

$$
supply_{r,t} = fixedSupplyIn(r, t) + \sum_{e \in E} executorSupply_{e,r,t}, \; \forall r \in R_{storage}, \; \forall t \in T
$$

---

## 七、断言

### 1. 非空批次

**描述**：通过批次方式添加列时，批次集合不能为空。

$$
|B| > 0 \quad \text{（addColumns 批次变体中）}
$$

### 2. 非空任务

**描述**：通过任务方式添加列时，任务集合不能为空。

$$
|Tasks| > 0 \quad \text{（addColumns 任务变体中）}
$$

### 3. 容量边界有效性

**描述**：资源容量边界必须至少包含一个有限边界（上界或下界）。

$$
\forall c \in C \; (|LB(c)| < \infty \vee |UB(c)| < \infty)
$$

### 4. 时间槽生成有效性

**描述**：时间槽的开始时间必须严格小于结束时间。

$$
\forall s \in S \; (beginTime(s) < endTime(s))
$$

---

## 八、约束

### 1. 资源容量上界约束

**[英]**：Resource Capacity Upper Bound Constraint
**描述**：每个时间槽的总资源使用量不得超过管辖容量的上界。当启用松弛且允许超量时，超量松弛变量吸收违反量。

$$
s.t. \quad quantity_{s} \leq solverUpperBound_{c(s)}, \; \forall s \in S
$$

启用松弛时（$withSlack \wedge overEnabled$）：

$$
s.t. \quad quantity_{s} - overQuantity_{s} \cdot polyX \leq solverUpperBound_{c(s)}, \; \forall s \in S
$$

标记 $ResourceCapacityShadowPriceKey(s)$ 以提取对偶变量。

### 2. 资源容量下界约束

**[英]**：Resource Capacity Lower Bound Constraint
**描述**：每个时间槽的总资源使用量不得低于管辖容量的下界。当启用松弛且允许欠量时，欠量松弛变量吸收违反量。

$$
s.t. \quad quantity_{s} \geq solverLowerBound_{c(s)}, \; \forall s \in S
$$

启用松弛时（$withSlack \wedge lessEnabled$）：

$$
s.t. \quad quantity_{s} + lessQuantity_{s} \cdot polyX \geq solverLowerBound_{c(s)}, \; \forall s \in S
$$

标记 $ResourceCapacityShadowPriceKey(s)$ 以提取对偶变量。

---

## 九、目标函数

### 1. 资源超量最小化

**描述**：最小化所有时间槽的超量松弛总和，惩罚资源容量违反。支持基于阈值的松弛加权。

$$
\min \sum_{s \in S} coeff(s) \cdot overQuantity_{s}
$$

### 2. 资源欠量最小化

**描述**：最小化所有时间槽的欠量松弛总和，惩罚资源容量利用不足。支持基于阈值的松弛加权。

$$
\min \sum_{s \in S} coeff(s) \cdot lessQuantity_{s}
$$

---

## 十、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 列生成（Column Generation） | ospf-kotlin-core | 第三章、第六章、第八章、第九章 | 批次/产能调度模式的 RMP 求解与列迭代 |
| 松弛函数（Slack Function） | ospf-kotlin-core | 第八章 | 将约束违反量分解为正/负松弛分量 |
| 影子价格管道（Shadow Price Pipeline） | ospf-kotlin-framework | 第八章 | 统一约束添加与对偶变量提取，用于 CG 定价 |
| 协程并行（Coroutine Parallelism） | Kotlin Coroutines | 第六章（存储） | 存储资源的并行供给/成本计算 |

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 资源 | $R$ / Resource\<C,V\> | Resource | 拥有容量并被任务消耗的实体 |
| 容量 | $C$ / ResourceCapacity\<V\> | Capacity | 资源可用性的时间有界数量范围 |
| 数量范围 | QuantityRange\<V\> | Quantity Range | 允许使用量的 [LB,UB] 区间 |
| 欠量 | lessQuantityValue | Less Quantity | 允许的最大欠量 |
| 超量 | overQuantityValue | Over Quantity | 允许的最大超量 |
| 初始数量 | initialQuantityValue | Initial Quantity | 消耗前的起始资源水平 |
| 时间槽 | $s$ / ResourceTimeSlot\<R,C,V\> | Time Slot | 资源的离散化时间区间 |
| 时间窗口 | TimeWindow\<V\> | Time Window | 带离散化的调度视界 |
| 使用量 | $U$ / ResourceUsage\<S,R,C,V\> | Usage | 跨时间槽的聚合资源消耗 |
| 数量 | $quantity_{s}$ | Quantity | 时间槽上的总资源使用量 LP 变量 |
| 松弛 | SlackFunction | Slack | 将约束违反量分解为正/负部分 |
| 影子价格 | ShadowPrice | Shadow Price | 用于 CG 定价的对偶变量 |
| 执行器供给 | $executorSupply_{e,r,t}$ | Executor Supply | 每个执行器的资源贡献（存储） |
| 固定成本 | $fixedCostIn(r,t)$ | Fixed Cost | 时间比例的固定资源成本 |
| 固定供给 | $fixedSupplyIn(r,t)$ | Fixed Supply | 时间比例的固定资源供给 |
| 生产动作 | $A$ / ProductionAction | Production Action | 消耗资源的制造动作 |
| 列 | $B$ / CapacityColumn / AbstractTaskBunch | Column | CG 的候选调度模式 |
| 编排 | Compilation | Compilation | 跨迭代累积列变量 |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 三种资源原型（执行、连接、存储） | 统一资源模型 | 不同的消耗语义（执行期间、转换期间、供给/成本平衡）需要不同的抽象 | 2025 |
| 求解器值边界转换（领域 V 到 Flt64） | 直接使用领域类型的求解器 | 边界转换将求解器数值关注点与领域模型隔离，支持无单位的资源定义 | 2025 |
| 惰性变量初始化（register 中的 lateinit） | 急切初始化 | 延迟初始化避免过早的求解器分配，支持迭代列生成工作流 | 2025 |
| 通过 addColumns 方法支持列生成 | 单体注册 | 为批次、任务和产能调度分别提供 addColumns 变体，支持按模式灵活集成 CG | 2025 |
| 任务级调度存根返回 Failed | 完整任务级实现 | 任务级调度已延迟；存根提供明确的失败信号而不阻塞其他模式 | 2025 |
| 存储资源的协程并行 | 顺序计算 | 存储资源涉及独立的执行器计算，可从并行执行中获益 | 2025 |
| 影子价格管道用于约束/对偶耦合 | 手动对偶提取 | 统一管道自动执行约束添加和对偶变量标记，减少 CG 迭代中的样板代码 | 2025 |
| 为 ProductionAction 引入 CapacityActionResource | 复用现有资源类型 | 产能调度中的生产动作具有独特的消耗模式，需要专用资源变体 | 2025 |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始资源上下文模型 | 建立具有三种原型的可消耗资源容量框架 |
| v1.1 | 新增松弛变量支持（超量/欠量） | 支持容量边界的软约束松弛 |
| v1.2 | 列生成集成（addColumns） | 支持批次和产能调度 CG 模式 |
| v1.3 | 存储资源供给/成本平衡 | 建模具有累积语义的库存类资源 |
| v1.4 | 为产能调度引入 CapacityActionResource | 集成生产动作资源消耗 |
| v1.5 | 影子价格管道集成 | 自动化 CG 定价的对偶变量提取 |
