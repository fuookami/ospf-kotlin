package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// Linear Monomial/Polynomial Combine Operations (Flt64 specialized)
// ============================================================================

/**
 * Combine like terms in a linear monomial list.
 * Direct Typed operation - no Generic conversion.
 */
fun Iterable<LinearMonomial<Flt64>>.combineTerms(): List<LinearMonomial<Flt64>> {
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

/**
 * Combine like terms in a linear polynomial.
 * Uses Typed operation from LinearQuadraticOps.kt.
 */
fun LinearPolynomial<Flt64>.combineTerms(): LinearPolynomial<Flt64> {
    return this.combineLinearTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

// ============================================================================
// Quadratic Monomial/Polynomial Combine Operations (Flt64 specialized)
// ============================================================================

/**
 * Combine like terms in a quadratic monomial list.
 * Direct Typed operation - no Generic conversion.
 */
fun Iterable<QuadraticMonomial<Flt64>>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<QuadraticMonomial<Flt64>> {
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

/**
 * Combine like terms in a quadratic polynomial.
 * Uses Typed operation from LinearQuadraticOps.kt.
 */
fun QuadraticPolynomial<Flt64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64> {
    return this.combineQuadraticTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

// ============================================================================
// Canonical Monomial/Polynomial Combine Operations (Flt64 specialized)
// ============================================================================

/**
 * Combine like terms in a canonical monomial list.
 * Uses Typed operation from CanonicalOps.kt.
 */
fun Iterable<CanonicalMonomial<Flt64>>.combineCanonicalTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalMonomial<Flt64>> {
    return this.combineCanonicalMonomials(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

/**
 * Combine like terms in a canonical polynomial.
 * Uses Typed operation from CanonicalOps.kt.
 */
fun CanonicalPolynomial<Flt64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    return this.combineCanonicalPolynomialTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}