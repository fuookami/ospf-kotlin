# Task Compilation Context Domain Model

[toc]

## 1. Overview

The Task Compilation Context solves the task-to-executor assignment and scheduling problem in the Gantt scheduling framework. It assigns tasks to executors via binary decision variables, models temporal constraints (delays, advances, switch costs), supports column generation, and provides objective function components.

### 1. Dependent Contexts

1. gantt-scheduling-domain-task-context (task model: AbstractTask, Executor, AssignmentPolicy)
2. gantt-scheduling-infrastructure (TimeWindow, TimeRange)
3. ospf-kotlin-core (MetaModel, LinearIntermediateSymbols, BinVariable, SlackFunction, etc.)
4. ospf-kotlin-framework (ShadowPrice, ShadowPriceKey, Pipeline)

---

## 2. Concepts / Entities

### 1. Task

A unit of work to be scheduled; the core entity of the compilation model.

**$\text{index}_{t}$** : Index number of task $t$.
**$\text{id}_{t}$** : Unique identifier of task $t$.
**$\text{name}_{t}$** : Name of task $t$.
**$\text{cancelEnabled}_{t}$** : Whether task $t$ allows cancellation.
**$\text{executorChangeEnabled}_{t}$** : Whether task $t$ allows executor reassignment.
**$\text{delayEnabled}_{t}$** : Whether task $t$ allows delay.
**$\text{advanceEnabled}_{t}$** : Whether task $t$ allows advance.
**$\text{enabledExecutors}_{t}$** : Set of available executors for task $t$.
**$\text{maxDelay}_{t}$** : Maximum allowed delay amount for task $t$.
**$\text{maxAdvance}_{t}$** : Maximum allowed advance amount for task $t$.
**$\text{lastEndTime}_{t}$** : Latest end time of task $t$.
**$\text{earliestEndTime}_{t}$** : Earliest end time of task $t$.
**$\text{scheduledTime}_{t}$** : Scheduled time of task $t$.

### 2. Executor

A resource that performs tasks.

**$\text{index}_{e}$** : Index number of executor $e$.
**$\text{id}_{e}$** : Unique identifier of executor $e$.
**$\text{name}_{e}$** : Name of executor $e$.

### 3. Compilation

Interface for task-executor assignment modeling, defining the core structure of the compilation model.

**$\text{taskCancelEnabled}$** : Whether task cancellation modeling is enabled.
**$\text{withExecutorLeisure}$** : Whether executor idle modeling is enabled.
**$y_{t}$** : Task cancel decision variable set.
**$z_{e}$** : Executor idle decision variable set.
**$\text{taskAssignment}_{t,e}$** : Task assignment intermediate value, representing $x_{t,e}$.
**$\text{taskCompilation}_{t}$** : Task compilation integrity intermediate value.
**$\text{executorCompilation}_{e}$** : Executor compilation integrity intermediate value.

### 4. TaskTime

Interface for temporal modeling, defining the structure for delay, advance, and other time deviations.

**$\text{delayEnabled}$** : Whether delay modeling is enabled.
**$\text{overMaxDelayEnabled}$** : Whether over-max delay modeling is enabled.
**$\text{advanceEnabled}$** : Whether advance modeling is enabled.
**$\text{overMaxAdvanceEnabled}$** : Whether over-max advance modeling is enabled.
**$\text{estimateStartTime}_{t}$** : Estimated start time of task $t$.
**$\text{estimateEndTime}_{t}$** : Estimated end time of task $t$.
**$\text{delayTime}_{t}$** : Delay time of task $t$.
**$\text{advanceTime}_{t}$** : Advance time of task $t$.
**$\text{overMaxDelayTime}_{t}$** : Over-max delay time of task $t$.
**$\text{overMaxAdvanceTime}_{t}$** : Over-max advance time of task $t$.
**$\text{delayLastEndTime}_{t}$** : Delay amount relative to last end time of task $t$.
**$\text{advanceEarliestEndTime}_{t}$** : Advance amount relative to earliest end time of task $t$.
**$\text{onTime}_{t}$** : Whether task $t$ is completed on time.
**$\text{notOnTime}_{t}$** : Whether task $t$ is not completed on time.

### 5. Switch

Interface for executor transition modeling, describing executor behavior between different tasks.

**$\text{switch}_{e,t_1,t_2}$** : Whether executor $e$ transitions from task $t_1$ to task $t_2$.
**$\text{switchTime}_{t_1,t_2}$** : Switch time between tasks $t_1$ and $t_2$.
**$\text{frontOf}_{t_1,t_2}$** : Whether task $t_1$ starts before task $t_2$.
**$\text{betweenIn}_{t_3,t_1,t_2}$** : Whether task $t_3$ is between tasks $t_1$ and $t_2$.

### 6. Makespan

Maximum completion time across all tasks.

**$\text{makespan}$** : $\max_{t \in T}(\text{estimateEndTime}_{t})$.

### 7. TaskSolution

Solution entity for task compilation results.

**$\text{assignedTasks}$** : Set of assigned tasks.
**$\text{canceledTasks}$** : Set of canceled tasks.
**$\text{summary.assignedTaskCount}$** : Count of assigned tasks.
**$\text{summary.canceledTaskCount}$** : Count of canceled tasks.
**$\text{summary.totalTaskCount}$** : Total task count.

### 8. SolverTimeWindowBoundary

Adapter for business-time to solver-value conversion.

**$\text{source}$** : Original time window source.
**$\text{continues}$** : Whether the time is continuous.
**$\text{durationValue}$** : Solver representation of duration.
**$\text{endValue}$** : Solver representation of end time.

---

## 3. Variables

### 1. Decision Variables

**$x_{t,e}$** : Binary, domain is $\{0, 1\}$, whether task $t$ is assigned to executor $e$, $\forall t \in T$, $\forall e \in E$.

**$y_{t}$** : Binary, domain is $\{0, 1\}$, whether task $t$ is canceled, $\forall t \in T$.

**$z_{e}$** : Binary, domain is $\{0, 1\}$, whether executor $e$ is idle, $\forall e \in E$.

**$\text{est}_{t}$** : Continuous/Integer, domain is $[0, \text{windowEnd}]$, estimated start time of task $t$, $\forall t \in T$.

**$x^{i}_{t}$** : Binary (iterative), domain is $\{0, 1\}$, iterative column selection variable in column generation, $\forall t \in T^{iter}_{i}$.

**$\text{xor}_{e}$** : Binary (iterative), domain is $\{0, 1\}$, whether executor $e$ has at least one assigned column, $\forall e \in E$.

### 2. Auxiliary Variables

Auxiliary variables in this context are implicitly expressed through SlackFunction in intermediate values and are not separately defined.

---

## 4. Predicates

### 1. Task Property Predicates

**cancelEnabled** : Whether the task allows cancellation, i.e., $\text{cancelEnabled}_{t} = \text{true}$.
**executorChangeEnabled** : Whether the task allows executor reassignment.
**delayEnabled** : Whether the task allows delay.
**advanceEnabled** : Whether the task allows advance.
**hasMaxDelay** : Whether the task defines a maximum delay amount, i.e., $\text{maxDelay}_{t} \neq \text{null}$.
**hasMaxAdvance** : Whether the task defines a maximum advance amount, i.e., $\text{maxAdvance}_{t} \neq \text{null}$.
**hasLastEndTime** : Whether the task defines a latest end time, i.e., $\text{lastEndTime}_{t} \neq \text{null}$.
**hasEarliestEndTime** : Whether the task defines an earliest end time, i.e., $\text{earliestEndTime}_{t} \neq \text{null}$.
**hasScheduledTime** : Whether the task has a fixed scheduled time, i.e., $\text{scheduledTime}_{t} \neq \text{null}$.

### 2. Conflict Predicates

**conflict** : Whether two tasks conflict on the same executor, $\text{conflict}(e, t_i, t_j)$.

---

## 5. Sets

### 1. Task Set

**$T$** : Universal set of all tasks.

**$T^{cancel}$** : Subset satisfying predicate cancelEnabled; tasks that allow cancellation.
**$T^{lockCancel}$** : Subset of tasks forced to be canceled (lockCancelTasks); tasks that must be canceled by business rules.
**$T^{iter}_{i}$** : Tasks added in column generation iteration $i$.
**$T^{removed}$** : Columns pruned during column generation.
**$T^{fixed}$** : Tasks fixed to 1 (globally or locally fixed).
**$T^{kept}$** : Tasks kept during column removal.

### 2. Executor Set

**$E$** : Universal set of all executors.

**$E^{idle}$** : Subset where $z_{e} = 1$; idle executors (hiddenExecutors).

---

## 6. Intermediate Values

### 1. Task Assignment

**Description**: The task assignment intermediate value directly equals the decision variable $x_{t,e}$.

$$
\text{taskAssignment}_{t,e} = x_{t,e}
$$

### 2. Task Compilation Integrity

**Description**: Compilation integrity for each task, ensuring the task is assigned or canceled. When cancellation is enabled, the cancel variable is included; otherwise only the sum of assignment variables is used.

$$
\text{taskCompilation}_{t} = \begin{cases}
y_{t} + \displaystyle\sum_{e \in E} x_{t,e},& \text{cancelEnabled}_{t} = \text{true} \\ \\
\displaystyle\sum_{e \in E} x_{t,e},& \text{otherwise}
\end{cases}
$$

### 3. Executor Compilation Integrity

**Description**: Compilation integrity for each executor, ensuring the executor is used or marked idle. When executor leisure modeling is enabled, the idle variable is included.

$$
\text{executorCompilation}_{e} = \begin{cases}
\text{OR}_{t \in T}(x_{t,e}) + z_{e},& \text{withExecutorLeisure} = \text{true} \\ \\
\text{OR}_{t \in T}(x_{t,e}),& \text{otherwise}
\end{cases}
$$

### 4. Estimated Start Time

**Description**: The estimated start time of a task directly equals the decision variable $\text{est}_{t}$.

$$
\text{estimateStartTime}_{t} = \text{est}_{t}
$$

### 5. Estimated End Time

**Description**: The estimated end time is computed from the estimated start time and task duration via an injected end-time calculator.

$$
\text{estimateEndTime}_{t} = \text{estimateEndTimeCalculator}(t, \text{est}_{t})
$$

### 6. Slack Time

**Description**: The deviation between estimated start time and scheduled start time, computed via SlackFunction.

$$
\text{estSlack}_{t} = \text{Slack}(\text{est}_{t}, \text{scheduledStart}_{t})
$$

### 7. Delay Time

**Description**: The positive deviation component of slack time, effective only for compiled tasks. Uncompiled tasks contribute zero (masking).

$$
\text{delayTime}_{t} = \text{pos}(\text{estSlack}_{t}) \times \text{taskCompilation}_{t}
$$

### 8. Advance Time

**Description**: The negative deviation component of slack time, effective only for compiled tasks.

$$
\text{advanceTime}_{t} = \text{neg}(\text{estSlack}_{t}) \times \text{taskCompilation}_{t}
$$

### 9. Over-Max Delay Time

**Description**: The portion of delay time exceeding the maximum allowed delay, effective only for compiled tasks.

$$
\text{overMaxDelayTime}_{t} = \text{Slack}(\text{delayTime}_{t}, \text{maxDelay}_{t}) \times \text{taskCompilation}_{t}
$$

### 10. Over-Max Advance Time

**Description**: The portion of advance time exceeding the maximum allowed advance, effective only for compiled tasks.

$$
\text{overMaxAdvanceTime}_{t} = \text{Slack}(\text{advanceTime}_{t}, \text{maxAdvance}_{t}) \times \text{taskCompilation}_{t}
$$

### 11. Delay Last End Time

**Description**: The portion of estimated end time exceeding the latest end time, effective only for compiled tasks.

$$
\text{delayLastEndTime}_{t} = \text{Slack}(\text{estimateEndTime}_{t}, \text{lastEndTime}_{t}) \times \text{taskCompilation}_{t}
$$

### 12. Advance Earliest End Time

**Description**: The portion by which estimated end time is earlier than the earliest end time, effective only for compiled tasks.

$$
\text{advanceEarliestEndTime}_{t} = \text{Slack}(\text{estimateEndTime}_{t}, \text{earliestEndTime}_{t}) \times \text{taskCompilation}_{t}
$$

### 13. Front-Of Relation

**Description**: Whether task $t_1$ starts before task $t_2$, modeled via IfFunction.

$$
\text{frontOf}_{t_1,t_2} = \text{If}(\text{estimateStartTime}_{t_1} \leq \text{estimateStartTime}_{t_2})
$$

### 14. Between-In Relation

**Description**: Whether task $t_3$ is between tasks $t_1$ and $t_2$, composed via AndFunction on front-of relations.

$$
\text{betweenIn}_{t_3,t_1,t_2} = \text{frontOf}_{t_1,t_3} \wedge \text{frontOf}_{t_3,t_2}
$$

### 15. Switch

**Description**: Executor $e$ transitions from task $t_1$ to task $t_2$; requires both tasks assigned to the same executor, $t_1$ before $t_2$, and no intermediate task.

$$
\text{switch}_{e,t_1,t_2} = x_{e,t_1} \wedge x_{e,t_2} \wedge \text{frontOf}_{t_1,t_2} \wedge \neg(\text{betweenIn}_{t_3,t_1,t_2})
$$

### 16. Switch Time

**Description**: The time gap between two consecutive tasks, computed by summing switch variables across all executors and multiplying by the time gap mask.

$$
\text{switchTime}_{t_1,t_2} = \text{Masking}\left(\sum_{e \in E} \text{switch}_{e,t_1,t_2}, \text{timeGap}\right)
$$

### 17. Makespan

**Description**: The maximum estimated end time across all tasks.

$$
\text{makespan} = \max_{t \in T}(\text{estimateEndTime}_{t})
$$

### 18. On-Time Indicator

**Description**: Whether a task completes before its latest end time and after its earliest end time.

$$
\text{onTime}_{t} = \text{If}(\text{estimateEndTime}_{t} \leq \text{lastEndTime}_{t}) + \text{If}(\text{estimateEndTime}_{t} \geq \text{earliestEndTime}_{t})
$$

### 19. Not-On-Time Indicator

**Description**: Whether a task violates its time constraints.

$$
\text{notOnTime}_{t} = \neg(\text{onLastEndTime}_{t}) + \neg(\text{onEarliestEndTime}_{t})
$$

### 20. Iterative Task Cost

**Description**: Task cost in column generation iteration, weighted sum over iterative columns.

$$
\text{taskCost} = \sum_{i} \text{cost}(t_i) \times x^{i}_{t_i}
$$

---

## 7. Assertions

### 1. Task Compilation Integrity

**Description**: Every task must be assigned to an executor or be canceled.

$$
\forall t \in T \; (\text{taskCompilation}_{t} = 1)
$$

### 2. Executor Compilation Integrity

**Description**: Every executor must have tasks assigned or be marked idle.

$$
\forall e \in E \; (\text{executorCompilation}_{e} = 1)
$$

### 3. Task Assignment Variable Domain

**Description**: Task assignment variables are binary.

$$
\forall t \in T, \forall e \in E \; (x_{t,e} \in \{0, 1\})
$$

### 4. Non-Cancelable Task Constraint

**Description**: Tasks that do not allow cancellation must have their cancel variable set to 0.

$$
\forall t \in T \; (\neg \text{cancelEnabled}_{t} \rightarrow y_{t} = 0)
$$

### 5. Forced Cancel Task Constraint

**Description**: Tasks forced to be canceled must have their cancel variable set to 1.

$$
\forall t \in T^{lockCancel} \; (y_{t} = 1)
$$

### 6. Unavailable Executor Constraint

**Description**: A task cannot be assigned to an executor not in its available executor set.

$$
\forall t \in T, \forall e \in E \; (e \notin \text{enabledExecutors}_{t} \rightarrow x_{t,e} = 0)
$$

### 7. Fixed Executor Constraint

**Description**: When a task has a fixed executor and does not allow executor change or cancellation, the assignment variable must be 1.

$$
\forall t \in T, \forall e \in E \; (\text{executor}_{t} = e \wedge \neg \text{executorChangeEnabled}_{t} \wedge \neg \text{cancelEnabled}_{t} \rightarrow x_{t,e} = 1)
$$

---

## 8. Constraints

### 1. Task Compilation Constraint

**[CN]**：任务编排约束
**Description**: Each task must be assigned to exactly one executor or be canceled, ensuring assignment integrity.

$$
s.t. \quad \text{taskCompilation}_{t} = 1, \; \forall t \in T
$$

### 2. Executor Compilation Constraint

**[CN]**：执行器编排约束
**Description**: Each executor must have at least one task assigned or be marked idle, ensuring executor state integrity.

$$
s.t. \quad \text{executorCompilation}_{e} = 1, \; \forall e \in E
$$

### 3. Task Conflict Constraint

**[CN]**：任务冲突约束
**Description**: Two conflicting tasks cannot be assigned to the same executor simultaneously.

$$
s.t. \quad x_{t_i,e} + x_{t_j,e} \leq 1, \; \forall e \in E, \forall (t_i, t_j) \in \text{conflict}(e)
$$

### 4. Task Time Conflict Constraint

**[CN]**：任务时间冲突约束
**Description**: Two temporally overlapping tasks cannot be assigned to the same executor simultaneously.

$$
s.t. \quad x_{t_i,e} + x_{t_j,e} \leq 1, \; \forall e \in E, \forall (t_i, t_j) \in \text{temporalConflict}(e)
$$

### 5. Task Step Conflict Constraint

**[CN]**：任务步进冲突约束
**Description**: At most one task in each conflict group may be compiled.

$$
s.t. \quad \sum_{t \in \text{group}} \text{taskCompilation}_{t} \leq 1, \; \forall \text{group} \in \text{stepGroups}
$$

### 6. Task Delay Time Constraint

**[CN]**：任务延迟时间约束
**Description**: For tasks that do not allow delay, the estimated start time must not exceed the scheduled start time.

$$
s.t. \quad \text{estimateStartTime}_{t} \leq \text{scheduledStart}_{t}, \; \forall t \in T \; (\neg \text{delayEnabled}_{t})
$$

### 7. Task Advance Time Constraint

**[CN]**：任务提前时间约束
**Description**: For tasks that do not allow advance, the estimated start time must not be earlier than the scheduled start time.

$$
s.t. \quad \text{estimateStartTime}_{t} \geq \text{scheduledStart}_{t}, \; \forall t \in T \; (\neg \text{advanceEnabled}_{t})
$$

### 8. Task Over-Max Delay Constraint

**[CN]**：任务超最大延迟约束
**Description**: The delay time of a task must not exceed its maximum allowed delay amount.

$$
s.t. \quad \text{delayTime}_{t} \leq \text{maxDelay}_{t}, \; \forall t \in T \; (\text{hasMaxDelay}_{t})
$$

### 9. Task Over-Max Advance Constraint

**[CN]**：任务超最大提前约束
**Description**: The advance time of a task must not exceed its maximum allowed advance amount.

$$
s.t. \quad \text{advanceTime}_{t} \leq \text{maxAdvance}_{t}, \; \forall t \in T \; (\text{hasMaxAdvance}_{t})
$$

### 10. Task Delay Last End Time Constraint

**[CN]**：任务延迟最晚结束时间约束
**Description**: The estimated end time of a task must not exceed its latest end time.

$$
s.t. \quad \text{estimateEndTime}_{t} \leq \text{lastEndTime}_{t}, \; \forall t \in T \; (\text{hasLastEndTime}_{t})
$$

### 11. Task Advance Earliest End Time Constraint

**[CN]**：任务提前最早结束时间约束
**Description**: The estimated end time of a task must not be earlier than its earliest end time.

$$
s.t. \quad \text{estimateEndTime}_{t} \geq \text{earliestEndTime}_{t}, \; \forall t \in T \; (\text{hasEarliestEndTime}_{t})
$$

---

## 9. Objective Function

All minimization objectives support a threshold slack pattern: when the threshold is greater than zero, only the excess beyond the threshold is penalized.

### 1. Makespan Minimization

**Description**: Minimize the maximum completion time across all tasks.

$$
\min \; \text{coefficient} \times \max_{t \in T}(\text{estimateEndTime}_{t})
$$

### 2. Task Cancel Minimization

**Description**: Minimize the weighted sum of task cancellations.

$$
\min \; \sum_{t \in T} \text{coefficient}_{t} \times y_{t}
$$

### 3. Task Cost Minimization

**Description**: Minimize the task cost from iterative columns.

$$
\min \; \text{taskCost}
$$

### 4. Task Executor Cost Minimization

**Description**: Minimize the weighted cost of task-executor assignments.

$$
\min \; \sum_{t \in T} \sum_{e \in E} \text{cost}(t,e) \times x_{t,e}
$$

### 5. Executor Cost Minimization

**Description**: Minimize the weighted cost of executor usage.

$$
\min \; \sum_{e \in E} \text{cost}_{e} \times \text{executorCompilation}_{e}
$$

### 6. Executor Leisure Minimization

**Description**: Minimize the weighted cost of idle executors.

$$
\min \; \sum_{e \in E} \text{cost}_{e} \times z_{e}
$$

### 7. Switch Cost Minimization

**Description**: Minimize the weighted cost of executor switches.

$$
\min \; \sum_{e \in E} \sum_{t_1 \in T} \sum_{t_2 \in T} \text{coeff}(e,t_1,t_2) \times \text{switch}_{e,t_1,t_2}
$$

### 8. Switch Time Minimization

**Description**: Minimize the weighted sum of inter-task switch times.

$$
\min \; \sum_{t_1 \in T} \sum_{t_2 \in T} \text{coeff}(t_1,t_2) \times \text{switchTime}_{t_1,t_2}
$$

### 9. Task Delay Time Minimization

**Description**: Minimize the weighted sum of task delay times.

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{delayTime}_{t}
$$

### 10. Task Advance Time Minimization

**Description**: Minimize the weighted sum of task advance times.

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{advanceTime}_{t}
$$

### 11. Task Over-Max Delay Minimization

**Description**: Minimize the weighted sum of task over-max delay times.

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{overMaxDelayTime}_{t}
$$

### 12. Task Over-Max Advance Minimization

**Description**: Minimize the weighted sum of task over-max advance times.

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{overMaxAdvanceTime}_{t}
$$

### 13. Task Delay Last End Time Minimization

**Description**: Minimize the weighted sum of delay amounts relative to latest end time.

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{delayLastEndTime}_{t}
$$

### 14. Task Advance Earliest End Time Minimization

**Description**: Minimize the weighted sum of advance amounts relative to earliest end time.

$$
\min \; \sum_{t \in T} \text{coeff}_{t} \times \text{advanceEarliestEndTime}_{t}
$$

### 15. Task On-Time Maximization

**Description**: Maximize the number of tasks completed on time.

$$
\max \; \text{coefficient} \times \sum_{t \in T} \text{onTime}_{t}
$$

### 16. Task Not-On-Time Minimization

**Description**: Minimize the number of tasks not completed on time.

$$
\min \; \text{coefficient} \times \sum_{t \in T} \text{notOnTime}_{t}
$$

---

## 10. Algorithm References

This domain model does not reference standalone algorithm documents.

---

## 11. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Task | $T$ / $t$ | A unit of work to be scheduled |
| Executor | $E$ / $e$ | A resource that performs tasks |
| Assignment Policy | $A$ | Strategy for assigning tasks to executors |
| Compilation | - | The process of assigning tasks to executors |
| Task Assignment | $x_{t,e}$ | Binary: 1 if task $t$ assigned to executor $e$ |
| Task Cancel | $y_{t}$ | Binary: 1 if task $t$ is canceled |
| Executor Leisure | $z_{e}$ | Binary: 1 if executor $e$ is idle |
| Estimated Start Time | $\text{est}_{t}$ | Decision variable for when task starts |
| Estimated End Time | $\text{estimateEndTime}_{t}$ | Computed from start time |
| Delay Time | $\text{delayTime}_{t}$ | Positive deviation from scheduled start |
| Advance Time | $\text{advanceTime}_{t}$ | Negative deviation from scheduled start |
| Over-Max Delay | $\text{overMaxDelayTime}_{t}$ | Excess beyond maxDelay bound |
| Over-Max Advance | $\text{overMaxAdvanceTime}_{t}$ | Excess beyond maxAdvance bound |
| Last End Time | $\text{lastEndTime}$ | Deadline by which task must end |
| Earliest End Time | $\text{earliestEndTime}$ | Earliest time task is allowed to end |
| On Time | $\text{onTime}_{t}$ | Indicator: meets both time bounds |
| Not On Time | $\text{notOnTime}_{t}$ | Indicator: violates time bounds |
| Switch | $\text{switch}_{e,t_1,t_2}$ | Binary: executor $e$ transitions $t_1 \to t_2$ |
| Switch Time | $\text{switchTime}_{t_1,t_2}$ | Masked time gap between consecutive tasks |
| Front-Of | $\text{frontOf}_{t_1,t_2}$ | Binary: task1 starts before task2 |
| Between-In | $\text{betweenIn}_{t_3,t_1,t_2}$ | Binary: task3 is between task1 and task2 |
| Makespan | $\text{makespan}$ | Maximum estimated end time |
| Threshold Slack | - | Penalizes only excess beyond threshold |
| Column Generation | - | Iterative adding/removing of columns |
| Shadow Price | - | Dual value for CG pricing |
| Reduced Cost | - | Cost minus shadow price contributions |
| Pipeline | - | A constraint or objective in the model |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Slack-based delay/advance modeling | Direct deviation variables | SlackFunction naturally decomposes positive/negative deviation, supports threshold slack | - |
| Masking for task cancellation | Separate constraint handling | Canceled tasks contribute zero to all time quantities, simplifying modeling | - |
| Threshold slack pattern | Fixed penalty | All minimization objectives support optional threshold for flexibility | - |
| SolverTimeWindowBoundary adapter | Inline conversion logic | Centralizes business-time to solver-value conversions | - |
| Pluggable end-time calculator | Hardcoded computation logic | Injected function supports different task-duration logic | - |
| Dual compilation paths | Unified compilation interface | TaskCompilation for one-shot, IterativeTaskCompilation for column generation | - |
| Pipeline architecture | Monolithic model building | Composable registration of constraints and objectives | - |
| FrontOf/BetweenIn ordering | Direct time-difference modeling | Temporal ordering via IfFunction and AndFunction | - |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1.0 | Initial version | Establish task compilation domain model |
