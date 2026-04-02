package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// Latex Operations (Ring-based, no Generic conversion)
// ============================================================================

data class LatexNumberOps<T>(
    val isZero: (T) -> Boolean,
    val isOne: (T) -> Boolean,
    val isNegative: (T) -> Boolean,
    val abs: (T) -> T,
    val format: (T) -> String
)

private data class SignedTerm(
    val body: String,
    val negative: Boolean
)

private fun Symbol.latexName(): String {
    return displayName ?: name
}

private fun mulSymbol(options: LatexOptions): String {
    return if (options.useCdot) {
        if (options.compact) {
            "\\cdot"
        } else {
            " \\cdot "
        }
    } else {
        ""
    }
}

private fun mergeTerms(
    terms: List<SignedTerm>,
    options: LatexOptions
): String {
    if (terms.isEmpty()) {
        return "0"
    }
    val plus = if (options.compact) "+" else " + "
    val minus = if (options.compact) "-" else " - "
    val builder = StringBuilder()
    for ((index, term) in terms.withIndex()) {
        if (index == 0) {
            if (term.negative) {
                builder.append("-")
            }
            builder.append(term.body)
        } else {
            builder.append(if (term.negative) minus else plus)
            builder.append(term.body)
        }
    }
    return builder.toString()
}

private fun <T> formatMonomialTerm(
    coefficient: T,
    variable: String,
    options: LatexOptions,
    ops: LatexNumberOps<T>
): String {
    val absCoefficient = ops.abs(coefficient)
    val coefficientText = if (!options.showOnes && ops.isOne(absCoefficient)) {
        ""
    } else {
        ops.format(absCoefficient)
    }
    if (coefficientText.isEmpty()) {
        return variable
    }
    val multiply = mulSymbol(options)
    return if (multiply.isEmpty()) {
        coefficientText + variable
    } else {
        coefficientText + multiply + variable
    }
}

/**
 * Convert a linear monomial to LaTeX string.
 */
fun <T> LinearMonomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    return formatMonomialTerm(
        coefficient = coefficient,
        variable = symbol.latexName(),
        options = options,
        ops = ops
    )
}

/**
 * Convert a quadratic monomial to LaTeX string.
 */
fun <T> QuadraticMonomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    if (symbol2 == null) {
        return formatMonomialTerm(
            coefficient = coefficient,
            variable = symbol1.latexName(),
            options = options,
            ops = ops
        )
    }
    val variable = if (symbol1 == symbol2) {
        "${symbol1.latexName()}^{2}"
    } else {
        val multiply = mulSymbol(options)
        if (multiply.isEmpty()) {
            symbol1.latexName() + symbol2.latexName()
        } else {
            symbol1.latexName() + multiply + symbol2.latexName()
        }
    }
    return formatMonomialTerm(
        coefficient = coefficient,
        variable = variable,
        options = options,
        ops = ops
    )
}

/**
 * Convert a canonical monomial to LaTeX string.
 */
fun <T> CanonicalMonomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    if (powers.isEmpty()) {
        return ops.format(coefficient)
    }
    val multiply = mulSymbol(options)
    val variable = powers.entries.joinToString(separator = multiply) {
        if (it.value.toInt() == 1) {
            it.key.latexName()
        } else {
            "${it.key.latexName()}^{${it.value}}"
        }
    }
    return formatMonomialTerm(
        coefficient = coefficient,
        variable = variable,
        options = options,
        ops = ops
    )
}

/**
 * Convert a linear polynomial to LaTeX string.
 */
fun <T> LinearPolynomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatexString(ops, options),
                negative = ops.isNegative(monomial.coefficient)
            )
        )
    }
    if (!ops.isZero(constant)) {
        terms.add(
            SignedTerm(
                body = ops.format(ops.abs(constant)),
                negative = ops.isNegative(constant)
            )
        )
    }
    return mergeTerms(terms, options)
}

/**
 * Convert a quadratic polynomial to LaTeX string.
 */
fun <T> QuadraticPolynomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatexString(ops, options),
                negative = ops.isNegative(monomial.coefficient)
            )
        )
    }
    if (!ops.isZero(constant)) {
        terms.add(
            SignedTerm(
                body = ops.format(ops.abs(constant)),
                negative = ops.isNegative(constant)
            )
        )
    }
    return mergeTerms(terms, options)
}

/**
 * Convert a canonical polynomial to LaTeX string.
 */
fun <T> CanonicalPolynomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatexString(ops, options),
                negative = ops.isNegative(monomial.coefficient)
            )
        )
    }
    if (!ops.isZero(constant)) {
        terms.add(
            SignedTerm(
                body = ops.format(ops.abs(constant)),
                negative = ops.isNegative(constant)
            )
        )
    }
    return mergeTerms(terms, options)
}