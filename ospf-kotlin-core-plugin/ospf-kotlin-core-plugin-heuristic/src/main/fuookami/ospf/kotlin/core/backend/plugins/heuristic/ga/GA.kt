package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import kotlin.time.*
import kotlin.time.Duration.Companion.minutes
import kotlin.random.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.Cross

interface AbstractGAPolicy<V> : AbstractHeuristicPolicy {
    suspend fun migrate(
        iteration: Iteration,
        populations: List<AbstractPopulation<V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<AbstractPopulation<V>>

    suspend fun select(
        iteration: Iteration,
        population: AbstractPopulation<V>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Chromosome<V>>

    suspend fun cross(
        iteration: Iteration,
        population: List<Chromosome<V>>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<Chromosome<V>>

    suspend fun mutate(
        iteration: Iteration,
        population: List<Chromosome<V>>,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Chromosome<V>>
}

class GAPolicy<V>(
    val migration: Migration<V>,
    val selectionMode: SelectionMode<V>,
    val selection: Selection,
    val crossMode: CrossMode<V>,
    val cross: Cross<V>,
    val mutationMode: MutationMode<V>,
    val mutation: Mutation<V>,
    val normalization: ObjectiveNormalization<V>,
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
) : HeuristicPolicy(iterationLimit, notBetterIterationLimit, timeLimit), AbstractGAPolicy<V> {
    override suspend fun migrate(
        iteration: Iteration,
        populations: List<AbstractPopulation<V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<AbstractPopulation<V>> {
        return migration(iteration, populations, model)
            .map { (population, newIndividuals) ->
                val individuals = (population.individuals + newIndividuals)
                        .sortedWithPartialThreeWayComparator { lhs, rhs ->
                            model.compareObjective(lhs.fitness, rhs.fitness)
                        }
                AbstractPopulation(
                    individuals = individuals,
                    elites = individuals.take(population.eliteAmount.toInt()),
                    best = individuals.first(),
                    eliteAmount =  population.eliteAmount,
                    densityRange = population.densityRange,
                    mutationRateRange = population.mutationRateRange,
                    parentAmountRange = population.parentAmountRange
                )
            }
    }

    override suspend fun select(
        iteration: Iteration,
        population: AbstractPopulation<V>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Chromosome<V>> {
        val amount = selectionMode(iteration, population, model)
        val weights = normalization(model, population.individuals.map { it.fitness })
        val indexes = selection(iteration, weights, amount)
        return population.individuals.mapIndexedNotNull { index, individual ->
            if (UInt64(index) in indexes) {
                individual
            } else {
                null
            }
        }
    }

    override suspend fun cross(
        iteration: Iteration,
        population: List<Chromosome<V>>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<Chromosome<V>> {
        val weights = normalization(model, population.map { it.fitness })
        val parentGroups = crossMode(iteration, population, weights, model, parentAmountRange)
        return coroutineScope {
            parentGroups.map { parents ->
                async(Dispatchers.Default) {
                    cross(iteration, parents, model).map { newIndividual ->
                        val fixIndividual = newIndividual.mapIndexed { i, value ->
                            coerceIn(
                                iteration = iteration,
                                index = i,
                                value = value,
                                model = model
                            )
                        }
                        Chromosome(
                            solution = fixIndividual,
                            fitness = model.objective(fixIndividual).ifNull { model.defaultObjective }
                        )
                    }
                }
            }.awaitAll()
        }.flatten()
    }

    override suspend fun mutate(
        iteration: Iteration,
        population: List<Chromosome<V>>,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Chromosome<V>> {
        val weights = normalization(model, population.map { it.fitness })
        val mutationRate = mutationMode(iteration, population, weights, model, mutationRateRange)
        return coroutineScope {
            population.mapIndexed { i, individual ->
                async(Dispatchers.Default) {
                    if (randomGenerator()!! geq mutationRate[i]) {
                        val newIndividual = mutation(iteration, individual, model, mutationRate[i])
                        val fixIndividual = newIndividual.mapIndexed { i, value ->
                            coerceIn(
                                iteration = iteration,
                                index = i,
                                value = value,
                                model = model
                            )
                        }
                        Chromosome(
                            solution = fixIndividual,
                            fitness = model.objective(fixIndividual).ifNull { model.defaultObjective }
                        )
                    } else {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }
}

class GeneAlgorithm<Obj, V>(
    val population: List<PopulationBuilder>,
    val migrationPeriod: UInt64,
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractGAPolicy<V>,
) {
    suspend operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, V>,
        runningCallBack: ((Iteration, Chromosome<V>, List<Chromosome<V>>, List<AbstractPopulation<V>>) -> Try)? = null
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
                        val combined = (crossed + mutated + population.elites)
                            .sortedWithPartialThreeWayComparator { lhs, rhs ->
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
            refreshGoodIndividuals(
                goodIndividuals = goodChromosomes,
                newIndividuals = newChromosomes,
                model = model,
                solutionAmount = solutionAmount
            )
            if (model.compareObjective(newBestChromosome.fitness, bestChromosome.fitness) is Order.Less) {
                bestChromosome = newBestChromosome
                globalBetter = true
            }

            model.flush()
            policy.update(
                iteration = iteration,
                better = globalBetter,
                bestIndividual = bestChromosome,
                goodIndividuals = goodChromosomes,
                populations = populations.map { it.individuals },
                model = model
            )
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
}

typealias GA = GeneAlgorithm<Flt64, Flt64>
typealias MulObjGA = GeneAlgorithm<MulObj, List<Flt64>>
