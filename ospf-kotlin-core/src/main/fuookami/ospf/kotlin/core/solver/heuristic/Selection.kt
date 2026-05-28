/**
 * 选择策略接口与实现
 * Selection strategy interface and implementations
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.math.functional.sum

/**
 * 选择策略接口，定义从种群中选择个体的行为。
 * Selection strategy interface, defining behavior for selecting individuals from the population.
 */
interface Selection {
    operator fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): UInt64

    suspend operator fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        amount: UInt64
    ): List<UInt64>
}

/**
 * 轮盘赌选择策略，按权重概率选择个体。
 * Roulette selection strategy, selecting individuals with probability proportional to weights.
 *
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class RouletteSelection(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>
) : Selection {
    override operator fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): UInt64 {
        val accumulation = weights.runningFold(Flt64.zero) { acc, weight -> acc + weight }
        val p = randomGenerator()!! * accumulation.last()
        val i = accumulation.indexOfFirst { p geq it }
        return if (i == -1) {
            UInt64.zero
        } else {
            UInt64(i)
        }
    }

    override suspend operator fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        amount: UInt64
    ): List<UInt64> {
        if (amount.toInt() >= weights.size) {
            return weights.indices.map { UInt64(it) }
        }

        val accumulation = weights.runningFold(Flt64.zero) { acc, weight -> acc + weight }
        return coroutineScope {
            amount.indices.map {
                async(Dispatchers.Default) {
                    val p = randomGenerator()!! * accumulation.last()
                    val i = accumulation.indexOfFirst { p geq it }
                    if (i == -1) {
                        UInt64.zero
                    } else {
                        UInt64(i)
                    }
                }
            }.awaitAll()
        }.distinct().sorted()
    }
}

/**
 * 排名选择策略，选择权重最高的个体。
 * Rank selection strategy, selecting individuals with the highest weights.
 */
data object RankSelection : Selection {
    override operator fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): UInt64 {
        return UInt64(weights.withIndex().maxBy { it.value }.index)
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        amount: UInt64
    ): List<UInt64> {
        if (amount.toInt() >= weights.size) {
            return weights.indices.map { UInt64(it) }
        }

        val sortedWeights = weights.withIndex().sortedByDescending { it.value }
        return sortedWeights
            .take(amount.toInt())
            .map { UInt64(it.index) }
    }
}

/**
 * 锦标赛选择策略，将种群分组后选择每组精英。
 * Tournament selection strategy, dividing population into groups and selecting elites from each.
 *
 * @property eliteAmount 每组精英数量函数 / Elite amount per group function
 * @property groupMinAmount 每组最小数量函数 / Minimum group size function
 */
data class TournamentSelection(
    private val eliteAmount: (Iteration) -> UInt64,
    private val groupMinAmount: (Iteration) -> UInt64
) : Selection {
    companion object {
        operator fun invoke(
            eliteAmount: UInt64,
            groupMinAmount: UInt64
        ): TournamentSelection {
            return TournamentSelection(
                { _ -> eliteAmount },
                { _ -> groupMinAmount }
            )
        }
    }

    override operator fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): UInt64 {
        return RankSelection(iteration, weights)
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        amount: UInt64
    ): List<UInt64> {
        if (amount.toInt() >= weights.size) {
            return weights.indices.map { UInt64(it) }
        }

        val thisEliteAmount = eliteAmount(iteration)
        val thisGroupMinAmount = groupMinAmount(iteration)
        val groupNumber = min(
            amount / thisEliteAmount,
            UInt64(weights.size) / thisGroupMinAmount
        )
        val actualEliteAmount = max(
            thisEliteAmount,
            amount / groupNumber
        )

        val groups = weights
            .withIndex()
            .shuffled()
            .chunked(groupNumber.toInt())

        return coroutineScope {
            groups.map { group ->
                async(Dispatchers.Default) {
                    group
                        .sortedByDescending { it.value }
                        .take(actualEliteAmount.toInt())
                        .map { UInt64(it.index) }
                }
            }.awaitAll()
        }.flatten().distinct().sorted()
    }
}

/**
 * 随机通用选择策略，使用等间距指针进行选择。
 * Stochastic universal selection strategy, using equally spaced pointers for selection.
 *
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class StochasticUniversalSelection(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>
) : Selection {
    override operator fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): UInt64 {
        return (randomGenerator()!! * Flt64(weights.size)).round().toUInt64()
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        amount: UInt64
    ): List<UInt64> {
        val sum = weights.sum()
        val step = sum / amount.toFlt64()
        val start = randomGenerator()!! * step
        val positions = amount.indices.map { start + it.toFlt64() * step }

        val accumulation = weights.runningFold(Flt64.zero) { acc, weight -> acc + weight }
        return coroutineScope {
            positions.map { p ->
                async(Dispatchers.Default) {
                    val i = accumulation.indexOfFirst { p geq it }
                    if (i == -1) {
                        UInt64.zero
                    } else {
                        UInt64(i)
                    }
                }
            }.awaitAll()
        }.distinct().sorted()
    }
}

/**
 * 截断选择策略，按阈值截断后随机选择。
 * Truncation selection strategy, truncating by threshold then randomly selecting.
 *
 * @property truncationThreshold 截断阈值函数 / Truncation threshold function
 */
data class TruncationSelection(
    private val truncationThreshold: (Iteration) -> Flt64
) : Selection {
    companion object {
        operator fun invoke(
            truncationThreshold: Flt64
        ): TruncationSelection {
            return TruncationSelection { _ -> truncationThreshold }
        }
    }

    override fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): UInt64 {
        return UInt64(
            weights
                .withIndex()
                .sortedByDescending { it.value }
                .take(
                    maxOf(
                        1,
                        (truncationThreshold(iteration) * Flt64(weights.size)).round().toUInt64().toInt()
                    )
                )
                .shuffled()
                .first()
                .index
        )
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        amount: UInt64
    ): List<UInt64> {
        return weights
            .withIndex()
            .sortedByDescending { it.value }
            .take(
                max(
                    UInt64.one,
                    amount,
                    (truncationThreshold(iteration) * Flt64(weights.size)).round().toUInt64()
                ).toInt()
            )
            .shuffled()
            .take(amount.toInt())
            .map { UInt64(it.index) }
    }
}

/**
 * 玻尔兹曼选择策略，基于温度的退火选择。
 * Boltzmann selection strategy, annealing-based selection with temperature.
 *
 * @property temperature 温度函数 / Temperature function
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class BoltzmannSelection(
    private val temperature: (Iteration) -> Flt64,
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>
) : Selection {
    companion object {
        operator fun invoke(
            initialTemperature: Flt64,
            decayRate: Flt64,
            randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        ): BoltzmannSelection {
            return BoltzmannSelection(
                { iteration -> initialTemperature * (-decayRate * iteration.iteration.toFlt64()).exp() },
                randomGenerator
            )
        }
    }

    override operator fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): UInt64 {
        val thisTemperature = temperature(iteration)
        val accumulation = weights.runningFold(Flt64.zero) { acc, weight -> acc + (weight / thisTemperature).exp() }
        val p = randomGenerator()!! * accumulation.last()
        val i = accumulation.indexOfFirst { p geq it }
        return if (i == -1) {
            UInt64.zero
        } else {
            UInt64(i)
        }
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        amount: UInt64
    ): List<UInt64> {
        if (amount.toInt() >= weights.size) {
            return weights.indices.map { UInt64(it) }
        }

        val thisTemperature = temperature(iteration)
        val accumulation = weights.runningFold(Flt64.zero) { acc, weight -> acc + (weight / thisTemperature).exp() }
        return coroutineScope {
            amount.indices.map {
                async(Dispatchers.Default) {
                    val p = randomGenerator()!! * accumulation.last()
                    val i = accumulation.indexOfFirst { p geq it }
                    if (i == -1) {
                        UInt64.zero
                    } else {
                        UInt64(i)
                    }
                }
            }.awaitAll()
        }.distinct().sorted()
    }
}

/**
 * 局部选择策略基类，基于邻域进行选择。
 * Base class for local selection strategies, selecting based on neighborhood.
 */
abstract class LocalSelection : Selection {
    protected abstract val neighborhoodSize: (Iteration) -> UInt64
    protected abstract val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>

    protected abstract fun getNeighbours(weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, amount: UInt64): Set<UInt64>

    override fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): UInt64 {
        return (randomGenerator()!! * Flt64(weights.size)).round().toUInt64()
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        amount: UInt64
    ): List<UInt64> {
        val neighbours = getNeighbours(weights, amount)
        val neighbourWeights = weights.withIndex().filter { UInt64(it.index) in neighbours }

        val accumulation = neighbourWeights.runningFold(Flt64.zero) { acc, weight -> acc + weight.value }
        return coroutineScope {
            amount.indices.map {
                async(Dispatchers.Default) {
                    val p = randomGenerator()!! * accumulation.last()
                    val i = accumulation.indexOfFirst { p geq it }
                    if (i == -1) {
                        UInt64.zero
                    } else {
                        UInt64(neighbourWeights[i].index)
                    }
                }
            }.awaitAll()
        }.distinct().sorted()
    }
}

/**
 * 环形局部选择策略，在环形邻域内进行选择。
 * Ring local selection strategy, selecting within a ring-shaped neighborhood.
 *
 * @property neighborhoodSize 邻域大小函数 / Neighborhood size function
 * @property randomGenerator 随机数生成器 / Random number generator
 */
data class RingLocalSelection(
    override val neighborhoodSize: (Iteration) -> UInt64,
    override val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>
) : LocalSelection() {
    companion object {
        operator fun invoke(
            neighborhoodSize: UInt64,
            randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        ): LocalSelection {
            return RingLocalSelection(
                { _ -> neighborhoodSize },
                randomGenerator
            )
        }
    }

    override fun getNeighbours(weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, amount: UInt64): Set<UInt64> {
        val neighbours = HashSet<UInt64>()
        val medium = (randomGenerator()!! * Flt64(weights.size)).round().toUInt64()
        for (offset in amount.indices) {
            if (offset eq UInt64.zero) {
                neighbours.add(medium)
            } else {
                if (offset gr medium) {
                    neighbours.add(
                        (offset - medium) % UInt64(weights.size)
                    )
                } else {
                    neighbours.add(
                        medium - offset
                    )
                }
                neighbours.add((medium + offset) % UInt64(weights.size))
            }
        }
        return neighbours
    }
}
