# Gantt Scheduling 泛型化计划

日期：2026-06-10（G21 验收完成；下一轮：G22——剩余兼容入口与 suppress 最终治理）

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

G5-G21 已完成主迁移链路和验收收口：

1. 领域模型、时间、容量、资源、产出消耗、成本、汇总结果和应用快照已具备泛型 `V` 与 `Quantity<V>` 主路径。
2. solver/model 必要 `Flt64` 已集中到 adapter、solver boundary、算法内部或明确兼容入口，并由 scan gate 分类约束。
3. 已清理无引用 typealias、重复 adapter 别名、循环委托链路、无调用 Flt64 工厂方法和可迁移 deprecated 调用。
4. application 层剩余 `Flt64` 已审计为算法内部、Iteration 模型或结果边界，不向领域对象泄漏。
5. demo4、FltX/Quantity 测试主路径、scan gate 分类和 G21 验收文档已同步。
6. G21 验收通过：gantt-scheduling 当前 surefire 汇总为 21 reports / 124 tests / 0 failures / 0 errors；example reactor 37 modules BUILD SUCCESS。

## 3. 当前基线

最新 scan gate 基线：

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

当前仅保留 1 个 `@file:Suppress("DEPRECATION")`：

| 文件 | 保留原因 |
|------|---------|
| `WorkingCalendar.kt` | 文件内保留 `ContinuousProductivityCalendar` deprecated 兼容入口；G21 审查认为文件级 suppress 可能可缩小或移除，但尚未执行代码变更。 |

当前 `Compat Wrapper=1`：

`SlotBasedCapacityResult.kt` 的 `@Deprecated` / `ReplaceWith(...)` 迁移提示文本包含 `Flt64`，不是待清理运行时代码。

## 4. 当前保留边界

以下 `Flt64` 使用不作为当前阻塞项，但必须保持边界清晰：

1. solver 建模对象、变量、符号、求解结果、pre-solver 模型和 solver facade。
2. framework shadow price 基础 API，通过 adapter/helper 隔离到泛型结果模型。
3. branch-and-price 内部 objective、reduced cost、optimal rate、heartbeat、剪枝阈值等算法标量。
4. WorkingCalendar、TimeWindow 与生产力日历内部的时间数值计算边界。
5. `TimeWindow.toFlt64Boundary()` 中保留的 Flt64 返回类型与转换，用于 WorkingCalendar 的日历/solver 边界。

## 5. G22 目标

G22 聚焦最后的兼容入口治理，不再做大范围泛型化。

1. 尝试移除或局部化 `WorkingCalendar.kt` 的文件级 `@file:Suppress("DEPRECATION")`，以编译输出为准。
2. 若 suppress 可移除，更新文档和验收基线；若必须保留，明确最小保留范围与原因。
3. 复核 `ContinuousProductivityCalendar` 是否仍需作为 deprecated 兼容入口保留。
4. 保持 scan gate 稳定：`Domain DTO Pending=0`、`unclassified=0`、`Adapter Conversion=38` 不退化。
5. 确认 G21 的测试数量和验收数字不再出现文档漂移。

## 6. G22 事项清单

优先检查以下文件：

1. `gantt-scheduling-infrastructure/src/main/.../WorkingCalendar.kt`
2. `gantt-scheduling-infrastructure/src/main/.../TimeWindow.kt`
3. `gantt-scheduling-domain-bunch-compilation-context/src/main/.../model/SlotBasedCapacityResult.kt`
4. `ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1`
5. `ospf-kotlin-framework-gantt-scheduling/daily.md`
6. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4`

执行清单：

1. 删除或缩小 `WorkingCalendar.kt` 的 `@file:Suppress("DEPRECATION")`，运行 gantt-scheduling compile/test 验证是否出现 deprecation warning。
2. 若出现 warning，定位具体 deprecated 调用，将 suppress 缩到最小声明或函数范围。
3. 审查 `ContinuousProductivityCalendar` 的仓库内调用方，确认是否可迁移到 `ContinuousQuantityProductivityCalendar`。
4. 运行 scan gate 并确认分类不退化。
5. 运行 gantt-scheduling 全量测试和 example reactor 编译。
6. 回写本文档，记录 G22 实际完成摘要、最新基线、剩余兼容入口和下一步。

## 7. G22 验收标准

1. `rg '@file:Suppress\("DEPRECATION"\)' ospf-kotlin-framework-gantt-scheduling --glob '*.kt'` 结果为 0，或仅保留有明确最小化理由的位置。
2. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过，并记录 surefire 汇总测试数。
3. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
4. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
5. scan gate 保持 `Domain DTO Pending=0`、`unclassified=0`。
6. `Compat Wrapper=1` 或进一步下降；若保留，必须指向迁移提示文本或明确兼容入口。
7. `Adapter Conversion=38` 不上升。
8. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 无 whitespace error。
9. 本文件保持总目标不变，并回写 G22 验收结果与下一轮交接状态。
