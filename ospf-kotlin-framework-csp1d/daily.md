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

## 3. 需要修正的事项

当前没有已确认的建模偏差需要立即修正。剩余工作转入未完成事项，主要是规模化生成、application 使用面、示例迁移和验证收口。

## 4. 未完成事项

### 4.1 Material-context 当前实体能力补齐

#### 目标

在不一比一复刻 POIT 的前提下，补齐当前 material-context 已暴露实体自然应支持的通用行为。

#### 事项

1. `Product.dynamicLength` 与 length assignment 建模已有连接，但方案生成阶段仍缺少更明确的默认长度策略。
2. `Costar` 的默认行为已通过 filler 收口，后续还需要把“只补余宽、不参与 demand contribution”的语义沉淀到更稳定的模型文档或配置边界。
3. 当前统一可行性入口覆盖基础物料、设备和幅宽，后续需要和更细粒度长度、缺陷、分段实体的扩展边界保持一致。

#### 计划

1. 为 dynamic/fixed length 产品补充生成阶段的默认长度策略和测试。
2. 将 Costar filler 的语义写入更稳定的 public 文档或配置说明，避免下游误把 costar 当作需求产品。
3. 随 material model 新增实体时，扩展统一可行性入口，避免生成器和 solver 分叉。

#### 修改清单

1. `csp1d-domain-material-context/src/main/.../ProductDemand.kt`
2. `csp1d-domain-material-context/src/main/.../CuttingPlanDemandContribution.kt`
3. `csp1d-domain-material-context/src/main/.../Costar.kt`
4. `csp1d-domain-material-context/src/main/.../Material.kt`
5. `csp1d-domain-cutting-plan-generation-context/src/main/...`
6. `csp1d-domain-material-context/src/test/...`

#### 验收标准

1. 动态长度与固定长度产品在生成和最终 MILP 中的行为均有测试覆盖。
2. Costar 语义在 public 文档或配置边界中清晰，不引入业务 DTO。
3. 新增 material entity 后，生成器和 solver 对同一个 plan 的基础可行性判断一致。

### 4.2 切割方案生成算法增强

#### 目标

把当前生成器从“链路验证可用”推进到“中等规模可用”，并为后续扩展模型预留清晰入口。

#### 事项

1. DFS/NSum/NSame/FullSum 已可用，基础贡献构造和可行性入口已收口，但并行生成、缓存和规模化剪枝不足。
2. 单位长度、更细粒度长度约束、动态长度产品和中等规模性能基线仍待收口。
3. 缺陷、分段、onSide/inMiddle、`unitBatch` 等 POIT 语义暂不属于当前实体边界；只有当 material model 增加通用实体后再纳入。

#### 计划

1. 为生成器增加 coroutine 并行、统一候选上限、可中断统计和中等规模基线。
2. 增加 width combination 的上界估计、dominance 剪枝和按 material/demand unit 的缓存。
3. 扩展 canonical 去重到生成器批量输出统计，记录被过滤的重复列数量。
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
2. 列生成 trace 已覆盖基础终止原因、LP 失败和重复列收敛；恢复、最终 MILP 失败和部分成功结果表达仍可读性不足。
3. 目前增强配置分散在 yield/waste/length，缺少面向调用方的一站式建模配置。

#### 计划

1. 收口 `Csp1dProblem` builder 和 `Csp1dSolveConfig`，统一 solver、pricing、yield、waste、length、trace、Top-K 配置。
2. 补充 `Csp1dSolution` KPI：需求满足、欠产/超产、余宽、余料、物料成本、产能使用、列生成收敛信息。
3. 明确异常和部分成功结果：最终 MILP 不可行、恢复失败、warm start 失效和局部可用解。
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

下一轮尽量以一次宽范围迭代完成生成器规模化增强、application API/KPI 收口、demo3 示例迁移和验证基线固化，减少反复切换上下文。

#### 事项

1. 扩展生成器性能、缓存、剪枝、统计和中等规模基线。
2. 补齐 dynamic/fixed length 产品在生成阶段的默认长度策略。
3. 打通 application 一站式配置、KPI、trace 终止原因和部分成功结果。
4. 迁移 demo3 示例，删除示例侧手写 RMP/SP 主路径。
5. 固化当前受上游 framework 未跟踪改动影响时的 CSP1D 局部验证命令。

#### 计划

1. 第一段：生成器增强，完成并行开关、缓存、dominance 剪枝、统计输出和性能样例。
2. 第二段：长度策略收口，完成 dynamic/fixed length 默认策略、约束入口和测试。
3. 第三段：application API 与 demo3，完成统一 solve config、KPI 输出、最终 MILP 状态、部分成功结果和示例迁移。
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

1. 生成器中等规模样例有候选数量、耗时和重复列过滤统计。
2. fake solver 测试覆盖 generation、application API 和 demo3 入口的新增行为。
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
