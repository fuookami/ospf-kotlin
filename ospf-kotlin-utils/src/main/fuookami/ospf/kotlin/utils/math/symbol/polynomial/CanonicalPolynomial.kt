package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Nonlinear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial

/**
 * 标准多项式 / Canonical polynomial
 *
 * 由多个标准单项式组成的表达式
 * Expression composed of multiple canonical monomials
 *
 * @param T 系数类型
 * @param E 指数类型，默认为 Int32
 */
data class CanonicalPolynomial<T, E : Number>(
    val monomials: List<CanonicalMonomial<T, E>> = emptyList(),
    val constant: T
) {
    val category: Category
        get() = when (monomials.maxOfOrNull { it.degree } ?: 0) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }
}