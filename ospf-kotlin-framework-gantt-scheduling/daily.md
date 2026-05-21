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

## 4. 分层策略

### 3.1 领域数值层

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

### 3.2 时间映射层

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

### 3.3 Solver adapter 层

新增或统一：

- `SchedulingSolverValueAdapter<V>`
- `Flt64SchedulingSolverValueAdapter`
- `SchedulingModelBoundary<V>`

职责：

1. 领域 `V` -> solver `Flt64`。
2. solver solution `Flt64` -> 领域 `V`。
3. 控制舍入、非有限值、溢出和精度损失。

## 5. 改造步骤

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

## 6. 风险

| 风险 | 说明 | 缓解 |
|------|------|------|
| solver 只接受 Flt64 | 领域 V 无法直接入模 | adapter 集中转换 |
| reduced cost/shadow price 类型分裂 | 列生成跨多个上下文 | 先统一 `Cost<V>` / `ShadowPriceMap<V>` |
| 时间映射精度 | Instant/Duration 到 V 可能损失精度 | `fromDouble` / `toDouble` 策略显式化 |
| 改动面大 | 126 个 Kotlin 文件 | 按子模块分阶段，每阶段保持 Flt64 wrapper 编译 |

## 7. 向后兼容

建议保留：

```kotlin
typealias Flt64Cost = Cost<Flt64>
typealias Flt64CapacityColumn<E, A> = CapacityColumn<E, A, Flt64>
typealias Flt64SlotBasedCapacityResult<M, R> = SlotBasedCapacityResult<M, R, Flt64>
```

旧 application 入口保留为 Flt64 wrapper，新入口显式带 `<V>`。
