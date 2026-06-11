# CSP1D 下一轮交接

日期：2026-06-11

## 1. 总目标与当前判断

`ospf-kotlin-framework-csp1d` 的长期目标是完成一维分切通用内核的列生成生命周期与下游扩展能力闭环：public solve 入口可扩展，基础领域对象、决策对象、约束、目标、候选生成、pricing、主问题流程判断、shadow price、solution/KPI/render 都能通过 framework 通用抽象扩展；内置管线逐步接入 `CGPipeline` / `AbstractShadowPriceMap` 生命周期；`addColumns` 最终支持真实增量列生成，并让 `Csp1dColumnGeneration` 复用 context / pipeline / shadow price map 机制。

本次检查结论：

1. 当前 `daily.md` 已是更新后的交接文档，但另一个会话报告完成的是更新前旧计划中的“public 建模 pipeline 扩展入口”事项。
2. 代码检查确认旧计划主项已落地：`Csp1dModelingExtension<V>`、`Csp1dExtensionMode`、`Csp1dSolveConfig.extensions`、builder DSL、普通 MILP、列生成 LP master、final MILP、recovery/partial 路径传播，以及 `Csp1dExtraPipelineTest` 中的 public 入口测试均已存在。
3. 代码检查未发现结构化扩展包或 policy 体系：尚未出现 `domainPolicy`、`objectivePolicy`、`generationPolicy`、`pricingPolicy`、`flowPolicy`、`extractionPolicy` 或统一 `Csp1dExtension`/`Csp1dExtensionPolicy` 入口。
4. `addColumns` 仍是去重占位实现；`Csp1dCGPipeline` / `AbstractCsp1dShadowPriceMap` 已有类型桥接，但内置约束管线尚未真正接入 extractor/refresh 生命周期。

下一轮不要重复实现 public pipeline 透传；目标应直接切到“非 pipeline 的计算、判断、过滤、成本、候选接受、流程策略扩展能力”。

仍然不引入 POIT 包名、DTO、接口服务、公式语言、训练平台、缺陷、分段、位置约束、`unitBatch` 等下游或延后能力。POIT 的 same unit length、same width、宽差、材质兼容、设备兼容、成本修正、候选过滤等只作为扩展接口验证样例，不进入 framework 内置业务语义。

## 2. 已完成事项摘要

已完成事项只保留阶段级摘要，不保留逐类、逐断言、逐命令细节。

1. 已完成 CSP1D framework 基础领域模型、问题输入、物理量、泛型数值和 PO/DTO 无关主路径。
2. 已完成 material、cutting plan、produce、yield、length assignment、wasting minimization 的核心语义与 application public 使用面。
3. 已完成普通 MILP、列生成、LP shadow price、pricing、final MILP、Top-K、KPI/render、trace、recovery、warm start 和 partial/failed 主链路。
4. 已完成初始方案生成、统计、benchmark、缓存、剪枝、并行和 dominance 等阶段性能力。
5. 已完成 demo3 主路径迁移，示例不再维护手写 RMP/SP。
6. 已完成当前模型与 POIT CSP1D 边界复核，延后能力继续保留在下游适配或未来 material model 扩展中。
7. 已完成 application failure/partial 边界收口，异常安全、失败状态、LP failure trace、pricing 统计 trace 和 render KPI 已进入 public 输出口径。
8. 已完成 README/README_ch 对 public API、能力边界、failure/partial 语义、`LpInfeasible` 推断语义和 KPI key 的同步说明。
9. 已完成 POIT-CSP1D 与 Gantt/BPP3D 相关建模模式阅读，确认以 `MetaModel` / `AbstractLinearMetaModel` 为轴心的 framework 架构方向。
10. 已完成 CSP1D 建模注册改造：核心接口、Aggregation、Pipeline、`Csp1dProduceContext`、LP/MILP 模式区分和 `Csp1dMilpSolver` 瘦身已落地。
11. 已完成 builder 级与 public solve 级 fake 扩展约束测试，same unit length / same width 类约束可通过 pipeline 注入 MILP、LP、final MILP、recovery/partial 路径。
12. 已完成 `Csp1dShadowPriceKey`、`AbstractCsp1dShadowPriceMap`、`Csp1dCGPipeline` 类型别名和 `toShadowPriceMap()` 桥接。
13. 已修复 shadow price 泛型转换问题，LP 对偶值已改为基于样本值显式执行 `Flt64 -> V` 转换。
14. 已完成 public 建模 pipeline 扩展入口：`Csp1dModelingExtension<V>`、`Csp1dExtensionMode`、`Csp1dSolveConfig.extensions`、builder DSL、普通 MILP、列生成 LP、final MILP、recovery/partial 路径均已传播扩展配置。
15. 已完成 README/README_ch 建模扩展章节和 `addColumns` 预留接口说明。

## 3. 源码检查后的扩展缺口

### 3.1 Framework CSP1D 当前缺口

1. `Material.enabled(plan, machines)` 仍硬编码 material id、machine id、widthRange、machine.enabled 判断；需要可插拔的物料/设备/方案可行性 policy。
2. `WidthRange.canCut(productWidth)` 当前只按上界判断；需要可插拔的可切宽度策略，支持宽差、下界、步长、业务可切规则等。
3. `CuttingPlan.usedWidth/restWidth` 固定按 slice width * amount 与 material upperBound 计算；需要为余料代理、单位口径、业务宽度计算保留策略。
4. `DemandConstraintPipeline` 固定按 product id + unit 匹配贡献，并按有无 yield slack 切换 EQ/GE；需要 demand contribution、需求匹配、软硬约束选择的扩展点。
5. `MaterialConstraintPipeline`、`MachineConstraintPipeline` 固定按物料可用批次、设备批次、产能消耗建模；需要物料可用性、设备兼容、产能消耗、设备偏好/成本的扩展点。
6. `Csp1dProduceContext.setObjective` 固定基础 batch coefficient，再叠加 yield/waste/length objective；需要额外目标项、目标系数修正和目标构建上下文扩展点。
7. `Csp1dColumnGeneration` 仍硬编码 initial plan pool 去重、pricing objective config 映射、new plan 去重、终止判断和 final MILP 状态处理；需要候选过滤、去重策略、pricing 目标修正、partial/fallback 接受策略、终止策略扩展点。
8. generation/pricing 内部已有 `CuttingPlanConstraint` 作为生成约束扩展点，但 `GenerationCollector.record`、canonical 去重、dominance、length bound、width/knife pruning、template reuse、reduced cost benefit/cost/isImproving/sort 仍缺少 policy 注入点。
9. 内置管线仍主要是 `Pipeline<LinearMetaModel<Flt64>>`，尚未接入 `Csp1dCGPipeline` 的 `refresh()` / `extractor()` 生命周期。
10. `addColumns` 仍是去重占位实现，未在已有模型上增量注册列变量并刷新表达式。

### 3.2 POIT CSP1D 暴露出的扩展形态

1. 子问题 MILP 会按配置组合 `MaterialConstraint`、`ReducedCostMinimization` / `WasteMinimization`、`UnitBatchConflictConstraint`、`PositionAmountConstraint`、`OnSideConstraint`、`InMiddleConstraint`，说明扩展点需要覆盖子问题决策对象、额外中间值、约束和目标。
2. same unit length / same width 并不只是主问题约束，还涉及子问题 `Cut` 对象中的 on side / in middle 变量、中间值、位置数量约束和解后分析。
3. POIT 在解解析后还会判断 products 非空、有产出、`material.enabled(...)`、`partialEq` 去重，说明需要候选验收、业务可行性和等价判断扩展点。
4. POIT 主问题 pipeline 含 material compatibility、width diff、cutting plan amount/switch、multi width usage、material amount/switch、动态长度、not-same-product、costar ratio、capacity 等目标或约束，说明扩展点必须支持额外变量、中间值、软约束、目标成本和开关策略。
5. POIT schedule produce 还按日期、设备、产能组织约束和目标，说明 framework 扩展上下文要能暴露 material、machine、plan、quantity、usage variable 和建模阶段，而不是只给一个裸 `Pipeline`。
6. `unitBatch`、缺陷、分段、位置约束本身仍是延后能力，不进 framework 主路径；但它们证明 framework 需要能在子问题可行性、冲突判断、候选过滤和自定义决策对象上留入口。

## 4. 当前未闭环点

1. **P0 非 pipeline 扩展能力不足**：public pipeline 入口已贯穿 solve 主链路，但尚无统一扩展包承载可行性、贡献计算、目标系数、candidate filter、pricing cost、流程判断、solution/KPI/render enrich。
2. **P0 扩展上下文不够结构化**：`Csp1dModelingExtension` 当前只承载 `Pipeline<LinearMetaModel<Flt64>>`，下游若要访问 product/material/machine/plan/aggregation/quantity/modeling mode，仍需自己闭包捕获。
3. **P1 generation/pricing 缺少策略入口**：`CuttingPlanConstraint` 能做一部分剪枝，但 pricing reduced cost、objective cost、candidate accept、dedup、dominance、sort、template reuse 等仍是固定逻辑。
4. **P1 主流程判断缺少策略入口**：initial plan pool、deduplicatePlans、是否接受 partial、是否 fallback、终止原因映射、failure message 合并等仍由 application 硬编码。
5. **P1 内置管线未接入 CGPipeline 生命周期**：当前 shadow price 提取仍主要靠手动 constraint name map，尚未把内置约束纳入 `CGPipeline` extractor/refresh。
6. **P2 增量列生成未实现**：`addColumns` 当前只做去重，不更新已有模型。

## 5. 下一轮目标

下一轮强制目标：**CSP1D 扩展能力收口第一轮**。

完成后，下游应能通过 public framework API 和通用领域上下文扩展以下类型的行为，而无需修改 application solver 主流程：

1. 已有 modeling pipeline 入口继续可用，并能与新扩展包兼容。
2. material/product/machine/cutting plan 的可行性判断与派生计算可以通过 domain policy 扩展。
3. 主问题目标项或目标系数可以通过 objective policy 扩展。
4. generation/pricing 的候选过滤、reduced cost 成本修正和候选接受判断可以通过 policy 扩展。
5. application 主循环至少两个判断节点可以通过 flow policy 预留或最小接入。
6. solution/KPI/render/shadow price extractor 有明确结构位置，即使下一轮不全部实现。

下一轮不是要把 POIT 功能搬进 framework，而是要证明 framework 的通用抽象足以承载 POIT 类 same unit length、same width、宽差、材质兼容、设备兼容、业务成本和候选验收规则。

## 6. 下一轮事项

### 6.1 必做：基线审计与过时描述清理

1. 记录 `git status --short`，确认只处理 CSP1D 范围改动，不回滚其他会话内容。
2. 复核 `Csp1dSolveConfig.extensions`、`Csp1dProblemBuilder.extension(...)`、`extensionPipeline(...)`、`Csp1dMilpSolver.solve/solveLP`、`Csp1dMilp`、`Csp1dColumnGeneration`、`Csp1dRecovery`、`Csp1dExtraPipelineTest`。
3. 确认 public pipeline 扩展入口已覆盖普通 MILP、LP master、final MILP、recovery/partial；只补发现的遗漏，不重新设计该入口。
4. README/README_ch/daily.md 不再把 public modeling pipeline 入口描述成待实现能力。

### 6.2 必做：结构化扩展上下文

新增 CSP1D 扩展上下文类型，建议放在 produce/material/generation 的 domain model 层，避免 application solver 承担业务细节。

1. 建模上下文需要暴露：
   - `Csp1dModelingMode` 和是否 final MILP。
   - `ProduceAggregation` 以及 cutting plan usage variables。
   - demands、materials、machines、cutting plans。
   - yield/waste/length aggregation 的只读引用。
   - `LinearMetaModel<Flt64>` registration context。
2. 领域计算上下文需要暴露：
   - product、demand、material、machine、cutting plan、slice。
   - quantity/unit、width/length/capacity、demand contribution。
   - 当前 arithmetic 和 `V : RealNumber<V>` 样本值。
3. generation/pricing 上下文需要暴露：
   - generation input、existing plans、shadow price map、objective config。
   - candidate plan、canonical key、reduced cost benefit/cost、statistics 计数口径。
4. application flow 上下文需要暴露：
   - iteration、current plans、new plans、LP/final MILP result、termination reason、allowPartialSolution、recovery mode。
5. 所有上下文保持泛型化与物理量化，对外不暴露裸 `Double` 作为业务入口。

### 6.3 必做：统一扩展包接口

在现有 `Csp1dModelingExtension<V>` 基础上增加更完整的扩展承载结构，命名可采用 `Csp1dExtension<V>`、`Csp1dExtensionSet<V>` 或等价名称。默认实现必须为空行为，保持现有 API 兼容。

至少预留以下接口槽位：

1. `modelingExtensions`：继续承载已落地的 `Csp1dModelingExtension<V>`，支持 MILP/LP/final MILP 模式过滤。
2. `domainPolicy`：物料/设备/宽度/方案可行性、rest width、capacity consumption、demand contribution 等计算判断。
3. `objectivePolicy`：基础 batch cost、material cost、waste/yield/length 以外的业务目标项和系数修正。
4. `generationPolicy`：候选生成前后过滤、可行性、canonical/dominance、template reuse、统计记录。
5. `pricingPolicy`：dual benefit、objective cost、isImproving、candidate sort、max generated plan accept。
6. `flowPolicy`：initial plan pool、deduplicatePlans、termination、partial/fallback 接受判断。
7. `extractionPolicy`：solution extractor、KPI/render enricher、shadow price extractor 的后续接口位置。

兼容要求：

1. 旧的 `Csp1dSolveConfig.extensions` 继续可用。
2. 若新增 `extensionSet` 或 `policies` 字段，默认值必须保持现有行为。
3. builder DSL 需要能同时追加旧 modeling extension 和新 policy。

### 6.4 必做：domain policy 最小落地

优先选择不需要大规模重写模型的计算/判断点落地，证明基础领域对象可扩展。

1. 为 `Material.enabled(plan, machines)` 或调用侧提供可注入 feasibility policy，默认行为保持当前逻辑。
2. 为 `WidthRange.canCut(...)` 调用侧提供 width feasibility policy，默认行为保持当前上界判断。
3. 为 `CuttingPlan` 的 used/rest width 相关调用提供计算策略入口或 adapter，默认保持当前计算。
4. 增加 fake policy 测试：
   - 宽差或设备兼容类 fake policy 能拒绝某个候选方案。
   - 默认 policy 下 generation canonical 结果不变。
5. 如果直接改 `Material`/`WidthRange` 会造成 API 震荡，可先在 generation/application 调用侧接入 policy，不强制修改 data class 构造器。

### 6.5 必做：objective/modeling 扩展收口

1. 保留现有 `Csp1dModelingExtension` 兼容入口，并让新的扩展包能包装或生成现有 modeling pipeline。
2. 新增 context-aware modeling extension 或 pipeline factory，使扩展 pipeline 能访问结构化 `Csp1dModelingContext`。
3. 目标扩展至少支持追加 objective monomials 或修正基础 batch coefficient。
4. 增加 fake objective 测试，证明下游能追加一个业务成本项且普通 MILP/final MILP 路径生效。
5. 保持 LP 模式语义：除非扩展明确声明适用于 LP，否则不要影响 shadow price 提取口径。

### 6.6 必做：generation/pricing policy 最小落地

1. 在 `CuttingPlanGenerationInput` 或 generator config 中接入 generation policy 列表，默认空策略不改变行为。
2. 在 `GenerationCollector.record` 或调用侧接入 candidate accept/reject policy，并记录被扩展拒绝的统计或至少测试可观察结果。
3. 在 `ReducedCostPricingGenerator` 接入 pricing policy：
   - 可修正 objective cost。
   - 可拒绝 candidate。
   - 可调整候选排序或 tie-break。
4. 保持 `CuttingPlanConstraint` 现有能力，不与新 policy 重复冲突；它仍是生成阶段硬约束/剪枝入口。
5. 增加 fake pricing 测试：
   - fake policy 增加某个 material cost 后 candidate 不再 improving。
   - fake filter 拒绝某类 plan。
   - 默认空 policy 下既有 pricing 测试结果不变。

### 6.7 必做：application flow policy 最小接入

1. 在 `Csp1dSolveConfig` 或列生成 config 中预留 flow policy。
2. 最小接入以下节点中的至少两个：
   - initial plan pool 过滤/排序。
   - `deduplicatePlans` 等价判断。
   - pricing 产出为空或全重复时的 termination message。
   - final MILP 失败时是否接受 partial。
   - recovery/fallback 是否启用。
3. 默认策略保持当前语义，failure/partial、`LpInfeasible`、`LpSolveFailed`、trace/KPI/render 不变。
4. 增加 fake flow policy 测试，证明 public config 能影响候选接受或 partial/fallback 判断。

### 6.8 必做：POIT 扩展样例验证

不引入 POIT 代码，只用 framework fake 对象验证承载能力。

1. same unit length / same width：继续用 fake modeling pipeline 验证主问题扩展。
2. 宽差：用 fake domain/generation policy 验证候选过滤或可行性判断。
3. 材质兼容/设备兼容：用 fake feasibility/objective policy 验证拒绝或成本修正。
4. 子问题后处理：用 fake generation/pricing policy 验证“解后验收 + 去重/等价判断”可承载。
5. 主问题业务成本：用 fake objective policy 验证目标项可追加。

### 6.9 条件加做：CGPipeline / shadow price 样板

仅在 6.1 至 6.8 稳定后执行。

1. 阅读 framework 通用 `Pipeline`、`CGPipeline`、`ShadowPriceMap`，以及 Gantt/BPP3D 的 extractor/refresh 用法。
2. 选择一个约束作为样板，优先 demand constraint。
3. 将样板管线对偶值写入 `AbstractCsp1dShadowPriceMap`，再通过 `toShadowPriceMap(vSample)` 转为 pricing 使用的 `ShadowPriceMap<V>`。
4. 保持 LP 对偶值到 `V` 显式转换，禁止恢复 `dualValue as? V`。
5. 增加专项测试覆盖 extractor/refresh/map 转换。
6. 如果样板引发大范围重构，停止条件加做，把结果记录为后续阶段。

### 6.10 后续阶段记录

以下不作为下一轮强制执行：

1. 真实增量 `addColumns`：新增列变量、刷新需求/物料/设备表达式、刷新目标项、失败回滚。
2. `Csp1dColumnGeneration` 全面切换到增量 master。
3. 全量内置 pipeline 改造成 `Csp1dCGPipeline`。
4. 缺陷、分段、位置约束、`unitBatch`、公式语言、训练平台。

## 7. 推荐执行计划

1. **现状确认**
   - 跑 `git status --short`。
   - 搜索 `Csp1dModelingExtension`、`extensions`、`extensionPipeline`、`domainPolicy`、`objectivePolicy`、`generationPolicy`、`pricingPolicy`、`flowPolicy`、`addColumns`、`dualValue as? V`、`Csp1dCGPipeline`。
   - 只补 public pipeline 入口的实际遗漏；不要重复做已完成的入口传播。

2. **设计扩展上下文**
   - 先定义只读 context 类型。
   - 明确哪些 context 面向 modeling，哪些面向 generation/pricing，哪些面向 flow。
   - 先通过编译和最小测试确认 context 不破坏现有 API。

3. **设计统一扩展包**
   - 保留 `Csp1dModelingExtension`。
   - 新增扩展包默认空实现。
   - 在 `Csp1dSolveConfig` 与 builder 中接入新扩展包。
   - 确认旧 DSL 与旧构造调用不破坏。

4. **接入低风险 domain/objective policy**
   - 先做 feasibility/width policy。
   - 再做 objective policy。
   - 每接入一类都补 fake 测试，确认默认行为不变。

5. **接入 generation/pricing policy**
   - 先接 candidate filter。
   - 再接 pricing objective cost 修正。
   - 最后接排序或接受判断。
   - 对照 generation canonical benchmark，确认默认空 policy 不改变结果。

6. **接入 flow policy**
   - 只选两个节点做最小闭环，优先 initial plan pool 和 deduplicatePlans。
   - 不重写列生成主循环。
   - 确认 failure/partial 与 LP failure 语义不变。

7. **文档与验证**
   - 更新 README/README_ch 的扩展章节。
   - 更新 `daily.md` 实际结果。
   - 跑新增专项测试、application acceptance、generation 关键测试和门禁搜索。

8. **条件加做**
   - 若前面稳定，再做一个 `CGPipeline` shadow price 样板。
   - 未做则明确记录为后续，不阻塞扩展能力收口。

## 8. 修改清单

下一轮允许修改 CSP1D 范围内以下文件或同目录新增类型；避免触碰无关模块。

1. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/model/Csp1dProblem.kt`
2. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/model/Csp1dProblemBuilder.kt`
3. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dMilp.kt`
4. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dMilpSolver.kt`
5. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dColumnGeneration.kt`
6. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dRecovery.kt`
7. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dProduceContext.kt`
8. `ospf-kotlin-framework-csp1d/csp1d-application/src/test/.../application/service`
9. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context/src/main/.../domain/material/model`
10. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/.../domain/cutting_plan_generation`
11. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/.../domain/cutting_plan_generation/service`
12. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce/model`
13. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce/service/pipeline`
14. `ospf-kotlin-framework-csp1d/csp1d-domain-yield-context/src/main/...`
15. `ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context/src/main/...`
16. `ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context/src/main/...`
17. `ospf-kotlin-framework-csp1d/README.md`
18. `ospf-kotlin-framework-csp1d/README_ch.md`
19. `ospf-kotlin-framework-csp1d/daily.md`
20. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

如条件加做 `CGPipeline` 样板需要修改更多 domain pipeline，可限于 CSP1D 子模块内扩展。不要修改 `ospf-kotlin-framework` 通用契约，除非先确认不会影响 Gantt/BPP3D 并扩大验证范围。

## 9. 验收标准

### 9.1 强制验收

1. public pipeline 扩展入口经审计确认覆盖普通 MILP、LP master、final MILP、recovery/partial，默认空配置兼容。
2. 新增或调整的扩展包能承载 modeling extension、domain policy、objective policy、generation policy、pricing policy、flow policy 和 extraction 预留位置。
3. 扩展上下文能提供 product、demand、material、machine、cutting plan、slice、quantity/unit、modeling mode、model、produce aggregation 等通用信息。
4. 至少一个 domain 计算/判断扩展点真实生效，并有 fake 测试。
5. 至少一个 objective 扩展点真实生效，并有 fake 测试。
6. 至少一个 generation 或 pricing 扩展点真实生效，并有 fake 测试。
7. 至少两个 application flow 判断节点完成预留或最小接入，并有 fake 测试覆盖其中一个。
8. fake POIT 类样例覆盖 same unit length / same width、宽差、材质或设备兼容、业务成本、候选过滤中的至少四类。
9. 既有 `Csp1dProblem`、`Csp1dMilp`、`Csp1dColumnGeneration`、`Csp1dSchedule`、`Csp1dRecovery` 使用方式不破坏。
10. failure/partial、`LpInfeasible`、`LpSolveFailed`、trace/KPI/render public 语义不变。
11. `V : RealNumber<V>` public 泛型边界保持稳定，不新增面向业务侧的 `Flt64` 唯一入口。
12. 宽度、长度、需求、产出、余料、容量等领域量继续使用 `Quantity<V>` 或明确单位。
13. LP 对偶值到 `V` 的转换必须显式，禁止恢复 `dualValue as? V`。
14. README/README_ch 与实际 API 一致，说明 framework 不内置 POIT 语义，但提供足够扩展入口。
15. demo3 不恢复手写 RMP/SP。
16. 不引入 POIT 包名、DTO、公式语言、训练平台、缺陷、分段、位置约束、`unitBatch` 到 framework 主路径。

### 9.2 条件加做验收

若执行 `CGPipeline` / shadow price 样板，则必须满足：

1. 至少一个 shadow price 相关约束管线完成 extractor/refresh 样板或等价桥接。
2. `AbstractCsp1dShadowPriceMap` 能承载该约束的 LP 对偶值。
3. `toShadowPriceMap()` 能把框架 map 转换为 pricing 使用的轻量 `ShadowPriceMap<V>`。
4. 有专项测试覆盖 extractor/refresh/map 转换。
5. 未执行本节时，应在 `daily.md` 记录为后续阶段任务，不影响 9.1 验收。

### 9.3 非本轮验收

以下内容不作为下一轮强制验收：

1. `addColumns` 在已有模型上真实增量添加列变量。
2. 需求、物料、设备和目标表达式的增量刷新。
3. `Csp1dColumnGeneration` 全面切换到增量 master。
4. 全量内置 pipeline 改造成 `Csp1dCGPipeline`。

## 10. 最小验证集

执行后必须记录实际结果，不复用历史报告。

1. 新增或更新的扩展入口专项测试。
2. application acceptance，覆盖 MILP、列生成、Top-K、KPI/render、recovery、warm start、partial solution 和扩展入口。
3. generation/pricing 目标测试，至少确认默认空 policy 下 canonical 结果集合不变。
4. material、yield、length assignment、wasting minimization 子模块测试。
5. 若执行条件加做，运行新增 CGPipeline/shadow price 专项测试。
6. Gurobi profile：至少执行 `-Pgurobi-cg-test -DskipTests test-compile`；当前 Gurobi 10 环境可用时执行真实 solver smoke。
7. 门禁搜索：
   - `rg -n "com\\.poit|framework\\.bpp3d" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "rollDemand|weightDemand|sheetDemand" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
   - `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "candidatePlans" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "println\\(" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "DefectCostar|defect|缺陷|segment|分段|positionConstraint|位置约束|unitBatch|formula|公式语言|training|训练平台|history sample|历史样本" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/target/**" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
   - `rg -n "dualValue as\\? V" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `git diff --check -- ospf-kotlin-framework-csp1d`

## 11. 交接提示

1. 下一会话不要重复实现 public pipeline 扩展入口；先审计确认已落地，再补 policy/strategy 缺口。
2. 下一会话先读 `Csp1dModelContext`、`Csp1dProduceContext`、`Csp1dMilpSolver`、`Csp1dColumnGeneration`、`CuttingPlanGenerationContext`、`GenerationCollector`、`Material`、`WidthRange`、`CuttingPlan`、`DemandConstraintPipeline`、`MachineConstraintPipeline` 和 `Csp1dExtraPipelineTest`。
3. POIT 参考代码重点看 `cutting_plan_generation/service/MILP.kt`、`OnSideConstraint.kt`、`InMiddleConstraint.kt`、`MaterialConstraint.kt`、`UnitBatchConflictConstraint.kt`、`ProducePipelineListGenerator.kt`、`SchedulePipelineListGenerator.kt`，只提炼通用扩展形态，不搬入 POIT 语义。
4. 每一步都要保持 `V : RealNumber<V>`、`Quantity<V>` 和显式 `Flt64 -> V` 转换边界。
5. 所有计算、判断计算都尽可能留下扩展点；默认实现可以是当前逻辑，但不要把可变业务规则继续硬编码在 application solver 编排层。
6. 工作区可能存在非 CSP1D 脏改，不得回滚或混入无关模块。
