/**
 * 合并同类项
 * Combine Like Terms
 *
 * 提供针对 Flt64 类型的多项式同类项合并操作的便捷封装。
 * 将通用运算封装为类型特定的简化接口，简化常见使用场景。
 * Provides convenient wrappers for polynomial like-term combination operations for Flt64 type.
 * Wraps generic operations into type-specific simplified interfaces,
 * simplifying common use cases.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// Linear Monomial/Polynomial Combine Operations (Flt64 specialized)
// ============================================================================

/**
 * Combine like terms in a linear monomial list.
 * Direct Typed operation - no Generic conversion.
 */
fun Iterable<LinearMonomial<F64>>.combineTerms(): List<LinearMonomial<F64>> {
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
fun LinearPolynomial<F64>.combineTerms(): LinearPolynomial<F64> {
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
fun Iterable<QuadraticMonomial<F64>>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<QuadraticMonomial<F64>> {
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
fun QuadraticPolynomial<F64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<F64> {
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
fun Iterable<CanonicalMonomial<F64>>.combineCanonicalTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalMonomial<F64>> {
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
fun CanonicalPolynomial<F64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<F64> {
    return this.combineCanonicalPolynomialTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}