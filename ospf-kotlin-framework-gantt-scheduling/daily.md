# Gantt Scheduling 泛型化计划

日期：2026-06-10（G20 完成；泛型化精修完毕）

## 1. 总目标

`ospf-kotlin-framework-gantt-scheduling` 的长期目标是完成领域数值泛型化与物理量化，并将 `Flt64` 收敛为 solver/model boundary、adapter boundary、算法内部或明确的迁移期兼容入口。

核心目标：

1. 领域层的时间、数量、成本、资源用量、产出消耗等有量纲字段支持 `Quantity<V>`。
2. solver/model 层可以继续使用 `Flt64`，但 `Flt64` 必须集中在 adapter、model boundary 或算法边界。
3. `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>` 等建模类型不再从 application/service API 向领域对象泄漏。
4. `Cost<V>`、`ShadowPriceMap<V>`、`CapacityColumn<V>`、`SlotBasedCapacityResult<V>`、solution summary、application snapshot 等领域和应用结果模型支持 `Flt64` 与 `FltX`。
5. 迁移期保留必要的 `Flt64` typealias、wrapper 和旧入口，确保现有应用可按阶段迁移；兼容入口应逐步 deprecated 并最终退出。
6. 裸 `V` 只用于无量纲量，例如相对改善率、利用率、折扣、权重、归一化 reduced cost、排序评分。
7. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4` 作为 gantt-scheduling 示例使用方，必须随框架 API 泛型化同步更新并持续通过 reactor 编译。

## 2. 已完成事项摘要

G5-G20 已完成全部主迁移链路和四轮收口。当前状态：

1. 领域模型、时间窗口、容量、资源、产出消耗、solution summary、iteration snapshot 与 solved quantity 出口已具备泛型 `V` 与 `Quantity<V>` 主路径。
2. solver/model 中必要的 `Flt64` 已集中到 adapter、solver boundary、算法内部或明确兼容入口，并由 scan gate 分类约束。
3. 旧裸值入口、无引用 typealias、重复 adapter 别名、循环委托链路、无调用的 Flt64 工厂方法、和可迁移的 deprecated 调用已完成清理或迁移。
4. application 层剩余 `Flt64` 已审计为 branch-and-price 算法内部、Iteration 模型和结果边界，不向领域对象泄漏。
5. demo4 示例和 FltX/Quantity 测试主路径已同步，gantt-scheduling 与 example 验证全部通过。
6. scan gate 分类已精修，`toFlt64Boundary()` 内的 Flt64 引用从 Compat Wrapper 归入 Time/Calendar Adapter。

G20 完成事项（本轮）：

7. **迁移 `SlotConstraints.from()` 到 Quantity 版本**：`tolerance != null` 分支从 deprecated 裸值属性 `produceByProduct`/`consumptionByMaterial`/`resourceUsageByResource` 迁移到 Quantity 版本 `produceQuantityByProduct`/`consumptionQuantityByMaterial`/`resourceUsageQuantityByResource`，消除对 deprecated `SlotConstraints` invoke operator 的调用。移除 `@file:Suppress("DEPRECATION")`。
8. **迁移 `SlotBasedCapacityPreSolver` 到 Quantity 主构造函数**：调用方从 deprecated 裸值构造函数迁移到 Quantity 主构造函数。
9. **移除 `CapacityActionProduce.kt` 的多余 suppress**：该文件的 `produce`/`consumption` 不是 deprecated API，`@file:Suppress("DEPRECATION")` 是多余的，已移除。
10. **移除 4 个测试文件的 DEPRECATION suppress**：`CostQuantityAlternativeTest.kt`、`TimeRangeDifferenceTest.kt`、`TimeRangeFindTest.kt`、`TimeWindowTest.kt` 无实际 deprecated 引用，全部移除。
11. **修正 scan gate 分类**：`TimeWindow.kt` 中 `toFlt64Boundary()` 函数体内的 `TimeWindow<Flt64>` 和 `Flt64(it)` 从 Compat Wrapper（待清理）归入 Time/Calendar Adapter（长期边界），Compat Wrapper 从 2 降至 1。

当前 scan gate 基线：

```text
main=1,186
test=137
total=1,323
unclassified=0

Domain DTO Pending=0
Compat Wrapper=1
Adapter Conversion=38
Solver Boundary=771
Application Algorithm Internal=76
Application Result Boundary=2
Compilation Solver Time Boundary=7
Compilation Solver Result Boundary=6
Compilation Algorithm Scalar=32
Compilation Algorithm Internal=0
Time/Calendar Adapter=8
Time/Calendar Boundary=42
Shadow Price Boundary=47
Generation Boundary=2
Numeric Constant/Internal=1
Import/Comment=153
Test=137
```

## 3. 当前保留边界

以下 `Flt64` 使用不作为当前阻塞项，但必须保持边界清晰：

1. solver 建模对象、变量、符号、求解结果、pre-solver 模型和 solver facade。
2. framework shadow price 基础 API，通过 adapter/helper 隔离到泛型结果模型。
3. branch-and-price 内部 objective、reduced cost、optimal rate、heartbeat、剪枝阈值等算法标量。
4. WorkingCalendar、TimeWindow 与生产力日历内部的时间数值计算边界。
5. `TimeWindow.toFlt64Boundary()` 中保留的 Flt64 返回类型与转换，用于 WorkingCalendar 的日历/solver 边界。

**剩余 `@file:Suppress("DEPRECATION")` 保留说明**：

| 文件 | 保留原因 |
|------|---------|
| `WorkingCalendar.kt` | 调用 `toFlt64Boundary()` 构建 Flt64 日历，属于 Time/Calendar Boundary |
| `SlotBasedCapacityResult.kt` | 定义 deprecated 裸值兼容构造函数/属性供外部迁移期使用，内部实现引用自身 deprecated 符号 |

**剩余 Compat Wrapper=1 保留说明**：

`SlotBasedCapacityResult.kt` 中 `@Deprecated` 注解的 `ReplaceWith(...)` 字符串包含 `Flt64`，被 scan gate 匹配为 Compat Wrapper。这是 deprecated 入口的替换提示，不是待清理代码，属于合法的迁移兼容入口。

## 4. 后续可选事项

以下事项可在后续版本中考虑，但不作为当前阻塞项：

1. **删除 `SlotBasedCapacityResult` deprecated 裸值构造函数和属性**：当所有外部调用方迁移到 Quantity 版本后可删除。需先审计仓库外使用方。
2. **删除 `CapacityIntermediateValues` deprecated 裸值方法**：同上。
3. **删除 `SlotConstraints` deprecated invoke operator 和裸值属性**：同上。
4. **Application 层泛型 Iteration facade**（低优先级）：若未来需要让 application 层的 `Iteration` 模型内部不暴露 Flt64，需要重新设计泛型 snapshot/result facade，工作量较大且当前不向领域泄漏。
5. **WorkingCalendar 泛型化**（低优先级）：WorkingCalendar 内部建模全部使用 `TimeWindow<Flt64>`，需要重新设计日历建模路径。

## 5. G20 验收状态

本轮已完成以下验收项：

1. scan gate 通过：exit code 0，`Domain DTO Pending=0`，`unclassified=0`。
2. Compat Wrapper 从 2 降至 1（`toFlt64Boundary` 归入 Time/Calendar Adapter）。
3. Adapter Conversion 保持 38，不上升。
4. main/test 中剩余 `@file:Suppress("DEPRECATION")` 从 7 减至 2（`WorkingCalendar.kt` 和 `SlotBasedCapacityResult.kt`），保留理由已在本文档说明。
5. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
6. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
7. `git diff --check` 无 whitespace error（仅 CRLF 警告）。
8. diff 统计：7 files changed, 20 insertions(+), 27 deletions(-)（净减 7 行）。

## 6. 累计验证状态

G5-G20 累计验证：

1. scan gate：`main=1,186`、`unclassified=0`。
2. Compat Wrapper 从初始 125 降至 1（下降 99%）。
3. Adapter Conversion 从 40 降至 38。
4. 所有 deprecated 入口的委托方向已反转，Quantity 版本为主入口。
5. 所有无引用的 typealias、adapter 别名、deprecated 工厂方法已删除。
6. 所有无实际 deprecated 引用的 `@file:Suppress("DEPRECATION")` 已移除（从约 50 个减至 2 个）。
7. `toFlt64Boundary()` 的 scan gate 分类已修正为 Time/Calendar Adapter。
8. demo4 持续使用推荐 API 编译通过。
