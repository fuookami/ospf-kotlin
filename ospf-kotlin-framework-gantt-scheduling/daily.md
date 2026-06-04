# Gantt Scheduling 泛型化计划

日期：2026-06-04（最后更新：2026-06-04 会话交接）

## 1. 总目标

`ospf-kotlin-framework-gantt-scheduling` 的长期目标是完成领域数值泛型化与物理量化，同时保持现有 `Flt64` 应用路径可逐步迁移。

核心目标：

1. 领域层的时间、数量、成本、资源用量、产出消耗等有量纲字段支持 `Quantity<V>`。
2. solver/model 层可以继续使用 `Flt64`，但 `Flt64` 必须集中在 adapter、model boundary 或算法边界。
3. `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>` 等建模类型不再从 application/service API 向领域对象泄漏。
4. `Cost<V>`、`ShadowPriceMap<V>`、`CapacityColumn<V>`、`SlotBasedCapacityResult<V>` 等领域结果模型支持 `Flt64` 与 `FltX`。
5. 保留 `Flt64` typealias、wrapper 和旧入口，确保现有应用可按阶段迁移。
6. 裸 `V` 只用于无量纲量，例如相对改善率、利用率、折扣、权重、归一化 reduced cost、排序评分。

物理量化原则：

| 类型 | 示例字段 | 目标表达 |
|------|----------|----------|
| 时间点/时间跨度 | `start`, `end`, `duration`, `timeWindow` 映射值 | `Quantity<V>` 或时间专用边界类型 |
| 产能 | `capacity`, `availableCapacity`, `executorCapacity` | `Quantity<V>` |
| 资源用量 | `resourceUsage`, `quantity`, `overQuantity`, `lessQuantity` | `Quantity<V>` |
| 产出/消耗 | `produce`, `consumption`, `demand` | `Quantity<V>` |
| 成本 | `cost`, `objective`, `penalty` | `Quantity<V>` 或明确的成本数值边界 |

## 2. 已完成事项

已完成：

1. G0 基线与门禁已完成，已有可复现扫描命令和允许保留的 `Flt64` 文件清单。
2. G1 基础领域类型泛型化已完成，核心成本、任务组、产能列、产能结果、产出/消耗数量签名已支持领域泛型，并保留 `Flt64` 兼容路径。
3. G2 `TimeWindow` 泛型化已完成，旧 `Flt64TimeWindow` 兼容路径保留，下游调用已显式选择 `TimeWindow<Flt64>`。
4. G2.5 `ProductivityCalendar` 数量物理量化已完成，新增 `QuantityProductivityCalendar` 路径，产量入参、产量返回值、平均单位产出已支持 `Quantity<V>`，并覆盖单位一致性校验。
5. 截至 G2.5，相关 gantt-scheduling reactor 模块编译与测试通过。
6. **3.1 Solver adapter 与 model boundary 收敛**已完成（commit `e1c387ed`）。新增 `SchedulingSolverValueAdapter<V>` 统一 V↔Flt64 转换边界，替换 9 处重复 `flt64Converter` 和 1 处 `resolveFlt64ValueConverter` 调用。
7. **3.2 Task/Bunch compilation 泛型化**已完成（commit `d451a6b6`）。Bunch 编译链 12 核心类 + 10 下游文件、Task 编译链 4 文件完成 `V : RealNumber<V>` 传播，solver 边界使用 `.toFlt64()`。`BunchSolution` 移除了 `out B`/`out V`（因 `AbstractTaskBunch<T,E,A,V>` 中 V 不变）。

仍允许暂时保留的边界：

1. framework shadow price 基础 API 当前仍固定 `Flt64`。
2. solver 建模对象内部仍可固定 `Flt64`。
3. application 层 iteration、branch-and-price、column generation 的旧路径仍可固定 `Flt64`。
4. 旧 API wrapper、typealias、兼容测试中的 `Flt64` 可继续保留。

## 3. 未完成事项

### 3.1 Solver adapter 与 model boundary 收敛 — ✅ 已完成

> commit `e1c387ed` | 11 files changed

已完成：新增 `SchedulingSolverValueAdapter<V>` 于 `gantt-scheduling-domain-task-context`，统一 `IntoValue<V>` + `roundSolution` + `floorToUInt64` + `floorValue`。替换 9 处 `private val flt64Converter` 为 `SchedulingSolverValueAdapter.Flt64`，替换 `ShadowPriceMap.kt` 中 `resolveFlt64ValueConverter` 为 `SchedulingSolverValueAdapter.create<V>()`。

### 3.2 Task/Bunch compilation 泛型化 — ✅ 已完成

> commit `d451a6b6` | 22 files changed

已完成：Bunch 编译链（BunchSolution、BunchAggregation、Compilation、TaskTime、Aggregation、BunchCompilationContext、SolutionAnalyzer 等 12 核心 + 10 下游）和 Task 编译链（IterativeAggregation、Compilation、IterativeContext、BranchAndPriceAlgorithm）全部添加 `V : RealNumber<V>`。`BunchSolution` 移除 `out B`/`out V`（因 `AbstractTaskBunch` 中 V 不变位）。solver 边界使用 `.toFlt64()`。

### 3.3 Capacity/Resource/Produce 上下文泛型化 — ✅ 已完成

> **完成状态（2026-06-04 会话 2+3+4+复核补漏）**：Capacity、Resource、Produce/Consumption 上下文已完成领域泛型化，solver 边界继续固定 `Flt64` 并在入模前统一 `.toFlt64()`。复核补齐 `MaterialDemand<V>` / `MaterialReserves<V>` 后，所有改动**已通过有效编译与测试验收、未提交**。
>
> 验收标准完成情况：
> - ✅ 产能、资源、产出/消耗的业务数量字段不再直接固定 `Flt64`
> - ✅ 目标函数系数进入模型前统一转换为 `Flt64`
> - ✅ shadow price 写回不绕过统一 adapter
> - ✅ `Flt64` 兼容路径编译验收通过
> - ⏳ 至少一个 `FltX` 或非默认 `V` 的领域构造测试通过（待 3.6 补齐）
>
> 已执行验收：
> - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-capacity-scheduling-context -am -DskipTests compile`
> - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context -am -DskipTests compile`
> - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context -am -DskipTests compile`
> - 显式列出 gantt-scheduling 全部子模块的 `-pl ... -am -DskipTests compile`
> - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am test`
> - `git diff --check -- ospf-kotlin-framework-gantt-scheduling`
>
> 下一步：
> 1. 提交 commit
> 2. 进入 3.4 Application 算法泛型化

#### 事项

改造产能、资源、产出/消耗上下文，使业务数量字段统一从裸 `Flt64` 推进到领域泛型或 `Quantity<V>`，目标函数和约束系数只在入模前转换为 `Flt64`。

#### 完成范围

- Capacity 上下文：`CapacitySchedulingContext`、`CapacityCompilation`、`CapacityOrderCompilation`、`IterativeCapacityCompilation`、`CapacityColumnAggregation`、`ProductionAction`、相关 limits 与 SlotBased 预求解路径。
- Resource 上下文：`Resource`、`StorageResource`、`ExecutionResource`、`ConnectionResource`、CapacityAction resource usage、resource capacity constraint、over/less quantity minimization。
- Produce/Consumption 上下文：`ProductionTask`、`MaterialDemand<V>`、`MaterialReserves<V>`、`Produce`、`Consumption`、CapacityScheduling produce 与 Plan/Bunch capacity produce、produce/consumption quantity limits。

#### 关键实现

1. `ValueRange<V>` 使用处补齐 `where V : RealNumber<V>, V : NumberField<V>`，solver 表达式仍保持 `Flt64`。
2. 领域值进入模型前统一 `.toFlt64()`，例如 quantity range、unit cost、unit operation time、resource usage、produce/consumption contribution。
3. 不再从类型参数直接访问 `V.constants.zero`；零值从已有 `V` 实例、`resourceQuantityZero(capacities)` 或 `TimeWindow.fromDouble(0.0)` 派生。
4. `ProductionTask.produceV()` / `consumptionV()` 保留泛型返回值，旧 `Flt64` `produce()` / `consumption()` 兼容扩展保留。
5. `ProductionAction` 保留 `TimeWindow<Flt64>` 兼容调用，通过 `asFlt64TimeWindow()` 隔离旧路径。

#### 设计决策记录

1. **移除 `out` 修饰符**：Resource 层级所有 `out` 被移除（C、V 都不变），因为 `AbstractTaskBunch<T,E,A,V>` 中 V 不变位，且 `AbstractResourceCapacity<V>` 中 V 不变位。
2. **solver 类型保持 Flt64**：`LinearExpressionSymbols*<Flt64>`、`LinearIntermediateSymbols*<Flt64>`、`MetaModel<Flt64>`、`LinearPolynomial<Flt64>` 全部保持。
3. **边界转换模式**：domain V 值入 solver 前调用 `.toFlt64()`，例如 `LinearMonomial(domainValue.toFlt64(), solverVariable)`、`LinearPolynomial(emptyList(), domainValue.toFlt64())`。
4. **领域零值来源**：领域零值不从类型参数直接取得，而是从已有 `V` 实例、`resourceQuantityZero(capacities)` 或 `TimeWindow.fromDouble(0.0)` 派生；solver 多项式常量仍使用 `Flt64.zero`。
5. **ResourceSlack.kt 不改**：solver 内部工具函数，threshold 和 value 都由调用方 `.toFlt64()` 后传入。

#### 验收标准

- [x] 产能、资源、产出/消耗的业务数量字段不再直接固定 `Flt64`。
- [x] 目标函数系数进入模型前统一转换为 `Flt64`。
- [x] shadow price 写回不绕过统一 adapter。
- [x] `Flt64` 兼容路径编译验收通过。
- [ ] 至少一个 `FltX` 或非默认 `V` 的领域构造测试通过（待 3.6 补齐）。

### 3.4 Application 算法泛型化

#### 事项

改造 application 层算法公开 API，使 iteration、branch-and-price、column generation、APS/LSP/MPS 入口支持领域泛型，并保留旧 `Flt64` wrapper。

#### 计划

1. 先收敛 application 层状态对象中的 cost、objective、shadow price、reduced cost 类型。
2. 将算法内部与 solver 交互的位置接入 `SchedulingModelBoundary<V>`。
3. 为旧入口保留 `Flt64` 默认 adapter。
4. 再迁移 APS/LSP/MPS 等上层入口。

#### 修改清单

- `Iteration` 状态对象
- `BranchAndPriceAlgorithm`
- `ColumnGenerationAlgorithm`
- APS 应用入口
- LSP 应用入口
- MPS 应用入口
- application 层 cost/objective/shadow price/reduced cost DTO

#### 验收标准

- [ ] 算法公开 API 支持 `V`。
- [ ] 旧 `Flt64` 入口作为 wrapper 保留且行为不变。
- [ ] reduced cost、shadow price、objective 全链路类型一致。
- [ ] application 层不直接向领域 API 泄漏 solver model 类型。
- [ ] APS/LSP/MPS 至少保留现有 `Flt64` 回归测试。

### 3.5 ShadowPriceMap 与 framework 固定 Flt64 边界处理

#### 事项

当前 gantt-scheduling 的 shadow price 仍受 framework 基础 API 固定 `Flt64` 影响，需要决定是上移泛型化 framework API，还是在 gantt-scheduling 内建立隔离 wrapper。

#### 计划

1. 先盘点 `AbstractShadowPriceMap` 及其调用方是否被其他 framework 模块共享。
2. 如果可控，优先泛型化 framework shadow price API。
3. 如果影响面过大，则在 gantt-scheduling 内提供 `SchedulingShadowPriceMap<V>` wrapper。
4. 将 shadow price 聚合缓存和写回路径接入统一转换边界。

#### 修改清单

- framework shadow price 基础接口或 gantt-scheduling wrapper。
- `AbstractGanttSchedulingShadowPriceMap`
- produce/resource/capacity 中 shadow price 聚合缓存。
- reduced cost 使用 shadow price 的计算点。

#### 验收标准

- [ ] gantt-scheduling 领域 API 可表达 `ShadowPriceMap<V>`。
- [ ] framework 其他模块不因 shadow price 调整产生行为回归。
- [ ] 旧 `Flt64` shadow price 入口保留。
- [ ] shadow price 与 reduced cost 的数值类型在同一条业务链路中一致。

### 3.6 测试、扫描门禁与最终收敛

#### 事项

补齐泛型化与物理量化回归测试，建立剩余 `Flt64` 的允许清单，确保后续新增代码不会重新把 solver 数值泄漏到领域 API。

#### 计划

1. 更新扫描命令，区分允许保留与需要清理的 `Flt64`。
2. 为每个完成阶段补齐兼容测试和至少一个非默认 `V` 构造测试。
3. 汇总最终允许保留文件清单。
4. 在所有使用方迁移后，将裸数量旧 API 降级为 legacy wrapper。

#### 修改清单

- `G0-baseline-report.md` 或新增最终扫描报告。
- `CostGenericTest`
- `TimeWindowGenericTest`
- `CapacityColumnGenericTest`
- `TaskCompilationFlt64CompatibilityTest`
- `BranchAndPriceFlt64CompatibilityTest`
- `QuantityProductivityCalendar` 相关回归测试扩展。
- 旧 API legacy wrapper 与 typealias 清单。

#### 验收标准

- [ ] 扫描结果中所有 `Flt64` 都能归类为 solver boundary、compat wrapper、test 或 legacy API。
- [ ] `MetaModel<Flt64>`、`AbstractLinearMetaModel<Flt64>` 不出现在领域 API。
- [ ] `Quantity<V>` 单位检查覆盖产能、资源、产出/消耗、产量日历关键路径。
- [ ] `mvn -pl ospf-kotlin-framework-gantt-scheduling test` 通过。
- [ ] 必要时执行包含依赖模块的 reactor 测试并通过。

## 4. 向后兼容要求

建议继续保留：

```kotlin
typealias Flt64Cost = Cost<Flt64>
typealias Flt64CapacityColumn<E, A> = CapacityColumn<E, A, Flt64>
typealias Flt64SlotBasedCapacityResult<M, R> = SlotBasedCapacityResult<M, R, Flt64>
```

旧 application 入口保留为 `Flt64` wrapper，新入口显式带 `<V>`。所有 wrapper 的目的只是兼容旧调用，不应成为新业务代码的主路径。

## 5. 风险与约束

| 风险 | 说明 | 缓解 |
|------|------|------|
| solver 只接受 `Flt64` | 领域 `V` 无法直接入模 | adapter/model boundary 集中转换 |
| shadow price 基础 API 固定 `Flt64` | framework 层可能限制 gantt-scheduling 泛型化 | 评估 framework 泛型化或局部 wrapper |
| reduced cost 类型分裂 | 列生成跨多个上下文 | 先统一 cost、shadow price、reduced cost 边界 |
| 时间映射精度 | `Instant`/`Duration` 到 `V` 可能损失精度 | `fromDouble`/`toDouble` 策略显式化 |
| 单位换算 | 不同业务单位不能直接相加 | 单位不一致时失败，要求调用方显式换算 |
| 改动面大 | 涉及多个 domain 与 application 子模块 | 按事项推进，每个事项保持 `Flt64` wrapper 编译 |
