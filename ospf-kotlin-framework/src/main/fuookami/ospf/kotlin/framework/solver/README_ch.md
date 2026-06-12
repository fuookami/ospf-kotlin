# Solver Package

:us: [English](README.md) | :cn: 简体中文

求解器抽象层，定义列生成、Benders 分解和组合求解器接口。

## 接口层次

```
ColumnGenerationSolver
  ├── solveMILP / solveMILPAsync        (MILP 求解)
  ├── solveLP / solveLPAsync            (LP 求解，返回对偶解)
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
| `FltXFeasibleSolverOutput` | `FeasibleSolverOutput<FltX>` |
| `Rtn64FeasibleSolverOutput` | `FeasibleSolverOutput<Rtn64>` |
| `RtnXFeasibleSolverOutput` | `FeasibleSolverOutput<RtnX>` |

`ColumnGenerationSolver.kt` 中额外定义了 `Flt64` 专用别名：

| 别名 | 展开形式 |
| --- | --- |
| `Flt64LinearMetaModel` | `LinearMetaModel<Flt64>` |
| `Flt64FeasibleSolverOutput` | `FeasibleSolverOutput<Flt64>` |
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

## 异步协程作用域

`FrameworkAsync.kt` 提供共享的 `CoroutineScope`（`SupervisorJob` + `Dispatchers.Default`），用于框架层异步求解的 `CompletableFuture` 创建。
