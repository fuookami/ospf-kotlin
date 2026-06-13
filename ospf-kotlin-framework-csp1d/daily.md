# CSP1D 下一轮交接

日期：2026-06-13

## 1. 总目标

`ospf-kotlin-framework-csp1d` 的长期目标是完成一维分切通用内核的列生成生命周期与下游扩展能力闭环：public solve 入口可扩展，基础领域对象、决策对象、约束、目标、候选生成、pricing、主问题流程判断、shadow price、solution/KPI/render 都能通过 framework 通用抽象扩展；内置管线逐步接入 `CGPipeline` / `AbstractShadowPriceMap` 生命周期；`addColumns` 最终支持真实增量列生成，并让 `Csp1dColumnGeneration` 复用 context / pipeline / shadow price map 机制。

仍然不引入 POIT 包名、DTO、接口服务、公式语言、训练平台、缺陷、分段、位置约束、`unitBatch` 等下游或延后能力。POIT 的 same unit length、same width、宽差、材质兼容、设备兼容、成本修正、候选过滤、候选验收、输出扩展等只作为扩展接口验证样例，不进入 framework 内置业务语义。

## 2. 当前判断

本轮已完成 CGPipeline / Shadow Price 统一迁移。主约束管线（demand / material / machine）已从 `Pipeline<LinearMetaModel<Flt64>>` 迁移到 `Csp1dCGPipeline`（即 `CGPipeline<AbstractCsp1dShadowPriceArguments, AbstractLinearMetaModel<Flt64>, AbstractCsp1dShadowPriceMap<...>>`），通过 `constraint.args = Csp1dShadowPriceKey` 替代 constraint-name registry。`Csp1dMilpSolver.solveLP()` 优先使用 CGPipeline refresh / extractor 机制提取 shadow price。`Csp1dColumnGeneration` 主循环不再直接拼装 shadow price key。剩余：

1. fake POIT 类样例测试未执行（验收 7.1.10）。
2. 完整 Maven reactor / Gurobi smoke 验证未执行。
3. README/README_ch 需要同步更新 CGPipeline / shadow price lifecycle 说明。

## 3. 已完成事项摘要

1. 已完成 CSP1D framework 的通用领域模型、物理量、泛型数值、PO/DTO 无关问题输入和 public API 基础面。
2. 已完成 material、cutting plan、produce、yield、length assignment、wasting minimization 的核心语义与建模注册结构。
3. 已完成普通 MILP、列生成、LP shadow price、pricing、final MILP、Top-K、KPI/render、trace、recovery、warm start、partial/failed 主链路。
4. 已完成初始方案生成、统计、benchmark、缓存、剪枝、并行和 dominance 等生成能力增强。
5. 已完成 demo3 主路径迁移，示例不再维护手写 RMP/SP。
6. 已完成 POIT CSP1D 能力边界复核，延后能力继续保留在下游适配或未来通用领域模型扩展中。
7. 已完成 application failure/partial 边界收口，异常安全、失败状态、LP failure trace、pricing 统计 trace 和 render KPI 已进入 public 输出口径。
8. 已完成 README/README_ch 对 public API、能力边界、failure/partial 语义、`LpInfeasible` 推断语义和 KPI key 的同步说明。
9. 已完成 Gantt/BPP3D/POIT 相关建模模式阅读，并将 CSP1D 改造到以 `MetaModel` / `AbstractLinearMetaModel` 为轴心的 context / aggregation / pipeline 结构。
10. 已完成 public modeling extension、context-aware pipeline、统一 `Csp1dExtensionSet`、builder DSL 和 fake 下游扩展样例。
11. 已完成 domain/objective/generation/pricing/flow/extraction policy 的 public 注入和主路径生效。
12. 已完成 `widthFeasibilityCheck`、`enabledWithoutWidthCheck`、`overridesWidthFeasibility` 的宽度扩展收口。
13. 已完成 shadow price key/map 类型桥接、显式 `Flt64 -> V` 转换和基础 shadow price lifecycle wrapper。
14. 已完成列生成生命周期与输出扩展闭环：flow policy、extraction policy、pricing benefit/isImproving、custom canonical key、candidate dominance、termination、partial/fallback 和输出扩展均已接入主路径。
15. 已完成 `addColumns` 真实原地增量列生成。
16. 已完成约束管线中间符号迁移。
17. 已完成 canonicalKeyOverrides / dominanceAcceptOverrides 全链路贯通。
18. 已完成 flow policy selectTermination customReason 写回。
19. 已完成 recovery flow policy allowRecoveryFallback 接入。
20. 已完成测试更新：变量名适配、四个行为断言测试。
21. **已完成 CGPipeline / Shadow Price 统一迁移**：DemandConstraintPipeline / MaterialConstraintPipeline / MachineConstraintPipeline 从 `Pipeline<LinearMetaModel<Flt64>>` 迁移到 `Csp1dCGPipeline`，通过 `constraint.args = Csp1dShadowPriceKey` 替代 constraint-name registry；`Csp1dShadowPriceLifecycle` 优先通过 CGPipeline refresh / extractor 提取影子价格，保留 registry 作为兼容 fallback；`Csp1dMilpSolver.solveLP()` 传入 CG 管线列表给 lifecycle；`Csp1dProduceContext.extractShadowPrice` 直接调用 CGPipeline refresh；`Csp1dIterativeContext.extractShadowPrice` 签名移除 shadowPriceKeys 参数。

## 4. 下一轮目标

下一轮强制目标：**fake POIT 样例测试、完整验证与文档同步**。

完成后应达到以下状态：

1. fake POIT 类样例至少覆盖 same unit length、same width、宽差、设备或材质兼容、业务成本、候选过滤、候选验收、输出扩展中的六类（验收 7.1.10）。
2. 完成 CSP1D 范围内完整验证，包含 Maven reactor、application acceptance、generation、相关 domain 子模块、Gurobi 10 smoke 或明确环境原因。
3. README/README_ch/demo3/daily.md 与当前 API 和真实能力边界一致。
4. 门禁搜索全部通过。

## 5. 下一轮执行包

### 5.1 fake POIT 样例测试

1. 为 same unit length、same width、宽差、设备或材质兼容、业务成本、候选过滤、候选验收、输出扩展各编写一个 fake 扩展样例。
2. 每个样例通过 `Csp1dProblemBuilder` 注入 `Csp1dExtensionSet` 或 `Csp1dSolveConfig`。
3. 确认样例对主路径求解语义无侵入（默认空扩展结果不变）。
4. 不把这些 fake 业务语义变成 framework 内置规则。

### 5.2 完整验证

1. Maven reactor 构建通过。
2. application acceptance 测试通过（MILP、列生成、Top-K、KPI/render、recovery、warm start、partial solution 和扩展入口）。
3. generation/pricing 关键测试通过。
4. material、produce、yield、length assignment、wasting minimization 子模块测试通过。
5. Gurobi 10 smoke 或明确环境原因。
6. `git diff --check -- ospf-kotlin-framework-csp1d` 通过。

### 5.3 文档同步

1. README/README_ch 新增：CGPipeline / shadow price lifecycle 说明、addColumns 真实能力边界。
2. demo3 不恢复手写 RMP/SP。
3. daily.md 完成后再次压缩已完成事项。

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
9. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dShadowPriceLifecycle.kt`
10. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dSolutionEnrichment.kt`
11. `ospf-kotlin-framework-csp1d/csp1d-application/src/test/.../application/service`
12. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context/src/main/.../domain/material/model`
13. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context/src/test/.../domain/material/model`
14. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/.../domain/cutting_plan_generation`
15. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/.../domain/cutting_plan_generation/model`
16. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/.../domain/cutting_plan_generation/service`
17. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce`
18. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce/model`
19. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce/service/pipeline`
20. `ospf-kotlin-framework-csp1d/csp1d-domain-yield-context/src/main/...`
21. `ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context/src/main/...`
22. `ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context/src/main/...`
23. `ospf-kotlin-framework-csp1d/README.md`
24. `ospf-kotlin-framework-csp1d/README_ch.md`
25. `ospf-kotlin-framework-csp1d/daily.md`
26. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

如确实需要修改 `ospf-kotlin-framework` 通用契约，必须先确认 Gantt/BPP3D 影响并扩大验证范围；默认不要改通用契约。

## 7. 验收清单

### 7.1 功能验收

1. `addColumns` 有明确实现和测试；当前已支持真实原地增量。
2. 新增列能参与需求、物料、设备约束和目标函数；重复列不重复添加；失败路径不污染已有列集合。
3. demand/material/machine shadow price 主约束进入统一 extractor/map 或明确保留 fallback 的边界。
4. `Csp1dMilpSolver.solveLP()` 输出的 lightweight `ShadowPriceMap<V>` 与 framework `AbstractCsp1dShadowPriceMap` 语义一致。
5. `Csp1dColumnGeneration` 复用新的 context/pipeline/policy/extraction 能力，public 输出语义不回退。
6. public modeling extension、context-aware pipeline、domain/objective/generation/pricing/flow/extraction policy 均能通过 public config 或 builder 注入。
7. 非宽度 policy 不绕过 `canCut`；声明 `overridesWidthFeasibility = true` 的 policy 能放宽或收窄宽度判断。
8. flow policy 覆盖 initial plan pool、deduplicate、partial 接受、fallback 启用、termination、recovery fallback 中至少六个节点。
9. extraction policy 能写入自定义 solution detail 或 render KPI，并覆盖普通 MILP、列生成、failure/partial 路径。
10. fake POIT 类样例至少覆盖 same unit length、same width、宽差、设备或材质兼容、业务成本、候选过滤、候选验收、输出扩展中的六类。

### 7.2 行为兼容验收

1. 既有 `Csp1dProblem`、`Csp1dMilp`、`Csp1dColumnGeneration`、`Csp1dSchedule`、`Csp1dRecovery` 使用方式不破坏。
2. failure/partial、`LpInfeasible`、`LpSolveFailed`、trace/KPI/render public 语义不变。
3. 默认空 extension set 不改变既有 generation canonical 结果集合。
4. `V : RealNumber<V>` public 泛型边界保持稳定，不新增面向业务侧的 `Flt64` 唯一入口。
5. 宽度、长度、需求、产出、余料、容量等领域量继续使用 `Quantity<V>` 或明确单位。
6. LP shadow price 到 `V` 的转换保持显式，禁止 `dualValue as? V`。
7. README/README_ch 与实际 API 一致，说明 framework 不内置 POIT 语义，但提供足够扩展入口。
8. demo3 不恢复手写 RMP/SP。
9. 不引入 POIT 包名、DTO、公式语言、训练平台、缺陷、分段、位置约束、`unitBatch` 到 framework 主路径。

### 7.3 测试验收

下一轮必须记录实际命令与结果，不复用历史报告。

1. 新增或更新的 addColumns/CGPipeline/shadow price/flow/extraction 专项测试通过。
2. application acceptance 通过，覆盖 MILP、列生成、Top-K、KPI/render、recovery、warm start、partial solution 和扩展入口。
3. generation/pricing 关键测试通过，默认空 policy 下 canonical 结果集合不变。
4. material、produce、yield、length assignment、wasting minimization 子模块测试通过。
5. Gurobi profile 执行 `-Pgurobi-cg-test -DskipTests test-compile`。
6. 当前 Gurobi 10 环境可用时执行真实 solver smoke。
7. 若修改通用 framework 契约，必须补跑受影响的 Gantt/BPP3D 相关测试。
8. `git diff --check -- ospf-kotlin-framework-csp1d` 通过。

### 7.4 门禁搜索

必须全部通过：

1. `rg -n "com\\.poit|framework\\.bpp3d" ospf-kotlin-framework-csp1d -g "*.kt"`
2. `rg -n "rollDemand|weightDemand|sheetDemand" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
3. `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"`
4. `rg -n "candidatePlans" ospf-kotlin-framework-csp1d -g "*.kt"`
5. `rg -n "println\\(" ospf-kotlin-framework-csp1d -g "*.kt"`
6. `rg -n "DefectCostar|defect|缺陷|segment|分段|positionConstraint|位置约束|unitBatch|formula|公式语言|training|训练平台|history sample|历史样本" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/target/**" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
7. `rg -n "dualValue as\\? V|dualValue as V" ospf-kotlin-framework-csp1d -g "*.kt"`
8. `rg -n "addColumns.*TODO|addColumns.*占位|placeholder" ospf-kotlin-framework-csp1d -g "*.kt" -g "*.md"`
9. `git diff --check -- ospf-kotlin-framework-csp1d`

## 8. 非本轮目标

以下内容仍不强制进入下一轮，除非前述目标已完成且验证稳定：

1. 缺陷、分段、位置约束、`unitBatch`、公式语言、训练平台。
2. POIT DTO、接口服务、租户/心跳/训练平台协议。
3. 全量重写 generation/pricing 算法。
4. 在没有明确验证计划的情况下修改 `ospf-kotlin-framework` 通用契约。
5. 大范围 core solver/token 机制重构；除非确认 `addColumns` 必须依赖且能同步验证所有受影响模块。

## 9. 交接提示

1. 下一会话先读本文件和第 5.1 列出的核心文件，不要只按历史报告行动。
2. CGPipeline 迁移已完成：三个主约束管线已从 `Pipeline` → `Csp1dCGPipeline`，使用 `constraint.args = Csp1dShadowPriceKey` 替代 constraint-name registry。`Csp1dShadowPriceLifecycle` 优先通过 CGPipeline refresh / extractor 提取影子价格。constraint-name registry 保留为 `@Deprecated` fallback。
3. 下一轮重点是：fake POIT 样例测试、完整验证、文档同步。
4. 每一步都要保持 `V : RealNumber<V>`、`Quantity<V>` 和显式 `Flt64 -> V` 转换边界。
5. 所有计算、判断计算都尽可能留下扩展点；默认实现可以是当前逻辑，但不要把可变业务规则继续硬编码在 application solver 编排层。
6. 工作区可能存在非 CSP1D 脏改，不得回滚或混入无关模块。
7. canonicalKeyOverrides / dominanceAcceptOverrides 已贯穿 generation → pricing → deduplication 全链路；flow policy selectTermination customReason 写回已实现；recovery flow policy allowRecoveryFallback 已接入。
