/**
 * NSGA-II 骨架 / NSGA-II skeleton
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.Dominance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.CrowdingDistance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.FastNonDominatedSort
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.ParetoArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation

/**
 * 多目标求解结果。 / Multi-objective solve result.
 *
 * 多目标没有「唯一最优」，因此返回的是一整条前沿，而不是单个解。 / Multi-objective problems have no single optimum, so the result is an entire front rather than one solution.
 *
 * @param V 值类型 / Value type
 * @property front 前沿上的解，按分层顺序排列 / Solutions on the front, ordered by front index
 * @property objectives 与 [front] 一一对应的目标向量 / Objective vectors aligned with [front]
 * @property iteration 迭代信息 / Iteration info
*/
data class MultiObjectiveResult<V>(
    val front: List<Solution<V>>,
    val objectives: List<List<Flt64>>,
    val iteration: Iteration
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 多目标求解器契约。 / Multi-objective solver contract.
 *
 * @param V 值类型 / Value type
*/
interface MultiObjectiveSolver<V> where V : RealNumber<V>, V : NumberField<V> {
    /** 算法名 / Algorithm name */
    val name: String

    /**
     * 求解模型 / Solve the model
     *
     * @param model 回调模型接口 / Callback model interface
     * @return 前沿 / The front
    */
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<*, *, V>
    ): Ret<MultiObjectiveResult<V>>
}

/**
 * NSGA-II 骨架。 / NSGA-II skeleton.
 *
 * 与单目标骨架的区别是根本性的，因此它是一个独立类而不是 [AbstractRunner] 的子类： /
 * The difference from the single-objective skeleton is fundamental, so this is a standalone class rather than an [AbstractRunner]
 * subclass:
 *
 * - 环境选择不是「比较两个目标值」，而是「先分层、层内再按拥挤距离截断」；
 *   survivor selection is not "compare two objective values" but "layer first, then truncate each layer by crowding distance";
 * - 收敛载体不是「最好的 k 个解」，而是一整条互不支配的前沿；
 *   the convergence carrier is not "the best k solutions" but a whole mutually non-dominated front;
 * - 模型只提供变量与约束，目标向量由 [objectivesOf] 直接给出——
 *   `CallBackModel` 的 `ObjValue` 是标量，多目标信息在模型层已被聚合掉。
 *   the model only supplies variables and constraints; objective vectors come straight from [objectivesOf], because
 *   `CallBackModel`'s `ObjValue` is scalar and the multi-objective information is aggregated away at the model level.
 *
 * @param V 值类型 / Value type
 * @property size 种群规模 / Population size
 * @property objectivesOf 目标向量提取器 / Objective vector extractor
 * @property dominance 支配判据 / Dominance predicate
 * @property update 解更新算子 / Solution update operator
 * @property mutation 变异算子 / Mutation operator
 * @property mutationRate 变异率 / Mutation rate
 * @property entropy 均匀分布熵源 / Uniform entropy source
 * @property stopping 终止判据 / Stopping criterion
 * @property converter 值转换器 / Value converter
 * @property archiveAmount 前沿档案容量 / Front archive capacity
*/
open class Nsga2Runner<V>(
    val size: Int,
    val objectivesOf: (Solution<V>) -> List<Flt64>,
    val dominance: Dominance,
    val update: SolutionUpdate,
    val mutation: Mutation,
    val mutationRate: Flt64 = Flt64(0.1),
    val entropy: Generator<Flt64>,
    val stopping: StoppingCriterion,
    val converter: IntoValue<V>,
    val archiveAmount: Int = 100
) where V : RealNumber<V>, V : NumberField<V> {

    /** 算法名 / Algorithm name */
    open val name: String get() = "nsga-ii"

    /**
     * 执行搜索 / Run the search
     *
     * @param model 回调模型接口 / Callback model interface
     * @return 多目标结果 / Multi-objective result
    */
    suspend fun run(model: AbstractCallBackModelInterface<*, *, V>): Ret<MultiObjectiveResult<V>> {
        val iteration = Iteration()
        val archive = ParetoArchive(
            capacity = archiveAmount,
            objectivesOf = { individual: SolutionWithFitness<List<Flt64>, V> -> individual.fitness },
            dominance = dominance
        )

        var population = model
            .initialSolutions(UInt64(size))
            .mapNotNull { evaluate(it) }
            .take(size)

        if (population.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "the model produced no initial solution / 模型未产生初始解"
            )
        }

        archive.offer(population)

        while (!stopping(iteration)) {
            val offspring = breed(model = model, iteration = iteration, population = population)
            val combined = population + offspring

            population = select(combined)
            archive.offer(population)

            val improved = offspring.isNotEmpty()
            iteration.next(improved)
            model.flush()
        }

        val front = archive.items
        return ok(
            MultiObjectiveResult(
                front = front.map { it.solution },
                objectives = front.map { it.fitness },
                iteration = iteration
            )
        )
    }

    /** 评估一个解 / Evaluate one solution */
    private fun evaluate(solution: Solution<V>): SolutionWithFitness<List<Flt64>, V>? {
        val objectives = objectivesOf(solution)
        if (objectives.isEmpty() || objectives.any { !it.toDouble().isFinite() }) {
            return null
        }
        return SolutionWithFitness(solution = solution, fitness = objectives)
    }

    /** 由当前种群繁殖子代 / Breed offspring from the current population */
    private suspend fun breed(
        model: AbstractCallBackModelInterface<*, *, V>,
        iteration: Iteration,
        population: List<SolutionWithFitness<List<Flt64>, V>>
    ): List<SolutionWithFitness<List<Flt64>, V>> {
        return coroutineScope {
            val groups = population.indices.map { index ->
                async(Dispatchers.Default) {
                    val parents = listOf(population[index], population[(index + 1) % population.size])
                    update(
                        iteration = iteration,
                        parents = parents,
                        model = model,
                        converter = converter,
                        entropy = entropy
                    ).mapNotNull { evaluate(it) }
                }
            }.awaitAll().flatten()

            val mutants = population.map { individual ->
                async(Dispatchers.Default) {
                    val solution = mutation(
                        iteration = iteration,
                        individual = individual,
                        model = model,
                        converter = converter,
                        entropy = entropy,
                        mutationRate = mutationRate
                    )
                    evaluate(solution)
                }
            }.awaitAll().filterNotNull()

            groups + mutants
        }
    }

    /**
     * 环境选择：先按非支配分层，逐层填满，最后一层按拥挤距离截断。 / Survivor selection: layer by non-dominance, fill front by front, truncate the last one by crowding distance.
     *
     * @param candidates 父代与子代的并集 / Union of parents and offspring
     * @return 新的种群 / New population
    */
    protected open fun select(
        candidates: List<SolutionWithFitness<List<Flt64>, V>>
    ): List<SolutionWithFitness<List<Flt64>, V>> {
        if (candidates.size <= size) {
            return candidates
        }

        val fronts = FastNonDominatedSort.sort(
            items = candidates,
            objectivesOf = { it.fitness },
            dominance = dominance
        )

        val selected = ArrayList<SolutionWithFitness<List<Flt64>, V>>(size)
        for (front in fronts) {
            if (selected.size + front.size <= size) {
                selected.addAll(front)
                continue
            }

            // 最后一层：保留拥挤距离最大的那些，维持前沿的覆盖度。
            // Last layer: keep the largest crowding distances to preserve the spread of the front.
            val distances = CrowdingDistance.assign(front.map { it.fitness })
            val remaining = size - selected.size
            val order = MutableList(front.size) { it }
            repeat(remaining) {
                var bestIndex = 0
                var bestDistance = Double.NEGATIVE_INFINITY
                for (position in order.indices) {
                    val candidate = order[position]
                    val distance = distances[candidate]
                    if (distance > bestDistance) {
                        bestDistance = distance
                        bestIndex = position
                    }
                }
                selected.add(front[order.removeAt(bestIndex)])
            }
            break
        }
        return selected
    }
}
