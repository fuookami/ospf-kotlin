@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.heuristic.gwo

import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.model.basic.MulObj
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.functional.sumOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

interface AbstractGWOPolicy<V> : AbstractHeuristicPolicy where V : RealNumber<V>, V : NumberField<V> {
    fun a(iteration: Iteration): List<Flt64>

    fun perturb(
        iteration: Iteration,
        leaders: List<Wolf<V>>,
        a: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Wolf<V>>

    fun move(
        iteration: Iteration,
        wolf: Wolf<V>,
        leaders: List<Wolf<V>>,
        a: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>
    ): Wolf<V>
}

class GWOPolicy<V>(
    val minA: Flt64 = Flt64(0.02),
    val maxA: Flt64 = Flt64(2.2),
    val b: Flt64 = Flt64(1.0),
    val growthRateAlpha: Flt64 = Flt64.two,
    val growthRateBeta: Flt64 = Flt64.three,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    // Safe when V=Flt64 (used by GWO/MulObjGWO typealiases); non-Flt64 callers must provide explicit converter
    private val converter: IntoValue<V> = @Suppress("UNCHECKED_CAST") (IntoValue.Flt64 as IntoValue<V>)
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractGWOPolicy<V> where V : RealNumber<V>, V : NumberField<V> {
    override fun a(iteration: Iteration): List<Flt64> {
        val iterationCoefficient = min(
            ((iteration.iteration.toFlt64() * Flt64.pi)
                    / (Flt64.two * iterationLimit.toFlt64())).exp(),
            ((Flt64(iteration.time.toDouble(DurationUnit.SECONDS)) * Flt64.pi)
                    / (Flt64.two * Flt64(timeLimit.toDouble(DurationUnit.SECONDS)))).exp()
        )
        val alphaA = (maxA * iterationCoefficient).pow(growthRateAlpha * (minA / maxA).lg2()!!).toFlt64()
        val betaA = (maxA * iterationCoefficient).pow(growthRateBeta * (minA / maxA).lg2()!!).toFlt64()
        return listOf(alphaA, betaA, (alphaA + betaA) / Flt64.two)
    }

    override fun perturb(
        iteration: Iteration,
        leaders: List<Wolf<V>>,
        a: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Wolf<V>> {
        fun Wolf<V>.randomWalk(): Wolf<V> {
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

    override fun move(
        iteration: Iteration,
        wolf: Wolf<V>,
        leaders: List<Wolf<V>>,
        a: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>
    ): Wolf<V> {
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
            converter.intoValue(coerceIn(
                iteration = iteration,
                index = i,
                value = newPosition,
                model = model
            ))
        }
        return Wolf(
            newSolution,
            model.objective(newSolution).ifNull { model.defaultObjective }
        )
    }
}

@OptIn(ExperimentalTime::class)
class GreyWolfOptimizer<Obj, V>(
    val population: List<PopulationBuilder>,
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractGWOPolicy<V>,
) where V : RealNumber<V>, V : NumberField<V> {
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        runningCallBack: ((Iteration, Wolf<V>, List<Wolf<V>>, List<AbstractPopulation<V>>) -> Try)? = null
    ): List<Wolf<V>> {
        val iteration = Iteration()
        val initialSolutions = model
            .initialSolutions(population.sumOf { it.densityRange.lowerBound.value.unwrap() })
            .map {
                Wolf(
                    solution = it,
                    fitness = model.objective(it).ifNull { model.defaultObjective }
                )
            }
        var populations = population.mapIndexed { i, thisPopulation ->
            val fromIndex = population.take(i).sumOf { it.densityRange.lowerBound.value.unwrap() }
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
            if (memoryUseOver()) {
                System.gc()
            }

            if (runningCallBack?.invoke(iteration, bestWolf, goodWolfs, populations) is Failed) {
                break
            }
        }

        return goodWolfs.take(solutionAmount.toInt())
    }
}

typealias GWO = GreyWolfOptimizer<Flt64, Flt64>
typealias MulObjGWO = GreyWolfOptimizer<MulObj, List<Flt64>>


