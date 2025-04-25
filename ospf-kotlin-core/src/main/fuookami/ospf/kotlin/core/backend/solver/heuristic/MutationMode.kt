package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
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

//interface MutationMode {
//    /**
//     * calculate mutation rate
//     *
//     * @param population
//     * @param model
//     * @return
//     */
//    operator fun invoke(
//        population: Population,
//        model: CallBackModelInterface
//    ): Flt64
//}
//
//data object StaticMutationMode : MutationMode {
//    override fun invoke(population: Population, model: CallBackModelInterface): Flt64 {
//        return population.mutationRange.upperBound.value.unwrap()
//    }
//}
//
//data object AdaptiveDynamicMutationMode : MutationMode {
//    override fun invoke(population: Population, model: CallBackModelInterface): Flt64 {
//        val (minFitness, maxFitness) = population.chromosomes
//            .mapNotNull { it.fitness }
//            .minMaxWithPartialThreeWayComparatorOrNull { lhs, rhs -> model.compareObjective(lhs, rhs) }
//            ?: return population.mutationRange.upperBound.value.unwrap()
//        val x = abs(maxFitness - minFitness) / max(minFitness, maxFitness)
//
//        return if (x ls Flt64.decimalPrecision) {
//            population.mutationRange.upperBound.value.unwrap()
//        } else {
//            return min(
//                population.mutationRange.lowerBound.value.unwrap() * x,
//                population.mutationRange.upperBound.value.unwrap()
//            )
//        }
//    }
//}
