/** 多元宇宙优化器实现 / Multi-Verse Optimizer implementation */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.heuristic.mvo

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.cleanupAfterSolverRun
import fuookami.ospf.kotlin.core.solver.cleanupOnSolverMemoryPressure
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.utils.functional.*

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/** 宇宙个体类型，带有适应度的解 / Universe type, solution with fitness */
typealias Universe<ObjValue, V> = SolutionWithFitness<ObjValue, V>

/** 多元宇宙优化器策略接口 / Multi-Verse Optimizer policy interface */
interface AbstractMVOPolicy<ObjValue, V> :
    AbstractHeuristicPolicy where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {

    /**
     * 计算虫洞存在概率（WEP）/ Calculate Wormhole Existence Probability (WEP)
     *
     * @param iteration 当前迭代 / current iteration
     * @return 虫洞存在概率 / wormhole existence probability
    */
    fun wep(iteration: Iteration): Flt64

    /**
     * 计算旅行距离率（TDR）/ Calculate Travelling Distance Rate (TDR)
     *
     * @param iteration 当前迭代 / current iteration
     * @return 旅行距离率 / travelling distance rate
    */
    fun tdr(iteration: Iteration): Flt64

    /**
     * 计算白洞概率 / Calculate white hole rates
     *
     * @param model 回调模型 / callback model
     * @param objs 目标值列表 / objective value list
     * @return 白洞概率列表 / white hole rate list
    */
    fun whiteHoleRates(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        objs: List<ObjValue>
    ): List<Flt64>

    /**
     * 变换宇宙 / Transform universes
     *
     * @param iteration 当前迭代 / current iteration
     * @param bestSolution 最优解 / best solution
     * @param solutions 宇宙列表 / universe list
     * @param model 回调模型 / callback model
     * @param wep 虫洞存在概率 / wormhole existence probability
     * @param tdr 旅行距离率 / travelling distance rate
     * @return 变换后的解列表 / transformed solution list
    */
    fun transformUniverses(
        iteration: Iteration,
        bestSolution: Solution<V>,
        solutions: List<Universe<ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        wep: Flt64,
        tdr: Flt64
    ): List<Solution<V>>
}

/**
 * 多元宇宙优化器策略
 *
 * 实现多元宇宙优化的虫洞存在概率、旅行距离率和白洞概率计算，
 * 使用白洞/虫洞穿越机制进行全局搜索。
 *
 * Multi-Verse Optimizer policy
 *
 * Implements Wormhole Existence Probability, Travelling Distance Rate, and white hole rate calculation
 * for MVO, using white hole/wormhole traversal mechanism for global search.
 *
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property minWEP 最小虫洞存在概率 / minimum wormhole existence probability
 * @property maxWEP 最大虫洞存在概率 / maximum wormhole existence probability
 * @property whiteHoleSelector 白洞选择器 / white hole selector
 * @property whiteHoleRateCalculator 白洞概率计算器 / white hole rate calculator
 * @property randomGenerator 随机数生成器 / random number generator
*/
open class MVOPolicy<ObjValue, V>(
    val minWEP: Flt64 = Flt64(0.2),
    val maxWEP: Flt64 = Flt64(1.0),
    val whiteHoleSelector: Selection,
    val whiteHoleRateCalculator: ObjectiveNormalization<ObjValue, V>,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    private val converter: IntoValue<V>
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractMVOPolicy<ObjValue, V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {

    /**
     * 创建默认多元宇宙优化器策略 / Create default MVO policy
     *
     * @param minWEP 最小虫洞存在概率 / minimum wormhole existence probability
     * @param maxWEP 最大虫洞存在概率 / maximum wormhole existence probability
     * @param iterationLimit 迭代次数上限 / iteration limit
     * @param notBetterIterationLimit 无改进迭代次数上限 / not-better iteration limit
     * @param timeLimit 时间上限 / time limit
     * @param randomGenerator 随机数生成器 / random number generator
     * @return 默认 MVO 策略实例 / default MVO policy instance
    */
    companion object {
        operator fun invoke(
            minWEP: Flt64 = Flt64(0.2),
            maxWEP: Flt64 = Flt64(1.0),
            iterationLimit: UInt64 = UInt64.maximum,
            notBetterIterationLimit: UInt64 = UInt64.maximum,
            timeLimit: Duration = 30.minutes,
            randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
        ): MVOPolicy<Flt64, Flt64> {
            return MVOPolicy(
                minWEP = minWEP,
                maxWEP = maxWEP,
                whiteHoleRateCalculator = SumNormalization,
                whiteHoleSelector = RouletteSelection(randomGenerator),
                iterationLimit = iterationLimit,
                notBetterIterationLimit = notBetterIterationLimit,
                timeLimit = timeLimit,
                randomGenerator = randomGenerator,
                converter = flt64Converter
            )
        }
    }

    /** 计算虫洞存在概率 / Calculate wormhole existence probability */
    override fun wep(iteration: Iteration): Flt64 {
        return minWEP + (maxWEP - minWEP) * max(
            Flt64(iteration.time / timeLimit),
            iteration.iteration.toFlt64() / iterationLimit.toFlt64()
        )
    }

    /** 计算旅行距离率 / Calculate travelling distance rate */
    override fun tdr(iteration: Iteration): Flt64 {
        return Flt64.one - max(
            Flt64(iteration.time / timeLimit),
            iteration.iteration.toFlt64() / iterationLimit.toFlt64()
        ).pow(Flt64(6.0).reciprocal())!!.toFlt64()
    }

    /** 计算白洞概率 / Calculate white hole rates */
    override fun whiteHoleRates(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        objs: List<ObjValue>
    ): List<Flt64> {
        return whiteHoleRateCalculator(model, objs)
    }

    /** 变换宇宙 / Transform universes */
    override fun transformUniverses(
        iteration: Iteration,
        bestSolution: Solution<V>,
        solutions: List<Universe<ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        wep: Flt64,
        tdr: Flt64
    ): List<Solution<V>> {
        val newSolutions = ArrayList<Solution<V>>()
        val whiteHoleRates = whiteHoleRates(model, solutions.map { it.fitness })
        val bestFlt64 = bestSolution.map { converter.fromValue(it) }
        for ((i, solution) in solutions.withIndex()) {
            val newSolution = solution.solution.toMutableList()
            for ((dimension, value) in solution.solution.withIndex()) {
                val token = model.tokens[dimension]
                val newFlt64 = if (randomGenerator()!! ls whiteHoleRates[i]) {
                    val selected = solutions[whiteHoleSelector(iteration, whiteHoleRates).toInt()].solution[dimension]
                    converter.fromValue(selected)
                } else {
                    converter.fromValue(value)
                }
                val finalFlt64 = if (randomGenerator()!! ls wep) {
                    applyWormhole(
                        bestSolutionFlt64 = bestFlt64,
                        dimension = dimension,
                        lowerBound = token.lowerBound!!.value.unwrap(),
                        upperBound = token.upperBound!!.value.unwrap(),
                        tdr = tdr
                    )
                } else {
                    newFlt64
                }
                newSolution[dimension] = converter.intoValue(
                    coerceIn(
                        iteration = iteration,
                        index = dimension,
                        value = finalFlt64,
                        model = model
                    )
                )
            }
            newSolutions.add(newSolution)
        }
        return newSolutions
    }

    /**
     * 应用虫洞变换 / Apply wormhole transformation
     *
     * @param bestSolutionFlt64 最优解的 Flt64 值列表 / best solution Flt64 value list
     * @param dimension 维度索引 / dimension index
     * @param lowerBound 下界 / lower bound
     * @param upperBound 上界 / upper bound
     * @param tdr 旅行距离率 / travelling distance rate
     * @return 变换后的值 / transformed value
    */
    private fun applyWormhole(
        bestSolutionFlt64: List<Flt64>,
        dimension: Int,
        lowerBound: Flt64,
        upperBound: Flt64,
        tdr: Flt64
    ): Flt64 {
        val range = upperBound - lowerBound
        return if (randomGenerator()!! ls Flt64(0.5)) {
            bestSolutionFlt64[dimension] + tdr * (range * randomGenerator()!! + lowerBound)
        } else {
            bestSolutionFlt64[dimension] - tdr * (range * randomGenerator()!! + lowerBound)
        }
    }
}

@OptIn(ExperimentalTime::class)

/**
 * 多元宇宙优化器
 *
 * 实现基于多元宇宙理论的优化算法，使用白洞和虫洞穿越机制在解空间中搜索最优解。
 *
 * Multi-Verse Optimizer
 *
 * Implements optimization algorithm based on multi-verse theory, using white hole and wormhole
 * traversal mechanism to search for optimal solutions in the solution space.
 *
 * @param Obj 目标类型 / objective type
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property population 种群构建参数列表 / population builder list
 * @property solutionAmount 期望解的数量 / desired number of solutions
 * @property policy 多元宇宙优化器策略 / MVO policy
*/
class MultiVerseOptimizer<Obj, ObjValue, V>(
    val universeAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractMVOPolicy<ObjValue, V>
) where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {

    /**
     * 执行多元宇宙优化算法 / Execute Multi-Verse Optimizer
     *
     * @param model 回调模型 / callback model
     * @param runningCallBack 运行回调函数 / running callback function
     * @return 最优个体列表 / best individual list
    */
    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, ObjValue, V>,
        runningCallBack: ((Iteration, Universe<ObjValue, V>, List<Universe<ObjValue, V>>) -> Try)? = null
    ): List<Individual<ObjValue, V>> {
        val iteration = Iteration()
        var universes = model.initialSolutions(universeAmount)
            .map {
                Universe(
                    solution = it,
                    fitness = model.objective(it).ifNull { model.defaultObjective }
                )
            }
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
        var bestUniverse = universes.first()
        val goodUniverses = universes.take(solutionAmount.toInt()).toMutableList()

        try {
            while (!policy.finished(iteration)) {
                var globalBetter = false
                val wep = policy.wep(iteration)
                val tdr = policy.tdr(iteration)

                val newUniverses = policy
                    .transformUniverses(
                        iteration = iteration,
                        bestSolution = bestUniverse.solution,
                        solutions = universes,
                        model = model,
                        wep = wep,
                        tdr = tdr
                    )
                    .map {
                        Universe(
                            solution = it,
                            fitness = model.objective(it).ifNull { model.defaultObjective }
                        )
                    }
                    .sortedWithPartialThreeWayComparator { lhs, rhs ->
                        model.compareObjective(lhs.fitness, rhs.fitness)
                    }
                val newBestUniverse = newUniverses.first()
                universes = newUniverses
                if (model.compareObjective(newBestUniverse.fitness, bestUniverse.fitness) is Order.Less) {
                    bestUniverse = newBestUniverse
                    globalBetter = true
                }
                refreshGoodIndividuals(
                    goodIndividuals = goodUniverses,
                    newIndividuals = newUniverses,
                    model = model,
                    solutionAmount = solutionAmount
                )

                model.flush()
                policy.update(
                    iteration = iteration,
                    better = globalBetter,
                    bestIndividual = bestUniverse,
                    goodIndividuals = goodUniverses,
                    populations = listOf(universes),
                    model = model
                )
                iteration.next(globalBetter)
                cleanupOnSolverMemoryPressure()

                if (runningCallBack?.invoke(iteration, bestUniverse, goodUniverses) is Failed) {
                    break
                }
            }

            return goodUniverses.take(solutionAmount.toInt())
        } finally {
            cleanupAfterSolverRun()
        }
    }
}

/** 单目标多元宇宙优化器别名 / Single-objective Multi-Verse Optimizer typealias */
typealias MVO = MultiVerseOptimizer<Flt64, Flt64, Flt64>

/** 多目标多元宇宙优化器别名 / Multi-objective Multi-Verse Optimizer typealias */
typealias MulObjMVO = MultiVerseOptimizer<List<Pair<MultiObjectLocation<Flt64>, Flt64>>, List<Flt64>, Flt64>
