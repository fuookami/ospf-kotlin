# 产能调度限界上下文 领域模型

[toc]

## 一、概述

解决甘特调度框架中的产能调度问题。确定如何将生产动作分配到执行器上的离散化时间槽中，满足产能约束，同时最小化成本。支持三种策略：直接（二维）、有序（三维带二值关联）、迭代/列生成。

### 1. 依赖上下文

1. **甘特调度基础设施**（gantt-scheduling-infrastructure）— TimeSlot、TimeWindow、TimeRange
2. **甘特调度任务域**（gantt-scheduling-domain-task-context）— Executor、SchedulingSolverValueAdapter
3. **OSPF 核心**（ospf-kotlin-core）— LinearMetaModel、LinearExpressionSymbols、UIntVariable、BinVariable
4. **OSPF 数学库**（ospf-kotlin-math）— RealNumber、Flt64、UInt64、LinearMonomial、LinearPolynomial
5. **OSPF 物理量**（ospf-kotlin-quantities）— Quantity、PhysicalUnit

---

## 二、概念/实体

### 1. 生产动作（ProductionAction）

在执行器上产生产能的一种方式，可以是离散批次或连续时长。

**$id_{a}$** ：生产动作 $a$ 的唯一标识。
**$name_{a}$** ：生产动作 $a$ 的名称。
**$displayName_{a}$** ：生产动作 $a$ 的显示名称。
**$executor_{a}$** ：生产动作 $a$ 所属的执行器。
**$discrete_{a}$** ：生产动作 $a$ 是否为离散批次计数模式（否则为时长单位模式），布尔值。
**$batchDuration_{a}$** ：生产动作 $a$ 的批次固定时长，物理量，单位为时间。
**$unitCapacity(a, tw)$** ：生产动作 $a$ 在时间窗口 $tw$ 内的单位产能。
**$unitCost(a, t)$** ：生产动作 $a$ 在时间 $t$ 处的单位成本，从双精度浮点数转换。
**$upperBound(a, s, tw)$** ：生产动作 $a$ 在时间槽 $s$ 和时间窗口 $tw$ 下的上界。
**$unitCapacityQuantity_{a}$** ：生产动作 $a$ 的单位产能物理量。
**$unitCostQuantity_{a}$** ：生产动作 $a$ 的单位成本物理量。

### 2. 产能列（CapacityColumn）

一个执行器在一个时间槽上的完整分配计划。是列生成中的基本列单元。

**$executor_{c}$** ：产能列 $c$ 对应的执行器。
**$slotIndex_{c}$** ：产能列 $c$ 对应的时间槽索引。
**$order_{c}$** ：产能列 $c$ 的顺序位置（有序变体中使用）。
**$allocations_{c}$** ：产能列 $c$ 的分配映射，键为生产动作，值为 UInt64 数量。
**$columnCost_{c}$** ：产能列 $c$ 的总成本。
**$totalAmount_{c}$** ：产能列 $c$ 的总分配数量，由分配映射推导。
**$isEmpty(c)$** ：产能列 $c$ 是否为空（分配映射为空或全为零），布尔谓词。
**$amountFor(c, a)$** ：产能列 $c$ 中生产动作 $a$ 的分配数量。
**$costQuantity(c, u)$** ：产能列 $c$ 在单位 $u$ 下的成本物理量。

### 3. 产能调度方案（CapacitySchedulingSolution）

调度求解的结果实体，包含所有动作分配和执行器产能信息。

**$actions_{sol}$** ：方案中涉及的所有生产动作集合。
**$actionAllocations_{sol}$** ：方案中的动作分配列表，每个元素为 ActionAllocation。
**$executorCapacities_{sol}$** ：方案中各执行器的产能使用结果列表。
**$allocationsBySlot_{sol}$** ：按时间槽分组的分配映射。
**$capacitiesBySlot_{sol}$** ：按时间槽分组的产能使用映射。

### 4. 动作分配（ActionAllocation）

一个生产动作在一个时间槽上的分配记录。

**$action_{alloc}$** ：分配涉及的生产动作。
**$slot_{alloc}$** ：分配涉及的时间槽。
**$slotIndex_{alloc}$** ：分配的时间槽索引。
**$amount_{alloc}$** ：分配的数量。
**$duration_{alloc}$** ：分配的时长。
**$order_{alloc}$** ：分配的顺序位置。
**$durationQuantity(alloc, tw, u)$** ：分配在时间窗口 $tw$ 和单位 $u$ 下的时长物理量。

### 5. 执行器产能结果（ExecutorCapacityResult）

每个执行器在每个时间槽上的产能使用情况。

**$executor_{ecr}$** ：产能结果对应的执行器。
**$slot_{ecr}$** ：产能结果对应的时间槽。
**$slotIndex_{ecr}$** ：产能结果的时间槽索引。
**$totalDuration_{ecr}$** ：该执行器在该时间槽上的总时长。

### 6. 产能调度聚合（CapacitySchedulingAggregation）

生产动作和时间槽的聚合管理，提供全局视图。

**$actions_{agg}$** ：所有生产动作列表。
**$slots_{agg}$** ：所有时间槽列表。
**$timeWindow_{agg}$** ：调度时间窗口。
**$actionsByExecutor_{agg}$** ：按执行器分组的生产动作映射。
**$slotCount_{agg}$** ：时间槽数量，由 $slots_{agg}$ 推导。
**$actionCount_{agg}$** ：生产动作数量，由 $actions_{agg}$ 推导。
**$executorCount_{agg}$** ：执行器数量，由 $actionsByExecutor_{agg}$ 推导。

### 7. 产能列聚合（CapacityColumnAggregation）

跨迭代管理产能列的集合，支持列的添加、移除和清除。

**$columnsIteration_{ca}$** ：按迭代分组的列映射。
**$columns_{ca}$** ：当前所有活跃列的列表。
**$removedColumns_{ca}$** ：已移除列的列表。
**$addColumns(cols)$** ：添加新列集合。
**$removeColumn(col)$** ：移除指定列。
**$clear()$** ：清除所有列。

### 8. 求解器值适配器（SchedulingSolverValueAdapter）

将业务物理量转换为求解器数值类型的适配器接口。

**amountToSolver($v$)** ：将 UInt64 数量转换为求解器值。
**durationToSolver($v$)** ：将时长物理量转换为求解器值。
**costToSolver($v$)** ：将成本物理量转换为求解器值。

---

## 三、变量

### 1. 决策变量

#### 直接编译（CapacityCompilation）

**$x_{a,s}$** ：生产动作 $a$ 在时间槽 $s$ 上的分配数量，非负整数变量，取值范围为 $[0, upperBound(a, s, tw)]$，表示批次/单位数，$\forall a \in A$，$\forall s \in S$。

**$cost$** ：总成本标量，$cost = \sum_{a \in A} \sum_{s \in S} unitCost(a, s) \cdot x_{a,s}$。

#### 有序编译（CapacityOrderCompilation）

**$x_{a,s,o}$** ：生产动作 $a$ 在时间槽 $s$ 的顺序位置 $o$ 上的分配数量，非负整数变量，取值范围为 $[0, upperBound(a, s, tw)]$，表示批次/单位数，$\forall a \in A$，$\forall s \in S$，$\forall o \in O$。

**$b_{a,s,o}$** ：二值变量，表示顺序位置 $o$ 是否被生产动作 $a$ 占用，取值范围为 $\{0, 1\}$，$\forall a \in A$，$\forall s \in S$，$\forall o \in O$。

**$cost$** ：总成本标量，$cost = \sum_{a \in A} \sum_{s \in S} \sum_{o \in O} unitCost(a, s) \cdot x_{a,s,o}$。

#### 迭代编译（IterativeCapacityCompilation）

**$x_{e,i,c}$** ：执行器 $e$ 的第 $i$ 次迭代中列 $c$ 的使用次数，非负整数变量，取值范围为 $[0, columnUpperBound(c)]$，$\forall e \in E$，$\forall i \in I_{iter}$，$\forall c \in C_{e,i}$。

**$cost$** ：总成本标量，$cost = \sum_{e \in E} \sum_{i \in I_{iter}} \sum_{c \in C_{e,i}} columnCost(c) \cdot x_{e,i,c}$。

### 2. 辅助变量

**$operationTime_{a,s}$** ：操作时间表达式，表示生产动作 $a$ 在时间槽 $s$ 上消耗的时长，$operationTime_{a,s} = unitOpTime \cdot x_{a,s}$。

**$capacity_{e,s}$** ：产能表达式，表示执行器 $e$ 在时间槽 $s$ 上的总时长，$capacity_{e,s} = \sum_{a: executor(a)=e} operationTime_{a,s}$。

---

## 四、谓词

### 1. 生产动作类型谓词

**discrete(a)** ：生产动作 $a$ 为离散批次计数模式，即按批次数量计算产能和成本。
**continuous(a)** ：生产动作 $a$ 为连续时长单位模式，即时长由时间窗口间隔决定。

### 2. 列状态谓词

**isEmpty(c)** ：产能列 $c$ 为空，即分配映射为空或所有分配数量均为零。

---

## 五、集合

### 1. 生产动作

**$A$** ：所有生产动作的全集。

**$A_{e}$** ：执行器 $e$ 下的生产动作子集，即 $executor(a) = e$ 的动作，$\forall e \in E$。

### 2. 时间槽

**$S$** ：所有时间槽的全集，由时间窗口离散化得到。

### 3. 执行器

**$E$** ：所有执行器的全集，由生产动作的执行器属性推导。

### 4. 顺序位置

**$O$** ：所有顺序位置的全集，仅在有序变体中使用，$O = \{0, 1, \ldots, maxOrderPerSlot - 1\}$。

### 5. 产能列（迭代变体）

**$C_{e,i}$** ：执行器 $e$ 在第 $i$ 次迭代中的产能列集合。

**$C_{e}^{rem}$** ：执行器 $e$ 已移除的产能列集合。

---

## 六、中间值

### 1. 单位操作时间（Unit Operation Time）

**描述**：每个生产动作的单位操作时间，根据动作是否为离散模式取不同值。

$$
unitOpTime_{a} = \begin{cases}
batchDuration_{a},& \text{discrete}(a) \\ \\
timeWindow.interval,& \neg \text{discrete}(a)
\end{cases}
$$

### 2. 操作时间（Operation Time）

**描述**：生产动作在时间槽上消耗的总时长，为单位操作时间与分配数量的乘积。

$$
operationTime_{a,s} = unitOpTime_{a} \cdot x_{a,s}, \; \forall a \in A, \; \forall s \in S
$$

### 3. 产能（Capacity）

**描述**：每个执行器在每个时间槽上的总时长消耗，为该执行器下所有动作操作时间之和。

$$
capacity_{e,s} = \sum_{a \in A_{e}} operationTime_{a,s}, \; \forall e \in E, \; \forall s \in S
$$

### 4. 成本（Cost）

**描述**：所有分配的总成本，为各动作在各时间槽上的单位成本与分配数量的乘积之和。

$$
cost = \sum_{a \in A} \sum_{s \in S} unitCost(a, s.time.start) \cdot x_{a,s}
$$

### 5. 列上界（Column Upper Bound）

**描述**：一个产能列在给定时间槽中的最大可用次数，为时间槽时长除以列操作时间的向下取整。

$$
columnUpperBound(c) = \lfloor \frac{slotDuration(s)}{columnOperationTime(c)} \rfloor
$$

当列操作时间为零或超出范围时，上界返回 0。

### 6. 总产能值（Total Capacity Value）

**描述**：所有动作在所有时间槽上的理论最大产能总和，用于评估问题规模。

$$
totalCapacityValue = \sum_{a \in A} \sum_{s \in S} unitCapacity(a) \cdot upperBound(a, s)
$$

---

## 七、断言

### 1. 动作索引前置条件

**描述**：生产动作必须在模型注册前完成索引，确保决策变量的下标映射正确。

$$
\forall a \in A \; (indexed(a) \rightarrow registered(a))
$$

### 2. 列数据完整性

**描述**：产能列必须通过数据清洗：时间槽索引在有效范围内，且所有动作属于正确的执行器。

$$
\forall c \in C \; (slotIndex_{c} \in [0, |S|) \wedge \forall a \in allocations_{c} \; (executor_{a} = executor_{c}))
$$

### 3. 列上界安全性

**描述**：当列的操作时间超出时间槽时长或为零时，列上界返回零。

$$
\forall c \in C \; (columnOperationTime(c) \geq slotDuration(s) \vee columnOperationTime(c) = 0 \rightarrow columnUpperBound(c) = 0)
$$

### 4. 顺序约束前置条件

**描述**：顺序约束要求决策变量 $x_{a,t,o}$ 具有有限上界。

$$
\forall a \in A, \forall t \in S, \forall o \in O \; (orderConstraintActive \rightarrow UB(a, t, o) < \infty)
$$

---

## 八、约束

### 1. 执行器产能约束

**[英]**：Executor Capacity Constraint（硬约束）
**描述**：每个执行器在每个时间槽上的总时长消耗不得超过该时间槽的可用时长。

$$
s.t. \quad capacity_{e,s} \leq availableDuration(s), \; \forall e \in E, \; \forall s \in S
$$

### 2. 顺序占用约束 C1

**[英]**：Order Occupation Constraint C1（硬约束，仅有序变体）
**描述**：在每个时间槽的每个顺序位置上，至多有一个生产动作被占用。

$$
s.t. \quad \sum_{a \in A} b_{a,t,o} \leq 1, \; \forall t \in S, \; \forall o \in O
$$

### 3. 顺序关联约束 C2

**[英]**：Order Linking Constraint C2（硬约束，仅有序变体）
**描述**：若顺序位置被占用，则对应的分配数量至少为 1。

$$
s.t. \quad x_{a,t,o} \geq b_{a,t,o}, \; \forall a \in A, \; \forall t \in S, \; \forall o \in O
$$

### 4. 顺序上界约束 C3

**[英]**：Order Upper Bound Constraint C3（硬约束，仅有序变体）
**描述**：若顺序位置未被占用，则对应的分配数量必须为零；若被占用，则分配数量不超过上界。

$$
s.t. \quad x_{a,t,o} \leq UB(a,t,o) \cdot b_{a,t,o}, \; \forall a \in A, \; \forall t \in S, \; \forall o \in O
$$

---

## 九、目标函数

### 1. 产能成本最小化

**描述**：最小化所有生产动作在所有时间槽上的总分配成本。

$$
\min \; cost = \min \sum_{a \in A} \sum_{s \in S} unitCost(a, s.time.start) \cdot x_{a,s}
$$

迭代变体下等价形式：

$$
\min \; cost = \min \sum_{e \in E} \sum_{i \in I_{iter}} \sum_{c \in C_{e,i}} columnCost(c) \cdot x_{e,i,c}
$$

---

## 十、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 列生成（Column Generation） | 框架内置 | 第三章、第六章 | 迭代变体的 RMP 求解与列迭代机制 |
| 直接编译（Direct Compilation） | 框架内置 | 第三章 | 二维变量矩阵的直接建模 |
| 有序编译（Ordered Compilation） | 框架内置 | 第三章 | 三维变量带二值关联的有序建模 |

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 生产动作 | $a$ / ProductionAction | Production Action | 在执行器上产生产能的一种方式，离散或连续 |
| 执行器 | $e$ / Executor | Executor | 执行生产动作的机器/资源 |
| 时间槽 | $s$ / TimeSlot | Time Slot | 离散化的时间区间 |
| 时间窗口 | $tw$ / TimeWindow | Time Window | 整体调度时间范围 |
| 产能列 | $c$ / CapacityColumn | Capacity Column | 一个执行器在一个时间槽上的分配计划 |
| 决策变量(数量) | $x_{a,s}$ / $x_{a,s,o}$ | Decision Variable (amount) | 整数变量：分配的单位/批次数 |
| 决策变量(选择) | $b_{a,s,o}$ | Decision Variable (selection) | 二值变量：顺序位置是否被占用 |
| 操作时间 | $operationTime_{a,s}$ | Operation Time | 动作在时间槽中消耗的时长 |
| 产能 | $capacity_{e,s}$ | Capacity | 每个执行器-时间槽的总时长 |
| 成本 | $cost$ | Cost | 所有分配的总成本 |
| 单位成本 | $unitCost$ | Unit Cost | 每单位/批次的成本 |
| 单位产能 | $unitCapacity$ | Unit Capacity | 每单位产生的产能 |
| 上界 | $upperBound$ | Upper Bound | 每个时间槽的最大单位/批次数 |
| 批次时长 | $batchDuration$ | Batch Duration | 每批次的固定时长（离散模式） |
| 列聚合 | $CapacityColumnAggregation$ | Column Aggregation | 跨迭代管理产能列集合 |
| 迭代 | $iteration$ | Iteration | 列生成的一轮迭代 |
| 顺序位置 | $order$ / $maxOrderPerSlot$ | Order Position | 时间槽内的顺序位置 |
| 求解器值适配器 | $schedulingSolverValueAdapter$ | Solver Value Adapter | 类型转换桥接器 |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 三种编译策略 | 单一编译模型 | 直接（二维）适用于简单场景，有序（三维带二值关联）支持顺序约束，迭代/列生成处理大规模问题 | 2024 |
| 泛型数值类型 V | 固定 Flt64 | 支持 Flt64 和 FltX，解耦业务域与求解器精度 | 2024 |
| 列结构化去重 | 基于内容比较 | 通过协程并行化的结构比较去重，避免语义相同的列重复生成 | 2024 |
| 迭代编译动态变量调整 | 固定变量矩阵 | 迭代过程中动态扩展变量，支持列生成的增量求解 | 2024 |
| 编译与约束/目标分离 | 统一编译模型 | 分离编译逻辑与约束/目标定义，提高可维护性和策略复用 | 2024 |
| 物理量类型别名 | 直接使用求解器类型 | 类型别名提供类型安全的物理量封装，同时保持求解器兼容性 | 2024 |
| 列上界取整策略 | 向上取整 / 直接除法 | 向下取整确保不超过时间槽容量，符合物理约束 | 2024 |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始产能调度模型 | 基础直接编译框架 |
| v1.1 | 新增有序编译策略 | 支持顺序位置约束 |
| v1.2 | 新增迭代/列生成编译 | 支持大规模问题的增量求解 |
| v1.3 | 泛型数值类型 V | 解耦业务域与求解器精度 |
| v1.4 | 列聚合与去重机制 | 管理跨迭代的列生命周期 |
| v1.5 | 物理量类型别名集成 | 类型安全的物理量封装 |
