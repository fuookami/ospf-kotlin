# CSP1D 泛型化计划

日期：2026-06-09

## 1. 总目标

将 `ospf-kotlin-framework-csp1d` 建设为相对抽象、可复用的一维分切开发包。目标不是把 `poit/csp1d` 原样搬入 framework，而是沉淀一维分切的通用内核，并把项目接口、运行参数、DTO 协议、公式语言、solver 插件选择等下游适配内容留在业务侧。

当前抽象口径以 `csp1d-domain-material-context` 已提供的实体能力为边界：只保留 `Product`、`ProductDemand`、`Production`、`Costar`、`Material`、`Machine`、`CuttingPlanSlice`、`CuttingPlan`、需求贡献和当前增强上下文能够表达的部分；不把 POIT 中尚未进入 material model 的缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、公式语言、训练平台和业务 DTO 当作当前阶段的必备模型。

## 2. 开发包边界

1. **通用核心层**：material、cutting plan、produce 主问题、solver/quantity adapter。
2. **通用增强层**：yield、length assignment、wasting minimization、schedule variant。
3. **应用编排层**：普通 MILP、列生成、recovery/warm start、Top-K、KPI、render DTO。
4. **下游适配层**：PO/DTO、公式语言、接口服务、控制台、项目运行参数、心跳、租户上下文、solver 插件选择、训练平台和历史样本管理。

暂不单独拆 `csp1d-domain-schedule-context`。schedule 相关能力先作为 produce、yield、length assignment、wasting minimization 的 variant 保留；当重复接口稳定后再抽出公共层。

## 3. 已完成事项

已完成事项只保留阶段级摘要，不保留逐类、逐断言、逐命令的历史细节。

1. 已完成 CSP1D framework 的基础模块、通用领域模型、应用入口和 PO/DTO 无关的问题输入主路径。
2. 已完成 material、cutting plan、produce 主问题、物理量、泛型数值和需求贡献的核心语义收口。
3. 已完成普通 MILP、列生成、LP shadow price、pricing、最终 MILP、Top-K、KPI/render、trace 的 application 主链路。
4. 已完成 yield、length assignment、wasting minimization 增强上下文的求解接入、结果回填和列生成透传。
5. 已完成设备产能、设备批次、动态/固定长度贡献、waste 面积代理和 canonical 去重的通用建模。
6. 已完成初始方案生成主能力、生成统计、benchmark 快照、缓存/剪枝/并行和 dominance 的阶段性性能增强。
7. 已完成 application public 使用面第一阶段，包括 builder、统一 solve config、partial/failed 状态、recovery/warm start、plan-pool adapter、native initial solution 和列生成 recovery。
8. 已完成 demo3 示例主路径迁移，示例不再维护手写 RMP/SP。
9. 已完成当前模型与 POIT CSP1D 边界复核，训练、历史列来源、公式语言、业务 DTO 和参数实验平台继续作为下游或未来扩展能力。
10. 已完成 failure/partial 边界收口：Csp1dMilpSolver 异常安全化、LpInfeasible/LpSolveFailed 区分、Failed 状态使用、LP failure message 传递和 trace 补充。
11. 已完成 trace/KPI/render 稳定化：pricing 生成统计和 LP failure message 进入 render KPI；failureMessage 合并 LP 和 MILP 失败信息。
12. 已完成 pricing 与增强目标复核：`Csp1dPricingObjectiveConfig` 正确映射 `batchMinPenalty`、`trimWidthPenalty`、`restMaterialPenalty`、`materialCostPenalty`；与 MILP 目标函数语义一致。
13. 已完成 failure/partial 语义全路径审计：Csp1dMilp、Csp1dColumnGeneration、Csp1dRecovery、Csp1dColumnGenerationRecovery 在各场景下 status/terminationReason/finalMilpStatus/partialSolutionAvailable/failureMessage 一致。
14. 已完成门禁搜索全项通过和 IDE 编译 + application acceptance 测试通过。

## 4. 需要修正的事项

1. `LpInfeasible` 终止原因基于首次 LP null 返回推断，非 solver 层不可行状态判定。当前语义合理但文档需说明此为推断而非确定判定；如需确定判定需 solver 适配层暴露状态。
2. Maven CLI 本地依赖安装已通过后台完成，但尚未执行完整 reactor 测试；以 IDE 编译和测试为主路径。
3. 工作区存在大量非 CSP1D 改动，提交已严格限定 CSP1D application 范围。
4. 尚未完成 README/README_ch/demo3 文档更新和 Gurobi profile 验证。

## 5. 下一轮目标

在不扩大到 POIT 业务 DTO 和未建模实体的前提下，完成 CSP1D framework 的交付验收收口：修正剩余输出一致性问题，跑通可复用验证基线，补齐 README/README_ch/demo3/daily 的交接信息，并明确真实 solver failure/partial 的当前承诺边界。

## 6. 下一轮事项

1. **验证环境收口**：定位 Maven 超时原因，重新执行 CSP1D 局部 reactor 测试、application acceptance、生成器测试和 Gurobi profile 编译；所有结果以本轮实际输出为准。
2. **failure/partial 语义收口**：确认普通 MILP、列生成、recovery、warm start 在无初始列、最终 MILP 失败、LP 失败、fallback 禁用和局部可用解场景下的状态、trace 和 failureMessage 一致。
3. **真实后端路径收口**：Gurobi 可用时补真实 solver smoke；不可用时至少保留 profile 编译、fake solver 边界覆盖和不可用原因记录。
4. **trace/KPI/render 稳定化**：统一 initial/pricing 生成统计、终止原因、partial/failed 状态、LP failure message、enhancement 结果在 solution、render 和 trace 中的 public 输出边界。
5. **pricing 与增强目标复核**：确认余宽、余料、物料成本、yield、length、waste 目标提示在 pricing 候选筛选、排序、统计和 trace 中的语义稳定。
6. **public API 与文档收口**：同步 README/README_ch、demo3 和最小使用样例，说明 builder、solve config、partial/failed、recovery/warm start、trace/KPI 和当前能力边界。
7. **延后能力边界维护**：缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、训练平台、历史样本服务和业务 DTO 继续只出现在边界说明中，不进入当前领域主路径。

## 7. 下一轮计划

1. 先恢复或绕开 Maven 超时问题，确保能得到当前源码对应的编译和测试结果。
2. 跑 application failure/partial 相关测试，若失败优先修正状态映射、trace、failureMessage 和 recovery 语义。
3. 跑 generation 相关测试和 benchmark 快照，确认新增统计、缓存、剪枝、并行和 dominance 不改变 canonical 结果集合。
4. 复核 trace/KPI/render 输出，将确认为 public 的新增字段同步到稳定 key 或文档说明。
5. 复核 README/README_ch/demo3，确保示例只依赖 CSP1D framework API，不恢复下游业务适配或手写 RMP/SP。
6. 执行门禁搜索、`git diff --check`、CSP1D 局部测试和 Gurobi profile 编译或 smoke，并把实际结果写回本文件。

## 8. 修改清单

下一轮允许在以下范围内集中修改，仍避免触碰无关 framework 模块。

1. `ospf-kotlin-framework-csp1d/csp1d-application`
2. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context`
3. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context`
4. `ospf-kotlin-framework-csp1d/csp1d-domain-yield-context`
5. `ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context`
6. `ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context`
7. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context`
8. `ospf-kotlin-framework-csp1d/csp1d-infrastructure`
9. `ospf-kotlin-framework-csp1d/README.md`
10. `ospf-kotlin-framework-csp1d/README_ch.md`
11. `ospf-kotlin-framework-csp1d/daily.md`
12. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

## 9. 验收标准

1. 当前 material-context 能表达的模型语义不偏离总目标，不引入 POIT DTO、运行参数、公式语言、训练平台或 solver 插件选择逻辑。
2. 普通 MILP、列生成、recovery、warm start 在完整解、空解、部分解、失败、fallback 禁用和 LP 失败场景下有明确状态、trace 和 failureMessage。
3. 若保留 `LpInfeasible`，必须由 solver 层可判定状态触发；否则首次 LP 异常或空结果只能验收为 `LpSolveFailed`。
4. initial/pricing 生成统计、终止原因、partial/failed 状态、enhancement 结果在 solution、render 和 trace 中的 public 输出口径一致。
5. 生成器增强不改变 DFS、N-Same、N-Sum、FullSum 的 canonical 结果集合；缓存、剪枝、并行和 dominance 均有稳定统计和回归测试。
6. benchmark 覆盖中等规模、混合需求单位、扩展规模、更多约束组合、设备可行性和更高候选上限；耗时只作为趋势观察，数量类统计作为稳定验收。
7. demo3 不维护手写 RMP/SP，README/README_ch 能说明当前 public API、能力边界、配置、failure/partial 语义和验证命令。
8. 延后能力只出现在文档边界中，不在当前领域主路径中出现半成品字段或业务适配逻辑。
9. CSP1D 门禁搜索无领域主路径违规命中，`git diff --check -- ospf-kotlin-framework-csp1d` 通过。
10. 至少通过 CSP1D 局部测试和 Gurobi profile `test-compile`；环境允许时补 Gurobi 端到端 smoke。

## 10. 验证基线

下一轮最小验证集如下，执行后需要记录实际结果，不复用历史报告。

1. CSP1D 局部 reactor 测试：覆盖 material、generation、enhancement 和 application。
2. 生成器目标测试：覆盖 DFS、N-Same、N-Sum、FullSum、parallelism、benchmark snapshot 和新增剪枝/缓存统计。
3. application acceptance：覆盖 MILP、列生成、Top-K、KPI/render、recovery、warm start 和 partial solution。
4. Gurobi profile：至少执行 `-Pgurobi-cg-test -DskipTests test-compile`，环境可用时执行目标 smoke。
5. 门禁搜索：
   - `rg -n "com\\.poit|framework\\.bpp3d" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "rollDemand|weightDemand|sheetDemand" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
   - `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "candidatePlans" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "println\\(" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `git diff --check -- ospf-kotlin-framework-csp1d`
