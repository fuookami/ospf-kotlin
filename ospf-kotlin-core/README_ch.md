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

## 与根 README 的分工

仓库根目录 README 负责仓库级导航。
本模块 README 聚焦：

1. core 架构与职责。
2. 统一入口的迁移方式。
3. core 级状态回调与输出契约。

