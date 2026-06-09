# Gantt Scheduling 泛型化计划

日期：2026-06-09（G18 已完成；下一轮：G19——Application 层收口与剩余 Compat Wrapper 清理）

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

G5-G18 已完成主迁移链路和两轮收口。当前状态：

1. 领域模型、时间窗口、容量、资源、产出消耗、solution summary、iteration snapshot 与 solved quantity 出口已具备泛型 `V` 与 `Quantity<V>` 主路径。
2. solver/model 中必要的 `Flt64` 已集中到 adapter、solver boundary、算法内部或明确兼容入口，并由 scan gate 分类约束。
3. 旧裸值入口、无引用 typealias、重复 adapter 别名和循环委托链路已完成清理或迁移。
4. demo4 示例、FltX/Quantity 普通测试路径和 gantt-scheduling reactor 编译测试已同步。
5. scan gate 当前达到 `Domain DTO Pending=0`、`unclassified=0`。

当前 scan gate 基线：

```text
main=1,204
test=137
total=1,341
unclassified=0

Domain DTO Pending=0
Compat Wrapper=11
Adapter Conversion=38
Solver Boundary=771
Application Algorithm Internal=76
Application Result Boundary=2
Compilation Solver Time Boundary=7
Compilation Solver Result Boundary=6
Compilation Algorithm Scalar=32
Time/Calendar Adapter=7
Time/Calendar Boundary=48
Shadow Price Boundary=47
Generation Boundary=2
Numeric Constant/Internal=1
Import/Comment=156
Test=137
```

## 3. 当前保留边界

以下 `Flt64` 使用不作为当前阻塞项，但必须保持边界清晰：

1. solver 建模对象、变量、符号、求解结果、pre-solver 模型和 solver facade。
2. framework shadow price 基础 API，通过 adapter/helper 隔离到泛型结果模型。
3. branch-and-price 内部 objective、reduced cost、optimal rate、heartbeat、剪枝阈值等算法标量。
4. WorkingCalendar、TimeWindow 与生产力日历内部的时间数值计算边界。
5. 剩余 11 行 Compat Wrapper（TimeWindow.kt 的 `@Deprecated seconds/minutes/hours` 工厂方法和 `toFlt64Boundary` 返回类型中的 Flt64 引用，以及 `ProductionTask.kt` 中 Flt64-only 扩展函数签名）。

公开 `Flt64...` typealias 已在 G18 第一批大规模清理，仓库内无引用的全部删除；仍有引用的 3 个已完成迁移。Adapter 单例别名已在 G18 第二批全部消除。

## 4. G19 目标

G19 是收尾轮，目标是进一步压缩迁移期兼容入口，并判断 application 层剩余 `Flt64` 是否需要继续泛型化 facade。

1. 评估并清理剩余 `Compat Wrapper=11`，无法清理的兼容入口必须写明保留理由。
2. 评估 `gantt-scheduling-application` 中 task/bunch iteration snapshot/result 的泛型 facade；若实施，应避免 solver 类型继续向应用结果模型泄漏。
3. 保持 scan gate `Domain DTO Pending=0`、`unclassified=0`，并确保新增 `Flt64` 使用都有明确分类。
4. 保持 demo4 和 gantt-scheduling reactor 编译测试通过。

## 5. G19 事项

1. 检查 `TimeWindow.kt` 的 deprecated Flt64 快捷工厂和 `toFlt64Boundary()`，用仓库内引用结果决定删除、继续 deprecated，或收敛到更明确的边界 API。
2. 检查 `ProductionTask.kt` 中 Flt64-only 扩展函数，迁移仓库内使用方到泛型/Quantity 主路径；若无必要保留则删除。
3. 审计 `gantt-scheduling-application` 的 115 行 main `Flt64`：区分 branch-and-price 算法内部标量、结果边界、task/bunch `Iteration` 模型；只对真正泄漏 API 的部分设计 facade。
4. 若新增或移动 `Flt64` 边界，更新 `flt64-scan-gate.ps1` 分类，避免出现 `unclassified`。
5. 同步 demo4、相关测试和本文档。

## 6. G19 修改清单

优先检查以下文件和模块：

1. `gantt-scheduling-infrastructure/src/main/.../TimeWindow.kt`
   - 处理剩余 deprecated Flt64 时间窗口工厂和边界转换入口。

2. `gantt-scheduling-domain-produce-context/src/main/.../ProductionTask.kt`
   - 处理 Flt64-only 生产/消耗扩展函数。

3. `gantt-scheduling-application/src/main/.../application/model/task/Iteration.kt`
   - 评估 task iteration snapshot/result 是否需要泛型 facade。

4. `gantt-scheduling-application/src/main/.../application/model/bunch/Iteration.kt`
   - 评估 bunch iteration snapshot/result 是否需要泛型 facade。

5. `gantt-scheduling-application/src/main/.../application/service/**`
   - 保持 branch-and-price 算法内部标量为明确边界，不做无收益泛型化。

6. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4`
   - 持续同步推荐 API。

7. `ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1`
   - 根据实际保留点更新分类和基线。

## 7. G19 验收标准

1. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
2. scan gate 保持 `Domain DTO Pending=0`、`unclassified=0`。
3. `Compat Wrapper` 下降；若保持 11，必须在本文档说明每个保留点的理由。
4. `Adapter Conversion` 不上升；新增转换必须复用现有 adapter/helper 或进入明确分类。
5. application 层新增或保留的 `Flt64` 必须属于算法内部、结果边界或 solver facade，不得新增未说明的 API 泄漏。
6. 普通测试主路径不新增 legacy-only 覆盖；兼容测试只保留必要断言。
7. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
8. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
9. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 无 whitespace error。
10. 本文件回写 G19 实际完成摘要、最新 scan gate 基线、剩余边界和下一步。

## 8. 交接状态

G18 收尾后已验证：

1. scan gate 通过，当前基线为 `main=1,204`、`test=137`、`total=1,341`、`unclassified=0`。
2. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml compile` 通过。
3. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
4. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
5. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 通过，仅有 CRLF 工作区警告。
6. 当前 Gantt/demo4 diff 统计：52 files changed, 306 insertions(+), 796 deletions(-)（净减 490 行）。
