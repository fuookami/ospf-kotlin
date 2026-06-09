# Gantt Scheduling 泛型化计划

日期：2026-06-10（G19 修正完成；下一轮：G20——剩余 deprecated 入口与边界精修）

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

G5-G19 已完成主迁移链路与收口工作：

1. 领域模型、时间窗口、容量、资源、产出消耗、汇总结果、迭代快照和 solved quantity 出口已具备泛型 `V` 与 `Quantity<V>` 主路径。
2. solver/model 必要 `Flt64` 已集中在 adapter、solver boundary、算法内部或明确兼容入口，并由 scan gate 约束。
3. 旧裸值入口、无引用 typealias、重复 adapter 别名、循环委托链路和无调用的 Flt64 时间窗口工厂已完成清理。
4. application 层剩余 `Flt64` 已审计为 branch-and-price 算法内部、Iteration 模型和结果边界，不向领域对象泄漏。
5. demo4 示例和 FltX/Quantity 测试主路径已同步，gantt-scheduling 与 example 验证在 G19 报告中通过。

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
5. `TimeWindow.toFlt64Boundary()` 中保留的 Flt64 返回类型与转换，用于 WorkingCalendar 的日历/solver 边界。

## 4. G20 目标

G20 聚焦最终精修，不再做无收益的大范围泛型化。

1. 清理或明确保留剩余 `@file:Suppress("DEPRECATION")`。
2. 迁移仍可低风险替换的 deprecated 裸值调用。
3. 判断 `Compat Wrapper=2` 是否应继续作为兼容入口统计，或更准确归入 Time/Calendar 边界分类。
4. 保持 scan gate `Domain DTO Pending=0`、`unclassified=0`，并确保 demo4 与主要验证链路继续通过。

## 5. G20 事项

1. 审计 main 中剩余 `@file:Suppress("DEPRECATION")`：`SlotBasedCapacityResult.kt`、`CapacityActionProduce.kt`、`WorkingCalendar.kt`。
2. 审计 test 中剩余 `@file:Suppress("DEPRECATION")`：`TimeWindowTest.kt`、`TimeRangeFindTest.kt`、`TimeRangeDifferenceTest.kt`、`CostQuantityAlternativeTest.kt`。
3. 将可替换的 deprecated 调用迁移到 Quantity/generic 主入口；无法迁移的保留点必须写明原因。
4. 评估 `TimeWindow.toFlt64Boundary()` 的 scan gate 分类：若它是长期 Time/Calendar boundary，不应继续被误读为待清理 Compat Wrapper。
5. 同步测试、demo4、scan gate 分类和本文档。

## 6. G20 修改清单

优先检查以下文件：

1. `gantt-scheduling-domain-bunch-compilation-context/src/main/.../service/SlotBasedCapacityPreSolver.kt`
2. `gantt-scheduling-domain-bunch-compilation-context/src/main/.../model/SlotBasedCapacityResult.kt`
3. `gantt-scheduling-domain-produce-context/src/main/.../model/CapacityActionProduce.kt`
4. `gantt-scheduling-infrastructure/src/main/.../WorkingCalendar.kt`
5. `gantt-scheduling-infrastructure/src/main/.../TimeWindow.kt`
6. `gantt-scheduling-infrastructure/src/test/.../TimeWindowTest.kt`
7. `gantt-scheduling-infrastructure/src/test/.../TimeRangeFindTest.kt`
8. `gantt-scheduling-infrastructure/src/test/.../TimeRangeDifferenceTest.kt`
9. `gantt-scheduling-domain-task-context/src/test/.../CostQuantityAlternativeTest.kt`
10. `ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1`
11. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4`

## 7. G20 验收标准

1. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
2. scan gate 保持 `Domain DTO Pending=0`、`unclassified=0`。
3. `Compat Wrapper` 下降，或对剩余 2 行给出稳定边界分类与保留说明。
4. `Adapter Conversion` 不上升；新增转换必须复用现有 adapter/helper 或进入明确分类。
5. main/test 中剩余 `@file:Suppress("DEPRECATION")` 要么减少，要么逐个记录保留理由。
6. 普通 FltX/Quantity 测试不新增 legacy-only 覆盖。
7. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
8. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
9. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 无 whitespace error。
10. 本文件回写 G20 实际完成摘要、最新 scan gate 基线、剩余边界和下一步。

## 8. 交接状态

本次修正已完成：

1. 修正 `TimeWindow.kt` 文件级注解排版。
2. 删除 `CapacitySchedulingContext.kt` 中无必要的 `@file:Suppress("DEPRECATION")`，并修正文件级注解排版。
3. 重写本文档为 G20 交接版本。
