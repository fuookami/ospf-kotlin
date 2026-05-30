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
    /**
     * 对个体执行变异操作生成新解。
     * Perform mutation on an individual to generate a new solution.
     *
     * @param T 个体类型 / Individual type
     * @param iteration 当前迭代 / Current iteration
     * @param individual 待变异的个体 / Individual to mutate
     * @param model 回调模型接口 / Callback model interface
     * @param mutationRate 变异率 / Mutation rate
     * @return 变异后的新解 / New solution after mutation
     */
    operator fun <T : Individual<*, V>> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        mutationRate: Flt64
    ): Solution<V>
}
