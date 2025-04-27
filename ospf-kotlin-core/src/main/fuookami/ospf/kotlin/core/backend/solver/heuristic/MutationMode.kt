package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface MutationMode<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Flt64>
}

data class StaticMutationMode<V>(
    val mutationRate: Flt64? = null
) : MutationMode<V> {
    override fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Flt64> {
        return List(population.size) {
            mutationRate?.coerceIn(mutationRateRange) ?: mutationRateRange.upperBound.value.unwrap()
        }
    }
}

data class RandomMutationMode<V>(
    private val randomGenerator: Generator<Flt64>
) : MutationMode<V> {
    override fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Flt64> {
        return List(population.size) {
            mutationRateRange.lowerBound.value.unwrap() + randomGenerator()!! * mutationRateRange.diff.unwrap()
        }
    }
}

class AdaptiveDynamicMutationMode<V> : MutationMode<V> {
    override fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRateRange: ValueRange<Flt64>
    ): List<Flt64> {
        val weight = (weights.max() - weights.min()) / weights.max()
        return List(population.size) {
            mutationRateRange.lowerBound.value.unwrap() + weight * mutationRateRange.diff.unwrap()
        }
    }
}
