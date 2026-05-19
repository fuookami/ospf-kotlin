@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.combineLinearTerms
import fuookami.ospf.kotlin.math.symbol.operation.combineQuadraticTerms
import fuookami.ospf.kotlin.math.symbol.operation.combineCanonicalMonomials
import fuookami.ospf.kotlin.math.symbol.operation.combineCanonicalPolynomialTerms

fun Iterable<LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>.combineTerms(): List<LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val coefficientOfSymbol = LinkedHashMap<Symbol, Flt64>()
    for (monomial in this) {
        coefficientOfSymbol[monomial.symbol] =
            (coefficientOfSymbol[monomial.symbol] ?: Flt64.zero) + monomial.coefficient
    }
    return coefficientOfSymbol
        .asSequence()
        .filter { it.value != Flt64.zero }
        .map { LinearMonomial(coefficient = it.value, symbol = it.key) }
        .toList()
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
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfKey = LinkedHashMap<Pair<Symbol, Symbol?>, Flt64>()

    fun normalizeKey(s1: Symbol, s2: Symbol?): Pair<Symbol, Symbol?> {
        if (s2 == null) return s1 to null
        return if (comparator.compare(s1, s2) <= 0) s1 to s2 else s2 to s1
    }

    for (monomial in this) {
        val key = normalizeKey(monomial.symbol1, monomial.symbol2)
        coefficientOfKey[key] = (coefficientOfKey[key] ?: Flt64.zero) + monomial.coefficient
    }

    return coefficientOfKey
        .asSequence()
        .filter { it.value != Flt64.zero }
        .map { QuadraticMonomial(coefficient = it.value, symbol1 = it.key.first, symbol2 = it.key.second) }
        .toList()
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