# ospf-kotlin-core

[English README (README.md)](./README.md)

`ospf-kotlin-core` 是 OSPF Kotlin 的核心建模与求解模块，提供：

1. LP/QP 的 Meta/Mechanism/Intermediate 分层建模链路。
2. 基于 `SolveOptions` 的统一求解入口。
3. 统一的建模阶段状态回调模型。
4. 带公共统计字段的统一求解输出模型。

## 统一求解入口

可通过 `backend/solver/SolverExt.kt` 中的扩展函数使用统一入口：

```kotlin
import fuookami.ospf.kotlin.core.backend.solver.SolveOptions
import fuookami.ospf.kotlin.core.backend.solver.solveWithOptions

suspend fun unifiedSolve(
    solver: AbstractLinearSolver,
    model: LinearMetaModel
) {
    val result = solver.solveWithOptions(
        model = model,
        options = SolveOptions.build {
            solutionAmount = UInt64.one
            modelBuildingStatusCallBack = { status ->
                println("${status.modelName} ${status.stage} ${status.ready}/${status.total}")
                ok
            }
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

以下 `Flt64` 便捷 typealias 已删除，请使用 V 类型化等价形式：

| 已删除别名 | 替代写法 |
|---|---|
| `CallBackModelInterface` | `CallBackModelInterfaceV<Flt64>`（或你的 V 类型） |
| `MultiObjectiveModelInterface` | `MultiObjectiveModelInterfaceV<Flt64>`（或你的 V 类型） |

### 已 Internal 化的方法

以下方法此前为 public，现已改为 `internal`（模块私有）：

- `LinearInequality<Flt64>.sign`、`QuadraticInequality.sign`
- `LinearInequality<Flt64>.flattenData`、`QuadraticInequality.flattenData`
- `LinearPolynomial<Flt64>.toFlattenData()`、`QuadraticPolynomial<Flt64>.toFlattenData()`
- `LinearRelation.toConstraint()`、`QuadraticRelation.toConstraint()`、`LinearRelation.toQuadraticConstraint()`
- `LinearPolynomial<Flt64>.toFrontendPolynomial()`（已删除——原为恒等函数）

### SolverBoundaryCasts

所有 `@Suppress("UNCHECKED_CAST")` 注解已集中到 `SolverBoundaryCasts.kt`。如之前使用类型擦除桥接方法（如 `registerAuxiliaryTokensAny`、`registerConstraintsAny`），请改为直接使用 V 类型化方法。框架内部通过 `SolverBoundaryCasts` 处理星投影泛型调用。

### 遗留 Typealias（仍可用）

`token/TokenList.kt` 中的以下 `Flt64` typealias 仍作为遗留便捷别名保留：

- `AbstractTokenList` → `AbstractTokenList<Flt64>`
- `TokenList` → `TokenList<Flt64>`
- `AddableTokenCollection` → `AddableTokenCollection<Flt64>`
- `AbstractMutableTokenList` → `AbstractMutableTokenList<Flt64>`
- `MutableTokenList` → `MutableTokenList<Flt64>`
- `AutoTokenList` → `AutoTokenList<Flt64>`
- `ManualTokenList` → `ManualTokenList<Flt64>`

新代码应直接使用泛型形式。

### adapter/flt64 兼容层

`math.symbol.adapter.flt64` 包提供 Flt64 专用便捷函数和 `QuadraticInequality` typealias。该层是**兼容边界**，不属于主链 V 类型化 API。

**退场策略：**

- `QuadraticInequality` 已标注 `@Deprecated(WARNING)`，请直接使用 `QuadraticInequalityOf<Flt64>`。
- 其余 adapter 函数（evaluate、parse、serde、DSL 等）保留向后兼容，新代码应优先使用泛型 V 类型化等价形式。
- 该 adapter 层将在未来主版本中移除，暂无明确时间线，deprecation 标注表明此意图。

### 扫描门禁（I5）

泛型化扫描门禁（`scripts/scan-full-genericization.ps1`）执行以下检查：

1. `public_api_blocking = 0`（非 adapter 公开 API 签名中无 Flt64）。
2. `UNCHECKED_CAST` blocking = 0（所有 cast 集中在 `SolverBoundaryCasts.kt`）。
3. 非 adapter 公开 API 签名 Flt64 = 0（I5 签名级检查，捕获嵌套泛型和多行签名等纯正则扫描盲区）。
4. 边界项分为三级：**permanent**（solver 固有）、**deprecated**（计划移除）、**must_decrease**（跟踪下降）。

## 与根 README 的分工

仓库根目录 README 负责仓库级导航。
本模块 README 聚焦：

1. core 架构与职责。
2. 统一入口的迁移方式。
3. core 级状态回调与输出契约。

