package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 物理量区间，使用显式上下界和开闭区间语义 / Quantity range with explicit bounds and interval semantics
 *
 * @param V 数值类型 / Numeric value type
 * @property lowerBound 下界 / Lower bound
 * @property upperBound 上界 / Upper bound
 * @property lowerInclusive 下界是否包含 / Whether lower bound is inclusive
 * @property upperInclusive 上界是否包含 / Whether upper bound is inclusive
*/
data class QuantityRange<V : RealNumber<V>>(
    val lowerBound: Quantity<V>,
    val upperBound: Quantity<V>,
    val lowerInclusive: Boolean = true,
    val upperInclusive: Boolean = true
) {
    init {
        val order = lowerBound partialOrd upperBound
        check(order != null && order !is Order.Greater)
        if (order is Order.Equal) {
            check(lowerInclusive && upperInclusive)
        }
    }

    /**
     * 判断值是否落在区间内 / Check whether the value is inside the range
     *
     * @param value 待判断值 / Value to check
     * @return 是否在区间内 / Whether inside the range
    */
    fun contains(value: Quantity<V>): Boolean {
        val leftOrder = (value partialOrd lowerBound) ?: return false
        val rightOrder = (value partialOrd upperBound) ?: return false
        val lowerOk = when (leftOrder) {
            is Order.Greater -> true
            is Order.Equal -> lowerInclusive
            is Order.Less -> false
        }
        val upperOk = when (rightOrder) {
            is Order.Less -> true
            is Order.Equal -> upperInclusive
            is Order.Greater -> false
        }
        return lowerOk && upperOk
    }
}
