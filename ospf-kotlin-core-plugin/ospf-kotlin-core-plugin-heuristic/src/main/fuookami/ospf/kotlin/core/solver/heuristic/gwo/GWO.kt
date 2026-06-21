/** 灰狼优化器实现 / Grey Wolf Optimizer implementation */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.heuristic.gwo

import kotlin.random.Random
import kotlin.time.*
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
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

/** 灰狼优化器策略接口 / Grey Wolf Optimizer policy interface */
interface AbstractGWOPolicy<ObjValue, V> : AbstractHeuristicPolicy where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 计算收敛系数 a / Calculate convergence coefficient a
     *
     * @param iteration 当前迭代 / current iteration
     * @return 收敛系数列表（alpha, beta, delta）/ convergence coefficient list (alpha, beta, delta)
     */
    fun a(iteration: Iteration): List<Flt64>

    /**
     * 扰动领导狼 / Perturb leader wolves
     *
     * @param iteration 当前迭代 / current iteration
     * @param leaders 领导狼列表 / leader wolf list
     * @param a 收敛系数 / convergence coefficients
     * @param model 回调模型 / callback model
     * @return 扰动后的领导狼列表 / perturbed leader wolf list
     */
    fun perturb(
        iteration: Iteration,
        leaders: List<Wolf<ObjValue, V>>,
        a: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Wolf<ObjValue, V>>

    /**
     * 移动狼 / Move wolf
     *
     * @param iteration 当前迭代 / current iteration
     * @param wolf 当前狼 / current wolf
     * @param leaders 领导狼列表 / leader wolf list
     * @param a 收敛系数 / convergence coefficients
     * @param model 回调模型 / callback model
     * @return 移动后的狼 / moved wolf
     */
    fun move(
        iteration: Iteration,
        wolf: Wolf<ObjValue, V>,
        leaders: List<Wolf<ObjValue, V>>,
        a: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Wolf<ObjValue, V>
}

/**
 * 灰狼优化器策略
 *
 * 实现灰狼优化器的收敛系数计算、领导狼扰动和狼群移动操作，
 * 使用 alpha/beta/delta 三级领导层次引导搜索。
 *
 * Grey Wolf Optimizer policy
 *
 * Implements convergence coefficient calculation, leader wolf perturbation, and wolf movement operations,
 * using alpha/beta/delta three-level leadership hierarchy to guide the search.
 *
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property minA 最小收敛系数 / minimum convergence coefficient
 * @property maxA 最大收敛系数 / maximum convergence coefficient
 * @property b 随机游走参数 / random walk parameter
 * @property growthRateAlpha alpha 增长率 / alpha growth rate
 * @property growthRateBeta beta 增长率 / beta growth rate
 * @property randomGenerator 随机数生成器 / random number generator
 */
class GWOPolicy<ObjValue, V>(
    val minA: Flt64 = Flt64(0.02),
    val maxA: Flt64 = Flt64(2.2),
    val b: Flt64 = Flt64(1.0),
    val growthRateAlpha: Flt64 = Flt64.two,
    val growthRateBeta: Flt64 = Flt64.three,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    // converter must be provided explicitly; use GWOPolicy.Flt64 companion for V=Flt64 convenience / 转换器必须显式提供；V=Flt64 时可使用 GWOPolicy.Flt64 伴生对象
    private val converter: IntoValue<V>
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractGWOPolicy<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun invoke(
            minA: Flt64 = Flt64(0.02),
            maxA: Flt64 = Flt64(2.2),
            b: Flt64 = Flt64(1.0),
            growthRateAlpha: Flt64 = Flt64.two,
            growthRateBeta: Flt64 = Flt64.three,
            iterationLimit: UInt64 = UInt64.maximum,
            notBetterIterationLimit: UInt64 = UInt64.maximum,
            timeLimit: Duration = 30.minutes,
            randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
        ): GWOPolicy<Flt64, Flt64> {
            return GWOPolicy(
                minA = minA,
                maxA = maxA,
                b = b,
                growthRateAlpha = growthRateAlpha,
                growthRateBeta = growthRateBeta,
                iterationLimit = iterationLimit,
                notBetterIterationLimit = notBetterIterationLimit,
                timeLimit = timeLimit,
                randomGenerator = randomGenerator,
                converter = flt64Converter
            )
        }
    }

    /** 计算收敛系数 a / Calculate convergence coefficient a */
    override fun a(iteration: Iteration): List<Flt64> {
        val iterationCoefficient = min(
            ((iteration.iteration.toFlt64() * Flt64.pi)
                    / (Flt64.two * iterationLimit.toFlt64())).exp(),
            ((Flt64(iteration.time.toDouble(DurationUnit.SECONDS)) * Flt64.pi)
                    / (Flt64.two * Flt64(timeLimit.toDouble(DurationUnit.SECONDS)))).exp()
        )
        val alphaA = (maxA * iterationCoefficient).pow(growthRateAlpha * (minA / maxA).lg2()!!)!!.toFlt64()
        val betaA = (maxA * iterationCoefficient).pow(growthRateBeta * (minA / maxA).lg2()!!)!!.toFlt64()
        return listOf(alphaA, betaA, (alphaA + betaA) / Flt64.two)
    }

    /** 扰动领导狼 / Perturb leader wolves */
    override fun perturb(
        iteration: Iteration,
        leaders: List<Wolf<ObjValue, V>>,
        a: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Wolf<ObjValue, V>> {
        fun Wolf<ObjValue, V>.randomWalk(): Wolf<ObjValue, V> {
            val newSolution = solution.mapIndexed { i, position ->
                val posFlt64 = converter.fromValue(position)
                val newPosFlt64 = coerceIn(
                    iteration = iteration,
                    index = i,
                    value = posFlt64 + (randomGenerator()!! * Flt64.two - Flt64.one) * a[i],
                    model
                )
                converter.intoValue(newPosFlt64)
            }
            return Wolf(
                newSolution,
                model.objective(newSolution).ifNull { model.defaultObjective }
            )
        }

        return listOf(
            leaders.alpha.randomWalk(),
            leaders.beta.randomWalk(),
            leaders.delta.randomWalk()
        )
    }

    /** 移动狼 / Move wolf */
    override fun move(
        iteration: Iteration,
        wolf: Wolf<ObjValue, V>,
        leaders: List<Wolf<ObjValue, V>>,
        a: List<Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Wolf<ObjValue, V> {
        val newSolution = wolf.solution.mapIndexed { i, position ->
            val posFlt64 = converter.fromValue(position)
            val alphaFlt64 = converter.fromValue(leaders.alpha.solution[i])
            val betaFlt64 = converter.fromValue(leaders.beta.solution[i])
            val deltaFlt64 = converter.fromValue(leaders.delta.solution[i])
            val a1 = a[0] * (Flt64.two * randomGenerator()!! - Flt64.one)
            val a2 = a[1] * (Flt64.two * randomGenerator()!! - Flt64.one)
            val a3 = a[2] * (Flt64.two * randomGenerator()!! - Flt64.one)
            val c1 = Flt64.two * randomGenerator()!!
            val c2 = Flt64.two * randomGenerator()!!
            val c3 = Flt64.two * randomGenerator()!!
            val x1 = alphaFlt64 - a1 * (c1 * alphaFlt64 - posFlt64).abs()
            val x2 = betaFlt64 - a2 * (c2 * betaFlt64 - posFlt64).abs()
            val x3 = deltaFlt64 - a3 * (c3 * deltaFlt64 - posFlt64).abs()
            val newPosition = (x1 + x2 + x3) / Flt64.three
            converter.intoValue(
                coerceIn(
                    iteration = iteration,
                    index = i,
                    value = newPosition,
                    model = model
                )
            )
        }
        return Wolf(
            newSolution,
            model.objective(newSolution).ifNull { model.defaultObjective }
        )
    }
}

@OptIn(ExperimentalTime::class)
/**
 * 灰狼优化器
 *
 * 实现基于灰狼群体的优化算法，使用 alpha/beta/delta 领导层次和随机游走机制进行全局搜索。
 *
 * Grey Wolf Optimizer
 *
 * Implements wolf-pack-based optimization algorithm, using alpha/beta/delta leadership hierarchy
 * and random walk mechanism for global search.
 *
 * @param Obj 目标类型 / objective type
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property population 种群构建参数列表 / population builder list
 * @property solutionAmount 期望解的数量 / desired number of solutions
 * @property policy 灰狼优化器策略 / GWO policy
 */
class GreyWolfOptimizer<Obj, ObjValue, V>(
    val population: List<PopulationBuilder>,
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractGWOPolicy<ObjValue, V>,
) where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 执行灰狼优化算法 / Execute Grey Wolf Optimizer
     *
     * @param model 回调模型 / callback model
     * @param runningCallBack 运行回调函数 / running callback function
     * @return 最优狼列表 / best wolf list
     */
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, ObjValue, V>,
        runningCallBack: ((Iteration, Wolf<ObjValue, V>, List<Wolf<ObjValue, V>>, List<AbstractPopulation<ObjValue, V>>) -> Try)? = null
    ): List<Wolf<ObjValue, V>> {
        val iteration = Iteration()
        val initialSolutions = model
            .initialSolutions(population.sumOf(UInt64) { it.densityRange.lowerBound.value.unwrap() })
            .map {
                Wolf(
                    solution = it,
                    fitness = model.objective(it).ifNull { model.defaultObjective }
                )
            }
        var populations = population.mapIndexed { i, thisPopulation ->
            val fromIndex = population.take(i).sumOf(UInt64) { it.densityRange.lowerBound.value.unwrap() }
            val toIndex = fromIndex + thisPopulation.densityRange.upperBound.value.unwrap()
            val thisIndividuals = initialSolutions
                .subList(fromIndex.toInt(), toIndex.toInt())
                .sortedWithPartialThreeWayComparator { lhs, rhs ->
                    model.compareObjective(lhs.fitness, rhs.fitness)
                }
            AbstractPopulation(
                individuals = thisIndividuals,
                elites = thisIndividuals.take(thisPopulation.eliteAmount.toInt()),
                best = thisIndividuals.first(),
                eliteAmount = thisPopulation.eliteAmount,
                densityRange = thisPopulation.densityRange,
                mutationRateRange = thisPopulation.mutationRateRange,
                parentAmountRange = thisPopulation.parentAmountRange
            )
        }
        var bestWolf = populations
            .map { it.best }
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
            .first()
        val goodWolfs = initialSolutions
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
            .take(solutionAmount.toInt())
            .toMutableList()

        try {
            while (!policy.finished(iteration)) {
                var globalBetter = false

                val a = policy.a(iteration)
                val newPopulationAndWolfs = coroutineScope {
                    populations.map { population ->
                        async(Dispatchers.Default) {
                            val movedWolfs = population.individuals.map { wolf ->
                                policy.move(
                                    iteration = iteration,
                                    wolf = wolf,
                                    leaders = listOf(
                                        population.alpha(),
                                        population.beta(),
                                        population.delta()
                                    ),
                                    a = a,
                                    model = model
                                )
                            }.sortedWithPartialThreeWayComparator { lhs, rhs ->
                                model.compareObjective(lhs.fitness, rhs.fitness)
                            }
                            val movedLeaders = movedWolfs.take(3)
                            val perturbedLeaders = policy.perturb(
                                iteration = iteration,
                                leaders = movedLeaders,
                                a = a,
                                model = model
                            )
                            val wolfs = (movedWolfs.subList(3, movedWolfs.size) + perturbedLeaders)
                                .sortedWithPartialThreeWayComparator { lhs, rhs ->
                                    model.compareObjective(lhs.fitness, rhs.fitness)
                                }
                            AbstractPopulation(
                                individuals = wolfs,
                                elites = wolfs.take(population.eliteAmount.toInt()),
                                best = wolfs.first(),
                                eliteAmount = population.eliteAmount,
                                densityRange = population.densityRange,
                                mutationRateRange = population.mutationRateRange,
                                parentAmountRange = population.parentAmountRange
                            ) to (movedWolfs + perturbedLeaders)
                        }
                    }.awaitAll()
                }
                populations = newPopulationAndWolfs.map { it.first }
                val newWolfs = newPopulationAndWolfs
                    .flatMap { it.second }
                    .sortedWithPartialThreeWayComparator { lhs, rhs ->
                        model.compareObjective(lhs.fitness, rhs.fitness)
                    }
                val newBestWolf = newWolfs.first()
                refreshGoodIndividuals(
                    goodIndividuals = goodWolfs,
                    newIndividuals = newWolfs,
                    model = model,
                    solutionAmount = solutionAmount
                )
                if (model.compareObjective(newBestWolf.fitness, bestWolf.fitness) is Order.Less) {
                    bestWolf = newBestWolf
                    globalBetter = true
                }

                model.flush()
                policy.update(
                    iteration = iteration,
                    better = globalBetter,
                    bestIndividual = bestWolf,
                    goodIndividuals = goodWolfs,
                    populations = populations.map { it.individuals },
                    model = model
                )
                iteration.next(globalBetter)
                cleanupOnSolverMemoryPressure()

                if (runningCallBack?.invoke(iteration, bestWolf, goodWolfs, populations) is Failed) {
                    break
                }
            }

            return goodWolfs.take(solutionAmount.toInt())
        } finally {
            cleanupAfterSolverRun()
        }
    }
}

/** 单目标灰狼优化器类型 / Single-objective Grey Wolf Optimizer type */
typealias GWO = GreyWolfOptimizer<Flt64, Flt64, Flt64>
/** 多目标灰狼优化器类型 / Multi-objective Grey Wolf Optimizer type */
typealias MulObjGWO = GreyWolfOptimizer<List<Pair<MultiObjectLocation<Flt64>, Flt64>>, List<Flt64>, Flt64>
