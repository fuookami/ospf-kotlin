# G6 迁移说明

## 迁移目标

G6 将 G5 的 `Quantity<V>` helper 推进为字段级主表达。新业务代码优先使用 `Quantity<V>` 字段；旧裸 `V` 构造、属性和 `Flt64` typealias 继续作为迁移期兼容入口。

## 已切换为 Quantity 主存储

1. task/cost：
   - `CostItem.costQuantity` 成为成本项主字段。
   - `Cost.costSum` 成为总成本主字段。
   - `value`、`sum`、`quantity()`、`sumQuantity()` 继续可用。
2. capacity：
   - `CapacityColumn.columnCost` 成为列成本主字段。
   - `cost` 和 `costQuantity()` 继续可用。
3. resource：
   - `AbstractResourceCapacity.quantityRangeValue`、`lessQuantityValue`、`overQuantityValue` 成为容量主字段。
   - `quantity`、`lessQuantity`、`overQuantity` 和对应 helper 继续可用。
4. produce：
   - `MaterialDemand`、`MaterialReserves` 使用 `quantityRangeValue`、`lessQuantityValue`、`overQuantityValue` 作为主字段。
   - `ProductionTask` 新增 `produceQuantityByProduct` 与 `consumptionQuantityByMaterial` 默认入口。
5. bunch-compilation：
   - `SlotBasedCapacityResult` 使用 `totalCostQuantityValue`、`produceQuantityByProduct`、`consumptionQuantityByMaterial`、`resourceUsageQuantityByResource` 作为主字段。
   - `SlotConstraints` 使用各类 `*Quantity` map 作为主字段。

## 兼容旧写法

旧源码可以继续使用裸值构造和裸值属性，例如：

```kotlin
CostItem(tag = "setup", value = cost)
CapacityColumn(executor = executor, slotIndex = 0, order = 0, allocations = allocations, cost = cost)
MaterialDemand(quantity = range, lessQuantity = less, overQuantity = over)
ResourceCapacity(time = time, quantity = range)
```

推荐新写法使用主字段：

```kotlin
CostItem(tag = "setup", costQuantity = Quantity(cost, NoneUnit))
CapacityColumn(executor = executor, slotIndex = 0, order = 0, allocations = allocations, columnCost = Quantity(cost, NoneUnit))
MaterialDemand(quantityRangeValue = Quantity(range, NoneUnit))
ResourceCapacity(time = time, quantityRangeValue = Quantity(range, NoneUnit))
```

`SlotConstraints` 的裸值兼容入口以 companion `invoke` 提供，避免 JVM 泛型擦除导致构造器签名冲突。

## 单位策略

兼容裸值入口默认使用 `NoneUnit`。业务侧已知成本、产能、物料或资源单位时，应显式传入 `Quantity(value, unit)`，不要把 `NoneUnit` 当作业务单位。

## 保留边界

以下内容仍保持为明确边界：

1. solver 建模对象、符号、松弛变量、求解结果和 pre-solver 内部值继续固定 `Flt64`。
2. application `Iteration` 的目标值、上下界、reduced cost 和改善阈值继续归为 branch-and-price algorithm internal。
3. task/bunch compilation 的时间变量、模型表达式和 solution analyzer 仍依赖 solver `Flt64` 边界，后续需单独迁移 DTO 层。
4. framework shadow price 基础 API 继续固定 `Flt64`，领域侧通过 adapter 隔离。

## 验证

本阶段要求保持：

1. 字段级 `Quantity<FltX>` 构造测试通过。
2. 旧 `Flt64`/裸值构造测试继续通过。
3. demo4 同时覆盖旧裸值构造和新 `Quantity` 字段构造。
4. `flt64-scan-gate.ps1` 保持 `unclassified=0`。
