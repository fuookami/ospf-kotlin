/**
 * 固定初始解算子 / Fixed initial solution operator
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration

/**
 * 固定初始解算子，总是返回同一组数值。 / Fixed initial solution operator that always returns the same values.
 *
 * 它的用途是**把一个已有解喂给另一个骨架**，从而组合出两阶段算法：全局搜索得到一个好解之后，
 * 用轨迹骨架从这个解出发做局部精修（memetic 的基本形态）。 /
 * Its purpose is to **feed an existing solution into another skeleton**, which composes two-stage algorithms: after the global
 * phase finds a good solution, a trajectory skeleton polishes it starting from that point (the basic shape of a memetic
 * algorithm).
 *
 * 数值以 `Double` 而非泛型形式保存，因此一个实例可以服务于任意值类型——转换由调用时的 `converter` 完成。 /
 * Values are stored as `Double` rather than a generic type so one instance serves any value type; conversion is done by the
 * `converter` supplied at call time.
 *
 * @property values 初始解的各维数值 / Per-dimension values of the initial solution
*/
class FixedInitializer(
    val values: List<Double>
) : Initializer {

    override fun <V> invoke(
        iteration: Iteration,
        model: AbstractCallBackModelInterface<*, *, V>,
        amount: UInt64,
        converter: IntoValue<V>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V> {
        if (values.isEmpty()) {
            return emptyList()
        }
        val solution = values.map { converter.intoValue(Flt64(it)) }
        return List(amount.toInt().coerceAtLeast(1)) { solution }
    }
}
