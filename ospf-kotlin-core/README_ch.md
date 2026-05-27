# ospf-kotlin-core

[English README (README.md)](./README.md)

`ospf-kotlin-core` 是 OSPF Kotlin 的核心建模与求解模块，提供：

1. LP/QP 的 Meta/Mechanism/Intermediate 分层建模链路。
2. 基于 `SolveOptions` 的统一求解入口。
3. 统一的建模阶段状态回调模型。
4. 带公共统计字段的统一求解输出模型。

## 模块结构

```
core/
├── error/                  - 错误处理类型 (ErrorCode, ErrorKind)
├── model/
│   ├── basic/              - 基础模型类型 (Constraint, Expression, Model, ModelView)
│   ├── callback/           - 建模状态回调
│   ├── intermediate/       - 中间模型层 (LinearTriadModel, QuadraticTetradModel)
│   └── mechanism/          - 机制模型层 (MetaModel, 约束注册)
├── solver/
│   ├── config/             - 求解器配置 (SolverConfig)
│   ├── heuristic/          - 启发式求解器实现
│   ├── iis/                - 不可约不可行集分析
│   ├── output/             - 求解器输出模型 (FeasibleSolverOutput)
│   └── value/              - 求解器值类型
├── symbol/
│   ├── flatten/            - 符号展平工具
│   └── function/           - 函数符号 (BigM, Binaryization, Piecewise 等)
├── token/                  - 模型构建的 Token 类型 (TokenList, TokenTable)
└── variable/               - 变量类型与集合 (VariableItem, VariableRange)
```

## 统一求解入口

可通过 `core/solver/SolverExt.kt` 中的扩展函数使用统一入口：

```kotlin
import fuookami.ospf.kotlin.core.solver.SolveOptions
import fuookami.ospf.kotlin.core.solver.solveWithOptions

suspend fun unifiedSolve(
    solver: AbstractLinearSolver,
    model: LinearTriadModelView
) {
    val result = solver.solveWithOptions(
        model = model,
        options = SolveOptions.build {
            solutionAmount = UInt64.one
            solvingStatusCallBack = { status ->
                println("obj=${status.obj}, gap=${status.gap}")
                ok
            }
        }
    )
}
```

## 兼容迁移说明

历史重载（如 `invoke(...)`、`solveAsync(...)`）仍可继续使用，建议按以下方式渐进迁移：

1. 旧调用点保持不变，保证外部模块无感升级。
2. 新调用点优先切换到 `solve(...)` / `solveWithOptions(...)`。
3. 分批将旧状态回调切换到 `ModelBuildingStatusCallBack`。

## 输出模型说明

`FeasibleSolverOutput` 在保持原字段的基础上，新增/统一了以下公共字段：

1. `iterations`
2. `nodeCount`
3. `bestBound`
4. `mipGap`
5. `solveTime`

便于 LP/QP/MIP 后端适配层输出统一结构，同时保持向后兼容。

## 泛型化迁移指南

核心模块已从 `Flt64` 专用 API 迁移为泛型 `V : RealNumber<V>, NumberField<V>` 类型化 API。本节记录下游代码的迁移路径。

### 已删除的 Typealias

以下 `Flt64` 便捷 typealias 已删除，请直接使用泛型形式：

| 已删除别名 | 替代写法 |
|---|---|
| `CallBackModelInterface`（Flt64 别名） | `CallBackModelInterface<Flt64>`（或你的 V 类型） |
| `MultiObjectiveModelInterface`（Flt64 别名） | `MultiObjectiveModelInterface<Flt64>`（或你的 V 类型） |

### 已 Internal 化的方法

以下方法此前为 public，现已改为 `internal`（模块私有）：

- `LinearInequality<Flt64>.sign`、`QuadraticInequalityOf<Flt64>.sign`
- `LinearInequality<Flt64>.flattenData`、`QuadraticInequalityOf<Flt64>.flattenData`
- `LinearPolynomial<Flt64>.toFlattenData()`、`QuadraticPolynomial<Flt64>.toFlattenData()`
- `LinearRelation.toConstraint()`、`QuadraticRelation.toConstraint()`、`LinearRelation.toQuadraticConstraint()`
- `LinearPolynomial<Flt64>.toFrontendPolynomial()`（已删除——原为恒等函数）

### SolverBoundaryCasts

所有 `@Suppress("UNCHECKED_CAST")` 注解已集中到 `SolverBoundaryCasts.kt`。如之前使用类型擦除桥接方法（如 `registerAuxiliaryTokensStar`、`registerConstraintsLinearStar` / `registerConstraintsQuadraticStar`），请改为直接使用 V 类型化方法。框架内部通过 `SolverBoundaryCasts` 处理星投影泛型调用。

### Token 类型

`token/TokenList.kt` 中的 Token 类型现已完全泛型化，使用时需指定类型参数：

- `AbstractTokenList<V>`
- `TokenList<V>`
- `AddableTokenCollection<V>`
- `AbstractMutableTokenList<V>`
- `MutableTokenList<V>`
- `AutoTokenList<V>`
- `ManualTokenList<V>`

新代码应直接使用泛型形式（如 `TokenList<Flt64>`）。

### QuadraticInequalityOf

二次不等式类型现已完全泛型化：`QuadraticInequalityOf<V>`。直接使用 V 类型参数即可（如 `QuadraticInequalityOf<Flt64>`），不提供兼容别名。

## 包命名迁移

`core.intermediate_symbol.*` 已重命名为 `core.symbol.*`，请相应更新导入：

| 旧导入 | 新导入 |
|---|---|
| `fuookami.ospf.kotlin.core.intermediate_symbol.*` | `fuookami.ospf.kotlin.core.symbol.*` |
| `fuookami.ospf.kotlin.core.intermediate_symbol.function.*` | `fuookami.ospf.kotlin.core.symbol.function.*` |
| `fuookami.ospf.kotlin.core.intermediate_symbol.flatten.*` | `fuookami.ospf.kotlin.core.symbol.flatten.*` |

## 与根 README 的分工

仓库根目录 README 负责仓库级导航。
本模块 README 聚焦：

1. core 架构与职责。
2. 统一入口的迁移方式。
3. core 级状态回调与输出契约。

