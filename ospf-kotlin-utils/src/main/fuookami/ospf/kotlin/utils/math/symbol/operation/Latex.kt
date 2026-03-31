package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.generic.GenericLatexNumberOps
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toLatex as toLatexGeneric
import fuookami.ospf.kotlin.utils.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
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

private val flt64LatexOps = GenericLatexNumberOps<Flt64>(
    isZero = { it == Flt64.zero },
    isOne = { it == Flt64.one },
    isNegative = { it.toDouble() < 0.0 },
    abs = { it.abs() },
    format = { formatNumber(it) }
)

fun LinearMonomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toGenericLinearMonomial().toLatexGeneric(flt64LatexOps, options)
}

fun QuadraticMonomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toGenericQuadraticMonomial().toLatexGeneric(flt64LatexOps, options)
}

fun CanonicalMonomial<Flt64, Int32>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toGenericCanonicalMonomial().toLatexGeneric(flt64LatexOps, options)
}

fun LinearPolynomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toGenericLinearPolynomial().toLatexGeneric(flt64LatexOps, options)
}

fun QuadraticPolynomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toGenericQuadraticPolynomial().toLatexGeneric(flt64LatexOps, options)
}

fun CanonicalPolynomial<Flt64, Int32>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toGenericCanonicalPolynomial().toLatexGeneric(flt64LatexOps, options)
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

fun LinearInequality.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

fun QuadraticInequality.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

fun CanonicalInequality.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}