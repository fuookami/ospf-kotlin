package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

interface Cross<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Solution>
}
