/**
 * 目标值归一化接口与实现
 * Objective normalization interface and implementations
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.neq
import fuookami.ospf.kotlin.utils.functional.sum
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/**
 * 目标值归一化接口，将目标值列表归一化为权重。
 * Objective normalization interface, normalizing objective value lists into weights.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 */
interface ObjectiveNormalization<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    operator fun invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        objs: List<ObjValue>
    ): List<fuookami.ospf.kotlin.math.algebra.number.Flt64>
}

/**
 * 最小-最大归一化，将目标值映射到 [0, 1] 区间。
 * Min-max normalization, mapping objective values to [0, 1] range.
 */
data object MinMaxNormalization : ObjectiveNormalization<Flt64, Flt64> {
    override fun invoke(model: AbstractCallBackModelInterface<*, Flt64, Flt64>, objs: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>): List<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        val minObj = objs.min()
        val maxObj = objs.max()
        return if (model.compareObjective(minObj, maxObj) is Order.Less) {
            objs.map { Flt64.one - (it - minObj) / (maxObj - minObj) }
        } else {
            objs.map { (it - minObj) / (maxObj - minObj) }
        }
    }
}

/**
 * 求和归一化，将目标值转换为占比权重。
 * Sum normalization, converting objective values to proportionate weights.
 */
data object SumNormalization : ObjectiveNormalization<Flt64, Flt64> {
    override fun invoke(model: AbstractCallBackModelInterface<*, Flt64, Flt64>, objs: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>): List<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        val minObj = objs.min()
        val maxObj = objs.max()
        val minMaxObjs = objs.map { (it - minObj) / (maxObj - minObj) }
        val sum = minMaxObjs.sum()
        return if (model.compareObjective(minObj, maxObj) is Order.Less) {
            if (sum neq Flt64.zero) {
                minMaxObjs.map { Flt64.one - it / sum }
            } else {
                objs.indices.map { Flt64.one / Flt64(objs.size) }
            }
        } else {
            if (sum neq Flt64.zero) {
                minMaxObjs.map { it / sum }
            } else {
                objs.indices.map { Flt64.one / Flt64(objs.size) }
            }
        }
    }
}
