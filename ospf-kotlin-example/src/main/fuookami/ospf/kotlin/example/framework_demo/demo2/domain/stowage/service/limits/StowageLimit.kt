package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Enforces stowage feasibility constraints, preventing infeasible item-position assignments.
 * 强制执行装载可行性约束，防止不可行的货物-位置分配。
 *
 * @property items the list of cargo items to be stowed / 待装载的货物项目列表
 * @property positions the list of available stowage positions / 可用装载位置列表
 * @property stowage the stowage decision variable matrix / 装载决策变量矩阵
 */
class StowageLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage,
    override val name: String = "stowage_limit"
): Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((i, item) in items.withIndex()) {
            for ((j, position) in positions.withIndex()) {
                if (item.status.available && position.status.available && !position.enabled(item).ok) {
                    when (val result = model.addConstraint(
                        stowage.stowage[i, j] eq false,
                        name = "${name}_${item}_${position}"
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
