# Gantt Scheduling 泛型化计划

日期：2026-06-05（最后更新：2026-06-07 G6 字段级物理量化收口）

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
4. 已完成 G6 第一轮字段级 `Quantity<V>` 主存储迁移，覆盖 task/cost、capacity column、resource capacity、material demand/reserves、production task quantity map、slot-based capacity result 和 slot constraints。
5. 已保留旧裸 `V` 构造、裸值属性、helper 和 `Flt64` typealias，兼容层继续用于迁移期。
6. 已补充 Flt64 legacy、FltX generic、Quantity<FltX> 字段构造、adapter conversion、demo4/example compile 的测试或编译验证。
7. 已新增 `MIGRATION_G6.md`，记录字段级 `Quantity<V>` 主路径、旧入口、单位默认策略和剩余边界。
8. 当前门禁基线：`flt64-scan-gate.ps1` 输出 main=1,351 / test=126 / total=1,477 / unclassified=0。新增行主要来自 G6 兼容 typealias 与注释。

当前允许保留的边界：

1. solver 建模对象、solver 内部结果、pre-solve 模型继续固定 `Flt64`。
2. framework shadow price 基础 API 继续固定 `Flt64`，通过 `reducedCost<V>` 与 adapter 隔离。
3. 旧 wrapper、typealias、legacy API 和兼容测试可继续使用 `Flt64`。
4. `ProductionAction.unitCost` 保留 `Flt64` 签名作为 solver 边界入口；泛型路径使用 `unitCostV<V>(time, fromDouble)` 或 `unitCostQuantity<V>`。
5. application `Iteration` 的目标值、上下界和 `Policy.reducedCost` 保持 `Flt64`，当前归为 branch-and-price 算法内部和 shadow price 边界。
6. task/bunch compilation 的时间变量、模型表达式、solution analyzer 和 pre-solver 内部值仍归为 solver/model 边界，下一轮继续压缩对外 DTO 泄漏。

## 3. 当前目标：G7 时间/DTO 泛型化、application 边界压缩与门禁深化

G7 目标是在 G6 字段级物理量主路径稳定后，继续拓宽到时间与结果 DTO、solution analyzer、application iteration/result，并收紧 scan gate 分类，使剩余 `Flt64` 明确集中在 solver 内核、shadow price 基础设施或算法内部无量纲值。

### 3.1 目标

1. 推进 `TaskTime`、`Makespan`、switch time/cost、delay/advance、summary DTO、solution analyzer 的时间与成本字段泛型化或物理量化。
2. 审计 task/bunch scheduling solution、bunch compilation solution/result，尽量将对外结果提升到 `V` 或 `Quantity<V>`。
3. 泛型化或隔离 application `Iteration` 的目标值、上下界、reduced cost、optimal rate 和结果回填；无法迁移的值必须明确归为 algorithm internal。
4. 将 `flt64-scan-gate.ps1` 从粗分类深化为 solver-only、DTO pending、field compat、application internal、shadow price boundary 等更细分类。
5. 继续同步 demo4，覆盖时间/DTO/application 结果的新入口和旧入口。
6. 保持旧 `Flt64` 调用源码兼容，并为计划移除的兼容入口建立 deprecated 路径。

### 3.2 事项

1. **时间与 task compilation DTO**
   审计并改造 `TaskTime`、`Makespan`、`Switch`、delay/advance、`SolutionAnalyzer`、task compilation solution/result 中可对外暴露的时间、成本、阈值字段。

2. **bunch compilation 与 pre-solver 边界**
   审计 `SlotBasedBunchCompilation*`、`TaskReverseBuilder`、`BunchCompilationContext`、`SlotBasedCapacityPreSolver`，将 solver-only 与领域结果 DTO 分离。

3. **application result 与 Iteration**
   审计 task/bunch branch-and-price `Iteration`、policy、lower/upper bound、objective/reduced cost 回填，补泛型 result adapter 或明确 algorithm internal。

4. **scan gate 分类深化**
   将 G6 后的 `Documented Pending=205` 和 `Algorithm Internal=341` 拆分为更可执行的细类，并保持 `unclassified=0`。

5. **测试矩阵扩展**
   扩展 Quantity<FltX> DTO、time quantity、application result construction、旧 Flt64 wrapper、demo4 compile 覆盖。

6. **文档与兼容策略**
   更新迁移说明，标记可 deprecated 的旧裸值入口，明确最终移除条件和剩余不可迁移边界。

### 3.3 计划

1. 先按 scan gate 文件清单定位 `TaskTime.kt`、`WorkingCalendar.kt`、application `Iteration.kt`、pre-solver 与 solution analyzer 的最大来源。
2. 优先处理对外 DTO 与 result，避免先触碰 solver 表达式内部实现。
3. 每完成一个 DTO/result 家族，同步补 `Quantity<FltX>` 和旧 `Flt64` 兼容测试。
4. 将仍固定 `Flt64` 的 application 值封装在 adapter/result 边界，避免继续向领域对象扩散。
5. 收紧 scan gate 分类并更新 baseline，最后跑完整 reactor、example 编译和空白检查。

### 3.4 修改清单

预计涉及：

1. `gantt-scheduling-domain-task-compilation-context`
   - `TaskTime`
   - `Makespan`
   - `Switch`
   - delay/advance limits
   - `SolutionAnalyzer`
   - task compilation solution/result DTO

2. `gantt-scheduling-domain-bunch-compilation-context`
   - `SlotBasedBunchCompilation*`
   - `TaskReverseBuilder`
   - `BunchCompilationContext`
   - pre-solver result adapter
   - bunch compilation solution/result DTO

3. `gantt-scheduling-application`
   - task/bunch `Iteration`
   - branch-and-price policy/result
   - objective/lower bound/upper bound/reduced cost adapter

4. `gantt-scheduling-infrastructure`
   - time DTO helper
   - `WorkingCalendar` 对外物理量边界

5. `ospf-kotlin-example`
   - `framework_demo/demo4`
   - 新旧路径 compile sample

6. 文档与门禁
   - `daily.md`
   - `flt64-scan-gate.ps1`
   - migration note

### 3.5 验收标准

1. G6 已迁移字段的旧裸值构造和新 `Quantity<V>` 主字段构造均保持编译和测试通过。
2. task/bunch solution、solution analyzer、summary DTO 和 application 对外 result 不再泄漏 solver 建模类型。
3. application `Iteration` 目标值/上下界完成泛型化，或被压缩为明确 algorithm internal 并有 adapter 返回泛型结果。
4. scan gate `unclassified=0`；`Documented Pending` 被拆分并明显下降，新增分类能解释所有保留 `Flt64`。
5. Flt64 legacy、FltX generic、Quantity<FltX> field construction、adapter conversion、application result construction、demo4 compile 均有测试或编译验证。
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
| 物理量化范围扩大 | `Quantity<V>` 会触发跨模块签名传播 | 按依赖顺序推进并保持每阶段测试可运行 |
| example 编译依赖外部仓库 | 基础设施失败可能掩盖 API 问题 | 记录具体错误，并保留 gantt-scheduling reactor 作为最低门槛 |

## 6. 当前基线与执行入口（2026-06-07）

### 当前状态一览

| 模块 | 当前状态 | 剩余重点 |
|------|----------|----------|
| task/cost | `CostItem.costQuantity`、`Cost.costSum` 为主字段 | 旧裸值入口 deprecated 策略 |
| capacity | `CapacityColumn.columnCost` 为主字段 | compilation/order result 继续审计 |
| resource | `ResourceCapacity` 核心数量字段为主字段 | usage/slack result DTO |
| produce | demand/reserves 和 task quantity map 有物理量主路径 | produce/consumption result DTO |
| bunch-compilation | `SlotBasedCapacityResult`、`SlotConstraints` 为物理量主路径 | pre-solver 与 solution analyzer 边界 |
| task-compilation | 已有泛型链路 | `TaskTime`、summary DTO、limits 时间字段 |
| application | algorithm 签名泛型 | `Iteration` 与 result 回填仍为核心边界 |
| example/demo4 | 覆盖旧裸值和新 `Quantity` 字段构造 | 下一轮扩展时间/DTO/application 样例 |

### 验证命令

```bash
mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test
mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile
pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1
git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example
```

### 当前 scan gate 基线

main=1,351 / test=126 / total=1,477 / unclassified=0
