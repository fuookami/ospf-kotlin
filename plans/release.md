# OSPF CP 发布与环境验收归档

## 发布状态

当前 CP/远程能力保持 `ImplementedWithBoundaries`。本文件集中记录发布前必须在部署环境完成的验证，不把本地单元测试、内存 E2E 或隔离 Maven 安装当作生产环境证据。

### 已关闭计划迁移登记（2026-08-10）

CP2、CP3 与首阶段求解执行契约计划的有效信息已经迁入本文件和 `plans/solver_cp.md`，原计划文件已删除。
本文件是已完成源码合同、发布边界、历史任务编号和外部验收事项的唯一权威记录；
`plans/solver_cp.md` 是未验收插件、条件式 native 能力和 JSCIPOpt 后续工作的唯一权威计划。

| 原计划范围 | 关闭状态 | 已完成或已决策范围 | 删除后承接位置 |
| --- | --- | --- | --- |
| `OSPF-SOL-001～029` | `ClosedForScipAndGurobi` | 正交报告、错误通道、capability/provenance、取消、稳定身份、诊断、指纹、组合 attempt、远程 DTO、golden/replay 与离线实验基础设施 | 本节记录首阶段关闭合同；其余插件适配由 `plans/solver_cp.md` 承接 |
| `OSPF-CP2-001～704` | `ImplementedWithBoundaries` | CP 稳定身份、统一报告、portable checkpoint v2、Benders primitive document、精确 lowering 和兼容策略均完成；条件式 JSCIP/native 能力按证据拒绝 | 本文件保留完成边界与发布事项；重新评估由 `plans/solver_cp.md` 承接 |
| `OSPF-CP3-A001～A106/A201～A204/A206/A401～A404` | `Closed` | 公共合同所有权、全链路身份、远程报告、本地终态矩阵和文档闭环完成 | 本节保留源码关闭证据 |
| `OSPF-CP3-A205` | `Transferred` | 本地 protocol/dispatcher/calculator 与 fixture 已完成 | `RELEASE-CP-205` |
| `OSPF-CP3-A301～A306` | `BlockedByDomainOwner` | 通用 Benders checkpoint 基础已完成，仓库缺少生产领域 serializer 样例 | `RELEASE-CP-301～306` |

#### 已关闭公共求解合同

以下任务编号继续作为兼容、回归和审计标识，不因原计划文件删除而失效：

| 任务编号 | 已关闭合同 |
| --- | --- |
| `OSPF-SOL-001～004` | `SolveReport<V>` 正交表达问题状态、终止原因、解存在性、证明、统计、诊断、来源与指纹；已启动且可形成可信报告的终态返回 `Ok(SolveReport)`，输入、环境或内部合同错误且无法形成可信报告时返回 `Failed`/`Fatal` |
| `OSPF-SOL-005～009` | `SolverDescriptor`、`SolverCapabilities` 与 `SolverProvenance` 记录实际模型能力、版本、配置、线程、种子、确定性和脱敏环境；SCIP/Gurobi 已验收，其他插件不得据类型存在宣称支持 |
| `OSPF-SOL-010～012` | 本地、组合、协程、`CompletableFuture` 与远程停止共享幂等取消语义；取消原因和资源释放必须以 backend 执行边界内的事实为准 |
| `OSPF-SOL-013～017` | stable/model-local 身份贯穿机制模型、triad/tetrad、已验收 SCIP/Gurobi 及已接入的组合、诊断和序列化路径；约束求值、IIS/Farkas/冲突证据保留来源、精度与完整度，诊断失败不覆盖既有求解结论。其他插件继续受 `plans/solver_cp.md` 的 `ModelLocal`/`Unsupported` 门禁约束 |
| `OSPF-SOL-018～023` | 规范化模型与确定数值编码生成 model/configuration/solver fingerprints；串并行组合保留全部 attempts、选择依据、失败集合与取消事实；版本化远程 DTO 无损保留线性/二次报告 |
| `OSPF-SOL-024～029` | SCIP/Gurobi fixture、golden/replay、批量实验、生成式样本和离线观测设施已完成；离线学习默认不得改变生产参数或分支决策 |

#### CP2/CP3 源码交付映射

- `OSPF-CP2-001～105`：冻结 capability、身份 scope 与兼容期，并完成 CP variable、constraint、objective、interval、artifact 和 Benders binding 的 stable/model-local 身份传播、冲突校验与跨重建回归。
- `OSPF-CP2-201～206`：完成 CP 统一报告、版本化远程 artifact、Int64/interval 数学复验、正交终态和 legacy DTO 兼容；真实部署进程的完整差分仍归 `RELEASE-CP-205`。
- `OSPF-CP2-301～307`：完成 portable checkpoint v2、validated incumbent、assumption/conflict、Benders iteration/cut pool、primitive serialization SPI、v1 读取与损坏/错配拒绝。恢复级别是 `RebuildFromSnapshot`，不是 native 搜索树续跑；领域 payload serializer 由领域 owner 提供。
- `OSPF-CP2-401～406/501～507/601～605`：JSCIP 增量/probing/conflict、SCIP native optional/variable interval 和 native resume 均因 API 或等价性证据不足以 `RejectedByEvidence` 关闭；继续使用 rebuild、exact lowering 和 portable checkpoint，不发布伪 native capability。
- `OSPF-CP2-701～704`：双语文档、迁移示例、兼容窗口和本地验收矩阵完成；native library/license 不可用按 `Skipped` 或 `RejectedByEvidence` 记录，不计为通过。
- `OSPF-CP3-A001～A106`：完成唯一 owner、稳定身份 source、规范化与 provenance、SCIP/Gurobi native artifact 投影、组合/诊断/checkpoint/remote 传播，以及重建、重名、冲突和序列化测试。
- `OSPF-CP3-A201～A204/A206`：完成 backend-neutral 远程 `SolveReport`、线性/二次 adapter、canonical fixture、版本与 artifact 负路径、完整本地终态矩阵；A205 的部署部分按下方移交表继续验收。
- `OSPF-CP3-A401～A404`：完成 SCIP/Gurobi、direct/remote/combinatorial、linear/quadratic/CP 的本地终态矩阵、关闭证据、后续插件前置条件和双语文档同步。

#### 冻结的兼容边界

- **主求解入口**：`Ret<SolveReport<V>>` 是 1.1.0 源码线唯一主入口；旧 `FeasibleSolverOutput` facade 已在未发布源码线移除。显式 IIS 入口仍可物化线性/二次 infeasible artifact，但不是主求解结果，也不承载新增终态字段。
- **身份与规范化**：`NormalizedMathematicalModel` 使用 schema `1.1`，canonical fingerprint 包含 namespace、schema、scope、主 origin 和按 `(kind, key)` 去重排序的完整 provenance。scope 读取兼容历史大小写、连字符和下划线别名，写入统一为 `stable`/`model-local`；兼容字符串 origin 映射为 `ModelElementOrigin("legacy-origin", origin)`，与显式 provenance 冲突时拒绝。
- **远程协议**：新 CP/result/report artifact 统一写 v2 并校验 schema 主版本、run/attempt 归属、raw/resultRef、digest 和正交状态；旧 v1 只经显式 legacy 分支读取，未知未来主版本、伪装 legacy、字段/归属/摘要不一致返回结构化错误。两仓库 canonical CP v2 `SerializedSolution` fixture 的 SHA-256 为 `EBAD2593BDD9279BFB1D536F1174A328AB774C93B64102234AB52599B26C8691`；线性/二次 identity provenance 新列表以空列表兼容旧 JSON，但不得据缺失元数据提升为 stable。
- **portable checkpoint**：唯一写入版本是 v2，v1 仅兼容读取；损坏、截断、未知 schema、模型/配置/solver fingerprint 错配、伪装来源或 capability 降级必须返回 `Ret` 错误或结构化 warning。v2 恢复仅接受明确可重建的三套历史配置指纹算法，以及固定 `sha256("scip-cp")` 或可精确重建的 `scip-runtime-1` 候选；不得凭 backend 名称或版本猜测 legacy。
- **SCIP runtime identity**：当前 `scip-runtime-2` 由构建/plugin 版本、实际 SCIP/native 与 binding 版本及原生库内容 SHA-256 构成；搜索路径、JAR URL、文件大小/mtime 和 `java.library.path` 不进入 solver identity，只在脱敏 provenance 中记录加载方式。无法证明旧路径代与文件属性完全一致时，跨路径 checkpoint 不兼容。
- **Benders document**：checkpoint 只允许带 schema、digest、fingerprints 和稳定成员 ID 的 primitive 或领域显式序列化 payload；闭包、native handle 和任意业务对象不得进入 document。包含 master 状态时必须提供显式恢复适配器，真实领域 serializer 与跨进程接入继续由 `RELEASE-CP-301～306` 管理。

#### 最新源码关闭证据（2026-08-10）

提交 `e0bca1eb4` 完成公共合同迁移，随后 `4efac629a`、`589307646`、`e5089f188`、
`b9db32a86` 与 `ae0b01fbb` 依次补齐 solver fingerprint、聚合 provenance、稳定多目标身份、
完整 attempt trace、无 incumbent 的 first-feasible 语义，以及 backend 完成时不可变的取消事实。
截至 `ae0b01fbb`，80 个模块全量编译与测试成功：Surefire `3332 tests, 0 failures,
0 errors, 6 skipped`，SCIP Failsafe 29 项、Gurobi Failsafe 11 项全部通过，
`git diff --check` 通过。该证据关闭本地源码合同，不替代下方部署和领域 owner 验收。

本次计划迁移收尾基于 `a677da935` 及删除三份旧计划后的工作树再次执行
`mvn clean compile test-compile -T 0.75C` 与 `mvn test -T 0.75C`，80 个模块均
`BUILD SUCCESS`；503 份 Surefire 报告合计 `3333 tests, 0 failures, 0 errors, 6 skipped`。
完整日志位于 `D:/temp/ospf-plan-retirement-20260810/final-clean-compile-test-compile.log` 和
`D:/temp/ospf-plan-retirement-20260810/final-test.log`。本次未重复运行 SCIP/Gurobi Failsafe，
其最近一次权威结果仍为上一段记录的 29/11 项。

### 移交任务登记（2026-08-07）

以下任务由原 CP3 计划正式移交本文件管理（2026-08-07 审查收尾确认），
每个交付项只有本文件一个实施所有者（满足 `OSPF-CP3-A001`“唯一实施所有者”要求）。

| 任务编号 | 来源 | 状态 | 责任归属（owner） | 前置条件 | 验收条件 |
| --- | --- | --- | --- | --- | --- |
| `RELEASE-CP-205` | `OSPF-CP3-A205` | 待部署环境验收 | remote-solver 部署 owner（`framework/remote-solver`） | 本地 protocol/dispatcher/calculator 部分已完成并有 fixture 回归 | 在真实部署组合下完成 protocol、dispatcher、calculator、Ktorm/数据库 migration（V1～V7）、HTTP API、README 与 `daily.md` 的锁步更新，并使用同一 fixture 双向验收；PostgreSQL/S3 对象存储回放、服务/worker 重启组合与完整终态矩阵通过 |
| `RELEASE-CP-301～306` | `OSPF-CP3-A301～A306` | `BlockedByDomainOwner` | 真实领域 owner（当前无：仓库无生产 Benders 样例） | 补充一个使用 Logic-Based Benders 的生产型领域示例（稳定领域键、可重建 master binding、明确 cut 语义） | A301～A306 的领域 serializer、摘要/指纹门禁、capture/decode/rebuild/resume、Exact cut 复验及同进程/JVM/LocalFS/H2/HTTP 链路测试全部通过 |

### CP3 当前源码契约状态（2026-08-08）

`OSPF-CP3-A101/A102/A401/A402` 以及 `OSPF-SOL-022/023` 已在 `ospf-kotlin` 本地源码范围关闭：派生
triad/tetrad 元素的 Stable/ModelLocal scope、无序且可编码的身份、完整 provenance、IIS 身份物化以及
SCIP/Gurobi、remote、组合求解和 checkpoint 的本地传播矩阵均有回归证据；远程 `SolveReport`
的 schemaVersion、正交终态、incumbent/bound/gap、诊断、provenance、fingerprint 和
run/attempt 在线性/二次 adapter 中均有锁步回归。`OSPF-SOL-013/022/023` 已完成
本地契约验收；未接入稳定身份的其他插件仍按 `plans/solver_cp.md` 保持
`ModelLocal`/`Unsupported`。

这不改变本文件的发布边界：`RELEASE-CP-205` 仍等待真实部署验收，负责 migration、HTTP
部署、PostgreSQL/S3、服务/worker 重启组合和生产终态矩阵；`RELEASE-CP-301～306` 仍为
`BlockedByDomainOwner`，生产性能基线仍未验收。

### 本地最终复验（2026-08-05）

本轮不依赖外部基础设施的稳定 ID registry 与 Benders primitive checkpoint 已通过定向和全量验证。Kotlin `clean compile test-compile`、全量 `test`、`verify`、隔离仓库 `install` 均 `BUILD SUCCESS`；Surefire XML 汇总 3212 tests、0 failures、0 errors、6 skipped，Failsafe 汇总 25 tests（SCIP 21、Gurobi 4）、0 failures、0 errors。完整日志为：

- `D:\temp\ospf-stable-id-benders-clean-compile-final.log`
- `D:\temp\ospf-stable-id-benders-test-final.log`
- `D:\temp\ospf-stable-id-benders-verify-final.log`
- `D:\temp\ospf-stable-id-benders-install-final.log`

制品已安装到隔离 Maven 仓库 `D:\environment\maven_repository`；该安装用于源码联调，不代表外部部署验收。

### 历史全量复验记录（2026-08-05）

以上内容为当时的源码联调记录：Kotlin `clean compile test-compile`、全量 `test`、SCIP/Gurobi `verify` 和隔离仓库 `install` 均 `BUILD SUCCESS`。Surefire XML 汇总 3223 tests、0 failures、0 errors、6 skipped；Failsafe 汇总 25 tests（SCIP 21、Gurobi 4）、0 failures、0 errors。当前权威结果见 2026-08-06 的“本轮最终本地验收”一节；当时日志为：

- `D:\temp\cp2-final-kotlin-clean-compile-current.log`
- `D:\temp\cp2-final-kotlin-test-current.log`
- `D:\temp\cp2-final-kotlin-verify-current.log`
- `D:\temp\cp2-final-kotlin-install-current-2.log`

Kotlin 制品已安装到 `D:\temp\ospf-cp2-local-m2`。remote-solver 使用该隔离仓库完成 `clean compile test-compile`、全量 `test`、`verify` 和 `install`，Surefire 汇总 326 tests、0 failures、0 errors，Failsafe 为 `RemoteCpHttpE2EIT` 1 test、0 failures、0 errors；日志为 `D:\temp\cp2-final-remote-clean-compile-current.log`、`D:\temp\cp2-final-remote-test-current.log`、`D:\temp\cp2-final-remote-verify-current.log`、`D:\temp\cp2-final-remote-install-current.log`。这些是源码联调和本地验证证据，不替代下方外部环境验收。

### 外部环境验收清单

以下事项不依赖继续修改 CP 算法，但必须在真实或生产等价环境中验证后才能关闭：

- **PostgreSQL 迁移与重启回放**：执行 remote-solver V1～V7 migration，在真实数据库中写入任务、slice、payload/config/result/checkpoint ObjectRef，重启服务后验证排队、恢复、replay、resume、timeline 和监控查询语义不变。
- **生产对象存储联调**：使用 S3/MinIO 等实际对象存储验证租户隔离、ETag/version、断点续传、结果与 checkpoint 读取、权限失败和对象丢失后的结构化错误；不能用内存或 LocalFS 结果替代。
- **部署级跨模块终态矩阵**：在真实 HTTP、dispatcher、calculator、worker 和对象存储组合下覆盖 Optimal、Feasible、Infeasible、Unknown、TimeLimit、NodeLimit、SolutionLimit、Cancelled、Interrupted、BackendFailure，以及有/无 incumbent 的组合。
- **外部 worker 与服务进程重启**：验证默认 in-process bridge 与显式 external-process bridge 在服务重启、worker 重启、slice 重试和 checkpoint 链路中保持 run/attempt、终止原因、artifact digest 和恢复边界一致。
- **生产代表性性能基线**：在目标硬件、SCIP/native 版本、线程和数据规模上记录 build/compile/solve/shrink/restore 的 p50/p95、节点、迭代、峰值内存和资源泄漏；Fake、小规模和单机 smoke benchmark 只能作为开发基线。

### 不属于环境验收的本地收尾

以下事项属于源码契约和本地可测试能力，不需要 PostgreSQL、S3/MinIO、外部 worker 或生产硬件：core 的显式稳定 ID registry 与 triad/tetrad artifact 映射、Benders primitive-only cut serializer/checkpoint codec、跨模型 fingerprint 门禁、独立 JVM portable decode，以及本地/远程终态和失败路径补测。

本轮已完成 core registry 和 versioned Benders checkpoint document；显式 ID 冲突不会破坏既有 fallback，feasibility artifact 也保留源约束的完整 provenance。领域 serializer 仍由领域调用方实现，不能把任意业务对象自动持久化。`OSPF-SOL-013` 的本地契约已按本文件“已关闭公共求解合同”关闭；未接入稳定身份的其他插件和部署级验收不在该关闭声明内，也不会因本地测试或部署环境自动互相替代。

本轮同时修正了远程 worker 的后端失败终态（`BACKEND_FAILURE` 不再输出为 completed）以及外部 bridge 的兼容分流（严格 CP 模式校验 artifact，旧非 CP key=value 模式允许缺失可选路径）。这些是源码契约修复，不属于外部环境验收项。

### 条件能力边界

JSCIP 原模型 bound 增量、probing、结构化 conflict、SCIP native optional/variable interval 和 native 搜索树 resume
当前仍按 `RejectedByEvidence` 处理：尚无完整 API 或等价性证据。本轮仅保留并复制了
`origin/master` 的 Event/EventHandler/EventMask/Node/Row/Column 上游 API 供后续评估，未将其作为
`feat/constraint_programing` 的提交；binding/runtime version probe、生命周期 API 和 reoptimization
证据全部延后。更换部署环境不会使这些能力自动成立，只有通过对应探针、差分测试和资源/性能门禁后才能重新评估。

后续 JSCIPOpt binding、同进程模型复用、probing/reoptimization、结构化 conflict 探针及
optional/variable interval/native checkpoint 的重新评估统一由 `plans/solver_cp.md` 承接，
当前状态为 `BlockedByUpstream`；已关闭的 OSPF 公共契约与源码证据由本文件保留。
本文件继续只管理真实 PostgreSQL/S3、进程重启组合、部署级终态矩阵和生产性能基线，
不会因 JSCIPOpt 源码可修改而改变这些环境验收项的状态。

### 历史工作区本地验收（2026-08-06）

- Kotlin `clean compile test-compile`、全量 `test`、SCIP/Gurobi `verify`、隔离 Maven `install` 均成功；remote-solver 使用 `D:\temp\ospf-cp2-local-m2` 中的 Kotlin 制品完成相同的 clean compile/test、全量 test、verify 和 install。
- 历史阶段统计（以 2026-08-06 本轮 XML 为准）为 Kotlin Surefire 3239 tests、6 skipped，Failsafe 25 tests；remote-solver Surefire 352 tests、Failsafe 1 test；全部 0 failures、0 errors。此前的 3226/343 也均为历史报告数字；当前权威统计见下方最新 CP3 本地复验。
- 完整日志位于 `D:\temp\cp2-final-k-clean-compile-20260806-final.log`、`D:\temp\cp2-final-k-test-20260806-final.log`、`D:\temp\cp2-final-k-verify-20260806-final.log`、`D:\temp\cp2-final-k-install-20260806-final-retry.log`、`D:\temp\cp2-final-remote-clean-compile-20260806-final.log`、`D:\temp\cp2-final-remote-test-20260806-final.log`、`D:\temp\cp2-final-remote-verify-20260806-final.log`、`D:\temp\cp2-final-remote-install-20260806-final.log`。
- CP3 当前源码收尾日志更新为 Kotlin `D:\temp\cp3-kotlin-clean-compile-final.log`、`D:\temp\cp3-kotlin-test-final.log`、`D:\temp\cp3-kotlin-verify-final.log`，remote-solver `D:\temp\cp3-remote-clean-compile-final.log`、`D:\temp\cp3-remote-test-final.log`、`D:\temp\cp3-remote-verify-final.log`；JSCIPOpt 条件能力未纳入本轮验收。
- 该历史记录只覆盖本地源码、隔离对象存储和内存/HTTP 测试，不替代本节外部验收清单；领域 cut serializer 跨进程接入、PostgreSQL/S3、服务/worker 重启组合和生产性能基线仍未关闭。

### 本轮源码门禁复核（2026-08-05）

### CP3 第二轮源码收尾（2026-08-07）

- Kotlin 当前源码完成 `test-compile`、定向测试、全量 `mvn test`、SCIP/Gurobi 子模块 `verify` 与
  隔离 `install`，均 `BUILD SUCCESS`。Surefire XML 汇总 `3258 tests, 0 failures, 0 errors, 6 skipped`；
  Failsafe 汇总 `25 tests, 0 failures, 0 errors`（SCIP 21、Gurobi 4）。全量 `mvn test` 在 77/80 因
  并发 Kotlin 编译 Metaspace OOM 中断一次，`-rf` 续跑完成，两段均为 0 failures/0 errors，
  属本机内存环境问题而非源码缺陷。
- 本轮新增：二次机制模型身份传播回归、SCIP/Gurobi 线性/二次求解器原生变量/约束名称的稳定 ID
  前向投影（`nativeElementName`/`sanitizeNativeName`）、checkpoint 身份往返回归；
  `OSPF-CP3-A106` 已按本地测试矩阵关闭，该历史阶段的 A101/A102/A103/A104 缺口已由后续轮次补齐。
- remote-solver 使用本轮安装到默认本地仓库的 Kotlin 1.1.0 制品完成全量 `test` 与 `verify`：
  Surefire `352 tests, 0 failures, 0 errors`，HTTP Failsafe `1 test, 0 failures, 0 errors`。
- 完整日志位于 `D:/temp/ospf-cp3-kotlin-verification-20260807/`（compile-targeted2.log、
  targeted-tests5.log、test-full.out.log、test-full-resume.out.log、verify-plugin.out.log、
  install-current.log、remote-test.log、remote-verify.log）。
- 该证据只覆盖本地源码与内存/HTTP 测试，不改变 `ImplementedWithBoundaries`：真实 PostgreSQL/S3、
  服务/worker 进程重启组合、部署级完整终态矩阵、领域 Benders cut serializer 跨进程接入、
  生产性能基线仍需外部环境或领域 owner 证据；后续 CP3 本地 `OSPF-SOL-013` 已按当前权威状态关闭。

### CP3 第三轮源码收尾（2026-08-07）

- A103 插件级验收完成：新增 `ScipLinearIdentityProjectionIT`/`GurobiLinearIdentityProjectionIT`，
  在 Configuration 回调断言稳定 ID 投影为 `ospf-variable-*`/`ospf-constraint-*` 原生名称，
  model-local 元素保留展示名；SCIP/Gurobi 二次路径与线性共享同一 `nativeElementName` 投影。
  诊断辅助模型独立命名（SCIP `diagnostic-var-*`/`diagnostic-row-*`，Gurobi `var-*`/`row-*`），
  不冒充源 ID；presolve 元素由原生求解器内部管理，不映射为源元素 ID。插件 Failsafe：
  SCIP 22 项、Gurobi 5 项，0 failure/0 error。
- A104 传播矩阵完成：组合求解器身份门禁、attempt trace 保留 attemptId/backendId 与
  provenance/fingerprints、诊断证据回查稳定 ID、checkpoint 身份往返、远程 DTO 身份往返；
  framework 定向测试 8 项通过。该历史阶段仅关闭 A103/A104；后续轮次已补齐
  A101/A102 的机制派生元素统一 origin、跨重建终态矩阵及 `OSPF-SOL-013` 本地契约。
- A205 边界明确：protocol/dispatcher/calculator 的本地部分已推进并有 fixture 回归；
  Ktorm/数据库 migration（V1～V7）、HTTP 部署链路、PostgreSQL/S3 对象存储回放和
  服务/worker 重启组合由本文件下方部署环境验收清单管理，不因本地 fixture 通过而关闭。
  （2026-08-07 经用户确认正式移交本文件管理。）
- A301～A306 移交记录：2026-08-07 经用户确认，真实领域 Benders cut serializer 示例后续在本文件
  补充；在补充前 A301～A306 保持 `BlockedByDomainOwner`，不视为
  CP3 源码缺陷或未完成事项。
- 完整日志位于 `D:/temp/ospf-cp3-progress-20260807-verify-plugin.log` 与
  `D:/temp/ospf-cp3-progress-20260807-targeted-framework.log`。
- 该证据只覆盖本地源码与内存测试，不改变 `ImplementedWithBoundaries`。

### CP3 第四轮审查修复（2026-08-07，历史记录）

- 移交正式化：`OSPF-CP3-A205` 与 `OSPF-CP3-A301～A306` 已按上文移交任务登记
  正式移交（`RELEASE-CP-205`、`RELEASE-CP-301～306`），含独立验收条件与唯一
  所有者；原计划已停止追踪其勾选状态，A001 唯一所有权无冲突。
- 命名投影修复：`nativeElementName`/`sanitizeNativeName` 改为可逆 UTF-8 字节
  十六进制转义（`_h<2 位 hex>_`，含下划线自身），`a/b`、`a b`、`a_b` 不再碰撞；
  超长名称按“安全前缀 + SHA-256 摘要”截断。`ModelingPreparationTest` 增至 8 项
  （新增碰撞、往返、超长回归），SCIP/Gurobi 插件级投影 IT 不受影响。
- 本机内存被其他项目活跃构建占用，Kotlin 全量并发从 `-T 0.75C` 降至 `-T 0.5C`；
  一次完整 `mvn test` `BUILD SUCCESS`（不再依赖 `-rf` 续跑）：Surefire XML 汇总
  `3261 tests, 0 failures, 0 errors, 6 skipped`；`mvn clean compile test-compile`
  同样一次 `BUILD SUCCESS`；SCIP/Gurobi 插件 `verify` Failsafe 汇总
  `27 tests, 0 failures, 0 errors`（SCIP 22、Gurobi 5）。
- remote-solver 使用隔离仓库 `D:/temp/ospf-cp3-fix-local-m2`（含更新后的
  `ospf-kotlin-core:1.1.0`）完成 `clean test` 与 `verify`：Surefire
  `352 tests, 0 failures, 0 errors`，HTTP Failsafe `1 test, 0 failures, 0 errors`。
- 完整日志位于 `D:/temp/ospf-cp3-review-fix-20260807/`（test-full-fix.log、
  clean-compile-test-compile-fix.log、verify-plugin-fix.log、remote-clean-test-fix2.log、
  remote-verify-fix.log）。该阶段仍为 `ImplementedWithBoundaries`，但后续源码收尾已关闭
  A101/A102/A401/A402 与 `OSPF-SOL-013` 的 CP3 本地契约；真实 PostgreSQL/S3、服务/worker
  重启组合与生产性能基线仍未关闭。

### CP3 A101/A102/A401/A402 本地最终复验（2026-08-07，历史记录）

- 在完成派生身份完整 provenance、规范化 fingerprint、IIS 身份物化、远程 DTO 往返和
  二次身份校验补强后，`mvn clean compile test-compile -T 0.75C` 与 `mvn test -T 0.75C`
  均为 `BUILD SUCCESS`。
- 当前工作树 Surefire XML 汇总为 497 个测试文件、`3281 tests, 0 failures, 0 errors,
  6 skipped`；统计由各报告的 suite 属性求和得到。完整日志为
  `D:/temp/ospf-cp3-provenance-20260807/kotlin-final-clean-compile-test-compile-alias-ret.log` 和
  `D:/temp/ospf-cp3-provenance-20260807/kotlin-final-test-alias-ret.log`。
- 本轮新增 CP checkpoint canonical scope/provenance permutation、远程线性/二次 malformed
  identity 结构化失败回归；CP 定向 27 项、framework 定向 25 项均为 0 failure、0 error。
- remote-solver 本轮使用隔离仓库 `D:/temp/ospf-cp3-fix-local-m2` 中的当前 core、framework、
  starter、SCIP 和 Gurobi 制品完成 `clean compile test-compile`、全量 `test` 与 `verify`；
  Surefire XML 汇总为 `353 tests, 0 failures, 0 errors, 0 skipped`，HTTP Failsafe 为
  `1 test, 0 failures, 0 errors, 0 skipped`。完整日志为
  `D:/temp/ospf-cp3-provenance-20260807/remote-clean-compile-test-compile.log`、
  `D:/temp/ospf-cp3-provenance-20260807/remote-test.log` 和
  `D:/temp/ospf-cp3-provenance-20260807/remote-verify.log`。
- 该复验只确认 `ospf-kotlin` 本地源码/契约范围，不改变 `RELEASE-CP-205` 的部署环境验收、
  `RELEASE-CP-301～306` 的 `BlockedByDomainOwner` 状态，也不替代 PostgreSQL/S3、服务/worker
  重启组合和生产性能基线。

### CP3 A101/A102/A401/A402 与 OSPF-SOL-022/023 当前锁步复验（2026-08-08）

- Kotlin 当前工作树完成 `mvn clean compile test-compile -T 0.75C`、全量 `mvn test -T 0.75C`
  和 SCIP/Gurobi `verify`，均为 `BUILD SUCCESS`；497 个 Surefire 报告汇总为
  `3291 tests, 0 failures, 0 errors, 6 skipped`，插件 Failsafe 为 `27 tests, 0 failures,
  0 errors`。本轮完整 Kotlin 日志为 `D:/temp/ospf-cp3-sol-022-023-kotlin-clean-compile-test-compile.log`、
  `D:/temp/ospf-cp3-sol-022-023-kotlin-test.log`；插件 Failsafe 的既有锁步日志仍为
  `D:/temp/ospf-cp3-kotlin-final-plugin-verify.log`。
- remote-solver 使用隔离仓库 `D:/temp/ospf-cp3-fix-local-m2` 中的当前 Kotlin 制品完成
  `clean compile test-compile`、全量 `test` 与 `verify`，均为 `BUILD SUCCESS`；protocol 20、
  calculator 31（1 skipped）、dispatcher 305，Surefire 合计
  `356 tests, 0 failures, 0 errors, 1 skipped`，HTTP Failsafe 为
  `1 test, 0 failures, 0 errors, 0 skipped`。完整日志为
  `D:/temp/ospf-cp3-remote-current-final-clean-compile-test-compile.log`、
  `D:/temp/ospf-cp3-remote-current-final-test.log` 和
  `D:/temp/ospf-cp3-remote-current-final-verify.log`。
- 该结果取代 2026-08-07 的 3281/353 阶段统计，仅证明本地源码、协议和内存/HTTP 测试通过；
  `OSPF-SOL-022/023` 的源码交付口径已完成，`RELEASE-CP-205` 仍待真实部署环境验收，
  `RELEASE-CP-301～306` 仍为 `BlockedByDomainOwner`，整体发布状态继续保持
  `ImplementedWithBoundaries`。

本轮已在不依赖外部基础设施的范围内补齐以下生产契约：外部进程 bridge 可恢复同一任务的历史 attempt checkpoint；严格 CP result/checkpoint 在上传前执行 snapshot 数学复验和完整审计字段交叉校验；v2 future schema、伪装 legacy 来源和损坏摘要拒绝；LocalFS/S3 checkpoint metadata 保留 schema、model/configuration/solver fingerprint 与 digest；稳定身份 registry 的并发访问和最终唯一性校验；Benders checkpoint 含 master 状态时必须提供显式恢复适配器。remote-solver 全 reactor Surefire 331 项、Kotlin 全仓库 Surefire 3223 项均通过，Kotlin Failsafe 25 项和 remote-solver HTTP Failsafe 1 项也通过。

这些源码证据不替代下方部署环境验收：领域业务 cut serializer 的实际接入、真实 PostgreSQL/S3、
服务/worker 重启组合、跨模块终态矩阵和生产性能基线仍需独立环境或领域 owner 证据；未接入
稳定身份的其他插件仍按 `solver_cp.md` 的 `ModelLocal`/`Unsupported` 边界处理。

### 当前恢复链与诊断复验（2026-08-06）

- external bridge 不再把恢复输入 checkpoint 当作当前切片输出；新切片必须生成新的 v2 checkpoint 并正确建立 parent/attempt 关系，没有新 checkpoint 时返回结构化 `BACKEND_FAILURE`，防止重复从同一搜索状态重启。
- calculator 与 external bridge 对带 incumbent 的 `BACKEND_FAILURE` 采用一致的 snapshot 语义校验；目标存在时重新求值，目标缺失只允许在 checkpoint 导出阶段由 snapshot 补齐。
- external CP diagnostics 已执行结构化 source、枚举、成员 ID、bound/domain、assumption 和 issue 条目校验；未知或畸形证据不会进入对象存储。Benders 主问题状态必须由显式 adapter 返回逐字段恢复证据。
- 这些是本地源码契约，不需要外部基础设施。此前恢复链阶段的 Kotlin Surefire 3223、remote-solver Surefire 337 仅作历史记录；当前权威统计见下方“本轮最终本地验收”。外部 PostgreSQL/S3、服务/worker 重启组合、全模块终态矩阵和生产性能基线仍不能由本地测试推断。

### 本轮最终本地验收（2026-08-06）

- plain CP、external bridge、stable identity 和旧 V2 Benders 摘要迁移门禁已完成源码修复；定向回归均通过。Kotlin 全量 Surefire 为 3226 tests、0 failures、0 errors、6 skipped，Failsafe 为 25 tests、0 failures、0 errors；remote-solver Surefire 为 343 tests、0 failures、0 errors，HTTP Failsafe 为 1 test、0 failures、0 errors。
- 两仓库均已用完整 `clean compile test-compile`、`test`、`verify` 和隔离 Maven `install` 验证；该阶段的 `ImplementedWithBoundaries` 边界已并入本文件开头的迁移登记和外部环境验收清单。
- 真实 PostgreSQL/S3、服务/worker 重启组合、跨模块完整终态矩阵、领域 cut serializer 跨进程持久化和生产性能基线仍需外部或领域 owner 证据；CP3 本地 `OSPF-SOL-013` 的当前状态见本文件开头，不能由本地构建成功推断部署完成。

### 历史本地验收（2026-08-06，已被 2026-08-07 当前权威记录取代）

- 当前源码已重新完成 Kotlin `clean compile test-compile`、全量 `test`、SCIP/Gurobi 子模块 `verify`；本轮最终完整日志为 `D:\temp\cp3-kotlin-clean-compile-final.log`、`D:\temp\cp3-kotlin-test-final.log` 和 `D:\temp\cp3-kotlin-verify-final.log`。Surefire XML 为 3239 tests、0 failures、0 errors、6 skipped，Failsafe 为 25 tests、0 failures、0 errors。
- core/framework/SCIP/Gurobi 目标制品已选择性安装到 `D:\temp\ospf-cp3-local-m2`；全 reactor install 受未提供的 CPLEX/Lingo/COPT 等厂商 `provided` SDK 阻塞，阻塞日志为 `D:\temp\cp3-kotlin-install-current.log`，不影响上述源码编译、测试和目标插件安装。
- remote-solver 使用该隔离制品完成 `clean compile test-compile`、全量 `test` 和 `verify`；本轮最终完整日志为 `D:\temp\cp3-remote-clean-compile-final.log`、`D:\temp\cp3-remote-test-final.log` 和 `D:\temp\cp3-remote-verify-final.log`。Surefire XML 为 352 tests、0 failures、0 errors；HTTP Failsafe 为 1 test、0 failures、0 errors。
- 本地结果不改变 `ImplementedWithBoundaries`：真实 PostgreSQL/S3、服务/worker 重启回放、跨模块完整终态矩阵、领域 cut serializer 跨进程持久化和生产性能基线仍需外部或领域 owner 证据；CP3 本地 `OSPF-SOL-013` 的当前状态见本文件开头。

### 归档迁移说明

下方完整保留已归档 CP 基础计划的内容；本文件是后续发布验收的唯一入口，不得丢失其能力边界、兼容策略、测试矩阵和关闭标准。

---

# 约束规划与 Logic-Based Benders 支持计划

## 1. 文档状态

| 项目 | 内容 |
| --- | --- |
| 状态 | Implemented with declared capability boundaries |
| 日期 | 2026-08-01 |
| 范围 | `ospf-kotlin-core`、`ospf-kotlin-core-plugin`、`ospf-kotlin-framework`、`ospf-kotlin-example` |
| 求解实现 | core 提供通用 CP 抽象；SCIP 提供首个原生实现；Gurobi 支持可精确降为 MIP 的子集 |
| 首个 Benders 形态 | 线性整数主问题 + CP 子问题 |
| 兼容策略 | 保留现有线性/二次模型、经典 Benders API 和现有 IIS 计算作为降级分支，新增平行能力 |

本文档定义 OSPF 对约束规划（Constraint Programming，CP）的目标架构和实施顺序，覆盖两种使用方式：

1. 在 core 中直接构造并求解 CP 模型。
2. 在 framework 中把 CP 模型作为 Logic-Based Benders 子问题。

## 2. 背景与现状

当前 OSPF 的高层建模以 `MetaModel` 为轴心，主要覆盖线性和二次模型：

- `LinearMetaModel` 负责线性约束和线性目标。
- `QuadraticMetaModel` 负责二次约束和二次目标。
- `LinearMechanismModel` / `QuadraticMechanismModel` 负责展开。
- `LinearTriadModel` / `QuadraticTetradModel` 负责向求解器插件传输稀疏模型。

当前 Benders 接口位于 framework，分为：

- `LinearBendersDecompositionSolver`
- `QuadraticBendersDecompositionSolver`

两者均以对偶解或 Farkas 证书生成 cut。这一机制不适用于一般 CP 子问题，因为 CP 求解器通常不提供能直接构造经典 Benders cut 的 LP 对偶信息。

因此，本计划采用 Logic-Based Benders：CP 后端提供子问题结论和可验证证据，领域 cut oracle 负责把证据转换为主问题 cut。

### 当前实现状态（2026-08-01）

- CP0～CP4 的 core AST、snapshot、fake solver、SCIP solver、精确 MIP lowerer、结构化不可行诊断、Logic-Based Benders、示例 pipeline 和测试已经落地。
- CP5 已提供整数 master no-good 编码、optional interval/variable duration 的精确 MIP-backed 支持、portable snapshot serialization、remote CP execution payload 和 checkpoint 重建评估。
- optional interval 与 variable duration 尚未声明为 SCIP native 能力；SCIP native 路径仍限制为固定 duration interval。Circuit、Automaton、Reservoir 已由 SCIP 编译器以受规模限制的精确线性分解支持，能力声明为 `ExactLowering`。
- native IIS/Farkas 通过 solver capability 和 analyzer SPI 接入：Gurobi 提供 LP/MIP/QP 原生 IIS，Gurobi 与 SCIP 提供连续 LP Farkas；未声明能力的 backend 使用带来源和证据等级的 legacy fallback。
- 全量验收使用 `mvn clean compile test-compile -T 0.75C`、`mvn test -T 0.75C` 和插件 `mvn verify`；Surefire 报告汇总为 0 failures、0 errors、6 skipped（Maven 模块汇总会重复计数，不以日志中的总测试数作为唯一统计口径），当前 SCIP Failsafe 集成测试为 14 tests、Gurobi Failsafe 集成测试为 4 tests，共 18 tests、0 failures、0 errors。

当前 `core/solver/iis` 通过弹性模型和删除过滤定位不可行元素。删除过滤保留的是必须继续放开才能恢复可行性的 slack，语义更接近最小修复集（Minimal Correction Set，MCS），不能普遍保证返回的原始约束与变量界自身仍然不可行，也不能普遍保证 IIS 的不可约性。该实现继续保留，但在统一报告中只能按实际保证标记为 `ElasticFilter` / `DeletionFilter` 的启发式或未知证据。

## 3. 核心架构决策

### 3.1 CP 是独立的一等模型族

新增 `ConstraintProgrammingModel`，不把 CP 全局约束伪装成线性或二次多项式，也不要求所有 CP 模型先线性化。

`ConstraintProgrammingModel` 不直接继承现有 `MetaModel`。原因是 `MetaModel` 的约束集合和目标集合当前绑定数学多项式语义，强行继承会导致空约束列表、旁路目标和不完整导出等错误契约。

CP 模型应复用以下公共能力：

- OSPF 变量及其稳定标识。
- 变量注册和解回填。
- `ObjectCategory`。
- 约束分组。
- `Try` / `Ret<T>` 错误传播。
- 求解进度和统一报告。

新增可选接口 `ConstraintGroupRegistry`，由 `MetaModel` 和 `ConstraintProgrammingModel` 实现。framework 的 `Pipeline.register` 通过该接口注册约束组，不再只识别 `MetaModel`。

### 3.2 CP AST 使用精确整数语义

首版 CP 模型采用整数和布尔值域。整数系数、上下界、interval 时间点和容量统一使用 `Int64` 或可无损转换为 `Int64` 的值。

禁止在 CP AST 内隐式使用浮点近似。对于小数业务量，调用方必须声明缩放规则：

```text
业务值 --显式 scale--> CP 整数值 --求解--> CP 整数解 --显式 unscale--> 业务值
```

以下情况必须返回失败：

- 非整数值未提供缩放策略。
- 缩放后超过 `Int64` 范围。
- 线性表达式累加可能溢出。
- 不同物理单位在未换算时参与同一 CP 表达式。

### 3.3 复用现有标量变量，新增 CP 结构变量

尽量复用现有 `BinVar`、`IntVar`、`UIntVar` 作为标量决策变量，使同一领域变量可以同时注册到 MILP 主问题和 CP 子问题。

新增的 CP 结构不伪装成标量变量：

- `BooleanLiteral`：变量和正负极性的组合。
- `IntegerDomain`：连续区间或离散值集合。
- `IntervalVariable`：start、size、end 和可选 presence literal。
- 后续可扩展 sequence、automaton state 等结构。

### 3.4 core 提供一次性求解和会话式求解

直接 CP 建模使用一次性入口；Benders 反复求解同一静态子问题时使用会话入口：

```kotlin
interface ConstraintProgrammingSolver {
    val descriptor: SolverDescriptor

    suspend fun solve(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
    ): Ret<ConstraintProgrammingSolverOutput>

    fun createSession(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
    ): Ret<ConstraintProgrammingSession>
}

interface ConstraintProgrammingSession : AutoCloseable {
    suspend fun solve(
        assumptions: List<BooleanLiteral> = emptyList(),
        hints: ConstraintProgrammingSolution? = null
    ): Ret<ConstraintProgrammingSolverOutput>
}
```

`ConstraintProgrammingSession` 是生命周期与可复用编译计划的边界，不承诺所有插件都能原地增量求解。不得把具体求解器变量泄露到 domain 或 application；插件无法增量修改模型时可以重建内部模型，但必须保持相同的公共契约。

### 3.5 求解器实现策略

OSPF 不依赖、不引入 OR-Tools。core 只定义 CP 模型、snapshot、solver SPI、能力声明和统一输出，不依赖任何具体求解器。

首版生产实现按以下顺序提供：

- 现有 SCIP 插件新增 `ScipConstraintProgrammingSolver`，优先使用 SCIP constraint handler、domain propagation 和 conflict analysis 能力。
- core 新增精确 `ConstraintProgrammingToLinearModelLowerer`，把能力子集等价转换为 `LinearMetaModel`。
- `MipBackedConstraintProgrammingSolver` 接收现有 `LinearSolver`；Gurobi 通过该入口求解可精确降维的 CP 子集。
- fake solver 和小规模穷举 oracle 只用于 core 契约与编译正确性测试，不作为生产 CP 引擎。

SCIP 的原生约束处理与 MIP 精确降维是不同实现机制，必须通过 capability 明确区分。不得把近似线性化、弱化约束或仅必要条件声明为已支持能力。

首版状态映射冻结如下：

| 求解器终态 | 统一输出 | 证明语义 |
| --- | --- | --- |
| 已证明最优 | `SolverStatus.Optimal` | `SolutionPresence.Optimal` + `ProofStatus.Verified` |
| 未证明最优但存在 incumbent | `SolverStatus.Feasible` | `SolutionPresence.Incumbent` + `ProofStatus.None` |
| 已证明不可行 | `SolverStatus.Infeasible` | `SolutionPresence.None` + `ProofStatus.Verified` |
| 达到限制且不存在 incumbent | `ConstraintProgrammingUnknownOutput` | `SolutionPresence.None` + `ProofStatus.None` |
| 模型校验失败 | 结构化 `Ret` 失败 | 不进入搜索，不产生 Benders 证书 |

`Exact` Benders 只接受求解器证明的最优或不可行子问题，以及声明全局有效性的 cut；可行但未证明最优、未知、取消和超时只能进入 `Heuristic` 或失败路径。精确 MIP 降维只有在证明与原 CP 约束等价时才能产生 `Verified` 终态。

### 3.6 framework 使用 Logic-Based Benders

首版只支持：

```text
LinearMetaModel master + ConstraintProgrammingModel subproblem
```

主问题继续由现有线性求解器求解，CP 子问题默认由 SCIP 插件求解；能力子集也可以使用注入了 Gurobi 等线性求解器的 `MipBackedConstraintProgrammingSolver`。具体求解器不生成领域 cut，framework 新增 `LogicBasedBendersEngine` 负责迭代，`BendersCutOracle` 负责生成 cut。

现有 `LinearBendersDecompositionSolver` 和 `QuadraticBendersDecompositionSolver` 保持不变，避免破坏插件和调用方兼容性。

### 3.7 精确模式和启发式模式必须分离

新增模式：

```kotlin
enum class BendersProofMode {
    Exact,
    Heuristic
}
```

`Exact` 模式必须满足：

- 每轮主问题得到可接受的最优证明。
- 用于收敛判断的 CP 子问题得到最优或不可行证明。
- 所有加入主问题的 cut 都声明全局有效性。
- 最终上下界满足配置的收敛容差。
- 超时、取消或 `Unknown` 子问题不能被当作精确证书。

`Heuristic` 模式可以接受可行但未证明最优的 CP 解，但最终报告不得声明全局最优。

### 3.8 CP conflict 复用于结构化不可行诊断

CP assumptions 和 conflict core 除用于 Logic-Based Benders 外，也作为 OSPF 通用结构化不可行诊断的一条精确路径。它替换的是当前通用 IIS 算法的首选实现，不替换 backend 原生 IIS/Farkas，也不要求把任意 LP、QP 或 QCP 转换为 CP。

默认诊断策略按模型类型和 capability 选择：

1. backend 能直接对原模型提供 native IIS 时优先使用 native IIS。
2. backend 能提供 Farkas 证书时，作为独立的精确不可行证明返回，不冒充 IIS；Farkas 不适用于只有整数语义才不可行而连续松弛可行的模型。
3. CP 模型或可精确编译为 CP/MIP assumption 模型的离散模型，使用 assumption conflict；后端 conflict core 不可用时，以全部活动 assumptions 作为可靠起点。
4. 不满足精确编译条件、包含 CP MVP 不支持的连续/二次语义或诊断达到限制时，降级到现有弹性/删除过滤实现。
5. 所有诊断失败只进入 `SolveDiagnostics` 的缺失/失败原因，不得覆盖已经确定的 `ProblemStatus.Infeasible`。

core 只定义 `InfeasibilityAnalyzer<M>`、策略编排和证据合同，不直接创建 SCIP、Gurobi 或其他插件实例。solver plugin 通过 capability 和 analyzer SPI 提供 native IIS、Farkas 或 conflict 能力；旧 `computeIIS(...)` 保留为兼容 facade，内部委托新编排器，并在无法使用新策略时调用现有实现。

CP conflict core 默认只表示“这些 assumptions 足以导致不可行”，不自动表示 IIS。只有满足以下条件时才能标记为不可约：

- 每个原始约束、变量下界、变量上界和 sparse domain 限制分别由稳定 activation ID 控制。
- 变量在 solver 中使用不额外制造不可行性的安全基础值域；如果关闭某个 bound/domain assumption 后该限制仍隐含在基础值域中，或无法构造安全有限值域，则该路径必须返回 `Unsupported`。
- CP 编译与原模型语义双向等价，不是松弛、必要条件或近似缩放。
- 删除一个成员后的每次子求解都得到已证明终态。
- 仅在剩余集合仍被证明不可行时删除成员；`Unknown`、超时或取消时保留成员并降低 minimality 等级。
- 最终成员只通过稳定 origin ID 回映射，求解器内部辅助约束不得泄露为公共证据成员。

这里的不可约是 inclusion-irreducible，不保证成员数量最少。对包含连续变量、非精确缩放或首版 CP AST 不支持的二次约束，不得启用 CP 精确诊断路径。

## 4. core 模型设计

### 4.1 拟议目录

```text
ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/
  model/constraint_programming/
    ConstraintProgrammingModel.kt
    ConstraintProgrammingModelSnapshot.kt
    ConstraintProgrammingExpression.kt
    ConstraintProgrammingConstraint.kt
    IntegerDomain.kt
    BooleanLiteral.kt
    IntervalVariable.kt
    ConstraintGroupRegistry.kt
  solver/constraint_programming/
    ConstraintProgrammingSolver.kt
    ConstraintProgrammingSession.kt
    ConstraintProgrammingSolveOptions.kt
    ConstraintProgrammingSolverOutput.kt
    ConstraintProgrammingFeature.kt
```

### 4.2 模型生命周期

`ConstraintProgrammingModel` 覆盖以下生命周期：

1. 注册标量变量和结构变量。
2. 注册具名表达式。
3. 注册约束和约束组。
4. 注册目标。
5. 校验模型。
6. 生成不可变 snapshot。
7. 由具体 solver adapter 或精确 lowerer 编译 snapshot。
8. 回填求解结果。

snapshot 必须满足：

- 变量和约束顺序确定。
- 每个元素都有稳定 ID。
- 不包含引擎内部可变状态。
- 可以重复编译。
- 编译后修改原模型不会影响已有 snapshot。

### 4.3 首版变量与表达式

| 类型 | 首版 | 说明 |
| --- | --- | --- |
| Boolean variable | 是 | 复用 `BinVar` |
| Integer variable | 是 | 复用 `IntVar` / `UIntVar` |
| Sparse integer domain | 是 | 支持区间集合和值集合 |
| Boolean literal | 是 | 支持正变量和 negated literal |
| Integer linear expression | 是 | 精确整数系数 |
| Interval variable | 是 | 固定或变量 duration |
| Optional interval | 是 | presence literal 控制 |
| Sequence variable | 否 | 后续按 solver capability 扩展 |
| Floating variable | 否 | 不属于 OSPF CP MVP |

### 4.4 首版约束

| 约束 | 首版 | 备注 |
| --- | --- | --- |
| 整数等式/不等式 | 是 | `==`、`<=`、`>=` |
| BoolAnd / BoolOr / BoolXor | 是 | 支持 literal |
| Implication | 是 | 支持 enforcement literal |
| Reified constraint | 是 | 明确单向和双向语义 |
| AllDifferent | 是 | 整数表达式集合 |
| Element | 是 | 常量或变量数组 |
| Allowed assignments | 是 | table constraint |
| Forbidden assignments | 是 | table constraint |
| NoOverlap | 是 | interval 集合 |
| Cumulative | 是 | interval、demand、capacity |
| Circuit | 是 | SCIP ExactLowering：排列 + MTZ 分解 |
| Automaton | 是 | SCIP ExactLowering：分层状态/转移流分解 |
| Reservoir | 是 | SCIP ExactLowering：稳定整数事件排序 + 乘积线性化 |

### 4.5 求解器输出

CP 输出不复用旧的可行输出 facade，统一使用 `SolveReport`；CP 专用输出只保留结构化报告和
领域 artifact 的组合：

```kotlin
sealed interface ConstraintProgrammingSolverOutput : SolverOutput {
    val report: SolveReport?
}

data class ConstraintProgrammingFeasibleOutput(
    val solution: ConstraintProgrammingSolution,
    val objective: Flt64?,
    val bestBound: Flt64?,
    val status: SolverStatus,
    val proofStatus: ProofStatus,
    override val report: SolveReport?
) : ConstraintProgrammingSolverOutput

data class ConstraintProgrammingInfeasibleOutput(
    val conflict: ConstraintProgrammingConflict?,
    val proofStatus: ProofStatus,
    override val report: SolveReport?
) : ConstraintProgrammingSolverOutput

data class ConstraintProgrammingUnknownOutput(
    val terminationReason: TerminationReason,
    override val report: SolveReport?
) : ConstraintProgrammingSolverOutput
```

`ConstraintProgrammingSolution` 以稳定变量 ID 保存精确整数值，并提供针对 `BinVar`、`IntVar`、`UIntVar` 和 interval 的类型化读取方法。

### 4.6 conflict 与统一不可行证据

`ConstraintProgrammingConflict` 不建立与 `SolveReport.InfeasibilityEvidence` 平行且无法互转的第二套诊断模型。CP 输出中的 conflict 必须能无损转换为统一证据，并保留以下信息：

- `source`：`NativeIIS`、`ConstraintConflict`、`Farkas`、`ElasticFilter`、`DeletionFilter` 或 `None`。
- 原始约束、变量下界、变量上界、sparse domain 和 assumption 的稳定 ID。
- validity：`Verified`、`Heuristic` 或 `Unknown`，表示是否已经证明当前成员集合不可行。
- minimality：`Irreducible`、`NotChecked` 或 `Partial`。
- completeness、耗时、求解次数、终止原因和缺失/失败原因。
- backend 原生证书或内部 conflict 的可选引用，但不暴露 JNI/native 对象。

现有 `variableBoundIds: Set<VariableId>` 无法区分同一变量的上下界，实施前改为包含 `VariableId + BoundSide` 的结构化成员；sparse domain 使用独立 `VariableDomainRef`，不伪装成连续上下界。现有 `EvidenceExactness` 中 `Exact` 与 `Irreducible` 不是互斥概念，实施前拆分 validity 与 minimality；例如，一个未完成缩减但已证明不可行的 conflict 应表达为 `Verified + Partial`，不能被迫在 `Exact` 与 `Irreducible` 之间二选一。

旧 `LinearInfeasibleSolverOutput.iis`、`QuadraticInfeasibleSolverOutput.iis` 和 `SolverOutputWithIIS` 在兼容期继续存在。新报告以 `InfeasibilityEvidence` 为主；只有证据成员可稳定映射回原始模型时，兼容 facade 才物化旧 IIS model view。弹性/MCS 结果不得通过新报告伪装成 native 或 verified IIS。

## 5. 求解器能力与配置

### 5.1 能力声明

扩展 `SolverModelType`：

```kotlin
enum class SolverModelType {
    LP,
    MIP,
    QP,
    QCP,
    CP
}
```

避免继续向 `SolverCapabilities` 添加大量平铺布尔字段，新增：

```kotlin
enum class ConstraintProgrammingFeature {
    BooleanLogic,
    Reification,
    SparseDomain,
    AllDifferent,
    Element,
    Table,
    Interval,
    OptionalInterval,
    NoOverlap,
    Cumulative,
    Assumption,
    ConflictCore,
    SolutionHint,
    IncrementalSolve
}

enum class ConstraintProgrammingSupportLevel {
    Native,
    ExactLowering,
    Unsupported
}
```

`SolverCapabilities` 增加带默认值的 `constraintProgrammingFeatures: Map<ConstraintProgrammingFeature, ConstraintProgrammingSupportLevel>`，以保持现有构造调用兼容。调用前必须检查能力；`Unsupported` 返回结构化失败，禁止静默降级。

### 5.2 配置

`ConstraintProgrammingSolveOptions` 首版包含：

- time limit
- random seed
- deterministic mode
- solution limit
- relative / absolute objective gap
- log switch
- progress context
- cancellation hook
- whether to collect conflict core
- solver-specific backend configuration

配置无效时返回 `Failed(ErrorCode.IllegalArgument, ...)`，错误消息遵守中英双语格式。

## 6. Logic-Based Benders 契约

### 6.1 变量绑定

新增 `BendersVariableBinding`，显式描述主问题变量如何生成 CP assumption：

```kotlin
interface BendersVariableBinding {
    fun bind(
        masterSolution: ConstraintProgrammingValueSource,
        subproblem: ConstraintProgrammingModel
    ): Ret<BendersSubproblemAssignment>
}
```

绑定必须基于稳定变量 ID 或明确的领域 key，不允许只依赖对象引用相等。

`BendersSubproblemAssignment` 至少包含：

- 主问题变量及其值。
- CP 变量及其固定值。
- 本轮加入的 assumption literals。
- assumption literal 到 master assignment 的反向映射。

### 6.2 子问题结果

```kotlin
sealed interface LogicBasedBendersSubproblemResult {
    val assignment: BendersSubproblemAssignment
}

data class FeasibleSubproblemResult(
    override val assignment: BendersSubproblemAssignment,
    val output: ConstraintProgrammingFeasibleOutput
) : LogicBasedBendersSubproblemResult

data class InfeasibleSubproblemResult(
    override val assignment: BendersSubproblemAssignment,
    val conflict: ConstraintProgrammingConflict?
) : LogicBasedBendersSubproblemResult

data class UnknownSubproblemResult(
    override val assignment: BendersSubproblemAssignment,
    val terminationReason: TerminationReason
) : LogicBasedBendersSubproblemResult
```

### 6.3 cut 生成

```kotlin
interface BendersCutOracle {
    fun feasibilityCuts(
        result: InfeasibleSubproblemResult,
        context: BendersCutContext
    ): Ret<List<BendersMasterCut>>

    fun optimalityCuts(
        result: FeasibleSubproblemResult,
        context: BendersCutContext
    ): Ret<List<BendersMasterCut>>
}
```

首版 master cut 类型：

```kotlin
data class BendersMasterCut(
    val inequality: LinearInequality<Flt64>,
    val kind: BendersCutKind,
    val validity: BendersCutValidity,
    val proofStatus: ProofStatus,
    val source: String
)
```

`BendersCutValidity` 至少区分：

- `Global`：对整个 master 可行域有效。
- `Assignment`：只对当前 assignment 有效，需要 point-cut 编码。
- `Heuristic`：没有全局有效性证明，只能在启发式模式使用。

Exact 模式只允许加入 `Global` cut，或者由 framework 通过已验证的 point-cut 编码转换为全局有效线性约束。

### 6.4 默认 feasibility cut

对于二进制 master assignment，可以生成 no-good cut。

给定当前赋值集合：

```text
S1 = {i | x_i = 1}
S0 = {i | x_i = 0}
```

默认 no-good cut：

```text
sum(1 - x_i, i in S1) + sum(x_i, i in S0) >= 1
```

如果 CP 后端返回 assumption core，只使用 core 中的变量生成 cut，从而得到更强的冲突 cut。

一般整数变量的“不得等于当前向量”是析取约束，不能直接表示为单条线性不等式。`IntegerNoGoodEncoding` 已提供经过范围门禁的辅助二进制编码，`LogicBasedBendersEngine` 可以注册其多约束 cut；engine 仍不默认替调用方生成一般整数 no-good，调用方必须显式选择该编码或提供业务 cut oracle。

### 6.5 默认 optimality cut 的限制

CP 最优值 `Q(x*)` 本身只描述当前 assignment，不能自动推导对所有 `x` 有效的经典 Benders optimality cut。

因此首版规定：

- framework 不根据 `Q(x*)` 自动伪造全局 optimality cut。
- Exact 模式下，优化型 CP 子问题必须提供业务 optimality cut oracle，或提供已验证的 point-cut 上下界编码。
- Exact 模式下，主问题目标含 first-stage cost、`theta` 或其他项时必须提供 `completeObjectiveEvaluator`，显式计算当前 assignment 的完整 incumbent 目标；未提供时只能返回结构化契约缺失，不能把 `Q(x*)` 直接与主问题目标比较。
- feasibility-only CP 子问题可以只使用 no-good/conflict cut。
- Heuristic 模式可以使用带明确标记的启发式 cut，但最终结果不得标记为全局最优。

### 6.6 Benders 迭代生命周期

```text
1. register master model
2. register static CP subproblem
3. compile CP session
4. solve master
5. validate master terminal state
6. bind master solution to CP assumptions
7. solve CP subproblem
8. validate CP terminal state and proof
9. generate cuts through cut oracle
10. validate, deduplicate and register cuts
11. update bounds, gap, trace and progress
12. converge, stop, or continue
13. extract final solution and certificates
```

每轮 trace 至少记录：

- master objective and bound
- subproblem objective and bound
- master/subproblem termination reason
- cut count by kind
- conflict core size
- elapsed time
- current proof mode
- convergence gap

## 7. framework Context 与 Pipeline

### 7.1 拟议接口

```kotlin
interface ConstraintProgrammingPipeline : Pipeline<ConstraintProgrammingModel>

interface BendersSubproblemPipeline {
    fun register(
        model: ConstraintProgrammingModel,
        binding: BendersVariableBinding
    ): Try

    fun extractSolution(
        solution: ConstraintProgrammingSolution
    ): Try = ok
}
```

Context、Aggregation 和 ModelComponent 的职责继续遵循 framework 架构规范：

- Context 对 application 暴露注册入口。
- Aggregation 组织变量、表达式和 CP 结构。
- ModelComponent 保存领域变量及结果提取引用。
- Pipeline 负责单一 CP 约束族或目标族。
- Application 只负责模型创建、上下文组合、求解器选择和结果组装。

### 7.2 建议注册入口

领域 Context 可以按能力提供：

```kotlin
fun registerForConstraintProgramming(
    model: ConstraintProgrammingModel
): Try

fun registerForBendersSubproblem(
    model: ConstraintProgrammingModel,
    binding: BendersVariableBinding
): Try
```

不要求所有 Context 同时支持 MILP 和 CP。需要双建模的领域能力应共享领域数据和稳定变量 key，但可以使用不同的 Pipeline 实现。

## 8. SCIP 与 MIP 降维实现计划

### 8.1 目录结构

```text
ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/constraint_programming/
  ConstraintProgrammingSolver.kt
  ConstraintProgrammingSession.kt
  ConstraintProgrammingSolveOptions.kt
  ConstraintProgrammingSolverOutput.kt
  lowering/
    ConstraintProgrammingToLinearModelLowerer.kt
    ConstraintProgrammingLoweringPolicy.kt
    MipBackedConstraintProgrammingSolver.kt

ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip/src/main/
  fuookami/ospf/kotlin/core/solver/scip/
    ScipConstraintProgrammingSolver.kt
    ScipConstraintProgrammingSession.kt
    ScipConstraintProgrammingCompiler.kt
    ScipConstraintProgrammingStatusMapper.kt
```

Gurobi 不新增独立 CP AST 编译器。调用方通过：

```kotlin
MipBackedConstraintProgrammingSolver(
    linearSolver = GurobiLinearSolver(),
    loweringPolicy = ConstraintProgrammingLoweringPolicy.Strict
)
```

求解可精确降为 MIP 的 CP 子集。其他现有 `LinearSolver` 插件也可以复用该入口。

### 8.2 首版能力矩阵

| CP 能力 | SCIP 插件 | MIP-backed/Gurobi | 首版策略 |
| --- | --- | --- | --- |
| 整数线性约束 | native linear handler | exact linear model | 支持 |
| Bool AND/OR/XOR | native constraint handler | exact binary lowering | 支持 |
| implication/reification | indicator/superindicator | indicator 或 exact big-M | 仅在有限界可证明时支持 |
| sparse integer domain | bound-disjunction 或枚举 | one-hot/disjunction | 有规模门禁 |
| all-different | exact decomposition | exact decomposition | 有规模门禁 |
| element | exact one-hot decomposition | exact one-hot decomposition | 有限 index/domain 才支持 |
| allowed/forbidden table | tuple selection/no-good decomposition | 同左 | 有规模门禁 |
| mandatory fixed-duration interval | start/end 等式 | exact linear model | 支持 |
| no-overlap | cumulative capacity 1 或 pairwise disjunction | pairwise order binaries | 支持固定 duration |
| cumulative | native cumulative handler | time-indexed lowering | SCIP native；MIP 首版默认不支持 |
| optional/variable-duration interval | 当前 JSCIP API 不完整 | 需额外 exact formulation | 不进入首版 |
| assumptions | 每轮加入固定等式并重建模型 | 每轮加入固定等式并重建模型 | 功能支持，非增量 |
| conflict core | 全部活动 assumptions；可选删除缩减 | 同左 | 有效但可能较弱 |
| solution hint | 按插件现有 warm-start 能力 | MIP start | capability 控制 |

所有 exact decomposition 必须满足双向等价，并用小规模穷举 oracle 对照原 CP 语义。超过 tuple 数、domain 大小、时间范围或辅助变量阈值时返回 `Unsupported`，不得生成不可控的大模型。

### 8.3 SCIP session 与 conflict 策略

当前 `jscip:1.0.0` 已暴露 cumulative、AND、OR、XOR、indicator、superindicator、bound-disjunction、set packing/covering/partitioning 和 SOS 等约束创建接口，但未暴露完整的增量 bound 修改、probing 和 conflict graph 提取接口。

首版采用以下正确性优先策略：

1. `ScipConstraintProgrammingSession` 缓存 OSPF snapshot 和编译计划，不缓存可变 SCIP 搜索状态。
2. 每轮 assumptions 作为固定等式编译到新的 SCIP 模型。
3. SCIP 证明不可行时，全部活动 assumptions 构成有效 conflict core。
4. 配置开启 core shrinking 时，通过删除一个 assumption 后重新求解，得到 irreducible 但不保证最小的 core。
5. 只有所有缩减子求解都得到已证明终态时，缩减后的 core 才可进入 Exact Benders。

后续若升级 JSCIP binding 暴露 probing、bound change 和 conflict analysis，再增加真正的增量 session；不得为了性能提前泄露 JNI 类型到 core SPI。

### 8.4 状态与资源边界

SCIP adapter 和 MIP-backed solver 必须：

- 在编译前检查 feature support level。
- 捕获可捕获的 Java/native binding 异常并转换为 `Ret`。
- 提取 objective、best bound、assignment、终止原因和统计信息。
- 在 session 关闭或单次求解完成后释放 solver model、constraint 和 variable 引用。
  - CP activation path 保持稳定 origin ID，禁止用求解器内部序号作为公共身份；线性/二次 native provider 对无 origin 元素明确标注为 `ModelLocal`，对有 origin 元素保留可回查身份。

### 8.5 assumption conflict IIS 算法与降级链

结构化 IIS 路径复用 CP session 的 assumption 能力，但以原模型元素而不是 Benders 主问题赋值作为 assumption 来源：

1. 为每个原始约束创建独立 activation literal，并以单向 enforcement 保持“激活时原约束完整生效”。
2. 为每个有限变量下界、上界和 sparse domain 限制分别创建 activation literal，禁止只使用 `VariableId` 合并两个边界或把离散 domain 冒充连续区间。
3. 编译诊断模型时把 solver 变量建立在经过证明的安全基础值域上，原 bound/domain 只通过 activation 约束生效；无法安全移除限制时返回 `Unsupported`。
4. 使用全部 activation literals 求解；只有后端证明不可行，才产生 verified conflict seed。
5. 后端支持可靠 conflict core 时先投影到原始 activation ID；否则以全部活动 assumptions 为 seed。
6. 对 seed 执行 deletion-based shrinking：移除候选后若仍证明不可行则永久移除；若证明可行则保留；若 `Unknown`、超时或取消则保留并把 minimality 标记为 `Partial`。
7. 最终集合始终重新验证不可行；只有所有必要性检查均得到证明，才标记为 `Irreducible`。

诊断编排不采用单一固定 solver，而是根据原求解器和模型 capability 选择策略：

```text
NativeIISAnalyzer
  -> FarkasAnalyzer（适用时）
  -> ConstraintConflictAnalyzer（仅精确 CP/离散编译范围）
  -> LegacyElasticInfeasibilityAnalyzer
  -> Unavailable evidence
```

`LegacyElasticInfeasibilityAnalyzer` 直接复用当前 `core/solver/iis` 代码，首轮迁移不删除其算法。其输出保留 slack 值和修复候选等解释信息，但来源必须是 `ElasticFilter` / `DeletionFilter`，默认 validity 为 `Heuristic` 或 `Unknown`，除非另有独立验证证明返回成员本身不可行。

诊断策略自身返回 `Ret<InfeasibilityEvidence>`，但报告编排必须把诊断 `Failed` / `Fatal` 转换为 `SolveDiagnostics` 中的结构化问题；它不能把原始求解已经证明的 `Infeasible` 改写成一次求解调用失败。

## 9. 分阶段实施任务

### Phase CP0：求解器边界与 SCIP 探针

- [x] `OSPF-CP-001` 冻结 OSPF 不依赖、不引入 OR-Tools 的边界。
- [x] `OSPF-CP-002` 冻结 core 通用 CP SPI、SCIP 原生实现和 MIP 精确降维三层结构。
- [x] `OSPF-CP-003` 建立最小 JSCIP 探针，验证布尔约束、indicator 和 cumulative 可创建并求解。
- [x] `OSPF-CP-004` 冻结整数缩放和溢出策略。
- [x] `OSPF-CP-005` 冻结 CP 求解器终态到 `SolveReport` 的映射。

验收：不增加新求解器依赖；现有 JSCIP 能求解最小布尔与 cumulative 模型；所有未决 API 能力被标为 `Native`、`ExactLowering` 或 `Unsupported`。

### Phase CP1：core CP 模型与 SPI

- [x] `OSPF-CP-101` 新增 `ConstraintGroupRegistry` 并兼容现有 `MetaModel`。
- [x] `OSPF-CP-102` 新增整数 domain、boolean literal 和 CP expression。
- [x] `OSPF-CP-103` 新增首版 CP constraints。
- [x] `OSPF-CP-104` 新增 interval 和 scheduling constraints。
- [x] `OSPF-CP-105` 新增 `ConstraintProgrammingModel` 和 immutable snapshot。
- [x] `OSPF-CP-106` 新增 solver、session、options 和 output SPI。
- [x] `OSPF-CP-107` 扩展 solver capabilities 和 model type。
- [x] `OSPF-CP-108` 新增 fake solver 测试夹具。

验收：core CP AST 不依赖 SCIP、Gurobi 或其他求解器类型；fake solver 能验证完整建模和结果回填路径。

### Phase CP2A：SCIP CP/CIP 实现

- [x] `OSPF-CP-201` 新增 `ScipConstraintProgrammingSolver`、session 和 compiler。
- [x] `OSPF-CP-202` 实现变量、domain、整数表达式和 objective 编译。
- [x] `OSPF-CP-203` 映射 AND、OR、XOR、indicator、superindicator 和 bound-disjunction。
- [x] `OSPF-CP-204` 映射 mandatory fixed-duration interval、no-overlap 和 cumulative。
- [x] `OSPF-CP-205` 实现 all-different、element 和 table 的受限精确分解。
- [x] `OSPF-CP-206` 实现状态、solution、objective bound、统计和错误映射。
- [x] `OSPF-CP-207` 实现基于模型重建的 assumptions session 和全 assumptions conflict core。
- [x] `OSPF-CP-208` 实现可选 deletion-based core shrinking 和证明门禁。
- [x] `OSPF-CP-209` 实现配置、取消、进度、资源释放和 capability 声明。

验收：Sudoku、固定 duration Job Shop 和 cumulative 调度由 SCIP 插件求解；限制终态不会被映射为已证明最优或不可行。

### Phase CP2B：精确 MIP 降维与 Gurobi

- [x] `OSPF-CP-221` 新增 `ConstraintProgrammingToLinearModelLowerer` 和严格 lowering policy。
- [x] `OSPF-CP-222` 实现整数线性、布尔逻辑、indicator 和 reification 的精确降维。
- [x] `OSPF-CP-223` 实现受规模限制的 sparse domain、all-different、element 和 table 降维。
- [x] `OSPF-CP-224` 实现固定 duration no-overlap 的 pairwise order formulation。
- [x] `OSPF-CP-225` 新增 `MipBackedConstraintProgrammingSolver`，复用现有 `LinearSolver` 状态、hint 和进度能力。
- [x] `OSPF-CP-226` 使用 fake linear solver 验证编译契约，并使用 Gurobi 插件完成定向集成测试。

验收：同一受支持 CP 模型通过 SCIP native 和 Gurobi MIP-backed 两条路径得到一致的可行性与目标值；不支持能力在建模编译阶段失败。

### Phase CP2C：结构化不可行证据与 IIS 迁移

- [x] `OSPF-CP-241` 扩展 `InfeasibilityEvidenceSource`，新增 `ConstraintConflict`，并把证据 validity 与 minimality 拆为正交字段。
- [x] `OSPF-CP-242` 新增可区分上下界的 `VariableBoundRef`、`VariableDomainRef` 和统一 `InfeasibilityMember`，贯穿 snapshot、编译映射和报告。
- [x] `OSPF-CP-243` 新增 backend-neutral `InfeasibilityAnalyzer<M>`、策略 capability 和诊断编排器。
- [x] `OSPF-CP-244` 实现约束/变量界/domain activation、安全基础值域门禁、全 assumptions conflict seed、origin 投影和最终不可行复验。
- [x] `OSPF-CP-245` 实现 deletion-based conflict shrinking，以及 `Verified + Irreducible/Partial/NotChecked` 证明门禁。
- [x] `OSPF-CP-246` 接入 backend native IIS/Farkas，并按模型类型与 capability 优先于通用降级算法。
- [x] `OSPF-CP-247` 将当前弹性/删除过滤封装为 `LegacyElasticInfeasibilityAnalyzer`，保留算法但按实际保证标记为启发式、未知或 MCS 证据。
- [x] `OSPF-CP-248` 将旧 `computeIIS(...)` 和 IIS output 改为兼容 facade；诊断失败写入 `SolveDiagnostics`，不得覆盖已证明的 `Infeasible`。
- [x] `OSPF-CP-249` 增加 native IIS、CP conflict、Farkas、legacy fallback 和诊断失败的策略矩阵测试。

验收：CP activation path 的矛盾约束和矛盾变量界能返回稳定 origin ID 的 verified infeasible subset；线性/二次 native path 对无 origin 元素返回明确的 model-local ID，不宣称跨重建稳定；完成全部缩减证明时标记为不可约；任一缩减求解为 `Unknown` 时不冒充 IIS；连续/二次不支持模型自动走 native 或 legacy 分支；所有诊断失败仍保留原始 `ProblemStatus.Infeasible`。

CP2C-246/248 的 native provider 接入当时不等同于完成 `OSPF-SOL-013`；该缺口已由 CP3
A101/A102/A401/A402 的派生身份、规范化、重建和远程序列化回归补齐。provider 对无 origin
元素仍必须保留 model-local 标记，不能升级为跨重建稳定 ID。

### Phase CP3：framework Logic-Based Benders

- [x] `OSPF-CP-301` 新增 master/subproblem variable binding。
- [x] `OSPF-CP-302` 新增 CP subproblem result 和 proof contract。
- [x] `OSPF-CP-303` 新增 master cut、validity 和 cut oracle。
- [x] `OSPF-CP-304` 实现 binary no-good cut。
- [x] `OSPF-CP-305` 实现全赋值 no-good cut 和缩减 assumption-core conflict cut。
- [x] `OSPF-CP-306` 实现 cut validation、deduplication 和 naming。
- [x] `OSPF-CP-307` 实现 `LogicBasedBendersEngine`。
- [x] `OSPF-CP-308` 实现 Exact/Heuristic 终止契约。
- [x] `OSPF-CP-309` 实现 iteration trace、progress 和 fallback hook。

验收：一个二进制 master + SCIP CP feasibility subproblem 的用例能通过冲突 cut 收敛，且错误终态不能被误报为最优。

### Phase CP4：framework 集成与示例

- [x] `OSPF-CP-401` 新增 `ConstraintProgrammingPipeline`。
- [x] `OSPF-CP-402` 新增 `BendersSubproblemPipeline`。
- [x] `OSPF-CP-403` 提供最小直接 CP core demo。
- [x] `OSPF-CP-404` 提供最小 framework CP Context/Aggregation/Pipeline demo。
- [x] `OSPF-CP-405` 提供 Logic-Based Benders demo。
- [x] `OSPF-CP-406` 更新中英文 README 和架构文档。

验收：下游能够只新增 Context/Pipeline 和 cut oracle，不修改 Benders engine 主循环。

### Phase CP5：增强与迁移

- [x] `OSPF-CP-501` 增加更多全局约束（SCIP 提供受规模门禁的精确分解；MIP-backed 仍按 capability 返回 Unsupported）。
- [x] `OSPF-CP-502` 增加 remote model serialization 与稳定解物化（CP snapshot 已进入 `ModelData`/`SolvePayload`；`RemoteConstraintProgrammingClient` 通过 `resultRef` 回填 Int64/interval 解，并交叉校验状态、值域、interval 与全部 snapshot 约束）。
- [x] `OSPF-CP-503` 评估 checkpoint/resume 能力（已提供 portable snapshot checkpoint 和 capability assessment；native resume 仍由 backend 声明）。
- [x] `OSPF-CP-504` 评估升级 JSCIP binding 以支持增量 bound、probing 和 conflict graph（当前 binding 均未暴露，生产路径明确使用 model-rebuild fallback）。
- [x] `OSPF-CP-505` 评估 demo2 或甘特排程框架迁移（demo2 已完成生产 Benders 迁移；Gantt 暂不迁移，保留现有 MetaModel/列生成路径并记录 CP interval 适配边界）。
- [x] `OSPF-CP-506` 增加一般整数 master no-good encoding。
- [x] `OSPF-CP-507` 增加 optional interval 和 variable duration 的精确支持（MIP-backed 路径；SCIP native 仍为 fixed-duration）。

## 10. 测试矩阵

| 层级 | 测试内容 | 必须覆盖 |
| --- | --- | --- |
| core unit | domain、literal、表达式、约束、snapshot | 空模型、重复 ID、非法 domain、溢出 |
| core contract | solver/session/output | Feasible、Optimal、Infeasible、Unknown |
| SCIP compiler unit | native handler mapping 和 decomposition | 每种首版约束、ID、资源释放 |
| SCIP integration | native CP/CIP solve | Sudoku、Job Shop、Cumulative、Automaton 确定性与规模门禁、Reservoir 同时刻排序/变量液位/不可行终态、终态映射 |
| lowerer property | 穷举 oracle 对照 | 原 CP 与 LinearMetaModel 可行集、目标值一致 |
| MIP-backed contract | fake/Gurobi linear solver | capability、unsupported、hint、状态传播 |
| cross-solver differential | SCIP 与 Gurobi 对照 | 受支持随机小模型可行性和最优值一致 |
| infeasibility contract | 统一证据、成员 ID、validity、minimality | native IIS、CP conflict、Farkas、legacy、unavailable |
| conflict shrinking | assumption seed 与删除缩减 | 约束、上下界、sparse domain、不可约、partial、Unknown |
| IIS compatibility | 旧 `computeIIS` 和旧 output facade | 可物化 IIS、MCS 不冒充 IIS、诊断失败不覆盖结论 |
| framework unit | binding 和 cut oracle | 二进制 no-good、conflict core、cut 去重 |
| Benders contract | 终态和证明 | master 非最优、SP 可行非最优、SP unknown |
| framework integration | Context/Pipeline | direct CP 和 Benders SP 两条注册路径 |
| end-to-end | Logic-Based Benders | 收敛、停滞、超时、取消、fallback |

测试必须验证以下负路径：

- 非整数 master 值无法绑定到整数 CP 变量。
- conflict core 含未知 assumption ID。
- conflict core 含求解器内部辅助约束但缺少稳定 origin ID。
- 同一变量上下界被错误合并为一个证据成员。
- 关闭 bound/domain assumption 后限制仍隐含在 solver 变量基础值域中。
- 无法构造安全基础值域却继续生成 verified conflict。
- 缩减子求解为 `Unknown`、超时或取消却被标记为 `Irreducible`。
- 弹性/MCS 结果被标记为 native、verified 或 irreducible IIS。
- IIS 诊断失败覆盖原始 `Infeasible` 结论。
- Exact 模式收到 heuristic cut。
- Exact 模式收到未证明最优的 CP 子问题结果。
- solver adapter 不支持已注册的 CP feature。
- exact lowering 缺少有限上下界或超过配置规模门禁。
- 非等价或只单向蕴含的 formulation 被错误声明为 `ExactLowering`。
- solver session 已关闭后继续求解。
- callback 或 progress reporter 返回失败。

## 11. 构建与验收策略

任务进行中优先执行受影响模块的增量构建，并把完整输出写入日志。例如：

```bash
mvn compile test-compile -pl ospf-kotlin-core,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-framework -am -T 0.75C > cp-incremental-build.log 2>&1
mvn test -pl ospf-kotlin-core,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-framework -am -T 0.75C > cp-incremental-test.log 2>&1
```

SCIP adapter 和 MIP lowerer 加入后增加对应插件的定向测试、lowering 性质测试和跨求解器测试。最终验收执行：

```bash
mvn clean compile test-compile -T 0.75C > cp-final-build.log 2>&1
mvn test -T 0.75C > cp-final-test.log 2>&1
```

任何失败都基于同一次构建的完整日志分析，不通过反复截断输出来重复运行 Maven。

## 12. 主要风险与控制措施

| 风险 | 影响 | 控制措施 |
| --- | --- | --- |
| CP 没有 LP 对偶证书 | 无法自动生成经典 Benders cut | 使用 Logic-Based Benders 和领域 cut oracle |
| 把当前 CP 最优值误当作全局 cut | 可能排除真正最优解 | cut validity + proof mode 强制校验 |
| 浮点缩放或整数溢出 | 模型语义错误 | 显式 scale、构建期范围分析、失败返回 |
| JSCIP 未暴露完整 SCIP API | 某些 native constraint、增量 session 或 conflict 无法接入 | capability 门禁、模型重建、必要时单独升级 binding |
| exact lowering 不等价 | 漏解、伪解或错误最优证明 | 双向等价审查、穷举 oracle、SCIP/Gurobi differential test |
| table/domain 降维膨胀 | 辅助变量和约束不可控 | tuple/domain/time horizon 规模门禁，超过阈值返回 unsupported |
| session 每轮重建 | Benders 子问题耗时增加 | 缓存 snapshot/编译计划，后续升级 JSCIP 增量 API |
| solver model 资源泄漏 | native 内存增长 | `AutoCloseable`、局部映射、生命周期和重复求解测试 |
| 过早迁移大型业务示例 | API 尚未稳定时产生大量返工 | 先最小 demo，再迁移 demo2/甘特排程 |
| 默认 no-good cut 太弱 | Benders 迭代次数过高 | assumption core、业务强 cut、trace 指标 |
| conflict core 过弱或缩减未证明 | Benders 迭代慢或 cut 无效 | 全 assumptions 安全回退、缩减子求解证明、Exact 门禁 |
| 把 conflict core 或 MCS 误报为 IIS | 用户修复错误模型元素，审计结果失真 | validity/minimality 正交标记、最终不可行复验、legacy 明确降级 |
| CP MVP 不覆盖连续或二次语义 | 无法替换所有线性/二次 IIS 路径 | native IIS/Farkas 优先，精确 capability 门禁，legacy fallback |
| 上下界或辅助约束无法回映射 | 结构化证据歧义或泄露 backend 细节 | `VariableBoundRef`、稳定 origin ID、无法投影时返回 partial/unavailable |
| 诊断失败覆盖求解结论 | 已证明不可行被错误报告为系统失败 | 诊断作为 `SolveDiagnostics` 附属结果，失败只记录结构化 issue |
| SCIP/Gurobi capability 语义漂移 | 同一 CP 模型跨 solver 行为不同 | 三态 capability、统一 contract test、跨求解器对照 |

## 13. MVP 非目标

以下内容不进入第一版：

- CP master problem。
- 混合 CP/MIP 单一求解模型。
- SCIP native optional interval 和 variable duration 的通用编译（MIP-backed 精确子集已支持）。
- 自动把任意 CP 全局约束降为 MIP。
- native checkpoint/resume；当前仍使用 portable snapshot/checkpoint 重建，后端原生能力按 descriptor 声明。
- 未经证明的自动 optimality cut。
- 任意未声明 capability 的全局约束自动降维。
- OSPF 自研生产级 CP 搜索引擎。
- OR-Tools adapter 或依赖。

## 14. MVP 完成定义

满足以下条件后，约束规划 MVP 才算完成：

1. core 用户可以不依赖 framework 构建 CP 模型，并通过注入 solver plugin 直接求解。
2. OSPF 不引入 OR-Tools 依赖或 adapter。
3. SCIP 插件能求解逻辑、table、固定 duration interval 和 cumulative 模型。
4. MIP-backed solver 能通过 Gurobi 求解 capability 声明覆盖的精确降维子集。
5. framework Context/Pipeline 可以注册 CP 子问题。
6. Logic-Based Benders 能处理二进制 master 的 SCIP CP feasibility subproblem。
7. 全 assumptions core 或经证明缩减的 core 可以转换为有效 conflict cut。
8. CP conflict 能输出结构化不可行证据，并在证明充分时输出 inclusion-irreducible IIS。
9. backend native IIS/Farkas 优先；现有弹性/删除过滤作为有明确来源和证据等级的降级分支保留。
10. 诊断失败不会覆盖已证明的 `Infeasible`；CP activation 成员可通过稳定 origin ID 回查，native 线性/二次 fallback 对无 origin 元素明确标记为 model-local。
11. Exact 模式不会接受未证明有效的 cut、非等价 lowering 或未证明终态。
12. 所有失败路径使用 `Try` / `Ret<T>`，求解器异常不会穿透 adapter 边界。
13. 中英文 README、最小 demo、能力矩阵和扩展点测试同步完成。
14. 全量编译与全量测试通过。

### CP5 迁移评估记录

- `RemoteConstraintProgrammingClient` 复用统一 `SolverExecutionPort` 生命周期；远端 payload 的 `ModelData.modelType` 为 `CP`，snapshot schema 和稳定变量/约束 ID 随 payload 传输，`resultRef` artifact 通过 Int64/interval 物化和一致性门禁后才进入 CP output。
- 当前 JSCIP binding 的 `incrementalBoundUpdates`、`probing`、`conflictGraph` 均为 `false`，对应评估对象会返回 `model-rebuild` fallback，不会把每轮重建伪装成增量求解。
- demo2 的生产链路已使用 framework Benders；其主问题和领域约束仍是线性/二次语义，迁移到 CP 不会带来等价收益，因此不强行改写。Gantt 框架继续使用现有 MetaModel/列生成生命周期，后续仅在 optional interval native 能力可用时再评估迁移。

## 15. 推荐实施顺序

严格按以下顺序推进：

```text
CP0 JSCIP 布尔/indicator/cumulative 探针
  -> CP1 core AST、SPI 和 fake solver
  -> CP2A SCIP native compiler 和 direct solve
  -> CP2A assumptions/session/conflict core
  -> CP2B exact MIP lowerer 和 Gurobi 对照
  -> CP2C 结构化不可行证据、IIS shrinking 和 legacy fallback
  -> CP3 Benders binding 和 conflict cut
  -> CP3 完整迭代器与证明门禁
  -> CP4 Context/Pipeline 和示例
  -> CP5 增强与业务迁移
```

第一项实现工作是 `OSPF-CP-003` 最小 JSCIP 能力探针。探针验证当前 binding 与 native library 可用后，再开始批量新增 core 公共 API。
