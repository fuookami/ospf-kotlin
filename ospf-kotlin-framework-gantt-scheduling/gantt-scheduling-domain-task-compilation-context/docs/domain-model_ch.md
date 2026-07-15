# 任务编排上下文 领域模型

[toc]

## 一、概述

任务编排上下文负责解决甘特调度框架中任务到执行器的分配与调度问题。通过二元决策变量建模任务-执行器分配关系，建模时间约束（延迟、提前、切换成本），支持列生成，并提供目标函数组件。

### 1. 依赖上下文

1. gantt-scheduling-domain-task-context（任务模型：AbstractTask、Executor、AssignmentPolicy）
2. gantt-scheduling-infrastructure（TimeWindow、TimeRange）
3. ospf-kotlin-core（MetaModel、LinearIntermediateSymbols、BinVariable、SlackFunction 等）
4. ospf-kotlin-framework（ShadowPrice、ShadowPriceKey、Pipeline）

---

## 二、概念/实体

### 1. 任务（Task）

待调度的工作单元，是编排模型的核心实体。

**$\text{index}_{t}$** ：任务 $t$ 的索引编号。
**$\text{id}_{t}$** ：任务 $t$ 的唯一标识符。
**$\text{name}_{t}$** ：任务 $t$ 的名称。
**$\text{cancelEnabled}_{t}$** ：任务 $t$ 是否允许取消。
**$\text{executorChangeEnabled}_{t}$** ：任务 $t$ 是否允许更换执行器。
**$\text{delayEnabled}_{t}$** ：任务 $t$ 是否允许延迟。
**$\text{advanceEnabled}_{t}$** ：任务 $t$ 是否允许提前。
**$\text{enabledExecutors}_{t}$** ：任务 $t$ 的可用执行器集合。
**$\text{maxDelay}_{t}$** ：任务 $t$ 的最大允许延迟量。
**$\text{maxAdvance}_{t}$** ：任务 $t$ 的最大允许提前量。
**$\text{lastEndTime}_{t}$** ：任务 $t$ 的最晚结束时间。
**$\text{earliestEndTime}_{t}$** ：任务 $t$ 的最早结束时间。
**$\text{scheduledTime}_{t}$** ：任务 $t$ 的计划时间。

### 2. 执行器（Executor）

执行任务的资源。

**$\text{index}_{e}$** ：执行器 $e$ 的索引编号。
**$\text{id}_{e}$** ：执行器 $e$ 的唯一标识符。
**$\text{name}_{e}$** ：执行器 $e$ 的名称。

### 3. 编排（Compilation）

任务-执行器分配建模的接口，定义编排模型的核心结构。

**$\text{taskCancelEnabled}$** ：是否启用任务取消建模。
**$\text{withExecutorLeisure}$** ：是否建模执行器空闲。
**$y_{t}$** ：任务取消决策变量集合。
**$z_{e}$** ：执行器空闲决策变量集合。
**$\text{taskAssignment}_{t,e}$** ：任务分配中间值，表示 $x_{t,e}$。
**$\text{taskCompilation}_{t}$** ：任务编排完整性中间值。
**$\text{executorCompilation}_{e}$** ：执行器编排完整性中间值。

### 4. 任务时间（TaskTime）

时间建模的接口，定义延迟、提前等时间偏差的建模结构。

**$\text{delayEnabled}$** ：是否启用延迟建模。
**$\text{overMaxDelayEnabled}$** ：是否启用超最大延迟建模。
**$\text{advanceEnabled}$** ：是否启用提前建模。
**$\text{overMaxAdvanceEnabled}$** ：是否启用超最大提前建模。
**$\text{estimateStartTime}_{t}$** ：任务 $t$ 的预估开始时间。
**$\text{estimateEndTime}_{t}$** ：任务 $t$ 的预估结束时间。
**$\text{delayTime}_{t}$** ：任务 $t$ 的延迟时间。
**$\text{advanceTime}_{t}$** ：任务 $t$ 的提前时间。
**$\text{overMaxDelayTime}_{t}$** ：任务 $t$ 的超最大延迟时间。
**$\text{overMaxAdvanceTime}_{t}$** ：任务 $t$ 的超最大提前时间。
**$\text{delayLastEndTime}_{t}$** ：任务 $t$ 相对于最晚结束时间的延迟量。
**$\text{advanceEarliestEndTime}_{t}$** ：任务 $t$ 相对于最早结束时间的提前量。
**$\text{onTime}_{t}$** ：任务 $t$ 是否准时完成。
**$\text{notOnTime}_{t}$** ：任务 $t$ 是否未准时完成。

### 5. 切换（Switch）

执行器过渡建模的接口，描述执行器在不同任务之间的切换行为。

**$\text{switch}_{e,t_1,t_2}$** ：执行器 $e$ 是否从任务 $t_1$ 切换到任务 $t_2$。
**$\text{switchTime}_{t_1,t_2}$** ：任务 $t_1$ 与 $t_2$ 之间的切换时间。
**$\text{frontOf}_{t_1,t_2}$** ：任务 $t_1$ 是否在任务 $t_2$ 之前。
**$\text{betweenIn}_{t_3,t_1,t_2}$** ：任务 $t_3$ 是否在任务 $t_1$ 与 $t_2$ 之间。

### 6. 最大完工时间（Makespan）

所有任务的最大完成时间。

**$\text{makespan}$** ：$\max_{t \in T}(\text{estimateEndTime}_{t})$。

### 7. 任务方案（TaskSolution）

任务编排的求解结果实体。

**$\text{assignedTasks}$** ：已分配的任务集合。
**$\text{canceledTasks}$** ：已取消的任务集合。
**$\text{summary.assignedTaskCount}$** ：已分配任务数量。
**$\text{summary.canceledTaskCount}$** ：已取消任务数量。
**$\text{summary.totalTaskCount}$** ：任务总数。

### 8. 求解器时间窗口边界（SolverTimeWindowBoundary）

业务时间到求解器数值的转换适配器。

**$\text{source}$** ：原始时间窗口来源。
**$\text{continues}$** ：是否为连续时间。
**$\text{durationValue}$** ：持续时间的求解器表示值。
**$\text{endValue}$** ：结束时间的求解器表示值。

---

## 三、变量

### 1. 决策变量

**$x_{t,e}$** ：二元变量，取值范围为 $\{0, 1\}$，任务 $t$ 是否分配给执行器 $e$，$\forall t \in T$，$\forall e \in E$。

**$y_{t}$** ：二元变量，取值范围为 $\{0, 1\}$，任务 $t$ 是否被取消，$\forall t \in T$。

**$z_{e}$** ：二元变量，取值范围为 $\{0, 1\}$，执行器 $e$ 是否空闲，$\forall e \in E$。

**$\text{est}_{t}$** ：连续/整数变量，取值范围为 $[0, \text{windowEnd}]$，任务 $t$ 的预估开始时间，$\forall t \in T$。

**$x^{i}_{t}$** ：二元变量（迭代），取值范围为 $\{0, 1\}$，列生成迭代中的列选择变量，$\forall t \in T^{iter}_{i}$。

**$\text{xor}_{e}$** ：二元变量（迭代），取值范围为 $\{0, 1\}$，执行器 $e$ 是否至少分配了一个列，$\forall e \in E$。

### 2. 辅助变量

本上下文的辅助变量通过 SlackFunction 在中间值中隐式表达，不单独定义辅助变量。

---

## 四、谓词

### 1. 任务属性谓词

**cancelEnabled** ：任务是否允许取消，即 $\text{cancelEnabled}_{t} = \text{true}$。
**executorChangeEnabled** ：任务是否允许更换执行器。
**delayEnabled** ：任务是否允许延迟。
**advanceEnabled** ：任务是否允许提前。
**hasMaxDelay** ：任务是否定义了最大延迟量，即 $\text{maxDelay}_{t} \neq \text{null}$。
**hasMaxAdvance** ：任务是否定义了最大提前量，即 $\text{maxAdvance}_{t} \neq \text{null}$。
**hasLastEndTime** ：任务是否定义了最晚结束时间，即 $\text{lastEndTime}_{t} \neq \text{null}$。
**hasEarliestEndTime** ：任务是否定义了最早结束时间，即 $\text{earliestEndTime}_{t} \neq \text{null}$。
**hasScheduledTime** ：任务是否有固定计划时间，即 $\text{scheduledTime}_{t} \neq \text{null}$。

### 2. 冲突谓词

**conflict** ：两个任务在同一个执行器上是否冲突，$\text{conflict}(e, t_i, t_j)$。

---

## 五、集合

### 1. 任务集合

**$T$** ：所有任务的全集。

**$T^{cancel}$** ：满足谓词 cancelEnabled 的子集，允许取消的任务集合。
**$T^{lockCancel}$** ：强制取消的任务子集（lockCancelTasks），业务上必须被取消的任务。
**$T^{iter}_{i}$** ：列生成第 $i$ 次迭代中新增的任务子集。
**$T^{removed}$** ：列生成过程中被剪枝的列集合。
**$T^{fixed}$** ：被固定为 1 的任务子集（全局或局部固定）。
**$T^{kept}$** ：列移除过程中被保留的任务子集。

### 2. 执行器集合

**$E$** ：所有执行器的全集。

**$E^{idle}$** ：满足 $z_{e} = 1$ 的子集，空闲执行器集合（hiddenExecutors）。

---

## 六、中间值

### 1. 任务分配

**描述**：任务分配中间值直接等于决策变量 $x_{t,e}$。

$$
\text{taskAssignment}_{t,e} = x_{t,e}
$$

### 2. 任务编排完整性

**描述**：每个任务的编排完整性约束，确保任务被分配或被取消。当启用取消功能时，包含取消变量；否则仅包含分配变量求和。

$$
\text{taskCompilation}_{t} = \begin{cases}
y_{t} + \displaystyle\sum_{e \in E} x_{t,e},& \text{cancelEnabled}_{t} = \text{true} \\ \\
\displaystyle\sum_{e \in E} x_{t,e},& \text{otherwise}
\end{cases}
$$

### 3. 执行器编排完整性

**描述**：每个执行器的编排完整性约束，确保执行器被使用或被标记为空闲。当启用执行器空闲建模时，包含空闲变量。

$$
\text{executorCompilation}_{e} = \begin{cases}
\text{OR}_{t \in T}(x_{t,e}) + z_{e},& \text{withExecutorLeisure} = \text{true} \\ \\
\text{OR}_{t \in T}(x_{t,e}),& \text{otherwise}
\end{cases}
$$

### 4. 预估开始时间

**描述**：任务的预估开始时间直接等于决策变量 $\text{est}_{t}$。

$$
\text{estimateStartTime}_{t} = \text{est}_{t}
$$

### 5. 预估结束时间

**描述**：任务的预估结束时间由预估开始时间和任务持续时间计算得出，计算逻辑通过注入的结束时间计算器实现。

$$
\text{estimateEndTime}_{t} = \text{estimateEndTimeCalculator}(t, \text{est}_{t})
$$

### 6. 松弛时间

**描述**：预估开始时间与计划开始时间之间的偏差，通过 SlackFunction 计算。

$$
\text{estSlack}_{t} = \text{Slack}(\text{est}_{t}, \text{scheduledStart}_{t})
$$

### 7. 延迟时间

**描述**：松弛时间的正偏差部分，仅对已编排的任务有效。未编排的任务贡献为零（masking）。

$$
\text{delayTime}_{t} = \text{pos}(\text{estSlack}_{t}) \times \text{taskCompilation}_{t}
$$

### 8. 提前时间

**描述**：松弛时间的负偏差部分，仅对已编排的任务有效。

$$
\text{advanceTime}_{t} = \text{neg}(\text{estSlack}_{t}) \times \text{taskCompilation}_{t}
$$

### 9. 超最大延迟时间

**描述**：延迟时间超出最大允许延迟量的部分，仅对已编排的任务有效。

$$
\text{overMaxDelayTime}_{t} = \text{Slack}(\text{delayTime}_{t}, \text{maxDelay}_{t}) \times \text{taskCompilation}_{t}
$$

### 10. 超最大提前时间

**描述**：提前时间超出最大允许提前量的部分，仅对已编排的任务有效。

$$
\text{overMaxAdvanceTime}_{t} = \text{Slack}(\text{advanceTime}_{t}, \text{maxAdvance}_{t}) \times \text{taskCompilation}_{t}
$$

### 11. 延迟最晚结束时间

**描述**：预估结束时间超出最晚结束时间的部分，仅对已编排的任务有效。

$$
\text{delayLastEndTime}_{t} = \text{Slack}(\text{estimateEndTime}_{t}, \text{lastEndTime}_{t}) \times \text{taskCompilation}_{t}
$$

### 12. 提前最早结束时间

**描述**：预估结束时间早于最早结束时间的部分，仅对已编排的任务有效。

$$
\text{advanceEarliestEndTime}_{t} = \text{Slack}(\text{estimateEndTime}_{t}, \text{earliestEndTime}_{t}) \times \text{taskCompilation}_{t}
$$

### 13. 前序关系

**描述**：任务 $t_1$ 是否在任务 $t_2$ 之前开始，通过 IfFunction 建模。

$$
\text{frontOf}_{t_1,t_2} = \text{If}(\text{estimateStartTime}_{t_1} \leq \text{estimateStartTime}_{t_2})
$$

### 14. 介于关系

**描述**：任务 $t_3$ 是否在任务 $t_1$ 与 $t_2$ 之间，通过 AndFunction 组合前序关系。

$$
\text{betweenIn}_{t_3,t_1,t_2} = \text{frontOf}_{t_1,t_3} \wedge \text{frontOf}_{t_3,t_2}
$$

### 15. 切换

**描述**：执行器 $e$ 从任务 $t_1$ 切换到任务 $t_2$，要求两个任务分配给同一执行器，且 $t_1$ 在 $t_2$ 之前，且没有中间任务。

$$
\text{switch}_{e,t_1,t_2} = x_{e,t_1} \wedge x_{e,t_2} \wedge \text{frontOf}_{t_1,t_2} \wedge \neg(\text{betweenIn}_{t_3,t_1,t_2})
$$

### 16. 切换时间

**描述**：两个连续任务之间的时间间隔，通过对所有执行器的切换变量求和并乘以时间间隔掩码计算。

$$
\text{switchTime}_{t_1,t_2} = \text{Masking}\left(\sum_{e \in E} \text{switch}_{e,t_1,t_2}, \text{timeGap}\right)
$$

### 17. 最大完工时间

**描述**：所有任务预估结束时间的最大值。

$$
\text{makespan} = \max_{t \in T}(\text{estimateEndTime}_{t})
$$

### 18. 准时指标

**描述**：任务是否在最晚结束时间之前完成且在最早结束时间之后完成。

$$
\text{onTime}_{t} = \text{If}(\text{estimateEndTime}_{t} \leq \text{lastEndTime}_{t}) + \text{If}(\text{estimateEndTime}_{t} \geq \text{earliestEndTime}_{t})
$$

### 19. 不准时指标

**描述**：任务是否违反时间约束。

$$
\text{notOnTime}_{t} = \neg(\text{onLastEndTime}_{t}) + \neg(\text{onEarliestEndTime}_{t})
$$

### 20. 迭代任务成本

**描述**：列生成迭代中的任务成本，按迭代列加权求和。

$$
\text{taskCost} = \sum_{i} \text{cost}(t_i) \times x^{i}_{t_i}
$$

---

## 七、断言

### 1. 任务编排完整性

**描述**：每个任务必须被分配给某个执行器或被取消。

$$
\forall t \in T \; (\text{taskCompilation}_{t} = 1)
$$

### 2. 执行器编排完整性

**描述**：每个执行器必须被分配任务或被标记为空闲。

$$
\forall e \in E \; (\text{executorCompilation}_{e} = 1)
$$

### 3. 任务分配变量范围

**描述**：任务分配变量为二元变量。

$$
\forall t \in T, \forall e \in E \; (x_{t,e} \in \{0, 1\})
$$

### 4. 不可取消任务约束

**描述**：不允许取消的任务，取消变量必须为 0。

$$
\forall t \in T \; (\neg \text{cancelEnabled}_{t} \rightarrow y_{t} = 0)
$$

### 5. 强制取消任务约束

**描述**：强制取消的任务，取消变量必须为 1。

$$
\forall t \in T^{lockCancel} \; (y_{t} = 1)
$$

### 6. 不可用执行器约束

**描述**：任务不能分配给不在其可用执行器集合中的执行器。

$$
\forall t \in T, \forall e \in E \; (e \notin \text{enabledExecutors}_{t} \rightarrow x_{t,e} = 0)
$$

### 7. 固定执行器约束

**描述**：当任务已有固定执行器且不允许更换执行器且不允许取消时，分配变量必须为 1。

$$
\forall t \in T, \forall e \in E \; (\text{executor}_{t} = e \wedge \neg \text{executorChangeEnabled}_{t} \wedge \neg \text{cancelEnabled}_{t} \rightarrow x_{t,e} = 1)
$$

---

## 八、约束

### 1. 任务编排约束

**[英]**：Task Compilation Constraint
**描述**：每个任务必须恰好被分配给一个执行器或被取消，确保任务分配的完整性。

$$
s.t. \quad \text{taskCompilation}_{t} = 1, \; \forall t \in T
$$

### 2. 执行器编排约束

**[英]**：Executor Compilation Constraint
**描述**：每个执行器必须被分配至少一个任务或被标记为空闲，确保执行器状态的完整性。

$$
s.t. \quad \text{executorCompilation}_{e} = 1, \; \forall e \in E
$$

### 3. 任务冲突约束

**[英]**：Task Conflict Constraint
**描述**：相互冲突的两个任务不能同时分配给同一个执行器。

$$
s.t. \quad x_{t_i,e} + x_{t_j,e} \leq 1, \; \forall e \in E, \forall (t_i, t_j) \in \text{conflict}(e)
$$

### 4. 任务时间冲突约束

**[英]**：Task Time Conflict Constraint
**描述**：时间上重叠的两个任务不能同时分配给同一个执行器。

$$
s.t. \quad x_{t_i,e} + x_{t_j,e} \leq 1, \; \forall e \in E, \forall (t_i, t_j) \in \text{temporalConflict}(e)
$$

### 5. 任务步进冲突约束

**[英]**：Task Step Conflict Constraint
**描述**：同一冲突组中最多只有一个任务被编排。

$$
s.t. \quad \sum_{t \in \text{group}} \text{taskCompilation}_{t} \leq 1, \; \forall \text{group} \in \text{stepGroups}
$$

### 6. 任务延迟时间约束

**[英]**：Task Delay Time Constraint
**描述**：不允许延迟的任务，其预估开始时间不得超过计划开始时间。

$$
s.t. \quad \text{estimateStartTime}_{t} \leq \text{scheduledStart}_{t}, \; \forall t \in T \; (\neg \text{delayEnabled}_{t})
$$

### 7. 任务提前时间约束

**[英]**：Task Advance Time Constraint
**描述**：不允许提前的任务，其预估开始时间不得早于计划开始时间。

$$
s.t. \quad \text{estimateStartTime}_{t} \geq \text{scheduledStart}_{t}, \; \forall t \in T \; (\neg \text{advanceEnabled}_{t})
$$

### 8. 任务超最大延迟约束

**[英]**：Task Over-Max Delay Constraint
**描述**：任务的延迟时间不得超过最大允许延迟量。

$$
s.t. \quad \text{delayTime}_{t} \leq \text{maxDelay}_{t}, \; \forall t \in T \; (\text{hasMaxDelay}_{t})
$$

### 9. 任务超最大提前约束

**[英]**：Task Over-Max Advance Constraint
**描述**：任务的提前时间不得超过最大允许提前量。

$$
s.t. \quad \text{advanceTime}_{t} \leq \text{maxAdvance}_{t}, \; \forall t \in T \; (\text{hasMaxAdvance}_{t})
$$

### 10. 任务延迟最晚结束时间约束

**[英]**：Task Delay Last End Time Constraint
**描述**：任务的预估结束时间不得超过最晚结束时间。

$$
s.t. \quad \text{estimateEndTime}_{t} \leq \text{lastEndTime}_{t}, \; \forall t \in T \; (\text{hasLastEndTime}_{t})
$$

### 11. 任务提前最早结束时间约束

**[英]**：Task Advance Earliest End Time Constraint
**描述**：任务的预估结束时间不得早于最早结束时间。

$$
s.t. \quad \text{estimateEndTime}_{t} \geq \text{earliestEndTime}_{t}, \; \forall t \in T \; (\text{hasEarliestEndTime}_{t})
$$

---

## 九、目标函数

所有最小化目标函数均支持阈值松弛模式：当阈值大于零时，仅对超出阈值的部分进行惩罚。

### 1. 最大完工时间最小化

**描述**：最小化所有任务的最大完成时间。

$$
\min \; \text{coefficient} \times \max_{t \in T}(\text{estimateEndTime}_{t})
$$

### 2. 任务取消最小化

**描述**：最小化任务取消的加权总和。

$$
\min \; \sum_{t \in T} \text{coefficient}_{t} \times y_{t}
$$

### 3. 任务成本最小化

**描述**：最小化迭代列的任务成本。

$$
\min \; \text{taskCost}
$$

### 4. 任务执行器成本最小化

**描述**：最小化任务-执行器分配的加权成本。

$$
\min \; \sum_{t \in T} \sum_{e \in E} \text{cost}(t,e) \times x_{t,e}
$$

### 5. 执行器成本最小化

**描述**：最小化执行器使用的加权成本。

$$
\min \; \sum_{e \in E} \text{cost}_{e} \times \text{executorCompilation}_{e}
$$

### 6. 执行器空闲最小化

**描述**：最小化空闲执行器的加权成本。

$$
\min \; \sum_{e \in E} \text{cost}_{e} \times z_{e}
$$

### 7. 切换成本最小化

**描述**：最小化执行器切换的加权成本。

$$
\min \; \sum_{e \in E} \sum_{t_1 \in T} \sum_{t_2 \in T} \text{coeff}(e,t_1,t_2) \times \text{switch}_{e,t_1,t_2}
$$

### 8. 切换时间最小化

**描述**：最小化任务间切换时间的加权总和。

$$
\min \; \sum_{t_1 \in T} \sum_{t_2 \in T} \text{coeff}(t_1,t_2) \times \text{switchTime}_{t_1,t_2}
$$

### 9. 任务延迟时间最小化

**描述**：最小化任务延迟时间的加权总和。

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{delayTime}_{t}
$$

### 10. 任务提前时间最小化

**描述**：最小化任务提前时间的加权总和。

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{advanceTime}_{t}
$$

### 11. 任务超最大延迟最小化

**描述**：最小化任务超最大延迟时间的加权总和。

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{overMaxDelayTime}_{t}
$$

### 12. 任务超最大提前最小化

**描述**：最小化任务超最大提前时间的加权总和。

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{overMaxAdvanceTime}_{t}
$$

### 13. 任务延迟最晚结束时间最小化

**描述**：最小化任务相对于最晚结束时间的延迟量加权总和。

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{delayLastEndTime}_{t}
$$

### 14. 任务提前最早结束时间最小化

**描述**：最小化任务相对于最早结束时间的提前量加权总和。

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{advanceEarliestEndTime}_{t}
$$

### 15. 任务准时最大化

**描述**：最大化准时完成的任务数量。

$$
\max \; \text{coefficient} \times \sum_{t \in T} \text{onTime}_{t}
$$

### 16. 任务不准时最小化

**描述**：最小化未准时完成的任务数量。

$$
\min \; \text{coefficient} \times \sum_{t \in T} \text{notOnTime}_{t}
$$

---

## 十、算法引用

本领域模型不引用独立算法文档。

---

## 十一、通用语言

| 术语 | 符号 | 英文 | 定义 |
|------|------|------|------|
| 任务 | $T$ / $t$ | Task | 待调度的工作单元 |
| 执行器 | $E$ / $e$ | Executor | 执行任务的资源 |
| 分配策略 | $A$ | Assignment Policy | 任务到执行器的分配策略 |
| 编排 | - | Compilation | 任务到执行器的分配过程 |
| 任务分配 | $x_{t,e}$ | Task Assignment | 二元变量：任务 $t$ 是否分配给执行器 $e$ |
| 任务取消 | $y_{t}$ | Task Cancel | 二元变量：任务 $t$ 是否被取消 |
| 执行器空闲 | $z_{e}$ | Executor Leisure | 二元变量：执行器 $e$ 是否空闲 |
| 预估开始时间 | $\text{est}_{t}$ | Estimated Start Time | 任务开始时间的决策变量 |
| 预估结束时间 | $\text{estimateEndTime}_{t}$ | Estimated End Time | 由开始时间计算得出 |
| 延迟时间 | $\text{delayTime}_{t}$ | Delay Time | 相对于计划开始时间的正偏差 |
| 提前时间 | $\text{advanceTime}_{t}$ | Advance Time | 相对于计划开始时间的负偏差 |
| 超最大延迟 | $\text{overMaxDelayTime}_{t}$ | Over-Max Delay | 超出 maxDelay 约束的部分 |
| 超最大提前 | $\text{overMaxAdvanceTime}_{t}$ | Over-Max Advance | 超出 maxAdvance 约束的部分 |
| 最晚结束时间 | $\text{lastEndTime}$ | Last End Time | 任务必须在此时间前完成 |
| 最早结束时间 | $\text{earliestEndTime}$ | Earliest End Time | 任务允许完成的最早时间 |
| 准时 | $\text{onTime}_{t}$ | On Time | 指标：同时满足两个时间约束 |
| 不准时 | $\text{notOnTime}_{t}$ | Not On Time | 指标：违反时间约束 |
| 切换 | $\text{switch}_{e,t_1,t_2}$ | Switch | 二元变量：执行器 $e$ 从 $t_1$ 过渡到 $t_2$ |
| 切换时间 | $\text{switchTime}_{t_1,t_2}$ | Switch Time | 连续任务间的时间间隔 |
| 前序 | $\text{frontOf}_{t_1,t_2}$ | Front-Of | 二元变量：$t_1$ 在 $t_2$ 之前开始 |
| 介于 | $\text{betweenIn}_{t_3,t_1,t_2}$ | Between-In | 二元变量：$t_3$ 在 $t_1$ 与 $t_2$ 之间 |
| 最大完工时间 | $\text{makespan}$ | Makespan | 所有任务的最大预估结束时间 |
| 阈值松弛 | - | Threshold Slack | 仅对超出阈值的部分进行惩罚 |
| 列生成 | - | Column Generation | 迭代添加/移除列的方法 |
| 影子价格 | - | Shadow Price | 列生成定价中的对偶值 |
| 约简成本 | - | Reduced Cost | 成本减去影子价格贡献 |
| 流水线 | - | Pipeline | 模型中的约束或目标函数 |

---

## 十二、设计决策

| 决策 | 备选方案 | 选择原因 | 日期 |
|------|----------|----------|------|
| 基于 Slack 的延迟/提前建模 | 直接使用差值变量 | SlackFunction 自然分解正负偏差，支持阈值松弛 | - |
| 取消任务的掩码机制 | 在约束中单独处理 | 已取消任务对所有时间量贡献为零，简化建模 | - |
| 阈值松弛模式 | 固定惩罚 | 所有最小化目标支持可选阈值，增强灵活性 | - |
| SolverTimeWindowBoundary 适配器 | 内联转换逻辑 | 集中管理业务时间到求解器数值的转换 | - |
| 可插拔结束时间计算器 | 硬编码计算逻辑 | 注入函数支持不同的任务持续时间逻辑 | - |
| 双重编排路径 | 统一编排接口 | TaskCompilation 用于一次性求解，IterativeTaskCompilation 用于列生成 | - |
| Pipeline 架构 | 整体式模型构建 | 可组合地注册约束和目标函数 | - |
| FrontOf/BetweenIn 排序建模 | 直接使用时间差 | 通过 IfFunction 和 AndFunction 建模时序关系 | - |

---

## 十三、演进记录

| 版本 | 变更 | 原因 |
|------|------|------|
| v1.0 | 初始版本 | 任务编排领域模型建立 |
