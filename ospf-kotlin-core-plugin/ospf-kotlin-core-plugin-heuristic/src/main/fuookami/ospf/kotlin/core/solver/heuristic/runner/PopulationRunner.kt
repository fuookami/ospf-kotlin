/**
 * 种群级搜索 / Population-level search
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.Archive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.Initializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.FitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.toIndividual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.SurvivorSelection
import fuookami.ospf.kotlin.core.solver.heuristic.operator.topology.Topology
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.topology.GlobalTopology
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation

/**
 * 种群级搜索模板，一代 = 交叉 → 变异 → 环境选择。 / Population-level search template where one generation is crossover → mutation → survivor selection.
 *
 * 所有基于种群的元启发式都是这三步的不同参数化：GA 用真实交叉，PSO 把交叉换成速度更新，
 * DE 把变异放进交叉。因此本模板只固定顺序，不固定算子。 / Every population-based metaheuristic is a different parameterisation of these three steps: GA uses a real crossover,
 * PSO replaces crossover with a velocity update, and DE folds mutation into crossover. The template therefore fixes
 * only the order, never the operators.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property size 种群规模 / Population size
 * @property initializer 初始化算子 / Initialization operator
 * @property fitnessEvaluator 适应度评估算子 / Fitness evaluation operator
 * @property update 解更新算子（交叉或位置更新）/ Solution update operator (crossover or position update)
 * @property mutation 变异算子 / Mutation operator
 * @property survivor 环境选择算子 / Survivor selection operator
 * @property mutationRate 变异率 / Mutation rate
 * @property topology 邻域拓扑 / Neighbourhood topology
*/
class PopulationRunner<ObjValue, V>(
    val size: Int,
    val initializer: Initializer = fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer,
    val fitnessEvaluator: FitnessEvaluator = fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.DefaultOnMissFitnessEvaluator,
    val update: SolutionUpdate,
    val mutation: Mutation,
    val survivor: SurvivorSelection,
    val mutationRate: Flt64 = Flt64(0.1),
    val topology: Topology = GlobalTopology,
    private val entropy: Generator<Flt64>,
    archive: Archive<ObjValue, V>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    observers: List<IterationObserver> = emptyList()
) : AbstractRunner<List<Individual<ObjValue, V>>, ObjValue, V>(archive, stopping, converter, observers)
    where V : RealNumber<V>, V : NumberField<V> {

    override suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): List<Individual<ObjValue, V>> {
        val solutions = initializer(
            iteration = iteration,
            model = model,
            amount = UInt64(size),
            converter = converter
        )
        return solutions
            .mapNotNull { fitnessEvaluator.toIndividual(model, it) }
            .take(size)
    }

    override suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: List<Individual<ObjValue, V>>
    ): List<Individual<ObjValue, V>> {
        if (state.isEmpty()) {
            return state
        }

        val offspring = coroutineScope {
            state.indices.map { index ->
                async(SearchExecution.dispatcher()) {
                    // 邻域内的父代先按优度升序排列：交叉本身不关心顺序，而位置更新类算子（GWO 的 α/β/δ 等）
                    // 依赖「前 k 名即头狼」的约定，因此由骨架统一保证。
                    // Neighbourhood parents are ranked by ascending quality first: crossover does not care about the order,
                    // whereas position-update operators (GWO's α/β/δ, …) rely on "the first k are the leaders", so the
                    // skeleton guarantees it uniformly.
                    val neighbours = topology
                        .neighbours(index = index, size = state.size)
                        .sorted()
                        .map { state[it] }
                    val parents = rankIndividuals(model, neighbours)
                    update(
                        iteration = iteration,
                        parents = parents,
                        model = model,
                        converter = converter,
                        entropy = entropy
                    ).mapNotNull { fitnessEvaluator.toIndividual(model, it) }
                }
            }.awaitAll().flatten()
        }

        val mutants = coroutineScope {
            state.map { individual ->
                async(SearchExecution.dispatcher()) {
                    val solution = mutation(
                        iteration = iteration,
                        individual = individual,
                        model = model,
                        converter = converter,
                        entropy = entropy,
                        mutationRate = mutationRate
                    )
                    fitnessEvaluator.toIndividual(model, solution)
                }
            }.awaitAll().filterNotNull()
        }

        return survivor(
            model = model,
            parents = state,
            offspring = offspring + mutants,
            amount = size
        )
    }

    /**
     * 供岛模型骨架使用的初始化入口 / Initialisation entry used by the island-model skeleton
     *
     * [initialize] 保持 `protected`，由本方法在模块内暴露给 [IslandRunner]，避免把钩子变成公开 API。 /
     * Keeps [initialize] protected and exposes it to [IslandRunner] inside the module, so the hooks do not become public API.
     *
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 当前迭代 / Current iteration
     * @return 初始种群 / Initial population
    */
    suspend fun openIsland(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): List<Individual<ObjValue, V>> {
        return initialize(model, iteration)
    }

    /**
     * 供岛模型骨架使用的单步入口 / Single-step entry used by the island-model skeleton
     *
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 当前迭代 / Current iteration
     * @param state 当前种群 / Current population
     * @return 推进后的种群 / Advanced population
    */
    suspend fun stepIsland(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: List<Individual<ObjValue, V>>
    ): List<Individual<ObjValue, V>> {
        return advance(model, iteration, state)
    }

    override fun membersOf(state: List<Individual<ObjValue, V>>): List<Individual<ObjValue, V>> {
        return state
    }
}
