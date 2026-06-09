# Gantt Scheduling 泛型化计划

日期：2026-06-09（G19 完成；泛型化收尾完毕）

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

G5-G19 已完成全部主迁移链路和三轮收口。当前状态：

1. 领域模型、时间窗口、容量、资源、产出消耗、solution summary、iteration snapshot 与 solved quantity 出口已具备泛型 `V` 与 `Quantity<V>` 主路径。
2. solver/model 中必要的 `Flt64` 已集中到 adapter、solver boundary、算法内部或明确兼容入口，并由 scan gate 分类约束。
3. 旧裸值入口、无引用 typealias、重复 adapter 别名和循环委托链路已完成清理或迁移。
4. demo4 示例、FltX/Quantity 普通测试路径和 gantt-scheduling reactor 编译测试已同步。
5. scan gate 当前达到 `Domain DTO Pending=0`、`unclassified=0`。

G19 完成事项（本轮）：

6. **删除 3 个 deprecated Flt64 时间窗口工厂**：`TimeWindow.seconds(Flt64...)`、`TimeWindow.minutes(Flt64...)`、`TimeWindow.hours(Flt64...)` 在仓库内无调用方，安全删除。Compat Wrapper 从 11 降至 2，Time/Calendar Boundary 从 48 降至 42。
7. **移除 44 个文件的多余 `@file:Suppress("DEPRECATION")`**：9 个 application 文件和 35 个 domain/infrastructure 文件的 DEPRECATION suppress 已无实际 deprecated API 引用，全部移除。仅 3 个仍引用 deprecated API 的文件（`SlotBasedCapacityResult.kt`、`CapacityActionProduce.kt`、`WorkingCalendar.kt`）保留。
8. **Application 层 Flt64 审计结论**：115 行 application Flt64 全部属于 branch-and-price 算法内部标量（76 行）和 task/bunch Iteration 模型（50 行），通过 `snapshot()` 方法正确转换为泛型 `IterationSnapshot<V>`，不向领域对象泄漏。不做无收益泛型化。
9. **ProductionTask Flt64-only 扩展函数审计结论**：ProductionTask.kt 已无任何 Flt64 引用，上一轮清理已完全解决。

当前 scan gate 基线：

```text
main=1,186
test=137
total=1,323
unclassified=0

Domain DTO Pending=0
Compat Wrapper=2
Adapter Conversion=38
Solver Boundary=771
Application Algorithm Internal=76
Application Result Boundary=2
Compilation Solver Time Boundary=7
Compilation Solver Result Boundary=6
Compilation Algorithm Scalar=32
Compilation Algorithm Internal=0
Time/Calendar Adapter=7
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
5. 剩余 2 行 Compat Wrapper（`TimeWindow.kt` 中 `toFlt64Boundary()` 返回类型中的 Flt64 引用和 `Flt64(it)` 转换）。

**保留理由**：
- `toFlt64Boundary()` 是 `WorkingCalendar` 工厂方法的 solver boundary 转换入口，5 处调用方全部在 `WorkingCalendar.kt` 的 deprecated Flt64-only 工厂方法中，属于合法的 Time/Calendar Boundary。
- `Flt64(it)` 在 `toFlt64Boundary()` 内部用于构建 Flt64 类型 fromDouble/toDouble 参数，是 solver boundary 必要的数值转换。

公开 `Flt64...` typealias 已在 G18 第一批大规模清理；Adapter 单例别名已在 G18 第二批全部消除；deprecated Flt64 时间窗口工厂已在 G19 全部删除。

## 4. 后续可选事项

以下事项可在后续版本中考虑，但不作为当前阻塞项：

1. **SlotBasedCapacityResult 旧构造函数迁移**：`SlotBasedCapacityPreSolver.kt` 仍使用 deprecated `SlotBasedCapacityResult` 裸值构造函数（1 处），可改为 Quantity 版本。
2. **CapacityActionProduce 旧入口迁移**：`CapacityActionProduce.kt` 仍使用 deprecated `produce`/`consumption` 裸值属性（5 处），可改为 `produceQuantityByProduct`/`consumptionQuantityByMaterial`。
3. **Application 层泛型 Iteration facade**（低优先级）：若未来需要让 application 层的 `Iteration` 模型内部不暴露 Flt64，需要重新设计泛型 snapshot/result facade，工作量较大且当前不向领域泄漏。
4. **WorkingCalendar 泛型化**（低优先级）：WorkingCalendar 内部建模全部使用 `TimeWindow<Flt64>`，需要重新设计日历建模路径。

## 5. G19 验收状态

本轮已完成以下验收项：

1. scan gate 通过：exit code 0，`Domain DTO Pending=0`，`unclassified=0`。
2. Compat Wrapper 从 11 降至 2（下降 82%）。
3. Adapter Conversion 保持 38，不上升。
4. main Flt64 从 1,204 降至 1,186（下降 18 行，删除 3 个 deprecated 工厂）。
5. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml compile` 通过。
6. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过（18 tests, 0 failures）。
7. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
8. `git diff --check` 无 whitespace error（仅 CRLF 警告）。
9. diff 统计：44 files changed, 2 insertions(+), 125 deletions(-)（净减 123 行）。

## 6. 累计验证状态

G5-G19 累计验证：

1. scan gate 从初始版本到当前：`main=1,186`（从初始基线下降约 18 行）、`unclassified=0`。
2. Compat Wrapper 从 125 降至 2（下降 98%）。
3. Adapter Conversion 从 40 降至 38。
4. 所有 deprecated 入口的委托方向已反转，Quantity 版本为主入口。
5. 所有无引用的 typealias、adapter 别名、deprecated 工厂方法已删除。
6. 所有无实际 deprecated 引用的 `@file:Suppress("DEPRECATION")` 已移除。
7. demo4 持续使用推荐 API 编译通过。
