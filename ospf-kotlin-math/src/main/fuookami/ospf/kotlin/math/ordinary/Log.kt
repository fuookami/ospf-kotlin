/**
 * 对数函数
 * Logarithm Functions
 *
 * 为浮点数类型提供自然对数和任意底数对数的高精度计算。
 * 自然对数 ln(x)：泰勒级数展开，使用变捌y = (x-1)/(x+1) 优化收敛速度，
 * 公式：ln(x) = 2 * [y + y^3/3 + y^5/5 + ...]，当 x <= 2 时直接计算，否则通过分解处理。
 * 对于 x > 2，使用公弌ln(x) = ln(m) + k * ln(2)，其丌m 圌[1, 2) 区间。
 * 任意底数对数 log(x, base)：使用换底公弌log(x, base) = ln(x) / ln(base)。
 * 边界情况：x <= 0 返回 NaN（对数未定义），base <= 0 返回 NaN。
 * 支持通过 digits 参数控制计算精度，precision 参数控制收敛阈值。
 *
 * Provides high-precision computation of natural logarithm and arbitrary-base logarithm for floating-point types.
 * Natural logarithm ln(x): Taylor series expansion using transformation y = (x-1)/(x+1) for faster convergence,
 * formula: ln(x) = 2 * [y + y^3/3 + y^5/5 + ...], direct computation when x <= 2, decomposition for larger values.
 * For x > 2, uses formula ln(x) = ln(m) + k * ln(2) where m is in [1, 2) interval.
 * Arbitrary-base logarithm log(x, base): uses change-of-base formula log(x, base) = ln(x) / ln(base).
 * Boundary cases: x <= 0 returns NaN (logarithm undefined); base <= 0 returns NaN.
 * Supports configurable precision via digits parameter and convergence threshold via precision parameter.
 */
package fuookami.ospf.kotlin.math.ordinary

import java.math.RoundingMode
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.*

/** 归一化 FltX 精度，非 FltX 类型保持不变 / Normalize FltX scale; non-FltX types remain unchanged */
@Suppress("UNCHECKED_CAST")
private fun <T : FloatingNumber<T>> normalizeFltXScale(value: T, digits: Int): T {
    return if (value is FltX) {
        value.withScale(digits, RoundingMode.HALF_UP) as T
    } else {
        value
    }
}

/**
 * 计算自然对数 ln(x)，使用泰勒级数展开
 * Compute natural logarithm ln(x) using Taylor series expansion
 *
 * @param x 真数 / Argument
 * @param constants 浮点数常量提供器 / Floating number constants provider
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 自然对数值，x <= 0 时返回 NaN / Natural logarithm value, or NaN if x <= 0
 */
fun <T : FloatingNumber<T>> ln(
    x: T,
    constants: FloatingNumberConstants<T>,
    digits: Int = constants.decimalDigits!!,
    precision: T = constants.epsilon
): T? {
    return if (x leq constants.zero) {
        constants.nan
    } else if (x leq constants.two) {
        val y = (x - constants.one) / (x + constants.one)
        var yPow = y
        var value = y
        var i = constants.one
        while (true) {
            yPow = yPow * y * y
            yPow = normalizeFltXScale(yPow, digits)
            var term = yPow / (constants.two * i + constants.one)
            term = normalizeFltXScale(term, digits)
            value += term
            i += constants.one

            if (term.abs() <= precision) {
                break
            }
        }
        value * constants.two
    } else {
        var m = x
        var k = constants.zero

        while (m >= constants.two) {
            m /= constants.two
            k += constants.one
        }
        while (m < constants.one) {
            m *= constants.two
            k -= constants.one
        }
        ln(m, constants)!! + k * constants.lg2
    }
}
/**
 * 计算自然对数 ln(x)（自动解析常量）
 * Compute natural logarithm ln(x) (auto-resolve constants)
 *
 * @param x 真数 / Argument
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 自然对数值，x <= 0 时返回 NaN / Natural logarithm value, or NaN if x <= 0
 */
inline fun <reified T : FloatingNumber<T>> ln(
    x: T,
    digits: Int = x.constants.decimalDigits!!,
    precision: T = x.constants.epsilon
): T? {
    val constants = resolveFloatingNumberConstantsOrNull<T>("Log") ?: return null
    return ln(
        x = x,
        constants = constants,
        digits = digits,
        precision = precision
    )
}

/**
 * 计算任意底数对数 log(x, base)，使用换底公式
 * Compute arbitrary-base logarithm log(x, base) using change-of-base formula
 *
 * @param x 真数 / Argument
 * @param base 对数底数 / Logarithm base
 * @param constants 浮点数常量提供器 / Floating number constants provider
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 对数值，x <= 0 或 base <= 0 时返回 NaN / Logarithm value, or NaN if x <= 0 or base <= 0
 */
fun <T : FloatingNumber<T>> log(
    x: T,
    base: T,
    constants: FloatingNumberConstants<T>,
    digits: Int = constants.decimalDigits!!,
    precision: T = constants.epsilon
): T? {
    return ln(
        x = x,
        constants = constants,
        digits = digits,
        precision = precision
    )?.let { lhs ->
        ln(
            x = base,
            constants = constants,
            digits = digits,
            precision = precision
        )?.let {
            lhs / it
        }
    } ?: constants.nan
}
/**
 * 计算任意底数对数 log(x, base)（自动解析常量）
 * Compute arbitrary-base logarithm log(x, base) (auto-resolve constants)
 *
 * @param x 真数 / Argument
 * @param base 对数底数 / Logarithm base
 * @param digits 精度位数 / Number of precision digits
 * @param precision 收敛精度阈值 / Convergence precision threshold
 * @return 对数值，x <= 0 或 base <= 0 时返回 NaN / Logarithm value, or NaN if x <= 0 or base <= 0
 */
inline fun <reified T : FloatingNumber<T>> log(
    x: T,
    base: T,
    digits: Int = x.constants.decimalDigits!!,
    precision: T = x.constants.epsilon
): T? {
    val constants = resolveFloatingNumberConstantsOrNull<T>("Log") ?: return null
    return log(
        x = x,
        base = base,
        constants = constants,
        digits = digits,
        precision = precision
    )
}
