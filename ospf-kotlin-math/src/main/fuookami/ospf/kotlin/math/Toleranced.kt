/**
 * 容差比较
 * Toleranced Comparison
 *
 * 提供带容差的相等和比较操作，用于浮点数等需要考虑精度的数值类型的精确比较。
 * Provides tolerance-based equality and comparison operations for precise comparison of numeric types that require precision consideration, such as floating-point numbers.
 */
package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.math.algebra.concept.*

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.operator.TolerancedEq
import fuookami.ospf.kotlin.math.operator.TolerancedOrd

/**
 * 默认容差相等比较
 * Default tolerance-based equality comparison
 *
 * @param T 数值类型
 * @return 容差相等比较器
 */
fun <T> defaultTolerancedEq(): TolerancedEq<T>
        where T : RealNumber<T>, T : NumberField<T> {
    return TolerancedEq { lhs, rhs, tolerance ->
        val diff = if (lhs >= rhs) {
            lhs - rhs
        } else {
            rhs - lhs
        }
        diff.abs() <= tolerance
    }
}

/**
 * 默认容差序比较
 * Default tolerance-based order comparison
 *
 * @param T 数值类型
 * @return 容差序比较器
 */
fun <T> defaultTolerancedOrd(): TolerancedOrd<T>
        where T : RealNumber<T>, T : NumberField<T> {
    return TolerancedOrd { lhs, rhs, tolerance ->
        val diff = if (lhs >= rhs) {
            lhs - rhs
        } else {
            rhs - lhs
        }
        if (diff.abs() <= tolerance) {
            Order.Equal
        } else if (lhs < rhs) {
            Order.Less()
        } else {
            Order.Greater()
        }
    }
}




