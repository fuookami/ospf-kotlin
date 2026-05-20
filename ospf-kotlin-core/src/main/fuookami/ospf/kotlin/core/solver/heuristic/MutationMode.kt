package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.algebra.value_range.coerceIn

interface MutationMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
}

data class StaticMutationMode<ObjValue, V>(
    val mutationRate: Flt64? = null
) : MutationMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): List<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        return List(population.size) {
            mutationRate?.coerceIn(mutationRateRange) ?: mutationRateRange.upperBound.value.unwrap()
        }
    }
}

data class RandomMutationMode<ObjValue, V>(
    private val randomGenerator: Generator<fuookami.ospf.kotlin.math.algebra.number.Flt64>
) : MutationMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): List<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        return List(population.size) {
            mutationRateRange.lowerBound.value.unwrap() + randomGenerator()!! * mutationRateRange.diff.unwrap()
        }
    }
}

class AdaptiveDynamicMutationMode<ObjValue, V> : MutationMode<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        mutationRateRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): List<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        val weight = (weights.max() - weights.min()) / weights.max()
        return List(population.size) {
            mutationRateRange.lowerBound.value.unwrap() + weight * mutationRateRange.diff.unwrap()
        }
    }
}



