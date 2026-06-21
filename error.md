# 错误语义化评估与改进计划

## 背景

上一阶段已完成错误处理迁移，主要代码路径已从抛异常迁移到 `Result` / `Ret<T>` / `Try` 返回错误模式。后续问题不是“把所有返回错误都新增成语义化错误类型”，而是识别哪些错误需要被上层程序分支、协议边界、重试/降级策略、用户纠错或领域诊断消费。

当前全局错误码仍集中在 `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/error/Code.kt` 的 `ErrorCode`，`Ret<T>` 仍是 `Result<T, ErrorCode, Error<ErrorCode>>`。因此应保持全局 `ErrorCode` 粗粒度、稳定，不建议把每个返回点都扩展成新的全局枚举值。

本阶段的语义化手段不止"携带 detail"一种。`Err` / `LazyErr` / `ExErr` / `LazyExErr` 均为 `open class`，可直接被命名子类型继承，调用方通过 `when (error is YourErrorSubclass)` 或读取子类型属性稳定断言错误原因，无需依赖 message 字符串匹配。该模式与"`ExErr` 携带 detail"可并存：对已有独立 sealed 错误类型（如 `CoreError` / `SolverError` / `EinsumError`），优先复用其结构作为子类型携带的 detail 或直接作为子类型继承来源；对尚未类型化的重复错误构造点（如 solver not found、incompatible unit 等），新增命名子类型去重。两种手段都不改变 `Ret<T>` 签名，不产生跨模块级联。

## 目标

1. 保留 `ErrorCode` 作为跨模块、跨协议边界的粗粒度错误码；确有控制流价值时可在 `ErrorCode` 上新增枚举项，但需保证编号不与现有值冲突、不重排已用编号。
2. 对需要程序化处理的错误补充语义化结构，手段二选一或并用：
   - 以 `ExErr(ErrorCode, message, detail)` 携带结构化 detail；
   - 定义继承 `Err` / `LazyErr` / `ExErr` / `LazyExErr` 的命名子类型，调用方按错误类型断言。
3. 避免把普通参数错误、内部不变量和一次性错误消息过度类型化。
4. 优先消除 `ApplicationException` / `ApplicationFailed` / `Other` 的含义混用。
5. 保持 public API 兼容：不改 `Ret<T>` 签名，不引入 `RetOf<T, C>` 级联，错误语义通过错误对象本身或 detail 表达。

## 非目标

1. 不把所有 `Failed(ErrorCode.IllegalArgument, "...")` 都迁移成领域错误。
2. 不为类型化而随意扩展全局 `ErrorCode`；仅当现有粗粒度码无法承载明确的控制流分支时才新增，新增时必须有调用方消费。
3. 不在基础工具层引入业务领域错误。
4. 不为了类型化而破坏现有 solver、framework、persistence public API。
5. 不把 `Ret<T>` 改成 `RetOf<T, DomainErrorCode>`，避免跨模块签名级联。

## 判断标准

一个错误值得语义化，至少应满足以下条件之一：

1. 调用方需要按错误类型分支处理，例如停止、重试、降级、换 solver、改走 client filter。
2. 错误跨 HTTP、DTO、任务调度、远程 solver、持久化 translator 等协议边界传递。
3. 用户可以根据错误类型执行明确修复动作，例如切换参数、关闭不支持能力、补充字段。
4. 同一粗粒度 `ErrorCode` 下存在多种业务含义，且测试或调用方需要稳定断言。
5. 日志、监控或追踪需要稳定 reason code，而不是解析 message。

不值得语义化的情况：

1. 低层参数错误，例如轴越界、负 step、单位不可比较、shape 不匹配。
2. 集合为空、找不到元素等通用工具错误，优先复用 `DataEmpty` / `DataNotFound`。
3. 一次性内部不变量，message 已足够定位且没有外部消费。
4. demo/example 中未形成稳定 API 的错误。

## 已观察到的现状

1. 高频返回错误主要集中在：
   - `ErrorCode.IllegalArgument`
   - `ErrorCode.ApplicationException`
   - `ErrorCode.ApplicationFailed`
   - `ErrorCode.ApplicationError`
   - `ErrorCode.Other`
2. 真正被程序控制流消费的错误码主要集中在 OR/solver：
   - `ORModelInfeasible`
   - `ORModelUnbounded`
   - `ORModelInfeasibleOrUnbounded`
   - `ORSolutionInvalid`
   - `SolverNotFound`
3. 已存在但未完全贯穿的语义化结构：
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/error/CoreError.kt`
   - `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/multiarray/einsum/EinsumError.kt`
   - `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/solver/remote/domain/Errors.kt`
   - `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression/UnsupportedPredicatePolicy.kt`
   - `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/Convert.kt` 中通过 `Failed(..., additionalValue)` 携带转换失败 detail 的做法。

## 推荐设计

语义化手段有两种，可二选一或并用，均不改 `Ret<T>` 签名：

### 手段一：`ExErr` 携带结构化 detail

适用于已有独立 sealed 错误类型（如 `CoreError` / `SolverError` / `EinsumError`）或只需附加结构化字段的场景。detail 作为 `ExErr.value` 携带，调用方通过 `error` 的 additional value 读取。

```kotlin
Failed(ExErr(ErrorCode.IllegalArgument, message, DomainErrorDetail(...)))
```

### 手段二：继承 `Err` / `LazyErr` / `ExErr` / `LazyExErr` 的命名子类型

适用于重复构造点去重、调用方需要按错误类型稳定断言的场景。基类均为 `open class`，可直接继承。子类型携带语义化字段，调用方通过 `when (error is YourErrorSubclass)` 分支。

```kotlin
class SolverNotFoundError(
    val solver: String
) : Err<ErrorCode>(
    code = ErrorCode.SolverNotFound,
    message = "No solver valid: $solver"
)

open class LocalizedError(
    code: ErrorCode,
    val localizedMessage: SystemText
) : LazyErr<ErrorCode>(
    code = code,
    message = { formatTemplate(localizedMessage.defaultText, localizedMessage.args) }
)
```

调用方通过 `when (error is SolverNotFoundError)` 稳定断言，不依赖 message 字符串匹配。

### 选择原则

1. 已有独立 sealed 错误类型（`CoreError` / `SolverError` / `EinsumError`），优先复用其结构：作为子类型携带的 detail 或直接继承。
2. 重复构造点（如 `SolverNotFound` + `"No solver valid."` 出现 18+ 次），提取为命名子类型去重。
3. 一次性、内部不变量错误保留 `Failed(ErrorCode.X, message)`，不类型化。
4. 不引入 `RetOf<T, DomainErrorCode>`，避免跨模块签名级联；public 边界统一用 `Ret<T>` / `ErrorCode`。
5. 每个新子类型必须有至少一个调用方消费，不为类型化而类型化。

## 优先事项

### P0：远程求解错误保留语义 detail

涉及文件：

- `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/solver/remote/domain/Errors.kt`
- `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/solver/remote/client/RemoteSolverHttpClient.kt`

问题：

`RemoteSolverErrorCode` 已经区分了 `NO_ELIGIBLE_NODE_AVAILABLE`、`NODE_OFFLINE`、`TASK_FAILED_HARD_TIMEOUT`、`STORAGE_IO_FAILED` 等，但 `RemoteSolverHttpClient` 目前会映射成 `IllegalArgument` / `ApplicationError` / `ApplicationFailed`，语义在返回边界丢失。

计划：

1. 定义远程求解语义错误，例如 `RemoteSolverFailureError : LazyErr<ErrorCode>`，或保留 `RemoteSolverFailureDetail` 并通过 `ExErr` 携带。
2. `failedRemote` 返回语义化错误对象，保留：
   - `RemoteSolverErrorCode`
   - message
   - metadata
   - HTTP status
   - taskId / sliceId / requestId（有上下文时）
3. 不改变 public `Ret<T>` 签名。
4. 为 timeout、storage、invalid task state、no node 等路径补充测试。

验收：

1. 调用方可通过 `error is RemoteSolverFailureError` 或 `ExErr.value is RemoteSolverFailureDetail` 识别远程错误原因。
2. HTTP status、taskId、sliceId、requestId（有上下文时）必须进入语义化错误对象。
3. 原有按 `ErrorCode` 判断的代码仍兼容。

### P0：solver/OR 失败细节结构化

涉及文件：

- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverFailureSupport.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/error/CoreError.kt`
- `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/solver/*Combinatorial*Solver.kt`

问题：

`ORModelInfeasible`、`ORModelUnbounded` 已经有明确控制流价值，不需要拆分。但 timeout、license、numerical、environment lost、modeling exception 等更适合保留结构化 detail。

计划：

1. 优先复用或完善 `SolverError` / `CoreError`。
2. 对重复构造点定义命名错误子类型，例如 `SolverNotFoundError`、`SolverEnvironmentLostError`、`SolverSolvingError`。
3. `SolverFailureSupport` 的 helper 保留粗粒度 `ErrorCode`，但返回命名错误子类型或携带 `SolverError` detail 的 `ExErr`。
4. 组合求解器继续按 `ErrorCode` 停止，但日志和上层可读取语义化错误对象。

验收：

1. 现有 stop-code 行为不变。
2. solver 插件失败时能通过错误类型或 detail 稳定区分 timeout、license、environment lost、numerical failure。
3. `"No solver valid."` 这类重复构造点不再裸返回 `Failed(ErrorCode.SolverNotFound, "...")`。

### P1：persistence unsupported predicate 语义化

涉及文件：

- `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression/UnsupportedPredicatePolicy.kt`
- `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-*/src/main/.../translator/*Translator.kt`

问题：

已经有 `UnsupportedPredicatePolicy` 和 `PredicateTranslation.Unsupported`，但实际 translator 在 fail-fast 和 client-filter 未实现时返回 `IllegalArgument`，调用方只能解析 message。

计划：

1. 定义 `UnsupportedPredicateError : LazyErr<ErrorCode>` 或 `UnsupportedPredicateDetail`，字段至少包括：
   - expression type
   - reason
   - policy
   - backend name
2. fail-fast 返回 `ErrorCode.IllegalArgument` + 语义化错误对象。
3. client-filter 未实现返回 `ErrorCode.ApplicationFailed` 或 `ApplicationError` + 语义化错误对象，避免混同用户参数错误。
4. MyBatis、MongoDB、Ktorm translator 保持一致。

验收：

1. unsupported predicate 的三个策略可通过错误类型或 detail 被测试稳定区分。
2. 不需要解析错误 message 判断原因。
3. Boolean translator 与 Scalar translator 的 unsupported 行为一致。

### P1：BPP3D 领域校验错误补充 detail

涉及文件示例：

- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/CylinderShapeContract.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Package.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/model/Load.kt`
- `ospf-kotlin-framework-bpp3d/bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/ColumnGenerationApplicationService.kt`

问题：

圆柱能力、连续半径优化 gap、unsupported geometry、混合 demand policy 等属于用户可修正的领域输入错误，当前大多压成 `IllegalArgument`。

计划：

1. 定义小范围的 BPP3D 语义错误对象（优先命名子类型，必要时 detail）例如：
   - `UnsupportedCylinderCapability`
   - `ContinuousCylinderRadiusOptimizationUnsupported`
   - `InvalidDemandMode`
   - `MixedDemandPolicyRequired`
   - `UnsupportedPackingGeometry`
2. 不为普通正数校验、单位兼容校验逐一新增类型。
3. 优先把已有 enum/detail 复用起来，例如连续半径 gap。

验收：

1. API 调用方能通过错误类型或 detail 区分“输入值非法”和“当前能力不支持”。
2. 错误消息仍保留足够上下文。
3. 不把所有 `IllegalArgument` 都迁移为领域错误，只覆盖用户可纠错且需要分支处理的路径。

### P1：`ApplicationException` / `Other` 的明显误用清理

涉及文件示例：

- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/functional/Collection.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/parallel/Find.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/parallel/MinMax.kt`
- `ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/parallel/MaxMin.kt`
- `ospf-kotlin-framework-gantt-scheduling/**/TimeWindow.kt`
- `ospf-kotlin-framework-gantt-scheduling/**/TaskStepConflictConstraint.kt`

问题：

集合为空、未找到元素不应使用 `ApplicationException`；未实现/暂不支持不应泛化为 `Other`。

计划：

1. 集合为空改用 `DataEmpty`。
2. 查找失败改用 `DataNotFound`。
3. 暂未实现/暂不支持改用命名错误子类型或结构化 unsupported detail，或至少改为更合适的 `ApplicationFailed` / `ApplicationError`。
4. 若只是基础工具函数，不新增领域 detail。

验收：

1. `ApplicationException` 只保留外部库兼容或真正异常边界。
2. `Other` 不再承载明确可分类错误。
3. 基础工具层不引入业务领域子类型。

### P2：math/multiarray 的语义 detail 按需收敛

涉及文件示例：

- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/multiarray/einsum/*`
- `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/Convert.kt`
- `ospf-kotlin-multiarray/src/main/fuookami/ospf/kotlin/multiarray/*`
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/*`

问题：

这些路径里很多错误是低层参数错误，不应全部类型化。但 `Convert.kt` 已经通过 additional value 携带转换失败 detail，是可以复用的模式。`EinsumError` 已有结构，但当前 Result 返回中多处会丢失 detail。

计划：

1. 保留大多数 `IllegalArgument + message`。
2. 只有外部 API 需要读取 shape/dimension/index 信息时，再用命名子类型或 `ExErr` 携带 `EinsumError`。
3. 不新增大量全局错误码。

验收：

1. 不扩大低层基础库 API 的复杂度。
2. 关键数学转换失败路径可通过错误类型或 detail 稳定断言。

## 建议执行顺序

1. 先做 P1 的低风险清理：`ApplicationException` -> `DataEmpty` / `DataNotFound`。
2. 做 remote solver detail，因为其协议边界最明确，收益最高。
3. 做 persistence unsupported predicate，统一 MyBatis/MongoDB/Ktorm 行为。
4. 做 solver detail，确保组合求解器行为不变。
5. 做 BPP3D 领域 detail，先覆盖圆柱能力和 mixed demand policy。
6. 最后按需收敛 math/multiarray detail。

## 测试建议

1. 每个新增语义错误类型或 detail 至少覆盖一个构造路径和一个上层读取路径。
2. 对 public API 保持原有 `ErrorCode` 断言，新增错误类型/detail 断言，不替代原断言。
3. remote solver 应补充 HTTP error envelope、非 2xx、任务失败、对象存储失败、反序列化失败测试。
4. persistence translator 应对 `FailFast`、`AlwaysFalse`、`ClientFilter` 三种策略分别测试。
5. BPP3D 应覆盖 unsupported cylinder capability、continuous radius gap、mixed demand policy。

## 风险与注意事项

1. 不要把 `Ret<T>` 大范围改成 `RetOf<T, DomainErrorCode>`，否则会造成跨模块级联修改。错误对象子类型化不等于签名类型化，二者不要混淆。
2. 新增错误子类型或 detail 类型时避免依赖上层模块，保持模块依赖方向正确。
3. `ErrorCode` 值是序列化/跨边界稳定契约；确有必要可以新增枚举项，但不得重排或复用编号，新增项必须有明确调用方消费。
4. message 仍要保留，因为日志、旧调用方和协议响应仍依赖它。语义化不等于删除 message。
5. 如果错误子类型或 detail 会跨 HTTP/DTO 边界传输，需要明确序列化模型，不要直接暴露内部 class。
6. 示例中的 `SystemText` / `formatTemplate` 只是本地化错误的可选形态；若当前仓库没有对应基础设施，不应把建立本地化系统作为本轮错误语义化的前置条件。
7. 每个新子类型应有稳定命名和最小必要字段；不要为了类型化复制一份与 message 等价、无人消费的字段。

