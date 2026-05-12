/**
 * 二次多项式
 * Quadratic Polynomial
 *
 * 定义二次多项式的数据结构和运算。二次多项式是二次单项式的线性组合，
 * 形如 Σc??x?x? + Σc?x? + b，其中 c??、c? 为系数，x?、x?、x? 为符号变量，b 为常数项。
 * 在二次规划和凸优化中广泛使用。
 * Defines data structures and operations for quadratic polynomials.
 * A quadratic polynomial is a linear combination of quadratic monomials,
 * in the form Σc??x?x? + Σc?x? + b, where c??, c? are coefficients,
 * x?, x?, x? are symbol variables, and b is the constant term.
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
import fuookami.ospf.kotlin.math.symbol.operation.ToQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.TryToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.toCanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

/**
 * 二次多项式
 * Quadratic Polynomial
 *
 * 表示二次多项式，形如 Σc??x?x? + Σc?x? + b。
 * 二次多项式是二次单项式的线性组合加上一个常数项，
 * 在二次规划和凸优化问题中广泛使用。
 * Represents a quadratic polynomial of the form Σc??x?x? + Σc?x? + b.
 * A quadratic polynomial is a linear combination of quadratic monomials plus a constant term,
 * widely used in quadratic programming and convex optimization problems.
 *
 * @property monomials 二次单项式列表 / List of quadratic monomials
 * @property constant 常数项 / Constant term
 */
data class QuadraticPolynomial<T : Ring<T>>(
    val monomials: List<QuadraticMonomial<T>> = emptyList(),
    val constant: T
) : ToQuadraticPolynomial<T>, ToCanonicalPolynomial<T>, TryToLinearPolynomial<T> {
    /**
     * 表达式类型分类
     * Expression type category
     *
     * 如果包含真正的二次项（symbol2 不为 null），则返回 Quadratic；否则返回 Linear。
     * Returns Quadratic if there are true quadratic terms (symbol2 not null); otherwise returns Linear.
     */
    val category: Category
        get() = if (monomials.any { it.isQuadratic }) Quadratic else Linear

    override fun toQuadraticPolynomial(): QuadraticPolynomial<T> = this

    override fun toCanonicalPolynomial(): CanonicalPolynomial<T> {
        return CanonicalPolynomial(monomials.map { it.toCanonicalMonomial() }, constant)
    }

    override fun toLinearPolynomialOrNull(): LinearPolynomial<T>? {
        if (monomials.any { it.isQuadratic }) return null
        return LinearPolynomial(
            monomials = monomials.map { LinearMonomial(it.coefficient, it.symbol1) },
            constant = constant
        )
    }
}

/**
 * 二次多项式的负运算符
 * Negation operator for quadratic polynomial
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @return 所有项取负后的二次多项式 / Quadratic polynomial with all terms negated
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.unaryMinus(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        -constant
    )
}

/**
 * 二次多项式与线性单项式的加法运算符
 * Addition operator between quadratic polynomial and linear monomial
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 线性单项式 / Linear monomial
 * @return 合并后的二次多项式 / Combined quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + QuadraticMonomial.linear(rhs.coefficient, rhs.symbol), constant)
}

/**
 * 线性单项式与二次多项式的加法运算符
 * Addition operator between linear monomial and quadratic polynomial
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 合并后的二次多项式 / Combined quadratic polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol)) + rhs.monomials, rhs.constant)
}

/**
 * 二次多项式与线性单项式的减法运算符
 * Subtraction operator between quadratic polynomial and linear monomial
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 线性单项式 / Linear monomial
 * @return 差值二次多项式 / Difference quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + QuadraticMonomial.linear(-rhs.coefficient, rhs.symbol), constant)
}

/**
 * 线性单项式与二次多项式的减法运算符
 * Subtraction operator between linear monomial and quadratic polynomial
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 差值二次多项式 / Difference quadratic polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        listOf(QuadraticMonomial.linear(coefficient, symbol)) +
                rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        -rhs.constant
    )
}

/**
 * 二次多项式之间的加法运算符
 * Addition operator between quadratic polynomials
 *
 * @receiver 左侧二次多项式 / Left-hand quadratic polynomial
 * @param rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @return 合并后的二次多项式 / Combined quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + rhs.monomials, constant + rhs.constant)
}

/**
 * 二次多项式之间的减法运算符
 * Subtraction operator between quadratic polynomials
 *
 * @receiver 左侧二次多项式 / Left-hand quadratic polynomial
 * @param rhs 右侧二次多项式 / Right-hand quadratic polynomial
 * @return 差值二次多项式 / Difference quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials + rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        constant - rhs.constant
    )
}

/**
 * 二次多项式与线性多项式的加法运算符
 * Addition operator between quadratic polynomial and linear polynomial
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 线性多项式 / Linear polynomial
 * @return 合并后的二次多项式 / Combined quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(monomials + lifted, constant + rhs.constant)
}

/**
 * 线性多项式与二次多项式的加法运算符
 * Addition operator between linear polynomial and quadratic polynomial
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 合并后的二次多项式 / Combined quadratic polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(lifted + rhs.monomials, constant + rhs.constant)
}

/**
 * 二次多项式与线性多项式的减法运算符
 * Subtraction operator between quadratic polynomial and linear polynomial
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 线性多项式 / Linear polynomial
 * @return 差值二次多项式 / Difference quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(-it.coefficient, it.symbol) }
    return QuadraticPolynomial(monomials + lifted, constant - rhs.constant)
}

/**
 * 线性多项式与二次多项式的减法运算符
 * Subtraction operator between linear polynomial and quadratic polynomial
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 差值二次多项式 / Difference quadratic polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(
        lifted + rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        constant - rhs.constant
    )
}

/**
 * 二次多项式与标量的乘法运算符
 * Multiplication operator for quadratic polynomial and scalar
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 标量值 / Scalar value
 * @return 所有系数乘以标量后的二次多项式 / Quadratic polynomial with all coefficients multiplied by scalar
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.times(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(it.coefficient * rhs, it.symbol1, it.symbol2) },
        constant * rhs
    )
}

/**
 * 标量与二次多项式的乘法运算符
 * Multiplication operator for scalar and quadratic polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 所有系数乘以标量后的二次多项式 / Quadratic polynomial with all coefficients multiplied by scalar
 */
operator fun <T : Ring<T>> T.times(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return rhs * this
}

/**
 * 二次多项式与标量的除法运算符
 * Division operator for quadratic polynomial and scalar
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 标量值 / Scalar value
 * @return 所有系数除以标量后的二次多项式 / Quadratic polynomial with all coefficients divided by scalar
 */
operator fun <T : Field<T>> QuadraticPolynomial<T>.div(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(it.coefficient / rhs, it.symbol1, it.symbol2) },
        constant / rhs
    )
}

/**
 * 二次多项式与标量的加法运算符
 * Addition operator for quadratic polynomial and scalar
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 标量值 / Scalar value
 * @return 常数项增加标量后的二次多项式 / Quadratic polynomial with constant increased by scalar
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials, constant + rhs)
}

/**
 * 标量与二次多项式的加法运算符
 * Addition operator for scalar and quadratic polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 常数项增加标量后的二次多项式 / Quadratic polynomial with constant increased by scalar
 */
operator fun <T : Ring<T>> T.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(rhs.monomials, this + rhs.constant)
}

/**
 * 二次多项式与标量的减法运算符
 * Subtraction operator for quadratic polynomial and scalar
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 标量值 / Scalar value
 * @return 常数项减少标量后的二次多项式 / Quadratic polynomial with constant decreased by scalar
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials, constant - rhs)
}

/**
 * 标量与二次多项式的减法运算符
 * Subtraction operator for scalar and quadratic polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 从标量减去多项式后的二次多项式 / Quadratic polynomial representing scalar minus polynomial
 */
operator fun <T : Ring<T>> T.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        this - rhs.constant
    )
}

