package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Computes absolute priority coefficients for cargo items at each position.
 * 计算每个位置货物项的绝对优先级系数。
 *
 * @property coefficient Mapping from cargo priority to a function that computes the coefficient for a given position. / 货物优先级到位置系数计算函数的映射
*/
class AbsoluteOrder(
    private val coefficient: HashMap<CargoPriority, (Position) -> Flt64>
) {
    companion object {
        operator fun invoke(
            items: List<Item>,
            positions: List<Position>
        ): AbsoluteOrder {
            TODO("not implemented yet")
        }
    }

    operator fun invoke(priority: CargoPriority, position: Position): Flt64 {
        return coefficient[priority]?.let { it(position) } ?: Flt64.one
    }
}
