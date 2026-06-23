/**
 * 物理量几何运算辅助函数
 * Quantity geometry operation helper functions
 *
 * 提供物理量的加减、比较、取极值、限制范围和获取零值等内部辅助函数。
 * Provides internal helper functions for quantity addition, subtraction, comparison, clamping, and zero value retrieval.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 两个物理量相加（内部辅助函数）
 * Add two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param V 数值类型 / Number type
 * @return 相加结果 / Sum result
 */
internal fun <V : FloatingNumber<V>> quantityPlusSafe(lhs: Quantity<V>, rhs: Quantity<V>): Ret<Quantity<V>> {
    if (lhs.unit == rhs.unit) {
        return ok(Quantity(lhs.value + rhs.value, lhs.unit))
    }
    if (lhs.unit.quantity != rhs.unit.quantity) {
        return Failed(
            ErrorCode.IllegalArgument,
            "物理量量纲不匹配：${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()} / " +
                    "Quantity dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
        )
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: return Failed(
            ErrorCode.IllegalArgument,
            "单位转换失败：${rhs.unit} -> ${lhs.unit} / Unit conversion failed: ${rhs.unit} -> ${lhs.unit}"
        )
    return ok(Quantity(lhs.value + converted.value, lhs.unit))
}

/**
 * 两个物理量相加（内部辅助函数）
 * Add two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param V 数值类型 / Number type
 * @return 相加结果，失败时返回 null / Sum result, or null on failure
 */
internal fun <V : FloatingNumber<V>> quantityPlusOrNull(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V>? {
    return quantityPlusSafe(lhs, rhs).value
}

/**
 * 两个物理量相减（内部辅助函数）
 * Subtract two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param V 数值类型 / Number type
 * @return 相减结果 / Difference result
 */
internal fun <V : FloatingNumber<V>> quantityMinusSafe(lhs: Quantity<V>, rhs: Quantity<V>): Ret<Quantity<V>> {
    if (lhs.unit == rhs.unit) {
        return ok(Quantity(lhs.value - rhs.value, lhs.unit))
    }
    if (lhs.unit.quantity != rhs.unit.quantity) {
        return Failed(
            ErrorCode.IllegalArgument,
            "物理量量纲不匹配：${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()} / " +
                    "Quantity dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
        )
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: return Failed(
            ErrorCode.IllegalArgument,
            "单位转换失败：${rhs.unit} -> ${lhs.unit} / Unit conversion failed: ${rhs.unit} -> ${lhs.unit}"
        )
    return ok(Quantity(lhs.value - converted.value, lhs.unit))
}

/**
 * 两个物理量相减（内部辅助函数）
 * Subtract two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param V 数值类型 / Number type
 * @return 相减结果，失败时返回 null / Difference result, or null on failure
 */
internal fun <V : FloatingNumber<V>> quantityMinusOrNull(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V>? {
    return quantityMinusSafe(lhs, rhs).value
}

/** 计算两个物理量的乘积（重载 1）/ Compute product of two quantities (overload 1) */
internal fun <V : FloatingNumber<V>> quantityProduct(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return Quantity(lhs.value * rhs.value, lhs.unit * rhs.unit)
}

/** 计算两个物理量的乘积（重载 2）/ Compute product of two quantities (overload 2) */
internal fun <V : FloatingNumber<V>> quantityProduct(lhs: Quantity<V>, rhs: V): Quantity<V> {
    return Quantity(lhs.value * rhs, lhs.unit)
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
 */
internal fun <V : FloatingNumber<V>> quantityOrdSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Order> {
    return lhs.partialOrd(rhs)?.let { ok(it) }
        ?: Failed(
            ErrorCode.IllegalArgument,
            "物理量不可比较：axis=$axis, ${lhs.unit} vs ${rhs.unit} / " +
                    "Incomparable quantity: axis=$axis, ${lhs.unit} vs ${rhs.unit}"
        )
}

/**
 * 比较两个物理量的大小（内部辅助函数）
 * Compare two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param axis 轴名称（用于错误信息）/ Axis name (for error messages)
 * @param V 数值类型 / Number type
 * @return 比较结果，失败时返回 null / Comparison result, or null on failure
 */
internal fun <V : FloatingNumber<V>> quantityOrdOrNull(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order? {
    return quantityOrdSafe(lhs, rhs, axis).value
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
internal fun <V : FloatingNumber<V>> quantityMaxSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Quantity<V>> {
    return quantityOrdSafe(lhs, rhs, axis).map {
        when (it) {
            is Order.Greater, Order.Equal -> lhs
            is Order.Less -> rhs
        }
    }
}

/**
 * 取两个物理量的较大值（内部辅助函数）
 * Get the maximum of two quantities (internal helper)
 *
 * @param lhs 左操作数 / Left operand
 * @param rhs 右操作数 / Right operand
 * @param axis 轴名称（用于错误信息）/ Axis name (for error messages)
 * @param V 数值类型 / Number type
 * @return 较大值，失败时返回 null / Maximum value, or null on failure
 */
internal fun <V : FloatingNumber<V>> quantityMaxOrNull(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V>? {
    return quantityMaxSafe(lhs, rhs, axis).value
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
internal fun <V : FloatingNumber<V>> quantityMinSafe(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Quantity<V>> {
    return quantityOrdSafe(lhs, rhs, axis).map {
        when (it) {
            is Order.Greater -> rhs
            is Order.Equal, is Order.Less -> lhs
        }
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
 * @return 较小值，失败时返回 null / Minimum value, or null on failure
 */
internal fun <V : FloatingNumber<V>> quantityMinOrNull(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V>? {
    return quantityMinSafe(lhs, rhs, axis).value
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
internal fun <V : FloatingNumber<V>> quantityClampSafe(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
    axis: String
): Ret<Quantity<V>> {
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
 * 将物理量限制在指定范围内（内部辅助函数）
 * Clamp a quantity to a specified range (internal helper)
 *
 * @param value 待限制的物理量 / Quantity to clamp
 * @param lb 下界 / Lower bound
 * @param ub 上界 / Upper bound
 * @param axis 轴名称（用于错误信息）/ Axis name (for error messages)
 * @param V 数值类型 / Number type
 * @return 限制后的物理量，失败时返回 null / Clamped quantity, or null on failure
 */
internal fun <V : FloatingNumber<V>> quantityClampOrNull(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
    axis: String
): Quantity<V>? {
    return quantityClampSafe(value, lb, ub, axis).value
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
internal fun <V : FloatingNumber<V>> quantityContainsInRangeSafe(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
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
 * @return 是否在范围内，失败时返回 null / Whether in range, or null on failure
 */
internal fun <V : FloatingNumber<V>> quantityContainsInRangeOrNull(
    value: Quantity<V>,
    lb: Quantity<V>,
    ub: Quantity<V>,
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
