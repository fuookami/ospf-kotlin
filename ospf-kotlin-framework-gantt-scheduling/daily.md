# Gantt Scheduling 泛型化计划

日期：2026-06-05（最后更新：2026-06-05 会话 3）

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

1. G0 基线、扫描门禁和允许保留边界初版已建立。
2. G1 基础领域类型已完成泛型化，并保留 `Flt64` 兼容路径。
3. G2 `TimeWindow<V>` 已完成泛型化，旧 `Flt64` 时间窗口入口保留。
4. G2.5 `ProductivityCalendar` 已补齐 `Quantity<V>` 产量路径与单位一致性校验。
5. 3.1 solver adapter 与 model boundary 已收敛到 `SchedulingSolverValueAdapter<V>`。
6. 3.2 Task/Bunch compilation 主链路已完成 `V : RealNumber<V>` 传播。
7. 3.3 Capacity/Resource/Produce 上下文已完成领域泛型化，solver 入模前统一转 `Flt64`。
8. 3.4 Application 层 branch-and-price 主入口已支持领域泛型 V，solver model 继续固定 `Flt64`。
9. 3.5 ShadowPriceMap 已确认以 `reducedCost<V>` 作为 Flt64 到 V 的隔离边界，framework shadow price 基础 API 暂不泛型化。
10. 3.6 测试与扫描收敛已完成，`Flt64` 扫描为 1,317 点 / 105 文件，新增 `GenericFltXPathTest` 覆盖 `Cost<FltX>` 与 `TimeWindow<FltX>`。

当前允许保留的边界：

1. solver 建模对象和 solver 内部结果继续固定 `Flt64`。
2. framework shadow price 基础 API 继续固定 `Flt64`。
3. 旧 wrapper、typealias、legacy API 和兼容测试可继续使用 `Flt64`。
4. `ProductionAction`、`Label.kt`、slot-based bunch 专用路径、`TaskReverseBuilder` 当前归类为待收敛 legacy 边界。

## 3. 下一轮目标：G4 Legacy 边界收敛、广域 FltX 覆盖与门禁工程化

下一轮目标是在不改动 solver 数值内核的前提下，一次性拓展收敛范围，尽量减少后续迭代：

1. 收敛或显式 wrapper 化剩余 legacy `Flt64` 入口，优先处理 slot-based bunch、bunch generation、task reverse 与 `ProductionAction`。
2. 将 `Flt64` 允许清单从文档说明升级为可复现扫描门禁，区分 solver boundary、compat wrapper、legacy API、test 和待清理项。
3. 扩大非默认 V 覆盖到 capacity、resource、produce、bunch compilation 与 application API 构造层。
4. 继续保证 demo4 和旧 `Flt64` 应用路径可编译。
5. 对无法在本轮低风险泛型化的入口，补齐命名、注释和文档，明确其 legacy 语义和迁移路径。

### 3.1 事项

1. **slot-based bunch legacy 收敛**
   处理 `SlotBasedBunchCompilationContext`、`SlotBasedBunchCompilation`、`SlotBasedBunchAggregation` 及其 capacity pre-solver 相关类型，判断并实施泛型化或 `Flt64` wrapper 分离。

2. **bunch generation 与 task reverse 收敛**
   处理 `Label.kt`、`TaskReverseBuilder` 中固定 `AbstractTaskBunch<..., Flt64>` 的公开入口，优先改为泛型 V；若影响范围过大，则保留 `Flt64` wrapper 并新增泛型入口。

3. **ProductionAction 兼容边界收窄**
   评估 `unitCapacity(TimeWindow<Flt64>)`、`unitCost`、`upperBound` 等 API 是否能引入泛型补充入口；旧方法保留为 wrapper，并继续通过 `asFlt64TimeWindow()` 隔离。

4. **非默认 V 测试扩展**
   在 capacity、resource、produce、bunch compilation、application API 构造层补充 `FltX` 或 `Quantity<FltX>` 测试，覆盖更多领域对象和边界转换。

5. **扫描门禁工程化**
   新增脚本或报告文件，自动扫描 gantt-scheduling 的 `Flt64` 使用点，并按允许类别输出统计与未归类项。

6. **example/demo4 同步验证**
   每次 public API 变化后检查 demo4；旧路径继续固定到 `Flt64` wrapper，并通过 example reactor 编译。

### 3.2 计划

1. 先新增 `Flt64` 扫描脚本与允许清单，锁定当前基线，避免改造过程中新增未归类使用点。
2. 处理 slot-based bunch 路径，将能泛型化的类型改为 `<V>`，暂不能泛型化的类型改名或文档化为 `Flt64` legacy wrapper。
3. 处理 `Label.kt` 和 `TaskReverseBuilder`，优先新增泛型入口，再让旧入口委托到泛型实现或明确保留为 wrapper。
4. 处理 `ProductionAction`，将 `TimeWindow<Flt64>` 依赖限制在兼容层，新增泛型辅助或 adapter。
5. 分模块补充 `FltX` / `Quantity<FltX>` 测试，先测领域构造和边界转换，不引入真实求解器。
6. 跑 gantt-scheduling reactor 测试、example reactor 编译和扫描门禁，最后更新 `daily.md` 的完成状态。

### 3.3 修改清单

预计涉及：

1. `gantt-scheduling-domain-bunch-compilation-context`
   - `SlotBasedBunchCompilationContext`
   - `SlotBasedBunchCompilation`
   - `SlotBasedBunchAggregation`
   - `SlotBasedCapacityResult`
   - slot constraints / capacity pre-solver 相关类型

2. `gantt-scheduling-domain-bunch-generation-context`
   - `Label.kt`
   - bunch generation service 中固定 `Flt64` 的入口和类型约束

3. `gantt-scheduling-domain-task-context`
   - `TaskReverseBuilder`
   - `TaskBunch` legacy typealias / wrapper
   - shadow price reduced-cost 泛型边界测试

4. `gantt-scheduling-domain-capacity-scheduling-context`
   - `ProductionAction`
   - capacity column / action / pre-solve 兼容入口

5. `gantt-scheduling-domain-resource-context`
   - resource quantity 构造与 slack/limit 边界测试

6. `gantt-scheduling-domain-produce-context`
   - `ProductionTask`
   - `MaterialDemand<V>`
   - `MaterialReserves<V>`
   - produce/consumption quantity limit 测试

7. `gantt-scheduling-application`
   - branch-and-price policy 构造兼容测试
   - legacy `Flt64` wrapper 编译验证

8. 文档与门禁
   - `daily.md`
   - 新增或更新 `Flt64` 扫描脚本 / 报告
   - 必要的 README 或 migration note

### 3.4 验收标准

1. `Flt64` 扫描脚本可复现当前使用点统计，并能输出所有未归类项。
2. 扫描结果中不再存在未说明的 `Flt64` 领域 API 泄漏；保留项必须归类为 solver boundary、compat wrapper、legacy API、algorithm internal 或 test。
3. slot-based bunch、bunch generation、task reverse、ProductionAction 的 `Flt64` 固定入口完成泛型化或明确 wrapper 化。
4. capacity、resource、produce、bunch compilation、application 构造层至少各有一个非默认 V 或 `Quantity<FltX>` 测试。
5. `GenericFltXPathTest` 继续通过，且新增测试不依赖真实 solver。
6. `mvn -B -ntp -f ospf-kotlin-framework-gantt-scheduling/pom.xml test` 通过。
7. `mvn -B -ntp -pl ospf-kotlin-example -am -DskipTests compile` 通过。
8. `git diff --check -- ospf-kotlin-framework-gantt-scheduling` 通过。
9. demo4 若受 API 影响，必须同步更新；若未受影响，需在 `daily.md` 记录原因。
10. 对仍无法迁移的 legacy API，必须记录原因、影响面和后续迁移条件。

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
| slot-based API 旧路径复杂 | 可能与 capacity pre-solver 强绑定 | 优先 wrapper 化，避免破坏旧路径 |
| 非默认 V 测试成本扩大 | 全求解测试成本高且不稳定 | 先覆盖领域构造和边界转换 |
| demo4 受 API 调整影响 | 示例编译可能破坏 | 每次 public API 变化后跑 example reactor |
