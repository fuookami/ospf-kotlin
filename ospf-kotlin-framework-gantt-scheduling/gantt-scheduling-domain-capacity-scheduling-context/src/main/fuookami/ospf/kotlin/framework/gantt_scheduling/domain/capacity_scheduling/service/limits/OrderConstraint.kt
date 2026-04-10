@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.model.mechanism.geq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.CapacityOrderCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 顺序约束（仅用于 CapacityOrderCompilation�?
 * Order Constraint (only for CapacityOrderCompilation)
 *
 * Each order position can have at most one action with non-zero allocation.
 * 每个顺序位置最多只能有一个动作不�?0�?
 */
class OrderConstraint<A : ProductionAction>(
    private val compilation: CapacityOrderCompilation<A>,
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val maxOrderPerSlot: UInt64,
    val name: String = "order"
) {
    /**
     * 应用约束到模�?
     * Apply constraint to model
     */
    operator fun invoke(model: LinearMetaModel): Try {
        val x = compilation.x
        val b = compilation.b

        for ((t, _) in slots.withIndex()) {
            for (o in 0 until maxOrderPerSlot.toInt()) {
                // Constraint 1: Each order position has at most one action
                // 约束1: 每个顺序位置最多一个动�?
                val sumPoly = MutableLinearPolynomial()
                for (a in actions.indices) {
                    sumPoly += b[a, t, o]
                }
                when (val result = model.addConstraint(sumPoly leq Flt64.one, name = "${name}_unique_${t}_$o")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                for ((a, _) in actions.withIndex()) {
                    // Constraint 2: Link b and x (lower bound)
                    // 约束2: 关联 b �?x（下界）
                    when (val result = model.addConstraint(x[a, t, o] geq b[a, t, o], name = "${name}_link_lb_${a}_${t}_$o")) {
                        is Ok -> {}
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }

                    // Constraint 3: Link b and x (upper bound)
                    // 约束3: 关联 b �?x（上界）
                    // x <= M * b where M uses x's own upper bound
                    // x <= M * b，其�?M 使用 x 自身上界
                    val upperBound = x[a, t, o].upperBound?.value?.unwrap()
                        ?: return Failed(
                            ErrorCode.IllegalArgument,
                            "${name}_link_ub_${a}_${t}_$o requires finite upper bound of x[$a,$t,$o]."
                        )
                    val upperBoundPoly = upperBound * b[a, t, o]
                    when (val result = model.addConstraint(x[a, t, o] leq upperBoundPoly, name = "${name}_link_ub_${a}_${t}_$o")) {
                        is Ok -> {}
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }
}



