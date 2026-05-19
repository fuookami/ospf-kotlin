/**
 * 规范多项式
 * Canonical Polynomial
 *
 * 定义规范多项式的数据结构和运算。规范多项式是规范单项式的线性组合，
 * 支持任意次数的多项式表达式。是最通用的多项式表示形式。
 * Defines data structures and operations for canonical polynomials.
 * A canonical polynomial is a linear combination of canonical monomials,
 * supporting polynomial expressions of any degree.
 * It is the most general form of polynomial representation.
 */
package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Nonlinear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.TryToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.TryToQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.combineLinearTerms
import fuookami.ospf.kotlin.math.symbol.operation.combineQuadraticTerms
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticMonomialOrNull
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.div
import fuookami.ospf.kotlin.math.symbol.monomial.times
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus

/**
 * 规范多项式
 * Canonical Polynomial
 *
 * 表示规范多项式，是规范单项式的线性组合加上一个常数项。
 * 规范多项式支持任意次数的多项式表达式，是最通用的多项式表示形式。
 * Represents a canonical polynomial, a linear combination of canonical monomials plus a constant term.
 * Canonical polynomials support polynomial expressions of any degree,
 * being the most general form of polynomial representation.
 *
 * @property monomials 规范单项式列表 / List of canonical monomials
 * @property constant 常数项 / Constant term
 */
data class CanonicalPolynomial<T : Ring<T>>(
    val monomials: List<CanonicalMonomial<T>> = emptyList(),
    val constant: T
) : ToCanonicalPolynomial<T>, TryToLinearPolynomial<T>, TryToQuadraticPolynomial<T> {
    /**
     * 表达式类型分类
     * Expression type category
     *
     * 根据单项式的最高次数返回对应的分类：
     * - 0 或 1：Linear
     * - 2：Quadratic
     * - 大于 2：Nonlinear
     * Returns the corresponding category based on the maximum degree of monomials:
     * - 0 or 1: Linear
     * - 2: Quadratic
     * - Greater than 2: Nonlinear
     */
    val category: Category
        get() = when (monomials.maxOfOrNull { it.degree } ?: 0) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }

    override fun toCanonicalPolynomial(): CanonicalPolynomial<T> = this

    override fun toLinearPolynomialOrNull(): LinearPolynomial<T>? {
        val linearMonomials = ArrayList<LinearMonomial<T>>(monomials.size)
        var canonicalConstant = constant
        for (monomial in monomials) {
            when (monomial.degree) {
                0 -> { canonicalConstant += monomial.coefficient }
                1 -> {
                    val entry = monomial.powers.entries.firstOrNull { it.value.toInt() == 1 } ?: return null
                    linearMonomials.add(LinearMonomial(monomial.coefficient, entry.key))
                }
                else -> { return null }
            }
        }
        val zero = constant - constant
        return LinearPolynomial(linearMonomials, canonicalConstant).combineLinearTerms(zero, { it == zero })
    }

    override fun toQuadraticPolynomialOrNull(): QuadraticPolynomial<T>? {
        val quadraticMonomials = ArrayList<QuadraticMonomial<T>>(monomials.size)
        var canonicalConstant = constant
        for (monomial in monomials) {
            when (monomial.degree) {
                0 -> { canonicalConstant += monomial.coefficient }
                1, 2 -> {
                    quadraticMonomials.add(monomial.toQuadraticMonomialOrNull() ?: return null)
                }
                else -> { return null }
            }
        }
        val zero = constant - constant
        return QuadraticPolynomial(quadraticMonomials, canonicalConstant).combineQuadraticTerms(zero, { it == zero })
    }
}

/**
 * 规范多项式的负运算符
 * Negation operator for canonical polynomial
 *
 * @receiver 规范多项式 / Canonical polynomial
 * @return 所有项取负后的规范多项式 / Canonical polynomial with all terms negated
 */
operator fun <T : Ring<T>> CanonicalPolynomial<T>.unaryMinus(): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials.map { -it }, -constant)
}

/**
 * 规范多项式与规范单项式的加法运算符
 * Addition operator between canonical polynomial and canonical monomial
 *
 * @receiver 规范多项式 / Canonical polynomial
 * @param rhs 规范单项式 / Canonical monomial
 * @return 合并后的规范多项式 / Combined canonical polynomial
 */
operator fun <T : Ring<T>> CanonicalPolynomial<T>.plus(rhs: CanonicalMonomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials + rhs, constant)
}

/**
 * 规范单项式与规范多项式的加法运算符
 * Addition operator between canonical monomial and canonical polynomial
 *
 * @receiver 规范单项式 / Canonical monomial
 * @param rhs 规范多项式 / Canonical polynomial
 * @return 合并后的规范多项式 / Combined canonical polynomial
 */
operator fun <T : Ring<T>> CanonicalMonomial<T>.plus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(listOf(this) + rhs.monomials, rhs.constant)
}

/**
 * 规范多项式与规范单项式的减法运算符
 * Subtraction operator between canonical polynomial and canonical monomial
 *
 * @receiver 规范多项式 / Canonical polynomial
 * @param rhs 规范单项式 / Canonical monomial
 * @return 差值规范多项式 / Difference canonical polynomial
 */
operator fun <T : Ring<T>> CanonicalPolynomial<T>.minus(rhs: CanonicalMonomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials + (-rhs), constant)
}

/**
 * 规范单项式与规范多项式的减法运算符
 * Subtraction operator between canonical monomial and canonical polynomial
 *
 * @receiver 规范单项式 / Canonical monomial
 * @param rhs 规范多项式 / Canonical polynomial
 * @return 差值规范多项式 / Difference canonical polynomial
 */
operator fun <T : Ring<T>> CanonicalMonomial<T>.minus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(listOf(this) + rhs.monomials.map { -it }, -rhs.constant)
}

/**
 * 规范多项式之间的加法运算符
 * Addition operator between canonical polynomials
 *
 * @receiver 左侧规范多项式 / Left-hand canonical polynomial
 * @param rhs 右侧规范多项式 / Right-hand canonical polynomial
 * @return 合并后的规范多项式 / Combined canonical polynomial
 */
operator fun <T : Ring<T>> CanonicalPolynomial<T>.plus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials + rhs.monomials, constant + rhs.constant)
}

/**
 * 规范多项式之间的减法运算符
 * Subtraction operator between canonical polynomials
 *
 * @receiver 左侧规范多项式 / Left-hand canonical polynomial
 * @param rhs 右侧规范多项式 / Right-hand canonical polynomial
 * @return 差值规范多项式 / Difference canonical polynomial
 */
operator fun <T : Ring<T>> CanonicalPolynomial<T>.minus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials + rhs.monomials.map { -it }, constant - rhs.constant)
}

/**
 * 规范多项式与标量的乘法运算符
 * Multiplication operator for canonical polynomial and scalar
 *
 * @receiver 规范多项式 / Canonical polynomial
 * @param rhs 标量值 / Scalar value
 * @return 所有系数乘以标量后的规范多项式 / Canonical polynomial with all coefficients multiplied by scalar
 */
operator fun <T : Ring<T>> CanonicalPolynomial<T>.times(rhs: T): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials.map { it * rhs }, constant * rhs)
}

/**
 * 标量与规范多项式的乘法运算符
 * Multiplication operator for scalar and canonical polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 规范多项式 / Canonical polynomial
 * @return 所有系数乘以标量后的规范多项式 / Canonical polynomial with all coefficients multiplied by scalar
 */
operator fun <T : Ring<T>> T.times(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return rhs * this
}

/**
 * 规范多项式与标量的除法运算符
 * Division operator for canonical polynomial and scalar
 *
 * @receiver 规范多项式 / Canonical polynomial
 * @param rhs 标量值 / Scalar value
 * @return 所有系数除以标量后的规范多项式 / Canonical polynomial with all coefficients divided by scalar
 */
operator fun <T : Field<T>> CanonicalPolynomial<T>.div(rhs: T): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials.map { it / rhs }, constant / rhs)
}

/**
 * 规范多项式与标量的加法运算符
 * Addition operator for canonical polynomial and scalar
 *
 * @receiver 规范多项式 / Canonical polynomial
 * @param rhs 标量值 / Scalar value
 * @return 常数项增加标量后的规范多项式 / Canonical polynomial with constant increased by scalar
 */
operator fun <T : Ring<T>> CanonicalPolynomial<T>.plus(rhs: T): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials, constant + rhs)
}

/**
 * 标量与规范多项式的加法运算符
 * Addition operator for scalar and canonical polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 规范多项式 / Canonical polynomial
 * @return 常数项增加标量后的规范多项式 / Canonical polynomial with constant increased by scalar
 */
operator fun <T : Ring<T>> T.plus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(rhs.monomials, this + rhs.constant)
}

/**
 * 规范多项式与标量的减法运算符
 * Subtraction operator for canonical polynomial and scalar
 *
 * @receiver 规范多项式 / Canonical polynomial
 * @param rhs 标量值 / Scalar value
 * @return 常数项减少标量后的规范多项式 / Canonical polynomial with constant decreased by scalar
 */
operator fun <T : Ring<T>> CanonicalPolynomial<T>.minus(rhs: T): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials, constant - rhs)
}

/**
 * 标量与规范多项式的减法运算符
 * Subtraction operator for scalar and canonical polynomial
 *
 * @receiver 标量值 / Scalar value
 * @param rhs 规范多项式 / Canonical polynomial
 * @return 从标量减去多项式后的规范多项式 / Canonical polynomial representing scalar minus polynomial
 */
operator fun <T : Ring<T>> T.minus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(rhs.monomials.map { -it }, this - rhs.constant)
}
