/**
 * 迭代观测钩子 / Iteration observer hook
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.Individual

/**
 * 迭代观测钩子，把「一轮结束后发生了什么」广播给需要反馈的算子。 / Iteration observer hook broadcasting "what happened at the end of a round" to operators that need feedback.
 *
 * 骨架持有循环与档案，因此只有它能回答「这一轮有没有变好」。需要奖励信号的自适应算子
 * （Q 学习、自适应步长等）通过本钩子拿到该信号，而不必自己重复计算。 /
 * The skeleton owns the loop and the archive, so only it can answer "did this round improve". Adaptive operators
 * that need a reward signal (Q-learning, adaptive step sizes, …) obtain it here instead of recomputing it.
 *
 * 类型参数声明在方法上，因此本接口本身无泛型，骨架可以直接持有 `List<IterationObserver>`。 /
 * Type parameters are declared on the method, so the interface itself is not generic and the skeleton can hold a
 * plain `List<IterationObserver>`.
*/
interface IterationObserver {
    /** 开始独立搜索时清空运行状态 / Clear runtime state before an independent search. */
    fun reset() {}


    /**
     * 观测一轮结果 / Observe the outcome of one round
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param iteration 当前迭代（本轮推进前的计数）/ Current iteration (count before this round advanced)
     * @param members 本轮成员 / Members of this round
     * @param improved 档案最优是否被改善 / Whether the archive best improved
     * @param converter 值转换器 / Value converter
    */
    fun <ObjValue, V> observe(
        iteration: Iteration,
        members: List<Individual<ObjValue, V>>,
        improved: Boolean,
        converter: IntoValue<V>
    ) where V : RealNumber<V>, V : NumberField<V>
}
