package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface CrossMode<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        population: List<T>,
        weights: List<Flt64>,
        model: AbstractCallBackModelInterface<*, V>,
        parentAmountRange: ValueRange<UInt64>
    ): List<List<T>>
}

//data object OneParentCrossMode : CrossMode {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): UInt64 {
//        TODO("Not yet implemented")
//    }
//}
//
//data object TwoParentCrossMode : CrossMode {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): UInt64 {
//        TODO("Not yet implemented")
//    }
//}
//
//data object MultiParentCrossMode : CrossMode {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): UInt64 {
//        TODO("Not yet implemented")
//    }
//}
//
//data object AdaptiveMultiParentCrossMode : CrossMode {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): UInt64 {
//        TODO("Not yet implemented")
//    }
//}
