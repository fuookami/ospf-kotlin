@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.adapter.flt64

import fuookami.ospf.kotlin.math.algebra.number.*
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
import fuookami.ospf.kotlin.math.symbol.operation.LatexNumberOps
import fuookami.ospf.kotlin.math.symbol.operation.LatexOptions
import fuookami.ospf.kotlin.math.symbol.operation.toLatexString
import java.math.BigDecimal

private fun formatNumber(value: Flt64): String {
    val doubleValue = value.toDouble()
    if (!doubleValue.isFinite()) {
        return value.toString()
    }
    return BigDecimal.valueOf(doubleValue).stripTrailingZeros().toPlainString()
}

private val flt64LatexOps = LatexNumberOps<Flt64>(
    isZero = { it == Flt64.zero },
    isOne = { it == Flt64.one },
    isNegative = { it.toDouble() < 0.0 },
    abs = { it.abs() },
    format = { formatNumber(it) }
)

fun LinearMonomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun QuadraticMonomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun CanonicalMonomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun LinearPolynomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun QuadraticPolynomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

fun CanonicalPolynomial<Flt64>.toLatex(
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

fun LinearInequality<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

fun QuadraticInequalityOf<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

fun CanonicalInequality.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

// Legacy toLatexString functions (simplified, without LatexOptions)
private fun Flt64.toLatexString(): String {
    val value = this.toDouble()
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

private fun LinearMonomial<Flt64>.toLatexStringSimple(): String {
    val coeff = coefficient
    val symbolName = symbol.displayName ?: symbol.name
    return when {
        coeff == Flt64.one -> symbolName
        coeff == -Flt64.one -> "-$symbolName"
        else -> "${coeff.toLatexString()} \\cdot $symbolName"
    }
}

private fun QuadraticMonomial<Flt64>.toLatexStringSimple(): String {
    val coeff = coefficient
    val s1Name = symbol1.displayName ?: symbol1.name
    return when {
        symbol2 == null -> {
            when {
                coeff == Flt64.one -> s1Name
                coeff == -Flt64.one -> "-$s1Name"
                else -> "${coeff.toLatexString()} \\cdot $s1Name"
            }
        }
        symbol1 == symbol2 -> {
            val s2Name = symbol2!!.displayName ?: symbol2!!.name
            when {
                coeff == Flt64.one -> "${s1Name}^2"
                coeff == -Flt64.one -> "-${s1Name}^2"
                else -> "${coeff.toLatexString()} \\cdot ${s1Name}^2"
            }
        }
        else -> {
            val s2Name = symbol2!!.displayName ?: symbol2!!.name
            when {
                coeff == Flt64.one -> "$s1Name \\cdot $s2Name"
                coeff == -Flt64.one -> "-$s1Name \\cdot $s2Name"
                else -> "${coeff.toLatexString()} \\cdot $s1Name \\cdot $s2Name"
            }
        }
    }
}

private fun CanonicalMonomial<Flt64>.toLatexStringSimple(): String {
    val coeff = coefficient
    val powerParts = powers.entries.joinToString(" \\cdot ") { (symbol, power) ->
        val symbolName = symbol.displayName ?: symbol.name
        if (power == Int32(1)) symbolName else "${symbolName}^{$power}"
    }
    return when {
        powerParts.isEmpty() -> coeff.toLatexString()
        coeff == Flt64.one -> powerParts
        coeff == -Flt64.one -> "-$powerParts"
        else -> "${coeff.toLatexString()} \\cdot $powerParts"
    }
}

fun LinearPolynomial<Flt64>.toLatexString(): String {
    if (monomials.isEmpty()) return constant.toLatexString()
    val parts = mutableListOf<String>()
    for ((index, monomial) in monomials.withIndex()) {
        val str = monomial.toLatexStringSimple()
        if (index == 0) {
            parts.add(str)
        } else {
            if (monomial.coefficient >= Flt64.zero) {
                parts.add("+ $str")
            } else {
                parts.add("- ${str.removePrefix("-")}")
            }
        }
    }
    if (constant != Flt64.zero) {
        if (constant > Flt64.zero) {
            parts.add("+ ${constant.toLatexString()}")
        } else {
            parts.add("- ${(-constant).toLatexString()}")
        }
    }
    return parts.joinToString(" ")
}

fun QuadraticPolynomial<Flt64>.toLatexString(): String {
    if (monomials.isEmpty()) return constant.toLatexString()
    val parts = mutableListOf<String>()
    for ((index, monomial) in monomials.withIndex()) {
        val str = monomial.toLatexStringSimple()
        if (index == 0) {
            parts.add(str)
        } else {
            if (monomial.coefficient >= Flt64.zero) {
                parts.add("+ $str")
            } else {
                parts.add("- ${str.removePrefix("-")}")
            }
        }
    }
    if (constant != Flt64.zero) {
        if (constant > Flt64.zero) {
            parts.add("+ ${constant.toLatexString()}")
        } else {
            parts.add("- ${(-constant).toLatexString()}")
        }
    }
    return parts.joinToString(" ")
}

fun CanonicalPolynomial<Flt64>.toLatexString(): String {
    if (monomials.isEmpty()) return constant.toLatexString()
    val parts = mutableListOf<String>()
    for ((index, monomial) in monomials.withIndex()) {
        val str = monomial.toLatexStringSimple()
        if (index == 0) {
            parts.add(str)
        } else {
            if (monomial.coefficient >= Flt64.zero) {
                parts.add("+ $str")
            } else {
                parts.add("- ${str.removePrefix("-")}")
            }
        }
    }
    if (constant != Flt64.zero) {
        if (constant > Flt64.zero) {
            parts.add("+ ${constant.toLatexString()}")
        } else {
            parts.add("- ${(-constant).toLatexString()}")
        }
    }
    return parts.joinToString(" ")
}
