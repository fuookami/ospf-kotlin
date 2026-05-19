package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64

interface Mutation<V> where V : RealNumber<V>, V : NumberField<V> {
    operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, V>,
        mutationRate: Flt64
    ): Solution<V>
}