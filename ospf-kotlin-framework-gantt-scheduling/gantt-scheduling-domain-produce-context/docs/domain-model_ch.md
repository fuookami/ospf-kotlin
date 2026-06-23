# 生产数量限界上下文 领域模型

[toc]

## 一、概述

生产数量上下文负责管理甘特调度中的生产数量域。跟踪计划生产任务的产品产出与原材料消耗，强制执行需求/储备边界及其软偏差松弛，支持直接调度与列生成两种范式。

### 1. 依赖上下文

1. **甘特调度任务域**（gantt-scheduling-domain-task-context）— AbstractTask、AbstractTaskBunch、Executor、AssignmentPolicy、AbstractMaterial
2. **甘特调度产能调度域**（gantt-scheduling-domain-capacity-scheduling-context）— ProductionAction、CapacityColumn、Capacity、IterativeCapacityCompilation
3. **甘特调度束编译域**（gantt-scheduling-domain-bunch-compilation-context）— BunchCompilation
4. **甘特调度基础设施**（gantt-scheduling-infrastructure）— TimeSlot、TimeWindow
5. **OSPF 核心层**（ospf-kotlin-core）— LinearMetaModel、LinearIntermediateSymbols、SlackFunction
6. **OSPF 框架层**（ospf-kotlin-framework）— ShadowPrice、ShadowPriceKey

---

## 二、概念/实体

### 1. 物料（Material）

任何物理物料的抽象基类。子类型包括：产品（Product，成品）、半成品（SemiProduct，中间品）、原材料（RawMaterial，输入物料）。

**$index_{m}$** ：物料 $m$ 的索引标识，整数。

### 2. 物料需求（MaterialDemand）

某产品的产出需求范围，描述要求产出量的上下界及允许的偏差。

**$quantityRangeValue_{d}$** ：需求 $d$ 的数量范围值，区间 $[LB, UB]$，表示产出量的下界与上界。
**$lessQuantityValue_{d}$** ：需求 $d$ 的欠量偏差上限值（可选）。
**$overQuantityValue_{d}$** ：需求 $d$ 的超量偏差上限值（可选）。
**$lessEnabled_{d}$** ：需求 $d$ 是否启用欠量松弛，布尔值，当 $lessQuantityValue \neq \text{null}$ 时为真。
**$overEnabled_{d}$** ：需求 $d$ 是否启用超量松弛，布尔值，当 $overQuantityValue \neq \text{null}$ 时为真。

### 3. 物料储备（MaterialReserves）

某物料的可用输入范围，描述可消耗量的上下界及允许的偏差。

**$quantityRangeValue_{r}$** ：储备 $r$ 的数量范围值，区间 $[LB, UB]$，表示可消耗量的下界与上界。
**$lessQuantityValue_{r}$** ：储备 $r$ 的欠量偏差上限值（可选）。
**$overQuantityValue_{r}$** ：储备 $r$ 的超量偏差上限值（可选）。
**$lessEnabled_{r}$** ：储备 $r$ 是否启用欠量松弛，布尔值，当 $lessQuantityValue \neq \text{null}$ 时为真。
**$overEnabled_{r}$** ：储备 $r$ 是否启用超量松弛，布尔值，当 $overQuantityValue \neq \text{null}$ 时为真。

### 4. 生产任务（ProductionTask）

一个已调度的生产任务，同时产出产品并消耗原材料。

**$index_{t}$** ：任务 $t$ 的索引标识，整数。
**$id_{t}$** ：任务 $t$ 的业务标识。
**$name_{t}$** ：任务 $t$ 的名称。
**$produceQuantityByProduct_{t}(p)$** ：任务 $t$ 对产品 $p$ 的单位产出数量。
**$consumptionQuantityByMaterial_{t}(c)$** ：任务 $t$ 对原材料 $c$ 的单位消耗数量。

### 5. 产量（Produce）

聚合产品产出模型，汇总所有任务的产出并跟踪偏差。

**$products_{P}$** ：产品集合，按索引排序。
**$quantity_{P}(p)$** ：产品 $p$ 的总产出量，连续非负决策变量。
**$overQuantity_{P}(p)$** ：产品 $p$ 的超量偏差，连续非负辅助变量。
**$lessQuantity_{P}(p)$** ：产品 $p$ 的欠量偏差，连续非负辅助变量。

### 6. 消耗量（Consumption）

聚合材料消耗模型，汇总所有任务的消耗并跟踪偏差。

**$materials_{C}$** ：原材料集合，按索引排序。
**$quantity_{C}(c)$** ：原材料 $c$ 的总消耗量，连续非负决策变量。
**$overQuantity_{C}(c)$** ：原材料 $c$ 的超量偏差，连续非负辅助变量。
**$lessQuantity_{C}(c)$** ：原材料 $c$ 的欠量偏差，连续非负辅助变量。

### 7. 产能动作产出（CapacityActionProduce）

每个生产动作的单位产出/消耗率。

**$produce_{a}(p)$** ：动作 $a$ 对产品 $p$ 的单位产出率。
**$consumption_{a}(c)$** ：动作 $a$ 对原材料 $c$ 的单位消耗率。

---

## 三、变量

### 1. 决策变量

**$quantity_{p}^{P}$** ：产品 $p$ 的总产出量，连续非负变量，取值范围为 $[0, +\infty)$，表示所有任务对产品 $p$ 的产出总和，$\forall p \in P$。

**$quantity_{c}^{C}$** ：原材料 $c$ 的总消耗量，连续非负变量，取值范围为 $[0, +\infty)$，表示所有任务对原材料 $c$ 的消耗总和，$\forall c \in C$。

### 2. 辅助变量

**$overQuantity_{p}^{P}$** ：产品 $p$ 的超量松弛变量，连续非负变量，取值范围为 $[0, +\infty)$，度量产出超过需求上界的超出部分，$\forall p \in P$。

**$lessQuantity_{p}^{P}$** ：产品 $p$ 的欠量松弛变量，连续非负变量，取值范围为 $[0, +\infty)$，度量产出低于需求下界的不足部分，$\forall p \in P$。

**$overQuantity_{c}^{C}$** ：原材料 $c$ 的超量松弛变量，连续非负变量，取值范围为 $[0, +\infty)$，度量消耗超过储备上界的超出部分，$\forall c \in C$。

**$lessQuantity_{c}^{C}$** ：原材料 $c$ 的欠量松弛变量，连续非负变量，取值范围为 $[0, +\infty)$，度量消耗低于储备下界的不足部分，$\forall c \in C$。

---

## 四、谓词

### 1. 物料需求谓词

**lessEnabled(d)** ：需求 $d$ 启用欠量松弛，即 $lessQuantityValue_{d} \neq \text{null}$。
**overEnabled(d)** ：需求 $d$ 启用超量松弛，即 $overQuantityValue_{d} \neq \text{null}$。

### 2. 物料储备谓词

**lessEnabled(r)** ：储备 $r$ 启用欠量松弛，即 $lessQuantityValue_{r} \neq \text{null}$。
**overEnabled(r)** ：储备 $r$ 启用超量松弛，即 $overQuantityValue_{r} \neq \text{null}$。

### 3. 产量/消耗量谓词

**overEnabled(P)** ：产量模型 $P$ 启用超量跟踪，取决于对应需求的 overEnabled。
**lessEnabled(P)** ：产量模型 $P$ 启用欠量跟踪，取决于对应需求的 lessEnabled。
**overEnabled(C)** ：消耗量模型 $C$ 启用超量跟踪，取决于对应储备的 overEnabled。
**lessEnabled(C)** ：消耗量模型 $C$ 启用欠量跟踪，取决于对应储备的 lessEnabled。

### 4. 数量非零谓词

**isNonZero(q)** ：数量值 $q$ 非零，即 $q.value \neq 0$。

---

## 五、集合

### 1. 产品

**$P$** ：所有产品的全集。

**$P^{demand}$** ：具有需求约束的产品子集，即存在 MaterialDemand 的产品。
**$P^{nonZero}_{t}$** ：任务 $t$ 中产出数量非零的产品子集，即满足 $isNonZero(produceQuantityByProduct_{t}(p))$ 的产品集合。

### 2. 原材料

**$C$** ：所有原材料的全集。

**$C^{reserves}$** ：具有储备约束的原材料子集，即存在 MaterialReserves 的原材料。
**$C^{nonZero}_{t}$** ：任务 $t$ 中消耗数量非零的原材料子集，即满足 $isNonZero(consumptionQuantityByMaterial_{t}(c))$ 的原材料集合。

---

## 六、中间值

### 1. 求解器下界（solverLowerBound）

**描述**：将需求或储备的范围下界从业务物理量转换为求解器数值。

$$
solverLowerBound(d) = quantityRangeValue_{d}^{LB}.unwrap().toSolverValue()
$$

### 2. 求解器上界（solverUpperBound）

**描述**：将需求或储备的范围上界从业务物理量转换为求解器数值。

$$
solverUpperBound(d) = quantityRangeValue_{d}^{UB}.unwrap().toSolverValue()
$$

### 3. 求解器欠量（solverLessQuantity）

**描述**：将需求或储备的欠量偏差上限从业务物理量转换为求解器数值，若未设置则为 0。

$$
solverLessQuantity(d) = \begin{cases}
lessQuantityValue_{d}.toSolverValue(),& lessQuantityValue_{d} \neq \text{null} \\ \\
0,& lessQuantityValue_{d} = \text{null}
\end{cases}
$$

### 4. 求解器超量（solverOverQuantity）

**描述**：将需求或储备的超量偏差上限从业务物理量转换为求解器数值，若未设置则为 0。

$$
solverOverQuantity(d) = \begin{cases}
overQuantityValue_{d}.toSolverValue(),& overQuantityValue_{d} \neq \text{null} \\ \\
0,& overQuantityValue_{d} = \text{null}
\end{cases}
$$

### 5. 求解器范围下界（solverRangeLowerBound）

**描述**：考虑欠量偏差后的有效范围下界。

$$
solverRangeLowerBound(d) = solverLowerBound(d) - solverLessQuantity(d)
$$

### 6. 求解器范围上界（solverRangeUpperBound）

**描述**：考虑超量偏差后的有效范围上界。

$$
solverRangeUpperBound(d) = solverUpperBound(d) + solverOverQuantity(d)
$$

### 7. 束产出（bunch.produce）

**描述**：一个任务束对某产品 $p$ 的总产出量，等于束内所有任务对该产品产出量之和。

$$
bunch.produce(p) = \sum_{t \in bunch.tasks} produceQuantityByProduct_{t}(p).value, \; \forall p \in P
$$

### 8. 束消耗（bunch.consumption）

**描述**：一个任务束对某原材料 $c$ 的总消耗量，等于束内所有任务对该原材料消耗量之和。

$$
bunch.consumption(c) = \sum_{t \in bunch.tasks} consumptionQuantityByMaterial_{t}(c).value, \; \forall c \in C
$$

### 9. 产能列产出（CapacityColumn.produce）

**描述**：产能列对某产品 $p$ 在给定分配量下的总产出，等于各分配动作的单位产出率与分配量之积的累加和。

$$
CapacityColumn.produce(p, amountValue) = \sum_{(a, alloc) \in allocations} unitProduce(a, p) \cdot amountValue(alloc), \; \forall p \in P
$$

### 10. 产能列消耗（CapacityColumn.consumption）

**描述**：产能列对某原材料 $c$ 在给定分配量下的总消耗，等于各分配动作的单位消耗率与分配量之积的累加和。

$$
CapacityColumn.consumption(c, amountValue) = \sum_{(a, alloc) \in allocations} unitConsumption(a, c) \cdot amountValue(alloc), \; \forall c \in C
$$

---

## 七、断言

### 1. 产品索引排序

**描述**：在产量模型（Produce）构建时，产品集合 $P$ 必须按索引升序排列。

$$
\forall i, j \in P \; (i < j \rightarrow index(P_i) \leq index(P_j))
$$

### 2. 原材料索引排序

**描述**：在消耗量模型（Consumption）构建时，原材料集合 $C$ 必须按索引升序排列。

$$
\forall i, j \in C \; (i < j \rightarrow index(C_i) \leq index(C_j))
$$

### 3. 束非空断言

**描述**：在 addColumns 操作中，任务束（Bunch）不能为空，必须包含至少一个任务。

$$
|bunch.tasks| \geq 1
$$

### 4. 数量零值前提

**描述**：quantityZero() 方法要求至少存在一个 ProductionTask 且其数量值可用。

$$
\exists t \in Tasks \; (produceQuantityByProduct_{t}(p).value \neq \text{null})
$$

---

## 八、约束

### 1. 产出上界约束

**[英]**：Produce Upper Bound Constraint
**描述**：每个有需求约束的产品，其总产出量不得超过需求范围的上界。当启用超量松弛时，使用超量符号替代硬上界。

$$
s.t. \quad quantity_{p}^{P} \leq solverUpperBound(demand_{p}), \; \forall p \in P^{demand}
$$

**推论**：当超量松弛启用时，超量变量受约束：

$$
s.t. \quad overQuantity_{p}^{P} \cdot polyX \leq solverUpperBound(demand_{p}), \; \forall p \in P^{demand} \mid overEnabled(demand_{p})
$$

### 2. 产出下界约束

**[英]**：Produce Lower Bound Constraint
**描述**：每个有需求约束的产品，其总产出量不得低于需求范围的下界。当启用欠量松弛时，使用欠量符号替代硬下界。

$$
s.t. \quad quantity_{p}^{P} \geq solverLowerBound(demand_{p}), \; \forall p \in P^{demand}
$$

**推论**：当欠量松弛启用时，欠量变量受约束：

$$
s.t. \quad lessQuantity_{p}^{P} \cdot polyX \geq solverLowerBound(demand_{p}), \; \forall p \in P^{demand} \mid lessEnabled(demand_{p})
$$

### 3. 消耗上界约束

**[英]**：Consumption Upper Bound Constraint
**描述**：每个有储备约束的原材料，其总消耗量不得超过储备范围的上界。

$$
s.t. \quad quantity_{c}^{C} \leq solverUpperBound(reserves_{c}), \; \forall c \in C^{reserves}
$$

### 4. 消耗下界约束

**[英]**：Consumption Lower Bound Constraint
**描述**：每个有储备约束的原材料，其总消耗量不得低于储备范围的下界。

$$
s.t. \quad quantity_{c}^{C} \geq solverLowerBound(reserves_{c}), \; \forall c \in C^{reserves}
$$

### 5. 超量上限约束

**[英]**：Over-Quantity Cap Constraint
**描述**：每个产品的超量偏差不得超过需求允许的超量上限。

$$
s.t. \quad overQuantity_{p}^{P} \leq solverOverQuantity(demand_{p}), \; \forall p \in P^{demand}
$$

### 6. 欠量上限约束

**[英]**：Less-Quantity Cap Constraint
**描述**：每个产品的欠量偏差不得超过需求允许的欠量上限（取负值表示下界方向）。

$$
s.t. \quad lessQuantity_{p}^{P} \geq -solverLessQuantity(demand_{p}), \; \forall p \in P^{demand}
$$

---

## 九、目标函数

### 1. 产出量最小化

**描述**：最小化所有产品的总产出量，按系数加权。

$$
\min \sum_{p \in P} coeff(p) \cdot quantity_{p}^{P}
$$

### 2. 产出量最大化

**描述**：最大化所有产品的总产出量，按系数加权。

$$
\max \sum_{p \in P} coeff(p) \cdot quantity_{p}^{P}
$$

### 3. 产出欠量最小化

**描述**：最小化所有产品的欠量偏差之和，按系数加权。

$$
\min \sum_{p \in P} coeff(p) \cdot lessQuantity_{p}^{P}
$$

### 4. 产出超量最小化

**描述**：最小化所有产品的超量偏差之和，按系数加权。

$$
\min \sum_{p \in P} coeff(p) \cdot overQuantity_{p}^{P}
$$

### 5. 消耗量最小化

**描述**：最小化所有原材料的总消耗量，按系数加权。

$$
\min \sum_{c \in C} coeff(c) \cdot quantity_{c}^{C}
$$

### 6. 消耗量最大化

**描述**：最大化所有原材料的总消耗量，按系数加权。

$$
\max \sum_{c \in C} coeff(c) \cdot quantity_{c}^{C}
$$

### 7. 消耗欠量最小化

**描述**：最小化所有原材料的欠量偏差之和，按系数加权。

$$
\min \sum_{c \in C} coeff(c) \cdot lessQuantity_{c}^{C}
$$

### 8. 消耗超量最小化

**描述**：最小化所有原材料的超量偏差之和，按系数加权。

$$
\min \sum_{c \in C} coeff(c) \cdot overQuantity_{c}^{C}
$$

> 所有目标函数均支持可选的阈值（threshold）参数，用于控制二次松弛变量的启用条件。

---

## 十、算法引用

| 算法名称 | 文件路径 | 引用位置 | 简要说明 |
|----------|----------|----------|----------|
| 松弛函数（SlackFunction） | 框架内置（ospf-kotlin-core） | 第八章约束、第九章目标函数 | 创建正/负偏差松弛变量 |
| 列生成（Column Generation） | 框架内置 | 第六章中间值 | RMP 求解与列迭代机制 |
| 影子价格管道（ShadowPrice Pipeline） | 框架内置（ospf-kotlin-framework） | 第九章目标函数 | CG 定价中的对偶变量传递架构 |

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 产品 | $P$ / $p$ | Product | 系统产出的成品 |
| 半成品 | - | Semi-Product | 中间物料 |
| 原材料 | $C$ / $c$ | Raw Material | 被消耗的输入物料 |
| 物料 | $M$ | Material | 任何物理物料 |
| 物料需求 | MaterialDemand | Material Demand | 产品产出的范围要求 $[LB, UB]$，含偏差上限 |
| 物料储备 | MaterialReserves | Material Reserves | 物料可用输入的范围 $[LB, UB]$，含偏差上限 |
| 生产任务 | ProductionTask | Production Task | 产出产品并消耗物料的已调度任务 |
| 产量 | Produce | Produce | 聚合产品产出模型，含偏差跟踪 |
| 消耗量 | Consumption | Consumption | 聚合材料消耗模型，含偏差跟踪 |
| 数量 | quantity | Quantity | 总产出/消耗量 |
| 超量 | overQuantity | Over-Quantity | 超出上界的松弛量 |
| 欠量 | lessQuantity | Less-Quantity | 低于下界的松弛量 |
| 影子价格 | ShadowPrice | Shadow Price | 列生成定价中的对偶变量 |
| 松弛函数 | SlackFunction | Slack Function | 创建正/负偏差变量的函数 |
| 流水线 | Pipeline | Pipeline | 模型中的约束或目标函数 |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 双模式设计：TaskScheduling（存根）与 BunchScheduling（完整实现） | 统一模式 | TaskScheduling 为简单场景预留，BunchScheduling 支持列生成迭代 | 2024 |
| 产能调度层次：PlanCapacitySchedulingProduce（即时）vs BunchCapacitySchedulingProduce（惰性迭代） | 单一策略 | 即时模式适用于直接调度，惰性迭代模式适用于 CG 场景 | 2024 |
| 泛型数值类型 V | 固定 Flt64 | 泛型支持 Flt64 和 FltX，解耦业务域与求解器精度 | 2024 |
| 松弛变量惰性初始化 | 预分配所有松弛变量 | 仅在约束启用时创建松弛变量，减少模型规模 | 2024 |
| 影子价格管道架构 | 直接传递对偶值 | Pipeline 模式统一约束与目标的定价通道，支持 CG 迭代 | 2024 |
| 基于阈值的二次松弛 | 无阈值直接松弛 | 阈值控制次要松弛的启用条件，避免不必要的变量膨胀 | 2024 |
| 不可变物料列表按索引排序 | 无序列表 | 排序保证确定性遍历顺序，支持二分查找等高效操作 | 2024 |
| 束列重建策略：每次迭代清除并重建 | 增量更新 | 清除重建避免增量维护的复杂性，保证迭代一致性 | 2024 |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始生产数量模型 | 基础产出/消耗跟踪 |
| v1.2 | 新增 MaterialDemand/MaterialReserves 偏差松弛 | 支持软约束边界 |
| v1.3 | 新增 CapacityActionProduce 产能动作模型 | 支持产能列的单位产出/消耗率 |
| v1.4 | 新增列生成支持与影子价格管道 | 支持 CG 范式下的定价与迭代 |
| v1.5 | 泛型数值类型 V 重构 | 解耦求解器精度，支持 FltX |
| v1.6 | 阈值二次松弛与惰性初始化 | 优化模型规模与松弛控制 |
