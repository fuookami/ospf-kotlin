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

1. 已建立 CSP1D 基础模块、领域模型、应用入口、render DTO 和 PO/DTO 无关的问题输入主路径。
2. 已完成核心实体、物理量、泛型数值、需求贡献和 product + unit 需求口径的基础收口。
3. 已完成 application 层普通 MILP、列生成、LP shadow price、pricing、最终 MILP、Top-K、KPI/render 和 trace 主链路。
4. 已完成 yield、length assignment、wasting minimization 增强上下文的基础 solver 接入、结果回填和列生成透传。
5. 已完成设备业务产能、设备批次边界、动态/固定长度生成贡献、waste 面积代理和 canonical 去重的第一阶段建模。
6. 已完成初始方案生成主能力：DFS、N-Same、N-Sum、FullSum、Costar filler、组合约束、timeout、候选上限、基础可行性过滤、统计报告和 benchmark 快照。
7. 已完成生成器性能第一阶段：数量缓存、宽度索引剪枝、长度上界剪枝、刀数下界可达性剪枝、物料等价宽度入口复用、物料等价切片模板复用、按物料并行和同贡献 dominance。
8. 已完成 application public 使用面第一阶段：builder、统一 solve config、partial solution、recovery/warm start、plan-pool adapter、native initial solution、列生成 recovery 和稳定 KPI key。
9. 已完成 demo3 示例迁移第一阶段：示例主路径改为 `Csp1dProblem<Flt64>` 与 framework column generation，不再维护手写 RMP/SP。
10. 已完成当前模型与 POIT CSP1D 的边界复核：现有模型来源正确，当前只承诺 material-context 已表达实体能建模的部分。
11. 已完成 public README/README_ch 语义同步、CSP1D 门禁搜索、`git diff --check`、CSP1D 局部测试和 Gurobi profile 编译基线。
12. 已清理过时的独立规划文档；训练、历史列来源和参数实验平台不作为当前 framework 内置能力。

## 4. 需要修正的事项

当前没有已确认的建模偏差需要立即修正。下一轮重点转入能力收口：更大规模生成性能、真实 solver failure/partial 边界、增强目标与 pricing 协同、public API 稳定性和更宽验证基线。

## 5. 下一轮事项

### 5.1 目标

以一次宽范围迭代完成 CSP1D framework 的可交付收口：在不扩大到 POIT 业务 DTO 和未建模实体的前提下，强化生成算法、application 求解/恢复、增强上下文协同、public 文档示例和验证门禁，减少后续小迭代次数。

### 5.2 事项

1. **生成算法性能与正确性收口**：深化组合 dominance、动态长度边界剪枝、更多约束/并行边界下的模板复用、设备可行性基线和更大规模 benchmark。
2. **pricing 与增强目标协同**：继续明确余宽、余料、物料成本、yield、length、waste 目标提示在 pricing 候选筛选、排序和 trace 中的稳定语义。
3. **application failure/partial 边界**：覆盖最终 MILP 不可行、求解失败、LP 失败、无初始列、fallback 禁用、warm start 失效和局部可用解的 fake solver 与真实后端路径。
4. **recovery/warm start 收口**：强化 previousSolution 兼容子集过滤、native initial solution 应用、plan-pool fallback、列生成 recovery 多轮复用和失败 trace。
5. **KPI/render/trace 稳定化**：补齐新增统计、单位表达、动态 key helper、终止原因、partial 状态和增强结果在 solution、render 与 trace 中的一致输出。
6. **public API 与文档示例收口**：整理 builder、solve config、README/README_ch、demo3 和最小使用样例，确保下游只依赖 framework API。
7. **延后能力边界维护**：缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、训练平台和历史样本服务继续保留为下游或未来 material model 扩展能力，不在当前代码中增加半成品字段。

## 6. 下一轮计划

1. 先做一次模型和 public API 审计，确认 material-context 边界、主问题多项式、增强目标、KPI key、render DTO 和 README 口径一致。
2. 集中推进生成算法增强：组合 dominance、动态长度剪枝、模板复用边界、设备可行性和更大规模 benchmark 同步落地，并保持 canonical 结果集合稳定。
3. 集中推进 application 求解边界：fake solver 覆盖失败/partial/recovery 组合，Gurobi 可用时补真实后端 smoke；不可用时至少保留 profile 编译和失败 trace 验证。
4. 将新增生成统计、pricing 提示、recovery 状态、warm start 结果、partial solution 和增强指标沉淀到 KPI/render/trace 稳定边界。
5. 同步 demo3、README/README_ch 和验收测试，确保示例仍只使用 CSP1D framework API，不恢复手写 RMP/SP。
6. 执行 CSP1D 局部测试、目标 benchmark 测试、Gurobi profile 编译或 smoke、门禁搜索和 `git diff --check`，并把实际验证结果写回 `daily.md`。

## 7. 修改清单

下一轮允许在以下范围内集中修改，仍避免触碰无关 framework 模块。

1. `ospf-kotlin-framework-csp1d/csp1d-infrastructure`
2. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context`
3. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context`
4. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context`
5. `ospf-kotlin-framework-csp1d/csp1d-domain-yield-context`
6. `ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context`
7. `ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context`
8. `ospf-kotlin-framework-csp1d/csp1d-application`
9. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`
10. `ospf-kotlin-framework-csp1d/README.md`
11. `ospf-kotlin-framework-csp1d/README_ch.md`
12. `ospf-kotlin-framework-csp1d/daily.md`

## 8. 验收标准

1. 当前 material-context 能表达的模型语义不偏离总目标，不引入 POIT DTO、运行参数、公式语言、训练平台或 solver 插件选择逻辑。
2. 生成器增强不改变 DFS、N-Same、N-Sum、FullSum 的 canonical 结果集合；新增剪枝、缓存和并行能力均有稳定统计和回归测试。
3. benchmark 覆盖中等规模、混合需求单位、扩展规模、更多约束组合、设备可行性和更高候选上限；耗时只作为趋势观察，数量类统计作为稳定验收。
4. pricing、普通 MILP、列生成、recovery 和 warm start 在完整解、空解、部分解、失败、fallback 禁用和真实后端 smoke 上有明确结果或失败 trace。
5. solution KPI、render KPI 和 trace 对新增统计、增强结果、warm start 处理、partial 状态和终止原因输出一致。
6. demo3 不维护手写 RMP/SP，README/README_ch 能说明当前 public API、边界、配置和验证命令。
7. 延后能力只出现在文档边界中，不在当前领域主路径中出现半成品字段或业务适配逻辑。
8. CSP1D 门禁搜索无领域主路径违规命中，`git diff --check -- ospf-kotlin-framework-csp1d` 通过。
9. 至少通过 CSP1D 局部测试和 Gurobi profile `test-compile`；环境允许时补 Gurobi 端到端 smoke。

## 9. 验证基线

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
