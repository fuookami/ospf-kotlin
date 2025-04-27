package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface Cross<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        parent: List<T>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Solution>
}

//data object OnePointCrossOperation : CrossOperation {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
//        TODO("Not yet implemented")
//    }
//}
//
//data object TwoPointCrossOperation : CrossOperation {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
//        TODO("Not yet implemented")
//    }
//}
//
//data object MultiPointCrossOperation : CrossOperation {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
//        TODO("Not yet implemented")
//    }
//}
//
//data object UniformCrossOperation : CrossOperation {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
//        TODO("Not yet implemented")
//    }
//}
//
//data object CycleCrossOperation : CrossOperation {
//    override fun invoke(population: Population, modelInterface: CallBackModelInterface): List<Chromosome> {
//        TODO("Not yet implemented")
//    }
//}
