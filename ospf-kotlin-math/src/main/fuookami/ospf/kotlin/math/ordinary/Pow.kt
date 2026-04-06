/**
 * 幂函数
 * Power Functions
 *
 * 为实数类型提供整数指数幂和浮点指数幂的高精度计算。
 * 整数指数幂 pow(base, index)：使用快速幂算法（二分递归），
 * 时间复杂度 O(log n)，通过平方递归减少乘法次数。
 * 正整数指数：适用于 TimesSemiGroup 类型，要求 base 支持乘法。
 * 负整数指数：适用于 TimesGroup 类型，要求 base 支持乘法和除法（逆元）。
 * 浮点指数幂 powf(base, index)：通过 ln 和 exp 实现，
 * 公式：pow(base, index) = exp(index * ln(base))。
 * 指数函数 exp(index)：泰勒级数展开 exp(x) = 1 + x + x^2/2! + x^3/3! + ...
 * 边界情况：index = 0 返回 one，正整数指数幂不支持负数 base 的负指数。
 * 支持通过 digits 和 precision 参数控制计算精度。
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

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.*
import java.math.RoundingMode

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

@Throws(IllegalArgumentException::class)
fun <T> pow(
    base: T,
    index: Int,
    constants: RealNumberConstants<T>,
    digits: Int = constants.decimalDigits ?: 0,
    precision: T = constants.epsilon
): T where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return if (index >= 1) {
        powPosImpl(
            value = constants.one,
            base = base,
            index = index,
            digits = digits,
            precision = precision
        )
    } else if (index <= -1) {
        throw IllegalArgumentException("Invalid argument for negative index exponential function: ${base.javaClass}")
    } else {
        constants.one
    }
}

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

@Suppress("UNCHECKED_CAST")
inline fun <reified T> pow(
    base: T,
    index: Int,
    digits: Int = base.constants.decimalDigits ?: 0,
    precision: T = base.constants.epsilon
): T where T : TimesSemiGroup<T>, T : RealNumber<T> {
    return pow(
        base = base,
        index = index,
        constants = resolveRealNumberConstants<T>("Pow"),
        digits = digits,
        precision = precision
    )
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> pow(
    base: T,
    index: Int,
    digits: Int = base.constants.decimalDigits ?: 0,
    precision: T = base.constants.epsilon
): T where T : TimesGroup<T>, T : RealNumber<T> {
    return pow(
        base = base,
        index = index,
        constants = resolveRealNumberConstants<T>("Pow"),
        digits = digits,
        precision = precision
    )
}

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

@Suppress("UNCHECKED_CAST")
inline fun <reified T : FloatingNumber<T>> powf(
    base: T,
    index: T,
    digits: Int = base.constants.decimalDigits ?: 0,
    precision: T = base.constants.epsilon
): T {
    return powf(
        base = base,
        index = index,
        constants = resolveFloatingNumberConstants<T>("Pow"),
        digits = digits,
        precision = precision
    )
}

@Suppress("UNCHECKED_CAST")
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
        if (thisItem is FltX) {
            thisItem = thisItem.withScale(digits, RoundingMode.HALF_UP) as T
        }
        value += thisItem
        i += constants.one

        if (thisItem.abs() leq precision) {
            break
        }
        term = thisItem
    }
    return value
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : FloatingNumber<T>> exp(
    index: T,
    digits: Int = index.constants.decimalDigits ?: 0,
    precision: T = index.constants.epsilon
): T {
    return exp(
        index = index,
        constants = resolveFloatingNumberConstants<T>("Pow"),
        digits = digits,
        precision = precision
    )
}
