# 约束规划求解器插件扩展计划

## 1. 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | Proposed |
| 日期 | 2026-08-02 |
| 前置计划 | 原 CP 基础计划已归档至 `plans/release.md`，已完成 SCIP direct CP 与 Gurobi MIP-backed CP |
| 公共契约 | 本文件“后续插件强制公共合同”；已完成源码证据见 `plans/release.md` |
| 承接范围 | SCIP/Gurobi 首阶段合同已关闭；其余插件的通用求解合同与 CP 支持由本计划承接 |
| 服务端联动 | `E:/workspace/ospf/ospf/framework/remote-solver/daily.md` |
| 必做范围 | Gurobi 11、COPT、CPLEX、MindOPT、Mosek 的 MIP-backed CP 验证与能力声明 |
| 条件范围 | CPLEX CP Optimizer、Hexaly 原生/启发式 CP、Lingo、OPTVerse、通用 heuristic CP、JSCIPOpt 上游增强 |
| 上游阻塞 | JSCIPOpt 源码修改当前为 `BlockedByUpstream`，获得明确修改/合并权限或采用受维护 fork 后方可启动 |
| 兼容策略 | 复用 core CP AST、精确 lowerer、统一报告和三态 capability，不复制插件私有 CP 协议 |

## 2. 背景与当前状态

已归档 CP 基础计划只对以下两个求解器插件声明并验收了生产 CP 路径：

- `ospf-kotlin-core-plugin-scip`：提供独立 `ScipConstraintProgrammingSolver`，混合使用 native handler 和精确分解。
- `ospf-kotlin-core-plugin-gurobi`：通过 `MipBackedConstraintProgrammingSolver(GurobiLinearSolver())` 求解可精确降为 MIP 的 CP 子集。

core 中的 `MipBackedConstraintProgrammingSolver` 接收任意 `LinearSolver`，但“类型上可以注入”不等于对应插件已经支持 CP。每个插件仍需证明：

- 整数变量、线性约束、目标和辅助变量被正确编译与求解。
- 求解终态、incumbent、目标值、best bound、gap、超时和取消能无损映射。
- 后端不会把未证明的结果提升为 Optimal、Infeasible 或 Verified proof。
- warm start、callback、中断和 native 资源生命周期满足统一 solver 契约。
- 插件的实际 native library、license 和版本组合通过显式集成测试。

本计划同时承接非 SCIP/Gurobi 插件的通用合同迁移。每个目标插件在进入 CP 验收前，
还必须完成下节适用的 `SolveReport`、`SolverDescriptor`、provenance、取消、诊断、指纹和
golden/replay 适配；不能用 CP wrapper 绕过底层求解合同缺陷。

### 2.1 后续插件强制公共合同

本节由已关闭的 `OSPF-SOL-001～029`、CP2 和 CP3 源码计划迁移而来，是删除原计划文件后
所有未验收插件的直接前置合同。SCIP 与现有 Gurobi 插件的完成证据、历史任务映射和发布边界
见 `plans/release.md`；公共类型已经存在不代表任一其他插件已经通过本节验收。

| 合同面 | 插件必须满足的语义 | 未完成时的声明 |
| --- | --- | --- |
| 报告与错误通道 | `SolveReport<V>` 正交保留 `ProblemStatus`、`TerminationReason`、`SolutionPresence`、proof、solution、statistics、diagnostics、provenance 和 fingerprints。已启动且能形成可信报告的最优、可行、不可行、限制、取消和 backend failure 返回 `Ok(SolveReport)`；无法形成可信报告的输入、环境或内部合同错误返回 `Failed`/`Fatal` | `NeedsAdapterWork` |
| 能力与来源 | `SolverDescriptor`/`SolverCapabilities` 只发布已验证的模型、IIS、dual/Farkas、warm start、解池、中断和 checkpoint 能力；`SolverProvenance` 记录 backend/plugin/native 版本、实际参数、线程、随机种子、确定性模式和脱敏环境摘要 | `Unsupported`、`BlockedBySDK` 或 `BlockedByLicense` |
| 取消与资源 | 每次调用使用独立取消句柄；幂等取消必须触发可信的 native interrupt/terminate。attempt 的取消原因在 backend 完成边界冻结，迟到取消不得污染 `Cancelled`、`Failed` 或 `Fatal` 审计；求解结束后释放 native 资源 | `Unsupported` |
| 身份与 provenance | stable 元素必须由调用方或规范化模型提供稳定 source，并在机制模型、triad/tetrad、native artifact、诊断、组合 attempt、远程 DTO 和 checkpoint 中保留 `id/namespace/schemaVersion/scope/origin/provenance`。无法证明跨重建稳定时必须使用 `ModelLocal` | 仅 `ModelLocal`，禁止 portable 绑定 |
| 诊断 | 约束求值保留 lhs/rhs/relation/slack/violation/tolerance；IIS、Farkas、冲突或 fallback 证据保留 source、exactness、completeness、成员 ID 和失败原因。诊断失败不得覆盖已确定的 Infeasible/Unbounded 等结论 | `Unsupported` 或准确的 legacy/fallback 等级 |
| 指纹 | 使用带 schema 版本的规范化模型和确定数值编码生成 model/configuration/solver fingerprints；不得使用 JVM 对象哈希，敏感配置不得明文进入报告 | `NeedsAdapterWork` |
| 组合与 attempt | 串并行、线性、二次和解池路径保留全部 attempts、父子关系、backend identity、状态、耗时、错误、provenance、fingerprints 与完成时取消事实；聚合报告保留全部来源，全部失败时保留原始错误集合 | 不得进入组合路由 |
| 远程协议 | 版本化 DTO 保持任务生命周期与求解结论正交，无损往返 incumbent、objective、bound/gap、诊断、provenance、fingerprints 和 run/attempt；未知版本、字段缺失、artifact/digest 或归属不一致返回结构化错误 | 不得进入远程 solver selection |
| 正确性与重放 | 固定 fixture 覆盖 Optimal、Feasible、Infeasible、Unbounded、Unknown、限制终止、取消、数值和 backend 故障；golden/replay 校验目标容差、残差、bound、诊断和指纹，SDK/license 缺失显式记为 skipped/unsupported | 不计入已验收矩阵 |

CP 接入还必须满足以下冻结边界：

1. stable identity、统一报告和 capability 协商先于插件 CP 路由；vendor 私有 DTO 不得替代公共合同。
2. portable checkpoint 只保存可复验 snapshot、incumbent、审计字段和 primitive/领域可序列化 payload；
   `RebuildFromSnapshot`、MIP start、reoptimization 和 native search resume 必须分开声明。
3. MIP lowering 只有通过等价性门禁才能声明 `ExactLowering`；存在厂商 API 不等于 `Native`，
   未验证能力必须是 `Unsupported`，不能静默回退或提升 proof。
4. 未接入稳定身份的插件继续保持 `ModelLocal`；未通过报告、取消、诊断、指纹和 replay 门禁的插件
   不得进入远程 capability、Exact Benders 或生产 solver selection。

当前未声明 CP 支持的插件盘点如下：

| 插件 | 当前基础 | CP 计划分类 | 初始优先级 |
| --- | --- | --- | --- |
| Gurobi 11 | 已实现 `LinearSolver`，未做 CP 集成测试 | Gurobi MIP-backed 路径兼容补齐 | P0 |
| COPT | 已实现 `LinearSolver`，无插件测试 | 精确 MIP-backed | P1 |
| CPLEX | 已实现 `LinearSolver`，无插件测试 | 精确 MIP-backed；CP Optimizer 条件探针 | P1 |
| MindOPT | 已实现 `LinearSolver`，无插件测试 | 精确 MIP-backed | P1 |
| Mosek | 已实现 `LinearSolver`，无插件测试 | 精确 MIP-backed/MIO | P1 |
| Hexaly | 已实现 `LinearSolver`，但证明和启发式语义待审计 | 条件式 MIP-backed 或原生/启发式 CP | P2 |
| Lingo | `LingoLinearSolver` 尚未实现统一接口 | 先完成基础插件，再评估 MIP-backed | P3/Blocked |
| OPTVerse | `OPTVerseLinearSolver` 尚未实现统一接口 | 先完成基础插件，再评估 MIP-backed | P3/Blocked |
| Heuristic | 只有领域无关元启发式算法，不是 `LinearSolver` | 单独的 Heuristic CP 路径 | P3/Conditional |

SCIP 与现有 Gurobi 插件作为 reference backend，不重复纳入本计划的“新增支持”计数，但必须参与 differential test。

## 3. 目标

- 为所有具备可信整数线性求解能力的插件提供一致的 MIP-backed CP 接入与显式验收。
- 为可能具备原生约束建模能力的 CPLEX CP Optimizer 和 Hexaly 建立有证据的条件式原生路线。
- 为只能提供 incumbent 的启发式后端建立不夸大证明的 CP 输出边界。
- 对尚未实现 `LinearSolver` 的插件明确阻塞条件，不用占位类宣称 CP 支持。
- 让本地和远程 solver selection 只消费已验证的 capability，不根据插件名称猜测能力。
- 保持 SCIP direct CP、Gurobi MIP-backed CP 和新增插件之间的模型、状态与报告语义一致。
- 独立管理 JSCIPOpt binding、SCIP direct CP 条件增强及其后置 OSPF/remote 接入，不阻塞其他插件交付。

## 4. 非目标与原则

- 不引入、不依赖、不适配 OR-Tools。
- 不为每个 MIP 插件复制一份 CP AST compiler；精确 MIP 路径统一复用 core lowerer。
- 不把 exact formulation 等同于 native CP；通过 MIP lowerer 的能力只能声明 `ExactLowering`。
- 不因厂商产品包含 CP、MIP 或全局约束功能就直接声明支持，必须先完成目标版本 API 探针。
- 不把启发式求得的最好解标记为已证明最优，也不把未找到解标记为已证明不可行。
- 不让 vendor/JNI/native 对象进入 core CP AST、snapshot、远程 payload、`SolveReport` 或 portable checkpoint。
- 不要求所有插件覆盖所有 CP feature；未实现能力通过 capability 返回 `Unsupported`。
- 不因 native library 或 license 在 CI 不可用而伪造通过；必须显式记录 skipped/unsupported 原因。

## 5. 统一支持层级

### 5.1 MIP-backed 基线

首轮插件扩展统一以现有 `ConstraintProgrammingToLinearModelLowerer` 为语义来源，目标能力为：

| CP feature | 目标支持级别 | 约束 |
| --- | --- | --- |
| Boolean logic | `ExactLowering` | 精确二进制编码 |
| Reification | `ExactLowering` | 只有在有限界和 big-M 可证明安全时支持 |
| Sparse domain | `ExactLowering` | 受枚举和辅助变量规模门禁限制 |
| AllDifferent | `ExactLowering` | 受 domain 与分解规模门禁限制 |
| Element | `ExactLowering` | index/domain 必须有限且可枚举 |
| Allowed/Forbidden table | `ExactLowering` | 受 tuple 数和 no-good 规模门禁限制 |
| Fixed interval | `ExactLowering` | `start + duration = end` |
| Optional/variable-duration interval | `ExactLowering` | presence、duration domain 与回填必须双向等价 |
| NoOverlap | `ExactLowering` | 精确顺序二进制 formulation |
| Assumption/fixed binding | `ExactLowering` | 每次重建，不宣称 native incremental |
| Solution hint | capability-dependent | 仅后端可信支持 MIP start 时声明 |
| Cumulative | `Unsupported` | core lowerer 当前未声明通用支持 |
| Circuit/Automaton/Reservoir | `Unsupported` | core MIP-backed 当前未声明通用支持 |
| Conflict core | `Unsupported` | 除非另有可验证 backend analyzer |

具体插件的实际 capability 可以弱于该基线，不能强于 lowerer 与 backend 两者能力的交集。

### 5.2 支持完成定义

一个插件只有同时满足以下条件，才能在文档和运行时声明 CP 支持：

1. 提供明确的 CP 构造入口或有文档的 `MipBackedConstraintProgrammingSolver` 组合方式。
2. `SolverDescriptor.modelTypes` 和 `constraintProgrammingFeatures` 与真实能力一致。
3. 通过共享 MIP-backed contract test。
4. 与 Fake 小规模穷举 oracle 及 SCIP/Gurobi reference backend 完成双向 differential test。
5. 覆盖 Optimal、Feasible、Infeasible、Unknown、TimeLimit、Cancelled、BackendFailure 及有/无 incumbent。
6. 验证 objective、best bound、gap 和 proof 不被错误提升。
7. 验证重复求解、取消、异常和 session 关闭后的 native 资源释放。
8. 更新中英文 README、capability matrix、示例和 native 运行前置条件。

### 5.3 Capability 发布门禁

插件 descriptor 只能发布已经验证的能力；存在实现类、可加载 JNI 或求解器名称都不是能力证据。每个 feature 必须同时给出：

| 门禁 | 必须提供的证据 | 未通过时的声明 |
| --- | --- | --- |
| 语义 | 与 Fake 穷举 oracle 及 SCIP/Gurobi reference 的可行集、目标、终态 differential | `Unsupported` 或 `NeedsAdapterWork` |
| 身份 | stable/model-local scope、origin 映射和独立重建回归 | 仅 `ModelLocal`，禁止 portable 绑定 |
| 报告 | solution presence、objective、bound、gap、proof、diagnostics 和 provenance 的无损映射 | `NeedsAdapterWork` |
| 运行 | 取消、超时、异常、重复 close 和资源释放 | `BlockedBySDK`、`BlockedByLicense` 或 `Unsupported` |
| 恢复 | 仅声明已验证的 rebuild/reuse/reoptimization；不把 MIP start 当 native resume | `PortableRebuildOnly` |

能力矩阵必须按插件、版本和平台记录。缺失任一证据时，路由和远程 capability 协商必须拒绝该 feature，而不是静默降级到语义更弱的实现。

### 5.4 共享插件测试模板

每个已声明 CP feature 至少复用以下固定模板，并将完整日志和 fixture 留在仓库外：

1. 构造最小可行、不可行和边界模型，验证 stable 与 model-local identity 的传播。
2. 覆盖 `Optimal`、带 incumbent 的限制终止、无 incumbent 的限制终止、`Infeasible`、`Unknown`、`Cancelled` 和 `BackendFailure`。
3. 对 objective、best bound、gap、proof、diagnostics、provenance 和 fingerprints 做逐字段断言。
4. 运行重建对照；若声明 reuse/reoptimization，再覆盖重复调用、错误 stage、取消和 close 后资源状态。
5. 对不支持的 feature 断言结构化 `Unsupported`，并确认不会进入 Exact Benders 或远程路由。

## 6. 分阶段实施计划

### Phase SCP-0：基线、探针与共享契约

- [ ] `OSPF-SCP-000` 为每个目标插件建立本文件 2.1 节公共合同差距表，覆盖报告、状态、provenance、取消、诊断、指纹和 replay；不适用能力明确为 `Unsupported`。
- [ ] `OSPF-SCP-001` 冻结“插件支持 CP”的完成定义、三态 capability 和 proof 映射规则。
- [ ] `OSPF-SCP-002` 为九个目标插件记录 SDK/native library 版本、license、平台、线程限制和 CI 可用性。
- [ ] `OSPF-SCP-003` 审计各 `LinearSolver` 的整数变量、状态、目标、best bound、gap、warm start、interrupt 和资源释放实现。
- [ ] `OSPF-SCP-004` 建立共享 `MipBackedConstraintProgrammingSolver` contract fixture，覆盖布尔、reification、稀疏值域、AllDifferent、Element、Table、interval 和 NoOverlap。
- [ ] `OSPF-SCP-005` 建立共享终态 fixture，禁止从 `feasible=false`、空解或厂商状态字符串推断更强结论。
- [ ] `OSPF-SCP-006` 建立 native dependency/license 不可用时的显式 skip 规范，区分未运行、unsupported 与通过。
- [ ] `OSPF-SCP-007` 与本文件 2.1 节的稳定 ID、统一报告和 capability 冻结点对齐，不在插件中复制公共 DTO。
- [ ] `OSPF-SCP-008` 在每个插件进入 MIP-backed/native/heuristic CP 测试前，先通过适用的公共状态映射、取消、provenance 和资源合同。
- [ ] `OSPF-SCP-009` 为具备原生 IIS/Farkas/diagnostic API 的目标插件建立 analyzer 探针；未验证时继续使用准确的 legacy fallback 来源与证据等级。
- [ ] `OSPF-SCP-010` 将通过的插件加入扩展 golden/replay 矩阵；在此之前不得计入本计划的插件完成统计。

验收：每个目标插件都有 `Ready`、`NeedsAdapterWork`、`BlockedBySDK`、`BlockedByLicense` 或 `NotApplicable` 的证据结论；共享 fixture 可被插件测试复用。

### Phase SCP-1：Gurobi 11 兼容补齐（P0）

- [ ] `OSPF-SCP-101` 对比 Gurobi 与 Gurobi 11 插件的变量、状态、callback、hint、bound/gap 和关闭语义。
- [ ] `OSPF-SCP-102` 使用 `MipBackedConstraintProgrammingSolver(gurobi11.GurobiLinearSolver())` 接入 CP。
- [ ] `OSPF-SCP-103` 复用现有 Gurobi CP 集成 fixture，验证两个版本的可行集、目标和终态一致。
- [ ] `OSPF-SCP-104` 增加 Gurobi 11 与 SCIP/Gurobi 的 differential test。
- [ ] `OSPF-SCP-105` 补齐 Gurobi 11 的 CP README、capability 和 license/环境说明。

验收：Gurobi 11 在其独立插件中达到现有 Gurobi MIP-backed CP 的同等语义边界；版本差异不泄露到 core。

### Phase SCP-2：COPT、CPLEX、MindOPT、Mosek 精确 MIP-backed（P1）

- [ ] `OSPF-SCP-201` 为 COPT 完成 integer/status/bound/gap/hint/interrupt 探针并接入共享 CP contract。
- [ ] `OSPF-SCP-202` 为 CPLEX MIP 完成相同探针并接入共享 CP contract；本任务不等同于 CP Optimizer native 支持。
- [ ] `OSPF-SCP-203` 为 MindOPT 完成相同探针并接入共享 CP contract。
- [ ] `OSPF-SCP-204` 为 Mosek MIO 完成相同探针并接入共享 CP contract。
- [ ] `OSPF-SCP-205` 对每个插件增加最小构造入口、准确 descriptor 和 feature capability。
- [ ] `OSPF-SCP-206` 对每个插件运行布尔、table、optional interval、variable duration、NoOverlap 和不支持能力负路径测试。
- [ ] `OSPF-SCP-207` 与 SCIP/Gurobi reference backend 执行随机小模型 differential test，固定种子并保存 fingerprint。
- [ ] `OSPF-SCP-208` 验证超时有 incumbent、超时无 incumbent、取消、数值失败和 license/backend failure。
- [ ] `OSPF-SCP-209` 验证重复创建/关闭、异常退出、callback 中断和 native 内存不持续增长。

验收：每个通过的插件可以独立声明 MIP-backed CP；任一插件失败不阻碍其他插件关闭，但必须保持 `Unsupported` 并记录证据。

### Phase SCP-3：Hexaly 路线判定（P2，条件式）

- [ ] `OSPF-SCP-301` 审计当前 `HexalyLinearSolver` 的可行性、最优性、不可行性和 bound 证明语义，禁止沿用不可信的线性状态映射。
- [ ] `OSPF-SCP-302` 探测目标 Hexaly/LocalSolver API 对整数 domain、逻辑、table、list、interval、NoOverlap、Cumulative 和中断的真实能力。
- [ ] `OSPF-SCP-303` 若线性入口能正确求解 exact MIP formulation，先接入 MIP-backed CP，但按真实证明能力返回 Feasible/Unknown。
- [ ] `OSPF-SCP-304` 若原生 API 能双向等价表达 OSPF CP feature，制作独立 adapter 原型并建立 origin ID 映射。
- [ ] `OSPF-SCP-305` 对 MIP-backed、原生原型、SCIP 和 oracle 执行 differential test，并比较 build/solve 时间、解质量和峰值内存。
- [ ] `OSPF-SCP-306` 结论限定为 `AcceptedMipBacked`、`AcceptedNative`、`HeuristicOnly`、`RejectedByEvidence` 或 `BlockedBySDK`。

验收：只有能证明的终态才进入 Verified；HeuristicOnly 路径只返回 incumbent/Unknown，不生成 Exact Benders 证书。

### Phase SCP-4：CPLEX CP Optimizer 原生探针（P2，条件式）

- [ ] `OSPF-SCP-401` 确认目标依赖是否包含受支持的 `IloCP` API、native library 和可用 license；当前 `ilog:cplex` 占位依赖不能作为能力证据。
- [ ] `OSPF-SCP-402` 探测整数表达式、AllDifferent、Element、Table、interval、optional interval、NoOverlap、Cumulative、Automaton、conflict/refineConflict 和 search status。
- [ ] `OSPF-SCP-403` 若 API 完整，建立独立 `CplexConstraintProgrammingSolver`，不得通过 `CplexLinearSolver` 暴露 CP Optimizer 对象。
- [ ] `OSPF-SCP-404` 映射 stable/origin ID、solution、objective、终止原因、proof、conflict 和资源释放。
- [ ] `OSPF-SCP-405` 与 SCIP、CPLEX MIP-backed 和小规模 oracle 做双向 differential test。
- [ ] `OSPF-SCP-406` 未满足 API、license、等价性或证明门禁时，以 `RejectedByEvidence`/`BlockedBySDK` 关闭，保留 CPLEX MIP-backed 路径。

验收：native CP 与 MIP-backed 是两个独立 capability；不存在从 CPLEX 产品名推断 native 支持的隐式逻辑。

### Phase SCP-5：Lingo 与 OPTVerse 基础阻塞解除（P3）

- [ ] `OSPF-SCP-501` 先分别完成符合统一接口的 `LingoLinearSolver` 和 `OPTVerseLinearSolver`；空类不进入 CP adapter。
- [ ] `OSPF-SCP-502` 覆盖基础 LP/MIP 变量、约束、目标、状态、值回填、bound/gap、中断和资源测试。
- [ ] `OSPF-SCP-503` 验证 SDK 是否支持可信整数求解；若仅支持外部脚本/文件协议，定义明确的进程与 artifact 边界。
- [ ] `OSPF-SCP-504` 基础 `LinearSolver` contract 通过后，再接入共享 MIP-backed CP contract。
- [ ] `OSPF-SCP-505` SDK、license 或基础插件仍不可用时保持 `BlockedBySDK`，不为满足计划勾选而建立伪实现。

验收：基础线性/MIP 插件先独立完成；CP 支持不得掩盖或绕过底层插件缺陷。

### Phase SCP-6：通用 heuristic CP（P3，条件式）

- [ ] `OSPF-SCP-601` 定义独立 `HeuristicConstraintProgrammingSolver` 语义，只承诺搜索可行 incumbent，不承诺最优或不可行证明。
- [ ] `OSPF-SCP-602` 复用 CP snapshot evaluator、值域和约束校验，不把 CP 模型错误转换为惩罚值后静默接受。
- [ ] `OSPF-SCP-603` 建立可插拔 neighborhood/population SPI，避免为 GA、PSO、SA 等算法复制 CP adapter。
- [ ] `OSPF-SCP-604` 对每个输出 incumbent 复验变量值域、interval、全部约束和目标值。
- [ ] `OSPF-SCP-605` 对无解、超时、取消和搜索失败统一返回 Unknown/None，不生成 conflict、IIS 或 Exact Benders cut。
- [ ] `OSPF-SCP-606` 仅在代表性模型上有可复现价值时接入具体 heuristic；否则以 `RejectedByEvidence` 关闭。

验收：heuristic CP 与 exact/native CP 在 descriptor、proof 和使用入口上可机器区分，不能被 Exact Benders 选择。

### Phase SCP-7：远程路由、文档与发布

- [ ] `OSPF-SCP-701` 将已验收插件 capability 接入远程 node descriptor 和 solver selection；未验收插件不得接受 CP task。
- [ ] `OSPF-SCP-702` 远程结果继续使用 CP2 统一报告和稳定 artifact，不新增 vendor 私有 DTO。
- [ ] `OSPF-SCP-703` 为每个插件记录 SDK/native/license/平台兼容矩阵和安装前置条件。
- [ ] `OSPF-SCP-704` 更新 core CP、各插件和远程求解服务端的中英文 README/capability matrix。
- [ ] `OSPF-SCP-705` 汇总共享 contract、differential、集成、资源和性能结果；跳过项逐项说明。
- [ ] `OSPF-SCP-706` 完成全量编译、全量测试、可用插件 verify 和 `git diff --check`。

验收：本地与远程按相同 capability 选择插件；插件不可用时返回结构化 unsupported/backend failure，不静默切换到语义更弱的后端。

## 7. 插件验收矩阵

| 插件 | Shared contract | SCIP/Gurobi differential | 终态/proof | hint/interrupt | 资源 | 远程 capability |
| --- | --- | --- | --- | --- | --- | --- |
| Gurobi 11 | 必须 | 必须 | 必须 | 必须 | 必须 | 通过后启用 |
| COPT | 必须 | 必须 | 必须 | 按探针 | 必须 | 通过后启用 |
| CPLEX MIP | 必须 | 必须 | 必须 | 按探针 | 必须 | 通过后启用 |
| MindOPT | 必须 | 必须 | 必须 | 按探针 | 必须 | 通过后启用 |
| Mosek MIO | 必须 | 必须 | 必须 | 按探针 | 必须 | 通过后启用 |
| Hexaly | 路线决定后必须 | 必须 | 强制降级门禁 | 按探针 | 必须 | 仅验收能力 |
| CPLEX CP Optimizer | 独立 native contract | 必须 | 必须 | 按探针 | 必须 | 仅验收能力 |
| Lingo | 基础接口通过后 | 必须 | 必须 | 按探针 | 必须 | 仅验收能力 |
| OPTVerse | 基础接口通过后 | 必须 | 必须 | 按探针 | 必须 | 仅验收能力 |
| Heuristic | 独立 heuristic contract | 可行性对照 | 不允许 Verified | 必须 | 必须 | 仅 Heuristic mode |

### 7.1 Stable identity 适配矩阵

在插件通过 shared contract 前，稳定身份只允许保留为 `ModelLocal`；以下矩阵是每个插件进入
`Stable`/portable 路径前必须完成的适配任务：

| 插件 | 当前 identity 状态 | 进入 Stable 前置条件 | 远程/恢复策略 |
| --- | --- | --- | --- |
| SCIP | `ModelLocal`/部分 artifact origin 已有映射 | native artifact 一对多映射、presolve 投影和独立重建回归 | 仅 portable rebuild |
| Gurobi | `ModelLocal`/部分 artifact origin 已有映射 | native row/column 投影、辅助元素隔离和重建回归 | 仅 portable rebuild |
| Gurobi 11 | `Unsupported` | 完成 SCP-1 contract、版本探针和 identity differential | 不进入 CP 路由 |
| COPT / CPLEX MIP / MindOPT / Mosek MIO | `Unsupported` | 基础 LinearSolver contract、Stable origin 投影和 CP differential | 不进入 CP 路由 |
| Hexaly / Lingo / OPTVerse | `NeedsAdapterWork` | 路线判定、状态/proof 合同和 identity adapter | 不进入 Exact 路由 |
| CPLEX CP Optimizer | `BlockedBySDK` | 独立 native contract、license/API 探针和 identity 映射 | 不进入 MIP-backed 路由 |
| Heuristic | `ModelLocal` | 只需可行 incumbent 复验，不得生成 Stable proof/cut | 仅 Heuristic mode |

每个插件的 `Stable` 状态必须同时记录 SDK/native/plugin 版本、scope/origin 映射、辅助元素
规则和独立重建测试；未满足时不得因为存在同名类型或可加载 native library 而提升能力。

## 8. JSCIPOpt 上游与 SCIP 插件条件能力

### 8.1 状态、基线与重新启动条件

| 项目 | 内容 |
| --- | --- |
| 当前状态 | `BlockedByUpstream` |
| 上游工作区 | `E:/workspace/JSCIPOpt` |
| 已核实基线 | 本地历史基线 `c1fff4100f61be85b1612b22744bde3bc5514e58`；`origin/master` 已包含至 `f7642d0` 的 event handler 变更 |
| 建议分支 | `feature/ospf-cp-native-capabilities` |
| 阻塞原因 | 当前不能继续修改、合并或发布 JSCIPOpt 上游源码 |
| 重新启动条件 | 获得明确上游修改/合并权限，或批准采用有版本和维护责任人的 fork |
| 阻塞期策略 | 保持 model rebuild、assumptions/shrink、`ExactLowering` 和 portable checkpoint |

JSCIPOpt 使用 CMake、SWIG 和 JNI 生成 JAR/native library，最低声明支持 SCIP 8.0。
当前绑定已有 `solve/solveConcurrent/interruptSolve/getStage/writeTransProblem`、固定时长 cumulative，
且上游 event handler 分支增加了 transformed variable/constraint 与 LP row 相关能力；尚未形成
可发布、可验收的 `freeSolve/freeTransform`、reoptimization、probing、增量 bound、结构化 conflict
和生命周期测试闭环。

阻塞期间不得在 `E:/workspace/JSCIPOpt` 创建计划分支、恢复已撤销实验或用未提交工作树制品更新
OSPF 默认依赖。建议分支名仅保留为解阻后的执行约定，创建时必须从同步后的 `origin/master` 开始。

### 8.2 能力决策

| 能力 | 上游可处理程度 | 当前决策 | 准确边界 |
| --- | --- | --- | --- |
| stage 与资源生命周期 | 可以 | `BlockedByUpstream`，解阻后实施 | 补齐 binding、高层 Java API 和释放测试 |
| original problem 同进程复用 | 可以 | `BlockedByUpstream`，解阻后原型 | `freeTransform` 后更新并重求，不保留搜索树 |
| probing 与临时传播 | 可以 | `BlockedByUpstream`，解阻后原型 | 只声明 propagation/cutoff，不冒充完整求解 |
| SCIP reoptimization | 部分可以 | `BlockedByUpstream`，解阻后原型 | 先验证目标变化；bound/constraint 修改逐项探针 |
| 一般 assumption 增量子问题 | 未证明 | `BlockedByProbe` | 不由 API 名称推断支持 |
| 结构化 conflict graph/core | 需额外 callback/DTO | `BlockedByProbe` | DOT/日志/计数不能作为生产证据 |
| optional fixed-duration interval | 公共 cumulative 不直接支持 | `RejectedByCurrentPublicApi` | indicator/cumulative 组合仍是 `ExactLowering` |
| variable-duration interval | 公共 cumulative duration 为常量 | `RejectedByCurrentPublicApi` | 需要独立 constraint handler 才能重新评估 |
| 跨进程 native checkpoint/resume | 没有公开完整导入 API | `RejectedByUpstreamApi` | problem export/reoptimization 不是搜索树恢复 |

JSCIPOpt 能消除 Java binding 缺失，但不能凭空增加 SCIP 公共 API 或改变 cumulative 语义。
所有结论必须按 binding、SCIP 主版本和平台分别记录。

### Phase J0：分支、版本与测试基础

- [ ] `OSPF-SCP-J001` 阻塞解除后同步 `origin/master`，从最新 event handler 基线创建
  `feature/ospf-cp-native-capabilities`，记录 base commit，不覆盖上游变更。
- [ ] `OSPF-SCP-J002` 增加唯一 binding 版本，并在 JAR manifest、Java API 和 native 构建信息中可读取；
  同版本制品不得被覆盖。
- [ ] `OSPF-SCP-J003` 暴露 SCIP major/minor/technical/API 版本和 compile/runtime capability；
  JAR 与 native library 不匹配时快速失败。
- [ ] `OSPF-SCP-J004` 建立 SCIP 8.x、9.2.x、10.x 兼容矩阵；若放弃已声明的 SCIP 8.x，
  必须提高最低版本并按破坏性变更发布。
- [ ] `OSPF-SCP-J005` 在 CMake 中接入 CTest/Java integration test，测试只能加载本次构建的 JAR/native。
- [ ] `OSPF-SCP-J006` 固化 SWIG regeneration，校验 `scipjni.i`、generated Java 和
  `scipjni_wrap.cxx` 同步，CI 重生成后无未提交差异。

验收：每个支持版本均能构建；至少 9.2.x、10.x 能执行最小运行测试；版本失配可预测失败。

### Phase J1：生命周期与安全 Java API

- [ ] `OSPF-SCP-J101` 绑定 `SCIPfreeSolve(restart)`、`SCIPfreeTransform()`、`SCIPrestartSolve()`，
  明确允许 stage 与调用后 stage。
- [ ] `OSPF-SCP-J102` 提供高层 Java 生命周期 API，覆盖未 create、重复 free、错误 stage、solve 中修改、
  interrupt 后清理，调用方不操作 SWIG output pointer。
- [ ] `OSPF-SCP-J103` 补齐 original/transformed variable/constraint 映射；transform 释放后旧 wrapper
  必须失效，不能在下一轮复用。
- [ ] `OSPF-SCP-J104` 为 variable、constraint、solution、event handler 和 row 建立 capture/release 所有权表，
  正常、异常和 interrupt 路径均释放资源。
- [ ] `OSPF-SCP-J105` 覆盖 create-solve-freeSolve-solve、freeTransform-modify-solve、interrupt-cleanup、
  错误 stage 和 100～1000 轮重复生命周期测试。

验收：非法生命周期不会造成 native crash、悬空引用或持续内存增长。

### Phase J2：原模型复用与增量更新

- [ ] `OSPF-SCP-J201` 绑定 original variable lower/upper bound、objective 及必要的 linear side/coef 更新 API；
  方法名和文档必须区分 original/transformed 与允许 stage。
- [ ] `OSPF-SCP-J202` 将 `freeTransform -> 修改 original problem -> solve` 定义为
  `ReusableOriginalModel`，不得宣称保留搜索树或 reoptimization。
- [ ] `OSPF-SCP-J203` 仅在确有用例时绑定 constraint add/delete/enable/disable；disable model constraint
  必须有对称恢复、可行性保护和负路径测试。
- [ ] `OSPF-SCP-J204` 覆盖 bound 放宽/收紧/固定/解除、目标/RHS 变化、可行到不可行及反向变化，
  与 fresh rebuild 做逐轮差分。
- [ ] `OSPF-SCP-J205` 不支持或失败的修改立即使 session 失效并要求 rebuild，不能继续使用可能污染的模型。

验收：所有声明的修改与 fresh rebuild 在状态、解、目标、bound 和证明上等价。

### Phase J3：probing 与传播

- [ ] `OSPF-SCP-J301` 绑定 start/new-node/backtrack/end/in-probing/depth API。
- [ ] `OSPF-SCP-J302` 绑定 probing lower/upper bound、fix 和 propagation，用 Java value object 返回
  cutoff、domain reductions 和传播轮次。
- [ ] `OSPF-SCP-J303` 正常、cutoff、异常、取消和 early return 均必须 end probing；临时状态不得泄漏。
- [ ] `OSPF-SCP-J304` 以穷举小模型验证域收紧和 cutoff；probing LP/propagation 不产生完整 CP/MIP 终态或 Verified IIS。

### Phase J4：SCIP reoptimization

- [ ] `OSPF-SCP-J401` 绑定 enable/is-enabled/free-reopt-solve/change-reopt-objective 和必要统计。
- [ ] `OSPF-SCP-J402` 先实现 SCIP 文档明确支持的目标变化，记录相似度、presolve、cut、seed、limit
  和累计统计语义。
- [ ] `OSPF-SCP-J403` 对 bound、constraint side/activation/add/delete 逐项验证 API 与 stage；
  objective-only 能力不能推断其他修改受支持。
- [ ] `OSPF-SCP-J404` 覆盖连续最优/可行/不可行、限制终止、取消后重试、方向变化和空目标，
  与 rebuild 差分并检查旧 incumbent/proof 不污染新报告。
- [ ] `OSPF-SCP-J405` 分别声明 `ReoptimizationObjectiveOnly`、`ReoptimizationBounds` 和
  `ReoptimizationConstraints`，不能合并成单一布尔值。

### Phase J5：结构化 conflict 探针

- [ ] `OSPF-SCP-J501` 评估 public conflict analysis、conflict constraint/event handler、probing cutoff
  和 implication graph 是否能稳定提取 bound/constraint 成员。
- [ ] `OSPF-SCP-J502` 可提取时定义无 native pointer 的 Java DTO，包含成员、bound、来源、validity、
  completeness、run 和 stage。
- [ ] `OSPF-SCP-J503` 需要自定义 conflict handler 时，单独处理 callback 生命周期、Java exception 隔离、
  handler ownership、并发限制和 event handler 兼容。
- [ ] `OSPF-SCP-J504` 在 OSPF SCIP plugin 中投影 origin ID；不完整或最终复验失败时降级为
  `Claimed`/`Unverified` 或 assumptions/shrink。
- [ ] `OSPF-SCP-J505` 公共 API 无法提供稳定结构化证据时记录 `RejectedByEvidence`，
  不解析日志或 DOT 文件进入生产。

### Phase J6：optional/variable interval 决策

- [ ] `OSPF-SCP-J601` 对目标 SCIP 8/9/10 及后续版本探测 cumulative/scheduling/optional job API，
  明确 duration、presence、demand 是常量还是变量。
- [ ] `OSPF-SCP-J602` JSCIPOpt helper 若只组合 indicator、linear、cumulative 或 big-M，
  capability 保持 `ExactLowering`，不得标为 `Native`。
- [ ] `OSPF-SCP-J603` 只有代表性排程基准证明现有 lowering 是显著瓶颈，才单独立项 native C/C++
  constraint handler；先评估 optional fixed duration，再独立评估 variable duration。
- [ ] `OSPF-SCP-J604` 自定义 handler 覆盖 copy/transform/delete、presolve、propagation、enforcement、
  feasibility、conflict resolving、interrupt 和资源释放，并与 Fake/MIP oracle 穷举差分。
- [ ] `OSPF-SCP-J605` variable duration 只有直接维护 start/end/duration/presence/resource 双向语义时
  才可声明 `Native`；否则继续精确 lowering。

### Phase J7：native checkpoint/resume 上游边界

- [ ] `OSPF-SCP-J701` 按 SCIP 主版本检查能否公开导出并导入开放节点、tree、cut/solution pool、
  pseudocost、random state、参数和证明连续性。
- [ ] `OSPF-SCP-J702` 没有完整公开导入 API时保持 `RejectedByUpstreamApi`，禁止绑定 `struct_reopt`、
  `struct_tree` 或序列化进程内存。
- [ ] `OSPF-SCP-J703` 业务价值成立时向 SCIP 上游提交最小需求；仅在稳定公开 API 发布后另建原型。
- [ ] `OSPF-SCP-J704` portable checkpoint 始终是公共恢复契约；native artifact 只能是同版本/平台的可选加速层。

### Phase J8：JSCIPOpt 发布

- [ ] `OSPF-SCP-J801` 更新 README/INSTALL 的版本矩阵、加载方式、线程/stage、reoptimization/probing、
  资源所有权和 unsupported 能力。
- [ ] `OSPF-SCP-J802` 执行完整 CMake/CTest/Java integration matrix，保存 configure/build/test 完整日志。
- [ ] `OSPF-SCP-J803` 生成唯一 binding 版本、JAR/native artifact、checksum 和 release notes。
- [ ] `OSPF-SCP-J804` SWIG regenerate、`git diff --check` 和发布提交检查通过后，才允许 OSPF 升级依赖。

### Phase J9：ospf-kotlin SCIP plugin 消费

- [ ] `OSPF-SCP-J901` 更新 JSCIPOpt 依赖和加载说明；solver provenance/fingerprint 纳入 binding、SCIP
  API/native 和插件版本，不包含路径或敏感参数。
- [ ] `OSPF-SCP-J902` 运行时逐项探测 reusable model、probing、各类 reoptimization、structured conflict
  和 native resume；编译成功不能自动声明 capability。
- [ ] `OSPF-SCP-J903` 旧 binding/SCIP 保持 rebuild、assumptions/shrink 和 portable checkpoint，并报告降级原因。
- [ ] `OSPF-SCP-J904` 先实现 SCIP plugin 内部 session；只有性能门禁通过才评审 backend-neutral core SPI。
- [ ] `OSPF-SCP-J905` session 状态机覆盖 Created、Compiled、Transformed、Solving、Solved、Interrupted、Closed；
  错误通过 `Ret` 返回，native exception 在 adapter 边界转换。
- [ ] `OSPF-SCP-J906` stable origin/artifact registry 维护 original/transformed 映射，每次 transform generation
  更新 native handle，公共 ID 保持稳定。
- [ ] `OSPF-SCP-J907` 实现 original-model reuse；修改不支持或状态污染时丢弃 session 并 rebuild。
- [ ] `OSPF-SCP-J908` probing 只用于传播/cutoff/候选证据，正式终态来自完整 solve；reoptimization 按细分能力启用。
- [ ] `OSPF-SCP-J909` 取消、超时、异常和 close 均释放资源，不复用状态不明 session。
- [ ] `OSPF-SCP-J910` reuse/reoptimization 与 rebuild 做状态、解、目标、bound、proof、conflict、diagnostics、
  cancellation 和 stable ID 差分。
- [ ] `OSPF-SCP-J911` 使用穷举 oracle 与 Gurobi MIP-backed 对照 assumptions、interval、NoOverlap/Cumulative
  和 Benders cut 有效性。
- [ ] `OSPF-SCP-J912` 同机同版本基准记录 build/transform/solve/propagate/shrink、p50/p95、节点和峰值内存；
  rebuild/compile 占比至少 20% 且中位端到端改善至少 15%、p95/内存无不可接受回归才默认启用。

### Phase J10：remote-solver 条件接入

- [ ] `OSPF-SCP-J1001` calculator 内部 session 不改变远程模型/结果协议；provenance 可记录实际执行模式，
  客户端不能要求 native session。
- [ ] `OSPF-SCP-J1002` node capability 新增字段时使用向后兼容 schema，旧节点字段缺失等价于 false。
- [ ] `OSPF-SCP-J1003` slice/checkpoint 继续以 portable checkpoint 为跨进程边界；进程结束不宣称延续 native tree。
- [ ] `OSPF-SCP-J1004` DTO/fingerprint/capability/artifact 变化锁步更新 fixture、migration、HTTP E2E、README
  和 remote-solver `daily.md`。
- [ ] `OSPF-SCP-J1005` 同一 worker 内 session 绑定 run/model/config/solver fingerprint；task/attempt 切换强制释放，
  worker 重启后 portable rebuild。

### 8.3 JSCIPOpt 测试、构建与发布门禁

| 层级 | 必测内容 |
| --- | --- |
| binding | SCIP 版本、SWIG/JNI 签名、retcode、JAR/native 失配 |
| lifecycle | stage、重复 solve/free、freeTransform、interrupt、异常清理、长循环资源 |
| reuse | bound/目标/RHS、放宽/收紧、可行/不可行双向变化、rebuild 差分 |
| probing | fix、传播、cutoff、backtrack/end、无状态泄漏、穷举 oracle |
| reoptimization | 目标/方向、限制/取消、统计、版本差异、rebuild 差分 |
| conflict | 成员、origin、validity/completeness、最终复验、降级 |
| interval | fixed/optional/variable、presence、horizon、resource、oracle |
| OSPF/remote | runtime capability、fingerprint、fallback、session 隔离、portable rebuild |

解阻后每个目标 SCIP 版本分别保存完整 configure/build/test 输出：

```powershell
$jscipLogDir = Join-Path $env:TEMP 'ospf-solver-cp-jscip-verification'
New-Item -ItemType Directory -Force -Path $jscipLogDir | Out-Null

cmake -S . -B build -DSCIP_DIR='<scip-cmake-dir>' *> (Join-Path $jscipLogDir 'configure.log')
cmake --build build --config Release *> (Join-Path $jscipLogDir 'build.log')
ctest --test-dir build -C Release --output-on-failure *> (Join-Path $jscipLogDir 'test.log')
git diff --check
```

进入 J9/J10 后，ospf-kotlin 和 remote-solver 按各自仓库规则执行完整构建/测试/verify，
每条命令只执行一次并完整保存输出。

### 8.4 阻塞期完成定义

本工作包当前以以下状态收敛，不要求伪造上游实现：

1. `BlockedByUpstream` 原因、基线、建议分支和重新启动条件已记录。
2. OSPF runtime capability 保持 false，model rebuild 与 portable checkpoint 可用。
3. optional/variable interval 继续 `ExactLowering`，native checkpoint 继续 `RejectedByUpstreamApi`。
4. 其他插件的 SCP-0～SCP-7 不受 JSCIPOpt 阻塞影响。

获得上游权限或批准 fork 后，必须按 J0～J10 顺序执行；不能直接从 OSPF session 或远程 capability 开始。

## 9. 与已关闭公共合同和远程服务端的执行关系

- SCP-0 的插件探针和共享 contract 可以立即开展。
- stable identity 公共合同已经冻结；各插件通过 2.1 节身份门禁后才能定稿 stable/origin ID 投影。
- 统一报告公共合同已经冻结；各插件通过 2.1 节报告门禁后才能定稿终态、proof、statistics、provenance 和 fingerprint 映射。
- `OSPF-SOL-013/022/023` 与通用 Benders checkpoint 源码合同已经关闭；领域 serializer 事项由 `plans/release.md` 管理，JSCIPOpt 工作包只由本计划跟踪。
- 插件本地 contract 与 differential test 通过后，才能更新远程服务端 `daily.md` 的 node capability。
- checkpoint 合同已经冻结；插件仍须独立证明恢复级别，MIP start 只表示 solution hint，不得声明 native resume。
- 条件式 native/heuristic 工作不阻塞 Gurobi 11 和 P1 MIP-backed 插件交付。
- J0～J10 在上游解阻前不得启动，也不阻塞其他插件形成 Verified/Blocked 结论。

推荐顺序：

```text
SCP-0 共享契约与插件探针
  -> SCP-1 Gurobi 11
  -> SCP-2 COPT/CPLEX MIP/MindOPT/Mosek
  -> SCP-7 基础远程路由与发布

SCP-3 Hexaly 路线判定 -----------\
SCP-4 CPLEX CP Optimizer 探针 ----+-> 条件能力验收 -> SCP-7 增量发布
SCP-5 Lingo/OPTVerse 基础修复 ----+
SCP-6 Heuristic CP ---------------/

JSCIPOpt BlockedByUpstream
  -> 解阻后 J0 分支/版本/测试
  -> J1/J2 生命周期与模型复用
  -> J3/J4 probing 与 reoptimization
  -> J5/J6/J7 conflict、interval、checkpoint 结论
  -> J8 发布
  -> J9 OSPF SCIP plugin 消费
  -> J10 remote-solver 条件接入
```

## 10. 主要风险与控制措施

| 风险 | 影响 | 控制措施 |
| --- | --- | --- |
| `LinearSolver` 可注入即被误认为已支持 | 未验证插件产生错误结果 | 插件级 contract、differential 和 capability 门禁 |
| exact lowerer 与后端状态语义混淆 | 提升 Optimal/Infeasible/proof | formulation 与 proof 分开验收，后端状态取能力交集 |
| vendor SDK/版本差异 | 编译通过但运行崩溃 | 记录版本矩阵、native probe、显式 profile |
| license 缺失被当作测试通过 | 形成虚假支持声明 | skipped/unsupported/verified 三类结果分离 |
| CPLEX CP Optimizer 与 CPLEX MIP 混用 | native capability 虚标 | 独立 solver、descriptor、测试和依赖探针 |
| Hexaly/heuristic 结果被当作 exact | Benders 错误收敛 | HeuristicOnly、无 Verified proof、禁止 Exact mode |
| Lingo/OPTVerse 占位类被包装 | 运行时才失败 | 基础 `LinearSolver` contract 作为硬前置 |
| 插件重复实现 lowerer | 语义漂移和维护分叉 | core lowerer 为唯一 MIP formulation 来源 |
| 远程节点过早声明 capability | CP 任务被路由到半成品插件 | 本地验收后再启用远程 capability |
| JSCIPOpt 上游阻塞被绕过 | 使用不可维护的本地二进制或未合并 API | 只有上游权限或批准 fork 才启动 J0 |
| original/transformed handle 混用 | native crash 或错误解 | generation、stage、所有权和长循环资源测试 |
| reoptimization/probing 被泛化 | 错误终态、证明或 IIS | 细分 capability、fresh rebuild 差分、最终复验 |
| helper 被标为 native interval | capability 欺骗 | 组合 formulation 始终标为 `ExactLowering` |
| 私有结构被用于 checkpoint | 版本崩溃和不可恢复 | 只接受稳定公开导入 API，portable-first |

## 11. 关闭标准

本计划按插件独立关闭，不要求所有条件式插件都实现，但整体关闭必须满足：

1. Gurobi 11、COPT、CPLEX MIP、MindOPT 和 Mosek 均有明确的 Verified、BlockedBySDK 或 BlockedByLicense 结论。
2. 所有 Verified 插件先通过本文件 2.1 节适用的公共报告、状态、provenance、取消、诊断、指纹和 replay 合同，再通过 CP shared contract、cross-solver differential、终态/proof 和资源测试。
3. CPLEX CP Optimizer、Hexaly、Lingo、OPTVerse 和 Heuristic 均形成有证据的 Accepted、RejectedByEvidence、Blocked 或 NotApplicable 结论。
4. JSCIPOpt 工作包至少保持有证据的 `BlockedByUpstream`，或在解阻后完成 J0～J10 的逐项结论；
   不能以本地未发布工作树替代上游制品。
5. 未通过验收的 feature 和插件继续声明 `Unsupported`，没有静默 fallback 或近似语义冒充 exact/native。
6. 本地与远程 capability matrix 一致，远程服务端只路由到已验收插件。
7. stable ID、统一报告、artifact 和 checkpoint 复用本文件 2.1 节公共契约，没有 vendor 私有协议泄露。
8. 中英文文档、最小示例、SDK/license/平台矩阵和失败语义已同步。
9. 全量编译、全量测试、可用 native 插件集成测试和 `git diff --check` 通过；所有 skipped 项有明确原因。

## 12. 构建与验收策略

任务进行中优先运行受影响插件及 core 的增量编译和定向测试，每条 Maven 命令的完整输出保存到仓库外日志。单个 native SDK/license 不可用时，不重复运行全量构建，应从同一日志确认阻塞原因并记录 capability 结论。

每个插件至少执行：

```powershell
$solverCpModule = 'ospf-kotlin-core-plugin-copt'
mvn compile test-compile -pl "ospf-kotlin-core-plugin/$solverCpModule" -am -T 0.75C
mvn test -pl "ospf-kotlin-core-plugin/$solverCpModule" -am -T 0.75C
mvn verify -pl "ospf-kotlin-core-plugin/$solverCpModule" -am -T 0.75C
```

所有已接受实现完成后，按项目规则执行一次全量收尾验收：

```powershell
$solverCpLogDir = Join-Path $env:TEMP 'ospf-solver-cp-verification'
New-Item -ItemType Directory -Force -Path $solverCpLogDir | Out-Null
$solverCpCompileLog = Join-Path $solverCpLogDir 'compile.log'
$solverCpTestLog = Join-Path $solverCpLogDir 'test.log'

mvn clean compile test-compile -T 0.75C *> $solverCpCompileLog
mvn test -T 0.75C *> $solverCpTestLog
git diff --check
```

每条构建命令只执行一次并完整利用日志；禁止通过 `head`、`tail` 或截断管道反复启动 Maven。
