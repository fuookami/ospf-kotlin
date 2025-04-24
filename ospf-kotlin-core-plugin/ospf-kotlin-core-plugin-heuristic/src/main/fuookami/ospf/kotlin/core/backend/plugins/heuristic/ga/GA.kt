package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import kotlin.time.*
import kotlin.time.Duration.Companion.minutes
import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

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
    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        runningCallBack: ((Iteration, Chromosome<V>, List<AbstractPopulation<Chromosome<V>, V>>) -> Try)? = null
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
        val populations = population.mapIndexed { i, thisPopulation ->
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

        while (!policy.finished(iteration)) {
            TODO("not implemented yet")
        }

        return goodChromosomes.take(solutionAmount.toInt())
    }
}

typealias GA = GeneAlgorithm<Flt64, Flt64>
typealias MulObjGA = GeneAlgorithm<MulObj, List<Flt64>>
