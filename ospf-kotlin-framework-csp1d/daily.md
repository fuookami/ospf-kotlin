# CSP1D 下一轮交接

日期：2026-06-11

## 1. 总目标与轮次策略

`ospf-kotlin-framework-csp1d` 的长期目标是完成列生成生命周期与下游扩展能力闭环：public solve 入口可扩展、内置管线接入 `CGPipeline` / `AbstractShadowPriceMap` 生命周期、`addColumns` 支持真实增量列生成，并最终让 `Csp1dColumnGeneration` 复用 context / pipeline / shadow price map 机制。

为了尽可能减少迭代次数，但避免一轮同时改动过大，本轮按以下策略执行：

1. **下一轮必做**：public 扩展入口闭环。把当前 builder 级 `extraPipelines` 打通到 `Csp1dSolveConfig`、普通 MILP、列生成 LP master、列生成 final MILP、recovery/partial 路径，并补 public API 测试与文档。
2. **下一轮条件加做**：`CGPipeline` / `AbstractCsp1dShadowPriceMap` 最小桥接。只有在 public 扩展入口稳定、测试通过后再做；优先做 shadow price 相关约束的样板，不要求全量替换所有 pipeline。
3. **后续最终深重构**：真实增量 `addColumns` 与 `Csp1dColumnGeneration` 全面重构。该部分涉及变量增量注册、约束刷新、目标刷新、失败回滚和列生成主循环，不纳入下一轮强制验收。

本轮仍不引入 POIT 包名、DTO、接口服务、公式语言、训练平台、缺陷、分段、位置约束、`unitBatch` 等下游或延后能力。POIT 的 same unit length、same width、宽差、材质兼容等规则只作为扩展接口验证样例，不进入 framework 内置业务语义。

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
10. 已完成 CSP1D 建模注册改造第一、二阶段：核心接口、Aggregation、Pipeline、`Csp1dProduceContext`、LP/MILP 模式区分和 `Csp1dMilpSolver` 瘦身已落地。
11. 已完成 fake 扩展约束测试，builder 级 `extraPipelines` 能验证 same unit length / same width 类约束可注入 MILP 和 LP 模型。
12. 已完成 `Csp1dShadowPriceKey`、`AbstractCsp1dShadowPriceMap`、`Csp1dCGPipeline` 类型别名和 `toShadowPriceMap()` 桥接。
13. 已修复 shadow price 泛型转换问题，LP 对偶值不再通过 `dualValue as? V` 静默丢失，已改为基于样本值的显式 `Flt64 -> V` 转换。

## 3. 当前未闭环点

1. **P0 public 扩展入口未打通**：`extraPipelines` 当前只在 `Csp1dProduceContextBuilder.extraPipeline(...)` builder 级可用；`Csp1dSolveConfig`、`Csp1dMilp`、`Csp1dColumnGeneration`、final MILP 和 recovery 路径尚未提供稳定 public 扩展参数。
2. **P1 内置管线未接入 CGPipeline 生命周期**：当前内置约束管线仍主要实现 `Pipeline<LinearMetaModel<Flt64>>`，尚未实现 `Csp1dCGPipeline` 所需的 `refresh()` / `extractor()` 机制。
3. **P1 ColumnGeneration 仍有手动影子价格路径**：`Csp1dColumnGeneration` 尚未充分复用 `AbstractCsp1dShadowPriceMap`、`Csp1dCGPipelineList` 和 context 的 `extractShadowPrice` 生命周期。
4. **P2 增量列生成未实现**：`Csp1dProduceContext.addColumns` 当前只做去重并返回新增方案，未在已有模型上添加列变量、刷新中间值、刷新约束表达式或更新目标函数。
5. **P2 文档需要随实现同步**：README/README_ch 目前尚未说明 public 扩展入口、`CGPipeline` 生命周期和增量列生成边界。

## 4. 下一轮目标

下一轮只设一个强制目标：**public 扩展入口闭环**。

完成后，下游应能通过 public framework API 注入 fake same unit length / same width 类约束，并在以下路径中生效：

1. 普通 MILP。
2. 列生成 LP master。
3. 列生成 final MILP。
4. recovery / partial 路径中重新求解的 MILP。

下一轮的条件加做目标是：在不扩大风险的前提下完成 `CGPipeline` / `AbstractCsp1dShadowPriceMap` 最小样板桥接。该目标不阻塞 public 扩展入口验收；若开始实现，则必须有专项测试。

下一轮明确不做真实增量 `addColumns` 和 `Csp1dColumnGeneration` 全面重构，只保留接口状态与后续计划，避免把变量增量注册、约束刷新、目标刷新和列生成主循环重写压进同一轮。

## 5. 下一轮事项

### 5.1 必做：扩展配置类型

1. 新增或选择合适位置定义 public 扩展配置类型，建议命名为 `Csp1dModelingExtension<V>` 或等价名称。
2. 最小必需能力：
   - 承载额外 `Pipeline<LinearMetaModel<Flt64>>`。
   - 能区分适用建模模式，例如 MILP、LP、final MILP 或全部模式。
   - 默认空配置不改变现有求解行为。
3. 暂不强制实现 solution extractor、KPI enricher、render enricher，但接口设计要避免将来无法扩展。
4. 扩展配置不应引入 POIT 业务字段；测试中的 same unit length / same width 只能作为 fake pipeline 形态。

### 5.2 必做：接入 Csp1dSolveConfig 与 Builder

1. 在 `Csp1dSolveConfig<V>` 增加扩展字段，默认 `emptyList()`，避免破坏既有构造调用。
2. 在 `Csp1dSolveConfigBuilder<V>` 增加方法：
   - 追加单个扩展。
   - 追加多个扩展。
   - 必要时追加便捷方法直接接收 pipeline。
3. 更新 KDoc，说明扩展入口用于追加通用建模规则，不承载下游 DTO 或公式语言。
4. 保持 `csp1dSolveConfig { ... }` DSL 兼容。

### 5.3 必做：接入普通 MILP

1. 修改 `Csp1dMilp` 的配置合并逻辑，确保 `problem.solveConfig` 和调用参数中的扩展配置不会丢失。
2. 修改 `Csp1dMilpSolver.solve(...)` 参数或上下文构建过程，把适用于 MILP 的扩展 pipeline 注入 `Csp1dProduceContextBuilder`。
3. 确认 warm start、yield/waste/length config、topK 和 allowPartialSolution 行为不受扩展配置影响。
4. 新增 public API 测试：通过 `Csp1dSolveConfig` 注入 fake same unit length 或 same width 约束，普通 MILP 模型注册该约束并求解。

### 5.4 必做：接入列生成 LP 与 final MILP

1. 修改 `Csp1dColumnGeneration` 的 LP master 构建路径，把适用于 LP 的扩展 pipeline 注入 context。
2. 修改 final MILP 构建路径，把适用于 final MILP 的扩展 pipeline 注入 context。
3. 确认 LP 模式下仍不创建 yield/length slack，`LpInfeasible` 和 `LpSolveFailed` 语义不变。
4. 确认 final MILP failure/partial 的状态映射、failureMessage、trace/KPI/render 语义不变。
5. 新增 public API 测试：同一个扩展配置在列生成 LP 和 final MILP 中均生效。

### 5.5 必做：接入 recovery / partial 路径

1. 检查 `Csp1dRecovery` 与 `Csp1dColumnGenerationRecovery` 如何传递 `Csp1dSolveConfig`。
2. 确保 fallback MILP、recovery MILP、partial solution final MILP 路径不丢失扩展配置。
3. 增加或调整测试，覆盖最终 MILP 失败后 recovery/partial 路径仍保留扩展配置。

### 5.6 必做：测试与文档

1. 将现有 `Csp1dExtraPipelineTest` 从 builder 级测试扩展为 public solve 入口测试，或新增独立测试类。
2. 覆盖至少三类场景：
   - 普通 MILP public 扩展。
   - 列生成 LP + final MILP public 扩展。
   - 多个扩展同时注册。
3. README/README_ch 补充 public 扩展入口示例，说明 same unit length / same width 类约束应在下游以 pipeline 注入。
4. README/README_ch 明确当前 `addColumns` 仍是预留接口，真实增量列生成属于后续阶段。
5. 更新 `daily.md`，记录下一轮实际验证结果。

### 5.7 条件加做：CGPipeline 最小样板

只有在 5.1 至 5.6 完成且测试稳定后再执行本节。

1. 阅读 `ospf-kotlin-framework` 的 `Pipeline.kt`、`ShadowPrice.kt`，以及 Gantt/BPP3D 的 `refresh()` / `extractor()` 示例。
2. 选择一个最小样板，优先从 demand constraint 开始，不一次性重写所有约束管线。
3. 让样板管线能向 `AbstractCsp1dShadowPriceMap` 写入 LP 对偶值，并通过 `toShadowPriceMap()` 转换为 pricing 可用的 `ShadowPriceMap<V>`。
4. 保持 LP 对偶值到 `V` 的转换显式，禁止恢复 `dualValue as? V`。
5. 增加专项测试验证 extractor / refresh / map 转换。
6. 若样板引起大范围改动，立即停止，把本节保留为后续阶段，不影响 public 扩展入口交付。

### 5.8 后续阶段记录：不在下一轮强制执行

1. 真实增量 `addColumns`：新增列变量、刷新需求/物料/设备表达式、刷新目标项和失败回滚。
2. `Csp1dColumnGeneration` 全面重构：用 context / `Csp1dCGPipelineList` / `AbstractCsp1dShadowPriceMap` 替换手动 shadow price 主路径。
3. 增量列生成默认启用策略：需在 canonical 结果、KPI、trace、失败语义稳定后再决定。

## 6. 推荐执行计划

1. **基线确认**
   - 记录 `git status --short`，不要混入非 CSP1D 改动。
   - 搜索 `dualValue as? V`、`extraPipelines`、`addColumns`、`Csp1dCGPipeline`，确认当前缺口。
   - 先跑或检查当前 application / generation / material / yield / length / waste 测试状态。

2. **扩展 API 设计**
   - 定义扩展配置类型和建模模式过滤枚举。
   - 接入 `Csp1dSolveConfig` 和 `Csp1dSolveConfigBuilder`。
   - 编译通过后再继续传播到 solver。

3. **普通 MILP 路径传播**
   - 从 `Csp1dMilp` 传到 `Csp1dMilpSolver.solve(...)`。
   - 从 solver 传到 `Csp1dProduceContextBuilder.extraPipeline(...)`。
   - 补普通 MILP public 扩展测试。

4. **列生成路径传播**
   - 先接 LP master。
   - 再接 final MILP。
   - 最后检查 recovery/partial 复用路径。
   - 补列生成 public 扩展测试。

5. **回归与文档**
   - 跑 application acceptance 和新增扩展测试。
   - 更新 README/README_ch。
   - 更新 `daily.md` 验证结果。
   - 跑门禁搜索和 `git diff --check -- ospf-kotlin-framework-csp1d`。

6. **条件加做 CGPipeline 样板**
   - 仅在 public 扩展入口已稳定时开始。
   - 只做一个可验证样板，避免把下一轮扩大成列生成深重构。
   - 完成则记录为下一阶段前置能力；未完成则记录为后续任务。

## 7. 修改清单

下一轮允许在以下范围内修改，仍避免触碰无关模块。

1. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/model/Csp1dProblem.kt`
2. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/model/Csp1dProblemBuilder.kt`
3. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dMilp.kt`
4. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dMilpSolver.kt`
5. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dColumnGeneration.kt`
6. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dRecovery.kt`
7. `ospf-kotlin-framework-csp1d/csp1d-application/src/main/.../application/service/Csp1dProduceContext.kt`
8. `ospf-kotlin-framework-csp1d/csp1d-application/src/test/.../application/service`
9. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce/model`
10. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context/src/main/.../domain/produce/service/pipeline`
11. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context/src/main/.../domain/material/model/ShadowPriceMap.kt`
12. `ospf-kotlin-framework-csp1d/README.md`
13. `ospf-kotlin-framework-csp1d/README_ch.md`
14. `ospf-kotlin-framework-csp1d/daily.md`
15. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

如条件加做 `CGPipeline` 样板需要修改更多 domain pipeline，可限于 CSP1D 子模块内扩展。不要修改 `ospf-kotlin-framework` 通用契约，除非先确认不会影响 Gantt/BPP3D 并扩大验证范围。

## 8. 验收标准

### 8.1 下一轮强制验收

1. `Csp1dSolveConfig` 或等价 public 配置可以注入扩展 pipeline，默认空配置保持兼容。
2. `Csp1dSolveConfigBuilder` 支持 DSL 方式追加扩展。
3. 普通 MILP 能通过 public 扩展入口注册 fake same unit length / same width 类约束。
4. 列生成 LP master 能通过 public 扩展入口注册同类扩展约束。
5. 列生成 final MILP 能通过 public 扩展入口注册同类扩展约束。
6. recovery / partial 路径不丢失扩展配置。
7. 多个扩展 pipeline 可同时注册，执行顺序稳定。
8. 既有 `Csp1dProblem`、`Csp1dMilp`、`Csp1dColumnGeneration`、`Csp1dSchedule`、`Csp1dRecovery` 使用方式不破坏。
9. failure/partial、`LpInfeasible`、`LpSolveFailed`、trace/KPI/render public 语义不变。
10. `V : RealNumber<V>` public 泛型边界保持稳定，不新增面向业务侧的 `Flt64` 唯一入口。
11. 宽度、长度、需求、产出、余料、容量等领域量继续使用 `Quantity<V>` 或明确单位。
12. LP 对偶值到 `V` 的转换必须显式，禁止恢复 `dualValue as? V`。
13. README/README_ch 与实际 API 一致，demo3 不恢复手写 RMP/SP。
14. 不引入 POIT 包名、DTO、公式语言、训练平台、缺陷、分段、位置约束、`unitBatch` 到 framework 主路径。

### 8.2 条件加做验收

若执行 `CGPipeline` 最小样板，则必须满足：

1. 至少一个 shadow price 相关约束管线完成 `extractor()` / `refresh()` 样板或等价桥接。
2. `AbstractCsp1dShadowPriceMap` 能承载该约束的 LP 对偶值。
3. `toShadowPriceMap()` 能把框架 map 转换为 pricing 使用的轻量 `ShadowPriceMap<V>`。
4. 有专项测试覆盖 extractor / refresh / map 转换。
5. 未执行本节时，应在 `daily.md` 记录为后续阶段任务，不影响 8.1 验收。

### 8.3 后续阶段非本轮验收

以下内容不作为下一轮强制验收：

1. `addColumns` 在已有模型上真实增量添加列变量。
2. 需求、物料、设备和目标表达式的增量刷新。
3. `Csp1dColumnGeneration` 全面切换到增量 master。
4. 全量内置 pipeline 改造成 `Csp1dCGPipeline`。

## 9. 最小验证集

执行后必须记录实际结果，不复用历史报告。

1. application acceptance，覆盖 MILP、列生成、Top-K、KPI/render、recovery、warm start、partial solution 和扩展入口。
2. 新增或更新的 public 扩展入口专项测试。
3. 若执行条件加做，运行新增 CGPipeline/shadow price 专项测试。
4. generation 目标测试，至少确认 canonical 结果集合不变。
5. material、yield、length assignment、wasting minimization 子模块测试。
6. Gurobi profile：至少执行 `-Pgurobi-cg-test -DskipTests test-compile`，当前 Gurobi 10 环境可用时执行真实 solver smoke。
7. 门禁搜索：
   - `rg -n "com\\.poit|framework\\.bpp3d" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "rollDemand|weightDemand|sheetDemand" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
   - `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "candidatePlans" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "println\\(" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "DefectCostar|defect|缺陷|segment|分段|positionConstraint|位置约束|unitBatch|formula|公式语言|training|训练平台|history sample|历史样本" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/target/**" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
   - `rg -n "dualValue as\\? V" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `git diff --check -- ospf-kotlin-framework-csp1d`

## 10. 交接提示

1. 本文件是下一会话执行依据；下一轮不要把真实增量 `addColumns` 和 `Csp1dColumnGeneration` 深重构作为强制目标。
2. 下一会话先读 `Csp1dProduceContext`、`Csp1dMilpSolver`、`Csp1dColumnGeneration`、`Csp1dProblem`、`Csp1dProblemBuilder` 和 `Csp1dExtraPipelineTest`。
3. public 扩展入口是主线，先完成它并跑通测试，再决定是否加做 `CGPipeline` 样板。
4. 每一步都要保持 `V : RealNumber<V>`、`Quantity<V>` 和显式 `Flt64 -> V` 转换边界。
5. 扩展接口要留足计算、判断、派生值、过滤、可行性检查、目标系数、shadow price、solution/KPI/render 的后续扩展空间，但本轮实现只要求建模 pipeline 入口闭环。
6. 工作区可能存在非 CSP1D 脏改，不得回滚或混入无关模块。
