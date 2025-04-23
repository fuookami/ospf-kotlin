package fuookami.ospf.kotlin.core.backend.solver.heuristic

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.Flt64

interface Selection {
    operator fun invoke(
        iteration: Iteration,
        weights: List<Flt64>
    ): UInt64

    suspend operator fun invoke(
        iteration: Iteration,
        weights: List<Flt64>,
        amount: UInt64
    ): List<UInt64>
}

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

data class BoltzmannSelection(
    private val temperature: (Iteration) -> Flt64,
    private val randomGenerator: Generator<Flt64>
) : Selection {
    companion object {
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

abstract class LocalSelection : Selection {
    protected abstract val neighborhoodSize: (Iteration) -> UInt64
    protected abstract val randomGenerator: Generator<Flt64>

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

data class RingLocalSelection(
    override val neighborhoodSize: (Iteration) -> UInt64,
    override val randomGenerator: Generator<Flt64>
) : LocalSelection() {
    companion object {
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
