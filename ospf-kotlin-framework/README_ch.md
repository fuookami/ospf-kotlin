# OSPF Kotlin Framework

:us: [English](README.md) | :cn: 简体中文

`ospf-kotlin-framework` 是 OSPF Kotlin 领域框架（CSP1D、BPP3D、甘特调度等）的共享基础设施模块。它提供求解器抽象、管线建模、持久化表达式 DSL、网络工具、日志上下文和运行心跳追踪。

## 作用范围

本模块覆盖：

1. **求解器抽象** — 列生成、Benders 分解及组合求解器接口，支持 MILP/LP 求解、异步变体和泛型值转换。
2. **管线建模** — 约束管线、列生成管线和启发式分析管线接口；影子价格键/价格/映射/提取器层次。
3. **持久化表达式 DSL** — SQL 谓词下推注解、仓储 API、排序、更新赋值和标量函数 DSL，用于 Ktorm 集成。
4. **网络工具** — 带重试机制和鉴权支持的 HTTP 响应发送。
5. **日志上下文** — 日志推送/保存接口和建造者模式上下文管理。
6. **运行心跳** — 子进度、运行中和完成心跳数据结构，用于长时间求解追踪。
7. **远程求解器** — 分时间片远程求解，支持检查点、对象存储端口和求解执行端口。

明确非目标：

1. 领域专用建模（下料方案、装箱、调度）——属于 `ospf-kotlin-framework-*` 领域模块。
2. 求解器后端实现 —— 属于 `ospf-kotlin-core-plugin-*`。
3. 持久化后端实现 —— 属于 `ospf-kotlin-framework-plugin-persistence-*`。

## 包概览

| 包 | 职责 |
| --- | --- |
| `solver` | 列生成、Benders 分解、组合求解器接口；求解选项；数值类型别名；异步协程作用域 |
| `solver.remote` | 远程求解客户端、领域模型、端口和适配器，用于分时间片求解 |
| `model` | 管线接口（约束/列生成/启发式分析）；影子价格层次 |
| `persistence` | 请求/响应 DTO、日志记录 DAO、持久化 API 控制器、SQL 类型扩展 |
| `persistence.expression` | 谓词下推注解、仓储 API、排序 DSL、更新赋值 DSL、标量函数 DSL |
| `network` | 带重试和鉴权的 HTTP 响应工具 |
| `log` | 日志上下文管理（推送/保存接口、建造者）和日志记录类型 |

## 求解器抽象

### ColumnGenerationSolver

列生成求解器核心接口：

```kotlin
interface ColumnGenerationSolver {
    val name: String

    // MILP 求解
    suspend fun solveMILP(name: String, metaModel: Flt64LinearMetaModel, ...): Ret<Flt64FeasibleSolverOutput>
    suspend fun solveMILP(metaModel: Flt64LinearMetaModel, options: FrameworkSolveOptions): Ret<Flt64FeasibleSolverOutput>

    // LP 求解（返回对偶解用于定价）
    suspend fun solveLP(name: String, metaModel: Flt64LinearMetaModel, ...): Ret<LPResult>

    // 异步变体（CompletableFuture）
    fun solveMILPAsync(...): CompletableFuture<Ret<Flt64FeasibleSolverOutput>>
    fun solveLPAsync(...): CompletableFuture<Ret<LPResult>>

    // 值转换变体（Flt64 -> V）
    suspend fun <V> solveMILPAs(name: String, metaModel: Flt64LinearMetaModel, converter: IntoValue<V>, ...): Ret<FeasibleSolverOutput<V>>
    suspend fun <V> solveLPAs(name: String, metaModel: Flt64LinearMetaModel, converter: IntoValue<V>, ...): Ret<LPResultOf<V>>
}
```

`LPResult` 将可行求解器输出与约束对偶解映射捆绑，对列生成定价至关重要。

### BendersDecompositionSolver

线性和二次 Benders 分解接口：

```kotlin
interface LinearBendersDecompositionSolver {
    val name: String
    suspend fun solveMaster(metaModel: Flt64LinearMetaModel, ...): Ret<Flt64FeasibleSolverOutput>
    suspend fun solveSub(metaModel: Flt64LinearMetaModel, ...): Ret<LinearSubResult>
}
```

`LinearSubResult` 是带有 `Feasible` 和 `Infeasible` 变体的密封接口，遵循 Benders 分解模式。

### 组合求解器

运行多个求解器并选择结果：

| 求解器 | 策略 | 结果选择 |
| --- | --- | --- |
| `SerialCombinatorialLinearSolver` | 串行运行求解器 | 首个成功 |
| `ParallelCombinatorialLinearSolver` | 并行运行求解器 | `First` 或 `Best`（通过 `ParallelCombinatorialMode`） |
| `SerialCombinatorialColumnGenerationSolver` | 串行运行列生成求解器 | 首个成功 |
| `ParallelCombinatorialColumnGenerationSolver` | 并行运行列生成求解器 | `First` 或 `Best` |

### FrameworkSolveOptions

带建造者模式的统一求解选项：

```kotlin
val options = FrameworkSolveOptions.Builder()
    .solveName("my-solve")
    .toLogModel(true)
    .solutionAmount(UInt64(3))
    .registrationStatusCallBack(myCallback)
    .solvingStatusCallBack(mySolvingCallback)
    .build()
```

### 远程求解器

`solver.remote` 子包提供带检查点支持的分时间片远程求解：

- **domain** — 值类型（TaskId、SliceId 等）、错误码、序列化/规范化/存储/任务模型
- **port** — `ObjectStoragePort`（put/get/delete/exists）和 `SolverExecutionPort`（start/resume/await/export/fetch/stop）
- **client** — `RemoteSolverClient` 实现逐轮求解与检查点；`RemoteSolverHttpClient` 用于 HTTP 传输
- **adapter** — `LocalFileObjectStoragePort` 用于本地文件系统存储；`OspfRemoteModelSerializer` 用于 OSPF 序列化格式

## 管线建模

### 管线层次

```
Pipeline<M>              — 约束管线：register() + invoke()
├── CGPipeline<Args, Model, Map>  — 列生成管线：增加 extractor() + refresh()
└── HAPipeline<M>        — 启发式分析管线：增加 calculate() + check()
```

- `Pipeline<M>` — 注册约束组到 `MetaModel` 并执行。
- `CGPipeline<Args, Model, Map>` — 扩展 `Pipeline`，增加影子价格提取和刷新，用于列生成定价。
- `HAPipeline<M>` — 扩展 `Pipeline`，增加启发式目标计算和解有效性检查。

`PipelineList<M>`、`CGPipelineList<Args, Model, Map>` 和 `HAPipelineList<M>` 类型别名支持通过 `operator fun invoke(model: M): Try` 进行批量执行。

### 影子价格体系

```
ShadowPriceKey(limit: KClass<*>)
  ↓
ShadowPrice(key, price: Flt64)
  ↓
AbstractShadowPriceMap<Args, M>  — put/get/putOrAdd/remove/shrink + 提取器注册
  ↓
ShadowPriceExtractor<Args, M> = (AbstractShadowPriceMap, Args) -> Flt64
```

`extractShadowPrice()` 从 `CGPipelineList` 刷新影子价格并注册提取器。`IntermediateSymbol<*>.refresh()` 处理中间符号的对偶值。

## 持久化表达式 DSL

`persistence.expression` 子包提供带 KSP 注解处理的类型安全 SQL 谓词下推框架。详见专属文档：

- [persistence/expression/README_ch.md](src/main/fuookami/ospf/kotlin/framework/persistence/expression/README_ch.md)

核心组件：

- `@PredicateEntity` / `@PredicateField` — KSP 注解，用于模式生成
- `ExpressionRepository<E>` — 仓储接口，支持 find/count/exists/update/delete
- `SortBy` / `SortItem` — 排序 DSL，支持 null 值排序
- `UpdateAssignments` — UPDATE SET DSL（set/setNull/setExpr）
- `UnsupportedPredicatePolicy` — 不可下推谓词的处理策略
- `ScalarFunctionDsl` — SQL 标量函数（lower、upper、trim、length、coalesce）

## 网络工具

`Response.kt` 提供带重试的 HTTP 响应发送：

- `Authorization` 接口和 `BasicAuthorization` 数据类
- `ResponseRetry` 配置（最大尝试次数、延迟）
- `response()` 扩展函数，支持 POST 和重试

## 日志上下文

`LogContext.kt` 提供日志上下文管理：

- `Pushing` / `Saving` 接口用于日志推送/保存操作
- `LogContext` 带建造者模式用于组装推送/保存处理器
- `LogRecordPO<T>` 用于日志记录持久化

`LogRecord.kt`（`log/` 包中）定义：

- `LogRecordType` 枚举（Info、Warning、Error、Fetal）
- `LogRecordRPO` / `LogRecordRDAO` 用于 Ktorm 实体和表定义

## 运行心跳

`RunningHeartBeat.kt` 定义三个用于长时间求解进度追踪的心跳数据结构：

- `SubProgressHeartBeat` — 预估时间、进度百分比、可选消息
- `RunningHeartBeat` — 任务 ID、运行时间、预估时间、优化率、时间戳
- `FinnishHeartBeat` — 任务 ID、总运行时间、状态码、消息、时间戳；伴生工厂方法支持成功和错误构造

## 使用示例

### 列生成与管线

```kotlin
// 定义约束管线
val constraintPipelines: PipelineList<LinearMetaModel<Flt64>> = listOf(
    DemandPipeline(),
    CapacityPipeline(),
    YieldPipeline()
)

// 注册并执行所有管线
val ret = constraintPipelines(metaModel)
if (ret is Failed) return ret

// 求解 LP 用于定价
val lpResult = solver.solveLP(
    name = "pricing-lp",
    metaModel = metaModel
)

// 提取影子价格
val shadowPriceMap = MyShadowPriceMap()
val refreshRet = extractShadowPrice(
    shadowPriceMap = shadowPriceMap,
    pipelineList = cgPipelines,
    model = metaModel,
    shadowPrices = lpResult.value.dualSolution
)
```

### 组合求解

```kotlin
// 并行尝试多个求解器，取最优结果
val combinatorialSolver = ParallelCombinatorialColumnGenerationSolver(
    solvers = listOf(solver1, solver2),
    mode = ParallelCombinatorialMode.Best
)
val result = combinatorialSolver.solveMILP(metaModel = metaModel)
```

### 值转换

```kotlin
// 使用 Flt64 求解并转换结果为 FltX
val result = solver.solveMILPAs<FltX>(
    name = "my-solve",
    metaModel = metaModel,
    converter = FltX.toIntoValue()
)
```

## 本地验证

编译框架模块：

```powershell
mvn -B -ntp -pl ospf-kotlin-framework -DskipTests compile
```

运行框架测试：

```powershell
mvn -B -ntp -pl ospf-kotlin-framework test
```
