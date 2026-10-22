# Solver Package

:us: [English](README.md) | :cn: 简体中文

求解器抽象层，定义列生成、Benders 分解和组合求解器接口。

## 接口层次

```
ColumnGenerationSolver
  ├── solveMILP / solveMILPAsync        (MILP 求解)
  ├── solveMILPWithStatus               (保留 MILP 终态)
  ├── solveLP / solveLPAsync            (LP 求解，返回对偶解)
  ├── solveLPWithStatus                 (保留 LP 终态)
  ├── solveMILPAs / solveMILPAsAsync    (带值转换的 MILP 求解)
  ├── solveLPAs / solveLPAsAsync        (带值转换的 LP 求解)
  └── LPResult / LPResultOf<V>         (LP 结果含对偶解)

LinearBendersDecompositionSolver
  ├── solveMaster / solveMasterAs       (主问题求解)
  ├── solveSub / solveSubAs             (子问题求解)
  └── LinearSubResult (Feasible | Infeasible)

QuadraticBendersDecompositionSolver
  ├── solveMaster / solveMasterAs       (二次主问题)
  ├── solveSub / solveSubAs             (二次子问题)
  └── QuadraticSubResult (Feasible | Infeasible)
```

`solveMILPWithStatus` 和 `solveLPWithStatus` 将不可行结果保留为
`MILPSolveResult.Infeasible` 或 `LPResultWithStatus.Infeasible`。对于可行 LP，
调用方必须检查 `LPResult.status == SolverStatus.Optimal`，才能将其对偶解作为精确定价证书。

## 组合求解器

| 类 | 策略 | 结果选择 |
| --- | --- | --- |
| `SerialCombinatorialLinearSolver` | 串行 | 首个成功 |
| `ParallelCombinatorialLinearSolver` | 并行 | `First` 或 `Best` |
| `SerialCombinatorialQuadraticSolver` | 串行 | 首个成功 |
| `ParallelCombinatorialQuadraticSolver` | 并行 | `First` 或 `Best` |
| `SerialCombinatorialColumnGenerationSolver` | 串行 | 首个成功 |
| `ParallelCombinatorialColumnGenerationSolver` | 并行 | `First` 或 `Best` |

`ParallelCombinatorialMode` 枚举控制并行组合求解器的结果选择策略：
- `First` — 返回第一个成功结果
- `Best` — 等待所有求解器完成，返回目标值最优的结果

## FrameworkSolveOptions

统一求解选项，收敛各类快捷求解入口的分散参数：

| 属性 | 说明 |
| --- | --- |
| `name` | 求解名称 |
| `toLogModel` | 是否输出模型日志 |
| `solutionAmount` | 期望解数量 |
| `registrationStatusCallBack` | 注册状态回调 |
| `solvingStatusCallBack` | 求解状态回调 |
| `valueConversionPolicy` | 值转换策略（默认 `Strict`） |
| `bendersIterationLimit` | Benders 迭代次数限制 |
| `bendersStallIterationLimit` | Benders 停滞迭代次数限制 |

建造者用法：

```kotlin
val options = FrameworkSolveOptions.build {
    name = "my-solve"
    toLogModel = true
    solutionAmount = UInt64(3)
}
```

## 类型别名速查

`FrameworkNumberAliases.kt` 为各数值类型提供元模型、求解器输出和解池的便捷别名：

| 别名 | 展开形式 |
| --- | --- |
| `Flt64LinearMetaModel` | `LinearMetaModel<Flt64>` |
| `FltXLinearMetaModel` | `LinearMetaModel<FltX>` |
| `Rtn64LinearMetaModel` | `LinearMetaModel<Rtn64>` |
| `RtnXLinearMetaModel` | `LinearMetaModel<RtnX>` |
| `Flt64QuadraticMetaModel` | `QuadraticMetaModel<Flt64>` |
| `FltXQuadraticMetaModel` | `QuadraticMetaModel<FltX>` |
| `Rtn64QuadraticMetaModel` | `QuadraticMetaModel<Rtn64>` |
| `RtnXQuadraticMetaModel` | `QuadraticMetaModel<RtnX>` |
| `FltXSolveReport` | `SolveReport<FltX>` |
| `Rtn64SolveReport` | `SolveReport<Rtn64>` |
| `RtnXSolveReport` | `SolveReport<RtnX>` |

`ColumnGenerationSolver.kt` 中额外定义了 `Flt64` 专用别名：

| 别名 | 展开形式 |
| --- | --- |
| `Flt64LinearMetaModel` | `LinearMetaModel<Flt64>` |
| `Flt64SolveReport` | `SolveReport<Flt64>` |
| `Flt64SolutionPool` | `List<Solution<Flt64>>` |

## Remote Solver 架构

`solver.remote` 子包实现分时间片远程求解：

```
client/                     客户端
  RemoteSolverClient        逐轮求解与检查点管理
  RemoteSolverHttpClient    HTTP 传输层
  RemoteSolverRuntimeConfig 运行时配置（租户、节点、时间量子、轮次）

domain/                     领域模型
  ValueTypes                值类型（TaskId, SliceId, NodeId 等）
  Errors                    错误码与异常
  SerializedModels          序列化模型
  NormalizedModels          规范化模型
  StorageModels             存储模型
  TaskModels                任务模型
  ExecutionModels           执行模型

port/                       端口
  ObjectStoragePort         对象存储接口（put/get/delete/exists）
  SolverExecutionPort       求解执行接口（start/resume/await/export/fetch/stop）

adapter/                    适配器
  localfs/LocalFileObjectStoragePort     本地文件系统存储适配
  ospf/OspfRemoteModelSerializer         OSPF 序列化格式适配
```

### CP Benders 与远程报告契约

`LogicBasedBendersEngine` 将主问题绑定、assumption、cut、证明状态和收敛证据分离管理。`Exact` 模式只接受已验证且全局有效的 cut，并要求主问题上下界间隙通过验证；可行子问题没有最优性证书时仍是 `Feasible`，不会提升为 `Optimal`。

`RemoteConstraintProgrammingClient.solveOutput()` 发送带版本的 CP snapshot，并从结果 artifact 物化 `variableValuesById` 和 `intervalValues`。客户端会校验值域、interval 等式、所有 snapshot 约束、目标表达式、raw/resultRef 一致性、指纹和 proof 声明。CP 整数使用 JSON `Long`；旧 DTO 仍可读取，但不能把未经验证的结果提升为更强结论。`ConstraintProgrammingCheckpointCodec` 与远程 checkpoint store 使用带完整性摘要的 portable v2 envelope，恢复语义是从 snapshot 重建；不支持 native 搜索状态续跑。

### 身份、可移植性与能力边界

模型元素身份是显式的，并分为两个作用域：

| 作用域 | 含义 | 是否可跨重建使用 |
| --- | --- | --- |
| `Stable` | 建模入口提供了 `id`、`namespace`、`schemaVersion` 及可选 `origin`。 | 通过指纹校验后可用于诊断、报告、远程 DTO 和 portable checkpoint。 |
| `ModelLocal` | 只有当前模型实例内可确定的身份。 | 可用于本地诊断，但不得冒充跨重建绑定。 |

缺少身份元数据的旧 payload 仍按 `ModelLocal` 读取，绝不会被隐式升级为 `Stable`。checkpoint 的可移植性意味着它包含经过验证的 snapshot 并能重建模型，不等于 native 搜索状态恢复；厂商句柄、transformed tree、JNI 指针和求解器内部搜索状态都不能跨 checkpoint 边界传输。

求解器复用也遵循同一边界。默认 CP 和 MIP-backed 路径每次 attempt 都重建后端模型。只有 backend descriptor 暴露并通过测试的 capability 时才能选择 `reuse` 或 `reoptimization`；求解器名称、warm start 或 solution hint 本身不代表 native 复用或精确恢复。能力发布必须通过 `plans/solver_cp.md` 中统一的终态、身份、provenance、取消和资源释放测试。

迁移示例：将模型迁移到远程 API 时保留序列化身份字段；恢复 checkpoint 前校验模型、配置和求解器指纹。如果字段缺失或作用域为 `ModelLocal`，应保持本地身份并重建模型，不得强制转换为稳定绑定。

## 异步协程作用域

`FrameworkAsync.kt` 提供共享的 `CoroutineScope`（`SupervisorJob` + `Dispatchers.Default`），用于框架层异步求解的 `CompletableFuture` 创建。

## 动态行与列定价投影

`ColumnProjection<V>` 是无 BPP 语义的共享线性分解能力，不是联合 CG/Benders 求解器。
它消费 `LinearMetaModel.state`，用 `ColumnDecision` 显式绑定局部变量与配置块；未绑定变量保留为全局变量。
`ProjectedColumn` 只保存局部决策值，不保存一份易过期的原始行系数。

- `master` 从当前源快照重建 Phase I LP、目标 LP、最终整数主问题或资源补全模型。
  行系数和定价系数来自同一行：`a(column) = sum(a_j * column_j)`，`rc = sum((c_j - sum(pi_i*a_ij))*column_j) - sigma`。
  跨块/全局行仅投影到主问题并通过对偶进入定价，不复制为每块的局部限制；局部行可通过 `pricingModel` 注册。
- 右端、全局变量和目标常数只计一次；最大化目标统一为最小化。资源补全保留原变量类型并扣除相应列贡献。
- `ColumnMasterRegistration` 绑定实际约束对象、源版本、投影版本和主问题结构版本，不依赖名称、token 顺序或跨模型行序。
  `refreshShadowPrice` 要求完整、有限、属于当前注册的原始行对偶；调用方必须先确认 LP 已最优。
- 新行、源目标/边界变化、`columnsChanged()`、每次重建和主问题直接修改均使相应旧结果不可消费。
  普通 `pricingRequest` 必须使用当前阶段对偶，只有显式 `seed = true` 才允许零对偶。
  同轮多个块先消费同一对偶并验证请求版本，再统一加列。列生成器应在接收定价证明及提取结果前再次校验版本。

调用方负责局部配置可行性、整数性、定价证明和新增行数学有效性。列范围在重建时按显式容差检查；
旧列违反收紧后的范围时返回错误，调用方需删列或重新定价，不能静默使用旧列。
当前采用安全重建和结构比较，未实现自动割强化、联合迭代、branch-and-price 或求解器原生增量矩阵修改。

`extractSolution` 使用本次注册冻结的列向量，按原始变量身份合并并输出原模型自身 token 顺序；拒绝过期注册、非整数列选择和不完整值。调用方仍需独立复核原始完整模型。
