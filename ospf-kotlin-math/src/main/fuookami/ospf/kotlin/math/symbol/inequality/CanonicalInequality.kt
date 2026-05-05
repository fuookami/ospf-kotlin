/**
 * 规范不等式
 * Canonical Inequality
 *
 * 定义规范形式的不等式，左右两边均为规范多项式。
 * 规范不等式支持任意次数的多项式，是最通用的不等式表示形式。
 * Defines inequalities in canonical form, where both sides are canonical polynomials.
 * Canonical inequalities support polynomials of any degree,
 * representing the most general form of inequalities.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

/**
 * 规范不等式
 * Canonical Inequality
 *
 * 表示规范形式的不等式，包含左侧多项式、右侧多项式和比较运算符。
 * 规范不等式可以表示任意次数的多项式不等式，是最通用的不等式表示形式。
 * Represents an inequality in canonical form, containing left-hand polynomial,
 * right-hand polynomial, and comparison operator.
 * Canonical inequalities can represent polynomial inequalities of any degree,
 * being the most general form of inequality representation.
 *
 * @property lhs 左侧规范多项式 / Left-hand canonical polynomial
 * @property rhs 右侧规范多项式 / Right-hand canonical polynomial
 * @property comparison 比较运算符 / Comparison operator
 */
data class CanonicalInequality(
    val lhs: CanonicalPolynomial<Flt64>,
    val rhs: CanonicalPolynomial<Flt64>,
    val comparison: Comparison
) {
    fun reverse(): CanonicalInequality {
        return CanonicalInequality(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse()
        )
    }
}