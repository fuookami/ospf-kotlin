
/**
 * 顺序约束
 * Order Constraint
 *
 * 每个顺序位置最多只能有一个动作不为0。
 * Each order position can have at most one action with non-zero allocation.
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * 顺序约束（仅用于 CapacityOrderCompilation）
 * Order Constraint (only for CapacityOrderCompilation)
 *
 * 每个顺序位置最多只能有一个动作不为0。
 * Each order position can have at most one action with non-zero allocation.
 *
 * @param V 数值类型 / Numeric type
 * @param A 生产动作类型 / Production action type
 * @property compilation 产能编译对象 / Capacity compilation object
 * @property actions 生产动作列表 / List of production actions
 * @property slots 时隙列表 / List of time slots
 * @property maxOrderPerSlot 每时隙最大顺序数 / Maximum order per slot
 * @property name 约束名称 / Constraint name
 */
class OrderConstraint<V : RealNumber<V>, A : ProductionAction>(
    private val compilation: CapacityOrderCompilation<V, A>,
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val maxOrderPerSlot: UInt64,
    val name: String = "order"
) {
    /**
     * 应用约束到模型
     * Apply constraint to model
     *
     * @param model Linear meta model / 线性元模型
     * @return Try result / Try 结果
     */
    operator fun invoke(model: LinearMetaModel<Flt64>): Try {
        val x = compilation.x
        val b = compilation.b

        for ((t, _) in slots.withIndex()) {
            for (o in 0 until maxOrderPerSlot.toInt()) {
                // Constraint 1: Each order position has at most one action
                // 约束1: 每个顺序位置最多一个动作
                val sumPoly = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
                for (a in actions.indices) {
                    sumPoly += LinearMonomial(Flt64.one, b[a, t, o])
                }
                when (val result = model.addConstraint(sumPoly.toLinearPolynomial() leq Flt64.one, name = "${name}_unique_${t}_$o")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                for ((a, _) in actions.withIndex()) {
                    // Constraint 2: Link b and x (lower bound)
                    // 约束2: 关联 b 与 x（下界）
                    when (val result = model.addConstraint(x[a, t, o] geq b[a, t, o], name = "${name}_link_lb_${a}_${t}_$o")) {
                        is Ok -> {}
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }

                    // Constraint 3: Link b and x (upper bound)
                    // 约束3: 关联 b 与 x（上界）
                    // x <= M * b where M uses x's own upper bound
                    // x <= M * b，其中 M 使用 x 自身上界
                    val upperBound = x[a, t, o].upperBound?.value?.unwrap()
                        ?: return Failed(
                            ErrorCode.IllegalArgument,
                            "${name}_link_ub_${a}_${t}_$o requires finite upper bound of x[$a,$t,$o]."
                        )
                    val upperBoundPoly = LinearMonomial(upperBound, b[a, t, o]).toLinearPolynomial()
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

