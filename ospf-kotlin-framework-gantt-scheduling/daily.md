# Gantt Scheduling 泛型化计划

日期：2026-06-05（最后更新：2026-06-07 G5 全量收口完成）

## 1. 总目标

`ospf-kotlin-framework-gantt-scheduling` 的长期目标是完成领域数值泛型化与物理量化，同时保持现有 `Flt64` 应用路径可逐步迁移。

核心目标：

1. 领域层的时间、数量、成本、资源用量、产出消耗等有量纲字段支持 `Quantity<V>`。
2. solver/model 层可以继续使用 `Flt64`，但 `Flt64` 必须集中在 adapter、model boundary 或算法边界。
3. `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>` 等建模类型不再从 application/service API 向领域对象泄漏。
4. `Cost<V>`、`ShadowPriceMap<V>`、`CapacityColumn<V>`、`SlotBasedCapacityResult<V>` 等领域结果模型支持 `Flt64` 与 `FltX`。
5. 保留 `Flt64` typealias、wrapper 和旧入口，确保现有应用可按阶段迁移。
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

1. G0-G2.5 已建立泛型化基线、扫描基线、基础领域类型、`TimeWindow<V>` 与 `ProductivityCalendar` 的泛型/物理量路径。
2. G3 已完成 solver adapter、Task/Bunch compilation 主链路、Capacity/Resource/Produce 上下文、Application branch-and-price 入口与 `ShadowPriceMap` 隔离边界。
3. G4 已完成 slot-based bunch、bunch generation label、task reverse 与 `ProductionAction` 的 legacy 边界收敛。
4. G4 已补充 capacity、resource、produce、bunch compilation 的 FltX 覆盖，共 24 个新增测试用例。
5. G4 已将 `Flt64` 扫描门禁切换为 PowerShell 脚本，适配当前 Windows 工作区；当前 main 扫描为 1,321 行 / 103 文件。
6. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 已通过，11 模块 BUILD SUCCESS。
7. `git diff --check -- ospf-kotlin-framework-gantt-scheduling` 已通过，仅存在 LF/CRLF 工作区提示。
8. demo4 当前未直接引用本轮调整的 SlotBased/Label/TaskReverseBuilder/ProductionAction API，暂未修改。
9. G5 已将 `flt64-scan-gate.ps1` 扩展为逐行分类门禁，输出 solver boundary、adapter conversion、compat wrapper、legacy API、algorithm internal、documented pending、test、import/comment 和 unclassified 分类；未归类项非 0 时脚本失败。
10. G5 当前扫描基线：main 1,321 行 / test 50 行 / total 1,371 行，未归类 main `Flt64` 使用点为 0。
11. G5 public API 第一轮审计确认 `Label`、`TaskReverseBuilder`、`SlotBasedBunchCompilation`、`SlotBasedBunchAggregation`、`SlotBasedBunchCompilationContext` 已采用 `*V` 泛型实现 + `Flt64` typealias 兼容模式。
12. G5 已修复 `LabelV.generateBunch` 回溯任务链时重复加入终点 label 的问题，新增 `LabelGenerateBunchTest` 覆盖 `Flt64Label` legacy 路径与 `LabelV<FltX>` 泛型路径。
13. 本轮 demo4 未直接引用 `Label.generateBunch` / `TotalCostCalculator` API，未修改；`mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 已通过，37 模块 BUILD SUCCESS。
14. G5 capacity 模块泛型化第二轮：`CapacitySchedulingAggregation<V, A>` 从 `TimeWindow<Flt64>` 提升为 `TimeWindow<V>`，`totalCapacity()` 返回 `V`；`ExecutorCapacityConstraint<V, A>` 和 `CapacityCostMinimization<V, A>` 泛型化 `timeWindow` 字段，solver 边界 `LinearMetaModel<Flt64>` 保留不变。
15. G5 为 `ProductionAction` 新增 `unitCostV<V>(time, fromDouble)` 泛型入口，允许调用方通过 `fromDouble` 转换器获取 `V` 类型成本值。
16. G5 新增 `AggregationFltXTest`（4 用例）和 `ProductionActionUnitCostVTest`（4 用例），覆盖 FltX 聚合、Flt64 typealias 兼容、unitCostV/unitCapacityV/upperBoundV 泛型路径；capacity 模块测试共 13 用例全部通过。
17. 新增 3 个 Flt64 兼容 typealias：`Flt64CapacitySchedulingAggregation`、`Flt64ExecutorCapacityConstraint`、`Flt64CapacityCostMinimization`。
18. 全量验证通过：`mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 11 模块 BUILD SUCCESS；`mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 37 模块 BUILD SUCCESS。
19. 当前扫描基线：main 1,328 行 / test 69 行 / total 1,397 行，未归类 main `Flt64` 使用点为 0。`git diff --check` 通过（仅 LF/CRLF 工作区提示）。
20. G5 resource 模块 V 泛型审计确认：`AbstractResourceCapacity<V>`、`Resource<C, V>`、`ResourceTimeSlot<R, C, V>`、`ResourceCapacityConstraint<..., V>`、`ResourceLessQuantityMinimization<..., V>`、`ResourceOverQuantityMinimization<..., V>` 全部已 V 泛型化；129 处 Flt64 使用全部归类为 solver boundary（`MetaModel<Flt64>`、`LinearExpressionSymbol<Flt64>`、`.toFlt64()` adapter 转换）。
21. G5 新增 `ResourceGenericFltXTest`（9 用例），覆盖 `resourceQuantityZero` FltX/Flt64 路径、`StorageResourceTimeSlot`/`ExecutionResourceTimeSlot`/`ConnectionResourceTimeSlot` 的 FltX relationTo、V→Flt64 solver 边界转换；resource 模块共 15 用例全部通过。
22. G5 produce 模块 V 泛型审计确认：`MaterialDemand<V>`、`MaterialReserves<V>`、`ProductionTask<E, A, P, C, V>`、`AbstractProduce<..., V>`、`AbstractConsumption<..., V>`、`produceV`/`consumptionV` 扩展函数全部已 V 泛型化；199 处 Flt64 使用全部归类为 solver boundary。
23. G5 新增 `ProduceGenericFltXTest`（5 用例），覆盖 `Flt64MaterialDemand`/`Flt64MaterialReserves` typealias 兼容、V→Flt64 转换边界、高精度 FltX 需求；produce 模块共 11 用例全部通过。
24. G5 task/cost/result + application 层全域审计完成：`Cost<V>`、`AbstractTaskBunch<..., V>`、`SchedulingSolverValueAdapter<V>`、`LabelV<T, E, A, V>`、`SlotBasedCapacityResult<V>`、`BunchCompilation<V>` 全部已 V 泛型化，具备 Flt64 typealias 兼容路径。application 层 `BranchAndPriceAlgorithm<V>` 签名泛型化、`Iteration` 类全 Flt64（solver 算法内部）、`Policy.reducedCost -> Flt64`（shadow price 保留边界），符合 G5 保留策略。
25. 全量验证通过：`mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 11 模块 BUILD SUCCESS；`mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` BUILD SUCCESS；scan gate main=1,328 test=125 total=1,453 unclassified=0；`git diff --check` 通过。
26. G5 capacity Quantity 辅助入口：gantt-scheduling 聚合 POM 显式依赖 `ospf-kotlin-quantities`；新增 `CapacityQuantity<V>`、`CapacityCostQuantity<V>`、`Flt64CapacityQuantity`、`Flt64CapacityCostQuantity` typealias；`ProductionAction` 新增 `unitCapacityQuantity`/`unitCostQuantity`，`CapacityColumn` 新增 `costQuantity`，`CapacitySchedulingAggregation` 新增 `totalCapacityQuantity`，旧 `Flt64` 方法和 typealias 保持不变。
27. 新增 capacity `Quantity<FltX>` 覆盖：`totalCapacityQuantityShouldSupportFltX`、`unitCapacityQuantityShouldSupportFltX`、`unitCostQuantityShouldSupportFltX`、`capacityColumnCostQuantityShouldSupportFltX`。
28. 本轮验证通过：`mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 11 模块 BUILD SUCCESS；`$env:MAVEN_OPTS='-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=256m'; mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 37 模块 BUILD SUCCESS；scan gate main=1,333 test=125 total=1,458 unclassified=0；`git diff --check` 通过（仅 LF/CRLF 工作区提示）。
29. G5 resource Quantity 辅助入口：新增 `ResourceQuantity<V>`、`ResourceQuantityRange<V>`、`ResourceCostQuantity<V>` 及 Flt64 兼容 typealias；`AbstractResourceCapacity` 新增 `quantityRange`/`lessQuantity`/`overQuantity`，`Resource` 新增 `initialQuantity`/`usedQuantityQuantity`，旧裸 `V` 字段保持不变。
30. G5 produce Quantity 辅助入口：新增 `MaterialQuantity<V>`、`MaterialQuantityRange<V>` 及 Flt64 兼容 typealias；`MaterialDemand`/`MaterialReserves` 新增数量范围和 slack 的 Quantity wrapper，`ProductionTask` 与 `AbstractTaskBunch` 生产/消耗量新增 Quantity wrapper。
31. G5 task/cost Quantity 辅助入口：新增 `CostQuantity<V>`、`Flt64CostQuantity`，`CostItem.quantity` 与 `Cost.sumQuantity` 支持成本物理量表达。
32. demo4 新增 `Demo4GenericQuantitySample`，覆盖 `FltX` 成本、物料需求、资源容量与 `Quantity` helper 的 compile-only 构造路径。
33. 新增 `MIGRATION_G5.md`，记录 `Flt64` 兼容路径、泛型 `V` + Quantity helper 优先路径、solver/application 保留边界与后续字段替换条件。

当前允许保留的边界：

1. solver 建模对象、solver 内部结果、pre-solve 模型继续固定 `Flt64`。
2. framework shadow price 基础 API 继续固定 `Flt64`，通过 `reducedCost<V>` 与 adapter 隔离。
3. 旧 wrapper、typealias、legacy API 和兼容测试可继续使用 `Flt64`。
4. `ProductionAction.unitCost` 保留 `Flt64` 签名作为 solver 边界入口；新增 `unitCostV<V>(time, fromDouble)` 与 `unitCostQuantity<V>` 泛型版本，调用方通过 `fromDouble` 提供 V 转换。
5. application `Iteration` 的目标值、上下界和 `Policy.reducedCost` 保持 `Flt64`，这是 branch-and-price 算法内部和 shadow price 边界；领域对象通过 `V` 与 adapter 隔离。

## 3. 当前目标：G5 全域 API 兼容、物理量化与门禁收口

G5 目标是在不改动 solver 数值内核的前提下，尽可能一次性完成 gantt-scheduling 领域 API 的泛型兼容、物理量表达、扫描归类和 example 验证，减少后续小步迭代。

### 3.1 目标

1. 形成稳定的 public API 迁移模式：泛型实现使用显式 `V` 名称或新入口，旧 `Flt64` 源码调用保留可编译路径。
2. 将剩余领域层 `Flt64` 使用点压缩到明确边界，并让扫描门禁输出可审计的未归类清单。
3. 将 capacity、resource、produce、task、cost、time 相关有量纲字段推进到 `Quantity<V>` 或明确的时间/成本边界类型。
4. 拓展 FltX/Quantity<FltX> 测试到 application 构造层、legacy wrapper 层和 demo4/example 编译层。
5. 整理文档、README 或 migration note，明确旧 API 到泛型 API 的迁移路径。

### 3.2 事项

1. **Public API 源码兼容收口**
   检查 `Label`、`TaskReverseBuilder`、`SlotBasedBunchCompilation`、`SlotBasedBunchAggregation`、`SlotBasedBunchCompilationContext` 等新增泛型参数的源码兼容性；必要时采用 `*V` 泛型实现 + 原名 `Flt64` typealias 的模式。

2. **Flt64 边界归类收口**
   扩展 `flt64-scan-gate.ps1`，把保留项细分为 solver boundary、adapter conversion、compat wrapper、legacy API、algorithm internal、test、documented pending，并输出未归类项数量。

3. **容量与动作物理量化**
   推进 `ProductionAction`、`CapacityColumn`、capacity pre-solver、中间值和 slot constraints 的 `Quantity<V>` 表达；评估 `unitCost` 的成本边界类型和默认转换策略。

4. **资源与产出消耗物理量化**
   推进 `ResourceCapacity`、resource slack、`MaterialDemand`、`MaterialReserves`、produce/consumption limit 与对应 minimization/maximization service 的 `Quantity<V>` 路径。

5. **任务与成本结果物理量化**
   检查 `TaskPlan`、`TaskBunch`、`Cost<V>`、`TaskSchedulingSolution`、`BunchSchedulingSolution`、iteration result 和 summary DTO，避免成本/时长/数量混用裸 `V`。

6. **Application 与 solver adapter 扩展**
   收敛 branch-and-price task/bunch application API，确保领域对象只持有 `V` 或 `Quantity<V>`，solver 入模和求解结果回填只通过 adapter 边界完成。

7. **example/demo4 同步**
   将 demo4 纳入每次 API 调整后的编译检查；如果旧路径继续保留，显式固定到 `Flt64` wrapper；如果迁移到泛型路径，补齐示例说明。

8. **测试矩阵扩展**
   补齐 Flt64 legacy、FltX generic、Quantity<FltX>、adapter conversion、application construction、demo4 compile 的测试或编译验证。

9. **文档与迁移说明**
   更新 `daily.md`、必要 README/migration note 和扫描门禁说明，记录保留边界、迁移条件和下一步删除条件。

### 3.3 计划

1. 已跑 `flt64-scan-gate.ps1` 锁定 G5 起点，并补齐未归类规则和失败条件。
2. 已完成 public API 兼容第一轮审计，并补齐 `LabelV.generateBunch` 的 legacy/generic 回归测试。
3. 下一步继续按 capacity -> resource -> produce -> task/cost -> application 的依赖顺序推进 `Quantity<V>`，每个阶段同步 adapter 边界。
4. 每收敛一组 API 就补一组 FltX/Quantity<FltX>/legacy wrapper 测试，避免最后集中修编译。
5. demo4/example 已纳入验证；后续涉及对外 API 时继续同步检查。
6. 每轮收口继续跑扫描门禁、gantt-scheduling reactor、example reactor、`git diff --check`，再压缩文档完成状态。

### 3.4 修改清单

预计涉及：

1. `gantt-scheduling-infrastructure`
   - `TimeWindow`
   - `TimeRange` / DTO 边界

2. `gantt-scheduling-domain-task-context`
   - `TaskPlan`
   - `Task`
   - `TaskBunch`
   - `Cost`
   - `ShadowPriceMap`
   - `SchedulingSolverValueAdapter`

3. `gantt-scheduling-domain-task-compilation-context`
   - aggregation / compilation / solution / limits
   - task time / makespan / switch 等时间与成本边界

4. `gantt-scheduling-domain-bunch-compilation-context`
   - `SlotBasedBunchCompilation*`
   - `TaskReverseBuilder`
   - `SlotBasedCapacityResult`
   - capacity pre-solver / slot constraints

5. `gantt-scheduling-domain-bunch-generation-context`
   - `Label`
   - total cost calculator
   - bunch generator service

6. `gantt-scheduling-domain-capacity-scheduling-context`
   - `ProductionAction`
   - `CapacityColumn`
   - `CapacityCompilation`
   - order / limit service

7. `gantt-scheduling-domain-resource-context`
   - resource model
   - resource usage
   - capacity/resource constraints and objectives

8. `gantt-scheduling-domain-produce-context`
   - material demand/reserves
   - produce/consumption model
   - produce/consumption constraints and objectives

9. `gantt-scheduling-application`
   - branch-and-price task/bunch API
   - iteration result / policy construction
   - solver adapter use sites

10. `ospf-kotlin-example`
    - `framework_demo/demo4`
    - example reactor compile support

11. 文档与门禁
    - `daily.md`
    - `flt64-scan-gate.ps1`
    - README 或 migration note（如需要）

### 3.5 验收标准

1. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 可运行，并输出 main/test 统计、分类统计、未归类清单和基线值。
2. 未归类 main `Flt64` 使用点为 0；保留项全部归类为 solver boundary、adapter conversion、compat wrapper、legacy API、algorithm internal、test 或 documented pending。
3. 旧 `Flt64` public API 源码调用具备明确兼容路径；新增泛型 public API 命名和类型参数语义一致。
4. capacity、resource、produce、task、cost、time 相关有量纲字段完成 `Quantity<V>` 迁移或记录清晰的待迁移原因。
5. `ProductionAction.unitCost`、capacity pre-solver、shadow price 基础 API 等保留边界均有 adapter 或文档化迁移条件。
6. Flt64 legacy、FltX generic、Quantity<FltX>、adapter conversion、application construction 至少各有一组测试或编译验证。
7. demo4 与旧 `Flt64` 应用路径通过包含 example 的 reactor 编译验证。
8. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
9. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过；若因基础设施失败，需记录具体错误和可复现命令。
10. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 通过。
11. `daily.md` 只保留阶段性完成概要、当前边界和下一轮计划，不累积冗长执行细节。

### 3.6 当前完成度与剩余工作

G5 已完成。当前已形成稳定的迁移模式：领域模型保留旧裸 `V` 字段和旧 `Flt64` 兼容入口，同时为 capacity、resource、produce、task/cost 增加 `Quantity<V>` helper；solver/model/application 算法内部边界继续固定 `Flt64`，通过 adapter、`unitCostV`、`reducedCost<V>` 和文档说明隔离。

本阶段不直接把现有构造器字段替换为 `Quantity<V>`，原因是会破坏旧源码调用和 solver 入模路径。后续字段级替换的前置条件是：提供旧构造器或工厂兼容层、明确单位来源、补齐 DTO/序列化迁移，并保持 scan gate unclassified=0。

G5 收口验证基线：scan gate main=1,347 / test=126 / total=1,473 / unclassified=0；gantt-scheduling reactor 测试、example reactor 编译和空白检查均作为最终验收命令。

## 4. 向后兼容要求

继续保留旧 `Flt64` wrapper 和 typealias，例如：

```kotlin
typealias Flt64Cost = Cost<Flt64>
typealias Flt64CapacityColumn<E, A> = CapacityColumn<E, A, Flt64>
typealias Flt64SlotBasedCapacityResult<M, R> = SlotBasedCapacityResult<M, R, Flt64>
```

旧 application 入口保留为 `Flt64` wrapper，新入口显式带 `<V>`。所有 wrapper 的目的只是兼容旧调用，不应成为新业务代码的主路径。

## 5. 风险与约束

| 风险 | 说明 | 缓解 |
|------|------|------|
| solver 只接受 `Flt64` | 领域 `V` 无法直接入模 | adapter/model boundary 集中转换 |
| shadow price 基础 API 固定 `Flt64` | framework 层泛型化影响面大 | 本阶段继续使用 `reducedCost<V>` 隔离 |
| public API 泛型参数变更 | 旧源码调用可能不再按原名原参数数编译 | G5 优先审计并补原名 typealias 或 wrapper |
| 物理量化范围扩大 | `Quantity<V>` 会触发跨模块签名传播 | 按依赖顺序推进并保持每阶段测试可运行 |
| example 编译依赖外部仓库 | 基础设施失败可能掩盖 API 问题 | 记录具体错误，并保留 gantt-scheduling reactor 作为最低门槛 |

## 6. G5 完成交接（2026-06-07）

### 本轮已完成

G5 完成了 public API 源码兼容、Flt64 边界归类、capacity/resource/produce/task/cost 的泛型和 Quantity helper、demo4 泛型构造样例、迁移说明与最终门禁。

### 当前 V 泛型状态一览

| 模块 | 核心类型 V 泛型 | Flt64 使用归类 | FltX 测试 |
|------|----------------|----------------|-----------|
| capacity | `Aggregation<V,A>`, `Constraint<V,A>`, `Minimization<V,A>`, `ProductionAction.unitCostV`, `*Quantity` helpers | solver boundary + compat typealias | 17 cases |
| resource | `ResourceCapacity<V>`, `Resource<C,V>`, `ResourceTimeSlot<R,C,V>`, `Constraint<...,V>`, `*Quantity` helpers | solver boundary + adapter conversion | 17 cases |
| produce | `MaterialDemand<V>`, `MaterialReserves<V>`, `ProductionTask<...,V>`, `produceV`/`consumptionV`, `*Quantity` helpers | solver boundary + adapter conversion | 14 cases |
| task/cost | `Cost<V>`, `TaskBunch<...,V>`, `SolverValueAdapter<V>`, `ShadowPriceMap` reducedCost, `CostQuantity` helpers | solver boundary + adapter | 3 cases |
| bunch-compilation | `SlotBasedBunchCompilationV<V>`, `SlotBasedCapacityResult<V>`, `BunchCompilation<V>` | solver boundary + pre-solver | 已有 |
| bunch-generation | `LabelV<T,E,A,V>`, `TotalCostCalculatorV<V>` | 无 Flt64 泄漏 | 已有 |
| application | `BranchAndPriceAlgorithm<V>` 签名泛型 | `Iteration` 全 Flt64，`Policy.reducedCost -> Flt64` | 无 |

### 后续推荐优先级

1. **字段级 Quantity 替换评估**：在 helper 稳定后，评估 `CapacityColumn.cost`、resource capacity/slack、material demand/reserves、cost sum 等字段正式替换为 `Quantity<V>` 的兼容工厂和 DTO 策略。
2. **时间物理量与 DTO 边界**：继续评估 `TimeWindow` 映射值、summary DTO、solution analyzer 输出是否需要更严格的时间/成本单位类型。
3. **application `Iteration` 泛型化评估**：`bestObj`/`lowerBound` 保留为算法内部 `Flt64`，只有当 solver adapter 支持完整回填 `V` 目标值时再提升。

### 验证命令

```bash
# 全量 reactor 测试（11 模块）
mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test

# example reactor 编译（含 demo4）
mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile

# 扫描门禁（unclassified 必须为 0）
pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1

# 空白检查
git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example
```

### 当前 scan gate 基线

main=1,347 / test=126 / total=1,473 / unclassified=0
