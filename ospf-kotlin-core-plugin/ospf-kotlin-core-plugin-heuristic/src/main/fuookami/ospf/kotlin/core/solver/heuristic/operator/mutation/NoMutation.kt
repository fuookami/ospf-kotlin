/**
 * 空变异算子 / No-op mutation
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*

/**
 * 空变异，原样返回解。 / No-op mutation returning the solution unchanged.
 *
 * 用于本身不含变异的算法（GWO、SCA、MVO 等），使它们可以复用种群骨架而不被强加一次扰动。 / Used by algorithms that have no mutation step of their own (GWO, SCA, MVO, …) so they can reuse the population skeleton
 * without being forced through a perturbation.
*/
data object NoMutation : Mutation {
    override fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        return individual.solution.toList()
    }
}
