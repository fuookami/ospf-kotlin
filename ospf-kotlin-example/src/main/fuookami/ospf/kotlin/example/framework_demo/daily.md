# Kotlin framework demo 改进计划

## 目标与边界

本计划用于继续推进
`E:/workspace/ospf-kotlin/ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo`
与 Rust 版 `E:/workspace/ospf-rust/ospf-rust-example/src/framework` 的双向对齐。

用户已确认：

1. Rust `airworthiness/capacity_limit.rs` 是冗余约束，Rust 删除。
2. 其余 Rust 中合理拆分出的约束或服务，Kotlin 也应同步拆分。
3. Kotlin 当前 `demo4/domain/bunch_generation` 未完整实现，需要参考
   `E:/workspace/fsra-proof/fsra-domain-bunch-generation-context/src/main/com/wintelia/fuookami/fsra/domain`
   中的完整算法补齐。

本计划优先保持 Kotlin 现有工程风格，不为了对齐 Rust 而破坏 Kotlin 的包结构和命名习惯。

## 执行优先级

| 优先级 | 工作项 | 目的 |
|---|---|---|
| P0 | 补齐 Demo4 `bunch_generation` 完整算法 | 当前 Kotlin 自身也缺完整列生成子问题实现 |
| P0 | 与 Rust 删除冗余 capacity 约束保持一致 | 避免 Kotlin 后续同步错误约束 |
| P1 | 对 Rust 合理新增的 limit 做 Kotlin 同步拆分 | 保持两端业务能力一致 |
| P1 | 梳理 Demo4 `bunch_compilation` 与 `bunch_generation` 边界 | 避免 master 与 pricing 逻辑混杂 |
| P2 | 文档、示例、测试补齐 | 固化迁移语义和回归边界 |

## P0-1 补齐 Demo4 bunch_generation

### 当前 Kotlin 状态

当前已有：

1. `demo4/domain/bunch_generation/Aggregation.kt`
2. `demo4/domain/bunch_generation/model/Graph.kt`
3. `demo4/domain/bunch_generation/service/BunchGenerator.kt`
4. `demo4/domain/bunch_generation/service/FeasibilityJudger.kt`
5. `demo4/domain/bunch_generation/service/GraphGenerator.kt`
6. `demo4/domain/bunch_generation/service/InitialBunchGenerator.kt`

这些文件相比 FSRA 参考实现更简化，缺少：

1. `BunchGenerationContext`
2. `FlightTaskReverse`
3. `Operator`
4. `FlightTaskFeasibilityJudger`
5. `RouteGraphGenerator`
6. `FlightTaskBunchGenerator`
7. `InitialFlightTaskBunchGenerator`
8. `AggregationInitializer`
9. 完整 Label Setting pricing
10. shadow price / reduced cost / recovery / locked task 处理

### 参考来源

FSRA 完整参考模块：

1. `bunch_generation_context/Aggregation.kt`
2. `bunch_generation_context/BunchGenerationContext.kt`
3. `bunch_generation_context/model/Graph.kt`
4. `bunch_generation_context/model/FlightTaskReverse.kt`
5. `bunch_generation_context/service/Operator.kt`
6. `bunch_generation_context/service/FlightTaskFeasibilityJudger.kt`
7. `bunch_generation_context/service/RouteGraphGenerator.kt`
8. `bunch_generation_context/service/FlightTaskBunchGenerator.kt`
9. `bunch_generation_context/service/InitialFlightTaskBunchGenerator.kt`
10. `bunch_generation_context/service/AggregationInitializer.kt`

### 目标文件结构

在 Kotlin demo4 中补齐或重命名为：

1. `demo4/domain/bunch_generation/Aggregation.kt`
2. `demo4/domain/bunch_generation/BunchGenerationContext.kt`
3. `demo4/domain/bunch_generation/model/Graph.kt`
4. `demo4/domain/bunch_generation/model/FlightTaskReverse.kt`
5. `demo4/domain/bunch_generation/service/Operator.kt`
6. `demo4/domain/bunch_generation/service/FlightTaskFeasibilityJudger.kt`
7. `demo4/domain/bunch_generation/service/RouteGraphGenerator.kt`
8. `demo4/domain/bunch_generation/service/FlightTaskBunchGenerator.kt`
9. `demo4/domain/bunch_generation/service/InitialFlightTaskBunchGenerator.kt`
10. `demo4/domain/bunch_generation/service/AggregationInitializer.kt`

现有简化文件处理：

1. `BunchGenerator.kt`：
   - 如果只是简化版 pricing，迁移后改名或替换为 `FlightTaskBunchGenerator.kt`。
   - 若已有调用方依赖，可短期保留 facade，并委托到新类。
2. `FeasibilityJudger.kt`：
   - 如果只是接口，保留为 operator 的一部分。
   - 如果含具体逻辑，合并进 `FlightTaskFeasibilityJudger.kt`。
3. `GraphGenerator.kt`：
   - 改为 `RouteGraphGenerator.kt` 或保留兼容 facade。
4. `InitialBunchGenerator.kt`：
   - 改为 `InitialFlightTaskBunchGenerator.kt` 或保留兼容 facade。

建议最终文件名与 FSRA 参考保持一致，降低后续回溯成本。

### 模型层迁移

#### Graph.kt

对齐 FSRA `Graph.kt`：

1. `Node` sealed class：
   - `RootNode`
   - `TaskNode`
   - `EndNode`
2. `Edge`：
   - 起点。
   - 终点。
   - 连接信息。
   - 是否来自 order change / reverse。
3. `Graph`：
   - 节点集合。
   - 边集合。
   - 出边索引。
   - BFS / 邻接查询。

注意：

1. 不重复定义 `FlightTask`，直接引用 demo4 task model。
2. 任务节点身份要稳定，避免仅用集合顺序表达。
3. 如果现有 `Graph.kt` 已有部分实现，优先渐进扩展，不一次性替换所有 public API。

#### FlightTaskReverse.kt

新增并迁移 FSRA `FlightTaskReverse.kt`：

1. `ReversiblePair`
2. `FlightTaskReverse`
3. `reverseEnabled(...)`
4. `symmetrical(...)`
5. 按任务查找可对换任务。
6. 双向 pair 查询。

注意：

1. 对换规则不要散落在 graph generator。
2. 保留对机型、航线、机场、时间窗、维护规则的扩展入口。

### 服务层迁移

#### Operator.kt

新增或补齐函数类型：

1. `RuleChecker`
2. `ConnectionTimeCalculator`
3. `MinimumDepartureTimeCalculator`
4. `CostCalculator`
5. `TotalCostCalculator`
6. `FeasibilityJudger`

这些 operator 是连接 task / rule / aircraft / cost 领域的扩展点，application 层只注入策略，不直接写业务判断。

#### FlightTaskFeasibilityJudger.kt

迁移 FSRA 具体可行性判断：

1. 机型匹配。
2. 子机型或机型族匹配。
3. 容量匹配。
4. 飞机可用性。
5. 前后任务机场衔接。
6. 最小连接时间。
7. 飞行时间与任务时间窗。
8. 维护 / AOG / recovery 限制。
9. rule checker 额外规则。
10. 对换任务额外规则。

验收标准：

1. `RouteGraphGenerator` 只调用 judger，不硬编码完整判断。
2. 不可行原因至少能通过日志、诊断对象或测试断言暴露。
3. 与 Rust 侧 `flight_task_feasibility_judger.rs` 的语义逐项可对照。

#### RouteGraphGenerator.kt

迁移 FSRA `RouteGraphGenerator.kt`：

1. 按 aircraft 构造 root -> task -> end 的有向图。
2. 对候选任务连接调用 `FlightTaskFeasibilityJudger`。
3. 通过 connection time / minimum departure time calculator 计算可连接时间。
4. 支持 `withOrderChange`：
   - 根据 `FlightTaskReverse` 插入对换边。
   - 标注边来源。
5. 输出可供 initial bunch 和 pricing 复用的 graph。

验收标准：

1. 图生成不注册 master 模型变量。
2. 图生成不计算 reduced cost。
3. graph 可被 `InitialFlightTaskBunchGenerator` 和 `FlightTaskBunchGenerator` 复用。

#### FlightTaskBunchGenerator.kt

迁移 FSRA `FlightTaskBunchGenerator.kt` 的 Label Setting 算法：

1. 输入：
   - aircraft。
   - route graph。
   - shadow price map。
   - cost calculator。
   - total cost calculator。
   - recovery / delay 参数。
   - reduced cost 阈值。
2. Label 状态：
   - 当前节点。
   - 已覆盖 task 序列。
   - 当前时刻。
   - 原始成本。
   - shadow price 扣减。
   - reduced cost。
   - recovery / delay 信息。
3. 扩展：
   - 沿 graph 出边扩展。
   - 更新任务时间和连接成本。
   - 累加任务覆盖 shadow price。
   - 对不可行扩展记录诊断。
4. 支配：
   - 同末端节点、同可比较任务集合下，时间和成本均不劣者保留。
   - 将支配判断拆成独立方法，便于测试。
5. 输出：
   - negative reduced cost bunch。
   - top N 或阈值筛选。
   - pricing 诊断。

验收标准：

1. 可由 branch-and-price 主循环重复调用。
2. shadow price 刷新后不需要重建静态 graph。
3. 对无可行列、存在负 reduced cost、支配剪枝都有测试。

#### InitialFlightTaskBunchGenerator.kt

迁移 FSRA 初始列生成：

1. 生成每架 aircraft 的初始 bunch。
2. 支持 locked task。
3. 支持 soft recovery。
4. 支持 empty bunch。
5. 输出初始列并写入 aggregation。

验收标准：

1. master 初始模型有可用列。
2. locked task 不丢失。
3. empty bunch 的成本和覆盖语义明确。

#### AggregationInitializer.kt

迁移 FSRA 初始化器：

1. 构建 `FlightTaskReverse`。
2. 为 aircraft 生成 route graph。
3. 生成 initial bunch。
4. 将 reverse、graphs、initial bunches 写入 `Aggregation`。
5. 如果沿用 FSRA 并行实现，需要确认 Kotlin demo 当前依赖与线程模型；不稳定时先单线程实现。

验收标准：

1. `BunchGenerationContext` 只需调用 initializer。
2. application 层不直接拼接 graph / initial bunch / pricing 逻辑。
3. 初始化错误有明确返回或异常边界。

### BunchGenerationContext.kt

新增 context：

1. 接收 aircraft、task、rule、parameter、cost strategy。
2. 创建并持有 `Aggregation`。
3. 暴露：
   - `register` 或初始化入口。
   - 初始 bunch 查询。
   - pricing 新列生成。
   - shadow price 刷新。
4. 与 `bunch_selection` 和 `bunch_compilation` 协作，不直接承担 master 约束注册。

## P0-2 与 Rust 删除 capacity_limit 保持一致

Kotlin 当前 `airworthiness_security` 没有独立 `CapacityLimit.kt`，容量/位置最大载重约束主要在 stowage 的 load weight 类约束中表达。

计划：

1. 不新增 `airworthiness_security/service/limits/CapacityLimit.kt`。
2. 若后续从 Rust 反向同步 limit 文件，明确跳过 `capacity_limit.rs`。
3. 在迁移对照表中记录：
   - Rust 删除 `airworthiness/capacity_limit.rs`。
   - Kotlin 保持无该文件。
   - 唯一位置载重约束归属 stowage。

验收标准：

1. Kotlin 不出现 airworthiness security 下的 capacity limit。
2. stowage 侧 load weight 约束仍存在且被 pipeline 注册。

## P1-1 同步 Rust 合理新增或拆分的 limit

### AdjacentGapLimit

Rust 当前有：

1. `demo2/domain/airworthiness/service/limits/adjacent_gap_limit.rs`

Kotlin 当前无对应文件。

计划：

1. 先确认该约束业务含义：
   - 是否表达相邻舱位/相邻 zone 的间隔、载重差或安全间隔。
   - 是否与现有 `LinearDensityLimit`、`UnsymmetricalLinearDensityLimit` 或 stowage 邻接类约束重复。
2. 若合理：
   - 新增 `demo2/domain/airworthiness_security/service/limits/AdjacentGapLimit.kt`。
   - 在 `PipelineListGenerator.kt` 注册。
   - 添加最小测试或样例数据。
3. 若重复：
   - 记录为 Rust 待删除项，而不是 Kotlin 新增项。

### MustShipLimit

Rust 当前有：

1. `demo2/domain/express_effectiveness/service/limits/must_ship_limit.rs`

计划：

1. 检查 Kotlin express effectiveness 是否已经通过 item priority 或 cargo required 字段表达必须装载。
2. 若没有等价逻辑：
   - 新增 `MustShipLimit.kt`。
   - 在 express effectiveness `PipelineListGenerator.kt` 注册。
   - 明确硬约束或软惩罚模式。
3. 与 Rust 对齐约束命名和激活开关。

### PriorityOrderLimit

Rust 当前有：

1. `demo2/domain/loading_effectiveness/service/limits/priority_order_limit.rs`

计划：

1. 检查 Kotlin 是否只有 `ItemOrderLimit` 或 priority cost，没有 priority order 硬/软约束。
2. 若合理：
   - 新增 `PriorityOrderLimit.kt`。
   - 在 loading effectiveness pipeline 中注册。
   - 支持按 policy 控制硬约束或软惩罚。

### SourceEarlyLimit

Rust 当前有：

1. `demo2/domain/loading_effectiveness/service/limits/source_early_limit.rs`

计划：

1. 检查 Kotlin 是否已有 source 维度的提前装载规则。
2. 若没有：
   - 新增 `SourceEarlyLimit.kt`。
   - 在 pipeline 中注册。
   - 与 existing source / destination / same-source 规则明确边界。

## P1-2 与 Rust 文件结构互相对照

### 已合理对齐的 Kotlin 文件

Kotlin 侧很多目录已是一类一文件，保持即可：

1. `demo2/domain/aircraft/model/*`
2. `demo2/domain/airworthiness_security/model/*`
3. `demo2/domain/airworthiness_security/service/limits/*`
4. `demo4/domain/bunch_compilation/model/*`
5. `demo4/domain/bunch_compilation/service/limits/*`

### Kotlin 需要新增的拆分点

如果 Rust 保留以下拆分，Kotlin 同步新增对应文件：

1. `AdjacentGapLimit.kt`
2. `MustShipLimit.kt`
3. `PriorityOrderLimit.kt`
4. `SourceEarlyLimit.kt`
5. Demo4 `bunch_generation` 的 FSRA 完整文件组。

### Kotlin 不需要跟随 Rust 的差异

1. Rust 的 `mod.rs` 不对应 Kotlin 文件。
2. Rust 的 snake_case 文件名不影响 Kotlin PascalCase 命名。
3. Rust 为所有权或 trait object 拆出的 adapter，如果 Kotlin 没有对应复杂性，不强制新增。

## P1-3 Demo4 bunch_compilation 与 bunch_generation 边界

### bunch_generation

职责：

1. 生成 route graph。
2. 生成 initial bunch。
3. 根据 shadow price pricing 新 bunch。
4. 输出 bunch 与诊断。

不得承担：

1. master model 中的覆盖约束。
2. fleet balance / flight link master 约束。
3. 编译后的最终 solution 解析。

### bunch_compilation

职责：

1. `Compilation`
2. `FleetBalance`
3. `FlightCapacity`
4. `FlightLink`
5. `FreeAircraftSelector`
6. `FleetBalanceLimit`
7. `FlightLinkLimit`
8. master model 相关 pipeline 注册。

不得承担：

1. Label Setting pricing。
2. route graph generation。
3. shadow price 到 reduced cost 的子问题计算。

### bunch_selection

职责：

1. branch-and-price 流程。
2. 从 master 提取 shadow price。
3. 调用 `BunchGenerationContext` pricing。
4. add columns。
5. final MILP / solution 组装。

## P2 文档与测试

### 文档

建议更新或新增：

1. `framework_demo/README_ch.md`
2. `framework_demo/README.md`
3. Demo4 局部说明。

内容至少包括：

1. `bunch_generation` 是 pricing 子问题。
2. `bunch_compilation` 是 master/编译约束。
3. `bunch_selection` 是 branch-and-price 编排。
4. shadow price 如何进入 reduced cost。
5. initial bunch 与 generated bunch 的区别。
6. 与 Rust 版的结构对应关系。

### 测试建议

新增最小可控测试：

1. `RouteGraphGeneratorTest`
   - 两个可连接任务生成边。
   - 不可连接任务不生成边。
   - `withOrderChange` 生成对换边。
2. `FlightTaskReverseTest`
   - 可对换任务双向查询。
   - 不可对换任务被拒绝。
3. `InitialFlightTaskBunchGeneratorTest`
   - locked task 必须覆盖。
   - empty bunch 可生成。
4. `FlightTaskBunchGeneratorTest`
   - 负 reduced cost 输出列。
   - 无负 reduced cost 不输出列。
   - dominated label 被剪枝。
5. `BunchGenerationContextTest`
   - initializer 写入 graph 和 initial bunch。
   - shadow price 更新后 pricing 使用新值。

如果当前 example 模块没有测试框架，先添加最小 main-level smoke check 或使用已有 Kotlin 项目测试约定。

## 回归命令

在 Kotlin 仓库根目录执行：

    ./gradlew :ospf-kotlin-example:compileKotlin

如果项目使用 Windows PowerShell 且没有 shell wrapper：

    .\gradlew.bat :ospf-kotlin-example:compileKotlin

如测试任务可用：

    .\gradlew.bat :ospf-kotlin-example:test

快速检索：

    rg "CapacityLimit" ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo
    rg "BunchGenerationContext|FlightTaskBunchGenerator|RouteGraphGenerator|FlightTaskReverse" ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/bunch_generation
    rg "MustShipLimit|PriorityOrderLimit|SourceEarlyLimit|AdjacentGapLimit" ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo

## 建议执行顺序

1. 从 FSRA 复制语义，先补 `FlightTaskReverse.kt` 和增强 `Graph.kt`。
2. 新增 `Operator.kt` 和 `FlightTaskFeasibilityJudger.kt`。
3. 将现有 `GraphGenerator.kt` 升级或替换为 `RouteGraphGenerator.kt`。
4. 将现有 `InitialBunchGenerator.kt` 升级或替换为 `InitialFlightTaskBunchGenerator.kt`。
5. 将现有 `BunchGenerator.kt` 升级或替换为 `FlightTaskBunchGenerator.kt`，补 Label Setting pricing。
6. 新增 `AggregationInitializer.kt` 和 `BunchGenerationContext.kt`。
7. 接入 `bunch_selection` 的 branch-and-price 主流程。
8. 检查并跳过 `CapacityLimit` 同步，保持 stowage 作为唯一位置载重约束来源。
9. 按业务确认同步新增 `AdjacentGapLimit`、`MustShipLimit`、`PriorityOrderLimit`、`SourceEarlyLimit`。
10. 补文档和测试，最后执行 Kotlin 编译与测试。
