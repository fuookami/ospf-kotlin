/**
 * 启发式变异操作接口
 * Heuristic mutation operation interface
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/**
 * 变异操作接口，定义对个体进行变异生成新解的行为。
 * Mutation operation interface, defining behavior for mutating individuals to generate new solutions.
 *
 * @param V 值类型 / Value type
 */
interface Mutation<V> where V : RealNumber<V>, V : NumberField<V> {
    operator fun <T : Individual<*, V>> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        mutationRate: Flt64
    ): Solution<V>
}
