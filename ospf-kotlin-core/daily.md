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

## 下一轮交接：先补四类型验收测试，再继续泛型化实现

### 当前已完成或已有基础

从当前文档计划和代码抽样看，以下内容已经有一定基础，但不能视为完全泛型化完成：

1. 已有完整的泛型化路线图，覆盖 `math`、`core`、`framework`、solver API、Benders/cut、framework 算法、兼容层和测试门禁。
2. `core` 中已经引入 `IntoValue<V>` 作为 `Flt64` 与泛型值 `V` 的边界转换能力，并在部分类型中使用。
3. token table、cache context、flatten context 等基础结构已有泛型签名和部分回归测试，例如 `GenericTokenTableRegressionTest`。
4. `LinearFunctionSymbolAdapter`、`MathFunctionSymbol`、部分函数符号已有泛型化外层签名和迁移测试，例如 `FunctionSymbolMigrationTest`。
5. `daily.md` 中已经明确原则：`Flt64` 只允许留在 solver adapter、compat/deprecated API、`adapter.flt64`、数值实现和测试基准等边界位置。
6. 现有 `ospf-kotlin-example` 已有大量 `Flt64` 示例和函数符号测试，可作为行为基准，但目前不能证明非 `Flt64` 类型可用。

### 当前主要缺口

当前最大风险是“签名泛型，内部仍然 `Flt64` 特化”的伪泛型状态。典型例子：

- `fuookami.ospf.kotlin.core.intermediate_symbol.function.AbsFunction#registerConstraints` 的入参已经是 `AbstractLinearMechanismModel<V>`，但内部仍然构造 `LinearInequality<Flt64>`、`LinearPolynomial<Flt64>`，并使用 `Flt64.zero`、`Flt64.one`。
- `core.intermediate_symbol.function` 下多类函数仍有类似模式，例如 `And`、`Or`、`UnivariateLinearPiecewise`、`SlackRange` 等。
- 当前测试大多只覆盖 `Flt64`，不足以暴露 `Rtn64`、`FltX`、`RtnX` 下的类型泄漏和 unchecked cast 问题。

因此下一轮不应先继续大规模重写实现，而应先建立四类型验收标准，让后续实现修改有稳定红绿灯。

### 意图

在下一轮泛型化实现前，先把 `Flt64`、`Rtn64`、`FltX`、`RtnX` 四种数值类型纳入单元测试和 `ospf-kotlin-example` 的编译样例，形成明确验收矩阵。

目标不是立即要求四种类型都能真实调用外部 solver 求解，而是先确保建模主链路可用：

- build meta model
- add variables
- add constraints
- add objective
- register intermediate function constraints
- dump mechanism model
- 在 solver adapter 边界集中处理 `V <-> Flt64` 转换

### 建议步骤

1. 确认四种数值类型的基础能力
   - 梳理 `Flt64`、`Rtn64`、`FltX`、`RtnX` 的构造方式。
   - 确认它们是否满足 `where V : RealNumber<V>, V : NumberField<V>`。
   - 为每种类型准备 `IntoValue<V>`，至少支持 `zero`、`one`、`intoValue(Flt64)`、`fromValue(V)`。

2. 新增泛型测试 fixture
   - 在 `ospf-kotlin-core/src/test` 下建立统一测试工具，例如 `GenericNumberCase<V>`。
   - 为四种类型分别提供 case。
   - 后续泛型测试通过 case 矩阵运行，避免重复写四份测试。

3. 补 core 主链路验收测试
   - 覆盖 `LinearMetaModel<V>` 和 `QuadraticMetaModel<V>`。
   - 覆盖 token table、flatten data、constraint、objective、mechanism model dump。
   - 每个测试必须跑 `Flt64`、`Rtn64`、`FltX`、`RtnX`。

4. 补中间函数符号验收测试
   - 第一批至少覆盖三类函数：
     - 线性辅助函数：`Abs`
     - 逻辑函数：`And` 或 `Or` 或 `If`
     - 辅助/分段函数：`Slack` 或 `Max` 或 `Min`
   - 测试重点：
     - 可以创建函数符号。
     - 可以注册辅助变量。
     - 可以执行 `registerConstraints`。
     - 注册出的 constraint 主类型为 `V`，不是 `Flt64`。

5. 先给 `AbsFunction` 写红灯测试
   - 用 `Rtn64` 或 `RtnX` 创建 `AbsFunction<V>`。
   - 调用 `registerConstraints(AbstractLinearMechanismModel<V>)`。
   - 当前实现预计会因内部 `LinearInequality<Flt64>` 特化失败或暴露类型泄漏。
   - 这个失败应作为下一步修复 `AbsFunction` 的明确目标。

6. 补 `ospf-kotlin-example` 泛型样例
   - 新增一个最小样例，例如 `core_demo/GenericNumberDemo.kt`。
   - 同一个简单线性模型分别用 `Flt64`、`Rtn64`、`FltX`、`RtnX` 建模。
   - 样例先要求 build/dump 和编译通过，不强依赖真实 solver。

7. 增加扫描门禁
   - 扫描禁止区域：
     - `ospf-kotlin-core/src/main/.../intermediate_symbol/function`
     - `ospf-kotlin-core/src/main/.../model/mechanism`
     - `ospf-kotlin-core/src/main/.../intermediate_model`
   - 禁止主链路新增或保留未批准的：
     - `LinearInequality<Flt64>`
     - `QuadraticInequalityOf<Flt64>`
     - `LinearPolynomial<Flt64>` 作为泛型实现内部主类型
     - `QuadraticPolynomial<Flt64>` 作为泛型实现内部主类型
     - `Flt64.zero as V`
     - `Flt64.one as V`
     - `this as Flt64`
     - `model as LinearMechanismModel<Flt64>`
     - `model as QuadraticMechanismModel<Flt64>`

8. 记录基线测试结果
   - 跑当前项目约定的 core/example 测试命令。
   - 若失败，按类型记录：
     - API 签名不支持。
     - 实现内部仍 `Flt64` 特化。
     - `IntoValue<V>` 缺失或转换不完整。
     - solver adapter 边界仍有 unchecked cast。
     - 非本轮范围的外部 solver 限制。

### 详细计划

#### A. 测试 fixture 设计

建议新增一个测试辅助文件，集中管理四类型 case：

- `name: String`
- `zero: V`
- `one: V`
- `two: V`
- `minusOne: V`
- `fromDouble(value: Double): V`
- `converter: IntoValue<V>`

测试 case 应尽量只依赖公开 API，不访问实现内部字段。这样可以作为外部用户视角的编译期 API 测试。

#### B. core 主链路测试矩阵

每种类型至少跑以下模型：

1. 线性最小模型
   - 一个连续变量 `x`
   - 一个约束 `x >= 1`
   - 一个目标 `min x`
   - dump mechanism model

2. 线性多变量模型
   - 两个变量 `x`、`y`
   - 约束 `x + 2y <= 10`
   - 目标 `3x + y`
   - 验证 constraint 和 objective 中系数类型为 `V`

3. 二次最小模型
   - 变量 `x`、`y`
   - 二次目标或二次约束
   - dump quadratic mechanism model

4. token/flatten/cache 测试
   - `AutoTokenTable<V>`
   - `ManualTokenTable<V>`
   - `LinearFlattenData<V>`
   - `QuadraticFlattenData<V>`
   - `TokenCacheContexts<V>`

#### C. 中间函数符号测试矩阵

第一批建议选择：

1. `AbsFunction<V>`
   - 输入 `x - 2`
   - 期望生成 abs result、positive slack、negative slack、binary indicator 等辅助结构。
   - 重点验证 `registerConstraints` 使用 `LinearInequality<V>`。

2. `AndFunction<V>` 或 `OrFunction<V>`
   - 输入两个线性表达式。
   - 验证逻辑函数产生的 indicator 约束不再硬编码 `Flt64.one`、`Flt64.zero`。

3. `SlackFunction<V>` 或 `MaxFunction<V>`
   - 验证辅助变量、范围和 Big-M 参数以 `V` 存储和传递。

#### D. example 编译样例

建议新增：

- `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/core_demo/GenericNumberDemo.kt`

内容为一个统一函数：

- `fun <V> buildGenericLinearDemo(case: GenericNumberCase<V>)`
- 分别由 `Flt64`、`Rtn64`、`FltX`、`RtnX` 调用。

样例不应依赖具体外部 solver。若需要 solver，必须明确这是 `Flt64` raw adapter 边界测试，不作为泛型建模能力的必要条件。

#### E. 扫描门禁

建议新增脚本或测试，先用 allowlist 方式逐步收紧。第一轮只针对高风险目录，避免全仓大量历史 `Flt64` 造成噪音。

高风险目录：

- `core/intermediate_symbol/function`
- `core/intermediate_model`
- `core/model/mechanism`

允许保留位置：

- solver adapter
- deprecated compatibility overload
- `math.symbol.adapter.flt64`
- `Flt64` 自身数值实现
- 明确标注为兼容入口的工厂函数
- 测试基准

### 下一轮验收标准

下一轮完成时，至少应满足：

1. `Flt64`、`Rtn64`、`FltX`、`RtnX` 四种类型都有统一测试 fixture。
2. 四种类型都能通过 `LinearMetaModel<V>` 的 build、add variable、add constraint、add objective、dump mechanism model 测试。
3. 四种类型都能通过至少一个 `QuadraticMetaModel<V>` 的 build/dump 测试。
4. `AbsFunction<V>` 对四种类型都能完成 auxiliary token 注册和 constraint 注册。
5. 至少一个逻辑函数和一个辅助/分段函数对四种类型通过同类注册测试。
6. `ospf-kotlin-example` 中存在四类型泛型样例，并且 example 模块能够编译。
7. 禁止区域扫描没有未批准的 `LinearInequality<Flt64>`、`QuadraticInequalityOf<Flt64>`、`LinearPolynomial<Flt64>` 主链路泄漏。
8. 若真实外部 solver 仍只支持 `Flt64` raw type，必须证明转换集中发生在 adapter 边界，并且返回值能够转换为 `FeasibleSolverOutput<V>`。
9. 所有新增测试失败项都必须归类并记录，不能只标记为“泛型化未完成”。

### 执行顺序建议

1. 先写四类型 fixture 和最小 build/dump 测试。
2. 再写 `AbsFunction` 红灯测试。
3. 修复 `AbsFunction`，让它成为函数符号泛型化模板。
4. 复制该模式迁移逻辑函数和辅助/分段函数。
5. 补 example 编译样例。
6. 最后加入扫描门禁，并根据 allowlist 收敛历史 `Flt64`。

这轮交接的核心判断是：先让 `Rtn64`、`FltX`、`RtnX` 进入日常测试矩阵，再继续改实现。否则泛型签名会继续掩盖内部 `Flt64` 特化，导致每轮迭代都只能证明 `Flt64` 可用。

## 进度更新（2026-05-11）

### 本轮已完成

1. `math` 修复（四类型验收前置）
   - `Floating.kt`：`floatingToRational` 改为 `BigDecimal(...).stripTrailingZeros().toPlainString()`，并修复 `num % 5L == 2L` 为 `num % 5L == 0L`。
   - `Rational.kt`：`Rtn64/RtnX` 构造约分改为 `gcd(num.abs(), den.abs())`。

2. `core` 四类型验收主链路
   - 新增四类型 fixture：`GenericNumberCase/GenericNumberCases`。
   - 新增主链路测试：`GenericLinearMetaModelBuildTest`、`GenericQuadraticMetaModelBuildTest`。
   - 新增函数符号四类型测试：
     - `FunctionSymbolGenericRegistrationTest`
     - `FunctionSymbolConditionalGenericRegistrationTest`
     - `FunctionSymbolPiecewiseGenericRegistrationTest`
     - `FunctionSymbolRoundingGenericRegistrationTest`

3. 慢测分层配置（Maven Surefire）
   - 默认排除：`slow,very-slow`
   - `-Pwith-slow-tests`：仅包含 `slow`（排除 `very-slow`）
   - `-Pwith-all-slow-tests`：包含所有慢测

4. `Rounding` 慢测拆分与标记
   - `FunctionSymbolRoundingGenericRegistrationTest` 拆为 4 个测试方法（`Flt64/FltX/Rtn64/RtnX`）。
   - 标签策略：
     - `Flt64/FltX/Rtn64` 标记 `@Tag("slow")`
     - `RtnX` 单独标记 `@Tag("very-slow")`

### 本轮验证结果

1. 默认快测（排除 `slow,very-slow`）
   - 目标组合：`GenericLinearMetaModelBuildTest, GenericQuadraticMetaModelBuildTest, FunctionSymbolGenericRegistrationTest, FunctionSymbolConditionalGenericRegistrationTest, FunctionSymbolPiecewiseGenericRegistrationTest, FunctionSymbolRoundingGenericRegistrationTest`
   - 结果：`BUILD SUCCESS`，`7 tests`，约 `1:10`

2. 中速慢测（`-Pwith-slow-tests`，不含 `RtnX`）
   - 结果：`BUILD SUCCESS`，`10 tests`，约 `4:19`
   - 其中 `FunctionSymbolRoundingGenericRegistrationTest`（3 个用例）约 `186.6s`

3. 全量慢测（`-Pwith-all-slow-tests`，含 `RtnX`）
   - 结果：`BUILD SUCCESS`，`11 tests`，约 `14:16`
   - 其中 `FunctionSymbolRoundingGenericRegistrationTest`（4 个用例）约 `791.5s`

4. `Rounding` 用例耗时分布（Surefire XML）
   - `Flt64`：接近 0s
   - `FltX`：接近 0s
   - `Rtn64`：约 `152.9s`
   - `RtnX`：约 `638.6s`（主要瓶颈）

### 当前结论

1. 四类型验收链路可稳定通过。
2. 日常回归可默认走快测或 `with-slow-tests`，把 `RtnX` 的 `very-slow` 分离到按需全量回归。
3. 下一步优化重点应放在 `RtnX` 相关路径的性能（优先 `Rounding`）。

## 进度更新（2026-05-11 第二轮）

### 本轮已完成

1. `Rounding` 四类型验收测试轻量化
   - 将 `FunctionSymbolRoundingGenericRegistrationTest` 中的机制模型由 `LinearMechanismModel` 改为轻量 `CollectingLinearMechanismModel`（测试桩）。
   - 保留验收语义：
     - `registerAuxiliaryTokens` 与 `registerConstraints` 必须成功。
     - 约束数量必须增长。
     - 约束系数类型仍需保持 `V`（防止 `Flt64` 泄漏）。

2. 四类型 fixture 常量缓存强化
   - `GenericNumberCase` 常量改为实例级缓存值（`zero/one/two/five/ten`）。
   - `cachedConverter` 中 `zero/one` 改为一次构造后复用。
   - `FltX` 也统一走 `cachedConverter`。

### 本轮验证结果

1. `RtnX` 单用例（`with-all-slow-tests`）
   - 命令：`mvn -pl ospf-kotlin-core -Pwith-all-slow-tests "-Dtest=FunctionSymbolRoundingGenericRegistrationTest#floorAndCeilAndRoundShouldRegisterConstraintsForRtnX" test`
   - 结果：`BUILD SUCCESS`
   - `FunctionSymbolRoundingGenericRegistrationTest` 单测耗时：`45.23s`
   - 对比上一轮同口径约 `347s`，显著下降。

2. 中速慢测（`with-slow-tests`）
   - 目标组合：`GenericLinearMetaModelBuildTest, GenericQuadraticMetaModelBuildTest, FunctionSymbolGenericRegistrationTest, FunctionSymbolConditionalGenericRegistrationTest, FunctionSymbolPiecewiseGenericRegistrationTest, FunctionSymbolRoundingGenericRegistrationTest`
   - 结果：`BUILD SUCCESS`，`10 tests`，总耗时约 `1:11`
   - 其中 `FunctionSymbolRoundingGenericRegistrationTest`（3 个 slow 用例）约 `26.28s`

3. 全量慢测（`with-all-slow-tests`）
   - 同上目标组合（含 `RtnX` very-slow）
   - 结果：`BUILD SUCCESS`，`11 tests`，总耗时约 `2:47`
   - 其中 `FunctionSymbolRoundingGenericRegistrationTest`（4 个用例）约 `122.6s`

### 当前结论

1. `Rounding` 四类型验收已从“重型机制模型路径”切换到“约束注册语义验证路径”，稳定通过且耗时显著下降。
2. `very-slow` 档位从“分钟级超长”降至可接受区间，适合纳入日常增强回归。
3. 下一步可继续把同类函数符号验收测试逐步迁移到轻量模型桩，以统一降低非必要的测试计算成本。
