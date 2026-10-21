/**
 * 目标比较规范化 / Objective comparison normalisation
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/**
 * 把模型的目标比较规范化为严格弱序。 / Normalize the model objective comparison into a strict weak ordering.
 *
 * 模型可以注册任意比较器（包括非严格的 `<=` / `>=`），此时 `compareObjective(a, a)` 可能返回
 * `Order.Less`，直接用于排序会破坏比较器契约、用于「是否改进」判断则会把「持平」误判为「改进」。
 * 本函数双向比较，只有真正更优时才返回负数。 / A model may register an arbitrary comparator (including the non-strict `<=` / `>=`), in which case
 * `compareObjective(a, a)` can return `Order.Less`. Using that directly for sorting breaks the comparator contract,
 * and using it for "has improved" detection misreads a tie as an improvement. This function compares in both
 * directions and only returns a negative value for a genuinely better objective.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param model 回调模型接口 / Callback model interface
 * @param lhs 左侧目标值 / Left-hand side objective value
 * @param rhs 右侧目标值 / Right-hand side objective value
 * @return 负数表示 lhs 更优，正数表示 rhs 更优，0 表示持平 / Negative when lhs is better, positive when rhs is better, 0 when tied
*/
fun <ObjValue, V> compareStrictly(
    model: AbstractCallBackModelInterface<*, ObjValue, V>,
    lhs: ObjValue,
    rhs: ObjValue
): Int where V : RealNumber<V>, V : NumberField<V> {
    val forward = model.compareObjective(lhs, rhs)
    val backward = model.compareObjective(rhs, lhs)
    return when {
        forward is Order.Less && backward is Order.Less -> 0
        forward is Order.Less -> -1
        backward is Order.Less -> 1
        else -> 0
    }
}
