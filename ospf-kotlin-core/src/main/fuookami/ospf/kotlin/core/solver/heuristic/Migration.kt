/**
 * 种群迁移策略接口与实现
 * Population migration strategy interface and implementations
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/**
 * 迁移策略接口，定义种群间个体迁移的行为。
 * Migration strategy interface, defining behavior for individual migration between populations.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 */
interface Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>>
}

/**
 * 随机迁移策略，从每个种群中随机选择个体进行迁移。
 * Random migration strategy, randomly selecting individuals from each population for migration.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class RandomMigration<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
) : Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>> {
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

/**
 * 优到劣迁移策略，将较优种群的最佳个体迁移到较差种群。
 * Better-to-worse migration strategy, migrating best individuals from better populations to worse ones.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class BetterToWorseMigration<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
) : Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val bestFromEach = populations.map { pop ->
            pop.individuals.minMaxWithPartialThreeWayComparatorOrNull { lhs, rhs ->
                model.compareObjective(lhs.fitness, rhs.fitness)
            }?.first ?: pop.individuals.first()
        }
        return populations.mapIndexed { index, population ->
            val sourceIndex = (index - 1 + populations.size) % populations.size
            population to listOf(bestFromEach[sourceIndex])
        }
    }
}

/**
 * 多到少迁移策略，从个体较多的种群向较少的种群迁移。
 * More-to-less migration strategy, migrating from larger populations to smaller ones.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class MoreToLessMigration<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
) : Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val sorted = populations.sortedByDescending { it.individuals.size }
        val donors = sorted.first().individuals.take(maxOf(1, sorted.first().individuals.size / populations.size))
        return populations.map { population ->
            population to donors
        }
    }
}

/**
 * 标准迁移策略，按固定比例从相邻种群迁移个体。
 * Standard migration strategy, migrating individuals from adjacent populations at a fixed rate.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class MigrationMigration<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
) : Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>> {
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

/**
 * 环形交换迁移策略，按概率在相邻种群间交换个体。
 * Ring exchange migration strategy, exchanging individuals between adjacent populations with probability.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class RingExchangeMigration<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
) : Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>> {
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

/**
 * 随机扩散迁移策略，从全局随机选择个体进行扩散。
 * Random diffusion migration strategy, randomly selecting individuals globally for diffusion.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class RandomDiffusionMigration<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
) : Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>> {
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

/**
 * 精英迁移策略，从每个种群中选择精英个体进行迁移。
 * Elitist migration strategy, selecting elite individuals from each population for migration.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class ElitistMigrationMigration<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
) : Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>> {
        if (populations.size < 2) return populations.map { it to emptyList() }
        val eliteCount = maxOf(1, populations.first().individuals.size / 10)
        val elites = populations.map { pop ->
            pop.individuals
                .sortedByDescending { individual ->
                    model.compareObjective(individual.fitness, pop.best.fitness) is Order.Less
                }
                .take(eliteCount)
        }
        return populations.mapIndexed { index, population ->
            val sourceIndex = (index - 1 + populations.size) % populations.size
            population to elites[sourceIndex]
        }
    }
}

/**
 * 种群合并迁移策略，将所有种群合并后重新分配。
 * Population merge migration strategy, merging all populations and redistributing.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class PopulationMergeMigration<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
) : Migration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, ObjValue, V>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): List<Pair<Population<T, ObjValue, V>, List<T>>> {
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
