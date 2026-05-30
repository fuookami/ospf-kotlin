@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.heuristic.sca

import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.core.solver.cleanupAfterSolverRun
import fuookami.ospf.kotlin.core.solver.cleanupOnSolverMemoryPressure
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.utils.functional.average
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/** 正弦余弦算法策略接口 / Sine Cosine Algorithm policy interface */
interface AbstractSCAPolicy<ObjValue, V> : AbstractHeuristicPolicy where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    /**
     * 计算控制参数 r1 / Calculate control parameter r1
     *
     * @param iteration 当前迭代 / current iteration
     * @return 控制参数 r1 / control parameter r1
     */
    fun r1(iteration: Iteration): Flt64

    /**
     * 计算随机参数 r2 / Calculate random parameter r2
     *
     * @param iteration 当前迭代 / current iteration
     * @param model 回调模型 / callback model
     * @return 随机参数 r2 列表 / random parameter r2 list
     */
    fun r2(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Flt64>

    /**
     * 计算随机参数 r3 / Calculate random parameter r3
     *
     * @param iteration 当前迭代 / current iteration
     * @param model 回调模型 / callback model
     * @return 随机参数 r3 列表 / random parameter r3 list
     */
    fun r3(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Flt64>

    /**
     * 移动个体 / Move individual
     *
     * @param iteration 当前迭代 / current iteration
     * @param solution 当前解 / current solution
     * @param bestSolution 最优解 / best solution
     * @param r1 控制参数 r1 / control parameter r1
     * @param r2 随机参数 r2 列表 / random parameter r2 list
     * @param r3 随机参数 r3 列表 / random parameter r3 list
     * @param model 回调模型 / callback model
     * @return 移动后的解 / moved solution
     */
    fun move(
        iteration: Iteration,
        solution: SolutionWithFitness<ObjValue, V>,
        bestSolution: SolutionWithFitness<ObjValue, V>,
        r1: Flt64,
        r2: List<Flt64>,
        r3: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): SolutionWithFitness<ObjValue, V>
}

data class QLearningState(
    val density: Flt64,
    val distance: Flt64
) {
    val discretize: UInt64 by lazy {
        val densityLevel = if (density leq Flt64.three.reciprocal()) {
            UInt64.zero
        } else if (density leq (Flt64.one - Flt64.three.reciprocal())) {
            UInt64.one
        } else {
            UInt64.two
        }
        val distanceLevel = if (distance leq Flt64.three.reciprocal()) {
            UInt64.zero
        } else if (distance leq (Flt64.one - Flt64.three.reciprocal())) {
            UInt64.one
        } else {
            UInt64.two
        }
        densityLevel * UInt64.three + distanceLevel
    }
}

class SCAPolicy<ObjValue, V>(
    private val alpha: Flt64 = Flt64(0.1),
    private val gamma: Flt64 = Flt64(0.9),
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    private val converter: IntoValue<V>
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractSCAPolicy<ObjValue, V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(
            alpha: Flt64 = Flt64(0.1),
            gamma: Flt64 = Flt64(0.9),
            iterationLimit: UInt64 = UInt64.maximum,
            notBetterIterationLimit: UInt64 = UInt64.maximum,
            timeLimit: Duration = 30.minutes,
            randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
        ): SCAPolicy<Flt64, Flt64> {
            return SCAPolicy(
                alpha = alpha,
                gamma = gamma,
                iterationLimit = iterationLimit,
                notBetterIterationLimit = notBetterIterationLimit,
                timeLimit = timeLimit,
                randomGenerator = randomGenerator,
                converter = flt64Converter
            )
        }
    }
    private val qTable = mutableMapOf<UInt64, MutableList<Flt64>>()
    private var currentState: UInt64 = UInt64.zero

    private val r1Range: Pair<Flt64, Flt64>
        get() {
            return when (currentState / UInt64.three) {
                UInt64.one -> Flt64.zero to (Flt64.one - Flt64.three.reciprocal())
                UInt64.two -> (Flt64.one - Flt64.three.reciprocal()) to (Flt64.one + Flt64.three.reciprocal())
                else -> (Flt64.one + Flt64.three.reciprocal()) to Flt64.two
            }
        }

    /** 计算控制参数 r1 / Calculate control parameter r1 */
    override fun r1(iteration: Iteration): Flt64 {
        val iterationCoefficient = max(
            ((iteration.iteration.toFlt64() * Flt64.pi)
                    / (Flt64.two * iterationLimit.toFlt64())).exp(),
            ((Flt64(iteration.time.toDouble(DurationUnit.SECONDS)) * Flt64.pi)
                    / (Flt64.two * Flt64(timeLimit.toDouble(DurationUnit.SECONDS)))).exp()
        )
        val (minR1, maxR1) = r1Range
        return minR1 + (maxR1 - minR1) * iterationCoefficient
    }

    /** 计算随机参数 r2 / Calculate random parameter r2 */
    override fun r2(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Flt64> {
        return model.tokens.tokens.indices.map {
            Flt64.two * Flt64.pi * randomGenerator()!!
        }
    }

    private val r3Range: Pair<Flt64, Flt64>
        get() {
            return when (currentState % UInt64.three) {
                UInt64.one -> Flt64.zero to (Flt64.one - Flt64.three.reciprocal())
                UInt64.two -> (Flt64.one - Flt64.three.reciprocal()) to (Flt64.one + Flt64.three.reciprocal())
                else -> (Flt64.one + Flt64.three.reciprocal()) to Flt64.two
            }
        }

    /** 计算随机参数 r3 / Calculate random parameter r3 */
    override fun r3(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Flt64> {
        val (minR3, maxR3) = r3Range
        return model.tokens.tokens.indices.map {
            minR3 + randomGenerator()!! * (maxR3 - minR3)
        }
    }

    /** 移动个体 / Move individual */
    override fun move(
        iteration: Iteration,
        solution: SolutionWithFitness<ObjValue, V>,
        bestSolution: SolutionWithFitness<ObjValue, V>,
        r1: Flt64,
        r2: List<Flt64>,
        r3: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): SolutionWithFitness<ObjValue, V> {
        val newSolution = solution.solution.mapIndexed { index, position ->
            val posFlt64 = converter.fromValue(position)
            val bestFlt64 = converter.fromValue(bestSolution.solution[index])
            val delta = r3[index] * bestFlt64 - posFlt64
            val newPosFlt64 = if (randomGenerator()!! < Flt64.two.reciprocal()) {
                posFlt64 + r1 * r2[index].sin() * abs(delta)
            } else {
                posFlt64 + r1 * r2[index].cos() * abs(delta)
            }
            converter.intoValue(newPosFlt64)
        }
        return SolutionWithFitness(
            newSolution,
            model.objective(newSolution).ifNull { model.defaultObjective }
        )
    }

    /** 更新策略状态（Q-learning 表和状态）/ Update policy state (Q-learning table and state) */
    override fun update(
        iteration: Iteration,
        better: Boolean,
        bestIndividual: Individual<*, *>,
        goodIndividuals: List<Individual<*, *>>,
        populations: List<List<Individual<*, *>>>,
        model: AbstractCallBackModelInterface<*, *, *>
    ) {
        if (better) {
            updateQTable(Flt64.one)
        } else {
            updateQTable(-Flt64(0.5))
        }
        updateState(
            population = populations.flatten(),
            best = bestIndividual,
            model = model
        )
    }

    /**
     * 更新 Q-learning 状态 / Update Q-learning state
     *
     * @param population 种群 / population
     * @param best 最优个体 / best individual
     * @param model 回调模型 / callback model
     */
    private fun updateState(
        population: List<Individual<*, *>>,
        best: Individual<*, *>,
        model: AbstractCallBackModelInterface<*, *, *>
    ) {
        val density = calculateDensity(population)
        val distance = calculateDistance(
            population = population,
            best = best,
            model = model
        )
        currentState = QLearningState(density, distance).discretize
    }

    /**
     * 更新 Q 表 / Update Q table
     *
     * @param reward 奖励值 / reward value
     */
    private fun updateQTable(reward: Flt64) {
        val actions = qTable.getOrPut(currentState) { mutableListOf(Flt64(9)) }
        val maxNextQ = qTable[currentState]?.maxOrNull() ?: Flt64.zero
        val bestAction = actions.indexOf(actions.maxOrNull()!!)
        actions[bestAction] += alpha * (reward + gamma * maxNextQ - actions[bestAction])
    }

    @Suppress("UNCHECKED_CAST")
    private fun typedPopulation(population: List<Individual<*, *>>): List<Individual<ObjValue, V>> {
        // 策略 update 回调链路中的 population 来自同一算法实例，元素类型与当前 ObjValue/V 一致。
        // In this policy update flow, population is produced by the same algorithm instance and matches ObjValue/V.
        return population as List<Individual<ObjValue, V>>
    }

    @Suppress("UNCHECKED_CAST")
    private fun typedIndividual(individual: Individual<*, *>): Individual<ObjValue, V> {
        // best 个体来自当前 population 的同源排序结果，类型约束与 population 保持一致。
        // The best individual comes from the same typed population ordering, so its type matches population constraints.
        return individual as Individual<ObjValue, V>
    }

    @Suppress("UNCHECKED_CAST")
    private fun unwrapBoundValue(value: Any?): V {
        // token 边界值由模型在同一泛型 V 下构建，这里只做边界读取时的最小作用域转换。
        // Token bound values are built under the same generic V; this keeps conversion local to bound extraction.
        return value as V
    }

    /**
     * 计算种群密度 / Calculate population density
     *
     * @param population 种群 / population
     * @return 密度值 / density value
     */
    private fun calculateDensity(
        population: List<Individual<*, *>>
    ): Flt64 {
        val safePopulation = typedPopulation(population)
        val flt64Solutions = safePopulation.map { ind -> ind.solution.map { converter.fromValue(it) } }
        val center = flt64Solutions.flatMap { it }.average()
        return flt64Solutions.sumOf { solution ->
            solution.sumOf { position ->
                (position - center).abs()
            }
        } / Flt64(population.size * flt64Solutions.first().size)
    }
    /**
     * 计算种群到最优个体的距离 / Calculate distance from population to best individual
     *
     * @param population 种群 / population
     * @param best 最优个体 / best individual
     * @param model 回调模型 / callback model
     * @return 距离值 / distance value
     */
    private fun calculateDistance(
        population: List<Individual<*, *>>,
        best: Individual<*, *>,
        model: AbstractCallBackModelInterface<*, *, *>
    ): Flt64 {
        val safePopulation = typedPopulation(population)
        val typedBest = typedIndividual(best)
        val bestFlt64 = typedBest.solution.map { converter.fromValue(it) }
        val maxDistance = model.tokens.tokens.sumOf {
            val upper = converter.fromValue(unwrapBoundValue(it.upperBound!!.value.unwrap()))
            val lower = converter.fromValue(unwrapBoundValue(it.lowerBound!!.value.unwrap()))
            (upper - lower).sqr()
        }.sqrt()
        return safePopulation.sumOf { individual ->
            val solFlt64 = individual.solution.map { converter.fromValue(it) }
            solFlt64.withIndex().sumOf { (index, position) ->
                (position - bestFlt64[index]).sqr()
            }.sqrt()
        } / (Flt64(population.size) * maxDistance)
    }
}

@OptIn(ExperimentalTime::class)
class SineCosineAlgorithm<Obj, ObjValue, V>(
    val populationAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractSCAPolicy<ObjValue, V>
) where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    /**
     * 执行正弦余弦算法 / Execute Sine Cosine Algorithm
     *
     * @param model 回调模型 / callback model
     * @param runningCallBack 运行回调函数 / running callback function
     * @return 最优个体列表 / best individual list
     */
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, ObjValue, V>,
        runningCallBack: ((Iteration, SolutionWithFitness<ObjValue, V>) -> Try)? = null
    ): List<Individual<ObjValue, V>> {
        val iteration = Iteration()
        val initialSolutions = model.initialSolutions(populationAmount)
        var population = initialSolutions
            .map {
                SolutionWithFitness(
                    it,
                    model.objective(it).ifNull { model.defaultObjective }
                )
            }
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
        var bestIndividual = population.first()
        val goodIndividuals = population.take(solutionAmount.toInt()).toMutableList()

        try {
            while (!policy.finished(iteration)) {
                var globalBetter = false

                val r1 = policy.r1(iteration)
                val r2 = policy.r2(iteration, model)
                val r3 = policy.r3(iteration, model)

                val newPopulation = coroutineScope {
                    population.map { individual ->
                        async(Dispatchers.Default) {
                            policy.move(
                                iteration = iteration,
                                solution = individual,
                                bestSolution = bestIndividual,
                                r1 = r1,
                                r2 = r2,
                                r3 = r3,
                                model = model
                            )
                        }
                    }.awaitAll()
                }.sortedWithPartialThreeWayComparator { lhs, rhs ->
                    model.compareObjective(lhs.fitness, rhs.fitness)
                }
                val newBestIndividual = newPopulation.first()
                population = newPopulation
                if (model.compareObjective(newBestIndividual.fitness, bestIndividual.fitness) is Order.Less) {
                    bestIndividual = newBestIndividual
                    globalBetter = true
                }
                refreshGoodIndividuals(
                    goodIndividuals = goodIndividuals,
                    newIndividuals = newPopulation,
                    model = model
                )

                model.flush()
                policy.update(
                    iteration = iteration,
                    better = globalBetter,
                    bestIndividual = bestIndividual,
                    goodIndividuals = goodIndividuals,
                    populations = listOf(population),
                    model = model
                )
                iteration.next(globalBetter)
                cleanupOnSolverMemoryPressure()

                if (runningCallBack?.invoke(iteration, bestIndividual) is Failed) {
                    break
                }
            }

            return goodIndividuals.take(solutionAmount.toInt())
        } finally {
            cleanupAfterSolverRun()
        }
    }
}

/** 单目标正弦余弦算法类型 / Single-objective Sine Cosine Algorithm type */
typealias SCA = SineCosineAlgorithm<Flt64, Flt64, Flt64>
/** 多目标正弦余弦算法类型 / Multi-objective Sine Cosine Algorithm type */
typealias MulObjSCA = SineCosineAlgorithm<List<Pair<MultiObjectLocation<Flt64>, Flt64>>, List<Flt64>, Flt64>
