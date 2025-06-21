package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface CrossMode<V> {
    enum class Method {
        WeightedRing,
        WeightedBidirectional,
        RandomRing,
        RandomBidirectional
    }

    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>>
}

class OneParentCrossMode<V> : CrossMode<V> {
    override fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>> {
        return population.map { listOf(it) }
    }
}

class TwoParentCrossMode<V>(
    val method: CrossMode.Method
) : CrossMode<V> {
    override fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>> {
        return when (method) {
            CrossMode.Method.WeightedRing -> {
                val weighted = population.withIndex().sortedByDescending { weights[it.index] }
                population.indices.map { i ->
                    weighted
                        .subList(i, i + 2)
                        .map { it.value }
                } + listOf(listOf(weighted.last().value, weighted.first().value))
            }

            CrossMode.Method.WeightedBidirectional -> {
                val weighted = population.withIndex().sortedByDescending { weights[it.index] }
                (0 until (population.size / 2 + 1)).mapNotNull { i ->
                    val j = population.lastIndex - i
                    if (i <= j) {
                        listOf(weighted[i].value, weighted[j].value)
                    } else {
                        null
                    }
                }
            }

            CrossMode.Method.RandomRing -> {
                val shuffled = population.shuffled()
                population.indices.map {
                    shuffled.subList(it, it + 2)
                } + listOf(listOf(shuffled.last(), shuffled.first()))
            }

            CrossMode.Method.RandomBidirectional -> {
                val shuffled = population.shuffled()
                (0 until (population.size / 2 + 1)).mapNotNull { i ->
                    val j = population.lastIndex - i
                    if (i <= j) {
                        listOf(shuffled[i], shuffled[j])
                    } else {
                        null
                    }
                }
            }
        }
    }
}

class MultiParentCrossMode<V>(
    val method: CrossMode.Method,
    val parentAmountCalculator: (Iteration, List<Flt64>, ValueRange<UInt64>) -> UInt64
) : CrossMode<V> {
    companion object {
        operator fun <V> invoke(
            method: CrossMode.Method,
            randomGenerator: Generator<Flt64>
        ): MultiParentCrossMode<V> {
            return MultiParentCrossMode(method) { _, _, range ->
                range.fixedValue
                    ?: (range.lowerBound.value.unwrap() + (randomGenerator()!! * range.diff.unwrap().toFlt64()).round().toUInt64())
            }
        }
    }

    override fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>> {
        val parentAmount = parentAmountCalculator(iteration, weights, parentAmountRange)
        return if (parentAmount == UInt64.zero) {
            OneParentCrossMode<V>()(iteration, population, weights, model, parentAmountRange)
        } else  {
            when (method) {
                CrossMode.Method.WeightedRing -> {
                    val weighted = population.withIndex().sortedByDescending { weights[it.index] }
                    population.indices.map { i ->
                        parentAmount.indices.map { j ->
                            weighted[(i + j.toInt()) % weighted.size].value
                        }
                    }
                }

                CrossMode.Method.WeightedBidirectional -> {
                    val weighted = population.withIndex().sortedByDescending { weights[it.index] }
                    (0 until (population.size / 2 + 1)).mapNotNull { i0 ->
                        val forwardAmount = if (parentAmount % UInt64.two == UInt64.zero) {
                            parentAmount / UInt64.two
                        } else {
                            parentAmount / UInt64.two + UInt64.one
                        }
                        val backwardAmount = parentAmount - forwardAmount
                        val i1 = i0 + forwardAmount.toInt()
                        val j0 = population.size - i0 - backwardAmount.toInt()
                        val j1 = population.size - i0
                        if (i1 <= j0) {
                            (weighted.subList(i0, i1) + weighted.subList(j0, j1)).map {
                                it.value
                            }
                        } else {
                            null
                        }
                    }
                }

                CrossMode.Method.RandomRing -> {
                    val shuffled = population.shuffled()
                    population.indices.map { i ->
                        parentAmount.indices.map { j ->
                            shuffled[(i + j.toInt()) % shuffled.size]
                        }
                    }
                }

                CrossMode.Method.RandomBidirectional -> {
                    val shuffled = population.shuffled()
                    (0 until (population.size / 2 + 1)).mapNotNull { i0 ->
                        val forwardAmount = if (parentAmount % UInt64.two == UInt64.zero) {
                            parentAmount / UInt64.two
                        } else {
                            parentAmount / UInt64.two + UInt64.one
                        }
                        val backwardAmount = parentAmount - forwardAmount
                        val i1 = i0 + forwardAmount.toInt()
                        val j0 = population.size - i0 - backwardAmount.toInt()
                        val j1 = population.size - i0
                        if (i1 <= j0) {
                            (shuffled.subList(i0, i1) + shuffled.subList(j0, j1))
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }
}

data object AdaptiveMultiParentCrossMode {
    operator fun <V> invoke(
        method: CrossMode.Method
    ): MultiParentCrossMode<V> {
        return MultiParentCrossMode(method) { _, weights, range ->
            if (range.fixedValue != null) {
                return@MultiParentCrossMode range.fixedValue!!
            }

            val weight = (weights.max() - weights.min()) / weights.max()
            (range.lowerBound.value.unwrap() + (weight * range.diff.unwrap().toFlt64()).round().toUInt64())
        }
    }
}
