/**
 * 二次多项式
 * Quadratic Polynomial
 *
 * 定义二次多项式的数据结构和运算。二次多项式是二次单项式的线性组合，
 * 形如 Σcᵢⱼxᵢxⱼ + Σcₖxₖ + b，其中 cᵢⱼ、cₖ 为系数，xᵢ、xⱼ、xₖ 为符号变量，b 为常数项。
 * 在二次规划和凸优化中广泛使用。
 * Defines data structures and operations for quadratic polynomials.
 * A quadratic polynomial is a linear combination of quadratic monomials,
 * in the form Σcᵢⱼxᵢxⱼ + Σcₖxₖ + b, where cᵢⱼ, cₖ are coefficients,
 * xᵢ, xⱼ, xₖ are symbol variables, and b is the constant term.
 * Widely used in quadratic programming and convex optimization.
 */
package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial

data class QuadraticPolynomial<T>(
    val monomials: List<QuadraticMonomial<T>> = emptyList(),
    val constant: T
) {
    val category: Category
        get() = if (monomials.any { it.isQuadratic }) Quadratic else Linear
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.unaryMinus(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        -constant
    )
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + QuadraticMonomial.linear(rhs.coefficient, rhs.symbol), constant)
}

operator fun <T : Ring<T>> LinearMonomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol)) + rhs.monomials, rhs.constant)
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + QuadraticMonomial.linear(-rhs.coefficient, rhs.symbol), constant)
}

operator fun <T : Ring<T>> LinearMonomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        listOf(QuadraticMonomial.linear(coefficient, symbol)) +
                rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        -rhs.constant
    )
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + rhs.monomials, constant + rhs.constant)
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials + rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        constant - rhs.constant
    )
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(monomials + lifted, constant + rhs.constant)
}

operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(lifted + rhs.monomials, constant + rhs.constant)
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(-it.coefficient, it.symbol) }
    return QuadraticPolynomial(monomials + lifted, constant - rhs.constant)
}

operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(
        lifted + rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        constant - rhs.constant
    )
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.times(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(it.coefficient * rhs, it.symbol1, it.symbol2) },
        constant * rhs
    )
}

operator fun <T : Ring<T>> T.times(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return rhs * this
}

operator fun <T : Field<T>> QuadraticPolynomial<T>.div(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(it.coefficient / rhs, it.symbol1, it.symbol2) },
        constant / rhs
    )
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials, constant + rhs)
}

operator fun <T : Ring<T>> T.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(rhs.monomials, this + rhs.constant)
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials, constant - rhs)
}

operator fun <T : Ring<T>> T.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        this - rhs.constant
    )
}

