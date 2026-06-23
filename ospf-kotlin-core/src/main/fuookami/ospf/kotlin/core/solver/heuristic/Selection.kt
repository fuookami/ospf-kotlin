/**
 * 选择策略接口与实现
 * Selection strategy interface and implementations
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 选择策略接口，定义从种群中选择个体的行为。
 * Selection strategy interface, defining behavior for selecting individuals from the population.
 */
interface Selection {
    /**
     * 按权重选择一个个体索引。
     * Select one individual index by weight.
     *
     * @param iteration 当前迭代 / Current iteration
     * @param weights 权重列表 / Weight list
     * @return 选中的个体索引 / Selected individual index
     */
    operator fun invoke(
        iteration: Iteration,
        weights: List<Flt64>
    ): UInt64

    /**
     * 按权重选择多个个体索引。
     * Select multiple individual indices by weight.
     *
     * @param iteration 当前迭代 / Current iteration
     * @param weights 权重列表 / Weight list
     * @param amount 选择数量 / Selection amount
     * @return 选中的个体索引列表 / Selected individual index list
     */
    suspend operator fun invoke(
        iteration: Iteration,
        weights: List<Flt64>,
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
    private val randomGenerator: Generator<Flt64>
) : Selection {
    override operator fun invoke(
        iteration: Iteration,
        weights: List<Flt64>
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
        weights: List<Flt64>,
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
        weights: List<Flt64>
    ): UInt64 {
        return UInt64(weights.withIndex().maxBy { it.value }.index)
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<Flt64>,
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
        /**
         * 以固定参数创建锦标赛选择策略。
         * Create a tournament selection strategy with fixed parameters.
         *
         * @param eliteAmount 每组精英数量 / Elite amount per group
         * @param groupMinAmount 每组最小数量 / Minimum group size
         * @return 锦标赛选择策略实例 / Tournament selection strategy instance
         */
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
        weights: List<Flt64>
    ): UInt64 {
        return RankSelection(iteration, weights)
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<Flt64>,
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
    private val randomGenerator: Generator<Flt64>
) : Selection {
    override operator fun invoke(
        iteration: Iteration,
        weights: List<Flt64>
    ): UInt64 {
        return (randomGenerator()!! * Flt64(weights.size)).round().toUInt64()
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<Flt64>,
        amount: UInt64
    ): List<UInt64> {
        val sum = weights.sum(Flt64)
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
        /**
         * 以固定阈值创建截断选择策略。
         * Create a truncation selection strategy with a fixed threshold.
         *
         * @param truncationThreshold 截断阈值 / Truncation threshold
         * @return 截断选择策略实例 / Truncation selection strategy instance
         */
        operator fun invoke(
            truncationThreshold: Flt64
        ): TruncationSelection {
            return TruncationSelection { _ -> truncationThreshold }
        }
    }

    override fun invoke(
        iteration: Iteration,
        weights: List<Flt64>
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
        weights: List<Flt64>,
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
    private val randomGenerator: Generator<Flt64>
) : Selection {
    companion object {
        /**
         * 以温度衰减参数创建玻尔兹曼选择策略。
         * Create a Boltzmann selection strategy with temperature decay parameters.
         *
         * @param initialTemperature 初始温度 / Initial temperature
         * @param decayRate 衰减率 / Decay rate
         * @param randomGenerator 随机数生成器 / Random number generator
         * @return 玻尔兹曼选择策略实例 / Boltzmann selection strategy instance
         */
        operator fun invoke(
            initialTemperature: Flt64,
            decayRate: Flt64,
            randomGenerator: Generator<Flt64>
        ): BoltzmannSelection {
            return BoltzmannSelection(
                { iteration -> initialTemperature * (-decayRate * iteration.iteration.toFlt64()).exp() },
                randomGenerator
            )
        }
    }

    override operator fun invoke(
        iteration: Iteration,
        weights: List<Flt64>
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
        weights: List<Flt64>,
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
    /** 邻域大小函数 / Neighborhood size function */
    protected abstract val neighborhoodSize: (Iteration) -> UInt64
    /** 随机数生成器 / Random number generator */
    protected abstract val randomGenerator: Generator<Flt64>

    /** 获取邻域内的个体索引集合 / Get the set of individual indices within the neighborhood */
    protected abstract fun getNeighbours(weights: List<Flt64>, amount: UInt64): Set<UInt64>

    override fun invoke(
        iteration: Iteration,
        weights: List<Flt64>
    ): UInt64 {
        return (randomGenerator()!! * Flt64(weights.size)).round().toUInt64()
    }

    override suspend fun invoke(
        iteration: Iteration,
        weights: List<Flt64>,
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
    override val randomGenerator: Generator<Flt64>
) : LocalSelection() {
    companion object {
        /**
         * 以固定邻域大小创建环形局部选择策略。
         * Create a ring local selection strategy with a fixed neighborhood size.
         *
         * @param neighborhoodSize 邻域大小 / Neighborhood size
         * @param randomGenerator 随机数生成器 / Random number generator
         * @return 局部选择策略实例 / Local selection strategy instance
         */
        operator fun invoke(
            neighborhoodSize: UInt64,
            randomGenerator: Generator<Flt64>
        ): LocalSelection {
            return RingLocalSelection(
                { _ -> neighborhoodSize },
                randomGenerator
            )
        }
    }

    /**
     * 获取环形邻域内的个体索引集合。
     * Get the set of individual indices within the ring neighborhood.
     *
     * @param weights 权重列表 / Weight list
     * @param amount 邻域大小 / Neighborhood size
     * @return 邻域个体索引集合 / Set of neighbor individual indices
     */
    override fun getNeighbours(weights: List<Flt64>, amount: UInt64): Set<UInt64> {
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
