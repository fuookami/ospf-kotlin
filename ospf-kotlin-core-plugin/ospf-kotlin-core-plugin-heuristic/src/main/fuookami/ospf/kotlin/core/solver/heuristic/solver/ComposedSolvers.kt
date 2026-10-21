/**
 * 组合式求解器工厂 / Composed solver factories
*/
package fuookami.ospf.kotlin.core.solver.heuristic.solver

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SinglePointCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.ArithmeticCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.GaussianMutation
import fuookami.ospf.kotlin.core.solver.heuristic.runner.PopulationRunner
import fuookami.ospf.kotlin.core.solver.heuristic.runner.TrajectoryRunner
import fuookami.ospf.kotlin.core.solver.heuristic.runner.ParticleSwarmRunner
import fuookami.ospf.kotlin.core.solver.heuristic.runner.ShadeRunner
import fuookami.ospf.kotlin.core.solver.heuristic.runner.IslandRunner
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.IslandMigration
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.RingMigration
import fuookami.ospf.kotlin.core.solver.heuristic.runner.ClpsoRunner
import fuookami.ospf.kotlin.core.solver.heuristic.runner.CmaEsRunner
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.LinearSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.BestArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.ElitistSurvivor
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.SurvivorSelection
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.RandomPerturbation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.Neighborhood
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.Acceptance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.AlwaysAcceptance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.BetterOnlyAcceptance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.NotWorseAcceptance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.topology.Topology
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.GreyWolfUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SineCosineUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.MultiVerseUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.HarmonySearchUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.WaterCycleUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SeagullUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.WarStrategyUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.EnergyValleyUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.GerminalCenterUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.WhaleOptimizationUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.HarrisHawkUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.ArithmeticOptimizationUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.TeachingLearningUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SalpSwarmUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.GravitationalSearchUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.FireflyUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.CuckooSearchUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.BatAlgorithmUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.MothFlameUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.GrasshopperUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.DifferentialCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.MetropolisAcceptance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.GeometricSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ConstantSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.NoMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.topology.GlobalTopology
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.ActionSelector
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.EpsilonGreedyActionSelector
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.Reward
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.ImprovementReward
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.StateDiscretizer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.DiversityStagnationDiscretizer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.QlearningAgent
import fuookami.ospf.kotlin.core.solver.heuristic.operator.rl.QlearningUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.Cross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation

/**
 * 组合式求解器工厂，把「算法」表达为「骨架 + 算子组合」。 / Composed solver factories expressing an "algorithm" as "skeleton + operator composition".
 *
 * 同一套骨架换不同算子即得到不同算法，无需为每个算法重写循环。 / Swapping operators on the same skeleton yields a different algorithm without rewriting the loop.
*/
data object ComposedSolvers {

    /**
     * 组合遗传算法 / Compose a genetic algorithm
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param cross 交叉算子 / Crossover operator
     * @param mutation 变异算子 / Mutation operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param mutationRate 变异率 / Mutation rate
     * @param topology 邻域拓扑 / Neighbourhood topology
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> genetic(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        cross: Cross = SinglePointCross,
        mutation: Mutation = GaussianMutation(),
        survivor: SurvivorSelection = ElitistSurvivor,
        mutationRate: Flt64 = Flt64(0.1),
        topology: Topology = GlobalTopology,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "genetic",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = cross,
                mutation = mutation,
                survivor = survivor,
                mutationRate = mutationRate,
                topology = topology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合爬山法（严格改进才接受）/ Compose hill climbing (accept only on strict improvement)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param neighborhood 邻域算子 / Neighbourhood operator
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> hillClimbing(
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        neighborhood: Neighborhood = RandomPerturbation(1),
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "hill-climbing",
            runner = TrajectoryRunner<ObjValue, V>(
                initializer = initializer,
                neighborhood = neighborhood,
                acceptance = BetterOnlyAcceptance,
                entropy = entropy,
                archive = BestArchive(1),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合随机游走（无条件接受）/ Compose a random walk (accept unconditionally)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param neighborhood 邻域算子 / Neighbourhood operator
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> randomWalk(
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        neighborhood: Neighborhood = RandomPerturbation(1),
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "random-walk",
            runner = TrajectoryRunner<ObjValue, V>(
                initializer = initializer,
                neighborhood = neighborhood,
                acceptance = AlwaysAcceptance,
                entropy = entropy,
                archive = BestArchive(1),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合灰狼优化（GWO）/ Compose grey wolf optimizer (GWO)
     *
     * GWO 没有交叉与变异，它的一次迭代就是一次「被三匹头狼牵引」的位置更新。因此这里把位置更新插到
     * [SolutionUpdate] 槽位、把变异换成 [NoMutation]，循环完全复用种群骨架。 / GWO has neither crossover nor mutation; one iteration is a single position update pulling every wolf by three leaders. Here the
     * position update occupies the [SolutionUpdate] slot, mutation is replaced by [NoMutation], and the loop is the shared
     * population skeleton.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> greyWolf(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = GreyWolfUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "gwo",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合正弦余弦算法（SCA）/ Compose the sine cosine algorithm (SCA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> sineCosine(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = SineCosineUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "sca",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合多元宇宙优化（MVO）/ Compose the multi-verse optimizer (MVO)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> multiVerse(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = MultiVerseUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "mvo",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合粒子群优化（PSO）/ Compose particle swarm optimization (PSO)
     *
     * PSO 需要携带速度与个体历史最优，无法写进无状态算子，因此使用第三种状态模板 [ParticleSwarmRunner]。 /
     * PSO carries a velocity and a personal best, which do not fit a stateless operator, so it uses the third state template
     * [ParticleSwarmRunner].
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 粒子数量 / Particle amount
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param inertia 惯性权重源 / Inertia weight source
     * @param personalWeight 个体学习因子源 / Personal learning factor source
     * @param socialWeight 社会学习因子源 / Social learning factor source
     * @param maxVelocity 最大速度 / Maximum velocity
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> particleSwarm(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        inertia: ParameterSource = ConstantSource(Flt64(0.4)),
        personalWeight: ParameterSource = ConstantSource(Flt64(2.0)),
        socialWeight: ParameterSource = ConstantSource(Flt64(2.0)),
        maxVelocity: Flt64 = Flt64(10000),
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "pso",
            runner = ParticleSwarmRunner<ObjValue, V>(
                size = size,
                inertia = inertia,
                personalWeight = personalWeight,
                socialWeight = socialWeight,
                maxVelocity = maxVelocity,
                initializer = initializer,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合和声搜索（HS）/ Compose harmony search (HS)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 和声库规模 / Harmony memory size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> harmonySearch(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = HarmonySearchUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "hs",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合水循环算法（WCA）/ Compose the water cycle algorithm (WCA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> waterCycle(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = WaterCycleUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "wca",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合海鸥优化算法（SOA）/ Compose the seagull optimization algorithm (SOA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> seagull(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = SeagullUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "soa",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合邻域搜索（NS）/ Compose neighbourhood search (NS)
     *
     * 邻域搜索与爬山仅在接受判据上不同：这里允许不劣即接受（含平台横向移动）。 / Neighbourhood search differs from hill climbing only in the acceptance criterion: moving across a plateau is allowed here.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param neighborhood 邻域算子 / Neighbourhood operator
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> neighborhoodSearch(
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        neighborhood: Neighborhood = RandomPerturbation(1),
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "ns",
            runner = TrajectoryRunner<ObjValue, V>(
                initializer = initializer,
                neighborhood = neighborhood,
                acceptance = NotWorseAcceptance,
                entropy = entropy,
                archive = BestArchive(1),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合战争策略优化（WarSO）/ Compose war strategy optimization (WarSO)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> warStrategy(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = WarStrategyUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "warso",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合能量谷优化（EVO）/ Compose the energy valley optimizer (EVO)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> energyValley(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = EnergyValleyUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "evo",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合生发中心优化（GCO）/ Compose germinal center optimization (GCO)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> germinalCenter(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = GerminalCenterUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "gco",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合差分进化（DE）/ Compose differential evolution (DE)
     *
     * 差分进化没有独立的变异步骤：`V = X_r1 + F·(X_r2 − X_r3)` 与二项式交叉本来就是一次操作，
     * 因此整代更新只是 `DifferentialCross`，变异位置留空。 / Differential evolution has no separate mutation step: `V = X_r1 + F·(X_r2 − X_r3)` and binomial crossover are
     * a single operation, so the whole generation update is just `DifferentialCross` and the mutation slot stays empty.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 解更新算子 / Solution update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> differentialEvolution(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = DifferentialCross(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "de",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合鲸鱼优化算法（WOA）/ Compose the whale optimization algorithm (WOA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> whaleOptimization(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = WhaleOptimizationUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "woa",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合哈里斯鹰优化（HHO）/ Compose Harris hawks optimization (HHO)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> harrisHawk(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = HarrisHawkUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "hho",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合算术优化算法（AOA）/ Compose the arithmetic optimization algorithm (AOA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> arithmeticOptimization(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = ArithmeticOptimizationUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "aoa",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合教与学优化（TLBO）/ Compose teaching-learning-based optimization (TLBO)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> teachingLearning(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = TeachingLearningUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "tlbo",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合樽海鞘群算法（SSA）/ Compose salp swarm algorithm (SSA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> salpSwarm(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = SalpSwarmUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "ssa",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合引力搜索算法（GSA）/ Compose gravitational search algorithm (GSA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> gravitationalSearch(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = GravitationalSearchUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "gsa",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合萤火虫算法（FA）/ Compose firefly algorithm (FA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> firefly(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = FireflyUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "fa",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合布谷鸟搜索（CS）/ Compose cuckoo search (CS)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> cuckooSearch(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = CuckooSearchUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "cs",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合蝙蝠算法（BA）/ Compose bat algorithm (BA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> batAlgorithm(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = BatAlgorithmUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "ba",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合飞蛾扑火优化（MFO）/ Compose moth-flame optimization (MFO)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> mothFlame(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = MothFlameUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "mfo",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合蝗虫优化算法（GOA）/ Compose grasshopper optimization algorithm (GOA)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 位置更新算子 / Position update operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> grasshopper(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        update: SolutionUpdate = GrasshopperUpdate(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "goa",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合 SHADE（成功历史自适应差分进化）/ Compose SHADE (success-history adaptive differential evolution)
     *
     * SHADE 的 `F` 与 `CR` 不是超参数，而是由成功历史累积出来的：每次子代优于父代时，把当次的 `F`、`CR`
     * 记入记忆库，再用莱默均值重新居中采样分布。因此这里没有 `differentialWeight` / `crossRate` 参数可调。 /
     * SHADE's `F` and `CR` are not hyper-parameters but are accumulated from success history: whenever an offspring beats its
     * parent, its `F` and `CR` enter the memory bank and the sampling distributions are re-centred by the Lehmer mean. That is
     * why there are no `differentialWeight` / `crossRate` knobs here.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param memorySize 记忆槽数量 / Number of memory slots
     * @param pBestRate p-best 比例 / p-best fraction
     * @param archiveAmount 档案容量 / Archive capacity
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> shade(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        memorySize: Int = 5,
        pBestRate: Double = 0.2,
        archiveAmount: Int = 1
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "shade",
            runner = ShadeRunner<ObjValue, V>(
                size = size,
                memorySize = memorySize,
                pBestRate = pBestRate,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合 L-SHADE（带种群缩减的 SHADE）/ Compose L-SHADE (SHADE with population reduction)
     *
     * 与 SHADE 的唯一差别是种群规模随迭代线性缩减，把算力逐步集中到少数优质个体上。
     * 缩减通过 [fuookami.ospf.kotlin.core.solver.heuristic.runner.ShadeRunner.sizeRatio] 表达，
     * 因此不需要另一套骨架。 / The only difference from SHADE is that the population size shrinks linearly with iterations, concentrating the
     * budget on fewer good individuals. The shrinkage is expressed through the runner's `sizeRatio`, so no second skeleton is
     * needed.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 初始种群规模 / Initial population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param minimumSize 缩减后的种群规模下界 / Lower bound of the shrunk population size
     * @param sizeRatio 种群规模的缩减源 / Shrinking source of the population size
     * @param memorySize 记忆槽数量 / Number of memory slots
     * @param pBestRate p-best 比例 / p-best fraction
     * @param archiveAmount 档案容量 / Archive capacity
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> lShade(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        minimumSize: Int = 4,
        sizeRatio: ParameterSource = LinearSource(Flt64.one, Flt64(0.05), UInt64(100)),
        memorySize: Int = 5,
        pBestRate: Double = 0.2,
        archiveAmount: Int = 1
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "l-shade",
            runner = ShadeRunner<ObjValue, V>(
                size = size,
                memorySize = memorySize,
                pBestRate = pBestRate,
                sizeRatio = sizeRatio,
                minimumSize = minimumSize,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合自适应粒子群（APSO）/ Compose adaptive particle swarm optimization (APSO)
     *
     * APSO 与标准 PSO 的差别不在结构，而在参数随迭代变化：惯性权重 w 由 0.9 线性降到 0.4，
     * 个体学习因子 c₁ 由 2.5 降到 0.5，社会学习因子 c₂ 反向由 0.5 升到 2.5。
     * 也就是「前期多信自己、多探索，后期多信群体、多开发」。 / APSO differs from standard PSO not in structure but in the parameters varying with iterations: the inertia weight w falls
     * linearly from 0.9 to 0.4, the personal factor c₁ falls from 2.5 to 0.5, and the social factor c₂ rises from 0.5 to 2.5.
     * Concretely: trust yourself and explore early, trust the swarm and exploit late.
     *
     * 因此它不需要新骨架，也不需要新算子——只是 [particleSwarm] 的一组参数调度。 /
     * It therefore needs neither a new skeleton nor a new operator: it is just a parameter schedule over [particleSwarm].
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 粒子数量 / Particle amount
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param inertia 惯性权重源 / Inertia weight source
     * @param personalWeight 个体学习因子源 / Personal learning factor source
     * @param socialWeight 社会学习因子源 / Social learning factor source
     * @param maxVelocity 最大速度 / Maximum velocity
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> adaptiveParticleSwarm(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        inertia: ParameterSource = LinearSource(Flt64(0.9), Flt64(0.4), UInt64(100)),
        personalWeight: ParameterSource = LinearSource(Flt64(2.5), Flt64(0.5), UInt64(100)),
        socialWeight: ParameterSource = LinearSource(Flt64(0.5), Flt64(2.5), UInt64(100)),
        maxVelocity: Flt64 = Flt64(10000),
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "apso",
            runner = ParticleSwarmRunner<ObjValue, V>(
                size = size,
                inertia = inertia,
                personalWeight = personalWeight,
                socialWeight = socialWeight,
                maxVelocity = maxVelocity,
                initializer = initializer,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合综合学习粒子群（CLPSO）/ Compose comprehensive learning particle swarm optimization (CLPSO)
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 粒子数量 / Particle amount
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param inertia 惯性权重源 / Inertia weight source
     * @param learningWeight 学习因子 c / Learning factor c
     * @param maxVelocity 最大速度 / Maximum velocity
     * @param refreshInterval 学习对象刷新间隔 / Exemplar refresh interval
     * @param stagnationLimit 重置 pbest 的停滞阈值 / Stagnation threshold for resetting pbest
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> comprehensiveLearningParticleSwarm(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        inertia: ParameterSource = LinearSource(Flt64(0.9), Flt64(0.4), UInt64(100)),
        learningWeight: Flt64 = Flt64(1.49445),
        maxVelocity: Flt64 = Flt64(10000),
        refreshInterval: Int = 7,
        stagnationLimit: Int = 5,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "clpso",
            runner = ClpsoRunner<ObjValue, V>(
                size = size,
                inertia = inertia,
                learningWeight = learningWeight,
                maxVelocity = maxVelocity,
                refreshInterval = refreshInterval,
                stagnationLimit = stagnationLimit,
                initializer = initializer,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合 CMA-ES（协方差矩阵自适应进化策略）/ Compose CMA-ES (covariance matrix adaptation evolution strategy)
     *
     * 这是唯一一个「分布型」算法：状态不是一群解，而是一个多元正态分布。它能学习变量之间的相关性与各方向的尺度，
     * 因此在强耦合的病态问题上远优于坐标型算法。代价是每代都要做一次特征分解。 /
     * This is the only distribution-based algorithm here: the state is not a set of solutions but a multivariate normal
     * distribution. It learns inter-variable correlations and per-direction scales, which makes it far better than coordinate-wise
     * methods on strongly coupled ill-conditioned problems. The price is one eigendecomposition per generation.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param initialSigma 初始步长 / Initial step size
     * @param archiveAmount 档案容量 / Archive capacity
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> cmaEs(
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        initialSigma: Double = 0.3,
        archiveAmount: Int = 1
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "cma-es",
            runner = CmaEsRunner<ObjValue, V>(
                initialSigma = initialSigma,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合岛模型遗传算法。 / Compose the island-model genetic algorithm.
     *
     * 岛模型把一个大种群拆成若干小岛：小种群更容易保住多样性，因此多峰问题上不容易整群陷入同一个局部最优；
     * 每隔 [migrationPeriod] 代按 [migration] 交换个体，好解又不会锁死在某个岛上。 /
     * The island model splits one large population into several islands: smaller populations preserve diversity better, so on
     * multimodal problems the species is less likely to collapse into one local optimum, while exchanging individuals every
     * `migrationPeriod` generations keeps good solutions from being locked inside one island.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param islandCount 岛屿数量 / Number of islands
     * @param islandSize 每个岛屿的种群规模 / Population size of each island
     * @param migrationPeriod 迁移周期（迭代数）/ Migration period in iterations
     * @param migration 迁移算子 / Migration operator
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param update 解更新算子 / Solution update operator
     * @param mutation 变异算子 / Mutation operator
     * @param mutationRate 变异率 / Mutation rate
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> islandGenetic(
        islandCount: Int,
        islandSize: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        migrationPeriod: UInt64 = UInt64(5),
        migration: IslandMigration<ObjValue, V> = RingMigration(),
        update: SolutionUpdate = ArithmeticCross,
        mutation: Mutation = GaussianMutation(),
        mutationRate: Flt64 = Flt64(0.1),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return islandGenetic(
            islands = List(islandCount) { IslandSpec(size = islandSize, mutationRate = mutationRate) },
            stopping = stopping,
            converter = converter,
            entropy = entropy,
            migrationPeriod = migrationPeriod,
            migration = migration,
            update = update,
            mutation = mutation,
            survivor = survivor,
            archiveAmount = archiveAmount
        )
    }

    /**
     * 组合岛模型遗传算法（异构岛屿）/ Compose an island-model genetic algorithm (heterogeneous islands)
     *
     * 与上一个重载的区别在于**每个岛可以有自己的规模与变异率**，对应旧 `GeneAlgorithm` 的
     * `List<PopulationBuilder>` 能力。旧类型里的 `eliteAmount` 与 `parentAmountRange` 从未被骨架读取，
     * 因此 [IslandSpec] 只保留真正生效的两项。 /
     * Unlike the overload above, each island may carry its own size and mutation rate, matching the old
     * `GeneAlgorithm`'s `List<PopulationBuilder>` capability. The old type also carried `eliteAmount` and
     * `parentAmountRange`, which the skeleton never read, so [IslandSpec] keeps only the two that take effect.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param islands 各岛屿规格 / Specification of each island
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param migrationPeriod 迁移周期（迭代数）/ Migration period in iterations
     * @param migration 迁移算子 / Migration operator
     * @param update 解更新算子 / Solution update operator
     * @param mutation 变异算子 / Mutation operator
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> islandGenetic(
        islands: List<IslandSpec>,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        migrationPeriod: UInt64 = UInt64(5),
        migration: IslandMigration<ObjValue, V> = RingMigration(),
        update: SolutionUpdate = ArithmeticCross,
        mutation: Mutation = GaussianMutation(),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        val runners = islands.map { spec ->
            PopulationRunner<ObjValue, V>(
                size = spec.size,
                initializer = initializer,
                update = update,
                mutation = mutation,
                survivor = survivor,
                mutationRate = spec.mutationRate,
                entropy = entropy,
                archive = BestArchive(1),
                stopping = StoppingCriterion { true },
                converter = converter
            )
        }
        return RunnerSolver(
            name = "island-ga",
            runner = IslandRunner(
                islands = runners,
                migrationPeriod = migrationPeriod,
                migration = migration,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter
            )
        )
    }

    /**
     * 组合 Q 学习驱动的种群搜索 / Compose a Q-learning driven population search
     *
     * 骨架提供一个池子的候选解更新算子，Q 学习按「多样性 × 停滞度」的态势在池子里挑一个来执行本代更新：
     * 探索期偏向幅度大的更新，收敛期偏向精细更新，而这一切不需要为每代写分支。 /
     * The skeleton is given a pool of candidate solution updates and Q-learning picks one per generation according to
     * the "diversity × stagnation" situation: wide moves while exploring, fine moves while converging, without a
     * branch per generation.
     *
     * 与旧实现的关键差别：奖励信号来自骨架档案的真实改进（`Archive.update` 的返回值），而不是旧实现里
     * 「Q 表只喂给自己」的空转。 / The crucial difference from the legacy version: the reward comes from the
     * skeleton archive's real improvement (the return value of `Archive.update`), not from the legacy set-up where
     * the Q-table only fed itself.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param size 种群规模 / Population size
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param candidates 候选解更新算子（动作集合）/ Candidate solution updates (the action set)
     * @param discretizer 状态离散化算子 / State discretization operator
     * @param selector 动作选择算子 / Action selection operator
     * @param reward 奖励算子 / Reward operator
     * @param alpha 学习率 / Learning rate
     * @param gamma 折扣因子 / Discount factor
     * @param survivor 环境选择算子 / Survivor selection operator
     * @param archiveAmount 档案容量 / Archive capacity
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> qLearning(
        size: Int,
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        candidates: List<SolutionUpdate> = listOf(GreyWolfUpdate(), SineCosineUpdate()),
        discretizer: StateDiscretizer = DiversityStagnationDiscretizer(),
        selector: ActionSelector = EpsilonGreedyActionSelector(),
        reward: Reward = ImprovementReward(),
        alpha: Flt64 = Flt64(0.5),
        gamma: Flt64 = Flt64(0.9),
        survivor: SurvivorSelection = ElitistSurvivor,
        archiveAmount: Int = 1,
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        val agent = QlearningAgent(
            discretizer = discretizer,
            actionAmount = candidates.size,
            selector = selector,
            reward = reward,
            alpha = alpha,
            gamma = gamma,
            randomGenerator = entropy
        )
        // 同一个对象既是解更新又是观测者：前者执行被选中的算子，后者根据本轮结果更新 Q 表。 /
        // One object is both the update and the observer: the former executes the chosen operator, the latter
        // updates the table from this round's outcome.
        val update = QlearningUpdate(candidates = candidates, agent = agent)
        return RunnerSolver(
            name = "q-learning",
            runner = PopulationRunner<ObjValue, V>(
                size = size,
                initializer = initializer,
                update = update,
                mutation = NoMutation,
                survivor = survivor,
                mutationRate = Flt64.zero,
                topology = GlobalTopology,
                entropy = entropy,
                archive = BestArchive(archiveAmount),
                stopping = stopping,
                converter = converter,
                observers = listOf(update)
            )
        )
    }

    /**
     * 组合模拟退火（SA）/ Compose simulated annealing (SA)
     *
     * 模拟退火就是「轨迹骨架 + Metropolis 判据」，与其他单解算法的唯一差别就是接受判据。 / Simulated annealing is "trajectory skeleton + Metropolis criterion"; the only difference from other single-solution
     * algorithms is the acceptance criterion.
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param stopping 终止判据 / Stopping criterion
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param neighborhood 邻域算子 / Neighbourhood operator
     * @param temperature 温度演化源 / Temperature evolution source
     * @param initializer 初始化算子 / Initialization operator
     * @return 求解器 / Solver
    */
    fun <ObjValue, V> simulatedAnnealing(
        stopping: StoppingCriterion,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        neighborhood: Neighborhood = RandomPerturbation(1),
        temperature: ParameterSource = GeometricSource(Flt64(100.0), Flt64(0.95)),
        initializer: Initializer = ModelInitializer
    ): HeuristicSolver<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
        return RunnerSolver(
            name = "simulated-annealing",
            runner = TrajectoryRunner<ObjValue, V>(
                initializer = initializer,
                neighborhood = neighborhood,
                acceptance = MetropolisAcceptance(temperature = temperature, entropy = entropy),
                entropy = entropy,
                archive = BestArchive(1),
                stopping = stopping,
                converter = converter
            )
        )
    }
}
