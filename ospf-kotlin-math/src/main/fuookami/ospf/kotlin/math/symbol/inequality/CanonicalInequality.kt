/**
 * 规范不等弌
 * Canonical Inequality
 *
 * 定义规范形式的不等式，左右两边均为规范多项式。
 * 规范不等式支持任意次数的多项式，是最通用的不等式表示形式。
 * Defines inequalities in canonical form, where both sides are canonical polynomials.
 * Canonical inequalities support polynomials of any degree,
 * representing the most general form of inequalities.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.*

/**
 * 规范不等弌
 * Canonical Inequality
 *
 * 表示规范形式的不等式，包含左侧多项式、右侧多项式和比较运算符。
 * 规范不等式可以表示任意次数的多项式不等式，是最通用的不等式表示形式。
 * Represents an inequality in canonical form, containing left-hand polynomial,
 * right-hand polynomial, and comparison operator.
 * Canonical inequalities can represent polynomial inequalities of any degree,
 * being the most general form of inequality representation.
 *
 * @param T the coefficient type (must implement Ring)
 * @property lhs 左侧规范多项弌/ Left-hand canonical polynomial
 * @property rhs 右侧规范多项弌/ Right-hand canonical polynomial
 * @property comparison 比较运算笌/ Comparison operator
 */
data class CanonicalInequality<T : Ring<T>>(
    val lhs: CanonicalPolynomial<T>,
    val rhs: CanonicalPolynomial<T>,
    val comparison: Comparison
) {
    /**
     * 返回反转后的不等式（交换左右两侧并反转比较运算符）。
     * Returns the reversed inequality (swaps left and right sides and reverses the comparison operator).
     *
     * @return 反转后的规范不等式 / The reversed canonical inequality
     */
    fun reverse(): CanonicalInequality<T> {
        return CanonicalInequality(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse()
        )
    }
}
