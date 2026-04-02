package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

fun LinearPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> Flt64 {
    return compileEvalLinear(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<Flt64>) -> Flt64 {
    return compileEvalQuadratic(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<Flt64>) -> Flt64 {
    return compileEvalCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator,
        one = Flt64.one
    )
}

fun LinearPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> List<Flt64> {
    return compileGradientLinear(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<Flt64>) -> List<Flt64> {
    return compileGradientQuadratic(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<Flt64>) -> List<Flt64> {
    return compileGradientCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}