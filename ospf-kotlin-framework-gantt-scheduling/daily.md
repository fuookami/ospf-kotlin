# Gantt Scheduling 泛型化计划

日期：2026-06-05（最后更新：2026-06-07 G6 计划草案）

## 1. 总目标

`ospf-kotlin-framework-gantt-scheduling` 的长期目标是完成领域数值泛型化与物理量化，同时保持现有 `Flt64` 应用路径可逐步迁移。

核心目标：

1. 领域层的时间、数量、成本、资源用量、产出消耗等有量纲字段支持 `Quantity<V>`。
2. solver/model 层可以继续使用 `Flt64`，但 `Flt64` 必须集中在 adapter、model boundary 或算法边界。
3. `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`LinearIntermediateSymbols<Flt64>` 等建模类型不再从 application/service API 向领域对象泄漏。
4. `Cost<V>`、`ShadowPriceMap<V>`、`CapacityColumn<V>`、`SlotBasedCapacityResult<V>` 等领域结果模型支持 `Flt64` 与 `FltX`。
5. 迁移期保留 `Flt64` typealias、wrapper 和旧入口，确保现有应用可按阶段迁移；当领域 API、example、application 调用方和 scan gate 均完成泛型/物理量路径验证后，旧兼容入口应进入 deprecated 到移除流程，最终状态不依赖 `Flt64` 兼容层。
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
4. 已为 capacity、resource、produce、task/cost 增加 `Quantity<V>` helper 和 Flt64 兼容 typealias，作为字段级物理量化前的稳定迁移层。
5. 已补充 Flt64 legacy、FltX generic、Quantity<FltX>、adapter conversion、demo4/example compile 的测试或编译验证。
6. 已新增 `MIGRATION_G5.md`，记录旧 API 到泛型/Quantity API 的迁移路径、保留边界和字段替换条件。
7. 当前门禁基线：`flt64-scan-gate.ps1` 输出 main=1,347 / test=126 / total=1,473 / unclassified=0。
8. 最近一次验证已通过 gantt-scheduling reactor 测试、example reactor 编译和 `git diff --check`。

当前允许保留的边界：

1. solver 建模对象、solver 内部结果、pre-solve 模型继续固定 `Flt64`。
2. framework shadow price 基础 API 继续固定 `Flt64`，通过 `reducedCost<V>` 与 adapter 隔离。
3. 旧 wrapper、typealias、legacy API 和兼容测试可继续使用 `Flt64`。
4. `ProductionAction.unitCost` 保留 `Flt64` 签名作为 solver 边界入口；新增 `unitCostV<V>(time, fromDouble)` 与 `unitCostQuantity<V>` 泛型版本，调用方通过 `fromDouble` 提供 V 转换。
5. application `Iteration` 的目标值、上下界和 `Policy.reducedCost` 保持 `Flt64`，这是 branch-and-price 算法内部和 shadow price 边界；领域对象通过 `V` 与 adapter 隔离。

## 3. 当前目标：G6 字段级物理量化、时间/DTO 泛型化与 application 边界压缩

G6 目标是在保留 solver 数值内核 `Flt64` 的前提下，尽可能一次性推进字段级 `Quantity<V>` 替换、时间/成本/数量 DTO 边界泛型化、application 结果模型泛型化和 scan gate 收口，减少后续小步迭代。

### 3.1 目标

1. 将已稳定的 `Quantity<V>` helper 向字段级模型推进，优先覆盖 capacity、resource、produce、task/cost 的核心有量纲字段。
2. 为旧裸 `V` 构造路径提供兼容构造器、工厂或 adapter，确保现有 `Flt64` 源码调用仍可编译。
3. 压缩 `Documented Pending` 和 `Algorithm Internal` 中可迁移的 `Flt64` 使用点，保留项必须指向清晰的 solver/model/application 边界。
4. 推进 task/bunch solution、solution analyzer、summary DTO、iteration result 的泛型和物理量表达，避免成本、时间、数量继续混用裸 `V`。
5. 评估并尽量泛型化 application `Iteration` 中的目标值、上下界和结果回填；若仍保留 `Flt64`，必须保留为算法内部边界并有文档依据。
6. 将 demo4 从 compile-only 样例扩展为可表达泛型构造、Quantity 字段和旧路径兼容的使用方验证。
7. 更新扫描门禁规则、迁移说明和 daily，使下一轮后剩余工作只集中在 solver 内核或明确的跨框架 shadow price 边界。

### 3.2 事项

1. **字段级 Quantity 替换**
   对 `CapacityColumn.cost`、capacity compilation/order result、`ResourceCapacity.quantity/lessQuantity/overQuantity`、resource usage/slack、`MaterialDemand`、`MaterialReserves`、produce/consumption limit、`CostItem.value`、`Cost.sum` 进行字段替换评估与落地。

2. **兼容构造与 adapter**
   为字段替换后的类型补旧构造器、静态工厂、`fromRaw`/`toRaw` adapter 或 typealias，确保旧 `Flt64` 调用和现有 solver 入模路径不被破坏。

3. **时间与结果 DTO 泛型化**
   审计 `TaskPlan`、`TaskTime`、makespan、switch time/cost、delay/advance、solution analyzer、task/bunch scheduling solution、summary DTO，将可迁移字段推进到 `V` 或 `Quantity<V>`。

4. **application 边界压缩**
   审计 task/bunch branch-and-price application、`Iteration`、policy、reduced cost、objective/lower bound 回填路径，尽量将 application 对外结果提升为 `V` 或 `Quantity<V>`。

5. **scan gate 深化**
   收紧 `flt64-scan-gate.ps1` 分类，区分字段级 pending、DTO pending、application internal、solver-only，并降低 documented pending 数量。

6. **测试矩阵扩展**
   扩展 Flt64 legacy 构造、FltX field-level construction、Quantity<FltX> DTO、adapter conversion、application result construction、demo4 compile 的覆盖。

7. **example/demo4 同步**
   将 demo4 扩成下一轮 API 的固定使用方，覆盖旧 `Flt64` 路径和新 `FltX`/Quantity 路径。

8. **文档与迁移说明**
   更新 `MIGRATION_G5.md` 或新增 G6 迁移说明，明确字段替换后的旧入口、推荐新入口、单位默认策略和剩余 solver 边界。

### 3.3 计划

1. 先跑 scan gate，按文件清单和分类统计确定 G6 起点，标出 documented pending、algorithm internal、DTO pending 的最大来源。
2. 按依赖顺序推进字段替换：task/cost 基础类型 -> capacity -> resource -> produce -> task/bunch compilation result -> application result。
3. 每替换一组字段，同步补兼容构造、adapter 和 Flt64/FltX/Quantity 测试，避免最后集中处理源码兼容。
4. 在 DTO 和 solution analyzer 层统一清理成本、数量、时间的命名与单位策略，避免不同模块各自发明 wrapper。
5. application `Iteration` 和 branch-and-price 结果先做对外 API 泛型化；无法迁移的内部值继续归为 algorithm internal，并在 scan gate 中明确。
6. demo4 在每个阶段保持可编译，最终包含旧 `Flt64` 和新 `FltX`/Quantity 两条构造路径。
7. 最后收紧 scan gate、更新迁移说明、压缩 daily，并跑完整验收命令。

### 3.4 修改清单

预计涉及：

1. `gantt-scheduling-infrastructure`
   - `TimeWindow`
   - `TimeRange` / 时间 DTO 边界
   - 时间物理量 helper 与单位策略

2. `gantt-scheduling-domain-task-context`
   - `TaskPlan`
   - `Task`
   - `TaskBunch`
   - `Cost`
   - `ShadowPriceMap`
   - `SchedulingSolverValueAdapter`
   - 旧裸值构造兼容层

3. `gantt-scheduling-domain-task-compilation-context`
   - aggregation / compilation / solution / limits
   - task time / makespan / switch 等时间与成本边界
   - solution analyzer 与 summary DTO

4. `gantt-scheduling-domain-bunch-compilation-context`
   - `SlotBasedBunchCompilation*`
   - `TaskReverseBuilder`
   - `SlotBasedCapacityResult`
   - capacity pre-solver / slot constraints
   - bunch compilation solution/result DTO

5. `gantt-scheduling-domain-bunch-generation-context`
   - `Label`
   - total cost calculator
   - bunch generator service

6. `gantt-scheduling-domain-capacity-scheduling-context`
   - `ProductionAction`
   - `CapacityColumn`
   - `CapacityCompilation`
   - `CapacityOrderCompilation`
   - order / limit service

7. `gantt-scheduling-domain-resource-context`
   - resource model
   - resource usage
   - capacity/resource constraints and objectives
   - resource slack 与 usage result

8. `gantt-scheduling-domain-produce-context`
   - material demand/reserves
   - produce/consumption model
   - produce/consumption constraints and objectives
   - produce/consumption slack 与 limits result

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
   - migration note / README（如需要）

### 3.5 验收标准

1. 旧 `Flt64` public API 源码调用仍具备明确兼容路径；字段替换不破坏现有构造器或提供替代兼容工厂。
2. capacity、resource、produce、task/cost 的核心有量纲字段完成 `Quantity<V>` 字段级迁移，无法替换项必须归入清晰边界。
3. task/bunch solution、solution analyzer、summary DTO 和 application 对外 result 不再泄漏 solver 建模类型。
4. application `Iteration` 目标值/上下界完成泛型化，或被压缩为明确 algorithm internal 并有 adapter 返回泛型结果。
5. scan gate `unclassified=0`；`Documented Pending` 数量较 G5 基线明显下降，新增分类能解释所有保留 `Flt64`。
6. Flt64 legacy、FltX generic、Quantity<FltX> field construction、adapter conversion、application result construction、demo4 compile 均有测试或编译验证。
7. `MIGRATION_G5.md` 或 G6 migration note 更新字段替换后的迁移方式、单位策略和剩余边界。
8. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
9. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过；若因基础设施失败，需记录具体错误和可复现命令。
10. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
11. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 通过。
12. `daily.md` 保持总目标、阶段完成概要、当前边界和下一轮计划，不累积执行细节。

### 3.6 当前完成度与剩余工作

当前完成的是 G5 收口，不是整体完全泛型化。领域 API 已具备 `V` 泛型和 `Quantity<V>` 迁移入口，但核心字段仍大量保留裸 `V`，solver/model/application 内部仍有明确 `Flt64` 边界。

G6 需要把 helper 迁移为字段级表达，并同步处理 DTO、solution、application result 和兼容构造。G6 完成后，剩余工作应只包括 solver 内核、跨框架 shadow price 基础设施，或经 scan gate 明确归类的算法内部无量纲值。

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

## 6. 当前基线与执行入口（2026-06-07）

### 当前状态一览

| 模块 | 核心类型 V 泛型 | Flt64 使用归类 | FltX 测试 |
|------|----------------|----------------|-----------|
| capacity | `Aggregation<V,A>`, `Constraint<V,A>`, `Minimization<V,A>`, `ProductionAction.unitCostV`, `*Quantity` helpers | solver boundary + compat typealias | 17 cases |
| resource | `ResourceCapacity<V>`, `Resource<C,V>`, `ResourceTimeSlot<R,C,V>`, `Constraint<...,V>`, `*Quantity` helpers | solver boundary + adapter conversion | 17 cases |
| produce | `MaterialDemand<V>`, `MaterialReserves<V>`, `ProductionTask<...,V>`, `produceV`/`consumptionV`, `*Quantity` helpers | solver boundary + adapter conversion | 14 cases |
| task/cost | `Cost<V>`, `TaskBunch<...,V>`, `SolverValueAdapter<V>`, `ShadowPriceMap` reducedCost, `CostQuantity` helpers | solver boundary + adapter | 3 cases |
| bunch-compilation | `SlotBasedBunchCompilationV<V>`, `SlotBasedCapacityResult<V>`, `BunchCompilation<V>` | solver boundary + pre-solver | 已有 |
| bunch-generation | `LabelV<T,E,A,V>`, `TotalCostCalculatorV<V>` | 无 Flt64 泄漏 | 已有 |
| application | `BranchAndPriceAlgorithm<V>` 签名泛型 | `Iteration` 全 Flt64，`Policy.reducedCost -> Flt64` | 待扩展 |

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

### 本轮提交

G5 收口修改已提交为 `f02d70db`：`完成 gantt-scheduling G5 泛型与 Quantity 辅助入口收口`。
