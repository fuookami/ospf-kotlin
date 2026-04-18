package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.minMaxWithPartialThreeWayComparatorOrNull
import fuookami.ospf.kotlin.math.algebra.number.Flt64

interface Migration<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>>
}

data class RandomMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val migrants = populations.map { population ->
            val migrateCount = maxOf(1, (Flt64(population.individuals.size) * Flt64(0.1)).round().toUInt64().toInt())
            population.individuals.shuffled().take(migrateCount)
        }
        return populations.mapIndexed { index, population ->
            val sourceIndex = (index - 1 + populations.size) % populations.size
            val incoming = migrants[sourceIndex]
            population to incoming
        }
    }
}

data class BetterToWorseMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val bestFromEach = populations.map { pop ->
            pop.individuals.minMaxWithPartialThreeWayComparatorOrNull { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }?.second ?: pop.individuals.first()
        }
        return populations.mapIndexed { index, population ->
            val sourceIndex = (index - 1 + populations.size) % populations.size
            population to listOf(bestFromEach[sourceIndex])
        }
    }
}

data class MoreToLessMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val sorted = populations.sortedByDescending { it.individuals.size }
        val donors = sorted.first().individuals.take(maxOf(1, sorted.first().individuals.size / populations.size))
        return populations.map { population ->
            population to donors
        }
    }
}

data class MigrationMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val migrationRate = Flt64(0.1)
        val migrants = populations.map { population ->
            val migrateCount = maxOf(1, (Flt64(population.individuals.size) * migrationRate).round().toUInt64().toInt())
            population.individuals.shuffled().take(migrateCount)
        }
        return populations.mapIndexed { index, population ->
            val sourceIndex = (index - 1 + populations.size) % populations.size
            population to migrants[sourceIndex]
        }
    }
}

data class RingExchangeMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val exchangeProb = Flt64(0.5)
        return populations.mapIndexed { index, population ->
            val neighborIndex = (index + 1) % populations.size
            val toExchange = population.individuals.filter { (randomGenerator()!!) ls exchangeProb }
            val incoming = populations[neighborIndex].individuals.take(toExchange.size)
            population to incoming
        }
    }
}

data class RandomDiffusionMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val diffusionRate = Flt64(0.1)
        val allIndividuals = populations.flatMap { it.individuals }.shuffled()
        return populations.map { population ->
            val replaceCount = maxOf(1, (Flt64(population.individuals.size) * diffusionRate).round().toUInt64().toInt())
            val incoming = allIndividuals.take(replaceCount)
            population to incoming
        }
    }
}

data class ElitistMigrationMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val eliteCount = maxOf(1, populations.first().individuals.size / 10)
        val elites = populations.map { pop ->
            pop.individuals
                .sortedByDescending { individual ->
                    model.compareObjective(individual.fitness, pop.best.fitness) is Order.Greater
                }
                .take(eliteCount)
        }
        return populations.mapIndexed { index, population ->
            val sourceIndex = (index - 1 + populations.size) % populations.size
            population to elites[sourceIndex]
        }
    }
}

data class PopulationMergeMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val merged = populations.flatMap { it.individuals }.shuffled()
        val chunkSize = maxOf(1, merged.size / populations.size)
        return populations.mapIndexed { index, _ ->
            val start = index * chunkSize
            val end = minOf(start + chunkSize, merged.size)
            populations[index] to merged.subList(start, end)
        }
    }
}
