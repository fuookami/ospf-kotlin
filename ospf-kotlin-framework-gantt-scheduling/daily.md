# Gantt Scheduling 泛型化计划

日期：2026-06-07（最后更新：G7 时间/DTO/application 边界收口）

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
5. 已完成 G7 时间/DTO/application 边界收口，新增 application iteration snapshot、TimeWindow 物理量 helper、task/bunch solution summary，并深化 scan gate 分类。
6. 已保留旧裸 `V` 构造、裸值属性、helper 和 `Flt64` typealias，兼容层继续用于迁移期。
7. 已补充 Flt64 legacy、FltX generic、Quantity<FltX> 字段构造、adapter conversion、time quantity、solution summary、application snapshot、demo4/example compile 的测试或编译验证。
8. 已新增 `MIGRATION_G5.md`、`MIGRATION_G6.md`，记录迁移路径、旧入口、单位默认策略和剩余边界。
9. 当前门禁基线：`flt64-scan-gate.ps1` 输出 main=1,355 / test=131 / total=1,486 / unclassified=0。

当前允许保留的边界：

1. solver 建模对象、solver 内部结果、pre-solve 模型继续固定 `Flt64`。
2. framework shadow price 基础 API 继续固定 `Flt64`，通过 `reducedCost<V>` 与 adapter 隔离。
3. 旧 wrapper、typealias、legacy API 和兼容测试可继续使用 `Flt64`。
4. `ProductionAction.unitCost` 保留 `Flt64` 签名作为 solver 边界入口；泛型路径使用 `unitCostV<V>(time, fromDouble)` 或 `unitCostQuantity<V>`。
5. application branch-and-price 内部迭代状态仍以 `Flt64` 驱动，外部通过 snapshot 转为泛型 `Quantity<V>` 与无量纲 `V`。
6. task/bunch compilation 的时间变量、模型表达式、solution analyzer 和 pre-solver 内部值仍归为 solver/model 边界，下一轮继续压缩对外 DTO 泄漏。

## 3. 当前目标：G8 深层 DTO、日历/pre-solver 与兼容入口退场准备

G8 目标是在 G7 已压缩 application result 与基础时间 helper 后，尽量扩大单轮范围，集中处理剩余 `Domain DTO Pending`、`Time/Calendar Boundary`、pre-solver、solution analyzer、resource/produce/capacity 结果 DTO 与旧入口 deprecated 策略，减少后续迭代次数。

### 3.1 目标

1. 深挖 `TaskTime`、`Makespan`、switch time/cost、delay/advance、`WorkingCalendar`、time window 映射结果，将对外时间/成本值提升到 `Quantity<V>` 或专用泛型 DTO。
2. 审计并改造 task/bunch scheduling solution、solution analyzer、bunch compilation solution/result、pre-solver result，使对外 DTO 不直接暴露 solver 内部 `Flt64`。
3. 推进 resource/produce/capacity 的剩余结果 DTO 和边界 helper，覆盖 usage、slack、demand、reserve、capacity result、order result 等跨上下文对象。
4. 将旧裸值构造、旧 helper、`Flt64` wrapper/typealias 分层标记为 deprecated 候选，建立最终移除清单和迁移替代路径。
5. 继续深化 `flt64-scan-gate.ps1` 分类，将 `Domain DTO Pending`、`Time/Calendar Boundary`、`Application Algorithm Internal` 中可迁移项进一步压缩或拆分。
6. 同步扩展 demo4，覆盖日历/时间、solution analyzer/result、resource/produce/capacity DTO 与 deprecated 替代路径。

### 3.2 事项

1. **时间与日历边界**
   改造 `TaskTime`、`Makespan`、switch、delay/advance、`WorkingCalendar`、时间映射 helper，统一对外数量表达和单位策略。

2. **solution analyzer 与 task/bunch 结果 DTO**
   改造 task/bunch solution analyzer、task compilation result、bunch compilation result，拆分 solver-only 与领域可读结果。

3. **pre-solver 与 compilation 边界**
   审计 `SlotBasedCapacityPreSolver`、`TaskReverseBuilder`、`BunchCompilationContext`、`SlotBasedBunchCompilation*`，为 solver 结果增加泛型 adapter 层。

4. **resource/produce/capacity DTO 扩展**
   压缩 resource usage/slack、produce demand/reserve/consumption、capacity order/result 中的裸数值或 `Flt64` 对外泄漏。

5. **兼容入口退场准备**
   为旧裸值入口、旧 wrapper、旧 helper 建立 deprecated 策略，明确替代 API、迁移顺序和最终移除条件。

6. **测试与示例矩阵扩展**
   扩展 Quantity<FltX>、Flt64 legacy、adapter conversion、time/calendar DTO、solution analyzer/result、pre-solver result、demo4 compile 覆盖。

7. **门禁与文档**
   更新 scan gate 分类与 baseline，补充 G8 迁移说明，并保持 `daily.md` 只记录阶段目标和状态。

### 3.3 计划

1. 先从 scan gate 分类输出定位 `Domain DTO Pending`、`Time/Calendar Boundary`、`Application Result Boundary` 的最大来源文件。
2. 按依赖顺序推进：infrastructure 时间/日历 -> task compilation DTO -> bunch compilation/pre-solver DTO -> resource/produce/capacity result -> application/example。
3. 每完成一个 DTO 家族，同步补泛型主路径测试、旧路径兼容测试和 demo4 编译样例。
4. 对无法迁移的 `Flt64` 保留点，必须归入 solver/model boundary、shadow price boundary、application algorithm internal 或明确 deprecated compat。
5. 最后统一收紧 scan gate、更新迁移说明、运行完整 reactor 与 example 编译验证。

### 3.4 修改清单

预计涉及：

1. `gantt-scheduling-infrastructure`
   - `TimeWindow`
   - `WorkingCalendar`
   - time/calendar DTO helper

2. `gantt-scheduling-domain-task-compilation-context`
   - `TaskTime`
   - `Makespan`
   - switch / delay / advance limits
   - `SolutionAnalyzer`
   - task compilation solution/result DTO

3. `gantt-scheduling-domain-bunch-compilation-context`
   - `SlotBasedBunchCompilation*`
   - `TaskReverseBuilder`
   - `BunchCompilationContext`
   - `SlotBasedCapacityPreSolver`
   - bunch compilation solution/result DTO

4. `gantt-scheduling-domain-resource-context`
   - resource usage/slack/result DTO
   - resource capacity helper

5. `gantt-scheduling-domain-produce-context`
   - produce/consumption/demand/reserve result DTO
   - production quantity/cost helper

6. `gantt-scheduling-domain-capacity-context`
   - capacity order/result DTO
   - capacity column/result helper

7. `gantt-scheduling-application`
   - task/bunch application result adapter
   - branch-and-price result boundary

8. `ospf-kotlin-example`
   - `framework_demo/demo4`
   - 新旧路径 compile sample

9. 文档与门禁
   - `daily.md`
   - `flt64-scan-gate.ps1`
   - migration note

### 3.5 验收标准

1. G5-G7 已迁移字段、snapshot、summary、time quantity helper 的旧路径和新泛型路径均保持编译和测试通过。
2. task/bunch solution analyzer、solution/result DTO、pre-solver result、resource/produce/capacity result 的对外 API 不再新增 solver 建模类型泄漏。
3. `Domain DTO Pending`、`Time/Calendar Boundary`、`Application Result Boundary` 中至少完成一轮大范围压缩；无法迁移项有明确分类和后续处置。
4. 旧裸值入口、旧 wrapper、旧 helper 形成 deprecated 候选清单，并为每类给出替代泛型/物理量 API。
5. Flt64 legacy、FltX generic、Quantity<FltX> field construction、adapter conversion、time/calendar DTO、solution analyzer/result、pre-solver result、demo4 compile 均有测试或编译验证。
6. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
7. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过；若因基础设施失败，需记录具体错误和可复现命令。
8. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
9. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 通过。
10. `daily.md` 保持总目标、阶段完成概要、当前边界和下一轮计划，不累积执行细节。

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
| shadow price 基础 API 固定 `Flt64` | framework 层泛型化影响面大 | 本阶段继续使用 `reducedCost<V>` 隔离 |
| public API 泛型参数变更 | 旧源码调用可能不再按原名原参数数编译 | 提供旧构造、工厂或 wrapper，并逐步 deprecated |
| deprecated 范围过大 | 调用方可能短期无法完成迁移 | 分层标记，先文档化替代路径，再逐步提高级别 |
| 物理量化范围扩大 | `Quantity<V>` 会触发跨模块签名传播 | 按依赖顺序推进并保持每阶段测试可运行 |
| example 编译依赖外部仓库 | 基础设施失败可能掩盖 API 问题 | 记录具体错误，并保留 gantt-scheduling reactor 作为最低门槛 |

## 6. 当前基线与执行入口（2026-06-07）

### 当前状态一览

| 模块 | 当前状态 | 剩余重点 |
|------|----------|----------|
| infrastructure | `TimeWindow<V>` 与 `quantityOf` 已可返回 `Quantity<V>` | `WorkingCalendar` 与时间 DTO |
| task/cost | `CostItem.costQuantity`、`Cost.costSum` 为主字段 | 旧裸值入口 deprecated 策略 |
| capacity | `CapacityColumn.columnCost` 为主字段 | compilation/order result 继续审计 |
| resource | `ResourceCapacity` 核心数量字段为主字段 | usage/slack result DTO |
| produce | demand/reserves 和 task quantity map 有物理量主路径 | produce/consumption result DTO |
| bunch-compilation | `SlotBasedCapacityResult`、`SlotConstraints` 为物理量主路径，solution summary 已补充 | pre-solver 与 solution analyzer 边界 |
| task-compilation | 已有泛型链路，solution summary 已补充 | `TaskTime`、analyzer、limits 时间字段 |
| application | algorithm 签名泛型，iteration snapshot 已补充 | result adapter 与旧入口 deprecated |
| example/demo4 | 覆盖旧裸值、新 `Quantity` 字段、time quantity、application snapshot、summary 样例 | 下一轮扩展日历/pre-solver/DTO 样例 |

### 验证命令

```bash
mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test
mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile
pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1
git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example
```

### 当前 scan gate 基线

main=1,355 / test=131 / total=1,486 / unclassified=0
