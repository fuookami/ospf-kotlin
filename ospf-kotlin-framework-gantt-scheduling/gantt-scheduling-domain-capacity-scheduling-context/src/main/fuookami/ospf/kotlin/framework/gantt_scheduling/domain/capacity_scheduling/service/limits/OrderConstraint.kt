package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * 顺序约束（仅用于 CapacityOrderCompilation）
 * Order Constraint (only for CapacityOrderCompilation)
 *
 * Each order position can have at most one action with non-zero allocation.
 * 每个顺序位置最多只能有一个动作不为 0。
 */
class OrderConstraint<A : ProductionAction>(
    private val compilation: CapacityOrderCompilation<A>,
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val maxOrderPerSlot: UInt64,
    override val name: String = "order"
) : Pipeline<AbstractLinearMetaModel> {

    override fun invoke(model: AbstractLinearMetaModel): Try {
        val x = compilation.x
        val b = compilation.b

        for ((t, _) in slots.withIndex()) {
            for (o in 0 until maxOrderPerSlot.toInt()) {
                // Constraint 1: Each order position has at most one action
                // 约束1: 每个顺序位置最多一个动作
                model.addConstraint(
                    constraint = sum(actions.indices.map { a -> b[a, t, o] }) leq Flt64.one,
                    name = "${name}_unique_${t}_$o"
                )

                for ((a, _) in actions.withIndex()) {
                    // Constraint 2: Link b and x (lower bound)
                    // 约束2: 关联 b 和 x（下界）
                    model.addConstraint(
                        constraint = x[a, t, o] geq b[a, t, o],
                        name = "${name}_link_lb_${a}_${t}_$o"
                    )

                    // Constraint 3: Link b and x (upper bound)
                    // 约束3: 关联 b 和 x（上界）
                    model.addConstraint(
                        constraint = x[a, t, o] leq Flt64(10000.0) * b[a, t, o],
                        name = "${name}_link_ub_${a}_${t}_$o"
                    )
                }
            }
        }

        return ok
    }
}