/**
 * Flt64 同类项合并快捷函数
 * Flt64 Combine Terms Convenience Functions
 *
 * 提供 Flt64 多项式的同类项合并快捷函数。
 * 封装通用合并运算，自动填入 Flt64 的零值。
 * Provides Flt64 polynomial like-term combination convenience functions.
 * Wraps generic combine operations with Flt64 zero constant.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

/**
 * 合并 Flt64 线性单项式集合中的同类项
 * Combine like terms in a collection of Flt64 linear monomials
 *
 * @return 合并后的单项式列表 / Combined monomial list
 */
fun Iterable<LinearMonomial<Flt64>>.combineTerms(): List<LinearMonomial<Flt64>> {
    return combineLinearMonomials(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 合并 Flt64 线性多项式中的同类项
 * Combine like terms in a Flt64 linear polynomial
 *
 * @return 合并后的多项式 / Combined polynomial
 */
fun LinearPolynomial<Flt64>.combineTerms(): LinearPolynomial<Flt64> {
    return combineLinearTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 合并 Flt64 二次单项式集合中的同类项
 * Combine like terms in a collection of Flt64 quadratic monomials
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 合并后的单项式列表 / Combined monomial list
 */
fun Iterable<QuadraticMonomial<Flt64>>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<QuadraticMonomial<Flt64>> {
    return combineQuadraticMonomials(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

/**
 * 合并 Flt64 二次多项式中的同类项
 * Combine like terms in a Flt64 quadratic polynomial
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 合并后的多项式 / Combined polynomial
 */
fun QuadraticPolynomial<Flt64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64> {
    return combineQuadraticTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

/**
 * 合并 Flt64 规范单项式集合中的同类项
 * Combine like terms in a collection of Flt64 canonical monomials
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 合并后的单项式列表 / Combined monomial list
 */
fun Iterable<CanonicalMonomial<Flt64>>.combineCanonicalTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalMonomial<Flt64>> {
    return combineCanonicalMonomials(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

/**
 * 合并 Flt64 规范多项式中的同类项
 * Combine like terms in a Flt64 canonical polynomial
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 合并后的多项式 / Combined polynomial
 */
fun CanonicalPolynomial<Flt64>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    return combineCanonicalPolynomialTerms(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}
