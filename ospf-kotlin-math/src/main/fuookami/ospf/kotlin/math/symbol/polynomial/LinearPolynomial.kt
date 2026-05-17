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
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.operation.toCanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

import kotlin.jvm.JvmName

/**
 * 线性多项式
 * Linear Polynomial
 *
 * 表示线性多项式，形如 c₁x₁ + c₂x₂ + ... + cₙxₙ + b。
 * 线性多项式是线性单项式的线性组合加上一个常数项，
 * 在线性规划和混合整数规划中是最基本的表达式形式。
 * Represents a linear polynomial of the form c₁x₁ + c₂x₂ + ... + cₙxₙ + b.
 * A linear polynomial is a linear combination of linear monomials plus a constant term,
 * being the most fundamental expression form in linear programming and mixed-integer programming.
 *
 * @property monomials 线性单项式列表 / List of linear monomials
 * @property constant 常数项 / Constant term
 */
data class LinearPolynomial<T : Ring<T>>(
    val monomials: List<LinearMonomial<T>> = emptyList(),
    val constant: T
) : ToLinearPolynomial<T>, ToQuadraticPolynomial<T>, ToCanonicalPolynomial<T> {
    val category: Category
        get() = Linear

    override fun toLinearPolynomial(): LinearPolynomial<T> = this

    override fun toQuadraticPolynomial(): QuadraticPolynomial<T> {
        return QuadraticPolynomial(monomials.map { it.toQuadraticMonomial() }, constant)
    }

    override fun toCanonicalPolynomial(): CanonicalPolynomial<T> {
        return CanonicalPolynomial(monomials.map { it.toCanonicalMonomial() }, constant)
    }
}

/**
 * 线性多项式的负运算符
 * Negation operator for linear polynomial
 *
 * @receiver 线性多项式 / Linear polynomial
 * @return 所有项取负后的线性多项式 / Linear polynomial with all terms negated
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.unaryMinus(): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        -constant
    )
}

/**
 * 线性多项式之间的加法运算符
 * Addition operator between linear polynomials
 *
 * @receiver 左侧线性多项式 / Left-hand linear polynomial
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 合并后的线性多项式 / Combined linear polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(monomials + rhs.monomials, constant + rhs.constant)
}

/**
 * 线性多项式之间的减法运算符
 * Subtraction operator between linear polynomials
 *
 * @receiver 左侧线性多项式 / Left-hand linear polynomial
 * @param rhs 右侧线性多项式 / Right-hand linear polynomial
 * @return 差值线性多项式 / Difference linear polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        constant - rhs.constant
    )
}

/**
 * 线性多项式与标量的乘法运算符
 * Multiplication operator for linear polynomial and scalar
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 标量值 / Scalar value
 * @return 所有系数乘以标量后的线性多项式 / Linear polynomial with all coefficients multiplied by scalar
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.times(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient * rhs, it.symbol) },
        constant * rhs
    )
}

/**
 * 标量与线性多项式的乘法运算符
 * Multiplication operator for scalar and linear polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 线性多项式 / Linear polynomial
 * @return 所有系数乘以标量后的线性多项式 / Linear polynomial with all coefficients multiplied by scalar
 */
operator fun <T : Ring<T>> T.times(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return rhs * this
}

/**
 * 线性多项式与标量的除法运算符
 * Division operator for linear polynomial and scalar
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 标量值 / Scalar value
 * @return 所有系数除以标量后的线性多项式 / Linear polynomial with all coefficients divided by scalar
 */
operator fun <T : Field<T>> LinearPolynomial<T>.div(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient / rhs, it.symbol) },
        constant / rhs
    )
}

/**
 * 线性多项式与标量的加法运算符
 * Addition operator for linear polynomial and scalar
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 标量值 / Scalar value
 * @return 常数项增加标量后的线性多项式 / Linear polynomial with constant increased by scalar
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(monomials, constant + rhs)
}

/**
 * 标量与线性多项式的加法运算符
 * Addition operator for scalar and linear polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 线性多项式 / Linear polynomial
 * @return 常数项增加标量后的线性多项式 / Linear polynomial with constant increased by scalar
 */
operator fun <T : Ring<T>> T.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(rhs.monomials, this + rhs.constant)
}

/**
 * 线性多项式与标量的减法运算符
 * Subtraction operator for linear polynomial and scalar
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 标量值 / Scalar value
 * @return 常数项减少标量后的线性多项式 / Linear polynomial with constant decreased by scalar
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(monomials, constant - rhs)
}

/**
 * 标量与线性多项式的减法运算符
 * Subtraction operator for scalar and linear polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 线性多项式 / Linear polynomial
 * @return 从标量减去多项式后的线性多项式 / Linear polynomial representing scalar minus polynomial
 */
operator fun <T : Ring<T>> T.minus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
      this - rhs.constant
    )
}