package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.error.*
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
 * Enforces item loading constraints based on item assignment status.
 * 根据货物分配状态强制执行货物装载约束。
 *
 * @property items 待装载的货物项目列表 / the list of cargo items to be stowed
 * @property stowage 装载决策变量矩阵 / the stowage decision variable matrix
*/
class ItemAssignmentLimit(
    private val items: List<Item>,
    private val stowage: Stowage,
    override val name: String = "item_assignment_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((i, item) in items.withIndex()) {
            when (item.status) {
                ItemStatus.Preassigned, ItemStatus.AdjustmentNeeded -> {
                    when (val result = model.addConstraint(
                        stowage.loaded[i] eq true,
                        name = "${name}_${item}"
                    )) {
                        is Ok -> {}

                        is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                    }
                }

                ItemStatus.Optional -> {
                    when (val result = model.addConstraint(
                        stowage.loaded[i] leq true,
                        name = "${name}_${item}"
                    )) {
                        is Ok -> {}

                        is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                    }
                }

                else -> {}
            }
        }

        return ok
    }
}
