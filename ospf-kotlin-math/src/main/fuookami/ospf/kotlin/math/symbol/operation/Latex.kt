/**
 * LaTeX 输出
 * LaTeX Output
 *
 * 提供将多项式和不等式转换为 LaTeX 格式字符串的便捷封装。
 * 支持配置输出格式选项，适用于 Flt64 类型。
 * Provides convenient wrappers for converting polynomials and inequalities
 * to LaTeX format strings. Supports configurable output format options,
 * suitable for Flt64 type.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import java.math.BigDecimal

data class LatexOptions(
    val compact: Boolean = true,
    val showOnes: Boolean = false,
    val useCdot: Boolean = false
)

private fun formatNumber(value: Flt64): String {
    val doubleValue = value.toDouble()
    if (!doubleValue.isFinite()) {
        return value.toString()
    }
    return BigDecimal.valueOf(doubleValue).stripTrailingZeros().toPlainString()
}

private val flt64LatexOps = LatexNumberOps<F64>(
    isZero = { it == Flt64.zero },
    isOne = { it == Flt64.one },
    isNegative = { it.toDouble() < 0.0 },
    abs = { it.abs() },
    format = { formatNumber(it) }
)

fun LinearMonomial<F64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun QuadraticMonomial<F64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun CanonicalMonomial<F64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun LinearPolynomial<F64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun QuadraticPolynomial<F64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun CanonicalPolynomial<F64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

private fun Comparison.latexSymbol(): String {
    return when (this) {
        Comparison.LT -> "<"
        Comparison.LE -> "\\le"
        Comparison.EQ -> "="
        Comparison.NE -> "\\ne"
        Comparison.GE -> "\\ge"
        Comparison.GT -> ">"
    }
}

fun LinearInequality<F64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

fun QuadraticInequalityOf<F64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

fun CanonicalInequality.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}
