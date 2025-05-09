package fuookami.ospf.kotlin.core.backend.plugins.heuristic.gwo

import kotlin.time.*
import kotlin.random.*
import kotlin.collections.mapIndexed
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

interface AbstractGWOPolicy<V> : AbstractHeuristicPolicy {
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
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
) : HeuristicPolicy(iterationLimit, notBetterIterationLimit, timeLimit), AbstractGWOPolicy<V> {
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
                coerceIn(
                    iteration = iteration,
                    index = i,
                    value = position + (randomGenerator()!! * Flt64.two - Flt64.one) * a[i],
                    model
                )
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
            val a1 = a[0] * (Flt64.two * randomGenerator()!! - Flt64.one)
            val a2 = a[1] * (Flt64.two * randomGenerator()!! - Flt64.one)
            val a3 = a[2] * (Flt64.two * randomGenerator()!! - Flt64.one)
            val c1 = Flt64.two * randomGenerator()!!
            val c2 = Flt64.two * randomGenerator()!!
            val c3 = Flt64.two * randomGenerator()!!
            val x1 = leaders.alpha.solution[i] - a1 * (c1 * leaders.alpha.solution[i] - position).abs()
            val x2 = leaders.beta.solution[i] - a2 * (c2 * leaders.beta.solution[i] - position).abs()
            val x3 = leaders.delta.solution[i] - a3 * (c3 * leaders.delta.solution[i] - position).abs()
            val newPosition = (x1 + x2 + x3) / Flt64.three
            coerceIn(
                iteration = iteration,
                index = i,
                value = newPosition,
                model = model
            )
        }
        return Wolf(
            newSolution,
            model.objective(newSolution).ifNull { model.defaultObjective }
        )
    }
}

class GreyWolfOptimizer<Obj, V>(
    val population: List<PopulationBuilder>,
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractGWOPolicy<V>,
) {
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
                eliteAmount =  thisPopulation.eliteAmount,
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
                                    population.alpha,
                                    population.beta,
                                    population.delta
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
                            eliteAmount =  population.eliteAmount,
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
            refreshGoodIndividuals(goodWolfs, newWolfs, model, solutionAmount)
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
