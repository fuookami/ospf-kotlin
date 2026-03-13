package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

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
    val name: String = "order"
) {
    /**
     * 应用约束到模型
     * Apply constraint to model
     */
    operator fun invoke(model: LinearMetaModel): Try {
        val x = compilation.x
        val b = compilation.b

        for ((t, _) in slots.withIndex()) {
            for (o in 0 until maxOrderPerSlot.toInt()) {
                // Constraint 1: Each order position has at most one action
                // 约束1: 每个顺序位置最多一个动作
                val sumPoly = MutableLinearPolynomial()
                for (a in actions.indices) {
                    sumPoly += b[a, t, o]
                }
                when (val result = model.addConstraint(sumPoly leq Flt64.one, name = "${name}_unique_${t}_$o")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                }

                for ((a, _) in actions.withIndex()) {
                    // Constraint 2: Link b and x (lower bound)
                    // 约束2: 关联 b 和 x（下界）
                    when (val result = model.addConstraint(x[a, t, o] geq b[a, t, o], name = "${name}_link_lb_${a}_${t}_$o")) {
                        is Ok -> {}
                        is Failed -> return Failed(result.error)
                    }

                    // Constraint 3: Link b and x (upper bound)
                    // 约束3: 关联 b 和 x（上界）
                    // x <= M * b where M is a large constant
                    // x <= M * b，其中 M 是一个大常数
                    val upperBoundPoly = 10000.0 * b[a, t, o]
                    when (val result = model.addConstraint(x[a, t, o] leq upperBoundPoly, name = "${name}_link_ub_${a}_${t}_$o")) {
                        is Ok -> {}
                        is Failed -> return Failed(result.error)
                    }
                }
            }
        }

        return ok
    }
}
