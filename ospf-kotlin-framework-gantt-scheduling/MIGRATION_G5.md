# G5 迁移说明

> G6 已开始把部分字段从 helper 迁移到 `Quantity<V>` 主存储。G5 文档保留为 helper 阶段的历史说明；字段级迁移请同时参考 `MIGRATION_G6.md`。

## 迁移目标

G5 的迁移策略是在不破坏旧 `Flt64` 源码调用的前提下，为新业务代码提供泛型 `V` 与 `Quantity<V>` 辅助入口。现有字段暂不直接替换为 `Quantity<V>`，避免构造器、DTO 和 solver 入模路径出现大面积兼容破坏。

## 推荐新写法

新代码优先使用泛型领域类型和 Quantity helper：

1. capacity：使用 `CapacityQuantity<V>`、`CapacityCostQuantity<V>`、`ProductionAction.unitCapacityQuantity`、`ProductionAction.unitCostQuantity`、`CapacityColumn.costQuantity`、`CapacitySchedulingAggregation.totalCapacityQuantity`。
2. resource：使用 `ResourceQuantity<V>`、`ResourceQuantityRange<V>`、`AbstractResourceCapacity.quantityRange`、`lessQuantity`、`overQuantity`、`Resource.initialQuantity`、`Resource.usedQuantityQuantity`。
3. produce：使用 `MaterialQuantity<V>`、`MaterialQuantityRange<V>`、`MaterialDemand.quantityRange`、`MaterialReserves.quantityRange`、`ProductionTask.produceQuantity`、`consumptionQuantity`、`AbstractTaskBunch.produceQuantityV`、`consumptionQuantityV`。
4. task/cost：使用 `CostQuantity<V>`、`CostItem.quantity`、`Cost.sumQuantity`。

## 兼容旧写法

旧 `Flt64` wrapper、typealias 和裸 `V` 字段继续保留。旧代码可以继续使用：

```kotlin
typealias Flt64Cost = Cost<Flt64>
typealias Flt64CapacityColumn<E, A> = CapacityColumn<E, A, Flt64>
typealias Flt64MaterialDemand = MaterialDemand<Flt64>
```

这些兼容入口用于平滑迁移，不建议作为新业务模型的默认写法。

## 保留边界

以下边界在 G5 继续固定为 `Flt64`：

1. solver 建模对象、符号、松弛变量、求解结果和 pre-solver 内部值。
2. branch-and-price `Iteration` 中的目标值、上下界和算法阈值。
3. framework shadow price 基础 API；业务侧通过 `reducedCost<V>` 和 adapter 转换隔离。
4. `ProductionAction.unitCost` 旧入口；泛型路径使用 `unitCostV(time, fromDouble)` 或 `unitCostQuantity(time, fromDouble)`。

## 字段替换条件

后续若将字段正式替换为 `Quantity<V>`，需要同时满足：

1. 提供旧构造器、工厂或 adapter，确保旧源码调用仍可编译。
2. 明确单位来源和默认单位策略，避免 `NoneUnit` 被误用为业务单位。
3. 更新 DTO、序列化、solution summary 和 demo4/example。
4. 保持 `flt64-scan-gate.ps1` 的 `unclassified=0`。
5. 通过 gantt-scheduling reactor 测试、example reactor 编译和 `git diff --check`。
