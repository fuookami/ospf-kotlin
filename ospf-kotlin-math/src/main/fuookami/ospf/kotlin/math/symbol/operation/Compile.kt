@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.*

fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<fuookami.ospf.kotlin.math.algebra.number.Flt64>) -> Flt64 {
    return compileEvalLinear(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<fuookami.ospf.kotlin.math.algebra.number.Flt64>) -> Flt64 {
    return compileEvalQuadratic(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<fuookami.ospf.kotlin.math.algebra.number.Flt64>) -> Flt64 {
    return compileEvalCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator,
        one = Flt64.one
    )
}

fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<fuookami.ospf.kotlin.math.algebra.number.Flt64>) -> List<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return compileGradientLinear(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<fuookami.ospf.kotlin.math.algebra.number.Flt64>) -> List<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return compileGradientQuadratic(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: Comparator<Symbol>? = null
): (List<fuookami.ospf.kotlin.math.algebra.number.Flt64>) -> List<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return compileGradientCanonical(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}