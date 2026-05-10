# math、core、framework 完全泛型化计划

## 目标

将 `ospf-kotlin-math`、`ospf-kotlin-core`、`ospf-kotlin-framework` 从“以 `Flt64` 为主、外层局部泛型”的状态推进到“建模、表达式、中间模型、框架算法全链路支持泛型数值类型”的状态。

外部求解器若只能接收 double，可以继续以 `Flt64` 作为 solver adapter 的原生值类型，但 `Flt64` 不应泄漏到泛型建模 API、框架算法 API、约束/目标/解输出的主类型签名中。

## 总体原则

- 泛型主类型统一记为 `V`，约束为 `where V : RealNumber<V>, V : NumberField<V>`。
- `Flt64` 只允许出现在明确的兼容层、求解器适配层、`adapter.flt64` 包、测试基准和数值转换实现中。
- 建模层、机制模型层、框架算法层的主 API 返回 `FeasibleSolverOutput<V>`、`Solution<V>`、`LinearInequality<V>`、`QuadraticInequalityOf<V>`。
- `IntoValue<V>` 作为 `Flt64` 与 `V` 的边界转换能力，但不能替代真正的泛型内部数据结构。
- 旧 `Flt64` API 可以保留为 deprecated overload，直到迁移完成后再统一移除或降级为 thin adapter。

## 阶段 0：边界定义与现状基线

### 工作项

1. 明确两类数值域：
   - 建模值类型 `V`：用户表达式、约束、目标、求解结果、框架算法内部使用。
   - 求解器原生值类型 `Raw`：外部 LP/MIP/QP solver 实际接受的数值，默认 `Flt64`。
2. 建立命中扫描脚本或命令，统计三个模块 `src/main` 中的 `Flt64`、`adapter.flt64`、`toFlt64`、`toDouble`、`LinearMetaModel<Flt64>`、`FeasibleSolverOutput<Flt64>`。
3. 标记允许保留 `Flt64` 的包和文件：
   - `math.symbol.adapter.flt64`
   - solver plugin 或 solver adapter 边界
   - deprecated compatibility overload
   - `Flt64` 自身数值实现和测试
4. 记录当前能编译、能通过的测试集合，作为迁移基线。

### 验收标准

- 有一份明确的 `Flt64` 允许清单和禁止清单。
- `mvn test` 或当前项目约定的核心测试命令可在迁移前跑通，若不能跑通，需要记录失败原因。
- 后续阶段每次完成都能用同一组命令对比回归。

## 阶段 1：math 通用符号能力上移

### 工作项

1. 梳理 `math.symbol` 下已经泛型化的核心类型：
   - `LinearMonomial<T>`
   - `QuadraticMonomial<T>`
   - `CanonicalMonomial<T>`
   - `LinearPolynomial<T>`
   - `QuadraticPolynomial<T>`
   - `CanonicalPolynomial<T>`
   - `LinearInequality<T>`
   - `QuadraticInequalityOf<T>`
2. 将 `symbol.adapter.flt64` 中可通用的逻辑迁移到 `symbol.operation` 或新的泛型 adapter：
   - normalize/combine terms
   - evaluate
   - compile
   - convert
   - latex
   - matrix form
   - serde
3. 为泛型操作补齐必要参数：
   - `zero`
   - `one`
   - `isZero`
   - `format`
   - `numberParser`
   - `symbolComparator`
4. `adapter.flt64` 改为 thin wrapper，只提供默认 `Flt64` 参数。
5. 检查 `Duration`、`geometry`、`chaotic_operator` 等天然 double/float 领域代码，决定是否属于完全泛型化范围；如果不是，标记为数学数值实现或浮点专用算法，不作为建模泛型阻塞。

### 验收标准

- 泛型 `LinearPolynomial<V>`、`QuadraticPolynomial<V>` 可以完成 normalize、evaluate、serde/latex 的核心路径。
- `adapter.flt64` 中不再承载唯一实现，只负责传入 `Flt64.zero`、`Flt64.one`、`Flt64` parser/formatter。
- `math` 模块测试覆盖至少包含 `Flt64` 和一个非 `Flt64` 数值类型。

## 阶段 2：core token、flatten、constraint 基础结构泛型化

### 工作项

1. 泛型化 token table 创建逻辑，移除 `MetaModel.kt` 中 `createTokenTable(...): AbstractMutableTokenTable<Flt64>` 到 `AbstractMutableTokenTable<V>` 的强转。
2. 移除或隔离 `AbstractMetaModel.flt64Tokens`。
3. 泛型化 flatten 数据结构和相关转换：
   - `LinearFlattenData<V>`
   - `QuadraticFlattenData<V>`
   - `toLinearFlattenData`
   - `toQuadraticFlattenData`
   - merge/normalize/multiply 工具函数
4. 泛型化 relation 和 constraint 实现：
   - `LinearRelationImpl<V>`
   - `QuadraticRelationImpl<V>`
   - `LinearConstraintImpl<V>`
   - `QuadraticConstraintImpl<V>`
5. 清理 `Flt64.zero as V`、`this as Flt64`、`model as LinearMechanismModel<Flt64>` 这类不安全转换。
6. 固定变量 `fixedVariables` 从 `Map<AbstractVariableItem<*, *>, Flt64>` 迁移为 `Map<AbstractVariableItem<*, *>, V>`，只在 solver dump adapter 中转为 `Flt64`。

### 验收标准

- `LinearMetaModel<V>` 和 `QuadraticMetaModel<V>` 的 token、constraint、sub-object 主存储均为 `V`。
- `core.model.mechanism` 主流程不需要通过 `flt64Tokens` 创建目标或约束。
- 除兼容层和 solver adapter 外，`core` 中不再出现 `LinearFlattenData<Flt64>` / `QuadraticFlattenData<Flt64>` 作为主 API。
- 至少一个非 `Flt64` 的 `LinearMetaModel<V>` 可以完成 add variable、add constraint、add objective、dump mechanism model。

## 阶段 3：中间符号与函数符号泛型化

### 工作项

1. 逐个迁移 `core.intermediate_symbol.function` 下的函数符号：
   - `Abs`
   - `Max` / `Min`
   - `Prod`
   - `BigM`
   - `Slack`
   - `And` / `Or` / `If`
   - `Floor` / `Ceil` / `Round`
   - `Sin` / `Cos`
   - 其他当前硬编码 `Flt64` 的函数
2. 所有函数符号注册约束时使用 `AbstractLinearMechanismModel<V>` 或 `AbstractQuadraticMechanismModel<V>`，避免 star projection 后再强转到 `Flt64`。
3. 常量创建统一使用：
   - `converter.zero`
   - `converter.one`
   - `converter.intoValue(Flt64(...))`
   - 或 `V.constants`
4. 范围、上下界、Big-M、epsilon、tolerance 等参数需要随 `V` 存储；只有 solver 输出边界保留 `Flt64`。
5. 将 `SolverBoundaryCasts` 收敛为真正的 adapter 边界，避免成为泛型逃生口。

### 验收标准

- 中间符号注册约束时不依赖 `MathFunctionSymbolBase<Flt64>`。
- 对至少三类函数符号完成非 `Flt64` 建模测试：线性函数、逻辑函数、分段/辅助变量函数。
- `SolverBoundaryCasts` 中 unchecked cast 数量显著下降，并且每个剩余 cast 都有明确边界说明。

## 阶段 4：solver API 分层重构

### 工作项

1. 重构 solver 接口，使主泛型入口返回 `V`：
   - `solve(model: LinearMetaModel<V>, converter: IntoValue<V>): Ret<FeasibleSolverOutput<V>>`
   - `solve(model: LinearMechanismModel<V>, converter: IntoValue<V>): Ret<FeasibleSolverOutput<V>>`
   - quadratic 同理
2. 明确外部求解器适配层：
   - `LinearSolverRaw<Flt64>`
   - `QuadraticSolverRaw<Flt64>`
   - 或等价命名
3. dump intermediate model 时允许从 `V` 转为 solver raw type，但转换必须集中在 adapter 中。
4. solution pool、IIS、dual solution、reduced cost、shadow price 等输出统一经过 `IntoValue<V>` 回到 `V`。
5. deprecated 旧接口：
   - `invoke(model: LinearMetaModel<Flt64>)`
   - `invoke(model: LinearMechanismModel<Flt64>)`
   - `Ret<FeasibleSolverOutput<Flt64>>`
   保留但委托到新泛型入口。

### 验收标准

- `AbstractLinearSolver` / `AbstractQuadraticSolver` 不再把 `FeasibleSolverOutput<Flt64>` 作为唯一主返回类型。
- `solveV` 不再通过 `model as LinearMechanismModel<Flt64>` 或 `model as QuadraticMechanismModel<Flt64>` 实现。
- `Flt64` 求解器适配层仍能通过原有测试。
- 非 `Flt64` 模型求解后返回 `FeasibleSolverOutput<V>`，包括 obj、gap、solution。

## 阶段 5：Benders、cut、dual 体系泛型化

### 工作项

1. 泛型化 cut 生成：
   - `generateOptimalCut`
   - `generateFeasibleCut`
   - `generateOptimalCutById`
   - `generateFeasibleCutById`
2. 参数从 `Flt64` 改为 `V`：
   - objective
   - fixedVariables
   - dualSolution
   - farkasDualSolution
   - generated cuts
3. raw solver dual values 在 solver adapter 转换为 `V` 后再进入框架算法。
4. 线性和二次 cut 都返回泛型 inequality：
   - `List<LinearInequality<V>>`
   - `List<QuadraticInequalityOf<V>>`
5. 保留 `Flt64` by-id 兼容 API，但内部调用泛型版本。

### 验收标准

- `LinearMechanismModel<V>` / `QuadraticMechanismModel<V>` 能生成 `V` 类型 Benders cuts。
- `core` 中 Benders cut 主逻辑不再直接创建 `Flt64.zero`、`Flt64.one`。
- cut 生成测试覆盖最优 cut、可行 cut、by-id dual lookup、线性和二次情形。

## 阶段 6：framework 泛型化

### 工作项

1. 泛型化列生成接口和实现：
   - `ColumnGenerationSolver<V>`
   - `SerialCombinatorialColumnGenerationSolver<V>`
   - `ParallelCombinatorialColumnGenerationSolver<V>`
2. 泛型化 Benders decomposition：
   - linear master/sub result
   - quadratic master/sub result
   - cut 类型
   - fixed variables
   - dual/farkas dual
3. 泛型化组合求解器：
   - `SerialCombinatorialLinearSolver<V>`
   - `ParallelCombinatorialLinearSolver<V>`
   - `SerialCombinatorialQuadraticSolver<V>`
   - `ParallelCombinatorialQuadraticSolver<V>`
4. 泛型化 framework model：
   - `ShadowPrice<V>`
   - `AbstractShadowPriceMap<Args, M, V>`
   - `Pipeline<V>`
   - objective extractor/checker
5. 在 framework 层提供 `Flt64` typealias 或 deprecated wrapper，兼容旧 API。

### 验收标准

- `framework` solver 包中主接口不再绑定 `LinearMetaModel<Flt64>` / `QuadraticMetaModel<Flt64>`。
- `ShadowPrice`、`Pipeline` 不再以 `Flt64` 作为唯一值类型。
- 列生成、Benders、组合求解器可以基于 `V` 编译，并能用 `Flt64` 兼容入口跑通原测试。

## 阶段 7：迁移兼容层与弃用策略

### 工作项

1. 为旧 API 增加明确 deprecation message：
   - 指向新的泛型 API
   - 标注计划移除版本
2. 对常用 `Flt64` DSL 保留快捷入口：
   - `LinearMetaModel(...)` 默认仍可返回 `LinearMetaModel<Flt64>`
   - 泛型用户通过显式 converter 创建
3. README 和模块文档更新：
   - 泛型建模示例
   - `Flt64` solver adapter 说明
   - 非 `Flt64` 类型的限制
4. 清理迁移中临时注释、临时强转、重复 overload。

### 验收标准

- 旧 `Flt64` 使用方不需要一次性全量迁移即可编译。
- 新泛型 API 有清晰示例。
- deprecated API 覆盖所有旧入口，且没有无提示行为变化。

## 阶段 8：测试与质量门禁

### 工作项

1. 单元测试矩阵至少覆盖：
   - `Flt64`
   - 一个定点数或有理数类型
   - 一个高精度或大数类型
2. 覆盖路径：
   - polynomial/inequality operation
   - meta model build
   - mechanism model dump
   - intermediate model dump
   - solve and solveAsync
   - solution pool
   - Benders cuts
   - column generation
   - shadow price
   - serde/export
3. 添加编译期 API 测试，确保泛型签名可被外部模块正常调用。
4. 对 solver adapter 转换策略增加测试：
   - strict conversion
   - overflow
   - non-finite
   - precision loss
5. 做一次全仓扫描，确认禁止区域没有新增 `Flt64` 主 API。

### 验收标准

- `ospf-kotlin-math`、`ospf-kotlin-core`、`ospf-kotlin-framework` 测试全部通过。
- 非 `Flt64` 建模链路至少能完成 build、dump、solve result conversion。
- 禁止区域 `Flt64` 扫描为零或仅剩有批准说明的 adapter/compat 位置。
- 所有 deprecated 旧 API 都有对应新 API 测试。

## 建议优先级

1. 先完成阶段 0 和阶段 2，解决 `core` 内部主数据结构仍伪泛型的问题。
2. 再做阶段 3 和阶段 4，打通从表达式到求解器的泛型主链路。
3. 阶段 5 和阶段 6 放在主链路稳定后做，否则 Benders/column generation 会反复跟着底层 API 改。
4. 阶段 1 可与阶段 2 并行推进，但必须避免同时重写同一批 polynomial/flatten 调用点。
5. 阶段 7 和阶段 8 贯穿整个迁移，不要等最后才补兼容和测试。

## 最终验收定义

- 用户可以使用非 `Flt64` 的 `V` 创建 `LinearMetaModel<V>` 和 `QuadraticMetaModel<V>`。
- 变量、表达式、中间符号、约束、目标、机制模型、框架算法均以 `V` 保存和传递。
- 外部 solver 若使用 `Flt64`，转换只发生在明确 adapter 边界。
- 求解结果、solution pool、dual、cut、shadow price、pipeline objective 均能以 `V` 返回。
- `Flt64` 旧接口仍可用，且只作为兼容包装，不再承载唯一业务实现。
