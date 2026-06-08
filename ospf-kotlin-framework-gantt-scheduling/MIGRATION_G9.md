# G9 迁移指南 / G9 Migration Guide

日期：2026-06-08（更新：G9 续轮内部读取迁移验证完成）

## 1. G9 范围

G9 在 G8 基础上推进两项工作：

1. 补齐 `TaskTime` 接口剩余的 `overMaxDelayTimeQuantity()` 和 `overMaxAdvanceTimeQuantity()` 物理量读取方法。
2. 为领域模型旧裸值兼容属性和旧构造函数/工厂标记 `@Deprecated`，引导调用方迁移到 `Quantity<V>` 主路径。

## 2. 新增 API

### 2.1 TaskTime 新增物理量方法

`TaskTime` 接口新增两个默认方法，签名与现有 6 个方法一致：

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `overMaxDelayTimeQuantity(task, model, adapter, unit)` | `TaskTimeQuantity<V>?` | 超最大延迟时间物理量 |
| `overMaxAdvanceTimeQuantity(task, model, adapter, unit)` | `TaskTimeQuantity<V>?` | 超最大提前时间物理量 |

迁移方式：直接使用新方法，无需额外适配。

## 3. 已标记 Deprecated 的入口

### 3.1 SlotBasedCapacityResult

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| 裸值构造函数 `SlotBasedCapacityResult(slot, slotIndex, actionAllocations, totalCost, produceByProduct, consumptionByMaterial, resourceUsageByResource)` | 主构造函数 + `Quantity(value, NoneUnit)` | 所有值字段改为 `Quantity<V>` |
| `val totalCost: V` | `totalCostQuantityValue.value` | |
| `val produceByProduct: Map<M, V>` | `produceQuantityByProduct` | 直接使用 `Map<M, Quantity<V>>` |
| `val consumptionByMaterial: Map<M, V>` | `consumptionQuantityByMaterial` | 直接使用 `Map<M, Quantity<V>>` |
| `val resourceUsageByResource: Map<R, V>` | `resourceUsageQuantityByResource` | 直接使用 `Map<R, Quantity<V>>` |

### 3.2 CapacityIntermediateValues

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| `fun produce(slot, product): V?` | `produceQuantity(slot, product)?.value` | 返回 `SlotQuantity<V>?` |
| `fun consumption(slot, material): V?` | `consumptionQuantity(slot, material)?.value` | 返回 `SlotQuantity<V>?` |
| `fun resourceUsage(slot, resource): V?` | `resourceUsageQuantity(slot, resource)?.value` | 返回 `SlotQuantity<V>?` |

### 3.3 SlotConstraints

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| 裸值工厂 `SlotConstraints(slot, slotIndex, maxProduce, minProduce, ...)` | 主构造函数 + `Quantity` 包装 | 所有 Map 值改为 `Quantity<V>` |
| `val maxProduce: Map<M, V>` | `maxProduceQuantity` | 直接使用 `Map<M, Quantity<V>>` |
| `val minProduce: Map<M, V>` | `minProduceQuantity` | |
| `val maxConsumption: Map<M, V>` | `maxConsumptionQuantity` | |
| `val minConsumption: Map<M, V>` | `minConsumptionQuantity` | |
| `val maxResourceUsage: Map<R, V>` | `maxResourceUsageQuantity` | |
| `val minResourceUsage: Map<R, V>` | `minResourceUsageQuantity` | |

## 4. 迁移策略

1. **IDE 自动替换**：大部分 `@Deprecated` 带有 `ReplaceWith`，可通过 IDE 快速替换。
2. **逐步迁移**：当前阶段所有旧入口仍可编译，`@file:Suppress("DEPRECATION")` 允许旧代码继续工作。
3. **最终移除条件**：当所有 example、application 调用方和 scan gate 均完成泛型/物理量路径验证后，旧入口将按 deprecated 到移除流程退出。

## 5. 剩余边界（不在 G9 deprecated 范围）

以下入口当前继续保留，不标记 deprecated：

1. solver 建模对象、solver 内部结果、pre-solve 模型继续固定 `Flt64`。
2. framework shadow price 基础 API 继续固定 `Flt64`，通过 adapter 与泛型 helper 隔离。
3. `Flt64` typealias 继续保留作为迁移桥；已迁移领域模型的 typealias 会逐步标记 deprecated。
4. `ProductionAction.unitCapacity`、`unitCost`、`upperBound` 保留 `Flt64` 签名作为 solver 边界入口；业务侧泛型路径使用 `unitCapacityQuantity<V>` 或 `unitCostQuantity<V>`。
5. application branch-and-price 内部迭代状态仍以 `Flt64` 驱动。
6. `MetaModel<Flt64>` 参数继续用于 `register()` 和 `*Quantity()` 方法（属于 solver boundary）。

## 6. G9 续轮新增 Deprecated（2026-06-08）

### 6.1 Cost（task-context）

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| `CostItem` 裸值构造函数 `(tag, value: V?, message)` | 主构造函数 `(tag, costQuantity: CostQuantity<V>?, message)` | 内部包装 `Quantity(value, NoneUnit)` |
| `val CostItem.value: V?` | `costQuantity?.value` | 裸值派生属性 |
| `Cost.Companion.invoke(items, constants, sums: V?)` | `Cost(items, costSum: CostQuantity<V>?)` | 裸值工厂方法 |
| `val Cost.sum: V?` | `costSum?.value` | 裸值派生属性 |
| `MutableCost` 裸值构造函数 `(constants, items, sum: V?)` | 主构造函数 `(constants, items, costSum: CostQuantity<V>?)` | |
| `ImmutableCost` 裸值构造函数 `(items, sum: V?)` | 主构造函数 `(items, costSum: CostQuantity<V>?)` | |
| `typealias Flt64CostItem` | `CostItem<Flt64>` | |
| `typealias Flt64Cost` | `Cost<Flt64>` | |
| `typealias Flt64MutableCost` | `MutableCost<Flt64>` | |
| `typealias Flt64ImmutableCost` | `ImmutableCost<Flt64>` | |

### 6.2 CapacityColumn（capacity-context）

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| 裸值构造函数 `(executor, slotIndex, order, allocations, cost: V)` | 主构造函数 + `columnCost: CapacityCostQuantity<V>` | 内部包装 `Quantity(cost, NoneUnit)` |
| `val cost: V` | `columnCost.value` | 裸值派生属性 |
| `typealias Flt64CapacityColumn<E, A>` | `CapacityColumn<E, A, Flt64>` | |

### 6.3 AbstractResourceCapacity / ResourceCapacity / Resource（resource-context）

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| `val quantity: ValueRange<V>` | `quantityRangeValue.value` | 裸值派生属性 |
| `val lessQuantity: V?` | `lessQuantityValue?.value` | 裸值派生属性 |
| `val overQuantity: V?` | `overQuantityValue?.value` | 裸值派生属性 |
| `ResourceCapacity` 裸值构造函数 `(time, quantity: ValueRange<V>, lessQuantity: V?, overQuantity: V?)` | 主构造函数 + `Quantity` 包装 | |
| `abstract val initialQuantity: V` | `initialQuantity(unit)` 返回 `Quantity<V>` | |
| `abstract fun usedQuantity(bunch, time): V` | `usedQuantityQuantity(bunch, time)` 返回 `Quantity<V>` | |

### 6.4 MaterialDemand / MaterialReserves（produce-context）

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| `MaterialDemand` 裸值构造函数 `(quantity: ValueRange<V>, lessQuantity: V?, overQuantity: V?)` | 主构造函数 + `Quantity` 包装 | |
| `val quantity: ValueRange<V>` | `quantityRangeValue.value` | |
| `val lessQuantity: V?` | `lessQuantityValue?.value` | |
| `val overQuantity: V?` | `overQuantityValue?.value` | |
| `MaterialReserves` 裸值构造函数 | 同 `MaterialDemand` 模式 | |
| `typealias Flt64MaterialDemand` | `MaterialDemand<Flt64>` | |
| `typealias Flt64MaterialReserves` | `MaterialReserves<Flt64>` | |

### 6.5 ProductionTask（produce-context）

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| `val produce: Map<P, V>` | `produceQuantityByProduct` | `Map<P, Quantity<V>>` |
| `val consumption: Map<C, V>` | `consumptionQuantityByMaterial` | `Map<C, Quantity<V>>` |
| `typealias Flt64ProductionTask<E, A, P, C>` | `ProductionTask<E, A, P, C, Flt64>` | |

### 6.6 ProductionAction（capacity-context）

| 旧入口 | 替代 API | 说明 |
|--------|----------|------|
| `unitCapacityV(timeWindow)` | `unitCapacityQuantity(timeWindow).value` | 裸 `V` helper 仅作为迁移期兼容入口 |
| `unitCostV(time, fromDouble)` | `unitCostQuantity(time, fromDouble).value` | 裸 `V` helper 仅作为迁移期兼容入口 |

`unitCapacity(timeWindow: TimeWindow<Flt64>)`、`unitCost(time: Instant)` 和 `upperBound(slot, timeWindow: TimeWindow<Flt64>)` 仍保留为 solver/model boundary，不在本轮 deprecated 范围内。

### 6.7 内部读取迁移

本轮同步减少 main 代码对 deprecated 裸值属性的自调用：

| 范围 | 旧读取 | 新读取 |
|------|--------|--------|
| task/bunch/capacity compilation | `cost.sum`、`column.cost` | `cost.costSum?.value`、`column.columnCost.value` |
| produce context | `demand.quantity`、`lessQuantity`、`overQuantity` | `quantityRangeValue.value`、`lessQuantityValue?.value`、`overQuantityValue?.value` |
| production task | `task.produce`、`task.consumption` | `produceQuantityByProduct`、`consumptionQuantityByMaterial` |
| resource context | `resourceCapacity.quantity`、`lessQuantity`、`overQuantity` | `quantityRangeValue.value`、`lessQuantityValue?.value`、`overQuantityValue?.value` |
| resource internals | `initialQuantity`、`usedQuantity(bunch, time)` | `initialQuantity().value`、`usedQuantityQuantity(bunch, time).value` |

## 7. 本轮验证

2026-06-08 12:19 已完成以下验证：

1. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test`
2. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile`
3. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1`
4. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example`

scan gate 基线保持 `main=1,402 / test=204 / total=1,606 / unclassified=0`。
