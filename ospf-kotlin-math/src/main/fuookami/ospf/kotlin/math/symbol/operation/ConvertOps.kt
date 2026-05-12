/**
 * 转换运算
 * Conversion Operations
 *
 * 提供单项式和多项式类型转换的核心实现。
 * 包括升阶转换（线性到二次到规范）和降阶转换（规范到二次到线性）。
 * Provides core implementation for monomial and polynomial type conversions.
 * Includes promotion conversions (linear to quadratic to canonical)
 * and demotion conversions (canonical to quadratic to linear).
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// Conversion Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * Convert a linear monomial to a quadratic monomial.
 */
fun <T> LinearMonomial<T>.toQuadraticMonomial(): QuadraticMonomial<T> where T : Ring<T> {
    return QuadraticMonomial(
        coefficient = coefficient,
        symbol1 = symbol,
        symbol2 = null
    )
}

/**
 * Convert a linear monomial to a canonical monomial.
 */
fun <T> LinearMonomial<T>.toCanonicalMonomial(): CanonicalMonomial<T> where T : Ring<T> {
    return CanonicalMonomial(
        coefficient = coefficient,
        powers = mapOf(symbol to Int32.one)
    )
}

/**
 * Convert a quadratic monomial to a linear monomial if possible.
 */
fun <T> QuadraticMonomial<T>.toLinearMonomialOrNull(): LinearMonomial<T>? where T : Ring<T> {
    if (isQuadratic) {
        return null
    }
    return LinearMonomial(
        coefficient = coefficient,
        symbol = symbol1
    )
}

/**
 * Convert a quadratic monomial to a canonical monomial.
 */
fun <T> QuadraticMonomial<T>.toCanonicalMonomial(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalMonomial<T> where T : Ring<T> {
    val powers = if (symbol2 == null) {
        mapOf(symbol1 to Int32.one)
    } else if (symbol1 == symbol2) {
        mapOf(symbol1 to Int32(2))
    } else {
        mapOf(symbol1 to Int32.one, symbol2 to Int32.one)
    }
    return CanonicalMonomial(
        coefficient = coefficient,
        powers = powers
    )
}

/**
 * Convert a canonical monomial to a linear monomial if possible.
 */
fun <T> CanonicalMonomial<T>.toLinearMonomialOrNull(): LinearMonomial<T>? where T : Ring<T> {
    if (degree != 1) {
        return null
    }
    val entry = powers.entries.firstOrNull { it.value.toInt() == 1 } ?: return null
    return LinearMonomial(
        coefficient = coefficient,
        symbol = entry.key
    )
}

/**
 * Convert a canonical monomial to a quadratic monomial if possible.
 */
fun <T> CanonicalMonomial<T>.toQuadraticMonomialOrNull(
    symbolComparator: Comparator<Symbol>? = null
): QuadraticMonomial<T>? where T : Ring<T> {
    if (degree == 1) {
        val entry = powers.entries.firstOrNull { it.value.toInt() == 1 } ?: return null
        return QuadraticMonomial(
            coefficient = coefficient,
            symbol1 = entry.key,
            symbol2 = null
        )
    }
    if (degree != 2) {
        return null
    }
    val entries = powers.entries.toList()
    if (entries.size == 1 && entries[0].value.toInt() == 2) {
        return QuadraticMonomial(
            coefficient = coefficient,
            symbol1 = entries[0].key,
            symbol2 = entries[0].key
        )
    } else if (entries.size == 2 && entries.all { it.value.toInt() == 1 }) {
        val comparator = symbolComparator ?: defaultSymbolComparator
        val sortedEntries = entries.sortedWith { a, b -> comparator.compare(a.key, b.key) }
        return QuadraticMonomial(
            coefficient = coefficient,
            symbol1 = sortedEntries[0].key,
            symbol2 = sortedEntries[1].key
        )
    }
    return null
}

/**
 * Convert a quadratic polynomial to a canonical polynomial.
 */
fun <T> QuadraticPolynomial<T>.toCanonicalPolynomial(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return CanonicalPolynomial(
        monomials = monomials.map { it.toCanonicalMonomial(symbolComparator) },
        constant = constant
    )
}

/**
 * Convert a canonical polynomial to a linear polynomial if possible.
 */
fun <T> CanonicalPolynomial<T>.toLinearPolynomialOrNull(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T>? where T : Ring<T> {
    val linearMonomials = ArrayList<LinearMonomial<T>>(monomials.size)
    var canonicalConstant = constant
    for (monomial in monomials) {
        when (monomial.degree) {
            0 -> {
                canonicalConstant += monomial.coefficient
            }
            1 -> {
                val entry = monomial.powers.entries.firstOrNull { it.value.toInt() == 1 } ?: return null
                linearMonomials.add(
                    LinearMonomial(
                        coefficient = monomial.coefficient,
                        symbol = entry.key
                    )
                )
            }
            else -> {
                return null
            }
        }
    }
    return LinearPolynomial(
        monomials = linearMonomials,
        constant = canonicalConstant
    ).combineLinearTerms(zero, isZero)
}

/**
 * Convert a canonical polynomial to a quadratic polynomial if possible.
 */
fun <T> CanonicalPolynomial<T>.toQuadraticPolynomialOrNull(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T>? where T : Ring<T> {
    val quadraticMonomials = ArrayList<QuadraticMonomial<T>>(monomials.size)
    var canonicalConstant = constant
    for (monomial in monomials) {
        when (monomial.degree) {
            0 -> {
                canonicalConstant += monomial.coefficient
            }
            1, 2 -> {
                quadraticMonomials.add(
                    monomial.toQuadraticMonomialOrNull(symbolComparator) ?: return null
                )
            }
            else -> {
                return null
            }
        }
    }
    return QuadraticPolynomial(
        monomials = quadraticMonomials,
        constant = canonicalConstant
    ).combineQuadraticTerms(zero, isZero, symbolComparator)
}

// ============================================================================
// Subtraction Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * Subtract two linear polynomials.
 */
fun <T> LinearPolynomial<T>.subtractLinear(
    rhs: LinearPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    return LinearPolynomial(
        monomials = monomials + rhs.monomials.map {
            LinearMonomial(
                coefficient = -it.coefficient,
                symbol = it.symbol
            )
        },
        constant = constant - rhs.constant
    ).combineLinearTerms(zero, isZero)
}

/**
 * Subtract two canonical polynomials.
 */
fun <T> CanonicalPolynomial<T>.subtractCanonical(
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return CanonicalPolynomial(
        monomials = monomials + rhs.monomials.map {
            CanonicalMonomial(
                coefficient = -it.coefficient,
                powers = it.powers
            )
        },
        constant = constant - rhs.constant
    ).combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}
