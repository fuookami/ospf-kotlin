# G8 迁移说明

## 迁移目标

G8 将时间/日历、capacity result、pre-solver result、task/makespan/switch、resource/produce/consumption solved value 的对外读取推进到 `Quantity<V>` 或泛型 DTO。solver/model 内部仍固定 `Flt64`，领域调用方优先通过 adapter helper 读取泛型结果。

## 推荐新写法

1. 时间与日历：
   - 使用 `WorkingCalendar.ActualTime.durationQuantity`、`workingDurationQuantity`、`breakDurationQuantity`、`connectionDurationQuantity`。
   - 使用 `WorkingCalendar.ValidTimes.durationQuantity`、`breakDurationQuantity`、`connectionDurationQuantity`。
   - 使用 `TaskTime.estimateStartTimeQuantity`、`estimateEndTimeQuantity`、`delayTimeQuantity`、`advanceTimeQuantity`、`delayLastEndTimeQuantity`、`advanceEarliestEndTimeQuantity`。
   - 使用 `Makespan.quantity` 和 `Switch.switchTimeQuantity`。
2. capacity 与 pre-solver：
   - 使用 `ActionAllocation.durationQuantity`、`ExecutorCapacityResult.totalDurationQuantity`。
   - 使用 `SlotBasedCapacityResult<Flt64>.toGeneric(adapter)` 和 `CapacityIntermediateValues<Flt64>.toGeneric(adapter)`。
   - 使用 `SlotBasedCapacityPreSolver.solveGeneric` 或 `extractIntermediateValuesGeneric` 读取泛型中间结果。
3. resource/produce/consumption：
   - 使用 `ResourceUsage.solvedQuantity`、`solvedOverQuantity`、`solvedLessQuantity`。
   - 使用 `Produce.solvedQuantity`、`solvedOverQuantity`、`solvedLessQuantity`。
   - 使用 `Consumption.solvedQuantity`、`solvedOverQuantity`、`solvedLessQuantity`。

## 兼容旧写法

旧 `Flt64` typealias、wrapper、裸值字段和 solver-only API 继续保留。旧代码可以继续使用 `Duration`、`Flt64`、`Flt64CapacityIntermediateValues`、`Flt64SlotBasedCapacityResult` 以及直接读取 solver symbol 的方式。

这些入口只作为迁移期兼容层；新业务代码应优先走 `Quantity<V>` 和 adapter helper。

## deprecated 候选

下一轮优先处理：

1. 旧裸值 helper：已存在 `Quantity<V>` 主字段或 helper 的裸值读取方法。
2. 旧 wrapper/typealias：仅用于 Flt64 legacy 调用的别名。
3. solver-only service API：要求外部直接传入 `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`List<Flt64>` 的结果读取入口。
4. DTO 裸值入口：已有 `Quantity<V>` 主字段的构造或 companion 兼容入口。

deprecated 标记应分批进行，并为每个入口提供替代 API 或迁移说明，避免一次性破坏源码兼容。

## 保留边界

以下内容在 G8 后仍保留为明确边界：

1. solver 建模对象、符号、变量、约束、求解 token 与内部结果。
2. branch-and-price 算法内部目标值、上下界、reduced cost 与阈值。
3. pre-solver 入模、求解器回调和初始列仍使用 `Flt64`，输出可通过泛型 adapter 转换。
4. shadow price 基础 API 仍固定 `Flt64`，领域侧通过 adapter helper 隔离。

## 验证

本阶段要求保持：

1. `Quantity<FltX>` 时间/日历、capacity result、resource/produce solved quantity 测试通过。
2. demo4 覆盖新 helper 和旧路径编译。
3. gantt-scheduling reactor 测试通过。
4. example reactor 编译通过。
5. `flt64-scan-gate.ps1` 保持 `unclassified=0`。
