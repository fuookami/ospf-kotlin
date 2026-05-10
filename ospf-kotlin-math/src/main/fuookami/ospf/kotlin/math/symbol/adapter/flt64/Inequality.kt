@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.adapter.flt64

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

/**
 * Flt64-specific convenience typealias for QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>.
 *
 * Prefer using [QuadraticInequalityOf] directly with your value type parameter.
 * This typealias will be removed in a future version.
 */
@kotlin.Deprecated(
    message = "Use QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> directly. This typealias will be removed in a future version.",
    replaceWith = ReplaceWith("QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>", "fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf"),
    level = DeprecationLevel.WARNING
)
typealias QuadraticInequality = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>

// ========== Comparison.satisfiedBy ==========

fun Comparison.satisfiedBy(lhs: Flt64, rhs: Flt64): Boolean {
    return when (this) {
        Comparison.LT -> lhs < rhs
        Comparison.LE -> lhs <= rhs
        Comparison.EQ -> lhs == rhs
        Comparison.NE -> lhs != rhs
        Comparison.GE -> lhs >= rhs
        Comparison.GT -> lhs > rhs
    }
}

// ========== Symbol-level DSL ==========

private fun Symbol.asLinearPolynomial(): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64.zero)
}

private fun Flt64.asLinearPolynomial(): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearPolynomial(emptyList(), this)
}

infix fun Symbol.lt(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Symbol.le(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Symbol.eq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Symbol.ne(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Symbol.ge(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Symbol.gt(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

infix fun Flt64.lt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Flt64.le(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Flt64.eq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Flt64.ne(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Flt64.ge(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Flt64.gt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

infix fun Symbol.lt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LT)
infix fun Symbol.le(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.LE)
infix fun Symbol.eq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.EQ)
infix fun Symbol.ne(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.NE)
infix fun Symbol.ge(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GE)
infix fun Symbol.gt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality(asLinearPolynomial(), rhs.asLinearPolynomial(), Comparison.GT)

// ========== isSatisfied ==========

fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfiedOrdered(order: List<Symbol>, values: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}

fun QuadraticInequality.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

fun QuadraticInequality.isSatisfiedOrdered(order: List<Symbol>, values: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfied(values: Map<Symbol, Flt64>): Boolean? {
    val provider = MapValueProvider(values)
    val lhsValue = lhs.evaluate(provider) ?: return null
    val rhsValue = rhs.evaluate(provider) ?: return null
    return comparison.satisfiedBy(lhsValue, rhsValue)
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.isSatisfiedOrdered(order: List<Symbol>, values: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>): Boolean {
    return comparison.satisfiedBy(
        lhs = lhs.evaluateOrdered(order, values),
        rhs = rhs.evaluateOrdered(order, values)
    )
}
