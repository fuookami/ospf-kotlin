package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.NumberField

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.operator.Abs
import fuookami.ospf.kotlin.utils.operator.abs

data class LinearMonomial<T>(
    val coefficient: T,
    val symbol: Symbol
) {
    val category: Category
        get() = Linear
}

private fun <T : NumberField<T>> zeroOf(value: T): T {
    return value - value
}

operator fun <T : NumberField<T>> LinearMonomial<T>.unaryMinus(): LinearMonomial<T> {
    return LinearMonomial(-coefficient, symbol)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.times(rhs: T): LinearMonomial<T> {
    return LinearMonomial(coefficient * rhs, symbol)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.div(rhs: T): LinearMonomial<T> {
    return LinearMonomial(coefficient / rhs, symbol)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.times(rhs: LinearMonomial<T>): QuadraticMonomial<T> {
    return QuadraticMonomial(coefficient * rhs.coefficient, symbol, rhs.symbol)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.times(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val quadraticTerms = rhs.monomials.map { this * it }
    val linearTerm = QuadraticMonomial.linear(coefficient * rhs.constant, symbol)
    return QuadraticPolynomial(quadraticTerms + linearTerm, zeroOf(coefficient))
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.times(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    val quadraticTerms = monomials.map { it * rhs }
    val linearTerm = QuadraticMonomial.linear(constant * rhs.coefficient, rhs.symbol)
    return QuadraticPolynomial(quadraticTerms + linearTerm, zeroOf(constant))
}

operator fun <T : NumberField<T>> LinearMonomial<T>.plus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this, rhs), zeroOf(coefficient))
}

operator fun <T : NumberField<T>> LinearMonomial<T>.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this) + rhs.monomials, rhs.constant)
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.plus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(monomials + rhs, constant)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.minus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this, -rhs), zeroOf(coefficient))
}

operator fun <T : NumberField<T>> LinearMonomial<T>.minus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this) + rhs.monomials.map { -it }, -rhs.constant)
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.minus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(monomials + (-rhs), constant)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.plus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this), rhs)
}

operator fun <T : NumberField<T>> T.plus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(rhs), this)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.minus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(listOf(this), -rhs)
}

operator fun <T : NumberField<T>> T.minus(rhs: LinearMonomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(listOf(-rhs), this)
}

fun <T : NumberField<T>> LinearMonomial<T>.abs(): LinearMonomial<T> {
    return LinearMonomial(abs(coefficient as Abs<T>), symbol)
}

fun <T : NumberField<T>> LinearMonomial<T>.reciprocal(): CanonicalMonomial<T> {
    return CanonicalMonomial(coefficient.reciprocal(), mapOf(symbol to Int32(-1)))
}

operator fun <T : NumberField<T>> T.times(rhs: LinearMonomial<T>): LinearMonomial<T> {
    return LinearMonomial(this * rhs.coefficient, rhs.symbol)
}

