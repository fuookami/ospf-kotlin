/**
 * 线性多项式
 * Linear Polynomial
 *
 * 定义线性多项式的数据结构和运算。线性多项式是线性单项式的线性组合，
 * 形如 c₁x₁ + c₂x₂ + ... + cₙxₙ + b，其中 cᵢ 为系数，xᵢ 为符号变量，b 为常数项。
 * 在线性规划和混合整数规划中广泛使用。
 * Defines data structures and operations for linear polynomials.
 * A linear polynomial is a linear combination of linear monomials,
 * in the form c₁x₁ + c₂x₂ + ... + cₙxₙ + b, where cᵢ are coefficients,
 * xᵢ are symbol variables, and b is the constant term.
 * Widely used in linear programming and mixed-integer programming.
 */
package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial

data class LinearPolynomial<T>(
    val monomials: List<LinearMonomial<T>> = emptyList(),
    val constant: T
) {
    val category: Category
        get() = Linear
}

operator fun <T : Ring<T>> LinearPolynomial<T>.unaryMinus(): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        -constant
    )
}

operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(monomials + rhs.monomials, constant + rhs.constant)
}

operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        constant - rhs.constant
    )
}

operator fun <T : Ring<T>> LinearPolynomial<T>.times(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient * rhs, it.symbol) },
        constant * rhs
    )
}

operator fun <T : Ring<T>> T.times(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return rhs * this
}

operator fun <T : Field<T>> LinearPolynomial<T>.div(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient / rhs, it.symbol) },
        constant / rhs
    )
}

operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(monomials, constant + rhs)
}

operator fun <T : Ring<T>> T.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(rhs.monomials, this + rhs.constant)
}

operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(monomials, constant - rhs)
}

operator fun <T : Ring<T>> T.minus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        this - rhs.constant
    )
}

