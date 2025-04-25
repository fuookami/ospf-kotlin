package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import fuookami.ospf.kotlin.core.backend.plugins.heuristic.pso.Particle
import kotlin.time.*
import kotlin.time.Duration.Companion.minutes
import kotlin.random.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.Cross
import fuookami.ospf.kotlin.utils.memoryUseOver

interface AbstractGeneAlgorithmPolicy<V> : AbstractHeuristicPolicy {
    fun <T : Individual<V>> migrate(
        iteration: Iteration,
        populations: List<AbstractPopulation<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<AbstractPopulation<T, V>>

    fun <T : Individual<V>> select(
        iteration: Iteration,
        population: AbstractPopulation<T, V>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<T>

    fun <T : Individual<V>> cross(
        iteration: Iteration,
        population: List<T>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<T>

    fun <T : Individual<V>> mutate(
        iteration: Iteration,
        population: List<T>,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<T>
}

class GeneAlgorithmPolicy<V>(
    val migration: Migration<V>,
    val selectionMode: SelectionMode<V>,
    val selection: Selection,
    val crossMode: CrossMode<V>,
    val cross: Cross<V>,
    val mutationMode: MutationMode<V>,
    val mutation: Mutation<V>,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
) : HeuristicPolicy(iterationLimit, notBetterIterationLimit, timeLimit), AbstractGeneAlgorithmPolicy<V> {
    override fun <T : Individual<V>> migrate(
        iteration: Iteration,
        populations: List<AbstractPopulation<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<AbstractPopulation<T, V>> {
        TODO("Not yet implemented")
    }

    override fun <T : Individual<V>> select(
        iteration: Iteration,
        population: AbstractPopulation<T, V>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Individual<V>> cross(
        iteration: Iteration,
        population: List<T>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Individual<V>> mutate(
        iteration: Iteration,
        population: List<T>,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<T> {
        TODO("Not yet implemented")
    }
}

class GeneAlgorithm<Obj, V>(
    val population: List<PopulationBuilder>,
    val migrationPeriod: UInt64,
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractGeneAlgorithmPolicy<V>,
) {
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        runningCallBack: ((Iteration, Chromosome<V>, List<Chromosome<V>>, List<AbstractPopulation<Chromosome<V>, V>>) -> Try)? = null
    ): List<Chromosome<V>> {
        val iteration = Iteration()
        val initialSolutions = model
            .initialSolutions(population.sumOf { it.densityRange.lowerBound.value.unwrap() })
            .map {
                Chromosome(
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
        var bestChromosome = populations
            .map { it.best }
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
            .first()
        val goodChromosomes = initialSolutions
            .sortedWithPartialThreeWayComparator { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }
            .take(solutionAmount.toInt())
            .toMutableList()

        while (!policy.finished(iteration)) {
            var globalBetter = false

            if (migrationPeriod > UInt64.zero && iteration.iteration % migrationPeriod == UInt64.zero) {
                populations = policy.migrate(iteration, populations, model)
            }

            val newPopulationAndChromosomes = coroutineScope {
                populations.map { population ->
                    async(Dispatchers.Default) {
                        val selected = policy.select(iteration, population, model)
                        val crossed = policy.cross(iteration, selected, model, population.parentAmountRange)
                        val mutated = policy.mutate(iteration, crossed, model, population.mutationRateRange)
                        val combined = (crossed + mutated + population.elites).sortedWithPartialThreeWayComparator { lhs, rhs ->
                            model.compareObjective(lhs.fitness, rhs.fitness)
                        }
                        AbstractPopulation(
                            individuals = combined,
                            elites = combined.take(population.eliteAmount.toInt()),
                            best = combined.first(),
                            eliteAmount =  population.eliteAmount,
                            densityRange = population.densityRange,
                            mutationRateRange = population.mutationRateRange,
                            parentAmountRange = population.parentAmountRange
                        ) to (crossed + mutated)
                    }
                }.awaitAll()
            }
            populations = newPopulationAndChromosomes.map { it.first }
            val newChromosomes = newPopulationAndChromosomes
                .flatMap { it.second }
                .sortedWithPartialThreeWayComparator { lhs, rhs ->
                    model.compareObjective(lhs.fitness, rhs.fitness)
                }
            val newBestChromosome = newChromosomes.first()
            refreshGoodChromosomes(goodChromosomes, newChromosomes, model)
            if (model.compareObjective(newBestChromosome.fitness, bestChromosome.fitness) is Order.Less) {
                bestChromosome = goodChromosomes.first()
                globalBetter = true
            }

            model.flush()
            iteration.next(globalBetter)
            if (memoryUseOver()) {
                System.gc()
            }

            if (runningCallBack?.invoke(iteration, bestChromosome, goodChromosomes, populations) is Failed) {
                break
            }
        }

        return goodChromosomes.take(solutionAmount.toInt())
    }

    private fun refreshGoodChromosomes(
        goodChromosomes: MutableList<Chromosome<V>>,
        newChromosomes: List<Chromosome<V>>,
        model: AbstractCallBackModelInterface<Obj, V>
    ) {
        var i = 0
        var j = 0
        while (i != goodChromosomes.size && j != newChromosomes.size) {
            if (model.compareObjective(newChromosomes[j].fitness, goodChromosomes[i].fitness) is Order.Less) {
                goodChromosomes.add(i, newChromosomes[j])
                ++i
                ++j
            } else {
                ++i
            }
        }
        if (j != newChromosomes.size) {
            goodChromosomes.addAll(
                newChromosomes.subList(
                    j,
                    minOf(newChromosomes.size, maxOf(j, solutionAmount.toInt() - goodChromosomes.size))
                )
            )
        }
    }
}

typealias GA = GeneAlgorithm<Flt64, Flt64>
typealias MulObjGA = GeneAlgorithm<MulObj, List<Flt64>>
