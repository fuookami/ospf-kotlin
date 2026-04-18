package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.math.algebra.number.Flt64

interface Mutation<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRate: Flt64
    ): Solution
}
