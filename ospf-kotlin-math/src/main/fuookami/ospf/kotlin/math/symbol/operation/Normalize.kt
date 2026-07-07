/**
 * Flt64 不等式规范化
 * Flt64 Inequality Normalization
 *
 * 提供 Flt64 线性和二次不等式的规范化操作。
 * 将不等式两侧归并为左侧减右侧、右侧为零的标准形式。
 * Provides normalization operations for Flt64 linear and quadratic inequalities.
 * Standardizes inequalities to LHS-minus-RHS form with zero on the right-hand side.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 将 Flt64 线性不等式规范化为标准形式（LHS - RHS <= 0）
 * Normalize a Flt64 linear inequality to standard form (LHS - RHS <= 0)
 *
 * @receiver 待规范化的线性不等式 / The linear inequality to normalize
 * @return 规范化的不等式 / Normalized inequality
 */
fun LinearInequality<Flt64>.normalize(): LinearInequality<Flt64> {
    val normalizedLhs = lhs.subtractLinear(rhs, Flt64.zero).combineTerms()
    return copy(
        lhs = normalizedLhs,
        rhs = LinearPolynomial(emptyList(), Flt64.zero)
    )
}

/**
 * 将 Flt64 二次不等式规范化为标准形式（LHS - RHS <= 0）
 * Normalize a Flt64 quadratic inequality to standard form (LHS - RHS <= 0)
 *
 * @receiver 待规范化的二次不等式 / The quadratic inequality to normalize
 * @return 规范化的不等式 / Normalized inequality
 */
fun QuadraticInequalityOf<Flt64>.normalize(): QuadraticInequalityOf<Flt64> {
    val negatedRhsMonomials = rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
    val diff = QuadraticPolynomial(
        monomials = lhs.monomials + negatedRhsMonomials,
        constant = lhs.constant - rhs.constant
    )
    val normalizedLhs = diff.combineTerms()
    return copy(
        lhs = normalizedLhs,
        rhs = QuadraticPolynomial(emptyList(), Flt64.zero)
    )
}
