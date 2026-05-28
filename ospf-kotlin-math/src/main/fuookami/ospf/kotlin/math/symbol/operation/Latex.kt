/**
 * Flt64 LaTeX 格式化
 * Flt64 LaTeX Formatting
 *
 * 提供 Flt64 多项式和不等式的 LaTeX 字符串转换。
 * 包含紧凑和简化两种格式化模式。
 * Provides LaTeX string conversion for Flt64 polynomials and inequalities.
 * Includes compact and simplified formatting modes.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*

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

/**
 * 将 Flt64 线性单项式转换为 LaTeX 字符串
 * Convert a Flt64 linear monomial to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
fun LinearMonomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

/**
 * 将 Flt64 二次单项式转换为 LaTeX 字符串
 * Convert a Flt64 quadratic monomial to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
fun QuadraticMonomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

/**
 * 将 Flt64 规范单项式转换为 LaTeX 字符串
 * Convert a Flt64 canonical monomial to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
fun CanonicalMonomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

/**
 * 将 Flt64 线性多项式转换为 LaTeX 字符串
 * Convert a Flt64 linear polynomial to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
fun LinearPolynomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

/**
 * 将 Flt64 二次多项式转换为 LaTeX 字符串
 * Convert a Flt64 quadratic polynomial to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
fun QuadraticPolynomial<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return toLatexString(flt64LatexOps, options)
}

/**
 * 将 Flt64 规范多项式转换为 LaTeX 字符串
 * Convert a Flt64 canonical polynomial to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
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

/**
 * 将 Flt64 线性不等式转换为 LaTeX 字符串
 * Convert a Flt64 linear inequality to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
fun LinearInequality<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

/**
 * 将 Flt64 二次不等式转换为 LaTeX 字符串
 * Convert a Flt64 quadratic inequality to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
fun QuadraticInequalityOf<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

/**
 * 将 Flt64 规范不等式转换为 LaTeX 字符串
 * Convert a Flt64 canonical inequality to a LaTeX string
 *
 * @param options LaTeX 格式选项 / LaTeX format options
 * @return LaTeX 字符串 / LaTeX string
 */
fun CanonicalInequality<Flt64>.toLatex(
    options: LatexOptions = LatexOptions()
): String {
    return "${lhs.toLatex(options)} ${comparison.latexSymbol()} ${rhs.toLatex(options)}"
}

// Solver-adapter formatting helper (simplified, without LatexOptions).
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

/**
 * 将 Flt64 线性多项式转换为简化 LaTeX 字符串
 * Convert a Flt64 linear polynomial to a simplified LaTeX string
 *
 * @return LaTeX 字符串 / LaTeX string
 */
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

/**
 * 将 Flt64 二次多项式转换为简化 LaTeX 字符串
 * Convert a Flt64 quadratic polynomial to a simplified LaTeX string
 *
 * @return LaTeX 字符串 / LaTeX string
 */
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

/**
 * 将 Flt64 规范多项式转换为简化 LaTeX 字符串
 * Convert a Flt64 canonical polynomial to a simplified LaTeX string
 *
 * @return LaTeX 字符串 / LaTeX string
 */
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
