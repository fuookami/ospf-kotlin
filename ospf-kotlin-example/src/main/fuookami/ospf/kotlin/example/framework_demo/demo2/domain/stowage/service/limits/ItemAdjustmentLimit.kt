package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Enforces that items requiring adjustment are not stowed in any position.
 * 强制执行需要调整的货物不被装载到任何位置。
 *
 * @property items the list of cargo items to be stowed / 待装载的货物项目列表
 * @property stowage the stowage decision variable matrix / 装载决策变量矩阵
 */
class ItemAdjustmentLimit(
    private val items: List<Item>,
    private val stowage: Stowage,
    override val name: String = "item_adjustment_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((i, item) in items.withIndex()) {
            when (item.status) {
                ItemStatus.AdjustmentNeeded -> {
                    when (val result = model.addConstraint(
            relation = sum(stowage.u[i, _a]) eq Flt64.zero,
            name = "${name}_${item}"
                    )) {
                        is Ok<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                        is Failed<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return result
                }

                is Fatal<fuookami.ospf.kotlin.utils.functional.Success, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return result
                }
                    }
                }

                else -> {}
            }
        }

        return ok
    }
}
