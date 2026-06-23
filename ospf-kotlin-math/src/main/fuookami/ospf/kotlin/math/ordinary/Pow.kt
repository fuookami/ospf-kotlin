/**
 * 幂函敌
 * Power Functions
 *
 * 为实数类型提供整数指数幂和浮点指数幂的高精度计算。
 * 整数指数幌pow(base, index)：使用快速幂算法（二分递归），
 * 时间复杂庌O(log n)，通过平方递归减少乘法次数。
 * 正整数指数：适用二TimesSemiGroup 类型，要汌base 支持乘法。
 * 负整数指数：适用二TimesGroup 类型，要汌base 支持乘法和除法（逆元）。
 * 浮点指数幌powf(base, index)：通过 ln 和exp 实现，
 * 公式：pow(base, index) = exp(index * ln(base))。
 * 指数函数 exp(index)：泰勒级数展开 exp(x) = 1 + x + x^2/2! + x^3/3! + ...
 * 边界情况：index = 0 返回 one，正整数指数幂不支持负数 base 的负指数。
 * 支持通过 digits 和precision 参数控制计算精度。
 *
 * Provides high-precision computation of integer-exponent and floating-exponent power for real number types.
 * Integer-exponent power pow(base, index): uses fast power algorithm (binary recursion),
 * time complexity O(log n), reducing multiplication count via square recursion.
 * Positive integer exponent: applies to TimesSemiGroup types requiring multiplication support.
 * Negative integer exponent: applies to TimesGroup types requiring multiplication and division (inverse).
 * Floating-exponent power powf(base, index): implemented via ln and exp,
 * formula: pow(base, index) = exp(index * ln(base)).
 * Exponential function exp(index): Taylor series exp(x) = 1 + x + x^2/2! + x^3/3! + ...
 * Boundary cases: index = 0 returns one; negative integer exponent not supported for TimesSemiGroup types.
 * Supports configurable precision via digits and precision parameters.
 */
package fuookami.ospf.kotlin.math.ordinary

import java.math.RoundingMode
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/** 归一化 FltX 精度，非 FltX 类型保持不变 / Normalize FltX scale; non-FltX types remain unchanged */
@Suppress("UNCHECKED_CAST")
private fun <T : FloatingNumber<T>> normalizeFltXScale(value: T, digits: Int): T {
    return if (value is FltX) {
        value.withScale(digits, RoundingMode.HALF_UP) as T
    } else {
        value
    }
}

/** 正整数指数快速幂递归实现 / Positive integer exponent fast power recursive implementation */
private tailrec fun <T : TimesSemiGroup<T>> powPosImpl(
    value: T,
    base: T,
    index: Int,
    digits: Int,
    precision: T
): T {
    return if (index == 1) {
        value * base
    } else if (index % 2 == 0) {
        powPosImpl(
            value = value,
            base = base * base,
            index = index / 2,
            digits = digits,
            precision = precision
        )
    } else {
        powPosImpl(
            value = value * base,
            base = base,
            index = index - 1,
            digits = digits,
            precision = precision
        )
    }
}

/** 负整数指数快速幂递归实现 / Negative integer exponent fast power recursive implementation */
private tailrec fun <T : TimesGroup<T>> powNegImpl(
    value: T,
    base: T,
    index: Int,
    digits: Int,
    precision: T
): T {
    return if (index == -1) {
        value / base
    } else if (index % 2 == 0) {
        powNegImpl(
            value = value,
            base = base * base,
            index = index / 2,
            digits = digits,
            precision = precision
        )
    } else {
        powNegImpl(
            value = value / base,
            base = base,
            index = index + 1,
            digits = digits,
            precision = precision
        )
    }
}

/**
 * 计算正整数指数幂，使用快速幂算法（不支持负指数）
 * Compute positive integer exponent power using fast power algorithm
 *
 * @param base 底数 / Base
 * @param index 指数（正整数） / Exponent (positive integer)
 * @param constants 数值常量提供器 / Real number constants provider
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值结果 / Power value result
 */
fun <T> pow(
    base: T,
    index: Int,
    constants: RealNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): Ret<T> where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return powSafe(
        base = base,
        index = index,
        constants = constants,
        digits = digits,
        precision = precision
    )
}

/**
 * 安全计算正整数指数幂，使用快速幂算法（不支持负指数）
 * Safely computes positive integer exponent power using fast power algorithm
 *
 * @param base 底数 / Base
 * @param index 指数（正整数） / Exponent (positive integer)
 * @param constants 数值常量提供器 / Real number constants provider
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值结果 / Power value result
 */
fun <T> powSafe(
    base: T,
    index: Int,
    constants: RealNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): Ret<T> where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return powOrNull(
        base = base,
        index = index,
        constants = constants,
        digits = digits,
        precision = precision
    )?.let { ok(it) }
        ?: Failed(
            ErrorCode.IllegalArgument,
            "负指数幂需要乘法群支持：${base.javaClass}。 / Negative exponent requires TimesGroup support: ${base.javaClass}."
        )
}

/**
 * 尝试计算正整数指数幂，负指数返回 null
 * Tries to compute positive integer exponent power; returns null for negative exponent
 *
 * @param base 底数 / Base
 * @param index 指数（正整数） / Exponent (positive integer)
 * @param constants 数值常量提供器 / Real number constants provider
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值，负指数时返回 null / Power value, or null for negative exponent
 */
fun <T> powOrNull(
    base: T,
    index: Int,
    constants: RealNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T? where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return if (index >= 1) {
        powPosImpl(
            value = constants.one,
            base = base,
            index = index,
            digits = digits,
            precision = precision
        )
    } else if (index <= -1) {
        null
    } else {
        constants.one
    }
}

/**
 * 计算整数指数幂（支持负指数），使用快速幂算法
 * Compute integer exponent power (supports negative), using fast power
 *
 * @param base 底数 / Base
 * @param index 指数 / Exponent
 * @param constants 数值常量提供器 / Real number constants provider
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值 / Power value
 */
fun <T> pow(
    base: T,
    index: Int,
    constants: RealNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T where T : TimesGroup<T>, T : RealNumber<T> {
    return if (index >= 1) {
        powPosImpl(
            value = constants.one,
            base = base,
            index = index,
            digits = digits,
            precision = precision
        )
    } else if (index <= -1) {
        powNegImpl(
            value = constants.one,
            base = base,
            index = index,
            digits = digits,
            precision = precision
        )
    } else {
        constants.one
    }
}
/**
 * 计算正整数指数幂（自动解析常量）
 * Compute positive integer exponent power (auto-resolve constants)
 *
 * @param base 底数 / Base
 * @param index 指数（正整数） / Exponent (positive integer)
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值 / Power value
 */
inline fun <reified T> pow(
    base: T,
    index: Int,
    digits: Int = base.constants.decimalDigits ?: 0,
    precision: T = base.constants.epsilon
): Ret<T> where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return resolveRealNumberConstantsSafe<T>("Pow").flatMapResolved { constants ->
        pow(
            base = base,
            index = index,
            constants = constants,
            digits = digits,
            precision = precision
        )
    }
}

/**
 * 安全计算正整数指数幂（自动解析常量）
 * Safely computes positive integer exponent power (auto-resolve constants)
 *
 * @param base 底数 / Base
 * @param index 指数（正整数） / Exponent (positive integer)
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值结果 / Power value result
 */
inline fun <reified T> powSafe(
    base: T,
    index: Int,
    digits: Int = base.constants.decimalDigits ?: 0,
    precision: T = base.constants.epsilon
): Ret<T> where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return resolveRealNumberConstantsSafe<T>("Pow").flatMapResolved { constants ->
        powSafe(
            base = base,
            index = index,
            constants = constants,
            digits = digits,
            precision = precision
        )
    }
}

/**
 * 尝试计算正整数指数幂（自动解析常量）
 * Tries to compute positive integer exponent power (auto-resolve constants)
 *
 * @param base 底数 / Base
 * @param index 指数（正整数） / Exponent (positive integer)
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值，负指数时返回 null / Power value, or null for negative exponent
 */
inline fun <reified T> powOrNull(
    base: T,
    index: Int,
    digits: Int = base.constants.decimalDigits ?: 0,
    precision: T = base.constants.epsilon
): T? where T : TimesSemiGroup<T>, T : RealNumber<T> {
    val constants = resolveRealNumberConstantsOrNull<T>("Pow") ?: return null
    return powOrNull(
        base = base,
        index = index,
        constants = constants,
        digits = digits,
        precision = precision
    )
}
/**
 * 计算整数指数幂（支持负指数，自动解析常量）
 * Compute integer exponent power (auto-resolve constants)
 *
 * @param base 底数 / Base
 * @param index 指数 / Exponent
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值 / Power value
 */
inline fun <reified T> pow(
    base: T,
    index: Int,
    digits: Int = base.constants.decimalDigits ?: 0,
    precision: T = base.constants.epsilon
): Ret<T> where T : TimesGroup<T>, T : RealNumber<T> {
    return resolveRealNumberConstantsSafe<T>("Pow").mapResolved { constants ->
        pow(
            base = base,
            index = index,
            constants = constants,
            digits = digits,
            precision = precision
        )
    }
}

/**
 * 计算浮点指数幂，通过 ln 和 exp 实现
 * Compute floating-point exponent power via ln and exp
 *
 * @param base 底数 / Base
 * @param index 浮点指数 / Floating-point exponent
 * @param constants 浮点数常量提供器 / Floating number constants provider
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值 / Power value
 */
fun <T : FloatingNumber<T>> powf(
    base: T,
    index: T,
    constants: FloatingNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T {
    val lnBase = ln(
        x = base,
        constants = constants,
        digits = digits,
        precision = precision
    )!!
    return exp(
        index = index * lnBase,
        constants = constants,
        digits = digits,
        precision = precision
    )
}
/**
 * 计算浮点指数幂（自动解析常量）
 * Compute floating-point exponent power (auto-resolve constants)
 *
 * @param base 底数 / Base
 * @param index 浮点指数 / Floating-point exponent
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 幂函数值 / Power value
 */
inline fun <reified T : FloatingNumber<T>> powf(
    base: T,
    index: T,
    digits: Int = base.constants.decimalDigits ?: 0,
    precision: T = base.constants.epsilon
): Ret<T> {
    return resolveFloatingNumberConstantsSafe<T>("Pow").mapResolved { constants ->
        powf(
            base = base,
            index = index,
            constants = constants,
            digits = digits,
            precision = precision
        )
    }
}
/**
 * 计算指数函数 exp(index)，使用泰勒级数展开
 * Compute exponential function exp(index) using Taylor series
 *
 * @param index 指数 / Exponent
 * @param constants 浮点数常量提供器 / Floating number constants provider
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 指数值 / Exponential value
 */
fun <T : FloatingNumber<T>> exp(
    index: T,
    constants: FloatingNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T {
    var value = constants.one
    var term = constants.one
    var i = constants.one
    while (true) {
        var thisItem = (term * index) / i
        thisItem = normalizeFltXScale(thisItem, digits)
        value += thisItem
        i += constants.one

        if (thisItem.abs() leq precision) {
            break
        }
        term = thisItem
    }
    return value
}
/**
 * 计算指数函数 exp(index)（自动解析常量）
 * Compute exponential function (auto-resolve constants)
 *
 * @param index 指数 / Exponent
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 指数值 / Exponential value
 */
inline fun <reified T : FloatingNumber<T>> exp(
    index: T,
    digits: Int = index.constants.decimalDigits ?: 0,
    precision: T = index.constants.epsilon
): Ret<T> {
    return resolveFloatingNumberConstantsSafe<T>("Pow").mapResolved { constants ->
        exp(
            index = index,
            constants = constants,
            digits = digits,
            precision = precision
        )
    }
}
