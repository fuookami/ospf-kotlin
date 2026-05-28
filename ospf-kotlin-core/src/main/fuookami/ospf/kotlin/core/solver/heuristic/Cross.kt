/**
 * 启发式交叉操作接口
 * Heuristic crossover operation interface
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/**
 * 交叉操作接口，定义从父代个体生成子代解的行为。
 * Crossover operation interface, defining behavior for generating offspring solutions from parent individuals.
 *
 * @param V 值类型 / Value type
 */
interface Cross<V> where V : RealNumber<V>, V : NumberField<V> {
    operator fun <T : Individual<*, V>> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>
    ): List<Solution<V>>
}
