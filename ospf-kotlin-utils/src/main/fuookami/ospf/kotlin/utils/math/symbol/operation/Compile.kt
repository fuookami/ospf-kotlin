package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.generic.compileEval as compileEvalGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.compileGradient as compileGradientGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

fun LinearPolynomial<Flt64>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> Flt64 {
    return toGenericLinearPolynomial().compileEvalGeneric(
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
    return toGenericQuadraticPolynomial().compileEvalGeneric(
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
    return toGenericCanonicalPolynomial().compileEvalGeneric(
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
    return toGenericLinearPolynomial().compileGradientGeneric(
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
    return toGenericQuadraticPolynomial().compileGradientGeneric(
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
    return toGenericCanonicalPolynomial().compileGradientGeneric(
        order = order,
        combineTerms = combineTerms,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}
