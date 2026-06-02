# Phase G0 — Flt64 基线扫描报告

日期：2026-06-02

## 1. 总览

扫描范围：`ospf-kotlin-framework-gantt-scheduling` 全部 Kotlin 源文件（133 个 .kt 文件）。

| 指标 | 数值 |
|------|------|
| Kotlin 文件总数 | 133 |
| Flt64 残留总行数（不含 daily.md） | **1179** |
| `AbstractLinearMetaModel<Flt64>` / `MetaModel<Flt64>` / `LinearIntermediate*<Flt64>` 残留行数 | 约 310 |

## 2. 各子模块 Flt64 残留统计

| 子模块 | Flt64 行数 | 占比 |
|--------|-----------|------|
| gantt-scheduling-domain-task-compilation-context | 418 | 35.5% |
| gantt-scheduling-domain-produce-context | 167 | 14.2% |
| gantt-scheduling-domain-resource-context | 162 | 13.7% |
| gantt-scheduling-domain-bunch-compilation-context | 161 | 13.7% |
| gantt-scheduling-application | 121 | 10.3% |
| gantt-scheduling-domain-capacity-scheduling-context | 80 | 6.8% |
| gantt-scheduling-infrastructure | 50 | 4.2% |
| gantt-scheduling-domain-task-context | 14 | 1.2% |
| gantt-scheduling-domain-bunch-generation-context | 6 | 0.5% |
| gantt-scheduling-domain-task-generation-context | 0 | 0.0% |
| **合计** | **1179** | **100%** |

## 3. Flt64 残留 TOP 10 文件

| 文件 | Flt64 行数 | 所属子模块 |
|------|-----------|-----------|
| TaskTime.kt（task-compilation） | 110 | task-compilation-context |
| StorageResource.kt | 52 | resource-context |
| Compilation.kt（task-compilation） | 35 | task-compilation-context |
| BranchAndPriceAlgorithm.kt（bunch） | 38 | application |
| BranchAndPriceAlgorithm.kt（task） | 35 | application |
| Resource.kt | 33 | resource-context |
| Aggregation.kt（bunch-compilation） | 33 | bunch-compilation-context |
| Switch.kt | 29 | task-compilation-context |
| WorkingCalendar.kt | 29 | infrastructure |
| IterativeAggregation.kt | 26 | task-compilation-context |

## 4. Solver 模型泄漏 TOP 10 文件

以下为 `AbstractLinearMetaModel<Flt64>` / `LinearMetaModel<Flt64>` / `MetaModel<Flt64>` / `LinearIntermediate*<Flt64>` 出现最多的文件，这些是 solver 类型向领域层泄漏的重点：

| 文件 | 泄漏行数 | 类型 |
|------|---------|------|
| TaskTime.kt（task-compilation） | 52 | LinearIntermediateSymbols + register(model) |
| Compilation.kt（task-compilation） | 20 | LinearIntermediateSymbol + register(model) |
| BunchCompilationContext.kt | 18 | register(model: AbstractLinearMetaModel) |
| Switch.kt | 16 | LinearIntermediateSymbols + register(model) |
| IterativeContext.kt | 16 | LinearIntermediateSymbols |
| StorageResource.kt | 15 | register(model) + extractSolution(model) |
| BranchAndPriceAlgorithm.kt（task） | 15 | model 参数 + LinearMetaModel 创建 |
| BranchAndPriceAlgorithm.kt（bunch） | 15 | model 参数 + LinearMetaModel 创建 |
| Aggregation.kt（bunch-compilation） | 13 | register(model: AbstractLinearMetaModel) |
| IterativeAggregation.kt | 12 | LinearIntermediateSymbols + register(model) |

## 5. 允许保留 Flt64 的文件清单

以下文件因处于 solver adapter 边界或算法内部状态管理，允许保留 Flt64（后续可包进 `SchedulingModelBoundary<V>` / `SchedulingSolverValueAdapter<V>`）：

| 文件 | 理由 |
|------|------|
| `application/service/bunch/BranchAndPriceAlgorithm.kt` | solver 模型创建和列生成算法边界，`LinearMetaModel<Flt64>` 的合法使用点 |
| `application/service/task/BranchAndPriceAlgorithm.kt` | 同上（task 版本） |
| `application/model/bunch/Iteration.kt` | 算法迭代状态（LP/IP 目标值、收敛率），可保留但后续建议泛型化为 `Iteration<V>` |
| `application/model/task/Iteration.kt` | 同上（task 版本） |
| `bunch-compilation/service/SlotBasedCapacityPreSolver.kt` | 预求解器直接操作 solver 模型，属于 adapter 边界 |
| `bunch-compilation/service/BunchSolutionAnalyzer.kt` | 从 solver 模型中抽取解，属于 adapter 边界 |
| `bunch-compilation/service/TaskSolutionAnalyzer.kt` | 同上 |
| `task-compilation/service/SolutionAnalyzer.kt` | 同上 |

上述文件中的 Flt64 在后续 Phase 中需要被包装进 `SchedulingSolverValueAdapter<V>`，但短期保留不影响其他子模块的泛型化。

## 6. 改造优先级建议

根据残留密度和依赖关系，建议改造顺序：

1. **infrastructure**（50 处）— 最底层，TimeWindow / WorkingCalendar 泛型化后其他模块可受益
2. **task-context**（14 处）— Cost、ShadowPriceMap、TaskBunch 是多个上下文依赖的基础领域类型
3. **task-compilation-context**（418 处）— 最大残留量，但大量是 constraint/minimization 的重复模式，可批量处理
4. **bunch-compilation-context**（161 处）— Aggregation + Compilation 为核心
5. **capacity-scheduling-context**（80 处）— CapacityCompilation + IterativeCapacityCompilation
6. **resource-context**（162 处）— Resource / StorageResource 为主
7. **produce-context**（167 处）— Produce / Consumption 对称结构
8. **bunch-generation-context**（6 处）— 极少量，可随时处理
9. **application**（121 处）— 最后处理，需要所有领域层泛型化完成后才能统一

## 7. 门禁命令

后续每个 Phase 完成后，运行以下命令对比残留数：

```powershell
# Flt64 总残留
git grep -c "Flt64" -- ospf-kotlin-framework-gantt-scheduling --include="*.kt" | awk -F: '{s+=$NF} END {print "Flt64 total:", s}'

# Solver 模型泄漏
git grep -c "AbstractLinearMetaModel<Flt64>\|LinearMetaModel<Flt64>\|MetaModel<Flt64>\|AbstractMetaModel<Flt64>\|LinearIntermediate.*Flt64" -- ospf-kotlin-framework-gantt-scheduling --include="*.kt" | awk -F: '{s+=$NF} END {print "Solver leak total:", s}'
```

基线值：**Flt64 = 1179 行，Solver 泄漏 ≈ 310 行**。
