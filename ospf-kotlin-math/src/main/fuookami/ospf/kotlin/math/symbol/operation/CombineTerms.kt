@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

fun Iterable<LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>.combineTerms(): List<LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    return combineLinearMonomials(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.combineTerms(): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return combineLinearTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun Iterable<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    return combineQuadraticMonomials(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return combineQuadraticTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun Iterable<CanonicalMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>.combineCanonicalTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    return combineCanonicalMonomials(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return combineCanonicalPolynomialTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}
