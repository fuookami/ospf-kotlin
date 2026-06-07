# Gantt Scheduling 泛型化计划

日期：2026-06-07（最后更新：G8 深层时间/DTO/pre-solver 边界收口）

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
9. 已新增 `MIGRATION_G5.md`、`MIGRATION_G6.md`、`MIGRATION_G8.md`，记录迁移路径、旧入口、单位默认策略和剩余边界。
10. 当前门禁基线：`flt64-scan-gate.ps1` 输出 main=1,400 / test=191 / total=1,591 / unclassified=0。

当前允许保留的边界：

1. solver 建模对象、solver 内部结果、pre-solve 模型继续固定 `Flt64`。
2. framework shadow price 基础 API 继续固定 `Flt64`，通过 adapter 与泛型 helper 隔离。
3. 旧 wrapper、typealias、legacy API 和兼容测试可继续使用 `Flt64`。
4. `ProductionAction.unitCost` 保留 `Flt64` 签名作为 solver 边界入口；泛型路径使用 `unitCostV<V>(time, fromDouble)` 或 `unitCostQuantity<V>`。
5. application branch-and-price 内部迭代状态仍以 `Flt64` 驱动，外部通过 snapshot 转为泛型 `Quantity<V>` 与无量纲 `V`。
6. task/bunch compilation、solution analyzer、pre-solver 的入模和求解参数仍归为 solver/model boundary；对外读取优先走泛型 summary、snapshot、quantity helper 或 adapter result。

## 3. 当前目标：G9 service/application API 去 solver 泄漏与兼容层退场

G9 目标是在 G8 已提供更完整泛型读取出口后，集中处理 service/application 层仍直接要求 `Flt64` model、symbol、solution list 的调用面，并启动 deprecated 退场标记，减少总目标收尾轮次。

### 3.1 目标

1. 将 task/bunch solution analyzer、application result adapter、pre-solver facade 的 solver-only API 与领域可读 API 分层，减少上层调用方直接接触 `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`List<Flt64>`。
2. 扩展 switch cost、limit cost、order/capacity result、resource slack、produce slack 等剩余领域结果的 `Quantity<V>` 出口。
3. 为旧裸值入口、旧 helper、`Flt64` wrapper/typealias 建立可执行 deprecated 清单，明确替代 API、标记批次和最终删除条件。
4. 继续压缩或拆分 `Domain DTO Pending`、`Time/Calendar Boundary`、`Application Algorithm Internal` 分类，确保新增 `Flt64` 都有明确边界解释。
5. 扩展 demo4 与测试矩阵，覆盖 service/application facade、deprecated 替代路径和跨上下文 adapter result。

### 3.2 事项

1. **service facade 分层**
   为 solution analyzer、pre-solver、branch-and-price result 增加领域可读 facade，保留 solver-only 内部入口。

2. **剩余 DTO 物理量出口**
   补齐 switch cost、limit penalty、capacity order/result、resource slack、produce slack 的泛型 quantity helper。

3. **兼容入口 deprecated**
   分批标记旧裸值构造、旧 helper 和 wrapper/typealias，配套 `ReplaceWith` 或迁移说明。

4. **application/example 同步**
   demo4 覆盖新 facade、剩余 DTO helper 和 deprecated 替代路径，example reactor 必须可编译。

5. **门禁与文档**
   更新 scan gate 分类和 baseline，保持 `daily.md` 只记录阶段状态与下一轮计划。

### 3.3 计划

1. 先从 application/service API 反向审计所有 `AbstractLinearMetaModel<Flt64>`、`MetaModel<Flt64>`、`List<Flt64>` 暴露点，拆分 solver-only 与领域 facade。
2. 再按 DTO 家族补 quantity helper：task limits -> capacity order/result -> resource/produce slack -> bunch/application result。
3. 每完成一个 facade 或 DTO 家族，同步补 FltX 主路径测试、Flt64 legacy 测试和 demo4 编译样例。
4. deprecated 先标记低风险 helper 和 typealias，再处理构造器/companion 旧入口，避免一次性破坏源码兼容。
5. 最后统一跑完整 gantt reactor、example reactor、scan gate 和 diff check。

### 3.4 修改清单

预计涉及：

1. `gantt-scheduling-domain-task-compilation-context`
   - solution analyzer facade
   - switch/limit quantity helper
   - deprecated 候选入口

2. `gantt-scheduling-domain-bunch-compilation-context`
   - pre-solver facade
   - bunch analyzer/result adapter
   - slot/capacity DTO 剩余 helper

3. `gantt-scheduling-domain-capacity-scheduling-context`
   - capacity order/result quantity helper
   - capacity compilation facade

4. `gantt-scheduling-domain-resource-context`
   - resource slack/result quantity helper
   -旧裸值入口 deprecated 候选

5. `gantt-scheduling-domain-produce-context`
   - produce/consumption slack/result quantity helper
   -旧裸值入口 deprecated 候选

6. `gantt-scheduling-application`
   - task/bunch application result facade
   - branch-and-price result boundary

7. `ospf-kotlin-example`
   - `framework_demo/demo4`
   - 新 facade、旧入口替代路径 compile sample

8. 文档与门禁
   - `daily.md`
   - `flt64-scan-gate.ps1`
   - migration note

### 3.5 验收标准

1. G5-G8 已迁移字段、snapshot、summary、time/calendar quantity helper、pre-solver adapter、resource/produce solved quantity 的旧路径和新泛型路径均保持编译和测试通过。
2. service/application 新增领域可读 facade 不直接要求调用方传入 solver 建模对象；solver-only 入口有明确分类。
3. 剩余 DTO helper 覆盖 switch cost、limit penalty、capacity order/result、resource slack、produce slack 的主要读取路径。
4. 旧裸值入口、旧 wrapper、旧 helper 完成第一批 deprecated 标记和替代 API 文档。
5. `Domain DTO Pending`、`Time/Calendar Boundary`、`Application Algorithm Internal` 至少完成一轮进一步拆分或压缩，且 `unclassified=0`。
6. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
7. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
8. `pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1` 通过。
9. `git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example` 通过。

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

## 6. 当前基线与执行入口（2026-06-07）

### 当前状态一览

| 模块 | 当前状态 | 剩余重点 |
|------|----------|----------|
| infrastructure | `TimeWindow<V>`、`WorkingCalendar` duration helper 已可返回 `Quantity<V>` | 时间 DTO deprecated 策略 |
| task/cost | `CostItem.costQuantity`、`Cost.costSum` 为主字段 | 旧裸值入口 deprecated 策略 |
| capacity | `CapacityColumn.columnCost`、duration helper、capacity solution duration helper 已覆盖 | order/result facade 与剩余成本 helper |
| resource | `ResourceCapacity` 主字段和 usage solved quantity 已覆盖 | slack/result facade 与 deprecated 策略 |
| produce | demand/reserves、task quantity map、produce/consumption solved quantity 已覆盖 | slack/result facade 与 deprecated 策略 |
| bunch-compilation | `SlotBasedCapacityResult`、`SlotConstraints`、pre-solver generic adapter 已覆盖 | analyzer facade 与 solver-only API 分层 |
| task-compilation | solution summary、`TaskTime`、`Makespan`、`Switch` quantity helper 已覆盖 | analyzer facade、limit/switch cost helper |
| application | algorithm 签名泛型，iteration snapshot 已补充 | service/result facade 与旧入口 deprecated |
| example/demo4 | 覆盖旧裸值、新 `Quantity` 字段、time/calendar、snapshot、summary、solved quantity、switch quantity 样例 | 下一轮扩展 facade 与 deprecated 替代路径 |

### 验证命令

```bash
mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test
mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile
pwsh.exe -NoLogo -NoProfile -File ospf-kotlin-framework-gantt-scheduling/flt64-scan-gate.ps1
git diff --check -- ospf-kotlin-framework-gantt-scheduling ospf-kotlin-example
```

### 当前 scan gate 基线

main=1,400 / test=191 / total=1,591 / unclassified=0
