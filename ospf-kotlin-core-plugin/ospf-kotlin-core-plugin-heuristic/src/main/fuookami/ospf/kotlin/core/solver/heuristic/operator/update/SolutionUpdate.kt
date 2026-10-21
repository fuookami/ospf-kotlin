/**
 * 解更新算子 / Solution update operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.update

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.Individual

/**
 * 解更新算子契约：从一个父代组产生一批新解。 / Solution update contract: produce a batch of new solutions from one parent group.
 *
 * 交叉与位置更新在数学上是同一件事——「看一组父代，产出新解」。把二者统一在一个契约下以后，
 * 种群骨架既可以直接插 GA 的交叉，也可以插 GWO / SCA / MVO 的位置更新，无需为每个算法重写循环。 / Crossover and position update are mathematically the same thing: "look at a group of parents, emit new solutions". Unifying
 * them under one contract lets the population skeleton host either a GA crossover or a GWO / SCA / MVO position update without
 * rewriting the loop per algorithm.
*/
interface SolutionUpdate {

    /**
     * 产生新解 / Produce new solutions
     *
     * @param V 值类型 / Value type
     * @param T 父代个体类型 / Parent individual type
     * @param iteration 当前迭代 / Current iteration
     * @param parents 父代组 / Parent group
     * @param model 回调模型接口 / Callback model interface
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 新解列表 / New solution list
    */
    operator fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V>
}
