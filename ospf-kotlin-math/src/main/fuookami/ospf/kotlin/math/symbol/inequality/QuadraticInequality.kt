/**
 * 二次不等式
 * Quadratic Inequality
 *
 * 定义二次不等式，左右两边均为二次多项式。
 * 二次不等式在二次规划和凸优化问题中广泛使用。
 * Defines quadratic inequalities, where both sides are quadratic polynomials.
 * Quadratic inequalities are widely used in quadratic programming
 * and convex optimization problems.
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticPolynomial

data class QuadraticInequality(
    val lhs: QuadraticPolynomial<Flt64>,
    val rhs: QuadraticPolynomial<Flt64>,
    val comparison: Comparison
)

private fun QuadraticMonomial<Flt64>.asPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(this), Flt64.zero)
}

private fun LinearMonomial<Flt64>.asPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol)), Flt64.zero)
}

private fun Flt64.asQuadraticPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(emptyList(), this)
}

infix fun QuadraticPolynomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.LT)
infix fun QuadraticPolynomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.LE)
infix fun QuadraticPolynomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.EQ)
infix fun QuadraticPolynomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.NE)
infix fun QuadraticPolynomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.GE)
infix fun QuadraticPolynomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.GT)

infix fun QuadraticMonomial<Flt64>.lt(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() lt rhs.asPolynomial()
infix fun QuadraticMonomial<Flt64>.le(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() le rhs.asPolynomial()
infix fun QuadraticMonomial<Flt64>.eq(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() eq rhs.asPolynomial()
infix fun QuadraticMonomial<Flt64>.ne(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() ne rhs.asPolynomial()
infix fun QuadraticMonomial<Flt64>.ge(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() ge rhs.asPolynomial()
infix fun QuadraticMonomial<Flt64>.gt(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = asPolynomial() gt rhs.asPolynomial()

infix fun QuadraticPolynomial<Flt64>.lt(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this lt rhs.toQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.le(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this le rhs.toQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.eq(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this eq rhs.toQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.ne(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this ne rhs.toQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.ge(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this ge rhs.toQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.gt(rhs: LinearPolynomial<Flt64>): QuadraticInequality = this gt rhs.toQuadraticPolynomial()

infix fun LinearPolynomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() lt rhs
infix fun LinearPolynomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() le rhs
infix fun LinearPolynomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() eq rhs
infix fun LinearPolynomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() ne rhs
infix fun LinearPolynomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() ge rhs
infix fun LinearPolynomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = toQuadraticPolynomial() gt rhs

infix fun QuadraticPolynomial<Flt64>.lt(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this lt rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.le(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this le rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.eq(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this eq rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.ne(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this ne rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.ge(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this ge rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.gt(rhs: QuadraticMonomial<Flt64>): QuadraticInequality = this gt rhs.asPolynomial()

infix fun QuadraticMonomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() lt rhs
infix fun QuadraticMonomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() le rhs
infix fun QuadraticMonomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() eq rhs
infix fun QuadraticMonomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() ne rhs
infix fun QuadraticMonomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() ge rhs
infix fun QuadraticMonomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() gt rhs

infix fun QuadraticPolynomial<Flt64>.lt(rhs: LinearMonomial<Flt64>): QuadraticInequality = this lt rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.le(rhs: LinearMonomial<Flt64>): QuadraticInequality = this le rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.eq(rhs: LinearMonomial<Flt64>): QuadraticInequality = this eq rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.ne(rhs: LinearMonomial<Flt64>): QuadraticInequality = this ne rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.ge(rhs: LinearMonomial<Flt64>): QuadraticInequality = this ge rhs.asPolynomial()
infix fun QuadraticPolynomial<Flt64>.gt(rhs: LinearMonomial<Flt64>): QuadraticInequality = this gt rhs.asPolynomial()

infix fun LinearMonomial<Flt64>.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() lt rhs
infix fun LinearMonomial<Flt64>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() le rhs
infix fun LinearMonomial<Flt64>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() eq rhs
infix fun LinearMonomial<Flt64>.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() ne rhs
infix fun LinearMonomial<Flt64>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() ge rhs
infix fun LinearMonomial<Flt64>.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asPolynomial() gt rhs

infix fun QuadraticPolynomial<Flt64>.lt(rhs: Flt64): QuadraticInequality = this lt rhs.asQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.le(rhs: Flt64): QuadraticInequality = this le rhs.asQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.eq(rhs: Flt64): QuadraticInequality = this eq rhs.asQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.ne(rhs: Flt64): QuadraticInequality = this ne rhs.asQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.ge(rhs: Flt64): QuadraticInequality = this ge rhs.asQuadraticPolynomial()
infix fun QuadraticPolynomial<Flt64>.gt(rhs: Flt64): QuadraticInequality = this gt rhs.asQuadraticPolynomial()

infix fun Flt64.lt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() lt rhs
infix fun Flt64.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() le rhs
infix fun Flt64.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() eq rhs
infix fun Flt64.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() ne rhs
infix fun Flt64.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() ge rhs
infix fun Flt64.gt(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = asQuadraticPolynomial() gt rhs

