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
12. 已完成当前主问题多项式复核：基础 demand、material、machine batch、yield、waste、length 线性表达方向正确；需要显式修正的风险主要集中在设备业务产能、换料次数、超产面积代理口径和增强目标未参与 pricing。
13. 已完成设备业务产能第一阶段建模：`CuttingPlan.capacityConsumption` 进入 `Machine.capacity` 约束，设备 batch shadow price 与 capacity shadow price 分离，`MachineCapacityUsage.used` 改为按 solver 解聚合实际使用量。
14. 已完成设备换料与批次因子边界收口：`maxSwitchCount` 明确为需要排程序列变量的后续能力，`availableBatches`、`maxBatchCount` 和业务产能口径已文档化。
15. 已完成 waste 面积口径显式化：超产面积和余料面积代理均进入配置与结果字段，缺少物料长度和多宽度产品场景已覆盖。
16. 已完成本轮目标验证：fake solver、cutting-plan-generation、Gurobi profile 编译、Gurobi 单目标测试和 CSP1D 门禁均已通过。
17. 已完成 pricing 与增强目标第一阶段对齐：pricing input 可携带方案使用、余宽、余料和物料成本目标提示，候选筛选与排序保持 LP shadow price 基础语义。
18. 已完成 material helper 与生成入口第一阶段收口：通用需求贡献 helper、Costar 不污染需求贡献、物料/设备基础可行性入口和生成器接入均已覆盖。
19. 已完成列生成方案池稳定化第一阶段：pricing 重复列按 ID 和 canonical key 过滤，初始方案池按 canonical key 去重并应用 `maxInitialPlans` 上限。
20. 已完成生成器统计与统一候选控制第一阶段：四类生成器均支持兼容式生成报告、候选上限、基础可行性拒绝统计、canonical 重复过滤统计和 trace 透传。
21. 已完成 dynamic/fixed length 生成阶段默认策略第一阶段：固定长度优先使用产品长度，动态长度重量贡献可按物料长度推导，并已覆盖回归测试。
22. 已完成 demo3 示例迁移第一阶段：示例侧手写领域模型、RMP、SP 和 shadow price map 已移除，主路径改为 `Csp1dProblem<Flt64>` 与 `Csp1dColumnGeneration`。
23. 已完成 application 一站式配置与 KPI 收口第一阶段：`solveConfig`、Top-K、最终 MILP 状态、部分解语义、增强结果计数和 render KPI 已进入普通 MILP 与列生成入口。
24. 已完成生成器规模化第一阶段：四类生成器接入数量计算缓存，DFS/NSum/FullSum 增加剩余宽度不可扩展时的提前剪枝，保持 canonical 输出语义稳定。
25. 已完成当前验证基线刷新：CSP1D 窄测试、Gurobi profile 编译和 demo3 限定编译均已通过。
26. 已完成 application public 使用面第一阶段：`Csp1dProblem` builder、`Csp1dSolveConfig` builder、README/README_ch 和 demo3 builder 示例已收口。
27. 已完成生成器并行第一阶段：DFS、NSum、NSame、FullSum 支持按物料 coroutine 并行开关，并验证 canonical 结果集合稳定。
28. 已完成 public 语义文档第一阶段：Costar filler、dynamic length 生成边界、trace/KPI/render、最终 MILP 状态和部分成功结果已写入 README。
29. 已完成当前验证基线刷新：CSP1D 窄测试、Gurobi profile 编译、demo3 限定编译、门禁搜索和 `git diff --check` 均已通过。
30. 已完成 application KPI 明细第一阶段：`Csp1dKpi.details` 与 render KPI 同步输出 yield、waste、length、物料使用、设备产能和列生成收敛明细。
31. 已完成 recovery/warm start 语义第一阶段：恢复输入、warm start 输入、恢复 trace、warm start ignored/invalid/fallback 语义和验收测试已建立。
32. 已完成生成器中等规模基线与 dominance 剪枝第一阶段：新增中等规模统计样例，支持同贡献候选 dominance 剪枝并输出 `dominatedCandidates`。
33. 已完成 KPI key 稳定化第一阶段：`Csp1dKpiKeys` 覆盖标量、列生成、初始生成和动态明细 key，README 已形成 public 字段入口。
34. 已完成 recovery 分类深化第一阶段：区分 ignored、adapter unsupported、invalid、fallback disabled 和 solver failure，并用带 trace 的异常表达不可恢复场景。
35. 已完成四类生成器中等规模 baseline 第一阶段：DFS、NSum、NSame、FullSum 共用统计验收口径，覆盖候选数量、去重、dominance 和停止原因。
36. 已完成生成器宽度索引第一阶段：需求宽度入口按产品、宽度和需求单位去重，DFS、NSum、FullSum 复用剩余宽度上界剪枝并透传 `widthBoundPrunedNodes`。
37. 已完成 warm start adapter 落点第一阶段：恢复流程可显式应用兼容切割方案池作为初始方案池，同时保留默认 unsupported 与 fallback 语义。
38. 已完成生成统计 KPI/render 同步第一阶段：新增宽度上界剪枝统计 key，并纳入 solution KPI 明细与 render KPI。
39. 已完成 public 文档同步第一阶段：README/README_ch 已说明宽度剪枝统计、warm start plan-pool adapter 和当前验证命令。
40. 已完成 solver 原生 warm start 落点第一阶段：恢复流程可从 previous solution 提取兼容方案使用量，并在 MILP 建模阶段写入 assignment 初始值。
41. 已完成 benchmark 稳定快照第一阶段：生成统计提供可比较的稳定快照输出，中等规模 baseline 改为固定数量类断言。
42. 已完成当前验证基线刷新：CSP1D 窄测试、Gurobi profile 编译、demo3 限定编译和局部生成器快照测试均已通过。
43. 已完成 solver 原生 warm start 真实后端 smoke 第一阶段：Gurobi profile 已覆盖 native initial solution 的真实 MILP 求解路径。
44. 已完成 benchmark 多场景快照第一阶段：生成器稳定快照扩展到混合需求单位与更紧刀数场景。
45. 已完成 recovery previousSolution 真实后端路径第一阶段：恢复流程可从上一轮解提取兼容方案池和使用量，并通过 Gurobi native warm start 完成真实求解 smoke。
46. 已完成当前验证基线刷新：CSP1D 窄测试、Gurobi profile 编译、Gurobi native warm start 双路径 smoke、demo3 限定编译、门禁搜索和 `git diff --check` 均已通过。
47. 已完成 recovery previousSolution 兼容子集过滤第一阶段：上一轮方案池可在当前问题变化后过滤不兼容方案，继续复用兼容方案和使用量，并覆盖 fake solver 初始解捕获与 Gurobi 真实后端 smoke。
48. 已完成当前验证基线刷新：CSP1D 窄测试、Gurobi native warm start 三路径 smoke、demo3 限定编译、门禁搜索和 `git diff --check` 均已通过。
49. 已完成列生成最终 MILP warm start 落点第一阶段：列生成入口可接收兼容方案使用量，并在最终主问题求解阶段写入 native initial solution。
50. 已完成当前验证基线刷新：CSP1D 窄测试、Gurobi native warm start 四路径 smoke、demo3 限定编译、门禁搜索和 `git diff --check` 均已通过。
51. 已完成 recovery warm start 复杂组合验证第一阶段：设备产能、yield/waste/length 增强配置、compatible warm start 的 fallback 禁用和 Gurobi 设备产能 + yield 真实后端路径均已覆盖。
52. 已完成当前验证基线刷新：新增 application recovery 验收、Gurobi native warm start 五路径 smoke、CSP1D 窄测试和 demo3 限定编译均已通过。

## 3. 需要修正的事项

当前没有已确认的建模偏差需要立即修正。剩余工作转入未完成事项，主要是 solver 后端 native initial solution 在多轮 previousSolution、partial solution 和更复杂列生成恢复组合下的稳定性验证、生成算法性能深化、更多业务规模 benchmark 扩展、Gurobi 列生成端到端覆盖和 public API 收口。

## 4. 未完成事项

### 4.1 Material-context 当前实体能力补齐

#### 目标

在不一比一复刻 POIT 的前提下，补齐当前 material-context 已暴露实体自然应支持的通用行为。

#### 事项

1. `Product.dynamicLength` 的生成阶段默认策略、最终 length assignment 边界和 public 文档说明已完成第一阶段。
2. `Costar` 的默认 filler 行为和“不参与 demand contribution”的 public 语义已完成第一阶段。
3. 当前统一可行性入口覆盖基础物料、设备和幅宽，后续需要随 material model 新增实体继续扩展。

#### 计划

1. 随 material model 新增实体时，扩展统一可行性入口，避免生成器和 solver 分叉。
2. 将缺陷、分段、位置约束、`unitBatch` 等延后能力维持在清单层，不在当前模型中增加半成品字段。
3. 持续用测试和 README 校准 dynamic length 与 Costar 的边界，避免下游把生成贡献误解为最终卷长分配。

#### 修改清单

1. `csp1d-domain-material-context/src/main/.../ProductDemand.kt`
2. `csp1d-domain-material-context/src/main/.../CuttingPlanDemandContribution.kt`
3. `csp1d-domain-material-context/src/main/.../Costar.kt`
4. `csp1d-domain-material-context/src/main/.../Material.kt`
5. `csp1d-domain-cutting-plan-generation-context/src/main/...`
6. `csp1d-domain-material-context/src/test/...`

#### 验收标准

1. 新增 material entity 后，生成器和 solver 对同一个 plan 的基础可行性判断一致。
2. Costar 和 dynamic length 语义持续保持在 public 文档或配置边界中，不引入业务 DTO。
3. 延后能力不影响当前 material-context 边界内的编译和求解。

### 4.2 切割方案生成算法增强

#### 目标

把当前生成器从“链路验证可用”推进到“中等规模可用”，并为后续扩展模型预留清晰入口。

#### 事项

1. DFS/NSum/NSame/FullSum 已可用，基础贡献构造、可行性入口、统一候选上限、统计报告、数量缓存、宽度索引剪枝、按物料并行、同贡献 dominance、四算法中等规模基线、多场景稳定快照已完成第一阶段。
2. 单位长度、更细粒度长度约束、更大规模 benchmark、物料等价复用和更强 dominance 剪枝仍待深化。
3. 缺陷、分段、onSide/inMiddle、`unitBatch` 等 POIT 语义暂不属于当前实体边界；只有当 material model 增加通用实体后再纳入。

#### 计划

1. 继续扩展 width combination 的上界估计、组合 dominance、物料等价场景复用和更细长度边界剪枝。
2. 将稳定 benchmark 快照继续扩展到更大物料池、更多宽度分布和更高候选上限场景，耗时类指标只作为趋势观察。
3. 将生成报告在 application KPI 与 render 边界持续固化，避免调用方只能读 trace。
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
2. 并行开关不改变结果集合的 canonical form，并持续覆盖回归测试。
3. 中等规模、混合单位和更大规模扩展样例的候选数量、重复过滤、dominance 剪枝、宽度上界剪枝和基础可行性拒绝有稳定快照记录。
4. 延后能力清单不影响当前 material-context 边界内的编译和求解。

### 4.3 Application API、KPI 和恢复能力

#### 目标

把 application 层从 solver 调用入口整理成稳定的 framework 使用面，减少下游项目直接碰 MILP 内部实现的需要。

#### 事项

1. `Csp1dProblem<V>`、`Csp1dSolution<V>`、KPI、Top-K、render 输出、builder、README 和 demo3 示例已完成统一入口第一阶段。
2. 列生成 trace 已覆盖基础终止原因、LP 失败、重复列收敛、最终 MILP 状态、部分成功结果和收敛 KPI 明细；恢复与 warm start 输入输出结构、状态分类、trace 异常、plan-pool adapter、native initial solution 落点、列生成最终 MILP warm start、recovery previousSolution 真实路径和兼容子集过滤已完成第一阶段。
3. 增强配置已通过一站式 `Csp1dSolveConfig` 聚合，KPI key 已有稳定 public 入口；设备产能、增强配置和 fallback 禁用的 warm start 验证已完成第一阶段，后续需要继续收口多轮 previousSolution、partial solution 和更复杂列生成恢复组合的端到端稳定性。

#### 计划

1. 继续明确异常和部分成功结果：最终 MILP 不可行、恢复失败、warm start 失效、fallback 禁用和局部可用解。
2. 在不引入业务 DTO 的前提下，继续扩展 solver 后端对 native initial solution 的多轮 previousSolution、partial solution 和更复杂列生成恢复组合验证，保留 plan-pool adapter 作为 framework 级 fallback。
3. 将 KPI key 的单位表达和动态 key helper 随新增指标持续补齐，避免下游依赖临时字符串。
4. 将 public README 与 acceptance test 随 API 变化同步维护。

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
3. trace 能解释终止原因、每轮 LP 目标值、新列数量、初始生成统计、warm start 应用结果、已应用使用量和最终 MILP 状态。
4. 不引入 POIT DTO、运行参数、公式语言或 solver 插件选择逻辑。

## 5. 下一轮执行计划

### 5.1 合并执行范围

#### 目标

下一轮尽量以一次宽范围迭代完成真实 solver warm start 复杂场景深化、生成算法性能深化、更大规模 benchmark 扩展、application public 使用面收口和 Gurobi 列生成端到端验证，减少反复切换上下文。

#### 事项

1. 在已完成 public key、KPI 明细、trace 最终 MILP 状态、部分成功结果、recovery 异常分类、plan-pool adapter、native initial solution 落点、direct MILP Gurobi smoke、列生成最终 MILP Gurobi smoke、recovery previousSolution Gurobi smoke、问题变化兼容子集过滤和设备产能 + yield recovery Gurobi smoke 的基础上，继续深化真实 solver warm start 复杂场景验证。
2. 在已完成数量缓存、宽度索引剪枝、并行开关、同贡献 dominance、四算法中等规模 baseline 和多场景稳定快照的基础上，推进组合 dominance、物料等价复用和更细长度剪枝。
3. 将生成统计继续沉淀到 application KPI/render 边界，并把 benchmark 快照扩展到更大规模可比较基线。
4. 扩展 application acceptance 与 demo3 覆盖，确保 public builder、solveConfig、recovery、Top-K、部分解和 render KPI 的使用面稳定。
5. 固化当前受上游 framework 未跟踪改动影响时的 CSP1D 局部验证命令，并在环境允许时执行 Gurobi 端到端目标测试。

#### 计划

1. 第一段：深化生成算法剪枝，覆盖组合 dominance、物料等价复用、长度边界剪枝和结果 canonical 稳定性。
2. 第二段：将四算法 benchmark 快照扩展为更大规模基线，明确耗时类统计与数量类统计的验收口径。
3. 第三段：继续验证 solver 后端对 native initial solution 的实际消费，扩展多轮 previousSolution、部分可行解、列生成恢复和恢复后再次求解场景；若 solver 层能力不足，保留接口和失败 trace，不把业务 DTO 带入 framework。
4. 第四段：继续把新增生成统计、recovery 状态、warm start 处理结果和 partial solution 语义沉淀到 KPI/render 或 trace 稳定边界。
5. 第五段：扩展 demo3 和 README 的 public 使用示例，确保示例仍只依赖 framework API。
6. 第六段：执行目标测试、Gurobi profile 编译或端到端验证、门禁搜索和 `git diff --check`。

#### 修改清单

1. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context`
2. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context`
3. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context`
4. `ospf-kotlin-framework-csp1d/csp1d-domain-yield-context`
5. `ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context`
6. `ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context`
7. `ospf-kotlin-framework-csp1d/csp1d-application`
8. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`
9. `ospf-kotlin-framework-csp1d/README.md`
10. `ospf-kotlin-framework-csp1d/README_ch.md`
11. `ospf-kotlin-framework-csp1d/daily.md`

#### 验收标准

1. 更强剪枝与缓存不改变四类生成器的 canonical 结果集合。
2. fake solver 测试覆盖 generation、application API、recovery、warm start adapter 和 demo3 入口的新增行为。
3. benchmark 快照能比较 DFS、NSum、NSame、FullSum 在中等规模、混合单位和更大规模场景下的候选数量、重复过滤、dominance 剪枝、宽度上界剪枝和基础可行性拒绝，耗时只作为趋势观察。
4. solver 原生 warm start 能力在 fake solver、Gurobi smoke、列生成最终 MILP、recovery previousSolution 真实路径和复杂恢复组合上有明确支持、降级或失败 trace，不引入业务 DTO。
5. Gurobi profile 至少完成 `test-compile`；环境可用时执行端到端目标测试。
6. demo3 示例不再维护手写 RMP/SP。
7. CSP1D 门禁搜索、`git diff --check -- ospf-kotlin-framework-csp1d` 通过。
8. `daily.md` 继续只保留阶段摘要和下一轮计划，不恢复历史流水。

### 5.2 验证基线

#### 目标

建立下一轮可重复执行的最小验证集，避免只依赖历史 surefire 报告。

#### 事项

1. 单模块测试在当前环境可能因依赖解析或 Gurobi 环境差异失败，需要明确命令和预期。
2. 门禁搜索必须覆盖领域代码和测试命名例外。
3. 固定数值类型命中需要持续分类。

#### 计划

1. 优先执行 CSP1D 窄 reactor application 测试，避免被上游未跟踪 framework 改动阻塞。
2. 优先执行 material-context 与 cutting-plan-generation-context 的目标测试，覆盖 helper、pricing 和生成器。
3. Gurobi 环境可用时执行 CSP1D 窄 reactor `-Pgurobi-cg-test`；不可用时至少执行 `-DskipTests test-compile`。
4. 每次改动后执行门禁 grep 和 `git diff --check`。

#### 修改清单

1. `ospf-kotlin-framework-csp1d/daily.md`
2. 必要时新增 `ospf-kotlin-framework-csp1d` 局部验证脚本或 README 片段。

#### 验收标准

1. 文档记录实际执行过的验证，不声明未执行的全量通过。
2. 测试失败或超时必须记录原因和下一步，不把旧报告当作当前结果。
3. 门禁命中能区分测试局部变量、DTO/adapter 边界和领域主路径问题。
