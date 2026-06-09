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

POIT 特有的缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、业务 DTO 协议和公式语言暂不在 framework 中建模；只有当它们先成为通用领域实体后，才进入本包。

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

## 输出

`Csp1dSolution<V>` 包含：

- `produce`：选中方案、物料使用、设备产能使用和未满足需求
- `generatedPlans`：最终进入 MILP 的方案池
- `kpi`：选中方案数、车次数、满足/未满足需求数、使用条目数、生成方案数、Top-K 数量、增强结果计数和可序列化 KPI 明细
- `render`：面向 UI 或序列化的稳定 DTO 边界
- `status`：`Feasible`、`Partial`、`NoInitialPlans` 或 `Failed`
- `failureMessage`：可用时回填失败原因
- `topPlans`：可选 Top-K 方案列表

`Csp1dColumnGenerationTrace` 记录终止原因、每轮 LP 目标值、新增定价方案数、初始生成统计、最终 MILP 状态、是否存在部分结果和失败信息。

稳定 KPI 字段名通过 `Csp1dKpiKeys` 暴露。稳定标量 key 包括方案数量、需求数量、使用数量、`solutionStatus`、`finalMilpStatus`、`partialSolutionAvailable`、列生成迭代/定价 key 和初始生成统计 key。动态明细 key 应通过 `materialUsageBatchCount(...)`、`machineCapacityUsed(...)`、`underProduction(...)`、`overProduction(...)`、`materialCost(...)`、`assignedLength(...)`、`overLength(...)` 等 helper 构造。

## 生成语义

generation context 提供 DFS、N-Same、N-Sum、FullSum 和 reduced-cost pricing 生成器。生成器共享 timeout、最大方案数、canonical 去重、基础可行性过滤、统计报告、宽度上界剪枝统计和 `GenerationConstraints.parallelism` 按物料协程并行开关。

DFS、N-Sum 和 FullSum 会按产品、宽度、宽度单位和需求单位去重需求宽度入口，并复用 suffix 最小宽度索引跳过剩余宽度无法容纳后续产品宽度的搜索分支。剪枝节点数记录为 `widthBoundPrunedNodes`，并通过 `Csp1dKpiKeys.InitialGenerationWidthBoundPrunedNodes` / `Csp1dKpiKeys.InitialWidthBoundPrunedNodes` 暴露。

`GenerationConstraints.enableDominancePruning` 用于开启同贡献候选 dominance 剪枝。对于物料、设备、产能消耗和需求贡献向量相同的候选，生成器保留余宽更小的方案，并把过滤数量记录到 `dominatedCandidates`。

中等规模 baseline 测试已覆盖 DFS、N-Sum、N-Same 和 FullSum，统一记录访问节点数、候选数量、接受方案数、不可行候选数、重复候选数、dominance 剪枝数、宽度上界剪枝节点数、耗时和停止原因。`CuttingPlanGenerationBenchmarkSnapshot` 只保留确定性的数量字段，并通过 `toStableLine()` 输出可比较的 benchmark 快照；耗时仍留在原始统计中作为趋势观察。

`Costar` 是余宽 filler。它可以出现在切片和 render 输出中，但不会产生 demand contribution。

动态长度产品在生成阶段构造需求贡献时，固定长度产品优先使用产品长度，动态长度产品在物料长度可用时使用物料长度兜底。最终卷长分配仍由 length assignment MILP 上下文负责。

## 恢复

`Csp1dRecovery` 保留简单的 `solve(problem, solveConfig)` API，同时提供 `solveWithTrace(Csp1dRecoveryInput<V>)`。trace 会记录恢复状态、warm start 状态、尝试次数、warm start 方案数、已应用方案数、已应用使用量条目数和说明。空 warm start 标记为 `Ignored`；兼容 warm start 交给配置的 `Csp1dWarmStartAdapter` 处理；不兼容 warm start 标记为 `Invalid`。

默认 adapter 为 `AdapterUnsupported`，在 `retryWithoutWarmStart` 启用时退回普通求解。显式传入 `Csp1dWarmStartPlanPoolAdapter` 后，可以把兼容的 warm start 切割方案作为初始方案池应用，并可选择是否追加普通初始方案生成器。提供兼容的 `previousSolution` 时，上一轮选中方案使用量会按 canonical plan key 匹配，并写入 MILP 模型作为原生 assignment 初始值。禁用 `retryWithoutWarmStart` 时抛出带 trace 的 `Csp1dRecoveryFallbackDisabledException`；solver 失败时抛出带 trace 的 `Csp1dRecoverySolveException`。

## Demo

framework 示例位于：

`ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

该示例使用 `Csp1dProblem<Flt64>`、framework 生成器、`ReducedCostPricingGenerator` 和 `Csp1dColumnGeneration`，不再维护示例侧手写 RMP/SP 模型。

## 本地验证

CSP1D 窄测试：

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application "-Dtest=ProductDemandModelTest,DFSGeneratorTest,NSumGeneratorTest,NSameGeneratorTest,FullSumGeneratorTest,CostarFillerTest,CuttingPlanCanonicalKeyTest,ReducedCostPricingGeneratorTest,GeneratorParallelismTest,GeneratorMediumScaleBaselineTest,Csp1dApplicationAcceptanceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Gurobi profile 编译门禁：

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application -Pgurobi-cg-test -DskipTests test-compile
```

demo3 编译门禁：

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-csp1d\csp1d-infrastructure,.\ospf-kotlin-framework-csp1d\csp1d-domain-material-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-cutting-plan-generation-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-length-assignment-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-yield-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-wasting-minimization-context,.\ospf-kotlin-framework-csp1d\csp1d-domain-produce-context,.\ospf-kotlin-framework-csp1d\csp1d-application,.\ospf-kotlin-starters\ospf-kotlin-starter-csp1d,.\ospf-kotlin-example "-Dexample.source.directory=src/main/fuookami/ospf/kotlin/example/framework_demo/demo3" -DskipTests compile
```
