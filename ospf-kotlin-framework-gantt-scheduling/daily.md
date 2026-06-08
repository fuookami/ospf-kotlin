# Gantt Scheduling 泛型化计划

日期：2026-06-08（最后更新：G9 续轮验证完成 — 内部裸值读取迁移到 Quantity 主路径）

## 1. 总目标

`ospf-kotlin-framework-gantt-scheduling` 的长期目标是完成领域数值泛型化与物理量化，并将 `Flt64` 收敛为 solver/model boundary、adapter boundary、算法内部或明确的迁移期兼容入口。

核心目标：

1. 领域层的时间、数量、成本、资源用量、产出消耗等有量纲字段支持 `Quantity<V>`。
2. solver/model 层可以继续使用 `Flt64`，但 `Flt64` 必须集中在 adapter、model boundary 或算法边界。
3. `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>` 等建模类型不再从 application/service API 向领域对象泄漏。
4. `Cost<V>`、`ShadowPriceMap<V>`、`CapacityColumn<V>`、`SlotBasedCapacityResult<V>`、solution summary、application snapshot 等领域和应用结果模型支持 `Flt64` 与 `FltX`。
5. 迁移期保留 `Flt64` typealias、wrapper 和旧入口，确保现有应用可按阶段迁移；这些兼容入口不是最终目标，待领域 API、example、application 调用方和 scan gate 均完成泛型/物理量路径验证后，应按 deprecated 到移除流程退出，最终状态不依赖 `Flt64` 兼容层。
6. 裸 `V` 只用于无量纲量，例如相对改善率、利用率、折扣、权重、归一化 reduced cost、排序评分。
7. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4` 作为 gantt-scheduling 示例使用方，必须随框架 API 泛型化同步更新，持续验证旧 `Flt64` 应用路径可编译。

物理量化原则：

| 类型 | 示例字段 | 目标表达 |
|------|----------|----------|
| 时间点/时间跨度 | `start`, `end`, `duration`, `timeWindow` 映射值 | `Quantity<V>` 或时间专用边界类型 |
| 产能 | `capacity`, `availableCapacity`, `executorCapacity` | `Quantity<V>` |
| 资源用量 | `resourceUsage`, `quantity`, `overQuantity`, `lessQuantity` | `Quantity<V>` |
| 产出/消耗 | `produce`, `consumption`, `demand` | `Quantity<V>` |
| 成本 | `cost`, `objective`, `penalty` | `Quantity<V>` 或明确的成本数值边界 |

同步更新原则：每次调整 gantt-scheduling 对外领域 API、application API、资源/产能/产出上下文签名或 `Flt64` 兼容入口时，都要同步检查并更新 `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4`；若 demo4 仍使用旧路径，应显式固定到 `Flt64` wrapper 或 typealias，并通过包含 example 的 reactor 编译验证。

## 2. 已完成事项

已完成：

1. 已建立 `TimeWindow<V>`、领域模型 `V` 泛型、solver adapter 与 Flt64 扫描门禁基线。
2. 已完成 task/bunch compilation、capacity/resource/produce、bunch generation、branch-and-price 主链路的泛型兼容审计与必要修复。
3. 已完成 G5 public API 源码兼容收口，旧 `Flt64` wrapper/typealias 和新 `V` 泛型入口可并存。
4. 已完成 G6 第一轮字段级 `Quantity<V>` 主存储迁移，覆盖 task/cost、capacity column、resource capacity、material demand/reserves、production task quantity map、slot-based capacity result 和 slot constraints。
5. 已完成 G7 时间/DTO/application 边界收口，覆盖 application iteration snapshot、TimeWindow helper、task/bunch solution summary 与 scan gate 分类深化。
6. 已完成 G8 深层时间/日历、capacity result、slot/pre-solver result、task/makespan/switch solved quantity、resource/produce/consumption solved quantity 的泛型物理量出口。
7. 已保留旧裸 `V` 构造、裸值属性、helper 和 `Flt64` typealias，兼容层继续用于迁移期。
8. 已补充 Flt64 legacy、FltX generic、Quantity<FltX> 字段构造、adapter conversion、time/calendar DTO、pre-solver result、demo4/example compile 的测试或编译验证。
9. 已新增 `MIGRATION_G5.md`、`MIGRATION_G6.md`、`MIGRATION_G8.md`、`MIGRATION_G9.md`，记录迁移路径、旧入口、单位默认策略和剩余边界。
10. 已完成 G9 第一轮：`TaskTime` 接口补齐 `overMaxDelayTimeQuantity()` 和 `overMaxAdvanceTimeQuantity()`；`SlotBasedCapacityResult`、`CapacityIntermediateValues`、`SlotConstraints` 的旧裸值属性、旧构造函数/工厂标记 `@Deprecated` 并配套 `ReplaceWith`。
11. 已审计 service/application 层 solver 暴露点，确认 `BranchAndPriceAlgorithm` public API 不暴露 solver 类型；剩余 `SolutionAnalyzer`/`BunchSolutionAnalyzer`/`TaskSolutionAnalyzer` 的 `AbstractLinearMetaModel<Flt64>` 参数属于 solver boundary 分类。
12. 当前门禁基线：`flt64-scan-gate.ps1` 输出 main=1,402 / test=204 / total=1,606 / unclassified=0。
13. 已完成 G9 续轮：`Cost`（6 个入口 + 4 个 Flt64 typealias）、`CapacityColumn`（2 个入口 + 1 个 typealias）、`AbstractResourceCapacity`/`ResourceCapacity`/`Resource`（6 个入口）、`MaterialDemand`/`MaterialReserves`（各 4 个入口 + 2 个 typealias）、`ProductionTask`（2 个入口 + 1 个 typealias）旧裸值属性/构造函数/工厂标记 `@Deprecated` + `ReplaceWith`。
14. 已新增测试 `CostQuantityAlternativeTest`，验证 `CostItem`/`Cost`/`MutableCost`/`ImmutableCost` 主构造函数 Quantity 存储与 deprecated 裸值属性兼容路径。
15. scan gate 脚本更新 `@Deprecated` typealias 分类规则，`unclassified` 归零。
16. `ProductionAction.unitCapacityV` 和 `unitCostV` 标记 `@Deprecated`，引导调用方迁移到 `unitCapacityQuantity` 和 `unitCostQuantity`；`unitCapacity`、`unitCost`、`upperBound` 的 `Flt64` 签名继续保留为 solver 边界。
17. task/capacity 编译内部读取已从 deprecated 裸值属性迁到 `costSum?.value`、`columnCost.value`。
18. demo4 示例已改用 `CostItem.costQuantity`、`Cost.costSum` 和 `bunch.cost.costSum!!.value`，覆盖 Quantity 替代路径并避免示例继续依赖 deprecated 成本入口。
19. produce/resource/capacity/bunch/task compilation 内部读取已继续迁移到 Quantity 主字段，覆盖 material demand/reserves、production task quantity map、resource capacity、resource usage、capacity column、bunch/task cost 等路径。
20. resource 模块内部 `initialQuantity`、`usedQuantity`、`ResourceCapacity.toString()` 读取已收口到 `initialQuantity().value`、`usedQuantityQuantity(...).value` 和 `quantityRangeValue.value`，避免 main 编译继续触发本轮 deprecated 裸值警告。

当前允许保留的边界：

1. solver 建模对象、solver 内部结果、pre-solve 模型继续固定 `Flt64`。
2. framework shadow price 基础 API 继续固定 `Flt64`，通过 adapter 与泛型 helper 隔离。
3. 旧 wrapper、typealias、legacy API 和兼容测试可继续使用 `Flt64`。
4. `ProductionAction.unitCapacity`、`unitCost`、`upperBound` 保留 `Flt64` 签名作为 solver 边界入口；泛型物理量路径使用 `unitCapacityQuantity<V>` 或 `unitCostQuantity<V>`。
5. application branch-and-price 内部迭代状态仍以 `Flt64` 驱动，外部通过 snapshot 转为泛型 `Quantity<V>` 与无量纲 `V`。
6. task/bunch compilation、solution analyzer、pre-solver 的入模和求解参数仍归为 solver/model boundary；对外读取优先走泛型 summary、snapshot、quantity helper 或 adapter result。

## 3. 当前目标：G9 续轮 — 扩展 deprecated 覆盖与进一步压缩分类

G9 第一轮已完成 TaskTime 物理量补齐和第一批 deprecated 标记。续轮目标是继续扩展 deprecated 覆盖范围，压缩 `Domain DTO Pending`、`Time/Calendar Boundary` 分类，并完善测试矩阵。

### 3.1 已完成事项（G9 第一轮）

1. `TaskTime` 接口补齐 `overMaxDelayTimeQuantity()` 和 `overMaxAdvanceTimeQuantity()`，与现有 6 个方法签名一致。
2. `SlotBasedCapacityResult` 裸值构造函数、4 个裸值属性标记 `@Deprecated` + `ReplaceWith`。
3. `CapacityIntermediateValues` 3 个裸值读取方法标记 `@Deprecated` + `ReplaceWith`。
4. `SlotConstraints` 6 个裸值属性、裸值工厂标记 `@Deprecated` + `ReplaceWith`。
5. demo4 新增 `overMaxDelayTimeQuantity` 和 `overMaxAdvanceTimeQuantity` 样例。
6. 新增测试 `taskTimeShouldExposeOverMaxDelayAndAdvanceFltXQuantities`。
7. 新增 `MIGRATION_G9.md` 迁移文档。
8. service/application solver 暴露点审计完成，确认应用层 public API 已清洁。

### 3.2 已完成事项（G9 续轮）

1. `CostItem.value`、`Cost.sum` 裸值属性标记 `@Deprecated`；`CostItem`/`MutableCost`/`ImmutableCost` 裸值构造函数标记 `@Deprecated`；`Cost.Companion.invoke(items, constants, sums: V?)` 裸值工厂标记 `@Deprecated`；4 个 Flt64 typealias 标记 `@Deprecated`。
2. `CapacityColumn.cost` 裸值属性和裸值构造函数标记 `@Deprecated`；`Flt64CapacityColumn` typealias 标记 `@Deprecated`。
3. `AbstractResourceCapacity.quantity`/`lessQuantity`/`overQuantity` 裸值属性标记 `@Deprecated`；`ResourceCapacity` 裸值构造函数标记 `@Deprecated`；`Resource.initialQuantity` 和 `Resource.usedQuantity` 标记 `@Deprecated`。
4. `MaterialDemand`/`MaterialReserves` 各 3 个裸值属性 + 裸值构造函数标记 `@Deprecated`；`ProductionTask.produce`/`consumption` 裸值 map 标记 `@Deprecated`；3 个 Flt64 typealias 标记 `@Deprecated`。
5. 新增测试 `CostQuantityAlternativeTest` 覆盖 5 个场景。
6. scan gate 脚本 `@Deprecated` typealias 分类规则已更新，`unclassified=0`。
7. `ProductionAction.unitCapacityV`、`unitCostV` 标记 deprecated，替代路径分别为 `unitCapacityQuantity(timeWindow).value` 和 `unitCostQuantity(time, fromDouble).value`。
8. task/capacity 编译内部成本读取迁移到 Quantity 主字段，减少 main 代码对 deprecated 裸值属性的依赖。
9. demo4 的成本样例和 shadow price reduced cost 改用 Quantity 主字段，并已通过 example reactor 编译。
10. produce/resource/capacity/bunch/task compilation 的 main 代码裸值读取继续迁到 Quantity 主字段：`quantityRangeValue.value`、`lessQuantityValue?.value`、`overQuantityValue?.value`、`produceQuantityByProduct`、`consumptionQuantityByMaterial`、`costSum?.value`、`columnCost.value` 等路径已覆盖。
11. resource 内部对 deprecated `initialQuantity` / `usedQuantity` 的自调用已收口到 Quantity helper，保留旧入口为兼容 API，不再作为内部主读取路径。

### 3.3 下一轮目标

1. 压缩 `Domain DTO Pending` 分类，将已泛型化的 DTO 条目重新分类或移除。
2. 压缩 `Time/Calendar Boundary` 分类，评估 `WorkingCalendar` 和 `TimeWindow` 中剩余 Flt64 的边界解释。
3. 继续审计 produce/resource/capacity service/result facade 中尚未覆盖的结果 facade 与旧入口，优先把对外示例和内部逻辑迁到 Quantity 主字段。
4. 下一轮改动后继续跑完整 gantt reactor、example reactor、scan gate 和 diff check。

### 3.4 计划

1. 先按 DTO 家族进一步压缩 scan gate 分类，将已泛型化条目重新归类。
2. 继续评估 `ProductionAction` 旧 `Flt64` solver 边界入口是否只保留为内部入模入口，避免对外业务代码新增依赖。
3. 每完成一个模块 deprecated 批次，同步补 FltX 主路径测试和 demo4 编译样例。
4. 最后统一跑完整 gantt reactor、example reactor、scan gate 和 diff check。

### 3.5 修改清单

预计涉及（续轮后续）：

1. `gantt-scheduling-domain-capacity-scheduling-context`
   - `ProductionAction` 旧 `Flt64` solver 边界入口审计
   - `CapacityCompilation`、`CapacityOrderCompilation` 旧裸值 helper

2. `ospf-kotlin-example`
   - `framework_demo/demo4`
   - 后续 deprecated 替代路径 compile sample

3. 文档与门禁
   - `daily.md`
   - `flt64-scan-gate.ps1`
   - `MIGRATION_G9.md` 更新

### 3.6 验收标准

1. G5-G9 已迁移字段、snapshot、summary、time/calendar quantity helper、pre-solver adapter、resource/produce solved quantity、TaskTime 全部 8 个 quantity helper 的旧路径和新泛型路径均保持编译和测试通过。
2. service/application public API 确认不直接暴露 solver 建模对象（`BranchAndPriceAlgorithm` public API 清洁）。
3. 第一批 deprecated 覆盖 `SlotBasedCapacityResult`、`CapacityIntermediateValues`、`SlotConstraints` 的裸值构造函数/工厂和裸值属性。
4. 续轮 deprecated 覆盖 `Cost`、`CapacityColumn`、`AbstractResourceCapacity`、`ResourceCapacity`、`Resource`、`MaterialDemand`、`MaterialReserves`、`ProductionTask` 的裸值属性/构造函数/Flt64 typealias。
5. `ProductionAction.unitCapacityV`、`unitCostV` deprecated 覆盖完成，`unitCapacity`、`unitCost`、`upperBound` 继续作为 solver boundary 保留。
6. demo4 成本替代路径使用 Quantity 主字段，example reactor 编译通过。
7. `Domain DTO Pending`、`Time/Calendar Boundary`、`Application Algorithm Internal` 分类保持不变，`unclassified=0`。
8. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
9. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
10. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
11. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 通过。

## 4. 向后兼容要求

继续保留旧 `Flt64` wrapper 和 typealias，例如：

```kotlin
typealias Flt64Cost = Cost<Flt64>
typealias Flt64CapacityColumn<E, A> = CapacityColumn<E, A, Flt64>
typealias Flt64SlotBasedCapacityResult<A, M, R> = SlotBasedCapacityResult<A, M, R, Flt64>
```

旧 application 入口保留为 `Flt64` wrapper，新入口显式带 `<V>`。所有 wrapper 的目的只是兼容旧调用，不应成为新业务代码的主路径。总目标完成后，兼容入口应按 deprecated 到移除流程退出。

## 5. 风险与约束

| 风险 | 说明 | 缓解 |
|------|------|------|
| solver 只接受 `Flt64` | 领域 `V` 无法直接入模 | adapter/model boundary 集中转换 |
| shadow price 基础 API 固定 `Flt64` | framework 层泛型化影响面大 | 本阶段继续使用 adapter 隔离 |
| public API 泛型参数变更 | 旧源码调用可能不再按原名原参数数编译 | 提供旧构造、工厂或 wrapper，并逐步 deprecated |
| deprecated 范围过大 | 调用方可能短期无法完成迁移 | 分层标记，先文档化替代路径，再逐步提高级别 |
| 物理量化范围扩大 | `Quantity<V>` 会触发跨模块签名传播 | 按依赖顺序推进并保持每阶段测试可运行 |
| example 编译耗时 | example reactor 会连带编译多个 framework | 保留可复现命令，并以 gantt reactor 作为最低门槛 |

## 6. 当前基线与执行入口（2026-06-08）

### 当前状态一览

| 模块 | 当前状态 | 剩余重点 |
|------|----------|----------|
| infrastructure | `TimeWindow<V>`、`WorkingCalendar` duration helper 已可返回 `Quantity<V>` | 时间 DTO deprecated 策略 |
| task/cost | `CostItem.costQuantity`、`Cost.costSum` 为主字段 | 旧裸值入口 deprecated 策略 |
| task-compilation | `TaskTime` 全部 8 个 quantity helper（含 `overMaxDelayTime`/`overMaxAdvanceTime`）、`Switch`、`Makespan` 已覆盖 | solution analyzer facade |
| capacity | `CapacityColumn.columnCost`、duration helper、capacity solution duration helper 已覆盖 | order/result facade 与 `ProductionAction` deprecated |
| resource | `ResourceCapacity` 主字段、usage solved quantity、内部 capacity/usage 读取已覆盖 Quantity 主路径 | result facade 与旧入口退出策略 |
| produce | demand/reserves、task quantity map、produce/consumption solved quantity、内部 demand/reserve 读取已覆盖 Quantity 主路径 | result facade 与旧入口退出策略 |
| bunch-compilation | `SlotBasedCapacityResult`、`SlotConstraints` 裸值入口已标记 deprecated，pre-solver generic adapter 已覆盖 | 继续扩展 deprecated |
| application | algorithm 签名泛型，iteration snapshot 已补充，public API 已清洁 | service/result facade 与旧入口 deprecated |
| example/demo4 | 覆盖旧裸值、新 `Quantity` 字段、time/calendar、snapshot、summary、solved quantity、switch quantity、overMax quantity、成本替代路径样例 | 后续 facade 替代路径 compile sample |

### 验证命令

```bash
mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test
mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile
pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1
git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example
```

### 当前 scan gate 基线

main=1,402 / test=204 / total=1,606 / unclassified=0
