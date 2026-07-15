# 批次编排限界上下文 领域模型

[toc]

## 一、概述

批次编排上下文实现甘特调度问题的批次级列生成编排层。任务组（"批次"）通过列生成迭代生成并编译为 LP 模型。每个批次表示多个任务在一个执行器上的可行分配方案。该上下文管理批次列的完整生命周期：注册、添加、移除、固定、影子价格提取和解分析。

### 1. 依赖上下文

1. **甘特调度基础设施层**（gantt-scheduling-infrastructure）— TimeWindow、TimeSlot、TimeRange
2. **甘特调度任务域**（gantt-scheduling-domain-task-context）— AbstractTask、AbstractTaskBunch、Executor、AssignmentPolicy
3. **甘特调度任务编排**（gantt-scheduling-domain-task-compilation-context）— Compilation、TaskSolution、TaskTimeImpl、BunchSchedulingTaskTime
4. **甘特调度产能调度**（gantt-scheduling-domain-capacity-scheduling-context）— ProductionAction、ActionAllocation、CapacityCompilation、CapacityColumn、Capacity
5. **OSP 框架核心**（ospf-kotlin-core）— MetaModel、BinVariable、LinearIntermediateSymbols、SlackFunction
6. **OSP 框架**（ospf-kotlin-framework）— ShadowPrice、ShadowPriceKey

---

## 二、概念/实体

### 1. 批次（Bunch, B）

分配给单个执行器的一组任务，是列生成中的一列。每个批次表示一个执行器在多个任务上的可行调度方案。

**$tasks_{b}$** ：批次 $b$ 包含的任务集合。
**$executor_{b}$** ：批次 $b$ 分配的执行器。
**$cost_{b}$** ：批次 $b$ 的成本，用于目标函数。
**$time_{b}$** ：批次 $b$ 的时间信息，包含每个任务的开始和结束时间。

### 2. 基于时间槽的批次（SlotBasedBunch）

扩展 Bunch，增加时间槽感知，将每个批次绑定到恰好一个离散时间区间。

**$slot_{b}$** ：批次 $b$ 所属的时间槽。
**$slotIndex_{b}$** ：时间槽在槽序列中的索引。

### 3. 批次聚合（BunchAggregation\<B, V, T, E, A\>）

管理列生成迭代过程中批次生命周期的容器。处理注册、去重、移除和迭代跟踪。

**$bunchesIteration_{a}$** ：从迭代编号到该迭代新增批次集合的映射。
**$bunches_{a}$** ：跨所有迭代累积的所有批次集合。
**$removedBunches_{a}$** ：在列管理过程中被移除的批次集合。
**$lastIterationBunches_{a}$** ：最近一次迭代新增的批次集合。

### 4. 批次编排（BunchCompilation\<B, V, T, E, A\>）

基于批次的列生成调度 LP 模型结构。包含主问题的决策变量、约束和中间符号。

**$tasks_{c}$** ：编排中的所有任务集合。
**$executors_{c}$** ：编排中的所有执行器集合。
**$lockCancelTasks_{c}$** ：强制取消的任务集合（锁定 $y[t] = 1$）。
**$withExecutorLeisure_{c}$** ：是否启用执行器空闲变量（$z[e]$）。
**$taskCancelEnabled_{c}$** ：是否启用任务取消变量（$y[t]$）。
**$aggregation_{c}$** ：管理批次生命周期的批次聚合容器。
**$x_{c}$** ：每轮迭代的批次选择二值决策变量。
**$y_{c}$** ：任务取消二值决策变量。
**$z_{c}$** ：执行器空闲二值决策变量。
**$bunchCost_{c}$** ：表示总批次成本的中间符号。
**$taskAssignment_{c}$** ：任务-执行器分配的中间符号矩阵。
**$taskCompilation_{c}$** ：任务覆盖的中间符号（批次 + 取消）。
**$executorCompilation_{c}$** ：执行器覆盖的中间符号（批次 + 空闲）。

### 5. 基于时间槽的批次聚合（SlotBasedBunchAggregation）

扩展 BunchAggregation，增加基于时间槽的去重，确保批次在其时间槽上下文中进行比较。

### 6. 基于时间槽的批次编排（SlotBasedBunchCompilation）

扩展 BunchCompilation，增加基于时间槽的结构，按时间槽组织批次和变量。

**$slots_{c}$** ：编排中的时间槽集合。
**$bunchesBySlot_{c}$** ：从时间槽到该槽中批次集合的映射。
**$xBySlot_{c}$** ：从时间槽到该槽中批次决策变量的映射。

### 7. 批次解（BunchSolution\<B, V, T, E, A\>）

求解 LP 模型后提取的解实体，包含选中的批次、取消的任务和汇总统计。

**$bunches_{s}$** ：解中选中的批次集合（$x[b] > 0$）。
**$canceledTasks_{s}$** ：解中被取消的任务集合（$y[t] > 0$）。
**$summary_{s}$** ：包含聚合统计信息的解汇总。

### 8. 批次解汇总（BunchSolutionSummary）

批次调度解的聚合统计信息。

**$bunchCount_{s}$** ：解中选中的批次数。
**$assignedTaskCount_{s}$** ：分配给执行器的任务数。
**$canceledTaskCount_{s}$** ：被取消的任务数。
**$totalTaskCount_{s}$** ：总任务数（已分配 + 已取消）。

### 9. 基于时间槽的产能结果（SlotBasedCapacityResult\<A, M, R, V\>）

单个时间槽的产能预求解结果，提供产量/消耗量/资源使用量的边界。

**$slot_{r}$** ：该结果适用的时间槽。
**$slotIndex_{r}$** ：时间槽的索引。
**$actionAllocations_{r}$** ：产能预求解确定的行动分配集合。
**$totalCostQuantityValue_{r}$** ：该时间槽的总成本量值。
**$produceQuantityByProduct_{r}$** ：从产品到该时间槽产量的映射。
**$consumptionQuantityByMaterial_{r}$** ：从物料到该时间槽消耗量的映射。
**$resourceUsageQuantityByResource_{r}$** ：从资源到该时间槽使用量的映射。

### 10. 时间槽约束（SlotConstraints\<M, R, V\>）

每个时间槽上产量、消耗量和资源使用量的上下界，由产能预求解导出。

**$maxProduceQuantity_{sc}(m)$** ：该时间槽中产品 $m$ 的最大产量。
**$minProduceQuantity_{sc}(m)$** ：该时间槽中产品 $m$ 的最小产量。
**$maxConsumptionQuantity_{sc}(m)$** ：该时间槽中物料 $m$ 的最大消耗量。
**$minConsumptionQuantity_{sc}(m)$** ：该时间槽中物料 $m$ 的最小消耗量。
**$maxResourceUsageQuantity_{sc}(r)$** ：该时间槽中资源 $r$ 的最大使用量。
**$minResourceUsageQuantity_{sc}(r)$** ：该时间槽中资源 $r$ 的最小使用量。

### 11. 任务反转（TaskReverse\<T, E, A\>）

可以交换时间顺序的任务对，用作邻域搜索移动，以探索替代调度序列。

**$symmetricalPairs_{tr}$** ：两个任务共享同一执行器的对称可逆任务对集合。
**$leftMapper_{tr}$** ：从可逆对到左侧（前驱）任务的映射。
**$rightMapper_{tr}$** ：从可逆对到右侧（后继）任务的映射。

### 12. 任务反转.可逆对（TaskReverse.ReversiblePair）

时间顺序可以反转的一对任务。

**$prevTask_{rp}$** ：当前在调度中排在前面的任务。
**$succTask_{rp}$** ：当前在调度中排在后面的任务。
**$symmetrical_{rp}$** ：该对中的两个任务是否共享同一执行器。

### 13. 批次调度任务时间（BunchSchedulingTaskTime）

扩展 TaskTimeImpl，增加列生成特有的时间估计能力。

**$compilation_{tt}$** ：该任务时间所属的批次编排。
**$redundancyRange_{tt}$** ：开始时间估计的允许偏差范围。
**$estRedundancy_{tt}$** ：开始时间估计的冗余变量，取值范围为 $[-redundancy, +redundancy]$。
**$estSlack_{tt}$** ：估计开始时间与计划开始时间之间的松弛量。
**$estimateStartTime_{tt}$** ：任务的估计开始时间，由冗余和加权批次开始时间计算得出。
**$estimateEndTime_{tt}$** ：任务的估计结束时间，由冗余和加权批次结束时间计算得出。

---

## 三、变量

### 1. 决策变量

**$x_{i}[b]$** ：第 $i$ 次迭代中批次 $b$ 的二值选择变量，无量纲量，取值范围为 $\{0, 1\}$，表示批次 $b$ 在第 $i$ 次迭代的解中是否被选中，$\forall b \in B_{i}$。

**$y[t]$** ：任务 $t$ 的二值取消变量，无量纲量，取值范围为 $\{0, 1\}$，表示任务 $t$ 是否被取消（未分配给任何执行器），$\forall t \in T$。对于 $t \in T^{lockCancel}$，锁定为 $1$。

**$z[e]$** ：执行器 $e$ 的二值空闲变量，无量纲量，取值范围为 $\{0, 1\}$，表示执行器 $e$ 是否空闲（未分配任何批次），$\forall e \in E$。仅在 $withExecutorLeisure = true$ 时存在。

### 2. 辅助变量

**$estRedundancy[t]$** ：任务 $t$ 的开始时间冗余变量，实值，取值范围为 $[-redundancy, +redundancy]$，表示相对于加权平均开始时间的允许偏差，$\forall t \in T$。

---

## 四、谓词

### 1. 基于时间槽的谓词

**SlotBasedBunch(b)** ：批次 $b$ 属于某个时间槽，即 $b$ 具有关联的 $slot_{b}$ 和 $slotIndex_{b}$。

### 2. 去重谓词

**sameColumnAs(b1, b2)** ：批次 $b_1$ 和 $b_2$ 是重复的，即它们具有相同的任务集合、执行器和（如果是基于时间槽的）时间槽。

### 3. 任务反转谓词

**reverseEnabled(t1, t2)** ：任务 $t_1$ 和 $t_2$ 可以交换时间顺序，即该对是有效的邻域移动。

**symmetrical(rp)** ：可逆对 $rp$ 是对称的，即 $prevTask_{rp}$ 和 $succTask_{rp}$ 两个任务共享同一执行器。

---

## 五、集合

### 1. 任务

**$T$** ：所有任务的全集。

**$T^{lockCancel}$** ：强制取消的任务，即锁定 $y[t] = 1$ 的任务。

### 2. 执行器

**$E$** ：所有执行器的全集。

**$E^{hidden}$** ：满足 $z[e] > 0$ 的执行器子集，即当前解中空闲的执行器。

### 3. 批次

**$B$** ：跨所有迭代累积的所有批次全集。

**$B_{i}$** ：第 $i$ 次迭代新增的批次，即 $B_{i} = bunchesIteration(i)$。
**$B^{removed}$** ：在列管理过程中被移除的批次，即 $B^{removed} = removedBunches$。
**$B^{fixed}$** ：固定为 $x[b] = 1$ 的批次，即锁定进入解的批次。
**$B^{kept}$** ：在当前解中具有正 LP 值（$x[b] > 0$）的批次。

### 4. 时间槽

**$S$** ：所有时间槽的全集。

**$B_{s}$** ：属于时间槽 $s$ 的批次，即 $B_{s} = \{b \in B \mid slot_{b} = s\}$。

---

## 六、中间值

### 1. 批次成本（BunchCost）

**描述**：所有选中批次的总成本，由各批次成本乘以其选择变量的加权和构成。

$$
bunchCost = \sum_{b \in B} cost(b) \cdot x[b]
$$

### 2. 任务分配（TaskAssignment）

**描述**：对于每个任务-执行器对，包含该任务且分配给该执行器的所有批次的选择变量之和。

$$
taskAssignment[t, e] = \sum_{\substack{b \in B \\ t \in tasks(b) \\ executor(b) = e}} x[b], \; \forall t \in T, \forall e \in E
$$

### 3. 任务编排（TaskCompilation）

**描述**：对于每个任务，取消变量加上包含该任务的所有批次的选择变量之和。此值必须等于 1 以保证正确覆盖。

$$
taskCompilation[t] = y[t] + \sum_{\substack{b \in B \\ t \in tasks(b)}} x[b], \; \forall t \in T
$$

### 4. 执行器编排（ExecutorCompilation）

**描述**：对于每个执行器，空闲变量加上分配给该执行器的所有批次的选择变量之和。此值必须等于 1 以保证正确覆盖。

$$
executorCompilation[e] = z[e] + \sum_{\substack{b \in B \\ executor(b) = e}} x[b], \; \forall e \in E
$$

### 5. 预估开始时间（EstimateStartTime）

**描述**：任务的估计开始时间，由冗余偏移量加上包含该任务的所有批次的开始时间加权和计算得出。

$$
estimateStartTime[t] = estRedundancy[t] + \sum_{\substack{b \in B \\ t \in tasks(b)}} time.start(b, t) \cdot x[b], \; \forall t \in T
$$

### 6. 预估结束时间（EstimateEndTime）

**描述**：任务的估计结束时间，由冗余偏移量加上包含该任务的所有批次的结束时间加权和计算得出。

$$
estimateEndTime[t] = estRedundancy[t] + \sum_{\substack{b \in B \\ t \in tasks(b)}} time.end(b, t) \cdot x[b], \; \forall t \in T
$$

### 7. 预估松弛（EstSlack）

**描述**：估计开始时间与计划开始时间之间的松弛量，通过 SlackFunction 计算。其定义另参考《SlackFunction》。

$$
estSlack[t] = Slack(estimateStartTime[t], scheduled\_start[t]), \; \forall t \in T
$$

### 8. 下一轮约简成本截断值（NextReducedCostCutoff）

**描述**：渐进式列移除的约简成本截断值，取最大约简成本的三分之二与最小阈值 5.0 中的较大者。

$$
nextReducedCostCutoff(max) = \max(\lfloor maxReducedCost \rfloor \times \frac{2}{3}, \; 5.0)
$$

---

## 七、断言

### 1. 已移除批次排除

**描述**：固定批次时，该批次不能在已移除批次集合中。已移除的批次不能被固定。

$$
\forall b \in B \; (fix(b) \rightarrow b \notin B^{removed})
$$

### 2. 编排初始化完整性

**描述**：BunchCompilation 初始化时，所有执行器和任务必须被索引。集合 $T$ 和 $E$ 必须在 LP 构建前完全枚举。

$$
|tasks_{c}| = |T| \wedge |executors_{c}| = |E|
$$

### 3. 任务反转有效性

**描述**：对于考虑反转的每个任务对，reverseEnabled 谓词必须成立。只有有效的对才能添加到 TaskReverse 结构中。

$$
\forall (t_1, t_2) \in TaskReverse \; (reverseEnabled(t_1, t_2))
$$

### 4. 非空列添加

**描述**：通过 BunchSchedulingTaskTime 添加列时，bunches 集合必须非空。添加零列是错误的。

$$
|bunches| > 0
$$

---

## 八、约束

### 1. 任务取消变量范围约束

**[英]**：Task Cancellation Variable Range Constraint
**描述**：对于锁定取消集合中的任务，取消变量锁定为 1。对于所有其他任务，取消变量为二值 $[0, 1]$。

$$
s.t. \quad y[t] = 1, \; \forall t \in T^{lockCancel}
$$

$$
s.t. \quad y[t] \in \{0, 1\}, \; \forall t \in T \setminus T^{lockCancel}
$$

### 2. 批次选择变量范围约束

**[英]**：Bunch Selection Variable Range Constraint
**描述**：每个批次选择变量为二值，指示该批次是否在解中被选中。

$$
s.t. \quad x[b] \in \{0, 1\}, \; \forall b \in B
$$

### 3. 执行器空闲变量范围约束

**[英]**：Executor Leisure Variable Range Constraint
**描述**：每个执行器空闲变量为二值，指示该执行器是否空闲。仅在启用执行器空闲时存在。

$$
s.t. \quad z[e] \in \{0, 1\}, \; \forall e \in E \mid withExecutorLeisure
$$

### 4. 时间槽产量上下界约束

**[英]**：Slot Production Bounds Constraint
**描述**：每个时间槽中每种产品的产量必须在产能预求解确定的边界范围内。

$$
s.t. \quad minProduceQuantity_{s}(m) \leq produce_{s}(m) \leq maxProduceQuantity_{s}(m), \; \forall s \in S, \forall m \in M
$$

### 5. 时间槽消耗量上下界约束

**[英]**：Slot Consumption Bounds Constraint
**描述**：每个时间槽中每种物料的消耗量必须在产能预求解确定的边界范围内。

$$
s.t. \quad minConsumptionQuantity_{s}(m) \leq consumption_{s}(m) \leq maxConsumptionQuantity_{s}(m), \; \forall s \in S, \forall m \in M
$$

### 6. 时间槽资源使用量上下界约束

**[英]**：Slot Resource Usage Bounds Constraint
**描述**：每个时间槽中每种资源的使用量必须在产能预求解确定的边界范围内。

$$
s.t. \quad minResourceUsageQuantity_{s}(r) \leq resourceUsage_{s}(r) \leq maxResourceUsageQuantity_{s}(r), \; \forall s \in S, \forall r \in R
$$

---

## 九、目标函数

### 1. 批次成本最小化

**描述**：最小化所有选中批次的总成本，引导求解器选择成本更低的任务-执行器分配方案。

$$
\min \; bunchCost = \sum_{b \in B} cost(b) \cdot x[b]
$$

---

## 十、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 列生成（Column Generation） | 框架内置 | 第三章、第六章、第九章 | 迭代批次生成与 RMP 求解 |
| 松弛函数（SlackFunction） | 框架内置 | 第六章预估松弛 | 计算估计开始时间与计划开始时间之间的松弛量 |
| 产能预求解（Capacity Pre-Solve） | gantt-scheduling-domain-capacity-scheduling-context | 第八章时间槽约束 | 确定每时间槽的产量/消耗量/资源使用量边界 |
| 任务反转（TaskReverse） | 内置 | 第二章 TaskReverse | 交换任务对时间顺序的邻域搜索移动 |

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 任务 | $t$ / T | Task | 原子可调度单元 |
| 执行器 | $e$ / E | Executor | 执行任务的资源 |
| 分配策略 | $A$ / Assignment Policy | Assignment Policy | 任务分配规则 |
| 批次 | $b$ / Bunch | Bunch | 分配给一个执行器的任务组；CG 中的一列 |
| 批次迭代 | $B_i$ / Bunch Iteration | Bunch Iteration | 每次 CG 迭代新增的批次 |
| 编排 | BunchCompilation | Compilation | 批次调度的 LP 模型结构 |
| 聚合 | BunchAggregation | Aggregation | 管理批次生命周期的容器 |
| 列选择变量 | $x_i[b]$ / Column | Column | 二值变量：批次 b 是否被选中？ |
| 取消变量 | $y[t]$ / Cancel Variable | Cancel Variable | 二值变量：任务 t 是否被取消？ |
| 空闲变量 | $z[e]$ / Leisure Variable | Leisure Variable | 二值变量：执行器 e 是否空闲？ |
| 批次成本 | bunchCost | Bunch Cost | 选中批次的成本之和 |
| 任务编排 | $taskCompilation[t]$ | Task Compilation | 覆盖任务 t 的批次 + 取消 |
| 执行器编排 | $executorCompilation[e]$ | Executor Compilation | 执行器 e 上的空闲 + 批次 |
| 任务分配 | $taskAssignment[t,e]$ | Task Assignment | 任务 t 在执行器 e 上的批次 |
| 影子价格 | ShadowPrice | Shadow Price | 对偶值，用于定价 |
| 约简成本 | Reduced Cost | Reduced Cost | 成本减去对偶贡献 |
| 固定批次 | $B^{fixed}$ / Fixed Bunch | Fixed Bunch | 锁定为 x=1 的批次 |
| 保留批次 | $B^{kept}$ / Kept Bunch | Kept Bunch | 具有正 LP 值的批次 |
| 隐藏执行器 | $E^{hidden}$ / Hidden Executor | Hidden Executor | z > 0 的执行器 |
| 时间槽 | $s$ / TimeSlot | Slot | 离散时间区间 |
| 容量预求解 | SlotBasedCapacityPreSolver | Capacity Pre-Solve | 获取时间槽级中间值 |
| 时间槽约束 | SlotConstraints | Slot Constraints | 每时间槽的边界 |
| 任务反转 | TaskReverse | Task Reverse | 可交换时间顺序的任务对 |
| 对称对 | symmetrical / Symmetrical Pair | Symmetrical Pair | 同一执行器上的可逆对 |
| 预估开始时间 | $estimateStartTime[t]$ | Estimate Start Time | 冗余 + 加权开始时间 |
| 预估结束时间 | $estimateEndTime[t]$ | Estimate End Time | 冗余 + 加权结束时间 |
| 最大完工时间 | Makespan | Makespan | 最大完成时间 |
| 冗余范围 | $redundancyRange$ | Redundancy Range | 时间估计的允许偏差 |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 列生成架构 | 直接 MILP / 分解 | Dantzig-Wolfe 分解高效处理大规模批次空间；主问题通过定价子问题迭代求解 | 2024 |
| 两级聚合 | 单一聚合 | BunchAggregation 负责存储/去重，AbstractBunchCompilationAggregation 负责 LP 管理；职责分离 | 2024 |
| 基于时间槽的扩展 | 无时间槽的批次模型 | 批次属于恰好一个时间槽；支持每时间槽的产能约束和并行求解 | 2024 |
| 产能预求解 | 内联产能约束 | 在主 CG 循环前通过独立 LP 确定时间槽级边界；降低主问题复杂度 | 2024 |
| 渐进式列移除 | 全部移除 / 不移除 | 2/3 因子与最小阈值 5；平衡模型大小和解质量 | 2024 |
| 局部固定策略 | 不固定 / 全局固定 | 局部固定最优批次；回退到单个最优；减少分支空间 | 2024 |
| Flt64 求解器边界 | 域级求解器类型 | 域类型在求解后转换为 Flt64；将业务逻辑与求解器数值解耦 | 2024 |
| 任务反转移建模 | 固定顺序 | 邻域搜索移动交换任务对的时间顺序；探索替代调度方案 | 2024 |
| 数据对象分析器 | 可变解对象 | 从数据对象进行无状态解重建；线程安全且可重现 | 2024 |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始批次编排模型 | 甘特调度的基础列生成框架 |
| v1.1 | 新增基于时间槽的扩展 | 支持时间槽感知调度与每时间槽产能约束 |
| v1.2 | 新增产能预求解集成 | 通过独立 LP 确定时间槽级边界，降低主问题复杂度 |
| v1.3 | 新增任务反转移邻域移动 | 通过交换任务对时间顺序探索替代调度方案 |
| v1.4 | 新增渐进式列移除 | 控制迭代过程中的模型规模增长 |
| v1.5 | 新增执行器空闲变量 | 显式建模空闲执行器，改善资源利用分析 |
