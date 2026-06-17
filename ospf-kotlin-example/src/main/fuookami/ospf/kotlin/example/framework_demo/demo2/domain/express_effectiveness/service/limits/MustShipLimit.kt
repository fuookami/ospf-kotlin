package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.limits

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
 * 确保必须发货的货物恰好分配到一个位置。Ensures must-ship cargos are assigned to exactly one position.
 *
 * @property private val items 参数。
 * @property private val positions 参数。
 * @property private val stowage 参数。
 * @property private val mustShipIndices 参数。
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
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}
                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(result.error)
                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(result.errors)
            }
        }

        return ok
    }
}
