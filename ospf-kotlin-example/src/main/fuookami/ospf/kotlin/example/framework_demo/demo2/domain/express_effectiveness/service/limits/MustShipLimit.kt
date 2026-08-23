package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Ensures must-ship cargos are assigned to exactly one position.
 * 确保必须发货的货物恰好分配到一个位置。
 *
 * @property items 货物项列表 / The list of cargo items.
 * @property positions 配载位置列表 / The list of stowage positions.
 * @property stowage 配载分配模型 / The stowage assignment model.
 * @property mustShipIndices 必须发货的货物项索引列表 / Indices of items that must be shipped.
*/
class MustShipLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage,
    private val mustShipIndices: List<Int>,
    override val name: String = "must_ship_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        // For each must-ship cargo, sum(x[c][p] for all p) = 1
        for (c in mustShipIndices) {
            val lhs = sum((0 until positions.size).map { p -> stowage.stowage[c, p] })
            val rhs = Flt64.one

            when (val result = model.addConstraint(
                relation = lhs eq rhs,
                name = "${name}_${items[c]}"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }
}
