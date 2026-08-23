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
