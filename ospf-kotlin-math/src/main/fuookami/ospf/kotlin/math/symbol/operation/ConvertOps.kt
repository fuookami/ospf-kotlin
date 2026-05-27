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
 * 将线性单项式转换为二次单项式
 * Convert a linear monomial to a quadratic monomial.
 *
 * @return 对应的二次单项式 / Corresponding quadratic monomial
 */
fun <T> LinearMonomial<T>.toQuadraticMonomial(): QuadraticMonomial<T> where T : Ring<T> {
    return QuadraticMonomial(
        coefficient = coefficient,
        symbol1 = symbol,
        symbol2 = null
    )
}

/**
 * 将线性单项式转换为规范单项式
 * Convert a linear monomial to a canonical monomial.
 *
 * @return 对应的规范单项式 / Corresponding canonical monomial
 */
fun <T> LinearMonomial<T>.toCanonicalMonomial(): CanonicalMonomial<T> where T : Ring<T> {
    return CanonicalMonomial(
        coefficient = coefficient,
        powers = mapOf(symbol to Int32.one)
    )
}

/**
 * 尝试将二次单项式降阶为线性单项式
 * Convert a quadratic monomial to a linear monomial if possible.
 *
 * @return 线性单项式，若为真正的二次项则返回 null / Linear monomial, or null if truly quadratic
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
 * 将二次单项式转换为规范单项式
 * Convert a quadratic monomial to a canonical monomial.
 *
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 对应的规范单项式 / Corresponding canonical monomial
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
 * 尝试将规范单项式降阶为线性单项式
 * Convert a canonical monomial to a linear monomial if possible.
 *
 * @return 线性单项式，若次数不为 1 则返回 null / Linear monomial, or null if degree is not 1
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
 * 尝试将规范单项式降阶为二次单项式
 * Convert a canonical monomial to a quadratic monomial if possible.
 *
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 二次单项式，若次数超过 2 则返回 null / Quadratic monomial, or null if degree exceeds 2
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
 * 将二次多项式转换为规范多项式
 * Convert a quadratic polynomial to a canonical polynomial.
 *
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 对应的规范多项式 / Corresponding canonical polynomial
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
 * 尝试将规范多项式降阶为线性多项式
 * Convert a canonical polynomial to a linear polynomial if possible.
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 线性多项式，若含有高于一次的项则返回 null / Linear polynomial, or null if higher-degree terms exist
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
 * 尝试将规范多项式降阶为二次多项式
 * Convert a canonical polynomial to a quadratic polynomial if possible.
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 二次多项式，若含有高于二次的项则返回 null / Quadratic polynomial, or null if higher-degree terms exist
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
 * 两个线性多项式相减
 * Subtract two linear polynomials.
 *
 * @param rhs 右操作数（被减数） / Right-hand side operand (subtrahend)
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @return 相减后的线性多项式 / Resulting linear polynomial after subtraction
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
 * 两个规范多项式相减
 * Subtract two canonical polynomials.
 *
 * @param rhs 右操作数（被减数） / Right-hand side operand (subtrahend)
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
 * @return 相减后的规范多项式 / Resulting canonical polynomial after subtraction
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
