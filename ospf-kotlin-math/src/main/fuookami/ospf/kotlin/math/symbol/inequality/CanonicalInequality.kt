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
