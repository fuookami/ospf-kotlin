# CSP1D 泛型化计划

日期：2026-06-08

## 1. 总目标

将 `ospf-kotlin-framework-csp1d` 建设为相对抽象、可复用的一维分切开发包。目标不是把 `poit/csp1d` 原样搬入 framework，而是沉淀一维分切的通用内核，并把项目接口、运行参数、DTO 协议、公式语言、solver 插件选择等下游适配内容留在业务侧。

当前抽象口径以 `csp1d-domain-material-context` 已提供的实体能力为边界：只保留 `Product`、`ProductDemand`、`Production`、`Costar`、`Material`、`Machine`、`CuttingPlanSlice`、`CuttingPlan`、需求贡献和当前增强上下文能够表达的部分；不把 POIT 中尚未进入 material model 的缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、公式语言和业务 DTO 当作当前阶段的必备模型。

### 1.1 开发包边界

1. **通用核心层**：material、cutting plan、produce 主问题、solver/quantity adapter。
2. **通用增强层**：yield、length assignment、wasting minimization、schedule variant。
3. **下游适配层**：PO/DTO、公式语言、接口服务、控制台、项目运行参数、心跳、租户上下文、solver 插件选择。

### 1.2 模块目标

| 模块 | 职责 | 状态 |
|------|------|------|
| `csp1d-infrastructure` | 通用配置、算法参数、solver 选项、排序策略、render DTO、单位/数值 adapter | 已建立，待收口 |
| `csp1d-domain-material-context` | 产品、需求、配规、原料、设备、幅宽、切割方案、解、重量计算和物料上下文 | 已建立，待补业务无关口径 |
| `csp1d-domain-cutting-plan-generation-context` | 初始方案生成、pricing、可行性规则、余宽和 reduced cost 目标 | 已建立，待增强和性能优化 |
| `csp1d-domain-produce-context` | 主问题输入、切割方案使用、物料使用、设备产能、解聚合模型 | 已建立，待产能口径修正 |
| `csp1d-domain-yield-context` | 需求产出聚合、欠产/超产分析和建模配置 | 基础 solver 接入已完成 |
| `csp1d-domain-length-assignment-context` | 动态卷长分配、超长检测和建模配置 | 基础 solver 接入已完成，待与方案级长度口径收敛 |
| `csp1d-domain-wasting-minimization-context` | 余宽、余料、成本、超产面积浪费分析和建模配置 | 基础 solver 接入已完成，待面积口径显式化 |
| `csp1d-application` | MILP、列生成、恢复、排程、KPI、Top-K 和解分析入口 | 主路径已建立，待能力整合 |

暂不单独拆 `csp1d-domain-schedule-context`。schedule 相关能力先作为 produce、yield、length assignment、wasting minimization 的 variant 保留；当重复接口稳定后再抽出公共层。

### 1.3 功能目标

1. 提供 PO/DTO 无关的 `Csp1dProblem<V>` 输入模型，供下游项目映射业务请求。
2. 使用 `ProductDemand<V>` 统一卷数、重量、张数三类需求；`DemandMode` 只作为标签，不参与数值计算和变量类型判定。
3. 用当前 material model 可表达的 `CuttingPlan<V>`、`CuttingPlanSlice<V>` 和需求贡献表达切割方案；后续扩展分段、缺陷、位置约束时必须先补通用领域实体。
4. 支持初始切割方案生成、reduced cost pricing、column generation 和最终 MILP 求解。
5. 在 application 层直接构建主问题 MILP/LP，不在领域层新增 solver 接口。
6. 输出 `Csp1dSolution<V>`、KPI 和 render 数据，render DTO 继续作为稳定序列化边界。
7. 将 `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3` 的一维 cutting stock 列生成示例迁移为 CSP1D framework 示例，删除示例侧手写 RMP/SP 建模。

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
5. `Flt64` / `FltX` 只能出现在 DTO、typealias、legacy factory、测试、数值 adapter、solver adapter、application 建模边界和 LP trace 等允许边界。
6. Kotlin 注释保持中英双语；超过 2 个参数的函数调用保持多行命名参数。

## 2. 已完成事项

已完成事项只保留阶段级摘要，不保留逐类、逐断言、逐命令的历史细节。

1. 已建立 CSP1D 基础模块、领域模型、应用入口和 PO/DTO 无关的问题输入主路径。
2. 已完成核心物理量、泛型数值、需求口径和 render DTO mapper 的基础收口。
3. 已完成 `ProductDemand<V>` 对卷数、重量、张数需求的统一表达，并按 product + unit 口径匹配需求贡献。
4. 已完成 `Product`、`Material`、`Machine`、`Costar`、`CuttingPlan`、需求贡献和方案使用等当前核心实体的泛型化。
5. 已完成 application 层主问题 MILP/LP 建模、LP shadow price 提取、`ShadowPriceMap<V>` 映射和列生成 trace 骨架。
6. 已完成 reduced cost pricing 的默认链路、真实 solver 列生成端到端验证和多需求单位回归验证。
7. 已完成 yield、length assignment、wasting minimization 三个增强上下文的 solver 化基础接入、结果回填和列生成透传。
8. 已完成 yield/waste/length 多需求口径、多物料场景、动态/固定长度差异和单位一致性的端到端验证。
9. 已完成切割方案生成第一阶段能力：DFS、N-Same、N-Sum、FullSum、Costar filler、可组合约束、超时退出和基础测试。
10. 已完成 waste 余料面积代理、超产面积 `product.maxWidth()` 口径和 canonical form 去重第一阶段。
11. 已完成当前模型与 POIT CSP1D 的边界复核：现有模型来源正确，主干未偏离；当前只承诺 material-context 已表达实体能建模的部分。
12. 已完成当前主问题多项式复核：基础 demand、material、machine、yield、waste、length 线性表达方向正确；需要显式修正的风险主要集中在设备 `capacity` 未入约束、超产面积代理口径和增强目标未参与 pricing。

## 3. 需要修正的事项

### 3.1 主问题产能与资源口径收口

#### 目标

让 produce 主问题的资源约束完整覆盖当前 `Material` 和 `Machine` 已暴露的字段，避免实体已有字段只在结果中回显而不参与可行性判断。

#### 事项

1. `Machine.capacity: Quantity<V>?` 当前未进入 MILP 约束，只在结果统计中回显。
2. `MachineCapacityUsage.used` 当前回填的是配置值，不是 solver 解中的实际使用值。
3. `Machine.maxSwitchCount` 当前未建模；如果保留字段，需要明确为后续能力或实现通用换产约束。
4. `Material.availableBatches` 已建模为 `Σx_j <= availableBatches`，但缺少与 future batch factor 的统一入口。

#### 计划

1. 为 `CuttingPlan` 或 produce 建模输入新增通用产能消耗描述，支持按方案使用量计算 `Σ consumption_j * x_j <= machine.capacity`。
2. 保持 `maxBatchCount` 作为离散批次数约束，`capacity` 作为带单位的业务产能约束，两者可同时启用。
3. 修改 `machineUsages()`，从 solver 解聚合实际产能使用量；无产能消耗定义时不伪装为已使用。
4. 对 `maxSwitchCount` 做二选一处理：实现基础 material switch 计数，或从当前阶段文档/API 中标记为未建模字段。

#### 修改清单

1. `csp1d-domain-material-context/src/main/.../Machine.kt`
2. `csp1d-domain-material-context/src/main/.../CuttingPlan.kt`
3. `csp1d-domain-produce-context/src/main/.../Produce.kt`
4. `csp1d-application/src/main/.../Csp1dMilpSolver.kt`
5. `csp1d-application/src/test/.../Csp1dApplicationAcceptanceTest.kt`
6. `csp1d-application/src/gurobi-test/.../Csp1dColumnGenerationRealSolverTest.kt`

#### 验收标准

1. 当 `Machine.capacity` 配置且方案有产能消耗时，MILP 添加对应 `<= capacity` 约束。
2. 同一设备同时配置 `maxBatchCount` 与 `capacity` 时，两类约束同时生效。
3. `MachineCapacityUsage.used` 反映 solver 解聚合后的实际使用量。
4. 未配置产能消耗时，结果不误报已使用产能。
5. fake solver 与 Gurobi profile 均覆盖产能约束生效、不可行或受限的场景。

### 3.2 超产面积与余料口径显式化

#### 目标

把 waste 目标中的面积类口径从隐式代理改成显式策略，保证调用方知道当前结果是最大宽度代理、方案级面积还是其他业务口径。

#### 事项

1. 当前 `overProductionAreaPenalty` 使用 `over_i * product.maxWidth()`，是当前实体能力下的保守代理，不等价于 POIT 的方案级 `plan.overProduceArea * x_j`。
2. 当前 `restMaterialPenalty` 使用 `restWidth * material.length * x_j`，适合作为余料面积代理，但需要与 `WasteModel` 分析层命名保持一致。
3. 多宽度产品、动态长度产品、缺少 material length 的场景需要统一空值策略。

#### 计划

1. 为 `WasteMinimizationConfig` 增加面积口径策略或明确命名，至少区分 `ProductMaxWidthProxy` 与未来可扩展的 `PlanArea`。
2. 将 `WasteMinimizationResult` 中的面积字段命名、注释和 render/KPI 输出统一为“代理”或“精确”口径。
3. 保留当前 `product.maxWidth()` 默认行为，但在测试中固定其语义，避免误改回 `width.first()`。
4. 评估是否需要在 `CuttingPlanDemandContribution` 中携带 width/length 来源，以支持后续方案级超产面积。

#### 修改清单

1. `csp1d-application/src/main/.../WasteMinimizationConfig.kt`
2. `csp1d-application/src/main/.../Csp1dMilpSolver.kt`
3. `csp1d-domain-wasting-minimization-context/src/main/.../WasteModel.kt`
4. `csp1d-application/src/test/.../Csp1dApplicationAcceptanceTest.kt`
5. `csp1d-application/src/gurobi-test/.../Csp1dColumnGenerationRealSolverTest.kt`

#### 验收标准

1. 文档、配置和结果字段能清晰说明超产面积使用何种口径。
2. 多宽度产品的超产面积测试覆盖宽度顺序不敏感。
3. 缺少 material length 时余料代理不产生错误目标项，并有反向测试覆盖。
4. Gurobi profile 中 waste + yield 联合建模仍能回填稳定数值。

### 3.3 Pricing 与最终目标的一致性修正

#### 目标

让 column generation 的 pricing 策略与最终 MILP 目标保持可解释的一致性，同时不破坏 LP shadow price 的基础语义。

#### 事项

1. 当前 LP 轮次只用于提取基础 demand/material/machine shadow price，不加入 yield slack、waste 目标和 length 目标，这是正确的基础列生成口径。
2. 最终 MILP 若启用 waste/length/yield 目标，当前 pricing 不会主动寻找能改善这些增强目标的列。
3. `ReducedCostPricingGenerator` 当前仍以枚举候选再筛选为主，适合正确性验证，不适合中大规模问题。

#### 计划

1. 明确两类 pricing 策略：基础 RMP reduced cost pricing 与最终目标感知候选增强。
2. 在不改变 LP shadow price 提取方式的前提下，为 pricing 增加候选评分项，可选考虑余宽、余料、物料成本和长度惩罚。
3. 将 pricing generator 注入点收口为稳定接口，支持 DFS/NSum/FullSum/MILP pricing/专用 reduced cost pricing 按场景替换。
4. 增加候选上界估计、重复列过滤、按物料和需求单位的缓存，以及可配置候选数量上限。

#### 修改清单

1. `csp1d-domain-cutting-plan-generation-context/src/main/.../CuttingPlanGenerationContext.kt`
2. `csp1d-domain-cutting-plan-generation-context/src/main/.../ReducedCostPricingGenerator.kt`
3. `csp1d-domain-cutting-plan-generation-context/src/main/.../CuttingPlanCanonicalKey.kt`
4. `csp1d-application/src/main/.../Csp1dColumnGeneration.kt`
5. `csp1d-application/src/test/...`
6. `csp1d-domain-cutting-plan-generation-context/src/test/...`

#### 验收标准

1. 默认 pricing 与现有列生成结果兼容，原有真实 solver 回归继续通过。
2. 启用 waste/length 目标时，候选生成能稳定偏向低余宽、低余料或低惩罚方案，并有测试证明。
3. duplicate plan 按 ID 和 canonical key 都能被排除。
4. 中等规模样例的候选数量、耗时或迭代次数有可记录改善。

## 4. 未完成事项

### 4.1 Material-context 当前实体能力补齐

#### 目标

在不一比一复刻 POIT 的前提下，补齐当前 material-context 已暴露实体自然应支持的通用行为。

#### 事项

1. `ProductDemand` 与 `CuttingPlanDemandContribution` 的贡献构造仍主要依赖调用方手动填写。
2. `Costar` 作为 `Production` 可进入切片，但没有清晰区分是否参与需求、是否只用于填充剩余宽度。
3. `Material.enabled(plan)` 只覆盖基础 machine 和 width range，尚未与 generation/pricing 的可行性入口完全统一。
4. `Product.dynamicLength` 与 length assignment 建模已有连接，但方案生成阶段仍缺少更明确的默认贡献和长度策略。

#### 计划

1. 增加通用 contribution builder，按 demand unit 生成 roll/weight/sheet 贡献，避免下游重复手写。
2. 将 Costar filler 的行为和 `Costar` 的通用语义写入模型或配置，明确 costar 默认不进入 demand contribution。
3. 将 `Material.enabled(plan)`、`Machine.enabled(material)`、generation constraints 和 pricing 入口合并为统一可行性检查链。
4. 为 dynamic/fixed length 产品补充生成阶段的默认长度策略和测试。

#### 修改清单

1. `csp1d-domain-material-context/src/main/.../ProductDemand.kt`
2. `csp1d-domain-material-context/src/main/.../CuttingPlanDemandContribution.kt`
3. `csp1d-domain-material-context/src/main/.../Costar.kt`
4. `csp1d-domain-material-context/src/main/.../Material.kt`
5. `csp1d-domain-cutting-plan-generation-context/src/main/...`
6. `csp1d-domain-material-context/src/test/...`

#### 验收标准

1. 常见 roll/weight/sheet 贡献可通过通用 helper 生成。
2. Costar filler 不会意外污染产品需求贡献。
3. 生成器和 solver 对同一个 plan 的基础可行性判断一致。
4. 动态长度与固定长度产品在生成和最终 MILP 中的行为均有测试覆盖。

### 4.2 切割方案生成算法增强

#### 目标

把当前生成器从“链路验证可用”推进到“中等规模可用”，并为后续扩展模型预留清晰入口。

#### 事项

1. DFS/NSum/NSame/FullSum 已可用，但并行生成、缓存和规模化剪枝不足。
2. 单位长度、更细粒度长度约束和动态长度产品的生成策略仍待收口。
3. 缺陷、分段、onSide/inMiddle、`unitBatch` 等 POIT 语义暂不属于当前实体边界；只有当 material model 增加通用实体后再纳入。

#### 计划

1. 为生成器增加 coroutine 并行、全局 timeout、候选上限和可中断统计。
2. 增加 width combination 的上界估计、dominance 剪枝和按 material/demand unit 的缓存。
3. 统一 DFS/NSum/NSame/FullSum 的 contribution 构造和 canonical 去重。
4. 将暂不建模的 POIT 语义集中放入“延后能力”清单，不在当前代码中留半成品字段。

#### 修改清单

1. `csp1d-domain-cutting-plan-generation-context/src/main/.../service/DFSGenerator.kt`
2. `csp1d-domain-cutting-plan-generation-context/src/main/.../service/NSumGenerator.kt`
3. `csp1d-domain-cutting-plan-generation-context/src/main/.../service/NSameGenerator.kt`
4. `csp1d-domain-cutting-plan-generation-context/src/main/.../service/FullSumGenerator.kt`
5. `csp1d-domain-cutting-plan-generation-context/src/main/.../model/GenerationConstraints.kt`
6. `csp1d-domain-cutting-plan-generation-context/src/test/...`

#### 验收标准

1. 所有生成器支持统一 timeout、候选上限和统计信息。
2. 并行开关不改变结果集合的 canonical form。
3. 中等规模样例生成耗时和候选数量有基线记录。
4. 延后能力清单不影响当前 material-context 边界内的编译和求解。

### 4.3 Application API、KPI 和恢复能力

#### 目标

把 application 层从 solver 调用入口整理成稳定的 framework 使用面，减少下游项目直接碰 MILP 内部实现的需要。

#### 事项

1. `Csp1dProblem<V>`、`Csp1dSolution<V>`、KPI、Top-K、render 输出仍需统一入口和文档化。
2. 列生成 trace 已有基础字段，但恢复、重复列、LP 失败和最终 MILP 失败的结果表达仍可读性不足。
3. 目前增强配置分散在 yield/waste/length，缺少面向调用方的一站式建模配置。

#### 计划

1. 收口 `Csp1dProblem` builder 和 `Csp1dSolveConfig`，统一 solver、pricing、yield、waste、length、trace、Top-K 配置。
2. 补充 `Csp1dSolution` KPI：需求满足、欠产/超产、余宽、余料、物料成本、产能使用、列生成收敛信息。
3. 明确异常和部分成功结果：LP 失败、pricing 无新列、重复列收敛、最终 MILP 不可行。
4. 为恢复和 warm start 预留输入输出结构，不引入业务 DTO。

#### 修改清单

1. `csp1d-application/src/main/.../model/Csp1dProblem.kt`
2. `csp1d-application/src/main/.../model/Csp1dSolution.kt`
3. `csp1d-application/src/main/.../service/Csp1dColumnGeneration.kt`
4. `csp1d-application/src/main/.../service/Csp1dMilpSolver.kt`
5. `csp1d-infrastructure/src/main/.../dto/RenderDTO.kt`
6. `csp1d-application/src/test/...`

#### 验收标准

1. 调用方可以通过一个稳定配置对象完成普通 MILP 和列生成求解。
2. solution 中的 KPI 与 solver 回填值一致，并覆盖空解、部分解和完整解。
3. trace 能解释终止原因、每轮 LP 目标值、新列数量和最终 MILP 状态。
4. 不引入 POIT DTO、运行参数、公式语言或 solver 插件选择逻辑。

### 4.4 demo3 示例迁移

#### 目标

将 `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3` 从手写一维 cutting stock 建模迁移为 CSP1D framework 使用样例。

#### 事项

1. demo3 当前仍维护独立的 `Product`、`CuttingPlan`、`RMP`、`SP` 和 `ShadowPriceMap`。
2. 示例输入仍偏裸数值，未体现 framework 的 `Quantity<V>` 和 `ProductDemand<V>`。
3. 示例输出需要能体现方案使用、需求满足和列生成 trace。

#### 计划

1. 将 demo3 输入映射为 `Product<Flt64>`、`Material<Flt64>`、`Machine<Flt64>`、`ProductDemand<Flt64>` 和 `Csp1dProblem<Flt64>`。
2. 使用 `Csp1dColumnGeneration` 替代示例侧 RMP/SP。
3. 删除或废弃 demo3 专用领域模型和手写 shadow price map。
4. 输出选中方案、未满足需求、KPI 和 trace。

#### 修改清单

1. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3/Domain.kt`
2. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3/RMP.kt`
3. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3/SP.kt`
4. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3/Main.kt`
5. `ospf-kotlin-framework-csp1d/csp1d-application/src/test/...`

#### 验收标准

1. demo3 不再直接构建 `LinearMetaModel`、`UIntVar`、RMP demand constraint 或 SP 背包模型。
2. demo3 不再定义独立于 framework 的一维分切领域模型。
3. demo3 通过 `Csp1dProblem<Flt64>` 和 `Csp1dColumnGeneration` 完成求解。
4. 示例编译通过，并保持 CSP1D 门禁无新增命中。

## 5. 下一轮执行计划

### 5.1 合并执行范围

#### 目标

下一轮尽量以一次宽范围迭代完成主问题修正、pricing 对齐、生成增强和示例迁移的基础版本，减少反复切换上下文。

#### 事项

1. 优先修正当前实体已暴露但未建模的 `Machine.capacity`。
2. 同步显式化 waste 面积口径，避免后续测试继续绑定隐式代理。
3. 在 pricing 中加入增强目标感知候选评分，不改变 LP shadow price 基础约束。
4. 扩展生成器性能和可中断能力。
5. 打通 application 一站式配置和 demo3 示例。

#### 计划

1. 第一段：主问题修正，完成产能约束、使用量回填、面积口径配置和相关测试。
2. 第二段：pricing/生成器增强，完成 canonical 去重稳定化、候选评分、并行/timeout/统计和性能样例。
3. 第三段：application API 与 demo3，完成统一 solve config、KPI 输出和示例迁移。
4. 第四段：执行目标测试、Gurobi profile 编译或端到端验证、门禁搜索和 `git diff --check`。

#### 修改清单

1. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context`
2. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context`
3. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context`
4. `ospf-kotlin-framework-csp1d/csp1d-domain-yield-context`
5. `ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context`
6. `ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context`
7. `ospf-kotlin-framework-csp1d/csp1d-application`
8. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

#### 验收标准

1. 当前 `material-context` 实体能表达的目标和约束均有明确多项式或明确“不建模”说明。
2. fake solver 测试覆盖主问题、yield、waste、length、pricing 和 generation 的新增行为。
3. Gurobi profile 至少完成 `test-compile`；环境可用时执行端到端目标测试。
4. demo3 示例不再维护手写 RMP/SP。
5. CSP1D 门禁搜索、`git diff --check -- ospf-kotlin-framework-csp1d` 通过。
6. `daily.md` 继续只保留阶段摘要和下一轮计划，不恢复历史流水。

### 5.2 验证基线

#### 目标

建立下一轮可重复执行的最小验证集，避免只依赖历史 surefire 报告。

#### 事项

1. 单模块测试在当前环境可能因依赖解析或 Gurobi 环境差异失败，需要明确命令和预期。
2. 门禁搜索必须覆盖领域代码和测试命名例外。
3. 固定数值类型命中需要持续分类。

#### 计划

1. 优先执行 `mvn -pl ospf-kotlin-framework-csp1d/csp1d-application -am test-compile`。
2. 优先执行 `mvn -pl ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context -am test`，若超时则拆目标测试。
3. Gurobi 环境可用时执行 `mvn -pl ospf-kotlin-framework-csp1d/csp1d-application -am -Pgurobi-cg-test test`；不可用时至少执行 `-DskipTests test-compile`。
4. 每次改动后执行门禁 grep 和 `git diff --check`。

#### 修改清单

1. `ospf-kotlin-framework-csp1d/daily.md`
2. 必要时新增 `ospf-kotlin-framework-csp1d` 局部验证脚本或 README 片段。

#### 验收标准

1. 文档记录实际执行过的验证，不声明未执行的全量通过。
2. 测试失败或超时必须记录原因和下一步，不把旧报告当作当前结果。
3. 门禁命中能区分测试局部变量、DTO/adapter 边界和领域主路径问题。
