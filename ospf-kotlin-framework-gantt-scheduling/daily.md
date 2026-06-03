# Gantt Scheduling 泛型化计划

日期：2026-05-21

## 1. 当前状态

`ospf-kotlin-framework-gantt-scheduling` 是当前 framework 系列中固定 `Flt64` 最多的模块。扫描结果显示：

| 子模块 | 固定数值残留概况 |
|--------|------------------|
| `gantt-scheduling-infrastructure` | `TimeWindow` 将 `Instant` / `Duration` 映射为 `Flt64` |
| `gantt-scheduling-domain-task-context` | `Cost`、`ShadowPriceMap`、`TaskBunch` 固定 `Flt64` |
| `gantt-scheduling-domain-task-compilation-context` | 大量 `MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>`、`Flt64` 结果抽取 |
| `gantt-scheduling-domain-bunch-compilation-context` | 聚合、列生成、solution analyzer 固定 `Flt64` |
| `gantt-scheduling-domain-bunch-generation-context` | label/shadow price/reduced cost 固定 `Flt64` |
| `gantt-scheduling-domain-capacity-scheduling-context` | capacity、column、compilation 固定 `Flt64` |
| `gantt-scheduling-domain-resource-context` | resource usage、slack、capacity constraint 固定 `Flt64` |
| `gantt-scheduling-domain-produce-context` | produce/consumption/slack/limits 固定 `Flt64` |
| `gantt-scheduling-application` | branch-and-price、column generation、iteration 状态固定 `Flt64` |

结论：这个模块不是简单字段替换，而是需要把“领域数量值 V”和“solver 数值 Flt64”分层。

## 2. 泛型化目标

1. 领域层的时间、数量、成本、资源用量、产出消耗等有量纲字段支持 `Quantity<V>`。
2. solver/model 层可以继续用 `Flt64`，但必须集中在 adapter 或算法边界。
3. `AbstractLinearMetaModel<Flt64>` 不再从 application/service API 向领域对象泄漏。
4. `Cost<V>`、`ShadowPriceMap<V>`、`CapacityColumn<V>`、`SlotBasedCapacityResult<V>` 等结果模型可支持 Flt64/FltX。
5. 保留 Flt64 typealias 和 wrapper，确保现有应用能逐步迁移。

## 3. 物理量化硬规则

Gantt Scheduling 不能只做数值泛型化，还要把有量纲字段 `Quantity<V>` 化：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 时间点/时间跨度 | `start`, `end`, `duration`, `timeWindow` 映射值 | `Time` |
| 产能 | `capacity`, `availableCapacity`, `executorCapacity` | `Amount / Time`、`Time` 或业务定义单位 |
| 资源用量 | `resourceUsage`, `quantity`, `overQuantity`, `lessQuantity` | 对应资源单位 |
| 产出/消耗 | `produce`, `consumption`, `demand` | `Amount` 或业务物料单位 |
| 成本 | `cost`, `objective`, `penalty` | `Currency` 或业务成本单位 |

裸 `V` 只用于无量纲量：相对改善率、利用率、折扣/权重、归一化 reduced cost、排序评分。

## 4. ProductivityCalendar 物理量化目标

`ProductivityCalendar` 当前的泛型核心是 `Q`：

```kotlin
sealed class ProductivityCalendar<Q, P, T, U>(...)
```

其中 `Q` 表示产出数量或单位时间产出数量，`Productivity.unitYields`、`actualTimeFrom(quantity)`、`actualTimeUntil(quantity)`、`actualQuantity()` 都直接使用裸 `Q`。这只完成了数值类型泛型化，没有完成业务量纲建模。

目标是把 `ProductivityCalendar` 的产出数量从裸 `Q` 推进为 `Quantity<V>`：

```kotlin
sealed class ProductivityCalendar<V, P, T, U>(...)
    where V : RealNumber<V>, ...
```

建议目标语义：

1. `Productivity.unitYields: Map<U, Quantity<V>>`，表达“单位时间产出多少带单位数量”。
2. `Productivity.capacityOf(material)` 保持返回 `Duration?`，表达“生产一个业务单位或指定基准数量所需时间”。
3. `Productivity.unitYieldOf(material)` 返回 `Quantity<V>?`。
4. `actualTimeFrom(material, startTime, quantity)` 的 `quantity` 改为 `Quantity<V>`。
5. `actualTimeUntil(material, endTime, quantity)` 的 `quantity` 改为 `Quantity<V>`。
6. `actualQuantity(material, time)` 返回 `Quantity<V>`。
7. `averageUnitYield` 返回 `Map<U, Quantity<V>>`。
8. 只有内部时间比例、利用率、进度比例、floor/ceil 计算可以临时使用裸 `V` 或 `Flt64`。

### 4.1 建模事项

| 事项 | 当前状态 | 目标 |
|------|----------|------|
| 数量入参 | `quantity: Q` | `quantity: Quantity<V>` |
| 实际产量返回 | `Q` | `Quantity<V>` |
| 单位时间产出 | `unitYields: Map<U, Q>` | `unitYields: Map<U, Quantity<V>>` |
| 平均单位时间产出 | `Map<U, Q>` | `Map<U, Quantity<V>>` |
| 时间耗量 | `capacities: Map<U, Duration>` | 短期保留，后续评估是否升级为 `Map<U, Pair<Quantity<V>, Duration>>` |
| 条件产出 | `conditionUnitYields: List<Pair<Condition<T>, Q>>` | `List<Pair<Condition<T>, Quantity<V>>>` |
| 离散/连续日历 | `UInt64` / `Flt64` | `Quantity<UInt64>` / `Quantity<Flt64>` 或 `Quantity<FltX>` |

### 4.2 迁移计划

#### Phase P0：兼容层

新增兼容 typealias 或 wrapper，保留旧调用：

```kotlin
typealias LegacyDiscreteProductivityCalendar<P, T, U> = DiscreteProductivityCalendar<P, T, U>
typealias LegacyContinuousProductivityCalendar<P, T, U> = ContinuousProductivityCalendar<P, T, U>
```

同时新增带物理量语义的新入口，避免一次性破坏现有应用：

```kotlin
class QuantityProductivityCalendar<V, P, T, U>(...)
```

验收：

- [ ] 旧 `ProductivityCalendar` 测试不变。
- [ ] 新 `QuantityProductivityCalendar` 可用 `Quantity<Flt64>` 和 `Quantity<FltX>` 构造。

#### Phase P1：Productivity 数量字段升级

将 `Productivity` 中和产出数量有关的字段升级为 `Quantity<V>`：

```kotlin
val unitYields: Map<U, Quantity<V>>
val conditionUnitYields: List<Pair<ProductivityCondition<T>, Quantity<V>>>
```

`capacities: Map<U, Duration>` 暂时保留，用于兼容“生产一个单位所需时间”的语义。

验收：

- [ ] `unitYieldOf(material)` 返回 `Quantity<V>?`。
- [ ] `averageUnitYield` 可正确保留单位。
- [ ] 不同单位混算时返回失败或显式拒绝，不做静默相加。

#### Phase P2：计算 API 升级

升级公开 API：

```kotlin
fun actualTimeFrom(
    material: T,
    startTime: Instant,
    quantity: Quantity<V>,
    ...
): ActualTime

fun actualTimeUntil(
    material: T,
    endTime: Instant,
    quantity: Quantity<V>,
    ...
): ActualTime

fun actualQuantity(
    material: T,
    time: TimeRange,
    ...
): Quantity<V>
```

验收：

- [ ] `actualTimeFrom` 能按 `Quantity<Flt64>` 计算完成时间。
- [ ] `actualQuantity` 返回值携带与输入生产率一致的单位。
- [ ] 离散产量可用 `Quantity<UInt64>` 或明确的离散 wrapper 表达。

#### Phase P3：TimeWindow 比例计算隔离

`TimeWindow.valueOf(duration)` 仍可返回裸数值比例，但该比例只能作为内部换算因子使用：

```kotlin
val produced = unitYield * timeWindow.valueOf(produceTime.duration)
```

这里 `unitYield` 是 `Quantity<V>`，乘以无量纲比例后仍为 `Quantity<V>`。

验收：

- [ ] `TimeWindow` 不负责产出单位。
- [ ] `ProductivityCalendar` 内部不把 `Quantity<V>` 拆成裸 `V` 后丢弃单位。

#### Phase P4：旧 API 收敛

在所有使用方迁移后：

1. 裸 `Q` 版本降级为 legacy wrapper。
2. 新主路径统一使用 `Quantity<V>`。
3. `DiscreteProductivityCalendar` / `ContinuousProductivityCalendar` 明确表达为数量域选择，而不是裸数值选择。

验收：

- [ ] `ProductivityCalendar` 主 API 不再暴露裸数量 `Q`。
- [ ] `Quantity<V>` 单位检查覆盖 `unitYields`、`actualQuantity`、`averageUnitYield`。
- [ ] APS 等上层可以直接传入 `Quantity<FltX>` 产能数量。

### 4.3 风险与约束

| 风险 | 说明 | 缓解 |
|------|------|------|
| `Quantity<V>` 运算能力不足 | 乘除无量纲比例、floor/ceil 可能缺少统一接口 | 在 `QuantityProductivityCalculator` 或 adapter 中集中实现 |
| 离散数量舍入 | `Quantity<UInt64>` 与 `Quantity<Flt64>` 的舍入语义不同 | 保留离散/连续策略对象，不在 `Quantity` 内隐式舍入 |
| 单位换算 | 不同产出单位不能直接相加 | 单位不一致时返回失败，或要求调用方先显式换算 |
| 兼容成本 | 当前测试大量使用裸 `UInt64` / `Flt64` | 先新增新 API，旧 API 保留 wrapper |

## 5. 分层策略

### 5.1 领域数值层

新增统一约束：

```kotlin
typealias SchedulingValue<V> = V
```

具体接口使用显式泛型：

```kotlin
data class CostItem<V : RealNumber<V>>(...)
interface Cost<V : RealNumber<V>> { val sum: Quantity<V>? }
data class CapacityColumn<E, A, V : RealNumber<V>>(...)
```

### 5.2 时间映射层

`TimeWindow` 当前把时间映射为 `Flt64`。改为输出时间量纲的 `Quantity<V>`，solver 边界再做数值转换：

```kotlin
class TimeWindow<V : RealNumber<V>>(
    val window: TimeRange,
    val durationUnit: DurationUnit,
    val constants: RealNumberConstants<V>,
    val fromDouble: (Double) -> V,
    val toDouble: (V) -> Double
)
```

保留：

```kotlin
typealias Flt64TimeWindow = TimeWindow<Flt64>
```

验收：

- [ ] `TimeWindow<Flt64>` 行为与旧实现一致，但领域层返回时间 `Quantity<Flt64>`。
- [ ] `TimeWindow<FltX>` 可编译。
- [ ] 时间到 solver 的转换只在边界发生。

### 5.3 Solver adapter 层

新增或统一：

- `SchedulingSolverValueAdapter<V>`
- `Flt64SchedulingSolverValueAdapter`
- `SchedulingModelBoundary<V>`

职责：

1. 领域 `V` -> solver `Flt64`。
2. solver solution `Flt64` -> 领域 `V`。
3. 控制舍入、非有限值、溢出和精度损失。

## 6. 改造步骤

### Phase G0：基线与门禁

建立扫描门禁：

```powershell
git grep -n "Flt64\\|AbstractLinearMetaModel<Flt64>\\|MetaModel<Flt64>\\|LinearIntermediate.*Flt64" -- ospf-kotlin-framework-gantt-scheduling
```

验收：

- [ ] 记录每个子模块当前残留数量。
- [ ] 明确允许保留的 Flt64 文件清单。

### Phase G1：基础领域类型泛型化

优先处理不依赖 solver 的领域类型：

1. `Cost.kt` -> `Cost<V>`。
2. `TaskBunch.kt` -> cost 聚合泛型化。
3. `CapacityColumn.kt` -> `CapacityColumn<E, A, V>`。
4. `SlotBasedCapacityResult.kt` -> `SlotBasedCapacityResult<M, R, V>`。
5. produce/resource/capacity 的数量 Map 从 `Map<*, Flt64>` 改为 `Map<*, V>`。

验收：

- [ ] 领域 model 子模块可单独编译。
- [ ] Flt64 typealias 保留旧调用。

### Phase G2：TimeWindow 泛型化

改造 `gantt-scheduling-infrastructure/TimeWindow.kt`。

验收：

- [ ] `Flt64TimeWindow` 兼容旧 API。
- [ ] 所有调用点显式选择 `V`。

### Phase G2.5：ProductivityCalendar 数量物理量化

处理 `gantt-scheduling-infrastructure/WorkingCalendar.kt` 中的：

1. `Productivity<Q, T, U>`。
2. `ProductivityCalendar<Q, P, T, U>`。
3. `DiscreteProductivityCalendar`。
4. `ContinuousProductivityCalendar`。
5. `WorkingCalendarTest.testProductivityCalendarActualTime` 相关测试。

策略：

1. 先新增 `QuantityProductivity` / `QuantityProductivityCalendar` 新路径。
2. 再把 `unitYields`、`conditionUnitYields`、`actualTimeFrom(quantity)`、`actualTimeUntil(quantity)`、`actualQuantity()` 迁移到 `Quantity<V>`。
3. 保留旧裸数量 API 作为 wrapper，内部将裸数量包装为 `Quantity<V>`，单位由调用方或默认兼容单位提供。

验收：

- [ ] 旧 `ProductivityCalendar` 测试通过。
- [ ] 新增 `QuantityProductivityCalendarTest`，覆盖 `Quantity<Flt64>` 和 `Quantity<FltX>`。
- [ ] `actualQuantity` 返回带单位结果。
- [ ] 单位不一致的 `unitYields` 聚合有明确失败路径或显式拒绝策略。

### Phase G3：task/bunch compilation 泛型化

处理：

1. `Aggregation.kt`
2. `IterativeAggregation.kt`
3. `Compilation.kt`
4. `TaskTime.kt`
5. `BunchCompilationContext.kt`
6. solution analyzer

策略：

1. 领域输入输出用 `V`。
2. 建模内部用 `Flt64` 时包进 `SchedulingModelBoundary<V>`。
3. 不让 `MetaModel<Flt64>` 出现在领域接口。

验收：

- [ ] task compilation Flt64 路径行为不变。
- [ ] `V = FltX` 的领域对象可构造和进入 adapter。

### Phase G4：capacity/resource/produce 上下文泛型化

处理：

1. `CapacitySchedulingContext`
2. `CapacityCompilation`
3. `CapacitySchedulingProduce`
4. `CapacitySchedulingResourceUsage`
5. produce/resource constraints and objectives

验收：

- [ ] `quantity`, `overQuantity`, `lessQuantity`, `capacity`, `produce`, `consumption` 全部泛型化。
- [ ] 目标函数系数进入模型前集中转换为 `Flt64`。

### Phase G5：application 算法泛型化

处理：

1. `Iteration` 状态。
2. `BranchAndPriceAlgorithm`。
3. `ColumnGenerationAlgorithm`。
4. APS/LSP/MPS 应用入口。

验收：

- [ ] 算法公开 API 支持 `V`。
- [ ] Flt64 旧入口保留为 wrapper。
- [ ] reduced cost、shadow price、objective 全链路类型一致。

### Phase G6：测试与回归

建议补齐：

- `CostGenericTest`
- `TimeWindowGenericTest`
- `CapacityColumnGenericTest`
- `TaskCompilationFlt64CompatibilityTest`
- `BranchAndPriceFlt64CompatibilityTest`

最终命令：

```powershell
mvn -pl ospf-kotlin-framework-gantt-scheduling test
```

## 7. 风险

| 风险 | 说明 | 缓解 |
|------|------|------|
| solver 只接受 Flt64 | 领域 V 无法直接入模 | adapter 集中转换 |
| reduced cost/shadow price 类型分裂 | 列生成跨多个上下文 | 先统一 `Cost<V>` / `ShadowPriceMap<V>` |
| 时间映射精度 | Instant/Duration 到 V 可能损失精度 | `fromDouble` / `toDouble` 策略显式化 |
| 改动面大 | 126 个 Kotlin 文件 | 按子模块分阶段，每阶段保持 Flt64 wrapper 编译 |
| ProductivityCalendar 单位语义迁移 | 裸 Q 迁移到 `Quantity<V>` 会影响排程耗时和产量计算 | 新增 `QuantityProductivityCalendar` 后迁移调用方，旧 API wrapper 兼容 |

## 9. 实际进展状态（2026-06-02）

### G0 状态：完成

- G0-baseline-report.md 已包含可复现的扫描命令和允许保留文件清单
- 门禁命令可复现
- G0 是历史基线，不随 G1 新增的 Flt64 兼容 wrapper/typealias 重新定义

### G1 状态：完成

当前扫描值：

- `KtMain=129`
- `Flt64=1268`
- `SolverLeak=349`

**已完成：**

- [x] Cost.kt → `Cost<V : RealNumber<V>>`，含 Flt64 typealias
- [x] TaskBunch.kt → `AbstractTaskBunch<..., V : RealNumber<V>>`，Flt64 旧路径保留
- [x] CapacityColumn.kt → `CapacityColumn<E, A, V : RealNumber<V>>`，含 Flt64 typealias
- [x] SlotBasedCapacityResult.kt → `SlotBasedCapacityResult<A, M, R, V>`，含 Flt64 typealias
- [x] CapacityActionProduce.kt → `CapacityActionProduce<P, C, V>`，`produce/consumption` 为 `Map<*, V>`
- [x] ProductionTask.kt → `ProductionTask<..., V>`，`produce/consumption` 为 `Map<*, V>`，Flt64 兼容读取集中到 helper
- [x] produce quantity constraints 不再直接强转领域数量 Map 为 `Map<*, Flt64>`
- [x] SlotBasedBunchGenerator shadow price 入参泛型化为 `Map<T, V>`
- [x] reducedCost 扩展函数返回 `V`，内部 Flt64 solver 边界通过 `Flt64ValueConverter` 转回领域数值
- [x] 11 个 gantt-scheduling reactor 模块完整 `test` 通过
- [x] Flt64 typealias 和 Flt64 旧调用路径保留

**仍保留的 Flt64 边界：**

- `AbstractGanttSchedulingShadowPriceMap` 继承 framework `AbstractShadowPriceMap`，framework shadow price API 当前固定 `Flt64`
- `ProduceQuantityConstraint` / `ConsumptionQuantityConstraint` 内部 shadow price 汇总缓存仍为 `HashMap<*, Flt64>`，用于写回 `ShadowPrice`
- `SlotBasedCapacityPreSolver`、bunch compilation aggregation、application iteration / branch-and-price 中的 `Map<*, Flt64>` 属于 G3/G5 的 solver/application 链路
- `MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>` 等 solver 建模类型仍属于 G3+ 范围

**结论：G0 完成；G1 按“基础领域类型 + 领域数量 Map + shadow price/reduced cost 领域签名”的边界已完成。剩余 Flt64 不属于 G1，归入 solver/framework/application 后续阶段。**

### G2 状态：完成

- [x] TimeWindow.kt → TimeWindow<V : RealNumber<V>>，新增 fromDouble/toDouble 转换函数
- [x] Flt64TimeWindow typealias 保留旧 API
- [x] 所有下游 ~40 个文件统一使用 TimeWindow<Flt64>
- [x] WorkingCalendar.kt 中 6 处 TimeWindow 类型标注更新
- [x] TimeWindowTest.kt 21 个构造调用更新
- [x] 10 个 gantt-scheduling reactor 模块编译 + 测试通过

### G2.5 状态：完成

- [x] 新增 QuantityProductivity<V, T, U> — unitYields: Map<U, Quantity<V>>，携带物理单位
- [x] 新增 QuantityProductivityCalendar<V, P, T, U> sealed class — actualTimeFrom/Until/Quantity 使用 Quantity<V>
- [x] 新增 DiscreteQuantityProductivityCalendar (V=UInt64) 和 ContinuousQuantityProductivityCalendar (V=Flt64)
- [x] 旧 Productivity / ProductivityCalendar 保持不变，零下游消费者
- [x] 新增 testQuantityProductivityCalendarActualTime 测试覆盖离散和连续路径
- [x] 10 个 gantt-scheduling reactor 模块编译 + 测试通过

## 8. 向后兼容

建议保留：

```kotlin
typealias Flt64Cost = Cost<Flt64>
typealias Flt64CapacityColumn<E, A> = CapacityColumn<E, A, Flt64>
typealias Flt64SlotBasedCapacityResult<M, R> = SlotBasedCapacityResult<M, R, Flt64>
```

旧 application 入口保留为 Flt64 wrapper，新入口显式带 `<V>`。
