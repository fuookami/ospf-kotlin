package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.FltX
import java.math.RoundingMode

data class FltXSeriesResult(
    val value: FltX,
    val iterations: Int,
    val converged: Boolean
)

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







