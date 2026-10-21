# operator — 算法算子族

:us: [English](README.md) | :cn: 简体中文

## 简介

元启发式算法共享少数几类可复用的决策。本包把它们命名为**正交的算子族**，使一个算法成为算子的**组合**，而不是手写的循环。

每个族都是一个接口加多个可互换实现。操作本身不携带状态的接口把类型参数声明在方法上（因此实现可以是 `data object` 单例）；有状态的族（`Archive`）把类型参数声明在类型上。

## 算子族

| 族 | 接口 | 实现 | 职责 |
|----|------|------|------|
| 初始化 | `initializer/Initializer` | `ModelInitializer`、`RandomUniformInitializer`、`OppositionBasedInitializer` | 产生初始解 |
| 适应度 | `fitness/FitnessEvaluator` | `ObjectiveFitnessEvaluator`、`DefaultOnMissFitnessEvaluator`、`ConstraintAwareFitnessEvaluator` | 为解打分，可选择拒绝不可行解 |
| 排序 | `ranking/Ranking` | `ObjectiveRanking`、`ReversedObjectiveRanking`、`IdentityRanking` | 按优度排列个体 |
| 边界 | `boundary/BoundaryHandler` | `ClampBoundary`、`ReflectBoundary`、`WrapBoundary` | 把越界值拉回可行域 |
| 终止 | `stopping/StoppingCriterion` | `IterationLimit`、`NotBetterLimit`、`TimeLimit`、`NeverStop`（支持 `or` / `and` 组合） | 判断何时停止 |
| 拓扑 | `topology/Topology` | `GlobalTopology`、`RingTopology`、`RingKTopology`、`VonNeumannTopology`、`RandomTopology` | 定义邻居关系 |
| 档案 | `archive/Archive` | `BestArchive`、`HistoryBestArchive` | 维护当前最优集合 |
| 多样性 | `diversity/DiversityMetric` | `MeanPairwiseDistance`、`FractalDimensionDiversity` | 监测种群塌缩 |
| 环境选择 | `survivor/SurvivorSelection` | `ElitistSurvivor`、`GenerationalSurvivor`、`TruncationSurvivor`、`ElitistPreservingSurvivor` | 合并父代与子代 |
| 邻域 | `neighborhood/Neighborhood` | `RandomPerturbation`、`GaussianPerturbation`、`LevyPerturbation` | 为单解生成邻域候选 |
| 随机过程 | `stochastic/` | `LevyFlight`、`NoiseStep` | 多维随机步长 |
| 参数演化 | `evolution/ParameterSource` | `ConstantSource`、`LinearSource`、`GeometricSource`、`ExponentialSource`、`ChaoticSource`、`FractalSource`、`DistributionSource`、`JitteredSource` | 让算法参数随迭代变化 |
| 解更新 | `update/SolutionUpdate` | `Cross` + 10 个交叉（`SinglePointCross`、`ArithmeticCross`、`SimulatedBinaryCross`、`DifferentialCross`、`OrderCross`、`CycleCross` …）以及各算法专属更新（`GreyWolfUpdate`、`PpfUpdate` …） | 由父代组计算出下一个解/位置 |
| 变异 | `mutation/Mutation` | `GaussianMutation`、`CauchyMutation`、`LevyMutation`、`UniformMutation`、`BoundaryMutation`、`PolynomialMutation`、`NonUniformMutation`、`ChaoticMutation`、`SwapMutation`、`InversionMutation`、`InsertionMutation`、`ScrambleMutation`、`NoMutation` | 在更新步之后扰动后代；`NoMutation` 为不需要变异的算法占位 |
| 接受准则 | `acceptance/Acceptance` | `BetterOnlyAcceptance`、`NotWorseAcceptance`、`AlwaysAcceptance`、`MetropolisAcceptance` | 判断候选解是否替换当前解 |
| 多目标 | `multi_objective/` | `ParetoDominance`、`FastNonDominatedSort`、`CrowdingDistance`、`ReferencePoints`、`ParetoArchive` | 真 Pareto 选择 |
| 迁移 | `migration/IslandMigration` | `NoMigration`、`RingMigration`、`BestToWorstMigration` | 岛之间交换个体 |
| 强化学习 | `rl/` | `QTable`、`DiversityStagnationDiscretizer`、`GreedyActionSelector`、`EpsilonGreedyActionSelector`、`SoftmaxActionSelector`、`ImprovementReward`、`SymmetricReward`、`QlearningAgent`、`QlearningSource`、`QlearningUpdate` | 用 Q 学习调参或选择算子 |

`Cross` 继承 `SolutionUpdate`，因此与各交叉一起待在 `update/`——一个槽位一个族。`Mutation` 是它的
兄弟槽位，故自成一族放在 `mutation/`。两者都曾以「共享数学原语」为名待在插件根包，但当骨架开始把
`Mutation` 当作一等槽位消费时，这个理由就不成立了。

## 组合示例

```kotlin
// 混沌序列 → 参数区间 → 抖动 → 惯性权重
val inertia = JitteredSource(
    base = ChaoticSource(generator = LogisticMapGenerator(), lowerBound = Flt64(0.4), upperBound = Flt64(0.9)),
    entropy = entropy,
    amplitude = Flt64(0.02)
)

// 分形序列（Mandelbrot 迭代）→ 幅值投影 → 步长
val step = FractalSource(
    generator = MandelbrotSetGenerator(),
    projection = ChannelProjection.Magnitude,
    lowerBound = Flt64(0.01),
    upperBound = Flt64(0.5)
)

// 种群多样性 → 自适应步长
val diversity = FractalDimensionDiversity(solutions, converter)
val adaptive = if (diversity.toDouble() < 1.2) Flt64(0.5) else Flt64(0.05)
```

## 严格序

`compareStrictly(model, lhs, rhs)` 把模型的目标比较规范化为严格弱序。模型可能注册非严格比较器，此时 `compareObjective(a, a)` 会返回 `Order.Less`；本包所有算子统一使用 `compareStrictly`，从而保证持平仍是持平、排序满足比较器契约、「是否改进」不会因持平而误判。

## 数值辅助

`NumericSupport.kt` 存放每个算子与骨架本来都要重复的 `internal` 样板：`lowerBoundOf` / `upperBoundOf`
（从回调模型读取变量的箱式上下界）、`asDouble` / `asValue`（数值解值的拆包与回写）、`unit()`
（从 `Generator<Flt64>` 采样 `[0, 1)` 均匀数）、`pickTwo`、`clampTo` 以及 `shouldFire`（按给定概率
决定是否触发事件）。

其中最要紧的是 `clampTo`：它在夹紧之外还把 `NaN` / `±∞` 替换为可用边界。非有限值一旦进入种群，
目标值会溢出为 `∞`，而 `∞` 之间的比较不构成全序——环境选择的排序随即抛出
`Comparison method violates its general contract`。

该文件原名 `GeneSupport.kt` 并位于插件根包，直到染色体概念被删除；其中从来没有与基因相关的内容。

## 相关

- [启发式插件 README](../../README_ch.md)
- [分布模块](../../../../../../../../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/distribution/README_ch.md)
- [序列投影](../../../../../../../../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/sequence/ChannelProjection.kt)
