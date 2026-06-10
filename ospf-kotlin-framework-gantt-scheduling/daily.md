# Gantt Scheduling 泛型化计划

日期：2026-06-10（G23 交接：完全退出迁移期兼容入口）

## 1. 总目标

`ospf-kotlin-framework-gantt-scheduling` 的最终目标是完成领域数值泛型化与物理量化，并完全退出迁移期兼容入口。

核心目标：

1. 领域层的时间、数量、成本、资源用量、产出消耗等有量纲字段统一使用 `Quantity<V>` 主路径。
2. solver/model 层可以继续使用 `Flt64`，但 `Flt64` 必须集中在 adapter、model boundary 或算法边界。
3. `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>` 等建模类型不得从 application/service API 向领域对象泄漏。
4. `Cost<V>`、`ShadowPriceMap<V>`、`CapacityColumn<V>`、`SlotBasedCapacityResult<V>`、solution summary、application snapshot 等领域和应用结果模型支持 `Flt64` 与 `FltX`。
5. 迁移期保留的 `Flt64` typealias、wrapper、deprecated constructor/property/function、deprecated class 和对应 suppress 必须分批移除，最终不再作为公开兼容入口存在。
6. 裸 `V` 只用于无量纲量，例如相对改善率、利用率、折扣、权重、归一化 reduced cost、排序评分。
7. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4` 作为 gantt-scheduling 示例使用方，必须随框架 API 泛型化同步更新并持续通过 reactor 编译。

## 2. 已完成事项摘要

G5-G22 已完成主迁移链路和兼容入口治理前置工作：

1. 领域模型、时间、容量、资源、产出消耗、成本、汇总结果和应用快照已具备泛型 `V` 与 `Quantity<V>` 主路径。
2. solver/model 必要 `Flt64` 已集中到 adapter、solver boundary、算法内部或明确边界，并由 scan gate 分类约束。
3. 已清理无引用 typealias、重复 adapter 别名、循环委托链路、无调用 Flt64 工厂方法和可迁移 deprecated 调用。
4. application 层剩余 `Flt64` 已审计为算法内部、Iteration 模型或结果边界，不向领域对象泄漏。
5. demo4、FltX/Quantity 测试主路径、scan gate 分类和 deprecated suppress 治理已同步。
6. `@file:Suppress("DEPRECATION")` 已降为 0；当前仅剩 `WorkingCalendar.kt` 内一个函数级 `@Suppress("DEPRECATION")`。

最近实测基线：

1. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml clean test` 通过；Surefire 汇总为 21 reports / 124 tests / 0 failures / 0 errors。
2. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过；37 modules BUILD SUCCESS。
3. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
4. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 无 whitespace error，仅 LF/CRLF 规范化 warning。

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

当前保留的迁移期兼容入口：

1. `WorkingCalendar.kt`：`ContinuousProductivityCalendar` deprecated 类，以及其 companion `invoke` 上的函数级 `@Suppress("DEPRECATION")`。
2. `Cost.kt`：裸值成本 constructor/property 兼容入口。
3. `Resource.kt`：裸值 `initialQuantity` / `usedQuantity` 兼容入口。
4. `ProductionTask.kt`：裸值 `produce` / `consumption` 兼容入口。
5. `Aggregation.kt`：裸值 `totalCapacity()` 兼容入口。
6. `SlotBasedCapacityResult.kt`：裸值结果、统计和 companion 构造兼容入口；当前 `Compat Wrapper=1` 来自其 ReplaceWith 文本中的 `Flt64`。

当前仍允许保留的非兼容边界：

1. solver 建模对象、变量、符号、求解结果、pre-solver 模型和 solver facade。
2. framework shadow price 基础 API，通过 adapter/helper 隔离到泛型结果模型。
3. branch-and-price 内部 objective、reduced cost、optimal rate、heartbeat、剪枝阈值等算法标量。
4. WorkingCalendar、TimeWindow 与生产力日历内部的时间数值计算边界。
5. `TimeWindow.toFlt64Boundary()` 中保留的 Flt64 返回类型与转换，用于 WorkingCalendar 的日历/solver 边界。

## 4. G23 目标

G23 进入破坏性 API 清理阶段，目标是移除第一批迁移期兼容入口，并保持现有泛型/Quantity 主路径、测试和 scan gate 稳定。

优先顺序：

1. 移除 `ContinuousProductivityCalendar` deprecated 类及其 companion `invoke`，同步删除函数级 `@Suppress("DEPRECATION")`。
2. 移除仓库内无外部调用方或可一次性迁移的裸值 deprecated 入口。
3. 对调用方较多或风险较高的入口，先完成仓库内调用方迁移，再按模块分批删除。
4. 每移除一批入口，都必须运行 compile/test、example reactor 编译和 scan gate，避免破坏泛型主路径。

## 5. G23 事项清单

优先检查以下文件：

1. `gantt-scheduling-infrastructure/src/main/.../WorkingCalendar.kt`
2. `gantt-scheduling-domain-task-context/src/main/.../Cost.kt`
3. `gantt-scheduling-domain-resource-context/src/main/.../Resource.kt`
4. `gantt-scheduling-domain-produce-context/src/main/.../ProductionTask.kt`
5. `gantt-scheduling-domain-capacity-scheduling-context/src/main/.../Aggregation.kt`
6. `gantt-scheduling-domain-bunch-compilation-context/src/main/.../model/SlotBasedCapacityResult.kt`
7. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4`
8. `ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1`

执行清单：

1. 用 `rg '@Deprecated' ospf-kotlin-framework-gantt-scheduling --glob '*.kt'` 建立最新 deprecated 入口清单，并区分真正兼容入口与外部库/语言级用法。
2. 用 `rg` 搜索每个 deprecated 入口的仓库内调用方，先迁移到 Quantity/generic 主路径。
3. 删除 `ContinuousProductivityCalendar` deprecated 类和相关 suppress，确认 `ContinuousQuantityProductivityCalendar` 覆盖现有使用场景。
4. 选择一批低风险裸值入口删除，建议优先处理 `Aggregation.totalCapacity()`、`ProductionTask.produce/consumption`、`Resource.initialQuantity/usedQuantity`。
5. 评估并开始拆除 `Cost.kt` 与 `SlotBasedCapacityResult.kt` 的兼容入口；若单轮风险过大，记录明确剩余清单和阻塞原因。
6. 更新或删除受影响测试、demo4 和迁移文档中对旧入口的引用。
7. 运行验收命令，回写本文档的 G23 实际结果、剩余入口数量和下一轮交接状态。

## 6. G23 验收标准

1. `rg '@file:Suppress\("DEPRECATION"\)' ospf-kotlin-framework-gantt-scheduling --glob '*.kt'` 结果保持 0。
2. `rg '@Suppress\("DEPRECATION"\)' ospf-kotlin-framework-gantt-scheduling --glob '*.kt'` 结果应为 0，或仅保留有明确阻塞说明的位置。
3. `ContinuousProductivityCalendar` 在 gantt-scheduling Kotlin 源码中不再作为 deprecated 兼容类存在；若保留，必须说明外部兼容约束。
4. `rg '@Deprecated' ospf-kotlin-framework-gantt-scheduling --glob '*.kt'` 数量较 G22 下降，并记录剩余入口清单。
5. `Compat Wrapper` 应从 1 降至 0；若未下降，必须说明对应 ReplaceWith 文本或兼容入口为何仍需保留。
6. scan gate 通过，且保持 `Domain DTO Pending=0`、`unclassified=0`。
7. `Adapter Conversion` 不上升；若因删除兼容入口导致分类变化，必须说明原因。
8. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml clean test` 通过，并记录 Surefire 汇总。
9. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
10. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 无 whitespace error。
11. `daily.md` 保持总目标不变，已完成事项只保留摘要，并回写下一轮可执行交接内容。
