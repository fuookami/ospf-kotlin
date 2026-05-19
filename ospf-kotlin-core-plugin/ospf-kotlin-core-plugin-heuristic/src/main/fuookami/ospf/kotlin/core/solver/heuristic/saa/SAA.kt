@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.heuristic.saa

import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.utils.functional.Order
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

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
interface AbstractSAAPolicy<V> : AbstractHeuristicPolicy where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    val markovLength: UInt64

    fun transformSolution(
        iteration: Iteration,
        solution: Solution<V>,
        model: AbstractCallBackModelInterface<*, V>
    ): Solution<V>

    fun accept(
        iteration: Iteration,
        currentObjective: V,
        newObjective: V
    ): Boolean

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
open class SAAPolicy<V>(
    val initialTemperature: Flt64 = Flt64(100.0),
    val finalTemperature: Flt64 = Flt64(1.0),
    val temperatureGradiant: Flt64 = Flt64(0.98),
    override val markovLength: UInt64 = UInt64(100),
    step: Flt64 = Flt64(0.5),
    val disturbanceAmount: UInt64 = UInt64(1),
    val distance: (V, V) -> Flt64,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    private val converter: IntoValue<V>
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractSAAPolicy<V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
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
        ): SAAPolicy<Flt64> {
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

    override fun transformSolution(
        iteration: Iteration,
        solution: Solution<V>,
        model: AbstractCallBackModelInterface<*, V>
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

    override fun accept(iteration: Iteration, currentObjective: V, newObjective: V): Boolean {
        val delta = distance(currentObjective, newObjective)
        val acceptPercentage = (-delta / currentTemperature(iteration)).exp()
        return acceptPercentage gr randomGenerator()!!
    }

    override fun improve() {
        _step *= Flt64(0.99)
    }

    override fun finished(iteration: Iteration): Boolean {
        return super.finished(iteration) || currentTemperature(iteration) leq finalTemperature
    }

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
class SimulatedAnnealingAlgorithm<Obj, V>(
    val policy: AbstractSAAPolicy<V>
) where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(): SimulatedAnnealingAlgorithm<Flt64, Flt64> {
            return SimulatedAnnealingAlgorithm(SAAPolicy())
        }
    }

    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        runningCallBack: ((Iteration, SolutionWithFitness<V>) -> Try)? = null
    ): List<Individual<V>> {
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
            if (memoryUseOver()) {
                System.gc()
            }

            if (runningCallBack?.invoke(iteration, bestSolution) is Failed) {
                break
            }
        }

        return listOf(bestSolution)
    }
}

typealias SAA = SimulatedAnnealingAlgorithm<Flt64, Flt64>
typealias MulObjSAA = SimulatedAnnealingAlgorithm<List<Pair<MultiObjectLocation<Flt64>, Flt64>>, List<Flt64>>


