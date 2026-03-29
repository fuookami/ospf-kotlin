# OSPF Kotlin Core Daily

日期：2026-03-29

## 本轮结论（对照 ospf-rust-core）
`ospf-kotlin-core` 需要从“前后端分离 + 大量重载入口”的现状，升级到“统一求解入口 + 统一状态回调 + 统一输出模型 + 渐进分层重构”的新架构。

## 对齐原则（确认）
1. `ospf-rust-core` 仅作为架构与行为对齐基线，不做 1:1 代码搬运。
2. 符号运算优先复用 `ospf-kotlin-utils/math/symbol`，`ospf-kotlin-core` 保持薄适配层。
3. `flatten/value/value-range` 缓存改造为独立上下文，命名与阶段语义尽量对齐 Rust，但以 Kotlin 可维护性为先。
4. 架构变更过程中必须保留 Kotlin 既有能力，不得因收敛接口而回退功能。
   - 保留 `MetaModel` 的 `.opm` 导出能力。
   - 保留 `Quantity/PhysicalUnit` 的单位量纲建模能力。
   - 保留 `withRangeSet` 的变量范围自动收紧能力。
   - 保留基于 `ospf-kotlin-utils/math/symbol` 的高级适配能力（解析/JSON/Latex/编译求值与梯度）。
   - 上述能力在每个阶段回归中都必须有对应用例覆盖。

## 事项清单（待落地）
1. 统一求解入口
   - 现状：线性/二次求解接口分别维护，`invoke/solveAsync` 重载过多。
   - 目标：引入 `SolveOptions` 与统一 `solve_with_options` 入口，旧接口仅做兼容转发。
2. 统一建模阶段状态
   - 现状：`RegistrationStatus`、`MechanismModelDumpingStatus`、`IntermediateModelDumpingStatus` 三套并行。
   - 目标：统一为 `ModelBuildingStage/ModelBuildingStatus`，减少 callback 适配成本。
3. 统一求解输出模型
   - 现状：`Feasible/Infeasible` 分型输出，字段较少。
   - 目标：补齐 `iterations/node_count/gap/best_bound/solve_time` 等公共字段，并保留 IIS 信息。
4. MetaConstraint 元数据补齐
   - 现状：已有 `group/lazy/args`，缺 `priority`。
   - 目标：补 `priority` 并打通到中间模型/求解器适配层。
5. 数值转换策略
   - 现状：缺少统一精度转换策略。
   - 目标：引入 `SolveValueConversionPolicy`（先 `Strict/AllowRounding`），先覆盖 `Flt64`，再评估扩展。
6. Callback 模型补完
   - 现状：`MultiObjectCallBackModel` 仍为 TODO。
   - 目标：对齐多目标回调接口，补齐行为与测试。
7. 文档与模块说明
   - 现状：`ospf-kotlin-core` 缺模块级 `README.md/README_ch.md`。
   - 目标：补双语文档、互链、迁移说明与使用入口示例。

## 拆解计划（细化版）

### P0（统一入口与状态骨架，优先落地）

#### P0-1：统一入口对象与扩展接口
1. 新增 `backend/solver/SolveOptions.kt`
   - 字段：`solutionAmount`、`modelBuildingStatusCallback`、`solvingStatusCallback`、`valueConversionPolicy`（先预留）。
   - 提供 builder 风格 API，避免调用点参数爆炸。
2. 新增 `backend/solver/SolverExt.kt`
   - 统一入口：`solve(model)`、`solveWithOptions(model, options)`。
   - 内部自动判断线性/二次路径，统一走 `MetaModel -> MechanismModel -> IntermediateModel -> Solver`。
3. 在 `LinearSolver.kt` 与 `QuadraticSolver.kt` 增加适配入口
   - 旧 `invoke/solveAsync` 保留。
   - 旧入口内部委托到新入口，减少重复流程代码。

P0-1 产物
1. 新文件：`SolveOptions.kt`、`SolverExt.kt`。
2. 旧 API 的兼容转发层。

P0-1 测试
1. 新入口覆盖线性路径。
2. 新入口覆盖二次路径。
3. 旧入口行为与返回值保持不变（回归测试）。

#### P0-2：统一建模状态模型
1. 新增 `frontend/model/status/ModelBuildingStage.kt`
   - 枚举值对齐 Rust 语义：`RegisterTokens/RegisterLinearConstraints/RegisterQuadraticConstraints/RegisterSymbols/FlattenLinearModel/FlattenQuadraticModel/BuildObjective`。
2. 新增 `frontend/model/status/ModelBuildingStatus.kt`
   - 字段：`modelName`、`stage`、`ready`、`total`。
3. 旧状态桥接
   - `RegistrationStatus.kt`
   - `MechanismModelDumpingStatus.kt`
   - `IntermediateModelDumpingStatus.kt`
   - 增加到统一状态的转换函数，确保旧 callback 仍可订阅。

P0-2 产物
1. 统一状态模型与 callback 类型。
2. 三类旧状态到新状态的桥接适配器。

P0-2 测试
1. 状态阶段顺序正确性测试。
2. 旧 callback 与新 callback 并存测试。

P0 验收（DoD）
1. 外部调用不改代码即可继续运行。
2. 新增统一入口可贯通 LP/QP。
3. 状态通知链路统一，且兼容旧接口。

---

### P1（统一输出模型与元数据透传）

#### P1-1：统一 SolverOutput
1. 调整 `backend/solver/output/SolverOutput.kt`
   - 收敛为统一主输出结构（支持可行/不可行状态）。
   - 补字段：`iterations`、`nodeCount`、`bestBound`、`mipGap`、`solveTime`。
2. 兼容旧分型输出
   - 保留旧类型别名或适配器，减少对下游模块冲击。
3. 对齐 `SolvingStatus.kt`
   - 字段命名和含义与统一输出保持一致。

P1-1 测试
1. 线性与二次后端输出字段完整性测试。
2. IIS 分支字段兼容测试。

#### P1-2：补齐 MetaConstraint 优先级
1. 扩展 `frontend/model/mechanism/MetaConstraint.kt`
   - 增加 `priority` 字段。
2. 透传链路改造
   - MetaModel 写入。
   - MechanismModel 保留。
   - IntermediateModel 和 solver adapter 透传。
3. 不支持优先级的求解器行为
   - 明确“忽略且可观测”的策略（日志或诊断字段）。

P1-2 测试
1. `group/lazy/priority/args` 全量透传测试。
2. 不同后端对 `priority` 的兼容行为测试。

P1 验收（DoD）
1. 输出模型可表达 LP/QP/MIP 常见求解状态。
2. 约束元数据可从 Meta 层稳定透传到后端。
3. 旧输出读法有兼容通路。

---

### P2（数值策略、symbol 复用与 callback 补完）

#### P2-1：数值转换策略
1. 新增 `backend/solver/value/SolveValue.kt` 与策略枚举。
2. 首版覆盖 `Flt64`，策略含 `Strict/AllowRounding`。
3. 将策略挂接到 `SolveOptions`，默认行为保持历史兼容。

P2-1 测试
1. 严格模式拒绝精度损失。
2. 允许模式可回落转换。

#### P2-2：symbol 复用与缓存上下文解耦
1. 原则：核心符号运算优先复用 `ospf-kotlin-utils/math/symbol`。
2. `ospf-kotlin-core` 保留薄适配层，逐步减少重复实现。
3. 缓存上下文拆分
   - `FlattenContext`
   - `ValueCacheContext`
   - `RangeCacheContext`
4. 将当前 `TokenTable` 中混合缓存逻辑迁出为上下文对象。

P2-2 测试
1. symbol 适配层 round-trip 测试（保持既有回归通过）。
2. 三类上下文缓存命中/失效/flush 独立性测试。

#### P2-3：Callback 多目标补完
1. 完成 `MultiObjectCallBackModel`。
2. 对齐单目标/多目标公共接口与行为。
3. 补齐初始化解、目标比较、状态回写逻辑。

P2-3 测试
1. 多目标 callback 求解路径回归。
2. 与单目标接口兼容性测试。

#### P2-4：文档补齐
1. 新增 `ospf-kotlin-core/README.md` 与 `README_ch.md`。
2. 双语互链，说明迁移入口、兼容策略、示例调用。
3. 与根仓 README 的定位关系说明（避免重复信息）。

P2 验收（DoD）
1. 转换策略可配置且测试覆盖完整。
2. symbol 复用与缓存上下文方案稳定。
3. callback 多目标功能可用且回归通过。
4. 模块文档完整、双语互链可读。

## 执行顺序建议
1. 先落地 P0（接口骨架与兼容层），再推进 P1（输出与元数据）。
2. P2 放在接口稳定后，避免重复返工。
3. 每阶段结束都执行 `ospf-kotlin-core` 模块测试，并保留回归清单。
4. 每个子阶段完成后记录“已改文件列表 + 风险 + 回滚点”到本文件。

## 风险与注意事项
1. 大量历史重载 API 可能在外部模块有隐式依赖，需保留兼容层过渡。
2. 输出模型调整会影响下游 starter/framework 的解析逻辑，需同步适配。
3. 精度策略引入后，默认行为必须与当前版本保持兼容（除显式开启严格策略外）。

## 阶段记录

### 2026-03-29 / P0 启动（统一入口扩展 + 统一状态桥接）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/frontend/model/status/ModelBuildingStage.kt`
   - `src/main/fuookami/ospf/kotlin/core/frontend/model/status/ModelBuildingStatus.kt`
   - `src/main/fuookami/ospf/kotlin/core/frontend/model/mechanism/RegistrationStatus.kt`
   - `src/main/fuookami/ospf/kotlin/core/frontend/model/mechanism/MechanismModelDumpingStatus.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/intermediate_model/IntermediateModelDumpingStatus.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/SolveOptions.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/SolverExt.kt`
   - `src/test/fuookami/ospf/kotlin/core/backend/solver/SolveOptionsTest.kt`
   - `src/test/fuookami/ospf/kotlin/core/frontend/model/status/ModelBuildingStatusBridgeTest.kt`
2. 风险
   - 统一入口当前以扩展函数提供，旧 `invoke/solveAsync` 尚未改为内部强制委托，存在双入口并存期。
   - `SolveValueConversionPolicy` 当前为占位枚举，后续需按 P2 扩展为 `Strict/AllowRounding`。
   - `solveAsync(options)` 通过统一入口包装实现，后续可在接口主实现中内聚以减少重复入口感知。
3. 回滚点
   - 删除新增文件并回退三处状态文件桥接扩展，即可恢复到改造前行为。

### 2026-03-29 / P2-4 文档补齐（模块双语 README + 互链 + 迁移入口示例）
1. 已改文件列表
   - `README.md`
   - `README_ch.md`
2. 风险
   - 示例代码以统一入口 API 为主，若后续入口签名调整，需要同步更新两份 README 示例。
   - 文档中“渐进迁移”属于策略性建议，若未来决定一次性切换接口，需要同步修订迁移章节。
3. 回滚点
   - 删除新增 `README.md` 与 `README_ch.md` 即可回滚本阶段文档改动。

### 2026-03-29 / P1-2 推进（MetaConstraint.priority 透传到 IntermediateModel）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/intermediate_model/LinearTriadModel.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/intermediate_model/QuadraticTetradModel.kt`
   - `src/test/fuookami/ospf/kotlin/core/backend/intermediate_model/ConstraintPriorityPropagationTest.kt`
2. 风险
   - 当前仅完成到 `IntermediateModel.constraints.priorities` 的链路；各 solver plugin 对 `priority` 的后端透传/忽略日志策略仍需逐个适配。
   - `QuadraticTetradModel.dumpConstraints` 同步修复了固定变量折叠分支对 `rhs` 的累积逻辑，虽然行为更合理，但建议在 plugin 级回归中重点关注固定变量场景。
3. 回滚点
   - 回退上述三个文件即可恢复改造前状态。

### 2026-03-29 / P1-2 推进（solver plugin 层可观测忽略策略）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/intermediate_model/ConstraintPriority.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/UnsupportedFeatureNotice.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt/src/main/fuookami/ospf/kotlin/core/backend/plugins/copt/CoptLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt/src/main/fuookami/ospf/kotlin/core/backend/plugins/copt/CoptQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/fuookami/ospf/kotlin/core/backend/plugins/cplex/CplexLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/fuookami/ospf/kotlin/core/backend/plugins/cplex/CplexQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi/GurobiLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi/GurobiQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi11/GurobiLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi11/GurobiQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly/src/main/fuookami/ospf/kotlin/core/backend/plugins/hexaly/HexalyLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly/src/main/fuookami/ospf/kotlin/core/backend/plugins/hexaly/HexalyQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt/src/main/fuookami/ospf/kotlin/core/backend/plugins/mindopt/MindOPTLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt/src/main/fuookami/ospf/kotlin/core/backend/plugins/mindopt/MindOPTQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek/src/main/fuookami/ospf/kotlin/core/backend/plugins/mosek/MosekLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/fuookami/ospf/kotlin/core/backend/plugins/scip/ScipLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/fuookami/ospf/kotlin/core/backend/plugins/scip/ScipQuadraticSolver.kt`
2. 风险
   - 当前策略是“统一 warn + 忽略优先级”，未按后端能力做细粒度支持开关，后续如某些后端支持约束优先级可再替换为真实透传。
   - 日志输出位于 `dump(model)` 阶段，每次求解会按模型非空优先级数量输出一次 warn。
3. 回滚点
   - 删除 `ConstraintPriority.kt`、`UnsupportedFeatureNotice.kt` 并回退上述 14 个 plugin 文件即可恢复到改造前行为。

### 2026-03-29 / P2-1 推进（SolveValueConversionPolicy 首版落地）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/value/SolveValue.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/SolveOptions.kt`
   - `src/test/fuookami/ospf/kotlin/core/backend/solver/SolveOptionsTest.kt`
2. 风险
   - 当前仅完成策略模型与默认行为落地（`AllowRounding` 兼容历史行为），尚未把策略接入具体后端数值转换分支。
   - 原占位值 `Default` 已移除为 `Strict/AllowRounding`，若外部调用直接依赖旧枚举值，需同步调整。
3. 回滚点
   - 回退上述三个文件即可恢复改造前策略定义。

### 2026-03-29 / P2-1 推进（Strict 策略接入统一入口前置校验）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/value/SolveValueValidation.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/SolverExt.kt`
   - `src/test/fuookami/ospf/kotlin/core/backend/solver/value/SolveValueValidationTest.kt`
2. 风险
   - 当前严格策略校验范围为 `LinearTriadModelView/QuadraticTetradModelView` 的约束与目标值（NaN/Infinity 拒绝），尚未覆盖 meta/mechanism 阶段及 plugin 内部所有转换分支。
   - 严格策略目前定义为“数值合法性严格”，尚未加入更细粒度的“舍入损失阈值”控制。
3. 回滚点
   - 回退上述三个文件即可恢复为无前置严格校验行为。

### 2026-03-29 / P2-1 推进（Strict 校验范围扩展到变量边界与初始值）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/value/SolveValueValidation.kt`
   - `src/test/fuookami/ospf/kotlin/core/backend/solver/value/SolveValueValidationTest.kt`
2. 风险
   - 严格策略下会拦截带 `Infinity` 的变量边界，这与部分求解器通过无界边界表达模型的习惯可能冲突，后续需根据目标语义确认是否应对“显式无穷边界”做白名单处理。
3. 回滚点
   - 回退上述两个文件即可恢复扩展前校验范围。

### 2026-03-29 / P0-1 推进（统一入口流水线内聚 + 机制层旧入口转发）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/SolverExt.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/LinearSolver.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/QuadraticSolver.kt`
2. 风险
   - `MetaModel` 旧入口（携带 `RegistrationStatusCallBack/MechanismModelDumpingStatusCallBack`）仍保留旧链路，尚未完全通过统一入口强制委托，后续可继续收敛。
   - 多解返回（`Ret<Pair<FeasibleSolverOutput, List<Solution>>>`）旧入口仍走历史路径，以避免丢失 `solutions` 明细，暂未统一到 `solveWithOptions` 主返回模型。
   - 当前环境缺少 `gradlew/gradle`，本次改动未完成模块级自动化回归执行。
3. 回滚点
   - 回退上述三个文件即可恢复改造前入口路由行为。

### 2026-03-29 / P0-1 续推（MetaModel 单解无 IIS 入口转发）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/LinearSolver.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/QuadraticSolver.kt`
2. 风险
   - 当前仅把 `MetaModel` 的“单解 + 无 IIS”入口在 `dump(meta, old callbacks)` 之后转发到统一入口；`IIS` 与“多解返回”入口仍走历史路径。
   - 当前环境仍缺少 `gradlew/gradle`，本次续推未执行自动化回归测试。
3. 回滚点
   - 回退上述两个文件即可恢复改造前 `MetaModel` 入口实现。

### 2026-03-29 / P2-3 推进（MultiObjectCallBackModel 落地 + 多目标回调测试）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/frontend/model/callback/CallBackModel.kt`
   - `src/test/fuookami/ospf/kotlin/core/frontend/model/callback/MultiObjectCallBackModelTest.kt`
2. 风险
   - 当前 `MultiObjectCallBackModel` 采用按 `objectiveLocation` 顺序的词典序比较；若后续希望支持 Pareto/epsilon-constraint 等非词典序策略，需要在接口层增加可配置比较策略。
   - 默认 `addObject(category, variable/constant)` 会落到 `objectiveLocation` 的第一个位置；若业务需要强制指定优先级，调用侧应使用带 `location` 的重载。
3. 回滚点
   - 回退上述两个文件即可恢复 `MultiObjectCallBackModel` 为未实现状态。

### 2026-03-29 / P1-1 推进（统一输出模型补齐到不可行分支 + 兼容测试）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/output/SolverOutput.kt`
   - `src/test/fuookami/ospf/kotlin/core/backend/solver/output/SolverOutputCompatibilityTest.kt`
2. 风险
   - 本次通过新增 `UnifiedSolverOutput` 接口统一 `iterations/nodeCount/bestBound/mipGap/solveTime` 字段；不可行输出默认这些字段为 `null`，调用侧若直接按非空读取需做空值分支。
   - 当前仅完成 core 输出模型与兼容测试，尚未在各 plugin 中补充不可行分支的统计字段回填（仍保持默认 `null`）。
3. 回滚点
   - 回退上述两个文件即可恢复改造前输出模型定义。

### 2026-03-29 / P2-1 推进（策略上下文接入 + COPT 转换分支落地）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/value/SolveValueConversionContext.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/SolverExt.kt`
   - `src/test/fuookami/ospf/kotlin/core/backend/solver/value/SolveValueConversionContextTest.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt/src/main/fuookami/ospf/kotlin/core/backend/plugins/copt/CoptLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt/src/main/fuookami/ospf/kotlin/core/backend/plugins/copt/CoptQuadraticSolver.kt`
2. 风险
   - 当前策略下沉先覆盖 COPT plugin，其他 plugin 仍主要依赖统一入口前置校验，尚未全部改成 `toSolverDouble` 分支校验。
   - `Strict` 在 COPT 建模阶段会额外拒绝 `Infinity` 值；若业务依赖无穷边界表达“无界变量”，后续需设计白名单或边界特判策略。
3. 回滚点
   - 回退上述五个文件即可恢复到策略下沉前行为。

### 2026-03-29 / P2-1 续推（转换策略扩展到多 plugin + 联编验证）
1. 已改文件列表
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/fuookami/ospf/kotlin/core/backend/plugins/cplex/CplexLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/fuookami/ospf/kotlin/core/backend/plugins/cplex/CplexQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi/GurobiLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi/GurobiQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi11/GurobiLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi11/GurobiQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly/src/main/fuookami/ospf/kotlin/core/backend/plugins/hexaly/HexalyLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly/src/main/fuookami/ospf/kotlin/core/backend/plugins/hexaly/HexalyQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt/src/main/fuookami/ospf/kotlin/core/backend/plugins/mindopt/MindOPTLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt/src/main/fuookami/ospf/kotlin/core/backend/plugins/mindopt/MindOPTQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek/src/main/fuookami/ospf/kotlin/core/backend/plugins/mosek/MosekLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/fuookami/ospf/kotlin/core/backend/plugins/scip/ScipLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/fuookami/ospf/kotlin/core/backend/plugins/scip/ScipQuadraticSolver.kt`
2. 验证
   - 执行 `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile`。
   - 结果：`core + framework + 8 个 plugin` 联编通过。
3. 风险
   - 当前 plugin 内大部分数值转换已改为 `toSolverDouble`，但并非每个调用点都携带字段路径参数；`Strict` 触发时的定位信息粒度仍可继续提升。
   - `Strict` 对无穷值的拒绝语义已在多后端生效；若业务需要“显式无穷边界”，仍需设计白名单或边界特判策略。
4. 回滚点
   - 回退上述 13 个 plugin 文件即可恢复本阶段之前的转换分支行为。

### 2026-03-29 / P2-1 续推（toSolverDouble 字段路径补齐 + 无参调用清零）
1. 已改文件列表
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt/src/main/fuookami/ospf/kotlin/core/backend/plugins/copt/CoptLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt/src/main/fuookami/ospf/kotlin/core/backend/plugins/copt/CoptQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/fuookami/ospf/kotlin/core/backend/plugins/cplex/CplexLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex/src/main/fuookami/ospf/kotlin/core/backend/plugins/cplex/CplexQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi/GurobiLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi/GurobiQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi11/GurobiLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11/src/main/fuookami/ospf/kotlin/core/backend/plugins/gurobi11/GurobiQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly/src/main/fuookami/ospf/kotlin/core/backend/plugins/hexaly/HexalyLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly/src/main/fuookami/ospf/kotlin/core/backend/plugins/hexaly/HexalyQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt/src/main/fuookami/ospf/kotlin/core/backend/plugins/mindopt/MindOPTLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt/src/main/fuookami/ospf/kotlin/core/backend/plugins/mindopt/MindOPTQuadraticSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek/src/main/fuookami/ospf/kotlin/core/backend/plugins/mosek/MosekLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/fuookami/ospf/kotlin/core/backend/plugins/scip/ScipLinearSolver.kt`
   - `../ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/fuookami/ospf/kotlin/core/backend/plugins/scip/ScipQuadraticSolver.kt`
2. 完成项
   - 对上述主流 plugin 的 `toSolverDouble` 调用补齐字段路径参数（变量边界/初值、约束 lhs/rhs/bounds、目标系数/常数、配置 gap）。
   - 目标范围内 `LinearSolver.kt/QuadraticSolver.kt` 的无参 `toSolverDouble()` 调用清零。
3. 验证
   - 执行 `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile`。
   - 结果：`core + framework + 8 个 plugin` 联编通过。
4. 风险
   - 字段路径字符串目前依赖手工约定命名，后续若要统一可观测规范，建议提炼常量或 helper，降低命名漂移风险。
   - 当前 `Strict` 语义仍是“拒绝 NaN/Infinity”，尚未引入“舍入损失阈值”策略。
5. 回滚点
   - 回退上述 15 个 plugin 文件即可恢复本阶段改造前的参数命名行为。

### 2026-03-30 / P1-1 续推（IIS 不可行输出补齐统一统计字段）
1. 已改文件列表
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/LinearSolver.kt`
   - `src/main/fuookami/ospf/kotlin/core/backend/solver/QuadraticSolver.kt`
2. 完成项
   - 在 `iisConfig` 分支包装 `solvingStatusCallBack`，捕获最后一次 `SolvingStatus`。
   - `LinearInfeasibleSolverOutput/QuadraticInfeasibleSolverOutput` 在 IIS 场景回填 `iterations/nodeCount/bestBound/mipGap/solveTime`。
   - 当失败前未触发 callback 时，`solveTime` 使用调用耗时作为兜底回填，避免统一输出出现空耗时。
3. 验证
   - 执行 `mvn -pl ospf-kotlin-core -am -DskipTests compile`，结果 `BUILD SUCCESS`。
4. 风险
   - 当前回填依赖“最后一次 callback 状态”；若某后端在失败前未触发 callback，`iterations/nodeCount/bestBound/mipGap` 仍可能为 `null`（`solveTime` 已有耗时兜底）。
   - 仅覆盖 core 侧 IIS 输出路径；plugin 若未来直接构造不可行输出，仍需各自补齐字段。
5. 回滚点
   - 回退上述两个 solver 文件即可恢复改造前行为。
