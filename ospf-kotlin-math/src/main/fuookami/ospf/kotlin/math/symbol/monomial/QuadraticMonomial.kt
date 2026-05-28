/**
 * 二次单项式
 * Quadratic Monomial
 *
 * 定义二次单项式的数据结构和运算。二次单项式形如 c*x*y 或 c*x2，
 * 其中 c 为系数，x、y 为符号变量。当 y 为 null 时表示线性项 c*x，
 * 当 x == y 时表示纯二次项 c*x2。是构建二次多项式的基本单元。
 * Defines data structures and operations for quadratic monomials.
 * A quadratic monomial has the form c*x*y or c*x2, where c is the coefficient
 * and x, y are symbol variables. When y is null, it represents a linear term c*x,
 * when x == y, it represents a pure quadratic term c*x2.
 * It is the basic building block for quadratic polynomials.
 */
package fuookami.ospf.kotlin.math.symbol.monomial

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/**
 * 二次单项式
 * Quadratic Monomial
 *
 * 表示二次单项式，形如 c*x*y 或 c*x2，其中 c 为系数，x、y 为符号变量。
 * 当 symbol2 为 null 时，表示线性项 c*symbol1；当 symbol1 == symbol2 时，表示纯二次项 c*symbol12。
 * 二次单项式是构建二次多项式的基本单元。
 * Represents a quadratic monomial of the form c*x*y or c*x2, where c is the coefficient
 * and x, y are symbol variables. When symbol2 is null, it represents a linear term c*symbol1;
 * when symbol1 == symbol2, it represents a pure quadratic term c*symbol12.
 * Quadratic monomials are the basic building blocks for quadratic polynomials.
 *
 * @property coefficient 系数 / The coefficient
 * @property symbol1 第一个符号变量 / First symbol variable
 * @property symbol2 第二个符号变量（可选），null时表示线性项 / Second symbol variable (optional), null indicates linear term
 */
data class QuadraticMonomial<T : Ring<T>>(
    val coefficient: T,
    val symbol1: Symbol,
    val symbol2: Symbol? = null
) : ToQuadraticPolynomial<T>, ToCanonicalPolynomial<T> {
    companion object {
        /**
         * 创建线性形式的二次单项式
         * Creates a linear-form quadratic monomial
         *
         * 创建形如 c*x 的单项式，在二次多项式框架中表示线性项。
         * Creates a monomial of form c*x, representing a linear term in quadratic polynomial framework.
         *
         * @param coefficient 系数 / The coefficient
         * @param symbol 符号变量 / Symbol variable
         * @return 线性形式的二次单项式 / Linear-form quadratic monomial
         */
        fun <T : Ring<T>> linear(coefficient: T, symbol: Symbol): QuadraticMonomial<T> {
            return QuadraticMonomial(coefficient, symbol)
        }

        /**
         * 创建二次形式的二次单项式
         * Creates a quadratic-form quadratic monomial
         *
         * 创建形如 c*x*y 的单项式。
         * Creates a monomial of form c*x*y.
         *
         * @param coefficient 系数 / The coefficient
         * @param symbol1 第一个符号变量 / First symbol variable
         * @param symbol2 第二个符号变量 / Second symbol variable
         * @return 二次形式的二次单项式 / Quadratic-form quadratic monomial
         */
        fun <T : Ring<T>> quadratic(coefficient: T, symbol1: Symbol, symbol2: Symbol): QuadraticMonomial<T> {
            return QuadraticMonomial(coefficient, symbol1, symbol2)
        }
    }

    /**
     * 是否为二次项
     * Whether this is a quadratic term
     *
     * 当 symbol2 不为 null 时返回 true，表示这是真正的二次项。
     * Returns true when symbol2 is not null, indicating this is a true quadratic term.
     */
    val isQuadratic: Boolean
        get() = symbol2 != null

    /**
     * 表达式类型分类
     * Expression type category
     *
     * 根据 isQuadratic 返回 Quadratic 或 Linear 分类。
     * Returns Quadratic or Linear category based on isQuadratic.
     */
    val category: Category
        get() = if (isQuadratic) Quadratic else Linear

    override fun toQuadraticPolynomial(): QuadraticPolynomial<T> {
        return QuadraticPolynomial(listOf(this), coefficient - coefficient)
    }

    override fun toCanonicalPolynomial(): CanonicalPolynomial<T> {
        return toCanonicalMonomial().toCanonicalPolynomial()
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
 * 二次单项式的负运算符
 * Negation operator for quadratic monomial
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @return 系数取负后的二次单项式 / Quadratic monomial with negated coefficient
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.unaryMinus(): QuadraticMonomial<T> {
    return copy(coefficient = -coefficient)
}

/**
 * 二次单项式与标量的乘法运算符
 * Multiplication operator for quadratic monomial and scalar
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 标量值 / Scalar value
 * @return 系数乘以标量后的二次单项式 / Quadratic monomial with coefficient multiplied by scalar
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.times(rhs: T): QuadraticMonomial<T> {
    return copy(coefficient = coefficient * rhs)
}

/**
 * 二次单项式与标量的除法运算符
 * Division operator for quadratic monomial and scalar
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 标量值 / Scalar value
 * @return 系数除以标量后的二次单项式 / Quadratic monomial with coefficient divided by scalar
 */
operator fun <T : Field<T>> QuadraticMonomial<T>.div(rhs: T): QuadraticMonomial<T> {
    return copy(coefficient = coefficient / rhs)
}

/**
 * 标量与二次单项式的乘法运算符
 * Multiplication operator for scalar and quadratic monomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次单项式 / Quadratic monomial
 */
operator fun <T : Ring<T>> T.times(rhs: QuadraticMonomial<T>): QuadraticMonomial<T> {
    return rhs * this
}

/**
 * 二次单项式之间的加法运算符
 * Addition operator between quadratic monomials
 *
 * @receiver 左侧二次单项式 / Left-hand quadratic monomial
 * @param rhs 右侧二次单项式 / Right-hand quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this, rhs), zeroOf(coefficient))
}

/**
 * 二次单项式之间的减法运算符
 * Subtraction operator between quadratic monomials
 *
 * @receiver 左侧二次单项式 / Left-hand quadratic monomial
 * @param rhs 右侧二次单项式 / Right-hand quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this, -rhs), zeroOf(coefficient))
}

/**
 * 二次单项式与线性单项式的加法运算符
 * Addition operator between quadratic monomial and linear monomial
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 线性单项式 / Linear monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this, QuadraticMonomial.linear(rhs.coefficient, rhs.symbol)), zeroOf(coefficient))
}

/**
 * 线性单项式与二次单项式的加法运算符
 * Addition operator between linear monomial and quadratic monomial
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol), rhs), zeroOf(coefficient))
}

/**
 * 二次单项式与线性单项式的减法运算符
 * Subtraction operator between quadratic monomial and linear monomial
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 线性单项式 / Linear monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this, QuadraticMonomial.linear(-rhs.coefficient, rhs.symbol)), zeroOf(coefficient))
}

/**
 * 线性单项式与二次单项式的减法运算符
 * Subtraction operator between linear monomial and quadratic monomial
 *
 * @receiver 线性单项式 / Linear monomial
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> LinearMonomial<T>.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol), -rhs), zeroOf(coefficient))
}

/**
 * 二次单项式与二次多项式的加法运算符
 * Addition operator between quadratic monomial and quadratic polynomial
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this) + rhs.monomials, rhs.constant)
}

/**
 * 二次多项式与二次单项式的加法运算符
 * Addition operator between quadratic polynomial and quadratic monomial
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + rhs, constant)
}

/**
 * 二次单项式与二次多项式的减法运算符
 * Subtraction operator between quadratic monomial and quadratic polynomial
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 二次多项式 / Quadratic polynomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this) + rhs.monomials.map { -it }, -rhs.constant)
}

/**
 * 二次多项式与二次单项式的减法运算符
 * Subtraction operator between quadratic polynomial and quadratic monomial
 *
 * @receiver 二次多项式 / Quadratic polynomial
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + (-rhs), constant)
}

/**
 * 二次单项式与标量的加法运算符
 * Addition operator between quadratic monomial and scalar
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 标量值 / Scalar value
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this), rhs)
}

/**
 * 标量与二次单项式的加法运算符
 * Addition operator between scalar and quadratic monomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> T.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(rhs), this)
}

/**
 * 二次单项式与标量的减法运算符
 * Subtraction operator between quadratic monomial and scalar
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 标量值 / Scalar value
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this), -rhs)
}

/**
 * 标量与二次单项式的减法运算符
 * Subtraction operator between scalar and quadratic monomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> T.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(-rhs), this)
}

/**
 * 二次单项式与线性多项式的加法运算符
 * Addition operator between quadratic monomial and linear polynomial
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 线性多项式 / Linear polynomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(listOf(this) + lifted, rhs.constant)
}

/**
 * 线性多项式与二次单项式的加法运算符
 * Addition operator between linear polynomial and quadratic monomial
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(lifted + rhs, constant)
}

/**
 * 二次单项式与线性多项式的减法运算符
 * Subtraction operator between quadratic monomial and linear polynomial
 *
 * @receiver 二次单项式 / Quadratic monomial
 * @param rhs 线性多项式 / Linear polynomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(-it.coefficient, it.symbol) }
    return QuadraticPolynomial(listOf(this) + lifted, -rhs.constant)
}

/**
 * 线性多项式与二次单项式的减法运算符
 * Subtraction operator between linear polynomial and quadratic monomial
 *
 * @receiver 线性多项式 / Linear polynomial
 * @param rhs 二次单项式 / Quadratic monomial
 * @return 二次多项式 / Quadratic polynomial
 */
operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(lifted + (-rhs), constant)
}
