/**
 * FltX 幂策略
 * FltX Power Strategy
 *
 * 为 FltX 高精度浮点数提供幂运算、指数和对数计算的高精度实现。
 * 使用泰勒级数展开进行数值计算，支持指定精度位数和最大迭代次数。
 * 自然对数 ln(x)：使用变换 y = (x-1)/(x+1) 收敛到泰勒级数，
 * 公式：ln(x) = 2 * [y + y^3/3 + y^5/5 + ...]，通过预处理将 x 调整到 [1, 2) 区间优化收敛。
 * 指数函数 exp(x)：泰勒级数展开 exp(x) = 1 + x + x^2/2! + x^3/3! + ...
 * 幂函数 pow(base, index)：通过 ln 和 exp 实现，pow(base, index) = exp(index * ln(base))。
 * 边界情况：ln(x <= 0) 返回 null（对数未定义），负指数幂在整数类型时会抛出异常。
 * FltXSeriesResult 包含计算结果、迭代次数和收敛状态，便于调试和分析。
 *
 * Provides high-precision implementation of power, exponential, and logarithm operations for FltX.
 * Uses Taylor series expansion for numerical computation with configurable precision and iteration limits.
 * Natural logarithm ln(x): uses transformation y = (x-1)/(x+1) converging to Taylor series,
 * formula: ln(x) = 2 * [y + y^3/3 + y^5/5 + ...], pre-processing adjusts x to [1, 2) for optimal convergence.
 * Exponential function exp(x): Taylor series exp(x) = 1 + x + x^2/2! + x^3/3! + ...
 * Power function pow(base, index): implemented via ln and exp, pow(base, index) = exp(index * ln(base)).
 * Boundary cases: ln(x <= 0) returns null (logarithm undefined); negative integer exponent throws exception.
 * FltXSeriesResult includes computed value, iteration count, and convergence status for debugging and analysis.
 */
package fuookami.ospf.kotlin.math.ordinary

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.FltX
import java.math.RoundingMode

/**
 * FltX 级数计算结果
 * FltX series computation result
 *
 * @param value 计算结果值 / Computed value
 * @param iterations 迭代次数 / Number of iterations
 * @param converged 是否收敛 / Whether the series converged
 */
data class FltXSeriesResult(
    val value: FltX,
    val iterations: Int,
    val converged: Boolean
)

/**
 * FltX 幂运算策略
 * FltX power strategy
 *
 * 提供 FltX 高精度浮点数的幂运算、指数和对数计算。
 * Provides high-precision power, exponential, and logarithm operations for FltX.
 */
object FltXPowerStrategy {
    fun defaultPrecision(digits: Int): FltX {
        val normalizedDigits = if (digits <= 0) {
            1
        } else {
            digits
        }
        val threshold = pow(
            base = FltX.ten,
            index = -normalizedDigits,
            constants = FltX,
            digits = normalizedDigits + 1
        )
        return min(threshold, FltX.epsilon)
    }

    fun ln(
        x: FltX,
        digits: Int,
        precision: FltX = defaultPrecision(digits),
        maxIterations: Int = 8192
    ): FltX? = lnWithStats(x, digits, precision, maxIterations)?.value

    fun lnWithStats(
        x: FltX,
        digits: Int,
        precision: FltX = defaultPrecision(digits),
        maxIterations: Int = 8192
    ): FltXSeriesResult? {
        if (x <= FltX.zero) {
            return null
        }
        val normalizedDigits = if (digits <= 0) {
            1
        } else {
            digits
        }
        val scale = normalizedDigits + 1
        var m = x.withScale(scale, RoundingMode.HALF_UP)
        var k = FltX.zero
        while (m >= FltX.two) {
            m = (m / FltX.two).withScale(scale, RoundingMode.HALF_UP)
            k += FltX.one
        }
        while (m < FltX.one) {
            m = (m * FltX.two).withScale(scale, RoundingMode.HALF_UP)
            k -= FltX.one
        }
        val core = lnUnitInterval(m, scale, precision, maxIterations)
        val value = (core.value + k * FltX.lg2).withScale(scale, RoundingMode.HALF_UP)
        return FltXSeriesResult(value, core.iterations, core.converged)
    }

    private fun lnUnitInterval(
        x: FltX,
        scale: Int,
        precision: FltX,
        maxIterations: Int
    ): FltXSeriesResult {
        val y = ((x - FltX.one) / (x + FltX.one)).withScale(scale, RoundingMode.HALF_UP)
        var yPow = y
        var value = y
        var i = FltX.one
        var iterations = 0
        while (iterations < maxIterations) {
            yPow = (yPow * y * y).withScale(scale, RoundingMode.HALF_UP)
            val denominator = FltX.two * i + FltX.one
            val term = (yPow / denominator).withScale(scale, RoundingMode.HALF_UP)
            value = (value + term).withScale(scale, RoundingMode.HALF_UP)
            iterations += 1
            if (term.abs() <= precision) {
                return FltXSeriesResult((value * FltX.two).withScale(scale, RoundingMode.HALF_UP), iterations, true)
            }
            i += FltX.one
        }
        return FltXSeriesResult((value * FltX.two).withScale(scale, RoundingMode.HALF_UP), iterations, false)
    }

    fun exp(
        index: FltX,
        digits: Int,
        precision: FltX = defaultPrecision(digits),
        maxIterations: Int = 8192
    ): FltX = expWithStats(index, digits, precision, maxIterations).value

    fun expWithStats(
        index: FltX,
        digits: Int,
        precision: FltX = defaultPrecision(digits),
        maxIterations: Int = 8192
    ): FltXSeriesResult {
        val normalizedDigits = if (digits <= 0) {
            1
        } else {
            digits
        }
        val scale = normalizedDigits + 1
        var value = FltX.one.withScale(scale, RoundingMode.HALF_UP)
        var term = FltX.one.withScale(scale, RoundingMode.HALF_UP)
        var i = FltX.one
        var iterations = 0
        while (iterations < maxIterations) {
            val next = ((term * index.withScale(scale, RoundingMode.HALF_UP)) / i).withScale(scale, RoundingMode.HALF_UP)
            value = (value + next).withScale(scale, RoundingMode.HALF_UP)
            iterations += 1
            if (next.abs() <= precision) {
                return FltXSeriesResult(value, iterations, true)
            }
            term = next
            i += FltX.one
        }
        return FltXSeriesResult(value, iterations, false)
    }

    fun pow(
        base: FltX,
        index: FltX,
        digits: Int,
        precision: FltX = defaultPrecision(digits),
        maxIterations: Int = 8192
    ): FltX {
        if (index.stripTrailingZeros() eq index.round()) {
            return pow(
                base = base,
                index = index.round().toInt32().value,
                constants = FltX,
                digits = digits,
                precision = precision
            )
        }
        val lnBase = ln(base, digits, precision, maxIterations)
            ?: throw ArithmeticException("ln(base) is undefined for base: $base")
        return exp(index * lnBase, digits, precision, maxIterations)
    }
}







