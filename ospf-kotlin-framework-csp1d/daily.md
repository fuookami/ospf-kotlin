# CSP1D 泛型化计划

日期：2026-05-21

## 1. 总目标

将 `ospf-kotlin-framework-csp1d` 建设为相对抽象、可复用的一维分切开发包。目标不是把 `poit/csp1d` 原样搬入 framework，而是沉淀一维分切的通用内核，并把项目接口、运行参数、DTO 协议、公式语言、solver 插件选择等下游适配内容留在业务侧。

### 1.1 开发包边界

1. **通用核心层**：material、cutting plan、produce 主问题、solver/quantity adapter。
2. **通用增强层**：yield、length assignment、wasting minimization、schedule variant。
3. **下游适配层**：PO/DTO、公式语言、接口服务、控制台、项目运行参数、心跳、租户上下文、solver 插件选择。

### 1.2 模块目标

第一阶段模块边界如下：

| 模块 | 职责 | 状态 |
|------|------|------|
| `csp1d-infrastructure` | 通用配置、算法参数、solver 选项、排序策略、render DTO、单位/数值 adapter | 已建立 |
| `csp1d-domain-material-context` | 产品、需求、配规、原料、设备、幅宽、切割方案、解、重量计算和物料上下文 | 已建立 |
| `csp1d-domain-cutting-plan-generation-context` | 初始方案生成、pricing、可行性规则、余宽和 reduced cost 目标 | 待增强 |
| `csp1d-domain-produce-context` | 主问题输入、切割方案使用、物料使用、设备产能、解聚合模型 | 已建立 |
| `csp1d-domain-yield-context` | 需求产出聚合、欠产/超产分析及后续约束目标插件 | 基础 solver 接入已完成，待真实验证和收口 |
| `csp1d-domain-length-assignment-context` | 动态卷长分配、超长检测及后续变量约束插件 | 基础 solver 接入已完成，待动态卷长真实建模 |
| `csp1d-domain-wasting-minimization-context` | 余宽、余料、成本、超产面积浪费分析及后续目标插件 | 基础 solver 接入已完成，待真实验证和余料收口 |
| `csp1d-application` | MILP、列生成、恢复、排程、KPI、Top-K 和解分析入口 | 待增强 |

暂不单独拆 `csp1d-domain-schedule-context`。schedule 相关能力先作为 produce、yield、length assignment、wasting minimization 的 variant 保留；当重复接口稳定后再抽出公共层。

### 1.3 功能目标

1. 提供 PO/DTO 无关的 `Csp1dProblem<V>` 输入模型，供下游项目映射业务请求。
2. 使用 `ProductDemand<V>` 统一卷数、重量、张数三类需求；`DemandMode` 只作为标签，不参与数值计算和变量类型判定。
3. 用 `CuttingPlan<V>`、`Cut<V>`、`Segment<V>` 表达切割方案、分段、缺陷、配规、余宽、长度和来源迭代。
4. 支持初始切割方案生成、MILP pricing、reduced cost pricing 和 column generation。
5. 在 application 层直接构建主问题 MILP/LP，不在领域层新增 solver 接口。
6. 输出 `Csp1dSolution<V>`、KPI 和 render 数据，render DTO 继续作为稳定序列化边界。
7. 将 `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3` 的一维 cutting stock 列生成示例迁移为 CSP1D framework 示例，删除示例侧手写 RMP/SP 建模，只保留问题构造、framework 调用和结果展示。

### 1.4 泛型与物理量规则

1. 宽度、长度、重量、坐标、产能等有量纲字段必须使用 `Quantity<V>`。
2. 损耗率、利用率、惩罚系数、归一化目标值等无量纲值使用裸 `V : RealNumber<V>`。
3. DTO 边界可以继续使用 `FltX` 或字符串/BigDecimal，但必须通过 mapper 从领域类型转换。
4. 保留 `Flt64` / `FltX` 兼容入口；领域主路径不得为了兼容入口固定为 `Flt64` 或 `FltX`。
5. 主问题 demand 与 contribution 必须按 product + unit 口径匹配，不允许跨单位聚合。

### 1.5 门禁

后续改动至少需要维持以下门禁：

1. `rg -n "com\\.poit|framework\\.bpp3d" ospf-kotlin-framework-csp1d -g "*.kt"` 无领域代码命中。
2. `rg -n "rollDemand|weightDemand|sheetDemand" ospf-kotlin-framework-csp1d -g "*.kt"` 无领域主路径命中。
3. `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"` 无命中。
4. `rg -n "candidatePlans" ospf-kotlin-framework-csp1d -g "*.kt"` 无命中。
5. `Flt64` / `FltX` 只能出现在 DTO、typealias、legacy factory、测试、数值 adapter、solver adapter 等允许边界。

## 2. 已完成事项

已完成事项仅保留阶段级摘要，不保留逐类、逐测试、逐命令的历史细节。

1. 已完成 CSP1D 基础模块、领域模型和应用入口的第一阶段骨架。
2. 已完成核心物理量、需求口径和泛型算术的基础收口。
3. 已完成统一需求模型、离散/连续需求聚合和 render DTO mapper 的基础能力。
4. 已完成切割方案、物料、设备、配规、需求贡献和方案使用等核心领域对象的泛型化。
5. 已完成 yield、length assignment、wasting minimization 三个增强上下文的分析层基础能力。
6. 已完成 DFS、N-Same、N-Sum、Costar filler、simple initial generator 等基础方案生成能力与对应测试。
7. 已完成 application 层主问题 MILP 化改造，删除 C0-1 阶段的领域层占位 solver 设计。
8. 已完成 LP shadow price 提取、`ShadowPriceMap<V>` 映射和列生成 trace 骨架。
9. 已完成 reduced cost pricing 的默认接入，并修正 demand shadow price key 的 product + unit 口径。
10. 已完成既有关键编译、目标测试和门禁检查；当前记录不声明全量 reactor 测试长期有效。
11. 已完成 C4 列生成迭代记录增强：`Csp1dTerminationReason`、`Csp1dIterationRecord`、LP 目标值追踪和空初始方案处理。
12. 已完成 C5 yield 上下文 solver 化：`ModeledUnderProduction`/`ModeledOverProduction` 建模层扁平类型、`YieldModelingConfig<V>`/`YieldModelingResult<V>` solver 接入、`Csp1dMilpSolver` 欠产/超产松弛变量注册、demand 等式约束转换、yield 惩罚目标、超产上限约束和 solver solution 回填。`Csp1dSolution` 已预留 `yieldResult` 字段。`Csp1dColumnGeneration` 已透传 `yieldConfig`。验收测试已增加 yield 建模正向和反向测试。
13. 已完成 C5 wasting minimization 上下文 solver 化基础接入：`WasteMinimizationConfig<V>` 和 `WasteMinimizationResult<V>` 建模层扁平类型；`Csp1dMilpSolver.solve()` 接受 `wasteConfig` 参数；`setObjective()` 加入余宽、物料成本和超产面积线性目标表达式；`extractWasteResult()` 从 solver solution 回填总余宽、物料成本和超产面积；`MilpResult`、`Csp1dColumnGeneration`、`Csp1dSolution` 已完成 `wasteResult` / `wasteConfig` 透传；验收测试已覆盖废弃建模正向和反向路径。
14. 已修正 waste/yield 联合建模的关键风险：超产松弛变量按 over penalty、over upper bound、over area penalty 任一需求统一注册；`extractWasteResult()` / `extractYieldResult()` 不再分散使用 `Flt64 as V`，改为集中通过 application 内部 solver 数值回填 helper 转为 `Flt64` / `FltX` 同类值；验收测试新增具体数值断言，并覆盖“仅超产面积惩罚”和“仅超产上限”场景。
15. 已完成 C5 length assignment 上下文 solver 化基础接入：`LengthAssignmentModelingConfig<V>`、`LengthAssignmentModelingResult<V>`、`ModeledAssignedLength<V>` 和 `ModeledOverLength<V>` 建模层扁平类型；`Csp1dMilpSolver.solve()` 接受 `lengthConfig` 参数；已注册动态长度产品超长松弛变量、超长上限约束和超长惩罚目标；`extractLengthResult()` 从 solver solution 回填超长结果；`MilpResult`、`Csp1dColumnGeneration`、`Csp1dSolution` 已完成 `lengthResult` / `lengthConfig` 透传；验收测试已覆盖 lengthConfig 正反向、仅超长上限和列生成透传路径。

## 3. 未完成事项

### 3.0 下一会话交接：C4~C5 未完成项

C4~C5 不能按整体完成处理。当前已经完成的是列生成 trace、yield/waste/length 三个增强上下文的 application solver 基础接入和 fake solver 验收；仍需下一会话继续做真实 solver、动态卷长和端到端口径收口。

下一会话建议优先顺序：

1. 先推进 C4 真实 solver 收敛验证：跑通 LP -> dual price -> reduced cost pricing -> 新列加入 -> 最终 MILP，并确认 trace 与实际迭代一致。
2. 再收口 C5 length assignment：补动态卷长决策变量、`assignedLengths` 回填、真实长度联动约束和 `totalLengthPenalty` 目标；`batchMinPenalty` 需先明确批次变量语义后再实现。
3. 同步补 C5 yield/waste 真实 solver 端到端验证，确认 solver 回填值与分析层口径一致。
4. 继续执行门禁搜索，避免引入 `com.poit`、`framework.bpp3d`、`ProduceSolver`、`SimpleProduceSolver`、`candidatePlans` 或不合规的固定数值类型。

### 3.1 C4 列生成真实 solver 收敛验证

#### 事项

`ReducedCostPricingGenerator` 已接入默认列生成流程，但当前 application 验收主要依赖 fake solver，尚未验证真实 solver 上的 LP 松弛、dual price、pricing 新列加入和收敛质量。

#### 计划

1. 构造一个小规模、可手算或可稳定复现的 CSP1D 列生成样例。
2. 使用真实 `ColumnGenerationSolver` 跑通 LP -> dual price -> reduced cost pricing -> 新列加入 -> MILP 求解链路。
3. 记录每轮方案池规模、`pricedPlanCount`、目标值变化和终止原因。
4. 对含不同需求单位和包含 `_` 的 id 场景做回归验证。

#### 修改清单

1. `csp1d-application/src/test/.../Csp1dApplicationAcceptanceTest.kt`
   - 新增真实 solver 或集成级列生成样例。
   - 保留 fake solver 单元验收，避免基础编排测试依赖外部 solver 可用性。
2. `csp1d-application/src/main/.../application/service/Csp1dColumnGeneration.kt`
   - 如有必要，补充 trace 字段以记录每轮 LP、pricing 和终止状态。
3. `csp1d-domain-cutting-plan-generation-context/src/test/...`
   - 补充 reduced cost pricing 与 shadow price key 的边界测试。

#### 验收标准

1. 真实 solver 样例中列生成至少完成一轮 LP 与 pricing，并能在无负 reduced cost 新列时停止。
2. `pricedPlanCount`、方案池规模和 trace 记录与实际迭代一致。
3. `ProductDemandShadowPriceKey(productId, unitSymbol)` 能区分 roll、sheet、weight 等不同单位需求。
4. product/material/machine id 包含 `_` 时 shadow price 仍能正确映射。
5. application 验收测试和 reduced cost pricing 测试通过。

#### 当前进度

- 已完成 `Csp1dTerminationReason` 枚举、`Csp1dIterationRecord` 数据类和列生成 trace 追踪。
- 已完成 `Csp1dColumnGenerationRealSolverTest` 测试骨架（gurobi-test 目录）。
- 尚未在真实 Gurobi solver 上完成端到端收敛验证。

### 3.2 C5 yield 上下文 solver 化（已完成基础接入）

#### 事项

`YieldContext` 已具备需求产出聚合、欠产和超产分析能力，但欠产/超产尚未接入主问题变量、约束和目标函数。

#### 已完成

1. 明确 yield 与 produce 主问题的组合边界：`YieldModelingConfig<V>` 作为 application 层建模参数传入 `Csp1dMilpSolver`，不侵入领域层 `ProduceInput`。
2. 为欠产和超产建立 `URealVar` 松弛变量，demand 约束转为等式约束。
3. 支持最小化欠产惩罚、超产惩罚和超产上限约束。
4. 保持需求比较按 product + unit 口径执行。
5. `ModeledUnderProduction`/`ModeledOverProduction` 建模层扁平类型与 `YieldModel` 分析层富类型共存，通过命名区分。
6. `Csp1dColumnGeneration` 透传 `yieldConfig`，`Csp1dSolution` 预留 `yieldResult` 字段。

#### 后续收口

1. 在真实 solver 上验证 yield 建模的欠产/超产回填值与分析层口径一致。
2. 补充 sheet、weight 需求口径下的端到端建模测试（当前测试覆盖 roll 和 unit 区分）。
3. 考虑 yield 建模与列生成的交互：LP 轮次中是否也需要 yield slack。

### 3.3 C5 length assignment 上下文 solver 化（已完成基础接入）

#### 已完成

1. 明确 length assignment 与 produce 主问题的组合边界：`LengthAssignmentModelingConfig<V>` 作为 application 层建模参数传入 `Csp1dMilpSolver`，不侵入领域层 `ProduceInput`。
2. 新增 `LengthAssignmentModelingConfig<V>`、`LengthAssignmentModelingResult<V>`、`ModeledAssignedLength<V>` 和 `ModeledOverLength<V>` 建模层扁平类型，与 `LengthAssignmentContext` 分析层富类型共存。
3. `Csp1dMilpSolver.solve()` 接受 `lengthConfig` 参数，并为动态长度产品按需注册超长松弛变量。
4. 支持超长惩罚目标和超长松弛变量上限约束，`extractLengthResult()` 可从 solver solution 回填超长结果。
5. `Csp1dColumnGeneration` 透传 `lengthConfig`，`Csp1dSolution` 预留 `lengthResult` 字段。
6. application 验收测试覆盖：无 `lengthConfig` 时不回填 `lengthResult`、配置超长惩罚时回填超长结果、仅配置超长上限时注册超长松弛变量，以及列生成最终 MILP 透传 `lengthConfig`。

#### 后续收口

1. 当前基础接入尚未建立动态卷长决策变量、已分配长度回填和 `assignedLength - maxOverProduceLength <= overLength` 等真实长度联动约束，需要补齐后才能让 solver 实际决定卷长。
2. `totalLengthPenalty` 和 `batchMinPenalty` 已在配置中预留，但尚未接入目标函数，需要明确总卷长最小化和动态长度批次最小化的线性表达方式。
3. 补充 `Flt64` / `FltX` 默认 length derivation 或 adapter，并明确动态长度产品与固定长度产品在主问题中的建模差异。
4. 继续补充固定长度混合和 `Quantity<V>` 单位一致性端到端建模测试。
5. 在真实 solver 上验证 length assignment 建模的超长回填值与分析层口径一致。

### 3.4 C5 wasting minimization 上下文 solver 化（已完成基础接入）

#### 已完成

1. 明确 wasting 与 produce 主问题的组合边界：`WasteMinimizationConfig<V>` 作为 application 层建模参数传入 `Csp1dMilpSolver`，不侵入领域层 `ProduceInput`。
2. 为余宽、物料成本和超产面积建立线性目标表达式，接入 `setObjective()`。
3. `extractWasteResult()` 从 solver solution 回填总余宽、物料成本和超产面积。
4. `Csp1dColumnGeneration` 透传 `wasteConfig`，`Csp1dSolution` 预留 `wasteResult` 字段。
5. 验收测试覆盖：废弃建模正向（余宽、物料成本、yield+废弃联合、仅超产面积惩罚、仅超产上限）和反向（无 wasteConfig）。
6. 超产松弛变量注册条件已收口，不再依赖单一 `overProductionPenalty` 配置。
7. solver solution 回填时已集中处理 `Flt64` / `FltX` 转换，避免在 waste/yield 回填路径分散强转。

#### 后续收口

1. 在真实 solver 上验证废弃建模的余宽/物料成本/超产面积回填值与分析层口径一致。
2. 补充不同物料宽度范围、多物料和多需求场景下的端到端建模测试。
3. 考虑 wasting 建模与列生成的交互：LP 轮次中是否也需要 waste 目标项。
4. 考虑余料（长度方向浪费）的建模，当前仅覆盖余宽和超产面积。
5. 多宽度产品的超产面积目前取 `product.width.firstOrNull()`，后续需要明确业务语义。

### 3.5 切割方案生成算法补齐

#### 事项

当前已有基础枚举和 reduced cost pricing 接入，但 FullSum、缺陷/分段、unitBatch、单位长度、并行和时间限制等能力仍未补齐。

#### 计划

1. 优先补齐不依赖 solver 的生成算法和可行性规则。
2. 将缺陷、分段、边部/中部、刀数、批次冲突、单位长度等规则抽成可组合 constraint。
3. 为不同 generator 建立一致的输入、输出、排序和去重策略。
4. 控制并行和时间限制，不让 generator 阻塞 application 主流程。

#### 修改清单

1. `csp1d-domain-cutting-plan-generation-context/src/main/...`
   - 补齐 FullSum、缺陷/分段、unitBatch、单位长度、并行和时间限制实现。
   - 梳理 `MaterialConstraint`、`SegmentConstraint`、`PositionAmountConstraint`、`OnSideConstraint`、`InMiddleConstraint`、`UnitBatchConflictConstraint` 的组合方式。
2. `csp1d-domain-cutting-plan-generation-context/src/test/...`
   - 增加各生成算法和约束插件的窄范围测试。

#### 验收标准

1. FullSum、DFS、N-Same、N-Sum 在相同输入下输出可解释且不重复的可行方案。
2. 缺陷、分段、边部/中部、刀数、批次冲突和单位长度约束均有测试覆盖。
3. generator 支持时间限制或候选数量限制。
4. cutting-plan-generation 域测试通过。

### 3.6 pricing 子问题性能优化

#### 事项

当前默认 `ReducedCostPricingGenerator` 复用初始方案生成器枚举候选，适合验证链路，但不足以支撑较大规模 pricing 子问题。

#### 计划

1. 评估 DFS、NSum、MILP pricing 和专用 reduced cost pricing 的适用场景。
2. 支持按问题规模、需求类型和物料数量注入不同 pricing generator。
3. 增加候选剪枝、上界估计、排序和缓存策略。
4. 在真实 solver 收敛验证后再做性能基准，避免优化错误链路。

#### 修改清单

1. `csp1d-domain-cutting-plan-generation-context/src/main/.../ReducedCostPricingGenerator.kt`
   - 支持更高效的候选生成和剪枝。
2. `csp1d-application/src/main/.../application/service/Csp1dColumnGeneration.kt`
   - 明确 pricing generator 注入点和默认策略。
3. `csp1d-domain-cutting-plan-generation-context/src/test/...`
   - 增加性能不退化的规模化样例或轻量 benchmark 测试。

#### 验收标准

1. pricing generator 可按场景替换，application 不依赖具体实现。
2. 在小规模真实 solver 样例中，优化后结果与基线一致。
3. 在中等规模样例中，候选数量、耗时或迭代次数有可记录改善。

### 3.7 Flt64/FltX 门禁持续收口

#### 事项

当前 reduced cost 主逻辑已避免固定 `Flt64` / `FltX`，但后续新增领域主代码仍需持续检查，确保泛型数值口径不被破坏。

#### 计划

1. 每轮涉及领域主代码改动后运行 `Flt64|FltX` grep。
2. 将允许位置分类为 DTO、typealias、legacy factory、测试、arithmetic adapter、solver adapter。
3. 对不在允许边界的命中进行泛型化或移动到 adapter。

#### 修改清单

1. `ospf-kotlin-framework-csp1d/**/*.kt`
   - 持续检查新增 `Flt64` / `FltX` 命中。
2. `csp1d-infrastructure`
   - 如确实需要固定数值边界，集中放入 adapter 或 legacy factory。

#### 验收标准

1. `rg -n "Flt64|FltX" ospf-kotlin-framework-csp1d -g "*.kt"` 的命中均可归类到允许边界。
2. 领域模型、生成算法、主问题输入和增强上下文不因 DTO 或 solver 兼容入口固定数值类型。
3. 新增测试覆盖 `Product<Flt64>` 与 `Product<FltX>` 或等价泛型路径。

### 3.8 demo3 示例迁移到 CSP1D framework

#### 事项

`ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3` 当前手写了 `Product`、`CuttingPlan`、RMP、SP 和列生成循环。等 CSP1D framework 的列生成、pricing、真实 solver 验证和必要增强能力稳定后，应将该示例整体迁移为 framework 使用样例，避免 example 继续维护一套独立的一维分切建模代码。

#### 计划

1. 将 demo3 的裸 `UInt64 length/demand` 输入映射为 `Product<Flt64>`、`Material<Flt64>`、`Machine<Flt64>` 和 `ProductDemand<Flt64>`。
2. 使用 `Quantity<Flt64>` 表达原料长度、产品长度或等价一维分切尺寸，避免在示例领域主路径继续使用裸数值量纲。
3. 使用 `Csp1dProblem<Flt64>` 承载示例输入，由 `Csp1dColumnGeneration` 执行列生成。
4. 使用真实 `ColumnGenerationSolver` 验证 demo3 的 LP -> shadow price -> pricing -> 新列加入 -> 最终 MILP 流程。
5. 删除或废弃 demo3 中手写的 `RMP.kt`、`SP.kt` 和示例专用 `ShadowPriceMap`，只保留 framework 示例入口和结果打印。

#### 修改清单

1. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3/Domain.kt`
   - 移除示例专用领域模型，或改为 framework 模型构造 helper。
2. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3/RMP.kt`
   - 删除手写主问题建模，改由 `Csp1dMilpSolver` / `Csp1dColumnGeneration` 承担。
3. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3/SP.kt`
   - 删除手写 pricing 子问题，改由 CSP1D pricing generator 承担。
4. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3/Main.kt`
   - 改为构造 `Csp1dProblem<Flt64>`、注入 solver、调用 `Csp1dColumnGeneration.solveWithTrace()` 并输出方案使用量和 trace。
5. `ospf-kotlin-framework-csp1d/csp1d-application/src/test/...`
   - 如 demo3 迁移需要新增 framework 侧能力，优先补 application 或 gurobi-test 的回归测试。

#### 验收标准

1. demo3 示例不再直接构建 `LinearMetaModel`、`UIntVar`、RMP demand constraint 或 SP 背包模型。
2. demo3 示例不再定义独立于 framework 的 `Product`、`CuttingPlan`、`ShadowPriceMap` 领域模型。
3. demo3 示例通过 `Csp1dProblem<Flt64>` 和 `Csp1dColumnGeneration` 完成求解。
4. demo3 示例输出的切割方案使用量、未满足需求和列生成 trace 可读，能体现每轮新列生成情况。
5. 迁移后的 demo3 编译通过，且不引入 `com.poit`、`framework.bpp3d`、`ProduceSolver`、`SimpleProduceSolver`、`candidatePlans` 等门禁命中。
6. 若真实 solver 在当前环境可用，demo3 能完成端到端运行；若真实 solver 不可用，至少保留可编译入口和对应真实 solver 集成测试说明。

### 3.9 文档与验证记录维护

#### 事项

`daily.md` 已从历史交接记录整理为目标、已完成摘要和未完成事项。后续需要维持该结构，避免恢复逐类逐测试的历史堆叠。

#### 计划

1. 每轮只更新已完成摘要和相关未完成事项状态。
2. 验证记录只记录当前实际执行过的命令，不声明未执行的全量测试。
3. 完成某个事项后，将其从未完成事项移入已完成摘要，并保留下一步事项。

#### 修改清单

1. `ospf-kotlin-framework-csp1d/daily.md`
   - 维护总目标、已完成事项和未完成事项三段结构。
   - 不恢复多轮交接日志和重复验证明细。

#### 验收标准

1. 已完成事项保持阶段级摘要，不出现逐文件、逐断言、逐命令的细节堆叠。
2. 每个未完成事项都按"事项、计划、修改清单、验收标准"组织。
3. 文档能直接指导下一轮实现，不需要翻历史交接记录。
