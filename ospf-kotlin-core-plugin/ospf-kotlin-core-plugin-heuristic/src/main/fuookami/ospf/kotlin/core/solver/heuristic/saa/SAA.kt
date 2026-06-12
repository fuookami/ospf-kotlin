/** 模拟退火算法实现 / Simulated Annealing Algorithm implementation */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.heuristic.saa

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.cleanupAfterSolverRun
import fuookami.ospf.kotlin.core.solver.cleanupOnSolverMemoryPressure
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/**
 *
 *
 * @param V
 *
 * @property markovLength               the length of the markov chain
 */
/** 模拟退火算法策略接口 / Simulated Annealing Algorithm policy interface */
interface AbstractSAAPolicy<ObjValue, V> :
    AbstractHeuristicPolicy where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    /** 马尔可夫链长度 / Markov chain length */
    val markovLength: UInt64

    /**
     * 变换解 / Transform solution
     *
     * @param iteration 当前迭代 / current iteration
     * @param solution 当前解 / current solution
     * @param model 回调模型 / callback model
     * @return 变换后的解 / transformed solution
     */
    fun transformSolution(
        iteration: Iteration,
        solution: Solution<V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Solution<V>

    /**
     * 接受准则 / Acceptance criterion
     *
     * @param iteration 当前迭代 / current iteration
     * @param currentObjective 当前目标值 / current objective value
     * @param newObjective 新目标值 / new objective value
     * @return 是否接受 / whether to accept
     */
    fun accept(
        iteration: Iteration,
        currentObjective: ObjValue,
        newObjective: ObjValue
    ): Boolean

    /** 改进参数（如步长）/ Improve parameters (e.g., step size) */
    fun improve()
}

/**
 *
 *
 * @param V
 * @property initialTemperature         initial temperature
 * @property finalTemperature           final temperature
 * @property temperatureGradiant        temperature gradiant, T(k) = gradiant * T(k - 1)
 * @property step                       the step size for searching
 * @property disturbanceAmount          the amount of disturbance
 * @property distance                   the distance rate function between two objectives
 * @property randomGenerator
 */
/**
 * 模拟退火算法策略
 *
 * 实现模拟退火的解变换、接受准则和温度调度，支持自适应步长衰减。
 *
 * Simulated Annealing Algorithm policy
 *
 * Implements solution transformation, acceptance criterion, and temperature scheduling
 * for simulated annealing, supporting adaptive step size decay.
 *
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property initialTemperature 初始温度 / initial temperature
 * @property finalTemperature 终止温度 / final temperature
 * @property temperatureGradiant 温度梯度 / temperature gradient
 * @property markovLength Markov 链长度 / Markov chain length
 * @property disturbanceAmount 扰动点数量 / disturbance point amount
 * @property distance 目标值距离函数 / objective distance function
 * @property randomGenerator 随机数生成器 / random number generator
 */
open class SAAPolicy<ObjValue, V>(
    val initialTemperature: Flt64 = Flt64(100.0),
    val finalTemperature: Flt64 = Flt64(1.0),
    val temperatureGradiant: Flt64 = Flt64(0.98),
    override val markovLength: UInt64 = UInt64(100),
    step: Flt64 = Flt64(0.5),
    val disturbanceAmount: UInt64 = UInt64(1),
    val distance: (ObjValue, ObjValue) -> Flt64,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    private val converter: IntoValue<V>
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractSAAPolicy<ObjValue, V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(
            initialTemperature: Flt64 = Flt64(100.0),
            finalTemperature: Flt64 = Flt64(1.0),
            temperatureGradiant: Flt64 = Flt64(0.98),
            markovLength: UInt64 = UInt64(100),
            step: Flt64 = Flt64(0.5),
            disturbanceAmount: UInt64 = UInt64(1),
            iterationLimit: UInt64 = UInt64.maximum,
            notBetterIterationLimit: UInt64 = UInt64.maximum,
            timeLimit: Duration = 30.minutes,
            randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
        ): SAAPolicy<Flt64, Flt64> {
            return SAAPolicy(
                initialTemperature = initialTemperature,
                finalTemperature = finalTemperature,
                temperatureGradiant = temperatureGradiant,
                markovLength = markovLength,
                step = step,
                disturbanceAmount = disturbanceAmount,
                distance = { lhs, rhs -> (lhs - rhs).abs() },
                iterationLimit = iterationLimit,
                notBetterIterationLimit = notBetterIterationLimit,
                timeLimit = timeLimit,
                randomGenerator = randomGenerator,
                converter = flt64Converter
            )
        }
    }

    private var _step: Flt64 = step
    val step: Flt64 by ::_step

    private val temperatureCache: MutableList<Flt64> = arrayListOf()

    /** 变换解 / Transform solution */
    override fun transformSolution(
        iteration: Iteration,
        solution: Solution<V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Solution<V> {
        val flt64Solution = solution.map { converter.fromValue(it) }.toMutableList()
        val disturbancePoints: MutableSet<Int> = HashSet()
        while (disturbancePoints.size < disturbanceAmount.toInt()) {
            val point = (randomGenerator()!! * Flt64(flt64Solution.size)).round().toUInt64().toInt()
            disturbancePoints.add(point)
        }
        for (point in disturbancePoints) {
            val token = model.tokens[point]
            val newValue = flt64Solution[point] + step * (token.upperBound!!.value.unwrap() - token.lowerBound!!.value.unwrap()) * randomGenerator()!!
            flt64Solution[point] = coerceIn(
                iteration = iteration,
                index = point,
                value = newValue,
                model = model
            )
        }
        return flt64Solution.map { converter.intoValue(it) }
    }

    /** 接受准则 / Acceptance criterion */
    override fun accept(iteration: Iteration, currentObjective: ObjValue, newObjective: ObjValue): Boolean {
        val delta = distance(currentObjective, newObjective)
        val acceptPercentage = (-delta / currentTemperature(iteration)).exp()
        return acceptPercentage gr randomGenerator()!!
    }

    /** 改进步长 / Improve step size */
    override fun improve() {
        _step *= Flt64(0.99)
    }

    /** 判断是否结束（温度低于终温或达到迭代限制）/ Check if finished (temperature below final or iteration limit reached) */
    override fun finished(iteration: Iteration): Boolean {
        return super.finished(iteration) || currentTemperature(iteration) leq finalTemperature
    }

    /**
     * 获取当前温度 / Get current temperature
     *
     * @param iteration 当前迭代 / current iteration
     * @return 当前温度 / current temperature
     */
    private fun currentTemperature(iteration: Iteration): Flt64 {
        if (iteration.iteration >= UInt64(temperatureCache.size)) {
            for (i in temperatureCache.size..iteration.iteration.toInt()) {
                temperatureCache.add(temperatureCache.lastOrNull().ifNull { initialTemperature } * temperatureGradiant)
            }
        }
        return temperatureCache[iteration.iteration.toInt()]
    }
}

@OptIn(ExperimentalTime::class)
/**
 * 模拟退火算法
 *
 * 实现基于温度调度的模拟退火优化算法，通过 Metropolis 准则接受劣解以跳出局部最优。
 *
 * Simulated Annealing Algorithm
 *
 * Implements temperature-scheduling-based simulated annealing optimization algorithm,
 * accepting worse solutions via Metropolis criterion to escape local optima.
 *
 * @param Obj 目标类型 / objective type
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property policy 模拟退火策略 / simulated annealing policy
 */
class SimulatedAnnealingAlgorithm<Obj, ObjValue, V>(
    val policy: AbstractSAAPolicy<ObjValue, V>
) where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(): SimulatedAnnealingAlgorithm<Flt64, Flt64, Flt64> {
            return SimulatedAnnealingAlgorithm(SAAPolicy())
        }
    }

    /**
     * 执行模拟退火算法 / Execute Simulated Annealing Algorithm
     *
     * @param model 回调模型 / callback model
     * @param runningCallBack 运行回调函数 / running callback function
     * @return 最优个体列表 / best individual list
     */
    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, ObjValue, V>,
        runningCallBack: ((Iteration, SolutionWithFitness<ObjValue, V>) -> Try)? = null
    ): List<Individual<ObjValue, V>> {
        val iteration = Iteration()
        val initialSolution = model.initialSolutions()
            .map {
                SolutionWithFitness(
                    solution = it,
                    fitness = model.objective(it).ifNull { model.defaultObjective }
                )
            }
            .sortedWithPartialThreeWayComparator { lhs, rhs -> model.compareObjective(lhs.fitness, rhs.fitness) }
            .first()
        var bestSolution = initialSolution
        var currentSolution = initialSolution

        try {
            while (!policy.finished(iteration)) {
                var globalBetter = false
                var betterTimes = UInt64.zero
                var badAcceptTimes = UInt64.zero
                var badRefuseTimes = UInt64.zero

                for (t in 0 until policy.markovLength.toInt()) {
                    val newSolution = policy.transformSolution(
                        iteration = iteration,
                        solution = currentSolution.solution,
                        model = model
                    )
                    val newObj = model.objective(newSolution).ifNull { model.defaultObjective }

                    val accept = if (model.compareObjective(newObj, currentSolution.fitness) is Order.Less) {
                        betterTimes += UInt64.one
                        true
                    } else {
                        if (policy.accept(iteration, currentSolution.fitness, newObj)) {
                            badAcceptTimes += UInt64.one
                            true
                        } else {
                            badRefuseTimes += UInt64.one
                            false
                        }
                    }

                    if (accept) {
                        currentSolution = SolutionWithFitness(
                            solution = newSolution,
                            fitness = newObj
                        )
                        if (model.compareObjective(currentSolution.fitness, bestSolution.fitness) is Order.Less) {
                            bestSolution = currentSolution
                            globalBetter = true
                            policy.improve()
                        }
                    }
                }

                model.flush()
                policy.update(
                    iteration = iteration,
                    better = globalBetter,
                    bestIndividual = bestSolution,
                    goodIndividuals = listOf(bestSolution),
                    populations = listOf(listOf(currentSolution)),
                    model = model
                )
                iteration.next(globalBetter)
                cleanupOnSolverMemoryPressure()

                if (runningCallBack?.invoke(iteration, bestSolution) is Failed) {
                    break
                }
            }

            return listOf(bestSolution)
        } finally {
            cleanupAfterSolverRun()
        }
    }
}

/** 单目标模拟退火算法类型 / Single-objective Simulated Annealing Algorithm type */
typealias SAA = SimulatedAnnealingAlgorithm<Flt64, Flt64, Flt64>
/** 多目标模拟退火算法类型 / Multi-objective Simulated Annealing Algorithm type */
typealias MulObjSAA = SimulatedAnnealingAlgorithm<List<Pair<MultiObjectLocation<Flt64>, Flt64>>, List<Flt64>, Flt64>
