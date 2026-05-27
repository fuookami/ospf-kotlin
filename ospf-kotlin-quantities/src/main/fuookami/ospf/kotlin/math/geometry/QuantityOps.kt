/**
 * 物理量几何运算辅助函数
 * Quantity geometry operation helper functions
 *
 * 提供物理量的加减、比较、取极值、限制范围和获取零值等内部辅助函数。
 * Provides internal helper functions for quantity addition, subtraction, comparison, clamping, and zero value retrieval.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 两个物理量相加（内部辅助函数）
 * Add two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param V 数值类型 / Number type
 * @return 相加结果 / Sum result
 * @throws IllegalArgumentException 如果量纲不匹配 / If dimensions don't match
 */
internal fun <V : FloatingNumber<V>> quantityPlus(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    if (lhs.unit == rhs.unit) {
        return Quantity(lhs.value + rhs.value, lhs.unit)
    }
    require(lhs.unit.quantity == rhs.unit.quantity) {
        "Dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: throw IllegalArgumentException("Cannot convert unit ${rhs.unit} to ${lhs.unit}.")
    return Quantity(lhs.value + converted.value, lhs.unit)
}

/**
 * 两个物理量相减（内部辅助函数）
 * Subtract two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param V 数值类型 / Number type
 * @return 相减结果 / Difference result
 * @throws IllegalArgumentException 如果量纲不匹配 / If dimensions don't match
 */
internal fun <V : FloatingNumber<V>> quantityMinus(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    if (lhs.unit == rhs.unit) {
        return Quantity(lhs.value - rhs.value, lhs.unit)
    }
    require(lhs.unit.quantity == rhs.unit.quantity) {
        "Dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: throw IllegalArgumentException("Cannot convert unit ${rhs.unit} to ${lhs.unit}.")
    return Quantity(lhs.value - converted.value, lhs.unit)
}

/**
 * 比较两个物理量的大小（内部辅助函数）
 * Compare two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param axis 轴名称（用于错误信息）/ Axis name (for error messages)
 * @param V 数值类型 / Number type
 * @return 比较结果 / Comparison result
 * @throws IllegalArgumentException 如果物理量不可比较 / If quantities are incomparable
 */
internal fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

/**
 * 取两个物理量的较大值（内部辅助函数）
 * Get the maximum of two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param axis 轴名称（用于错误信息）/ Axis name (for error messages)
 * @param V 数值类型 / Number type
 * @return 较大值 / Maximum value
 */
internal fun <V : FloatingNumber<V>> quantityMax(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

/**
 * 取两个物理量的较小值（内部辅助函数）
 * Get the minimum of two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param axis 轴名称（用于错误信息）/ Axis name (for error messages)
 * @param V 数值类型 / Number type
 * @return 较小值 / Minimum value
 */
internal fun <V : FloatingNumber<V>> quantityMin(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}

/**
 * 将物理量限制在指定范围内（内部辅助函数）
 * Clamp a quantity to a specified range (internal helper)
 *
 * @param value 待限制的物理量 / Quantity to clamp
 * @param lb 下界 / Lower bound
 * @param ub 上界 / Upper bound
 * @param axis 轴名称（用于错误信息）/ Axis name (for error messages)
 * @param V 数值类型 / Number type
 * @return 限制后的物理量 / Clamped quantity
 */
internal fun <V : FloatingNumber<V>> quantityClamp(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
    axis: String
): Quantity<V> {
    return when (quantityOrd(value, lb, "${axis}-lb")) {
        is Order.Less -> lb
        else -> when (quantityOrd(value, ub, "${axis}-ub")) {
            is Order.Greater -> ub
            else -> value
        }
    }
}

/**
 * 判断物理量是否在指定范围内（内部辅助函数）
 * Check if a quantity is within a specified range (internal helper)
 *
 * @param value 待检查的物理量 / Quantity to check
 * @param lb 下界 / Lower bound
 * @param ub 上界 / Upper bound
 * @param withLowerBound 是否包含下界 / Whether to include lower bound
 * @param withUpperBound 是否包含上界 / Whether to include upper bound
 * @param axis 轴名称（用于错误信息）/ Axis name (for error messages)
 * @param V 数值类型 / Number type
 * @return 是否在范围内 / Whether in range
 */
internal fun <V : FloatingNumber<V>> quantityContainsInRange(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
    withLowerBound: Boolean,
    withUpperBound: Boolean,
    axis: String
): Boolean {
    val lower = quantityOrd(value, lb, axis)
    val upper = quantityOrd(value, ub, axis)
    val lowerOk = if (withLowerBound) {
        lower is Order.Equal || lower is Order.Greater
    } else {
        lower is Order.Greater
    }
    val upperOk = if (withUpperBound) {
        upper is Order.Equal || upper is Order.Less
    } else {
        upper is Order.Less
    }
    return lowerOk && upperOk
}

/**
 * 获取与给定物理量同单位的零值（内部辅助函数）
 * Get a zero-valued quantity with the same unit as the given quantity (internal helper)
 *
 * @param quantity 参考物理量 / Reference quantity
 * @param V 数值类型 / Number type
 * @return 同单位的零值物理量 / Zero-valued quantity with the same unit
 */
internal fun <V : FloatingNumber<V>> quantityZeroOf(quantity: Quantity<V>): Quantity<V> {
    return quantity.value.constants.zero * quantity.unit
}
