/**
 * 规范单项式
 * Canonical Monomial
 *
 * 定义规范单项式的数据结构和运算。规范单项式形如 c*x₁^n₁*x₂^n₂*...，
 * 其中 c 为系数，xᵢ 为符号变量，nᵢ 为对应的幂次。
 * 是构建规范多项式的基本单元，支持任意次数的多项式。
 * Defines data structures and operations for canonical monomials.
 * A canonical monomial has the form c*x₁^n₁*x₂^n₂*..., where c is the coefficient,
 * xᵢ are symbol variables, and nᵢ are the corresponding powers.
 * It is the basic building block for canonical polynomials,
 * supporting polynomials of any degree.
 */
package fuookami.ospf.kotlin.math.symbol.monomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Nonlinear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol

data class CanonicalMonomial<T : Ring<T>>(
    val coefficient: T,
    val powers: Map<Symbol, Int32> = emptyMap()
) {
    constructor(
        coefficient: T,
        factors: List<Symbol>
    ) : this(
        coefficient = coefficient,
        powers = factors.groupingBy { it }.eachCount().mapValues { Int32(it.value) }
    )

    val factors: List<Symbol>
        get() = powers.entries
            .flatMap { (symbol, exp) -> List(exp.toInt()) { symbol } }

    val degree: Int
        get() = powers.values.sum().toInt()

    val category: Category
        get() = when (degree) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }
}

operator fun <T : Ring<T>> CanonicalMonomial<T>.unaryMinus(): CanonicalMonomial<T> {
    return copy(coefficient = -coefficient)
}

operator fun <T : Ring<T>> CanonicalMonomial<T>.times(rhs: T): CanonicalMonomial<T> {
    return copy(coefficient = coefficient * rhs)
}

operator fun <T : Field<T>> CanonicalMonomial<T>.div(rhs: T): CanonicalMonomial<T> {
    return copy(coefficient = coefficient / rhs)
}

