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
    /**
     * 从父代个体执行交叉操作生成子代解。
     * Perform crossover from parent individuals to generate offspring solutions.
     *
     * @param T 个体类型 / Individual type
     * @param iteration 当前迭代 / Current iteration
     * @param parents 父代个体列表 / Parent individual list
     * @param model 回调模型接口 / Callback model interface
     * @return 子代解列表 / Offspring solution list
     */
    operator fun <T : Individual<*, V>> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>
    ): List<Solution<V>>
}
