package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.generic.combineTerms as combineGenericCanonicalTerms
import fuookami.ospf.kotlin.utils.math.symbol.generic.combineTerms as combineGenericLinearTerms
import fuookami.ospf.kotlin.utils.math.symbol.generic.combineTerms as combineGenericQuadraticTerms
import fuookami.ospf.kotlin.utils.math.symbol.generic.combineCanonicalTerms as combineGenericCanonicalMonomialTerms
import fuookami.ospf.kotlin.utils.math.symbol.generic.combineLinearTerms as combineGenericLinearMonomialTerms
import fuookami.ospf.kotlin.utils.math.symbol.generic.combineQuadraticTerms as combineGenericQuadraticMonomialTerms
import fuookami.ospf.kotlin.utils.math.symbol.generic.toCanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toQuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

fun Iterable<LinearMonomial<Flt64>>.combineTerms(): List<LinearMonomial<Flt64>> {
    return map { it.toGenericLinearMonomial() }
        .combineGenericLinearMonomialTerms(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .map { it.toLinearMonomial() }
}

fun LinearPolynomial<Flt64>.combineTerms(): LinearPolynomial<Flt64> {
    return this.toGenericLinearPolynomial()
        .combineGenericLinearTerms(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .toLinearPolynomial()
}

fun Iterable<QuadraticMonomial<Flt64>>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<QuadraticMonomial<Flt64>> {
    return map { it.toGenericQuadraticMonomial() }
        .combineGenericQuadraticMonomialTerms(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            symbolComparator = symbolComparator
        )
        .map { it.toQuadraticMonomial() }
}

fun QuadraticPolynomial<Flt64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64> {
    return this.toGenericQuadraticPolynomial()
        .combineGenericQuadraticTerms(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            symbolComparator = symbolComparator
        )
        .toQuadraticPolynomial()
}

fun Iterable<CanonicalMonomial<Flt64>>.combineCanonicalTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalMonomial<Flt64>> {
    return map { it.toGenericCanonicalMonomial() }
        .combineGenericCanonicalMonomialTerms(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            symbolComparator = symbolComparator
        )
        .map { it.toCanonicalMonomial() }
}

fun CanonicalPolynomial<Flt64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    return this.toGenericCanonicalPolynomial()
        .combineGenericCanonicalTerms(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            symbolComparator = symbolComparator
        )
        .toCanonicalPolynomial()
}
