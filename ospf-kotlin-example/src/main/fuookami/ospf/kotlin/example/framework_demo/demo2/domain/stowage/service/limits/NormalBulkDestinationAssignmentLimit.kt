package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Enforces that normal bulk items with different destinations are not assigned to the same position.
 * 强制执行不同目的地的普通散货不被分配到同一位置。
 *
 * @property items the list of cargo items to be stowed / 待装载的货物项目列表
 * @property positions the list of available stowage positions / 可用装载位置列表
 * @property stowage the stowage decision variable matrix / 装载决策变量矩阵
*/
class NormalBulkDestinationAssignmentLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage,
    override val name: String = "normal_bulk_destination_assignment_limit"
): Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((i1, item1) in items.withIndex()) {
            if (!item1.location.bulk || item1.cargo.contains(CargoCode.AOG) || item1.cargo.contains(CargoCode.MAT)) {
                continue
            }

            for (i2 in (i1 + 1) until items.size) {
                val item2 = items[i2]
                if (!item2.location.bulk || item2.cargo.contains(CargoCode.AOG) || item2.cargo.contains(CargoCode.MAT)) {
                    continue
                }

                for ((j, position) in positions.withIndex()) {
                    if (!Stowage.stowageNeeded(item1, position)
                        || !Stowage.stowageNeeded(item2, position)
                        || position.type.contains(PositionTypeCode.AOGMATAppointed)
                        || position.loadedItems.isNotEmpty()
                    ) {
                        continue
                    }

                    when (val result = model.addConstraint(
            relation = run {
                val poly = MutableLinearPolynomial()
                poly += LinearMonomial(Flt64.one, stowage.stowage[i1, j])
                poly += LinearMonomial(Flt64.one, stowage.stowage[i2, j])
                LinearPolynomial(poly) leq Flt64.one
            },
            name = "${name}_${item1}_${item2}_${position}"
                    )) {
                        is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                        is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                            return Failed(result.error)
                        }

                        is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                            return Fatal(result.errors)
                        }
                    }
                }
            }
        }

        return ok
    }
}
