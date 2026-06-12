# CSP1D 下一轮交接

日期：2026-06-12

## 1. 总目标

`ospf-kotlin-framework-csp1d` 的长期目标是完成一维分切通用内核的列生成生命周期与下游扩展能力闭环：public solve 入口可扩展，基础领域对象、决策对象、约束、目标、候选生成、pricing、主问题流程判断、shadow price、solution/KPI/render 都能通过 framework 通用抽象扩展；内置管线逐步接入 `CGPipeline` / `AbstractShadowPriceMap` 生命周期；`addColumns` 最终支持真实增量列生成，并让 `Csp1dColumnGeneration` 复用 context / pipeline / shadow price map 机制。

仍然不引入 POIT 包名、DTO、接口服务、公式语言、训练平台、缺陷、分段、位置约束、`unitBatch` 等下游或延后能力。POIT 的 same unit length、same width、宽差、材质兼容、设备兼容、成本修正、候选过滤、候选验收等只作为扩展接口验证样例，不进入 framework 内置业务语义。

## 2. 当前判断

当前已完成 public 扩展能力的关键收口，但尚未达到长期总目标。

已经收口的主线：

1. public modeling extension 已覆盖普通 MILP、列生成 LP、列生成 final MILP、recovery/partial 相关路径。
2. `Csp1dModelingContext`、`Csp1dDomainCalculationContext`、`Csp1dPlanJudgmentContext` 等结构化上下文已落地。
3. `Csp1dExtensionSet<V>` 已承载 modeling、domain、objective、generation、pricing、flow 等扩展槽位。
4. domain/objective/generation/pricing/flow policy 已有最小生效路径。
5. context-aware modeling pipeline 已可通过 public builder DSL 注入。
6. `widthFeasibilityCheck` 已支持当前 `productWidth`，并通过 `overridesWidthFeasibility` 防止非宽度 policy 意外绕过 `canCut`。
7. fake POIT 类样例已覆盖 same unit length、same width、宽差、设备兼容、业务成本、候选过滤等方向。

尚未闭环的主线：

1. `addColumns` 仍不是真实增量列生成。
2. 内置约束管线尚未系统接入 `Csp1dCGPipeline` 的 `refresh()` / `extractor()` 生命周期。
3. `Csp1dColumnGeneration` 仍保留较多手动 master LP、shadow price、new plan、termination、final MILP 编排逻辑。
4. extraction policy 尚未最小落地，solution/KPI/render 扩展仍未闭环。
5. flow policy 主要覆盖 initial plan pool 和 deduplicate，partial/fallback/termination 等主流程判断仍需策略化。

下一轮应尽量用一轮完成“列生成生命周期与输出扩展闭环”，不要继续拆成零散小轮。

## 3. 已完成事项摘要

已完成事项只保留阶段级摘要，不保留逐类、逐断言、逐命令细节。

1. 已完成 CSP1D framework 基础领域模型、问题输入、物理量、泛型数值和 PO/DTO 无关主路径。
2. 已完成 material、cutting plan、produce、yield、length assignment、wasting minimization 的核心语义与 application public 使用面。
3. 已完成普通 MILP、列生成、LP shadow price、pricing、final MILP、Top-K、KPI/render、trace、recovery、warm start 和 partial/failed 主链路。
4. 已完成初始方案生成、统计、benchmark、缓存、剪枝、并行和 dominance 等阶段性能力。
5. 已完成 demo3 主路径迁移，示例不再维护手写 RMP/SP。
6. 已完成 POIT CSP1D 能力边界复核，延后能力继续保留在下游适配或未来通用 material/generation model 扩展中。
7. 已完成 application failure/partial 边界收口，异常安全、失败状态、LP failure trace、pricing 统计 trace 和 render KPI 已进入 public 输出口径。
8. 已完成 README/README_ch 对 public API、能力边界、failure/partial 语义、`LpInfeasible` 推断语义和 KPI key 的同步说明。
9. 已完成 Gantt/BPP3D/POIT 相关建模模式阅读，确认以 `MetaModel` / `AbstractLinearMetaModel` 为轴心的 framework 架构方向。
10. 已完成 CSP1D 建模注册改造，核心接口、Aggregation、Pipeline、`Csp1dProduceContext`、LP/MILP 模式区分和 `Csp1dMilpSolver` 瘦身已落地。
11. 已完成 public modeling extension、context-aware pipeline、统一扩展包和 builder DSL。
12. 已完成 domain/objective/generation/pricing/flow policy 的最小落地与 fake 扩展样例验证。
13. 已完成 shadow price key/map 类型桥接与 `Flt64 -> V` 显式转换修复。
14. 已完成 `widthFeasibilityCheck`、`enabledWithoutWidthCheck` 和 `overridesWidthFeasibility` 的宽度扩展收口与回归测试。
15. 已完成门禁搜索和格式检查的阶段性通过记录；下一轮仍必须重新执行并记录实际结果。
16. 已完成列生成生命周期与输出扩展闭环：`Csp1dFlowContext` + 上下文感知 `Csp1dFlowPolicy`（filterInitialPlans、isEquivalent、shouldStopIteration、selectTermination、acceptPartial、allowRecoveryFallback）落地；`Csp1dExtractionPolicy` 接入 solution enrichment，支持自定义 KPI/details 写入；`Csp1dShadowPriceLifecycle` 统一影子价格注册与提取，`LpResult` 同时承载 lightweight `ShadowPriceMap<V>` 和 framework `AbstractCsp1dShadowPriceMap`；`addColumns` 明确为 rebuild-compatible lifecycle 并在文档中说明边界；`Csp1dColumnGeneration` 主循环全面使用 context/pipeline/policy/extraction 机制；`Csp1dGenerationStrategy` 扩展 `canonicalKeyFor`、`acceptDominance`；`Csp1dPricingPolicy` 扩展 `modifyBenefit`、`isImproving`；`ReducedCostPricingGenerator` 接入 benefit modifier 和 custom isImproving judge；13 项 lifecycle 专项测试通过；README/README_ch 同步说明 extension set、flow/extraction policy、shadow price lifecycle 和 addColumns 能力边界；门禁搜索全部通过。

## 4. 下一轮目标

下一轮强制目标：**CSP1D 列生成生命周期与输出扩展闭环**。

完成后应达到以下状态：

1. `Csp1dColumnGeneration` 的 LP master、shadow price、pricing、new columns、final MILP、partial/fallback/termination、trace/KPI/render 尽量通过 context / pipeline / policy 机制组织。
2. shadow price 提取不再依赖零散手动 constraint name map，至少需求、物料、设备三类主 LP 约束进入统一 map 或 `CGPipeline` 样板机制。
3. `addColumns` 不再是去重占位；若底层 `MetaModel` 确实无法原地扩展变量，必须实现清晰的 rebuild 型增量替代并在接口/文档中说明，不得继续把占位实现标为完成。
4. extraction policy 至少覆盖 solution enrichment 与 KPI/render enrichment，一个 public 扩展能把自定义输出写入 solution details 或 render KPI。
5. flow policy 至少覆盖 partial 接受、fallback 启用、termination message 或 iteration stop 中的两个以上节点。
6. 默认空扩展必须保持既有求解语义、generation canonical 结果、failure/partial、`LpInfeasible`、`LpSolveFailed`、trace/KPI/render 口径不变。

## 5. 下一轮执行包

### 5.1 基线审计与保护

1. 记录 `git status --short`，确认只处理 CSP1D 范围改动；不得回滚或混入其他模块脏改。
2. 复核当前 public API：`Csp1dSolveConfig`、`Csp1dSolveConfigBuilder`、`Csp1dExtensionSet`、`Csp1dModelingExtension`、`Csp1dDomainPolicy`、`Csp1dObjectivePolicy`、`Csp1dGenerationStrategy`、`Csp1dPricingPolicy`、`Csp1dFlowPolicy`。
3. 复核当前列生成主流程：initial plan pool、LP solve、shadow price extraction、pricing input、deduplicate、termination、final MILP、solution enrichment。
4. 先补遗漏测试，不先重构；确认默认空扩展行为有基线测试可比较。

### 5.2 Flow Policy 扩展闭环

拓宽 `Csp1dFlowPolicy`，默认实现必须保持当前行为。

建议新增或等价实现以下上下文与接口：

1. `Csp1dFlowContext<V>`：暴露 problem、solve config、iteration、current plans、new plans、LP result、pricing report、termination reason、final MILP result、allowPartialSolution、recovery mode。
2. `filterInitialPlans(context, plans)`：兼容现有 `filterInitialPlans(plans)`。
3. `isEquivalent(context, existing, candidate)`：兼容现有 `isEquivalent(existing, candidate)`。
4. `shouldStopIteration(context)`：用于 iteration limit 之外的业务停止条件。
5. `selectTermination(context, defaultReason, defaultMessage)`：允许扩展 termination reason/message，但默认不变。
6. `acceptPartial(context, defaultDecision)`：final MILP 失败时是否接受 partial。
7. `allowRecoveryFallback(context, defaultDecision)`：recovery/fallback 是否启用。

接入节点至少包括：

1. initial plan pool 过滤或排序。
2. deduplicate 等价判断。
3. pricing 无新增、全重复、iteration limit、LP failure 的 termination message 中至少一个。
4. final MILP 失败后的 partial 接受判断。
5. recovery fallback 启用判断。

### 5.3 Extraction Policy 与输出扩展闭环

新增 `Csp1dExtractionPolicy<V>` 或等价接口，并加入 `Csp1dExtensionSet<V>`。

最小能力：

1. solution enrichment 前后可访问 problem、produce、generated/current plans、trace、final MILP status、pricing statistics。
2. 可向 `Csp1dSolution.details`、render KPI、trace 扩展字段或等价 output map 写入自定义信息。
3. 默认空策略不改变现有 solution、KPI、render 输出。
4. 如果现有 `Csp1dSolution` 结构不适合扩展，不要大改模型；优先通过已有 details/render KPI 或新增稳定扩展字段承载。
5. README/README_ch 需要说明 extraction policy 能扩展输出，但 framework 不承诺内置 POIT 业务字段。

测试要求：

1. fake extraction policy 写入一个自定义 KPI，并在普通 MILP 和列生成路径可观察。
2. 默认空 policy 下既有 KPI key 不变。
3. failure/partial 路径下 extraction policy 不导致异常逃逸。

### 5.4 CGPipeline 与 Shadow Price 生命周期收口

目标是让 LP shadow price 提取进入统一抽象，减少 application 手工拼 map。

执行策略：

1. 阅读通用 `Pipeline`、`CGPipeline`、`ShadowPriceMap`、`AbstractShadowPriceMap`，以及 Gantt/BPP3D 已有用法。
2. 优先处理 shadow-price-producing 主约束：需求约束、物料可用约束、设备批次/容量约束。
3. 为这些管线建立 `Csp1dCGPipeline` 或等价 wrapper，使其能注册约束、刷新表达式、提取 dual value。
4. LP 对偶值必须通过样本值显式转换为 `V`，禁止恢复 `dualValue as? V`。
5. `Csp1dMilpSolver.solveLP()` 应优先从 `AbstractCsp1dShadowPriceMap` 或统一 extractor 输出 pricing 使用的 `ShadowPriceMap<V>`。
6. 原有 constraint name map 可以短期保留为兼容 fallback，但不能再作为唯一主路径。
7. 选择清晰迁移边界，避免一次性重写所有 yield/waste/length 非 LP 约束。

测试要求：

1. 需求约束 dual 能进入统一 shadow price map。
2. 物料/设备相关 dual 能进入统一 shadow price map，或明确记录未迁移原因。
3. `toShadowPriceMap(vSample)` 转换结果与原 pricing 语义一致。
4. `LpInfeasible`、`LpSolveFailed` 语义不变。

### 5.5 `addColumns` 真实增量列生成

不得继续把 `addColumns` 保持为去重占位。

优先目标：

1. `Csp1dProduceContext.addColumns()` 能把新增 cutting plans 纳入 context 状态。
2. 新增 plan usage variable 能注册到已有 model，或通过明确的 rebuild master 实现等价增量效果。
3. 需求、物料、设备约束表达式和基础目标项能随新增列刷新。
4. objective policy、modeling extension、flow policy 对新增列保持一致行为。
5. add columns 失败时不得污染 context 状态；需要有回滚或 rebuild 失败保护。

如果底层变量容器不支持原地扩展：

1. 明确实现 `rebuildWithColumns` 或等价机制。
2. application 层可以先使用 rebuild 型 master，但 public lifecycle 不能继续称为真实原地增量。
3. 在 README/README_ch 和 daily.md 说明当前是 rebuild-compatible lifecycle，后续再升级为 in-place incremental。

测试要求：

1. addColumns 后 plan count 增加。
2. 新增列参与需求、物料、设备约束。
3. 新增列参与目标函数。
4. 重复列不重复添加。
5. 失败路径不污染已有列集合。

### 5.6 Column Generation 主循环复用收口

在 5.2 至 5.5 基础上收敛 `Csp1dColumnGeneration`。

1. initial LP master 应通过 context/pipeline 注册。
2. 每轮新增 columns 优先走 `context.addColumns()` 或 rebuild-compatible lifecycle。
3. shadow price 优先走 context/pipeline extractor。
4. pricing input 使用统一 shadow price map 和 pricing policy。
5. termination、partial、fallback、deduplicate 走 flow policy。
6. final MILP 使用与普通 MILP 一致的 context-aware modeling extension 和 objective/extraction policy。
7. trace、KPI、render 统一经过 enrichment/extraction policy。
8. 保持当前 public 状态枚举与 failure message 兼容，除非文档同步说明。

### 5.7 Generation/Pricing 扩展补强

在已落地的 candidate filter 和 pricing cost modifier 基础上补齐更可变的判断节点。

建议覆盖：

1. candidate canonical key 或等价判断扩展。
2. dominance accept/reject 或 tie-break 扩展。
3. reduced cost benefit 修正。
4. objective cost 修正。
5. `isImproving` 判断扩展。
6. candidate sort/tie-break 扩展。
7. generation statistics 中记录扩展拒绝数量。

默认空 policy 下 generation canonical 结果必须不变。

### 5.8 文档、示例与交接

1. README/README_ch 同步说明：
   - extension set 总览。
   - flow policy。
   - extraction policy。
   - shadow price lifecycle。
   - addColumns 当前真实能力边界。
2. demo3 不恢复手写 RMP/SP。
3. daily.md 在完成后重新压缩已完成事项，下一轮只保留未闭环项。
4. 不把 POIT 业务语义写成 framework 内置语义。

## 6. 修改清单

下一轮允许修改 CSP1D 范围内以下文件或同目录新增类型。避免触碰无关模块。

1. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/model/Csp1dProblem.kt`
2. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/model/Csp1dProblemBuilder.kt`
3. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/model/Csp1dSolution.kt`
4. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dMilp.kt`
5. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dMilpSolver.kt`
6. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dColumnGeneration.kt`
7. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dRecovery.kt`
8. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dProduceContext.kt`
9. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dSolutionEnrichment.kt`
10. `ospf-kotlin-framework-csp1d/csp1d-application/src/test/.../application/service`
11. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context/src/main/.../domain/material/model`
12. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context/src/test/.../domain/material/model`
13. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/.../domain/cutting_plan_generation`
14. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/.../domain/cutting_plan_generation/model`
15. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/.../domain/cutting_plan_generation/service`
16. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce`
17. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce/model`
18. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce/service/pipeline`
19. `ospf-kotlin-framework-csp1d/csp1d-domain-yield-context/src/main/...`
20. `ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context/src/main/...`
21. `ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context/src/main/...`
22. `ospf-kotlin-framework-csp1d/README.md`
23. `ospf-kotlin-framework-csp1d/README_ch.md`
24. `ospf-kotlin-framework-csp1d/daily.md`
25. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

如确实需要修改 `ospf-kotlin-framework` 通用契约，必须先确认 Gantt/BPP3D 影响并扩大验证范围；默认不要改通用契约。

## 7. 验收清单

### 7.1 功能验收

1. public modeling extension、context-aware pipeline、domain/objective/generation/pricing/flow/extraction policy 均能通过 public config 或 builder 注入。
2. `Csp1dExtensionSet` 默认空配置不改变既有行为。
3. 非宽度 policy 不绕过 `canCut`；声明 `overridesWidthFeasibility = true` 的 policy 能放宽或收窄宽度判断。
4. flow policy 至少覆盖 initial plan pool、deduplicate、partial 接受、fallback 启用、termination message 中的三个节点。
5. extraction policy 至少能写入一个自定义 solution detail 或 render KPI，并覆盖普通 MILP、列生成、failure/partial 路径。
6. shadow-price-producing 主约束至少需求、物料、设备三类进入统一 extractor/map 或明确迁移边界。
7. LP shadow price 到 `V` 的转换保持显式，禁止 `dualValue as? V`。
8. `addColumns` 不再是单纯去重占位；若采用 rebuild-compatible lifecycle，文档和测试必须清晰说明。
9. `Csp1dColumnGeneration` 使用新的 context/pipeline/policy/extraction 能力，且 public 输出语义不回退。
10. fake POIT 类样例至少覆盖 same unit length、same width、宽差、设备或材质兼容、业务成本、候选过滤、候选验收、输出扩展中的六类。

### 7.2 行为兼容验收

1. 既有 `Csp1dProblem`、`Csp1dMilp`、`Csp1dColumnGeneration`、`Csp1dSchedule`、`Csp1dRecovery` 使用方式不破坏。
2. failure/partial、`LpInfeasible`、`LpSolveFailed`、trace/KPI/render public 语义不变。
3. `V : RealNumber<V>` public 泛型边界保持稳定，不新增面向业务侧的 `Flt64` 唯一入口。
4. 宽度、长度、需求、产出、余料、容量等领域量继续使用 `Quantity<V>` 或明确单位。
5. README/README_ch 与实际 API 一致，说明 framework 不内置 POIT 语义，但提供足够扩展入口。
6. demo3 不恢复手写 RMP/SP。
7. 不引入 POIT 包名、DTO、公式语言、训练平台、缺陷、分段、位置约束、`unitBatch` 到 framework 主路径。

### 7.3 测试验收

下一轮必须记录实际命令与结果，不复用历史报告。

1. 新增或更新的 flow/extraction/addColumns/CGPipeline/shadow price 专项测试通过。
2. application acceptance 通过，覆盖 MILP、列生成、Top-K、KPI/render、recovery、warm start、partial solution 和扩展入口。
3. generation/pricing 关键测试通过，默认空 policy 下 canonical 结果集合不变。
4. material、yield、length assignment、wasting minimization 子模块测试通过。
5. Gurobi profile 至少执行 `-Pgurobi-cg-test -DskipTests test-compile`；当前 Gurobi 10 环境可用时执行真实 solver smoke。
6. 如果修改通用 framework 契约，必须补跑受影响的 Gantt/BPP3D 相关测试。

### 7.4 门禁搜索

必须全部通过：

1. `rg -n "com\\.poit|framework\\.bpp3d" ospf-kotlin-framework-csp1d -g "*.kt"`
2. `rg -n "rollDemand|weightDemand|sheetDemand" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
3. `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"`
4. `rg -n "candidatePlans" ospf-kotlin-framework-csp1d -g "*.kt"`
5. `rg -n "println\\(" ospf-kotlin-framework-csp1d -g "*.kt"`
6. `rg -n "DefectCostar|defect|缺陷|segment|分段|positionConstraint|位置约束|unitBatch|formula|公式语言|training|训练平台|history sample|历史样本" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/target/**" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
7. `rg -n "dualValue as\\? V" ospf-kotlin-framework-csp1d -g "*.kt"`
8. `git diff --check -- ospf-kotlin-framework-csp1d`

## 8. 非本轮目标

以下内容仍不强制进入下一轮，除非前述目标已完成且验证稳定：

1. 缺陷、分段、位置约束、`unitBatch`、公式语言、训练平台。
2. POIT DTO、接口服务、租户/心跳/训练平台协议。
3. 全量重写 generation/pricing 算法。
4. 在没有明确验证计划的情况下修改 `ospf-kotlin-framework` 通用契约。

## 9. 交接提示

1. 下一会话先读 `Csp1dModelContext`、`Csp1dProduceContext`、`Csp1dMilpSolver`、`Csp1dColumnGeneration`、`Csp1dSolutionEnrichment`、`CuttingPlanGenerationContext`、`GenerationCollector`、`Material`、`WidthRange`、`CuttingPlan`、`DemandConstraintPipeline`、`MaterialConstraintPipeline`、`MachineConstraintPipeline` 和现有扩展测试。
2. 先做 flow/extraction policy，因为这能快速闭合 public 输出和主流程判断；再做 CGPipeline/shadow price；最后收 `addColumns` 和列生成主循环复用。
3. 如果 `addColumns` 原地增量受底层变量结构限制，不要硬改大范围 core；先实现 rebuild-compatible lifecycle，并在文档中明确边界。
4. 每一步都要保持 `V : RealNumber<V>`、`Quantity<V>` 和显式 `Flt64 -> V` 转换边界。
5. 所有计算、判断计算都尽可能留下扩展点；默认实现可以是当前逻辑，但不要把可变业务规则继续硬编码在 application solver 编排层。
6. 工作区可能存在非 CSP1D 脏改，不得回滚或混入无关模块。
