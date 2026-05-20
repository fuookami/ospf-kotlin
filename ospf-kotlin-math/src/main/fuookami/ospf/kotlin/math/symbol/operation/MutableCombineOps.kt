/**
 * 可变合并运算
 * Mutable Combine Operations
 *
 * 提供可变多项式的原地同类项合并操作。
 * 支持快速累积模式：先使甌+= 累积，最后一次性合并。
 * Provides in-place like-term combination operations for mutable polynomials.
 * Supports FastSum pattern: accumulate with +=, then combine once at the end.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

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
    val combinedMonomials = _monomials.combineLinearMonomials(zero, isZero)
    _monomials.clear()
    _monomials.addAll(combinedMonomials)
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
    val combinedMonomials = _monomials.combineQuadraticMonomials(zero, isZero, symbolComparator)
    _monomials.clear()
    _monomials.addAll(combinedMonomials)
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
