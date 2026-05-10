/**
 * 线性单项式
 * Linear Monomial
 *
 * 定义线性单项式的数据结构和运算。线性单项式形如 c*x，
 * 其中 c 为系数，x 为符号变量。是构建线性多项式的基本单元。
 * Defines data structures and operations for linear monomials.
 * A linear monomial has the form c*x, where c is the coefficient
 * and x is the symbol variable. It is the basic building block for linear polynomials.
 */
package fuookami.ospf.kotlin.math.symbol.monomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.operator.Abs
import fuookami.ospf.kotlin.math.operator.abs

/**
 * 线性单项式
 * Linear Monomial
 *
 * 表示线性单项式，形如 c*x，其中 c 为系数，x 为符号变量。
 * 线性单项式是构建线性多项式的基本单元，表示一个线性项。
 * Represents a linear monomial of the form c*x, where c is the coefficient
 * and x is the symbol variable. Linear monomials are the basic building blocks
 * for linear polynomials, representing a single linear term.
 *
 * @property coefficient 系数 / The coefficient
 * @property symbol 符号变量 / The symbol variable
 */
data class LinearMonomial<T : Ring<T>>(
    val coefficient: T,
    val symbol: Symbol
) : ToLinearPolynomial<T>, ToQuadraticPolynomial<T> {
    val category: Category
        get() = Linear

    override fun toLinearPolynomial(): LinearPolynomial<T> {
        return LinearPolynomial(listOf(this), coefficient - coefficient)
    }

    override fun toQuadraticPolynomial(): QuadraticPolynomial<T> {
        return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol)), coefficient - coefficient)
    }
}

/**
 * 获取零值
 * Gets zero value
 *
 * 通过减法运算获取指定值的零值。
 * Obtains the zero value of the specified type through subtraction.
 *
 * @param value 参考值 / Reference value
 * @return 零值 / Zero value
 */
private fun <T : Ring<T>> zeroOf(value: T): T {
    return value - value
}

/**
 * 线性单项式的负运算符
 * Negation operator for linear monomial
 *
 * @receiver 线性单项式 / Linear monomial
 * @return 系数取负后的线性单项式 / Linear monomial with negated coefficient
 */
operator fun <T : Ring<T>> LinearMonomial<T>.unaryMinus(): LinearMonomial<T> {
    return LinearMonomial(-coefficient, symbol)
}

/**
 * 线性单项式与标量的乘法运算符
 * Multiplication operator for linear monomial and scalar
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 标量值 / Scalar value
 * @return 系数乘以标量后的线性单项式 / Linear monomial with coefficient multiplied by scalar
 */
operator fun <T : Ring<T>> LinearMonomial<T>.times(rhs: T): LinearMonomial<T> {
    return LinearMonomial(coefficient * rhs, symbol)
}

/**
 * 线性单项式与标量的除法运算符
 * Division operator for linear monomial and scalar
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 标量值 / Scalar value
 * @return 系数除以标量后的线性单项式 / Linear monomial with coefficient divided by scalar
 */
operator fun <T : Field<T>> LinearMonomial<T>.div(rhs: T): LinearMonomial<T> {
    return LinearMonomial(coefficient / rhs, symbol)
}

/**
 * 线性单项式之间的乘法运算符
 * Multiplication operator between linear monomials
 *
 * 两个线性单项式相乘，结果为二次单项式。
 * Multiplication of two linear monomials results in a quadratic monomial.
 *
 * @receiver 左侧线性单项式 / Left-hand linear monomial
 * @param rhs 右侧线性单项式 / Right-hand linear monomial
 * @return 二次单项式 / Quadratic monomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.times(rhs: LinearMonomial<T>): QuadraticMonomial<T> {
    return QuadraticMonomial(coefficient * rhs.coefficient, symbol, rhs.symbol)
}

/**
 * 线性单项式与线性多项式的乘法运算符
 * Multiplication operator between linear monomial and linear polynomial
 *
 * 线性单项式与线性多项式相乘，结果为二次多项式。
 * Multiplication of linear monomial and linear polynomial results in a quadratic polynomial.
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 线性多项式 / Linear polynomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.times(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val quadraticTerms = rhs.monomials.map { this * it }
    val linearTerm = QuadraticMonomial.linear(coefficient * rhs.constant, symbol)
    return QuadraticPolynomial(quadraticTerms + linearTerm, zeroOf(coefficient))
}

/**
 * 线性多项式与线性单项式的乘法运算符
 * Multiplication operator between linear polynomial and linear monomial
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 线性单项式 / Linear monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.times(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    val quadraticTerms = monomials.map { it * rhs }
    val linearTerm = QuadraticMonomial.linear(constant * rhs.coefficient, rhs.symbol)
    return QuadraticPolynomial(quadraticTerms + linearTerm, zeroOf(constant))
}

/**
 * 线性单项式之间的加法运算符
 * Addition operator between linear monomials
 *
 * @receiver 左侧线性单项式 / Left-hand linear monomial
 * @param rhs 右侧线性单项式 / Right-hand linear monomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.plus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this, rhs), zeroOf(coefficient))
}

/**
 * 线性单项式与线性多项式的加法运算符
 * Addition operator between linear monomial and linear polynomial
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 线性多项式 / Linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this) + rhs.monomials, rhs.constant)
}

/**
 * 线性多项式与线性单项式的加法运算符
 * Addition operator between linear polynomial and linear monomial
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 线性单项式 / Linear monomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(monomials + rhs, constant)
}

/**
 * 线性单项式之间的减法运算符
 * Subtraction operator between linear monomials
 *
 * @receiver 左侧线性单项式 / Left-hand linear monomial
 * @param rhs 右侧线性单项式 / Right-hand linear monomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.minus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this, -rhs), zeroOf(coefficient))
}

/**
 * 线性单项式与线性多项式的减法运算符
 * Subtraction operator between linear monomial and linear polynomial
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 线性多项式 / Linear polynomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.minus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this) + rhs.monomials.map { -it }, -rhs.constant)
}

/**
 * 线性多项式与线性单项式的减法运算符
 * Subtraction operator between linear polynomial and linear monomial
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 线性单项式 / Linear monomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(monomials + (-rhs), constant)
}

/**
 * 线性单项式与标量的加法运算符
 * Addition operator between linear monomial and scalar
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 标量值 / Scalar value
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.plus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this), rhs)
}

/**
 * 标量与线性单项式的加法运算符
 * Addition operator between scalar and linear monomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 线性单项式 / Linear monomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> T.plus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(rhs), this)
}

/**
 * 线性单项式与标量的减法运算符
 * Subtraction operator between linear monomial and scalar
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 标量值 / Scalar value
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.minus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this), -rhs)
}

/**
 * 标量与线性单项式的减法运算符
 * Subtraction operator between scalar and linear monomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 线性单项式 / Linear monomial
 * @return 线性多项式 / Linear polynomial
 */
operator fun <T : Ring<T>> T.minus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(-rhs), this)
}

/**
 * 线性单项式的绝对值
 * Absolute value of linear monomial
 *
 * 返回系数取绝对值后的线性单项式。
 * Returns the linear monomial with the coefficient's absolute value.
 *
 * @receiver 线性单项式 / Linear monomial
 * @return 系数取绝对值后的线性单项式 / Linear monomial with absolute coefficient
 */
fun <T> LinearMonomial<T>.abs(): LinearMonomial<T> where T : NumberField<T>, T : Abs<T> {
    return LinearMonomial(abs(coefficient), symbol)
}

/**
 * 线性单项式的倒数
 * Reciprocal of linear monomial
 *
 * 返回规范单项式形式的倒数，幂次为-1。
 * Returns the reciprocal in canonical monomial form, with power of -1.
 *
 * @receiver 线性单项式 / Linear monomial
 * @return 规范单项式形式的倒数 / Reciprocal in canonical monomial form
 */
fun <T> LinearMonomial<T>.reciprocal(): CanonicalMonomial<T> where T : Field<T>, T : TimesGroup<T> {
    return CanonicalMonomial(coefficient.reciprocal(), mapOf(symbol to Int32(-1)))
}

/**
 * 标量与线性单项式的乘法运算符
 * Multiplication operator between scalar and linear monomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 线性单项式 / Linear monomial
 * @return 线性单项式 / Linear monomial
 */
operator fun <T : Ring<T>> T.times(rhs: LinearMonomial<T>): LinearMonomial<T> {
    return LinearMonomial(this * rhs.coefficient, rhs.symbol)
}
