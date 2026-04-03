package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.unaryMinus
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.MutableCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// Mutable Linear Polynomial Combine Operations
// ============================================================================

/**
 * Combine like terms in a mutable linear polynomial in-place.
 *
 * This is the FastSum pattern: accumulate with +=, then combine once at the end.
 *
 * @param zero Zero value for the coefficient type
 * @param isZero Predicate to check if a value is zero
 */
fun <T : NumberField<T>> MutableLinearPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
) {
    val coefficientOfSymbol = LinkedHashMap<Symbol, T>()
    for (monomial in _monomials) {
        coefficientOfSymbol[monomial.symbol] =
            (coefficientOfSymbol[monomial.symbol] ?: zero) + monomial.coefficient
    }

    _monomials.clear()
    for ((symbol, coefficient) in coefficientOfSymbol) {
        if (!isZero(coefficient)) {
            _monomials.add(LinearMonomial(coefficient = coefficient, symbol = symbol))
        }
    }
}

/**
 * Add a polynomial and combine terms in one operation.
 */
fun <T : NumberField<T>> MutableLinearPolynomial<T>.addAssignAndCombine(
    rhs: LinearPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
) {
    // Add rhs monomials
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
    // Combine
    this.combineTerms(zero, isZero)
}

/**
 * Subtract a polynomial and combine terms in one operation.
 */
fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssignAndCombine(
    rhs: LinearPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
) {
    // Subtract rhs monomials
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
    // Combine
    this.combineTerms(zero, isZero)
}

// ============================================================================
// Mutable Quadratic Polynomial Combine Operations
// ============================================================================

/**
 * Combine like terms in a mutable quadratic polynomial in-place.
 *
 * @param zero Zero value for the coefficient type
 * @param isZero Predicate to check if a value is zero
 * @param symbolComparator Comparator for symbol ordering (optional)
 */
fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfKey = LinkedHashMap<Pair<Symbol, Symbol?>, T>()

    fun normalizeKey(s1: Symbol, s2: Symbol?): Pair<Symbol, Symbol?> {
        if (s2 == null) return s1 to null
        return if (comparator.compare(s1, s2) <= 0) s1 to s2 else s2 to s1
    }

    for (monomial in _monomials) {
        val key = normalizeKey(monomial.symbol1, monomial.symbol2)
        coefficientOfKey[key] = (coefficientOfKey[key] ?: zero) + monomial.coefficient
    }

    _monomials.clear()
    for ((key, coefficient) in coefficientOfKey) {
        if (!isZero(coefficient)) {
            _monomials.add(QuadraticMonomial(
                coefficient = coefficient,
                symbol1 = key.first,
                symbol2 = key.second
            ))
        }
    }
}

/**
 * Add a polynomial and combine terms in one operation.
 */
fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.addAssignAndCombine(
    rhs: QuadraticPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Add rhs monomials
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
    // Combine
    this.combineTerms(zero, isZero, symbolComparator)
}

/**
 * Subtract a polynomial and combine terms in one operation.
 */
fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssignAndCombine(
    rhs: QuadraticPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Subtract rhs monomials
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
    // Combine
    this.combineTerms(zero, isZero, symbolComparator)
}

// ============================================================================
// Mutable Canonical Polynomial Combine Operations
// ============================================================================

/**
 * Combine like terms in a mutable canonical polynomial in-place.
 *
 * Uses PowerVectorKey for optimal performance (see CanonicalOps.kt).
 *
 * @param zero Zero value for the coefficient type
 * @param isZero Predicate to check if a value is zero
 * @param symbolComparator Comparator for symbol ordering (optional)
 */
fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Use the same efficient algorithm as CanonicalOps.kt
    val combined = _monomials.combineCanonicalMonomials(zero, isZero, symbolComparator)
    _monomials.clear()
    _monomials.addAll(combined)
}

/**
 * Add a polynomial and combine terms in one operation.
 */
fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.addAssignAndCombine(
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Add rhs monomials
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
    // Combine
    this.combineTerms(zero, isZero, symbolComparator)
}

/**
 * Subtract a polynomial and combine terms in one operation.
 */
fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssignAndCombine(
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Subtract rhs monomials
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
    // Combine
    this.combineTerms(zero, isZero, symbolComparator)
}