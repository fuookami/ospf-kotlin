package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * 计算每个位置货物项的绝对优先级系数。Computes absolute priority coefficients for cargo items at each position.
 *
 * @property private val coefficient 参数。
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
