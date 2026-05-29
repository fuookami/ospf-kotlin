@file:JvmName("GeometryOpsKt")
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 几何数量运算
 * Geometry Quantity Operations
 *
 * 提供几何空间中浮点数的基本运算函数（加、减、乘、除、比较等）。
 * Provides basic arithmetic operation functions (add, subtract, multiply, divide, compare, etc.) for floating-point numbers in geometric space.
 */

/**
 * 浮点数加法
 * Floating-point addition
 *
 * @param V 数值类型 / The numeric type
 * @param lhs 左操作数 / The left operand
 * @param rhs 右操作数 / The right operand
 * @return 两数之和 / The sum of the two values
 */
internal fun <V : FloatingNumber<V>> quantityPlus(lhs: V, rhs: V): V {
    return lhs + rhs
}

/**
 * 浮点数减法
 * Floating-point subtraction
 *
 * @param V 数值类型 / The numeric type
 * @param lhs 左操作数 / The left operand
 * @param rhs 右操作数 / The right operand
 * @return 两数之差 / The difference of the two values
 */
internal fun <V : FloatingNumber<V>> quantityMinus(lhs: V, rhs: V): V {
    return lhs - rhs
}

/**
 * 浮点数比较，返回偏序结果
 * Floating-point comparison, returning partial order result
 *
 * @param V 数值类型 / The numeric type
 * @param lhs 左操作数 / The left operand
 * @param rhs 右操作数 / The right operand
 * @param axis 轴名称（用于错误信息） / The axis name (for error messages)
 * @return 偏序结果 / The partial order result
 */
internal fun <V : FloatingNumber<V>> quantityOrd(lhs: V, rhs: V, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable scalar on axis $axis")
}

/**
 * 浮点数取最大值
 * Floating-point maximum
 *
 * @param V 数值类型 / The numeric type
 * @param lhs 左操作数 / The left operand
 * @param rhs 右操作数 / The right operand
 * @param axis 轴名称（用于错误信息） / The axis name (for error messages)
 * @return 较大值 / The larger value
 */
internal fun <V : FloatingNumber<V>> quantityMax(lhs: V, rhs: V, axis: String): V {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

/**
 * 浮点数取最小值
 * Floating-point minimum
 *
 * @param V 数值类型 / The numeric type
 * @param lhs 左操作数 / The left operand
 * @param rhs 右操作数 / The right operand
 * @param axis 轴名称（用于错误信息） / The axis name (for error messages)
 * @return 较小值 / The smaller value
 */
internal fun <V : FloatingNumber<V>> quantityMin(lhs: V, rhs: V, axis: String): V {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}

/**
 * 浮点数钳制到指定范围
 * Clamp a floating-point value to the specified range
 *
 * @param V 数值类型 / The numeric type
 * @param value 待钳制的值 / The value to clamp
 * @param lb 下界 / The lower bound
 * @param ub 上界 / The upper bound
 * @param axis 轴名称（用于错误信息） / The axis name (for error messages)
 * @return 钳制后的值 / The clamped value
 */
internal fun <V : FloatingNumber<V>> quantityClamp(
    value: V,
    lb: V,
    ub: V,
    axis: String
): V {
    return when (quantityOrd(value, lb, "${axis}-lb")) {
        is Order.Less -> lb
        else -> when (quantityOrd(value, ub, "${axis}-ub")) {
            is Order.Greater -> ub
            else -> value
        }
    }
}

/**
 * 判断浮点数是否在指定范围内
 * Check whether a floating-point value is within the specified range
 *
 * @param V 数值类型 / The numeric type
 * @param value 待检测的值 / The value to check
 * @param lb 下界 / The lower bound
 * @param ub 上界 / The upper bound
 * @param withLowerBound 是否包含下界 / Whether to include the lower bound
 * @param withUpperBound 是否包含上界 / Whether to include the upper bound
 * @param axis 轴名称（用于错误信息） / The axis name (for error messages)
 * @return 是否在范围内 / Whether the value is within the range
 */
internal fun <V : FloatingNumber<V>> quantityContainsInRange(
    value: V,
    lb: V,
    ub: V,
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
 * 获取与给定值同类型的零值
 * Get the zero value of the same type as the given value
 *
 * @param V 数值类型 / The numeric type
 * @param value 参考值 / The reference value
 * @return 零值 / The zero value
 */
internal fun <V : FloatingNumber<V>> quantityZeroOf(value: V): V {
    return value.constants.zero
}
