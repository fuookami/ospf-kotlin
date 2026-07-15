@file:JvmName("GeometryOpsKt")
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Ret

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
internal fun <V : FloatingNumber<V>> quantityOrdSafe(lhs: V, rhs: V, axis: String): Ret<Order> {
    return lhs.partialOrd(rhs)?.let { ok(it) }
        ?: Failed(ErrorCode.IllegalArgument, "标量不可比较：axis=$axis / Incomparable scalar: axis=$axis")
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
internal fun <V : FloatingNumber<V>> quantityMaxSafe(lhs: V, rhs: V, axis: String): Ret<V> {
    return quantityOrdSafe(lhs, rhs, axis).map {
        when (it) {
            is Order.Greater, Order.Equal -> lhs
            is Order.Less -> rhs
        }
    }
}

/**
 * 浮点数取最大值
 * Floating-point maximum
 *
 * @param V 数值类型 / The numeric type
 * @param lhs 左操作数 / The left operand
 * @param rhs 右操作数 / The right operand
 * @param axis 轴名称（用于错误信息） / The axis name (for error messages)
 * @return 较大值，失败时返回 null / The larger value, or null on failure
*/
internal fun <V : FloatingNumber<V>> quantityMaxOrNull(lhs: V, rhs: V, axis: String): V? {
    return quantityMaxSafe(lhs, rhs, axis).value
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
internal fun <V : FloatingNumber<V>> quantityMax(lhs: V, rhs: V, axis: String): Ret<V> {
    return quantityMaxSafe(lhs, rhs, axis)
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
internal fun <V : FloatingNumber<V>> quantityMinSafe(lhs: V, rhs: V, axis: String): Ret<V> {
    return quantityOrdSafe(lhs, rhs, axis).map {
        when (it) {
            is Order.Greater -> rhs
            is Order.Equal, is Order.Less -> lhs
        }
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
 * @return 较小值，失败时返回 null / The smaller value, or null on failure
*/
internal fun <V : FloatingNumber<V>> quantityMinOrNull(lhs: V, rhs: V, axis: String): V? {
    return quantityMinSafe(lhs, rhs, axis).value
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
internal fun <V : FloatingNumber<V>> quantityMin(lhs: V, rhs: V, axis: String): Ret<V> {
    return quantityMinSafe(lhs, rhs, axis)
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
internal fun <V : FloatingNumber<V>> quantityClampSafe(
    value: V,
    lb: V,
    ub: V,
    axis: String
): Ret<V> {
    val lower = when (val result = quantityOrdSafe(value, lb, "${axis}-lb")) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return when (lower) {
        is Order.Less -> ok(lb)
        else -> quantityOrdSafe(value, ub, "${axis}-ub").map {
            when (it) {
                is Order.Greater -> ub
                else -> value
            }
        }
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
 * @return 钳制后的值，失败时返回 null / The clamped value, or null on failure
*/
internal fun <V : FloatingNumber<V>> quantityClampOrNull(
    value: V,
    lb: V,
    ub: V,
    axis: String
): V? {
    return quantityClampSafe(value, lb, ub, axis).value
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
): Ret<V> {
    return quantityClampSafe(value, lb, ub, axis)
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
internal fun <V : FloatingNumber<V>> quantityContainsInRangeSafe(
    value: V,
    lb: V,
    ub: V,
    withLowerBound: Boolean,
    withUpperBound: Boolean,
    axis: String
): Ret<Boolean> {
    val lower = when (val result = quantityOrdSafe(value, lb, axis)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val upper = when (val result = quantityOrdSafe(value, ub, axis)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
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
    return ok(lowerOk && upperOk)
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
 * @return 是否在范围内，失败时返回 null / Whether in range, or null on failure
*/
internal fun <V : FloatingNumber<V>> quantityContainsInRangeOrNull(
    value: V,
    lb: V,
    ub: V,
    withLowerBound: Boolean,
    withUpperBound: Boolean,
    axis: String
): Boolean? {
    return quantityContainsInRangeSafe(
        value = value,
        lb = lb,
        ub = ub,
        withLowerBound = withLowerBound,
        withUpperBound = withUpperBound,
        axis = axis
    ).value
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
): Ret<Boolean> {
    return quantityContainsInRangeSafe(
        value = value,
        lb = lb,
        ub = ub,
        withLowerBound = withLowerBound,
        withUpperBound = withUpperBound,
        axis = axis
    )
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
