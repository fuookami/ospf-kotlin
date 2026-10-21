/**
 * 启发式变异操作接口 / Heuristic mutation operation interface
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
 * 变异操作接口，定义对个体进行变异生成新解的行为。 / Mutation operation interface, defining how an individual is mutated into a new solution.
 *
 * 熵源与值转换器由调用方注入而非由实现持有，因此所有实现都是无状态单例，可安全共享于并行求解。 / The entropy source and value converter are injected by the caller instead of being held by the implementation,
 * so every implementation is a stateless singleton that can be shared safely across parallel runs.
*/
interface Mutation {

    /**
     * 对个体执行变异操作生成新解。 / Mutate an individual into a new solution.
     *
     * @param V 值类型 / Value type
     * @param T 个体类型 / Individual type
     * @param iteration 当前迭代 / Current iteration
     * @param individual 待变异的个体 / Individual to mutate
     * @param model 回调模型接口 / Callback model interface
     * @param converter 值转换器 / Value converter
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @param mutationRate 逐基因变异率 / Per-gene mutation rate
     * @return 变异后的新解 / New solution after mutation
    */
    operator fun <V, T> invoke(
        iteration: Iteration,
        individual: T,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>,
        mutationRate: Flt64
    ): Solution<V> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V>
}
