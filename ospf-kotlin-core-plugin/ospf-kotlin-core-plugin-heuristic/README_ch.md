# ospf-kotlin-core-plugin-heuristic

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的元启发式算法插件模块。本模块中的算法都是**组合**出来的，而不是逐个手写：
一条生命周期骨架负责驱动循环，一组无状态算子决定「一步算什么」。新增算法意味着挑选算子，
而不是再抄一份 `while` 循环。

## 架构

三层，各司其职：

```
┌──────────────────────────────────────────────────────────┐
│  solver/                 组装层 / Composition             │
│    ComposedSolvers       算法 = 骨架 + 算子               │
│    MultiObjectiveSolvers NSGA-II / NSGA-III              │
│    MemeticSolvers        在 GA 上挂局部搜索               │
├──────────────────────────────────────────────────────────┤
│  runner/                 骨架层 / Skeletons               │
│    AbstractRunner        迭代 · 档案 · 回调               │
│    PopulationRunner      交叉 → 变异 → 环境选择           │
│    TrajectoryRunner      邻域 → 接受                      │
│    ParticleSwarmRunner   速度 + 个体最优状态              │
│    IslandRunner          N 个子种群 + 迁移                │
│    Nsga2Runner           真多目标排序                     │
│    Nsga3Runner           参考点式多目标                    │
│    ShadeRunner           成功历史记忆                     │
│    ClpsoRunner           分维度学习                       │
│    CmaEsRunner           分布 (m, σ²C) 状态               │
├──────────────────────────────────────────────────────────┤
│  operator/               算子层 / Operators（17 族）      │
│    update · mutation · survivor · topology · initializer  │
│    fitness · ranking · boundary · stopping · archive      │
│    diversity · neighborhood · stochastic · evolution      │
│    acceptance · multi_objective · migration · rl          │
├──────────────────────────────────────────────────────────┤
│  交叉 + 变异（根包）        数学实现 / Math implementations │
└──────────────────────────────────────────────────────────┘
```

骨架持有循环、档案、终止判断与回调，但它**不知道**自己装的是哪个算子。正因如此，
`GreyWolfUpdate`、`ArithmeticCross`、`PpfUpdate` 才能占据 `PopulationRunner` 的同一个槽位，
而骨架里没有任何为它们单开的分支。

### 两级步进模板

骨架只暴露三个钩子：

| 钩子 | 含义 |
|------|------|
| `initialize(model, iteration)` | 构造初始状态 |
| `advance(model, iteration, state)` | 产生下一个状态 |
| `membersOf(state)` | 把状态摊平成个体列表 |

`PopulationRunner` 在其上增加第二级：`openIsland` / `stepIsland`。`IslandRunner` 正是通过这两个
公开包装器驱动 N 个 `PopulationRunner`，因此岛模型复用同一条种群循环，而不是复制一份。

## 算法

以下算法全部通过 `ComposedSolvers` 提供，且都是纯组合——没有任何算法专属的循环代码。

| 求解器名 | 工厂方法 | 类别 |
|----------|----------|------|
| `genetic` | `genetic` | GA |
| `island-ga` | `islandGenetic` | GA（岛模型） |
| `pso` | `particleSwarm` | PSO |
| `apso` | `adaptiveParticleSwarm` | PSO 变体 |
| `clpso` | `comprehensiveLearningParticleSwarm` | PSO 变体 |
| `gwo` | `greyWolf` | 群智能 |
| `sca` | `sineCosine` | 群智能 |
| `mvo` | `multiVerse` | 群智能 |
| `hs` | `harmonySearch` | 群智能 |
| `wca` | `waterCycle` | 群智能 |
| `soa` | `seagull` | 群智能 |
| `ns` | `neighborhoodSearch` | 群智能 |
| `warso` | `warStrategy` | 群智能 |
| `evo` | `energyValley` | 群智能 |
| `gco` | `germinalCenter` | 群智能 |
| `de` | `differentialEvolution` | 演化 |
| `woa` | `whaleOptimization` | 群智能 |
| `hho` | `harrisHawk` | 群智能 |
| `aoa` | `arithmeticOptimization` | 群智能 |
| `tlbo` | `teachingLearning` | 群智能 |
| `ssa` | `salpSwarm` | 群智能 |
| `gsa` | `gravitationalSearch` | 群智能 |
| `fa` | `firefly` | 群智能 |
| `cs` | `cuckooSearch` | 群智能 |
| `ba` | `batAlgorithm` | 群智能 |
| `mfo` | `mothFlame` | 群智能 |
| `goa` | `grasshopper` | 群智能 |
| `shade` | `shade` | 先进 |
| `l-shade` | `lShade` | 先进 |
| `cma-es` | `cmaEs` | 先进 |
| `q-learning` | `qLearning` | 自适应（算子选择） |
| `simulated-annealing` | `simulatedAnnealing` | 轨迹 |
| `hill-climbing` | `hillClimbing` | 轨迹 |
| `random-walk` | `randomWalk` | 轨迹 |

真多目标求解器位于 `MultiObjectiveSolvers`（`Nsga2Runner`、`Nsga3Runner`），它们基于 Pareto 前沿
而非单一目标值；记忆型（memetic）变体位于 `MemeticSolvers`。

## 文件结构

```
heuristic/                          （插件根包）
├── 契约
│   ├── HeuristicSolver.kt              求解器契约 + RunnerSolver 适配器
│   ├── HeuristicResult.kt              HeuristicResult、HeuristicSolutionStatus
│   ├── Iteration.kt                    迭代计数器
│   └── Population.kt                   Individual、SolutionWithFitness
├── 算法门面（根包）                     27 个顶层函数 + 4 个策略/规格 data class
├── runner/                            生命周期骨架
├── operator/                          18 个算子族（见 operator/README.md）
│   ├── Ordering.kt                    compareStrictly：严格弱序适配
│   ├── NumericSupport.kt              边界 / 拆包 / 夹紧 / 熵 / 概率辅助
│   ├── update/                        SolutionUpdate · Cross · 18 个算法更新
│   │                                  · SinglePoint · TwoPoint · MultiPoint · Uniform · Arithmetic
│   │                                  · Blend · SBX · Differential · Order · Cycle
│   ├── mutation/                      Mutation · Gaussian · Cauchy · Levy · Uniform · Boundary
│   │                                  · Polynomial · NonUniform · Chaotic · Swap · Inversion
│   │                                  · Insertion · Scramble · NoMutation
│   ├── migration/                     IslandMigration：No / Ring / BestToWorst
│   └── rl/                            QTable · StateDiscretizer · ActionSelector · Reward
└── solver/                            ComposedSolvers · MultiObjectiveSolvers · MemeticSolvers
```

只有门面直接位于根包：门面没有状态、没有自己的策略，因此不需要命名空间。凡是能填进槽位的零件——
交叉、变异、环境选择、初始化——都在 `operator/` 下，这让「插件来自 `operator/`」这条规则成立：

```kotlin
import fuookami.ospf.kotlin.core.solver.heuristic.*                    // 契约 + 27 个求解器
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.*    // 交叉
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.*  // 变异
```

`Cross` 继承 `SolutionUpdate`，因此 update 族连交叉一起收在自己的包里——一个槽位一个包。`Mutation`
是它的兄弟槽位，所以是第 18 族，`operator/mutation/` 正是为此存在。

### 门面转发全部参数

门面携带其委派工厂的**全部**参数，因此 `differentialEvolution(...)` 与
`ComposedSolvers.differentialEvolution(...)` 除了前缀之外可以互换。当可选参数达到五个时，这些旋钮
集中成一个策略 data class，而不是摊在签名上：

| 门面 | 可选参数数 | 形态 |
|------|-----------|------|
| 18 个种群类门面 | 4 | `update`、`survivor`、`archiveAmount`、`initializer` 平铺 |
| `hillClimbing`、`neighborhoodSearch` | 2 | `neighborhood`、`initializer` 平铺 |
| `cmaEs` | 2 | `initialSigma`、`archiveAmount` 平铺 |
| `shade` | 3 | `memorySize`、`pBestRate`、`archiveAmount` 平铺 |
| `lShade` | 5 | `policy: LShadePolicy` |
| `adaptiveParticleSwarm` | 6 | `policy: AdaptiveParticleSwarmPolicy` |
| `comprehensiveLearningParticleSwarm` | 7 | `policy: ComprehensiveLearningParticleSwarmPolicy` |
| `particleSwarm` | 6 | `inertia`、`personalWeight`、`socialWeight`、`maxVelocity`、`archiveAmount`、`initializer` 平铺 |
| `islandGenetic` | 7 | `mutation`、`migrationPeriod`、`migration`、`update`、`survivor`、`archiveAmount`、`initializer` 平铺 |

**尾部是数值超参时用策略**——SHADE 的记忆容量与 p 最优比例、APSO/CLPSO 的权重调度：调用方会成组地调
这些数，而它们的含义只存在于某一个算法内部。**尾部是算子槽位时平铺**——`particleSwarm` 的三个权重、
`islandGenetic` 的迁移对加上标准种群槽位：这些是用来替换的零件，与工厂一一对应才是目的。五个参数的
阈值是经验指引，不是不变量。

两种情形下策略都只打包算法旋钮：`stopping`、`converter`、`entropy` 保持为显式参数，因为
`StoppingCriterion` 可组合（`and` / `or`），把它塞进策略会丢掉这个能力。

`FacadeContractTest` 在编译期钉住该契约：每个门面都以「写全所有参数」的方式被调用，少一个就编译不过。

现在已经没有任何算法包。`gwo/`、`sca/`、`mvo/`、`saa/` 带着私有循环，最先被删除；`ga/` 与 `pso/`
则保留了骨架出现之前的策略形状（`GAPolicy`、`PSOPolicy` 里塞着 `iterationLimit` / `timeLimit`），
在骨架的 `StoppingCriterion` 与算子覆盖其能力后最后折叠。两者的入口现已是 `islandGenetic` 与
`particleSwarm` 门面。

岛模型需要一个新类型：`IslandSpec(size, mutationRate)`。它取代旧的 `PopulationBuilder` ——后者还带着
`eliteAmount` 与 `parentAmountRange`，但骨架从不读取，因此直接删掉而不是搬过来。

现在已没有任何算法持有私有循环：`gwo/`、`sca/`、`mvo/`、`saa/` 曾经持有，在组合工厂覆盖其能力后已删除。

## 通用特性

- **泛型值类型** — 适用于任何 `RealNumber<V>` + `NumberField<V>`
- **单目标与多目标** — 单目标求解器共享一个档案；多目标使用基于 Pareto 的算子
- **迭代与时间限制** — 可组合的终止判据（`and` / `or`）
- **运行回调** — 无需接管循环即可观察进度
- **自适应参数** — `operator/evolution` 把 `ParameterSource` 与 `ospf-kotlin-math` 的
  `chaotic` / `fractal` / `distribution` 生成器组合起来
- **内存压力处理** — 迭代间自动清理

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-heuristic:1.1.0")
```

### 组合求解器

```kotlin
val solver = ComposedSolvers.greyWolf<Flt64, Flt64>(
    size = 30,
    stopping = IterationLimit(UInt64(500)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() }
)
val result = solver(model)   // Ret<HeuristicResult<...>>
```

### 岛模型遗传算法

每个 `IslandSpec` 就是一个岛，因此各岛可以有不同规模与变异率。各岛独立演化，每 `migrationPeriod`
代交换个体。

```kotlin
val solver = islandGenetic<Flt64, Flt64>(
    islands = listOf(
        IslandSpec(size = 20, mutationRate = Flt64(0.05)),
        IslandSpec(size = 30, mutationRate = Flt64(0.15))
    ),
    stopping = IterationLimit(UInt64(300)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    migrationPeriod = UInt64(5),
    migration = RingMigration(rate = 0.1)   // 或 BestToWorstMigration(1)、NoMigration()
)
val result = solver(model)
```

`ComposedSolvers.islandGenetic` 另有一个统一规格的重载（`islandCount` / `islandSize` / `mutationRate`）
覆盖常见场景；门面使用的是异构岛屿重载。

### 粒子群优化

```kotlin
val solver = particleSwarm<Flt64, Flt64>(
    size = 100,
    stopping = IterationLimit(UInt64(500)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    inertia = ConstantSource(Flt64(0.4)),
    personalWeight = ConstantSource(Flt64(2.0)),
    socialWeight = ConstantSource(Flt64(2.0))
)
val result = solver(model)
```

三个权重接受任意 `ParameterSource`，因此「随迭代变化的权重」只是同一份代码的另一种调度；需要现成的
写法可以直接用 `adaptiveParticleSwarm` 门面。

### 模拟退火

```kotlin
val saa = ComposedSolvers.simulatedAnnealing<Flt64, Flt64>(
    stopping = IterationLimit(UInt64(500)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    temperature = GeometricSource(Flt64(100.0), Flt64(0.95))
)
val result = saa(model)
```

### Q 学习

Q 学习调整的是「搜索本身」。它以算子族（`operator/rl/`）的形式提供，有两种用法：
**调参**与**算子选择**。

```kotlin
// 算子选择：在一个候选更新算子池上建一张 Q 表。
val solver = ComposedSolvers.qLearning<Flt64, Flt64>(
    size = 30,
    stopping = IterationLimit(UInt64(400)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    candidates = listOf(GreyWolfUpdate(), SineCosineUpdate(), ArithmeticCross),
    selector = EpsilonGreedyActionSelector(epsilon = Flt64(0.1)),
    reward = ImprovementReward(positive = Flt64.one, negative = Flt64(-0.5))
)

// 调参：在一个候选调度池上建一张 Q 表。
val inertia = QlearningSource(
    candidates = listOf(ConstantSource(Flt64(0.4)), LinearSource(Flt64(0.9), Flt64(0.4))),
    randomGenerator = Generator { Random.nextFlt64() }
)
```

奖励来自档案的真实改进（`Archive.update` 的返回值），由骨架在每轮结束后投递给观测者
—— 见 `runner/IterationObserver.kt`。状态是 *(多样性, 停滞度)* 两轴，因此建表不需要目标值。

### 自定义骨架

若要新增一个循环形态确实不同的算法，请继承 `AbstractRunner` 并实现三个钩子——不要复制既有循环：

```kotlin
class MyRunner(...) : AbstractRunner<MyState, ObjValue, V>(entropy, archive, stopping, converter) {
    override suspend fun initialize(model: ..., iteration: Iteration): MyState = ...
    override suspend fun advance(model: ..., iteration: Iteration, state: MyState): MyState = ...
    override fun membersOf(state: MyState): List<Individual<ObjValue, V>> = ...
}
```

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 模型接口、`Individual`、`Ret` |
| `ospf-kotlin-math` | compile | `generator`、`chaotic`、`fractal`、`distribution` |
