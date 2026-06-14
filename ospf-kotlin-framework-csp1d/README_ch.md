# ospf-kotlin-framework-csp1d

[English](README.md)

`ospf-kotlin-framework-csp1d` 是可复用的一维分切开发包。它只沉淀通用 CSP1D 内核；下游请求 DTO、公式语言、项目运行参数、租户上下文、心跳逻辑和 solver 插件选择留给业务适配层。

## 边界

当前 public 模型只覆盖 `csp1d-domain-material-context` 已表达的实体：

- `Product`、`ProductDemand`、`Production`
- `Costar`
- `Material`、`Machine`
- `CuttingPlanSlice`、`CuttingPlan`
- 需求贡献、render DTO，以及当前已经接入的 yield、waste、length assignment 增强上下文

特定下游业务的缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、业务 DTO 协议和公式语言暂不在 framework 中建模；只有当它们先成为通用领域实体后，才进入本包。

## 基本使用

可以直接构造 `Csp1dProblem<V>`，也可以使用 builder DSL：

```kotlin
val problem = csp1dProblem<Flt64> {
    products(products)
    material(material)
    demands(demands)
    configuration(
        Csp1dConfiguration(
            maxInitialPlans = 64,
            maxPricingPlans = 16,
            iterationLimit = 8
        )
    )
    solveConfig {
        columnGeneration(
            maxInitialPlans = 128,
            maxPricingPlans = 32,
            iterationLimit = 16
        )
        yieldConfig(
            YieldModelingConfig(
                underProductionPenalty = mapOf(
                    ProductDemandShadowPriceKey(productId = "p1", unitSymbol = "m") to Flt64(100.0)
                ),
                overProductionPenalty = mapOf(
                    ProductDemandShadowPriceKey(productId = "p1", unitSymbol = "m") to Flt64(10.0)
                ),
                overProductionUpperBound = mapOf(
                    ProductDemandShadowPriceKey(productId = "p1", unitSymbol = "m") to Flt64(50.0)
                )
            )
        )
        lengthConfig(lengthConfig)
        wasteConfig(wasteConfig)
        topKPlanLimit(10)
        allowPartialSolution(true)
    }
}
```

普通 MILP 使用 `Csp1dMilp`，列生成使用 `Csp1dColumnGeneration`：

```kotlin
val milpSolution = Csp1dMilp<Flt64>(solver).solve(problem)
val result = Csp1dColumnGeneration<Flt64>(solver).solveWithTrace(problem)
```

`solveConfig` 可以作为 `solve(...)` 参数显式传入，也可以挂在 `problem.solveConfig`，还可以从 solver 入口构造参数提供默认增强配置。显式方法参数优先级最高。

`Csp1dColumnGeneration` 也接受 `yieldConfig`、`wasteConfig`、`lengthConfig` 和 `warmStartPlanUsages` 作为构造参数。`warmStartPlanUsages` 接受 `List<CuttingPlanUsage<V>>`，每个元素将一个 `CuttingPlan` 与 `UInt64` 使用量配对；这些配对会作为原生 assignment 初始值写入最终 MILP 模型：

```kotlin
val solver = Csp1dColumnGeneration<Flt64>(
    solver = columnGenerationSolver,
    yieldConfig = yieldConfig,
    warmStartPlanUsages = listOf(
        CuttingPlanUsage(plan = cuttingPlan, amount = UInt64(3))
    )
)
```

## 建模扩展

`Csp1dSolveConfig<V>` 暴露 `extensions` 字段，接受 `List<Csp1dModelingExtension<V>>`。每个扩展承载一个 `Pipeline<LinearMetaModel<Flt64>>` 和一个 `Csp1dExtensionMode` 过滤条件，决定管线在哪些求解阶段生效：

- `MILP` — 仅普通 MILP
- `LP` — 仅列生成 LP master
- `FINAL_MILP` — 仅列生成最终 MILP
- `ALL` — 所有阶段（默认）

下游业务约束（同单位长度、同宽度、宽差、材质兼容等）应以扩展管线注入，而非修改 framework 核心代码。

示例 — 通过 public 求解入口注入同宽度约束：

```kotlin
val sameWidthPipeline: Pipeline<LinearMetaModel<Flt64>> = SameWidthConstraintPipeline(materialId = "m1")
val extension = Csp1dModelingExtension<Flt64>(
    pipeline = sameWidthPipeline,
    mode = Csp1dExtensionMode.ALL
)
val solveConfig = Csp1dSolveConfig<Flt64>(
    extensions = listOf(extension)
)
// 或使用 builder DSL：
val config = csp1dSolveConfig<Flt64> {
    extension(Csp1dModelingExtension(sameWidthPipeline, Csp1dExtensionMode.ALL))
}
val solution = Csp1dMilp<Flt64>(solver).solve(problem, solveConfig)
```

扩展配置会传播到所有求解路径：普通 MILP、列生成 LP master 和最终 MILP、以及 recovery/partial 回退 MILP。默认空 `extensions` 列表保持向后兼容。

`Csp1dColumnGeneration` 在 pricing 循环中保留同一个 LP master，并在每批 pricing 方案被接受后调用 `Csp1dProduceContext.addColumns` 原地加列。`ProduceAggregation.addColumns()` 创建 `x_$iteration` 变量组和 `batch_$iteration` 中间符号组，通过 `flush+asMutable` 刷新约束中间符号使约束自动包含新列系数，并追加目标项。由于内置约束管线引用中间符号而非直接引用 x 变量，addColumns 只需 flush 中间符号无需刷新约束。扩展管线若也需要在新增列时同步刷新，可实现 `Csp1dIncrementalPipeline`，其 `addColumns` 返回值会继续作为后续扩展和列生成方案池的确认结果。

## 扩展策略

`Csp1dSolveConfig<V>` 暴露 `extensionSet` 字段（`Csp1dExtensionSet<V>` 类型），在建模扩展之外聚合六类策略：

- **领域策略**（`Csp1dDomainPolicy<V>`）：宽度可行性覆盖、候选验收。
- **目标策略**（`Csp1dObjectivePolicy<V>`）：修正目标函数中的批次系数。
- **生成策略**（`Csp1dGenerationStrategy<V>`）：候选验收、自定义 canonical key、dominance 验收。
- **定价策略**（`Csp1dPricingPolicy<V>`）：修正 reduced cost、修正 benefit、自定义 `isImproving` 判断。
- **流程策略**（`Csp1dFlowPolicy<V>`）：控制列生成主循环流程——过滤初始方案、判定方案等价、触发提前停止迭代、自定义终止原因/消息、最终 MILP 失败时接受部分解、允许 recovery 回退。
- **提取策略**（`Csp1dExtractionPolicy<V>`）：在不修改解模型的前提下，向 KPI 明细和 render KPI 映射写入自定义条目，扩展解输出。

所有策略都提供默认实现以保持既有行为。空策略列表完全向后兼容。

流程策略接收 `Csp1dFlowContext<V>`，暴露迭代号、当前方案、新增方案、迭代上限、LP 结果状态、pricing 统计和 `allowPartialSolution`，支持上下文感知判断而无需闭包捕获。

提取策略接收 `Produce<V>`、需求、物料、设备、已生成方案、迭代次数、终止原因、最终 MILP 状态和 pricing 统计，可向可变 `details` 和 `renderKpi` 映射写入。提取策略抛出的异常会被捕获并记录，不会逃逸到求解路径中。

## 影子价格生命周期

LP 影子价格提取通过 `Csp1dShadowPriceLifecycle<V>` 管理，使用 CGPipeline 机制提取。

内置非 length 约束管线——`DemandConstraintPipeline`、`MaterialConstraintPipeline`、`MachineConstraintPipeline` 和 `YieldConstraintPipeline`——已直接实现 `CGPipeline<Args, Model, Map>`。模型构建时，每个需要影子价格的约束通过 `constraint.args = Csp1dShadowPriceKey`（如 `ProductDemandShadowPriceKey`、`MaterialUsageShadowPriceKey`、`MachineBatchShadowPriceKey`、`MachineCapacityShadowPriceKey`、`YieldOverProductionBoundShadowPriceKey`）注册影子价格键。LP 求解后，`Csp1dShadowPriceLifecycle` 调用每个 CGPipeline 的 `refresh` 方法，通过 `model.constraintsOfGroup(this)` 查找管线注册的约束，利用 `constraint.args` 提取对偶值——无需约束名查找。

每个 CGPipeline 还实现了 `extractor()`，通过查找 `AbstractCsp1dShadowPriceMap` 中的影子价格计算给定切割方案的 reduced cost 贡献。yield 超产上限约束进入同一 refresh 生命周期，但显式返回零 reduced-cost 贡献，因为该约束限制的是聚合 yield slack，而不是单个切割方案的 benefit。length-assignment 仍是普通 `Pipeline` 例外，不注册 length shadow price key。

lifecycle 同时填充框架兼容的 `AbstractCsp1dShadowPriceMap`（供下游 CGPipeline 消费）和轻量级 `ShadowPriceMap<V>`（供 pricing 消费）。LP 对偶值通过显式 `Flt64 → V` 转换，禁止直接泛型强转。若扩展管线也需要在加列时同步刷新，应实现 `Csp1dIncrementalPipeline`。

## 输出

`Csp1dSolution<V>` 包含：

- `produce`：选中方案、物料使用、设备产能使用和未满足需求
- `generatedPlans`：最终进入 MILP 的方案池
- `kpi`：选中方案数、车次数、满足/未满足需求数、使用条目数、生成方案数、Top-K 数量、增强结果计数和可序列化 KPI 明细
- `render`：面向 UI 或序列化的稳定 DTO 边界
- `status`：`Feasible`、`Partial`、`NoInitialPlans` 或 `Failed`
- `failureMessage`：可用时回填失败原因；LP 失败信息与 MILP 失败信息以分号合并
- `topPlans`：可选 Top-K 方案列表

`Csp1dColumnGenerationTrace` 记录终止原因（`PricingConverged`、`IterationLimitReached`、`LpInfeasible`、`LpSolveFailed`、`AllDuplicates`、`NoInitialPlans`）、每轮 LP 目标值、新增定价方案数、初始和 pricing 生成统计、最终 MILP 状态、是否存在部分结果、失败信息和 LP 失败详细消息。

`LpInfeasible` 表示首次 LP 求解即失败（推测 LP 松弛不可行）；`LpSolveFailed` 表示有前序有效 LP 结果后 LP 求解失败。两者均基于 LP null 返回推断，非 solver 层确定判定。

`Failed` 状态仅在 `allowPartialSolution=false` 且求解失败时返回；`allowPartialSolution=true`（默认）时返回 `Partial`。两者均不抛异常。

稳定 KPI 字段名通过 `Csp1dKpiKeys` 暴露。稳定标量 key 包括方案数量、需求数量、使用数量、`solutionStatus`、`finalMilpStatus`、`partialSolutionAvailable`、`lpFailureMessage`、列生成迭代/定价 key、初始生成统计 key 和 pricing 生成统计 key。动态明细 key 应通过 `materialUsageBatchCount(...)`、`machineCapacityUsed(...)`、`underProduction(...)`、`overProduction(...)`、`materialCost(...)`、`assignedLength(...)`、`overLength(...)` 等 helper 构造。

## 生成语义

generation context 提供 DFS、N-Same、N-Sum、FullSum 和 reduced-cost pricing 生成器。生成器共享 timeout、最大方案数、canonical 去重、基础可行性过滤、统计报告、宽度/长度上界剪枝统计和 `GenerationConstraints.parallelism` 按物料协程并行开关。

DFS、N-Sum 和 FullSum 会按产品、宽度、宽度单位和需求单位去重需求宽度入口，并复用 suffix 最小宽度索引跳过剩余宽度无法容纳后续产品宽度的搜索分支。剪枝节点数记录为 `widthBoundPrunedNodes`，并通过 `Csp1dKpiKeys.InitialGenerationWidthBoundPrunedNodes` / `Csp1dKpiKeys.InitialWidthBoundPrunedNodes` 暴露。

配置 `GenerationConstraints.maxOverProduceLength` 时，DFS、N-Sum、N-Same 和 FullSum 会在组合搜索或单产品枚举前过滤超长产品入口。剪枝入口数记录为 `lengthBoundPrunedEntries`，并通过 `Csp1dKpiKeys.InitialGenerationLengthBoundPrunedEntries` / `Csp1dKpiKeys.InitialLengthBoundPrunedEntries` 暴露。

配置 `GenerationConstraints.minKnifeCount` 时，DFS、N-Sum 和 FullSum 会剪掉即使用后续最窄可用入口填满剩余宽度也无法达到最小刀数的组合分支。剪枝节点数记录为 `knifeBoundPrunedNodes`，并通过 `Csp1dKpiKeys.InitialGenerationKnifeBoundPrunedNodes` / `Csp1dKpiKeys.InitialKnifeBoundPrunedNodes` 暴露。

DFS、N-Sum 和 FullSum 还会对等价幅宽轮廓的物料复用已过滤的产品-宽度入口索引。该复用只作用于搜索入口索引，最终方案仍按物料分别构造，因此物料身份和 canonical 输出保持稳定。缓存命中数记录为 `materialWidthIndexCacheHits`，并通过 `Csp1dKpiKeys.InitialGenerationMaterialWidthIndexCacheHits` / `Csp1dKpiKeys.InitialMaterialWidthIndexCacheHits` 暴露。

在顺序生成路径且只使用内置生成约束时，DFS、N-Sum 和 FullSum 还可以跨等价幅宽轮廓物料复用切片组合模板。该缓存只保存 `CuttingPlanSlice` 组合，不保存 `CuttingPlan` 对象；每次命中都会按当前物料重新构造方案和需求贡献，包括依赖物料长度的动态长度贡献。缓存命中数记录为 `materialSliceTemplateCacheHits`，并通过 `Csp1dKpiKeys.InitialGenerationMaterialSliceTemplateCacheHits` / `Csp1dKpiKeys.InitialMaterialSliceTemplateCacheHits` 暴露。

`GenerationConstraints.enableDominancePruning` 用于开启同贡献候选 dominance 剪枝。对于物料、设备、产能消耗和需求贡献向量相同的候选，生成器保留余宽更小的方案，并把过滤数量记录到 `dominatedCandidates`。`GenerationConstraints.dominanceStrategy` 选择 `SameContribution`（默认）或 `CrossContribution`：后者按产品集合分组，贡献向量为严格超集且余宽不更优的候选被过滤。跨贡献 dominated 数量记录到 `crossContributionDominated`。

数量缓存避免对同一物料内重复产品宽度条目的需求贡献量重复计算。命中和未命中数量分别记录到 `quantityCacheHits` / `quantityCacheMisses`，通过 `Csp1dKpiKeys.InitialGenerationQuantityCacheHits` / `Csp1dKpiKeys.InitialGenerationQuantityCacheMisses` 输出。

切片模板缓存未命中数量记录到 `materialSliceTemplateCacheMisses`，通过 `Csp1dKpiKeys.InitialGenerationMaterialSliceTemplateCacheMisses` 输出。

在 `parallelism > 1` 下，跨 worker 重复候选（并行 worker 独立生成但 canonical 相同的候选）在合并时去重并记录到 `crossWorkerDuplicateCandidates`，通过 `Csp1dKpiKeys.InitialGenerationCrossWorkerDuplicateCandidates` 输出。

中等规模、混合需求单位和扩展规模 baseline 测试已覆盖 DFS、N-Sum、N-Same 和 FullSum，统一记录访问节点数、候选数量、接受方案数、不可行候选数、重复候选数、dominance 剪枝数、宽度上界剪枝节点数、刀数下界剪枝节点数、长度上界剪枝入口数、物料宽度索引缓存命中数、物料切片模板缓存命中数、数量缓存命中数、数量缓存未命中数、切片模板缓存未命中数、跨 worker 重复候选数、跨贡献 dominated 数、耗时和停止原因。`CuttingPlanGenerationBenchmarkSnapshot` 只保留确定性的数量字段，并通过 `toStableLine()` 输出可比较的 benchmark 快照；耗时仍留在原始统计中作为趋势观察。

`Costar` 是余宽 filler。它可以出现在切片和 render 输出中，但不会产生 demand contribution。

动态长度产品在生成阶段构造需求贡献时，固定长度产品优先使用产品长度，动态长度产品在物料长度可用时使用物料长度兜底。最终卷长分配仍由 length assignment MILP 上下文负责。

## 恢复

`Csp1dRecovery` 保留简单的 `solve(problem, solveConfig)` API，同时提供 `solveWithTrace(Csp1dRecoveryInput<V>)`。`Csp1dColumnGenerationRecovery` 为列生成提供相同的恢复输入、trace、fallback 和 adapter 约定。trace 会记录恢复状态、warm start 状态、尝试次数、warm start 方案数、已应用方案数、已应用使用量条目数和说明。空 warm start 标记为 `Ignored`；兼容 warm start 交给配置的 `Csp1dWarmStartAdapter` 处理；不兼容 warm start 标记为 `Invalid`。

默认 adapter 为 `AdapterUnsupported`，在 `retryWithoutWarmStart` 启用时退回普通求解。显式传入 `Csp1dWarmStartPlanPoolAdapter` 后，可以把兼容的 warm start 切割方案作为初始方案池应用，并可选择是否追加普通初始方案生成器。提供 `previousSolution` 时，恢复流程会提取仍与当前问题兼容的方案，上一轮选中方案使用量会按 canonical plan key 匹配，并写入 MILP 模型作为原生 assignment 初始值。普通 `Csp1dMilp`、`Csp1dRecovery`、`Csp1dColumnGeneration` 的最终 MILP 阶段和 `Csp1dColumnGenerationRecovery` 都会在存在兼容使用量时消费这些原生初始值。application 验收已覆盖列生成 recovery 从上一轮解恢复、恢复结果再次作为下一轮 previousSolution，以及最终 MILP 失败时保留 partial solution 语义。Gurobi profile 已包含 direct MILP 原生初始解、列生成最终 MILP 原生初始解、recovery previous-solution warm start、列生成 recovery previous-solution warm start、列生成 recovery 下一轮 previousSolution、问题变化后兼容子集过滤和设备产能 + yield recovery warm start 七条真实 solver smoke 路径。禁用 `retryWithoutWarmStart` 时抛出带 trace 的 `Csp1dRecoveryFallbackDisabledException`；solver 失败时抛出带 trace 的 `Csp1dRecoverySolveException`。

## Demo

framework 示例位于：

`ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

该示例使用 `Csp1dProblem<Flt64>`、framework 生成器、`ReducedCostPricingGenerator` 和 `Csp1dColumnGeneration`，不再维护示例侧手写 RMP/SP 模型。

## 本地验证

CSP1D 窄测试：

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application "-Dtest=ProductDemandModelTest,DFSGeneratorTest,NSumGeneratorTest,NSameGeneratorTest,FullSumGeneratorTest,CostarFillerTest,CuttingPlanCanonicalKeyTest,ReducedCostPricingGeneratorTest,GeneratorParallelismTest,GeneratorMediumScaleBaselineTest,Csp1dApplicationAcceptanceTest,Csp1dCgLifecycleTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Gurobi profile 编译门禁：

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application -Pgurobi-cg-test -DskipTests test-compile
```

Gurobi native warm start smoke：

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application -Pgurobi-cg-test "-Dcsp1d.gurobi.cg.test.enabled=true" "-Dtest=Csp1dColumnGenerationRealSolverTest#milpWarmStartInitialSolutionWorksOnRealSolver+columnGenerationFinalMilpWarmStartWorksOnRealSolver+recoveryPreviousSolutionWarmStartWorksOnRealSolver+columnGenerationRecoveryPreviousSolutionWarmStartWorksOnRealSolver+columnGenerationRecoveryResultCanBeReusedAsPreviousSolutionOnRealSolver+recoveryPreviousSolutionWarmStartFiltersChangedProblemOnRealSolver+recoveryWarmStartWithMachineCapacityAndYieldWorksOnRealSolver" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

demo3 编译门禁：

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-infrastructure,.\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application,.\ospf-kotlin-starters\ospf-kotlin-starter-csp1d,.\ospf-kotlin-example "-Dexample.source.directory=src/main/fuookami/ospf/kotlin/example/framework_demo/demo3" -DskipTests compile
```
