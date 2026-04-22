# P3 基线冻结与兼容面清单

日期：2026-04-22（修订版，修正统计口径与分类错误）

---

## 1. Core 包结构冻结

### 1.1 当前顶层包

```
core/
├── variable/          # 变量 + Token + TokenList（混放）
├── intermediate_model/ # 全部模型层混放（basic/mechanism/intermediate + TokenTable/TokenCache）
├── intermediate_symbol/ # 符号体系（IntermediateSymbol + FunctionSymbol）
├── model/             # Model 接口 + callback + status
└── solver/            # 求解器体系
```

### 1.2 Rust 目标顶层包

```
core/
├── variable/          # 纯变量
├── token/             # Token + TokenList + TokenTable + TokenCache
├── symbol/            # 对应当前 intermediate_symbol
└── model/
    ├── basic/         # BasicModel, BasicMechanismModel, Model interfaces
    ├── mechanism/     # MetaModel, MechanismModel, Constraint, MetaConstraint, SubObject
    ├── intermediate/  # LinearTriadModel, QuadraticTetradModel, Cell, SparseMatrix
    └── callback/      # CallBackModel, CallBackModelInterface
```

---

## 2. Core 主类型清单

### 2.1 variable 包

| 类型 | 泛型化 | 说明 |
|------|--------|------|
| `AbstractVariableItem<T, Type>` | ✅ | 变量基类 |
| `AnyVariable<V>` | ✅ | 任意类型变量 |
| `Token<V>` | ✅ | 泛型 Token |
| `AbstractTokenList<T>` | ✅ | 泛型 TokenList 基类 |
| `TokenList<T>` | ✅ | 泛型 TokenList |
| `MutableTokenList<T>` | ✅ | 泛型可变 TokenList |
| `AutoTokenList<T>` | ✅ | 自动管理 TokenList |
| `ManualTokenList<T>` | ✅ | 手动管理 TokenList |
| `VariableType<T>` | ✅ | 变量类型 |
| `IndependentVariableItem<T, Type>` | ✅ | 独立变量 |
| `VariableCombination<T, Type, S>` | ✅ | 组合变量 |
| `Range<T, V>` | ✅ | 变量范围 |

### 2.2 intermediate_model 包

| 类型 | 泛型化 | 目标子包 |
|------|--------|----------|
| `BasicModel<V>` | ✅ | model.basic |
| `BasicMechanismModel<V>` | ✅ | model.basic |
| `MechanismModel<V>` | ✅ | model.mechanism |
| `LinearMechanismModel<V>` | ✅ | model.mechanism |
| `QuadraticMechanismModel<V>` | ✅ | model.mechanism |
| `MetaModel<V>` | ✅ | model.mechanism |
| `LinearMetaModel<V>` | ✅ | model.mechanism |
| `QuadraticMetaModel<V>` | ✅ | model.mechanism |
| `AbstractMetaModel<V>` | ✅ | model.mechanism |
| `Constraint<V, P>` | ✅ | model.mechanism |
| `ConstraintImpl<V, P>` | ✅ | model.mechanism |
| `SubObject<V>` | ✅ | model.mechanism |
| `LinearSubObject<V>` | ✅ | model.mechanism |
| `QuadraticSubObject<V>` | ✅ | model.mechanism |
| `LinearTriadModel` | ❌ Flt64 | model.intermediate |
| `QuadraticTetradModel` | ❌ Flt64 | model.intermediate |
| `BasicLinearTriadModel` | ❌ Flt64 | model.intermediate |
| `BasicQuadraticTetradModel` | ❌ Flt64 | model.intermediate |
| `Cell<V>` | ✅ | model.intermediate |
| `LinearCell<V>` | ✅ | model.intermediate |
| `QuadraticCell<V>` | ✅ | model.intermediate |
| `SparseVector<V>` | ✅ | model.intermediate |
| `SparseMatrix<V>` | ✅ | model.intermediate |
| `TokenTable` | ❌ TokenListF64 | token |
| `MutableTokenTable` | ❌ MutableTokenListF64 | token |
| `ConcurrentTokenTable` | ❌ TokenListF64 | token |
| `AbstractTokenTable<V>` | ✅ | token |
| `LinearFlattenData<T>` | ✅ | token |
| `QuadraticFlattenData<T>` | ✅ | token |
| `TokenCacheContexts` | ❌ Flt64 | token |
| `ConstraintRelation` | ✅ | model.mechanism |
| `LinearRelation` | ✅ | model.mechanism |
| `ObjectCategory` | ✅ | model.mechanism |
| `RegistrationStatus` | ✅ | model.mechanism |
| `MetaDualSolution` | ❌ Flt64 | model.mechanism |
| `MetaConstraintGroup` | ✅ | model.mechanism |
| `LinearInequalityConstraint` | ❌ Flt64 | model.mechanism |
| `QuadraticInequalityConstraint` | ❌ Flt64 | model.mechanism |

### 2.3 intermediate_symbol 包

| 类型 | 泛型化 | 说明 |
|------|--------|------|
| `IntermediateSymbol<V>` | ✅ | 符号基接口 |
| `LinearIntermediateSymbol<V>` | ✅ | 线性符号 |
| `QuadraticIntermediateSymbol<V>` | ✅ | 二次符号 |
| `FunctionSymbol` | ❌ Flt64 固化 | 函数符号 |
| `LinearFunctionSymbol` | ❌ Flt64 固化 | 线性函数符号 |
| `SymbolCombination<Sym, S>` | ✅ | 符号组合 |
| `LinearExpressionSymbol` | ❌ Flt64 | 线性表达式符号 |

### 2.4 model 包

| 类型 | 泛型化 | 目标子包 |
|------|--------|----------|
| `Model` | ❌ AddableTokenCollectionF64 | model.basic |
| `LinearModel` | ❌ | model.basic |
| `QuadraticModel` | ❌ | model.basic |
| `CallBackModel` | ❌ Flt64 | model.callback |
| `CallBackModelInterface` | ❌ Flt64 | model.callback |
| `ModelBuildingStatus` | ✅ | model.basic |

### 2.5 solver 包

| 类型 | 泛型化 | 说明 |
|------|--------|------|
| `AbstractLinearSolver` | ❌ | 求解器边界保留 |
| `AbstractQuadraticSolver` | ❌ | 求解器边界保留 |
| `SolverOutput` | ❌ Flt64 | 求解器输出边界保留 |
| `FeasibleSolverOutput` | ❌ Flt64 | 求解器输出边界保留 |

---

## 3. Flt64 固化点分类

### 3.1 合法边界保留（不需要泛型化）

| 位置 | 原因 |
|------|------|
| `LinearTriadModel` / `QuadraticTetradModel` | MechanismModel -> IntermediateModel 转换边界 |
| `SolverOutput` / `FeasibleSolverOutput` | 求解器输出边界 |
| `AbstractLinearSolver` / `AbstractQuadraticSolver` | 求解器接口边界 |
| `CallBackModel` | 回调模型边界 |
| `FunctionSymbol` / `LinearFunctionSymbol` | 函数符号设计为 Flt64 固化 |
| `LinearExpressionSymbol` | 线性表达式符号 |
| `MetaDualSolution` | 对偶解边界 |
| `LinearInequalityConstraint` / `QuadraticInequalityConstraint` | 约束展平边界 |
| `LinearConstraintInput` | 约束输入边界 |
| `MathInequalityDsl` | DSL 边界 |
| `Cell` 实现类 (`LinearCellImpl`, `QuadraticCellImpl`) | 求解器交互边界 |

### 3.2 P3-2 泛型化范围（应泛型化）

| 位置 | 当前状态 | 目标 |
|------|----------|------|
| `TokenTable` | `tokenList: TokenListF64` | `TokenTable<V>` |
| `MutableTokenTable` | `tokenList: MutableTokenListF64` | `MutableTokenTable<V>` |
| `ConcurrentTokenTable` | `tokenList: TokenListF64` | `ConcurrentTokenTable<V>` |
| `TokenCacheContexts` | Flt64 缓存（LinearFlattenContext/QuadraticFlattenContext/ValueCacheContext/RangeCacheContext） | 泛型化缓存链路 |
| `Model` 接口 | `AddableTokenCollectionF64` | 泛型化或保留但标注 |
| `LinearModel` / `QuadraticModel` | 继承 Flt64 固化 Model | 评估泛型化可行性 |

**TokenCacheContexts 说明**：它是 token/symbol 计算缓存（缓存 IntermediateSymbol 的展平/求值/范围结果），不是求解器输出边界。应纳入 P3-2 泛型化范围。

### 3.3 F64 typealias 兼容层（保留，标记 @Deprecated）

共 37 个 `*F64` typealias，分布在：
- `variable/Token.kt`: 1 (TokenF64)
- `variable/TokenList.kt`: 6 (AbstractTokenListF64, TokenListF64, AddableTokenCollectionF64, AbstractMutableTokenListF64, MutableTokenListF64, AutoTokenListF64, ManualTokenListF64)
- `variable/AnyVariable.kt`: 1 (AnyVariableF64)
- `intermediate_model/Constraint.kt`: 3
- `intermediate_model/MechanismModel.kt`: 5
- `intermediate_model/Cell.kt`: 3
- `intermediate_model/BasicMechanismModel.kt`: 1
- `intermediate_model/MetaModel.kt`: 6
- `intermediate_model/SparseMatrix.kt`: 2
- `intermediate_model/SubObject.kt`: 2
- `intermediate_model/TokenCacheContext.kt`: 2
- `intermediate_model/TokenTable.kt`: 4
- `intermediate_symbol/IntermediateSymbol.kt`: 3
- `intermediate_symbol/function/Product.kt`: 1

---

## 4. Framework 对 Core 的依赖清单

### 4.1 framework 模块（10 文件引用 core）

| 文件 | 引用的 core 包 |
|------|----------------|
| `model/Pipeline.kt` | intermediate_model (LinearTriadModelView, QuadraticTetradModelView, MetaConstraintGroup, MetaDualSolution, MetaModel), model (Model) |
| `model/ShadowPrice.kt` | intermediate_symbol (IntermediateSymbol), intermediate_model (LinearDualSolution, MetaDualSolution, MetaModel, toMeta) |
| `solver/ColumnGenerationSolver.kt` | solver.output, model (Solution), intermediate_model (LinearDualSolution, LinearMetaModelF64, RegistrationStatusCallBack) |
| `solver/QuadraticBendersDecompositionSolver.kt` | solver.output, model (Solution), intermediate_model.*, variable (AbstractVariableItem) |
| `solver/SerialCombinatorialLinearSolver.kt` | intermediate_model (LinearTriadModelView), solver, model (Solution) |
| `solver/SerialCombinatorialQuadraticSolver.kt` | intermediate_model (QuadraticTetradModelView), solver, model (Solution) |
| `solver/ParallelCombinatorialLinearSolver.kt` | intermediate_model (LinearTriadModelView, ObjectCategory), solver, model (Solution) |
| `solver/ParallelCombinatorialQuadraticSolver.kt` | intermediate_model (QuadraticTetradModelView, ObjectCategory), solver, model (Solution) |
| `solver/SerialCombinatorialColumnGenerationSolver.kt` | solver.output, intermediate_model (LinearMetaModelF64, RegistrationStatusCallBack) |
| `solver/ParallelCombinatorialColumnGenerationSolver.kt` | solver.output, intermediate_model (LinearMetaModelF64, ObjectCategory, RegistrationStatusCallBack) |

### 4.2 framework Flt64 使用（6 文件）

| 文件 | 说明 |
|------|------|
| `RunningHeartBeat.kt` | Flt64 引用 |
| `model/Pipeline.kt` | Flt64 引用 |
| `model/ShadowPrice.kt` | Flt64 引用 |
| `solver/ColumnGenerationSolver.kt` | Flt64 引用 |
| `solver/QuadraticBendersDecompositionSolver.kt` | Flt64 引用 |
| `persistence/SqlType.kt` | Flt64 引用 |

---

## 5. Example 依赖清单

### 5.1 基本信息

- 位置：`E:\workspace\ospf\examples\ospf-kotlin-example`
- Kotlin 源文件数：310
- POM parent：`ospf-kotlin-parent:1.0.71`
- 依赖：`ospf-kotlin-starter`, `ospf-kotlin-starter-gantt-scheduling`, `ospf-kotlin-starter-csp1d`, `ospf-kotlin-core-plugin-gurobi`, `ospf-kotlin-core-plugin-scip`, `ospf-kotlin-core-plugin-heuristic`

### 5.2 Example 对 `core.frontend` 的引用（596 条 import，191 文件）

| 旧 frontend 包 | import 行数 | 迁移目标 |
|----------------|------------|----------|
| `core.frontend.model.mechanism.*` | 189 | `core.intermediate_model.*` |
| `core.frontend.expression.polynomial.*` | 103 | `math.symbol.polynomial.*` |
| `core.frontend.inequality.*` | 77 | `math.symbol.inequality.*` |
| `core.frontend.expression.monomial.*` | 73 | `math.symbol.monomial.*` |
| `core.frontend.expression.symbol.*` | 52 | `core.intermediate_symbol.*` |
| `core.frontend.variable.*` | 50 | `core.variable.*` |
| `core.frontend.expression.symbol.linear_function.*` | 46 | `core.intermediate_symbol.function.*` |
| `core.frontend.model.callback.*` | 2 | `core.model.callback.*` |
| `core.frontend.model.*` | 2 | `core.model.*` |
| `core.frontend.expression.symbol.quadratic_function.*` | 1 | `core.intermediate_symbol.function.*` |
| `core.frontend.expression.symbol.LinearIntermediateSymbols1` | 1 | `core.intermediate_symbol.LinearIntermediateSymbols1` |

### 5.3 Example 对 `core.backend` 的引用（61 条 import，50 文件）

| 旧 backend 包 | import 行数 | 迁移目标 |
|---------------|------------|----------|
| `core.backend.plugins.scip.*` | 46 | **无需迁移**（当前仓库仍为 `core.backend.plugins.scip.*`） |
| `core.backend.solver.config.*` | 7 | `core.solver.config.*` |
| `core.backend.plugins.gurobi.*` | 3 | **无需迁移**（当前仓库仍为 `core.backend.plugins.gurobi.*`） |
| `core.backend.plugins.heuristic.pso.*` | 2 | **无需迁移**（当前仓库仍为 `core.backend.plugins.heuristic.*`） |
| `core.backend.intermediate_model.*` | 2 | `core.intermediate_model.*` |
| `core.backend.solver.*` | 1 | `core.solver.*` |

**关键确认**：当前仓库中插件包路径仍为 `core.backend.plugins.*`（Gurobi/SCIP/Heuristic），与 example 引用一致，**不需要迁移**。

### 5.4 Example 对 `framework` 的引用

| framework 包 | 说明 |
|-------------|------|
| `framework.model.*` | Pipeline, ShadowPrice |
| `framework.solver.*` | 框架求解器 |
| `framework.gantt_scheduling.*` | 甘特调度领域 |

---

## 6. 三方映射表

### 6.1 Core 当前实现 -> Rust 目标 -> 旧 Kotlin 入口

| 当前包.类型 | Rust 目标 | 旧 frontend/backend 入口 |
|------------|-----------|-------------------------|
| `variable.AbstractVariableItem` | `variable::AbstractVariableItem` | `frontend.variable.AbstractVariableItem` |
| `variable.Token` | `token::Token` | `frontend.variable.Token`（旧） |
| `variable.TokenList` | `token::TokenList` | `frontend.variable.TokenList`（旧） |
| `intermediate_model.TokenTable` | `token::TokenTable` | `backend.intermediate_model.TokenTable` |
| `intermediate_symbol.IntermediateSymbol` | `symbol::IntermediateSymbol` | `frontend.expression.symbol.IntermediateSymbol` |
| `intermediate_symbol.function.*` | `symbol::function::*` | `frontend.expression.symbol.linear_function.*` / `quadratic_function.*` |
| `intermediate_model.BasicModel` | `model::basic::BasicModel` | `frontend.model.BasicModel` |
| `intermediate_model.MechanismModel` | `model::mechanism::MechanismModel` | `frontend.model.mechanism.MechanismModel` |
| `intermediate_model.MetaModel` | `model::mechanism::MetaModel` | `frontend.model.mechanism.MetaModel` |
| `intermediate_model.Constraint` | `model::mechanism::Constraint` | `frontend.inequality.Constraint` |
| `intermediate_model.LinearTriadModel` | `model::intermediate::LinearTriadModel` | `backend.intermediate_model.LinearTriadModel` |
| `intermediate_model.QuadraticTetradModel` | `model::intermediate::QuadraticTetradModel` | `backend.intermediate_model.QuadraticTetradModel` |
| `model.callback.CallBackModel` | `model::callback::CallBackModel` | `frontend.model.callback.CallBackModel` |
| `solver.*` | `solver::*` | `backend.solver.*` |
| `backend.plugins.gurobi.*` | `plugin::gurobi::*` | `backend.plugins.gurobi.*`（路径不变） |
| `backend.plugins.scip.*` | `plugin::scip::*` | `backend.plugins.scip.*`（路径不变） |
| `backend.plugins.heuristic.*` | `plugin::heuristic::*` | `backend.plugins.heuristic.*`（路径不变） |

---

## 7. P3-1~P3-5 改动对象清单

### P3-1（example import 迁移）
- 对象：example 全部 191 文件（含 frontend 引用）+ 50 文件（含 backend 引用）的 import 语句
- 不涉及 core/framework 代码修改
- 插件包路径无需迁移

### P3-2（泛型化补齐）
- 对象：`TokenTable.kt`, `TokenCacheContext.kt`, `Model.kt`（接口）
- TokenCacheContexts 是 token/symbol 计算缓存，纳入 P3-2 泛型化范围
- 影响面：framework 10 文件 + gantt-scheduling

### P3-3（variable/token 拆分）
- 对象：`Token.kt`, `TokenList.kt`（从 variable 迁到 token）, `TokenTable.kt`, `TokenCacheContext.kt`, `TokenCacheKey.kt`（从 intermediate_model 迁到 token）
- 影响面：core 内部所有引用方 + framework + gantt-scheduling

### P3-4（模型层重排）
- 对象：intermediate_model 下 20+ 文件 + model 下 5 文件
- 影响面：core 内部所有引用方 + framework + gantt-scheduling + example

### P3-5（example 迁入）
- 对象：example 仓库复制 + pom 调整 + 根 pom 聚合
- 影响面：仓库根 pom
