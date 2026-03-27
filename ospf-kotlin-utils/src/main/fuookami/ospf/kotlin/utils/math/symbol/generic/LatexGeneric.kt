package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.operation.LatexOptions

data class GenericLatexNumberOps<T>(
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
    ops: GenericLatexNumberOps<T>
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

fun <T> GenericLinearMonomial<T>.toLatex(
    ops: GenericLatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    return formatMonomialTerm(
        coefficient = coefficient,
        variable = symbol.latexName(),
        options = options,
        ops = ops
    )
}

fun <T> GenericQuadraticMonomial<T>.toLatex(
    ops: GenericLatexNumberOps<T>,
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

fun <T> GenericCanonicalMonomial<T>.toLatex(
    ops: GenericLatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    if (factors.isEmpty()) {
        return ops.format(coefficient)
    }
    val factorAmount = LinkedHashMap<Symbol, Int>()
    for (symbol in factors) {
        factorAmount[symbol] = (factorAmount[symbol] ?: 0) + 1
    }
    val multiply = mulSymbol(options)
    val variable = factorAmount.entries.joinToString(separator = multiply) {
        if (it.value == 1) {
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

fun <T> GenericLinearPolynomial<T>.toLatex(
    ops: GenericLatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatex(ops, options),
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

fun <T> GenericQuadraticPolynomial<T>.toLatex(
    ops: GenericLatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatex(ops, options),
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

fun <T> GenericCanonicalPolynomial<T>.toLatex(
    ops: GenericLatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatex(ops, options),
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
