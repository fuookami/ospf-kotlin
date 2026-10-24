# 活动级有限域排程

[English](README.md) | 简体中文

本模块提供独立于任务束生成的活动级 MILP 注册路径。它复用 Gantt 的 `TaskId`、`ExecutorId`、`TimeRange` 与 `SchedulingSolverValueAdapter`，不依赖 Network，也不要求把项目、工序或联合资源活动转换为路径。

## 业务合同

- `ActivitySchedulingProblem<V>` 是输入快照，每个模型通过 `ActivitySchedulingContext.create` 创建独立注册态。
- `Activity` 包含替代 `ActivityMode`，每种模式声明完整的允许起点、时长、费用和资源占用段。起点集合是显式业务域，不是搜索预算。
- 每个活动恰好选择一个候选；只有声明 `cancellationCost` 才允许取消。无候选的必做活动形成不可行模型，不作为输入错误。
- `ActivityResourceUse` 可声明相对活动起点的占用段；同一活动可以同时占用设备、人员和工装，且各自开始/结束时间不同。相同资源上的重叠占用会相加。
- `ActivityResource` 声明具名容量、不可用时间和非重叠时变容量覆盖。可以直接传入已有 `WorkingCalendar.unavailableTimes`。具名人员可用容量一表达，技能组容量并不替代个体身份互斥。
- 资源约束在全部占用、不可用时间与容量变化事件之间装配，采用半开区间。不会把活动开始时可用误认为全程可用；不会隐式跨休息暂停。
- 零时长模式可表达里程碑。抢占、分批、自动恢复、顺序准备和完整人员法规尚未在本模块提供。

## 时序物料与库存

容量装配按资源一次分桶并扫描端点，只重算占用发生变化的候选；库存装配按物料一次分桶，
推进每个候选的事件前缀。两者保留输入 occurrence 的浮点累加顺序：移除或较早位置插入时，
重算受影响组而非通过减法或重结合更新。实际输出非零系数仍有成本；乱序或大量重叠的对抗组
仍可能重复进行局部折叠，不承诺这些场景全部变为线性复杂度。

`ActivityMaterial<V>` 声明具名物料、期初库存、最低库存（默认零）与可选仓储上限。`ActivityMode.materialFlows` 声明相对开始时刻的事件，正量入库、负量消耗，支持开始投料、完成产出和内部时点；偏移必须位于模式执行区间内。不同物料不会相互抵消，同一物料先换算为期初库存的计量单位。

```kotlin
val raw = ActivityMaterial("raw", Quantity(Flt64(2.0), Kilogram))
val consumeAtStart = ActivityMaterialFlow("raw", Quantity(Flt64(-500.0), Gram))
val produceAtFinish = ActivityMaterialFlow(
    material = "finished",
    quantity = Quantity(Flt64(1.0), Kilogram),
    at = 2.hours
)
// 将事件传入对应 ActivityMode，并在 problem.materials 中声明 raw 和 finished。
// Pass events to the mode and declare both materials in problem.materials.
```

`problem.materialEvents` 提供与选择无关的未来到货或需求。库存约束包括事件发生前的期初量和全部事件后的累计量，不允许未来产出补偿过去缺料；包含优化区间两端，同一时刻的产出与消耗原子净额结算，不约束该时刻内部的瞬时装卸顺序。若业务要求先入库再消耗或中间仓储峰值，应声明不同事件时刻或显式中间活动。

滚动重排的期初库存必须已反映优化起点前的执行历史，`Running`/`Completed` 不会自动再生成物料事件。执行中活动的剩余完成产出通过未来固定事件显式传入，防止重复投料或重复入库。

默认库存为硬约束。允许整单未服务时，可显式声明需求活动的 `cancellationCost`，对应取消量受该订单模式数量界限约束，并在结果中保留取消任务及罚费用；这不等同于任意数量缺口、连续欠料变量或自动替代物料。完整 BOM/MRP、部分交付、连续软缺口与物料配方推导仍未提供。

结果 `ActivitySchedule.materialInventory` 保留每种物料的期初量与事件后库存，独立复核从选中事件重放，不读取库存模型行。无活动但固定到货/需求违反库存边界仍形成正常不可行模型。

## 依赖与存在性

`ActivityDependency` 的 `from`/`to` 分别选择前后活动的 Start/Finish，覆盖 FS、SS、FF、SF；差值受 `minimum` 与可选 `maximum` 限制，允许负间隔。

存在性必须明确：

| 规则 | 语义 |
|---|---|
| `WhenBothPresent` | 仅双方都执行时限制时间 |
| `RequirePredecessor`（默认） | 后继执行则前驱必须执行 |
| `RequireBoth` | 两者共同执行或共同取消 |

同步时间关系不被强制为 DAG；相互矛盾的关系形成不可行模型。候选间不相容通过二元选择行表达，不使用无来源的大 M。

## 基线与执行事实

`ActivityBaseline` 支持冻结选择，以及改时间、改模式的一次性费用。非冻结基线可以位于新候选域之外；冻结基线必须仍有相同候选。取消只计算显式取消费用，不重复收取时间/模式变更费。

执行事实与待排活动的 `TaskId` 不得重叠：

- `Completed`：必须在优化起点之前完成，保留实际时间，用于后继依赖，不重复计算沉没成本。
- `Running`：实际开始不晚于优化起点，显式提供预计完成时间，资源占用只描述起点后的剩余部分。不会重新执行全部工作，也不会自行推导剩余工期。

冻结、执行事实和日历冲突会导致不可行，不会被默默放松。事实只按声明的活动时间及剩余占用校验，不宣称审计了优化区间以外的完整历史。

## 注册与独立求解

```kotlin
val context = ActivitySchedulingContext.create(
    problem = problem,
    adapter = schedulingSolverValueAdapter
)
// 对 Ret 做分支处理后：
context.value?.register(model)  // 嵌入其他主模型，不启动求解
```

正式调用应处理 `Ok`、`Failed` 与 `Fatal`。`register(model, minimizeCost = false)` 可由调用方组合目标；`aggregation.compilation.cost` 暴露本域费用表达式。

独立入口为 application 模块的 `ActivitySchedulingApplicationService<V>`：创建上下文、共享注册、调用注入的 solver、保留结构化终态、提取并复核。其使用现有 `ColumnGenerationSolver` 的 MILP 能力，不执行列生成。

## 扩展与数值边界

- `ActivityRow` 提供有单位的候选/取消系数及上下界，可表达覆盖、预算和额外配额。系数、引用和单位在创建上下文时统一校验。
- `additionalCost` 返回完整候选的额外费用，可注入交期、偏好或其他业务策略；在创建时计算一次并保存，不跨轮读取旧影子价格。
- `aggregation.variable(key)` 和 `cancellationVariable(task)` 允许下游追加模型约束；独立入口也有 `extra(context, model)` 装配钩子。
- 不同资源的不同量纲不会相加，同一资源先转换到声明容量单位。费用转换到 `costUnit`。仿射单位、非有限值、非零下溢为零和转换失败返回错误。
- 公共资源量和费用使用 `Quantity<V>`，时间使用有单位的 `Instant`/`Duration`，solver 归一化为 `Flt64`。这不是任意精度求解器；极端量级仍需业务缩放。

## 校验与结论范围

`validate(choiceKeys)` 不读取模型行，复核活动存在性、冻结、依赖、所有事件上的资源容量、库存与扩展行。缺席的可选活动按显式取消处理；未知/重复候选返回不可行诊断。

`extractSolution` 还检查上下文/模型归属、二元解、活动恰选一次及当前模型所有约束，包括调用方追加行。返回的 `ActivitySchedule` 保留执行模式、完整内部时间、执行事实、取消任务、费用和候选域。

solver 的最优状态**仅对声明的有限候选模型有效**，不证明连续时间域最优，也不代表原有束列生成已达到全局整数最优。超时无解、取消和不可行保留为正常结构化终态。

## 验证与下一阶段

默认测试无需原生 solver，包含柔性车间手工 oracle、资源受限项目、设备—人员分阶段同步、滚动恢复、四类时间关系、时变容量、额外覆盖行、时序库存的全部二元组合、同时点结算、显式整单取消，以及 `Flt64`/`FltX` 单位转换和下溢拒绝。application 的显式 `gantt-scip-integration-tests` profile 验证实际求解，原生不可用不得视作验证通过。

这是通用底座及硬库存时序的阶段性交付，不替代现有 task/resource/produce 未完成注册路径，也不宣称完成 APS/MPS/LSP 全业务。后续阶段继续提炼序列状态、维护、连续软缺口、人员和能耗等能力，并评估现有 Core CP 编译路线。
