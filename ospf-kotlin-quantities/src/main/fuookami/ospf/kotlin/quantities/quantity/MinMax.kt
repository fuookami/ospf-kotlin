/**
 * 物理量最小/最大值函数
 * Minimum/Maximum Functions for Physical Quantities
 *
 * 提供物理量之间的最小值和最大值比较函数。
 * Provides minimum and maximum comparison functions between physical quantities.
 *
 * 基于 PartialOrd 进行比较，支持无法比较（返回 null）的情况。
 * Based on PartialOrd for comparison, supports incomparable cases (returns null).
 */
package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.PartialOrd

/**
 * 获取两个物理量中的较小值
 * Get the minimum of two quantities
 *
 * 基于 PartialOrd 进行比较。如果 lhs <= rhs，返回 lhs；否则返回 rhs。
 * Compares based on PartialOrd. Returns lhs if lhs <= rhs, otherwise returns rhs.
 *
 * 当量纲不匹配或值无法比较时，返回 null。
 * Returns null when dimensions don't match or values are incomparable.
 *
 * 示例 / Example:
 * ```kotlin
 * val a = Flt64(5.0) * Meter
 * val b = Flt64(3.0) * Meter
 * val result = min(a, b)  // Quantity(Flt64(3.0), Meter)
 *
 * val c = Flt64(5.0) * Second
 * val result2 = min(a, c)  // null (量纲不匹配 / dimension mismatch)
 * ```
 *
 * @param lhs 左侧物理量 / Left-hand quantity
 * @param rhs 右侧物理量 / Right-hand quantity
 * @return 较小的物理量，或 null 如果无法比较 / The smaller quantity, or null if incomparable
 */
fun <T : PartialOrd<T>> min(lhs: Quantity<T>, rhs: Quantity<T>): Quantity<T>? = when (lhs partialOrd rhs) {
    is Order.Less, is Order.Equal -> {
        lhs
    }

    is Order.Greater -> {
        rhs
    }

    null -> {
        null
    }
}

/**
 * 获取两个物理量中的较大值
 * Get the maximum of two quantities
 *
 * 基于 PartialOrd 进行比较。如果 lhs >= rhs，返回 lhs；否则返回 rhs。
 * Compares based on PartialOrd. Returns lhs if lhs >= rhs, otherwise returns rhs.
 *
 * 当量纲不匹配或值无法比较时，返回 null。
 * Returns null when dimensions don't match or values are incomparable.
 *
 * 示例 / Example:
 * ```kotlin
 * val a = Flt64(5.0) * Meter
 * val b = Flt64(3.0) * Meter
 * val result = max(a, b)  // Quantity(Flt64(5.0), Meter)
 *
 * val c = Flt64(5.0) * Second
 * val result2 = max(a, c)  // null (量纲不匹配 / dimension mismatch)
 * ```
 *
 * @param lhs 左侧物理量 / Left-hand quantity
 * @param rhs 右侧物理量 / Right-hand quantity
 * @return 较大的物理量，或 null 如果无法比较 / The larger quantity, or null if incomparable
 */
fun <T : PartialOrd<T>> max(lhs: Quantity<T>, rhs: Quantity<T>): Quantity<T>? = when (lhs partialOrd rhs) {
    is Order.Greater, is Order.Equal -> {
        lhs
    }

    is Order.Less -> {
        rhs
    }

    null -> {
        null
    }
}
