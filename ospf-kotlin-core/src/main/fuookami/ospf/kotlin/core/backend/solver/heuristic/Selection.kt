package fuookami.ospf.kotlin.core.backend.solver.heuristic

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*

sealed interface Selection {
    operator fun invoke(weights: List<Flt64>): UInt64
    suspend operator fun invoke(weights: List<Flt64>, amount: UInt64): List<UInt64>

    data class RouletteSelection(
        private val randomGenerator: Generator<Flt64>
    ) : Selection {
        override operator fun invoke(weights: List<Flt64>): UInt64 {
            val accumulation = weights.runningFold(Flt64.zero) { acc, weight -> acc + weight }
            val p = randomGenerator()!! * accumulation.last()
            val i = accumulation.indexOfFirst { p geq it }
            return if (i == -1) {
                UInt64.zero
            } else {
                UInt64(i)
            }
        }

        override suspend operator fun invoke(weights: List<Flt64>, amount: UInt64): List<UInt64> {
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
        override operator fun invoke(weights: List<Flt64>): UInt64 {
            return UInt64(weights.withIndex().maxBy { it.value }.index)
        }

        override suspend fun invoke(weights: List<Flt64>, amount: UInt64): List<UInt64> {
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
        val eliteAmount: UInt64,
        val groupMinAmount: UInt64
    ) : Selection {
        override operator fun invoke(weights: List<Flt64>): UInt64 {
            return RankSelection(weights)
        }

        override suspend fun invoke(weights: List<Flt64>, amount: UInt64): List<UInt64> {
            if (amount.toInt() >= weights.size) {
                return weights.indices.map { UInt64(it) }
            }

            val groupNumber = min(
                amount / eliteAmount,
                UInt64(weights.size) / groupMinAmount
            )
            val actualEliteAmount = max(
                eliteAmount,
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
}
