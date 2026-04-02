package fuookami.ospf.kotlin.utils.math.symbol.monomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

data class QuadraticMonomial<T>(
    val coefficient: T,
    val symbol1: Symbol,
    val symbol2: Symbol? = null
) {
    companion object {
        fun <T> linear(coefficient: T, symbol: Symbol): QuadraticMonomial<T> {
            return QuadraticMonomial(coefficient, symbol)
        }

        fun <T> quadratic(coefficient: T, symbol1: Symbol, symbol2: Symbol): QuadraticMonomial<T> {
            return QuadraticMonomial(coefficient, symbol1, symbol2)
        }
    }

    val isQuadratic: Boolean
        get() = symbol2 != null

    val category: Category
        get() = if (isQuadratic) Quadratic else Linear
}

private fun <T : Ring<T>> zeroOf(value: T): T {
    return value - value
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.unaryMinus(): QuadraticMonomial<T> {
    return copy(coefficient = -coefficient)
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.times(rhs: T): QuadraticMonomial<T> {
    return copy(coefficient = coefficient * rhs)
}

operator fun <T : Field<T>> QuadraticMonomial<T>.div(rhs: T): QuadraticMonomial<T> {
    return copy(coefficient = coefficient / rhs)
}

operator fun <T : Ring<T>> T.times(rhs: QuadraticMonomial<T>): QuadraticMonomial<T> {
    return rhs * this
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this, rhs), zeroOf(coefficient))
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this, -rhs), zeroOf(coefficient))
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this, QuadraticMonomial.linear(rhs.coefficient, rhs.symbol)), zeroOf(coefficient))
}

operator fun <T : Ring<T>> LinearMonomial<T>.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol), rhs), zeroOf(coefficient))
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this, QuadraticMonomial.linear(-rhs.coefficient, rhs.symbol)), zeroOf(coefficient))
}

operator fun <T : Ring<T>> LinearMonomial<T>.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol), -rhs), zeroOf(coefficient))
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this) + rhs.monomials, rhs.constant)
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + rhs, constant)
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this) + rhs.monomials.map { -it }, -rhs.constant)
}

operator fun <T : Ring<T>> QuadraticPolynomial<T>.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + (-rhs), constant)
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this), rhs)
}

operator fun <T : Ring<T>> T.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(rhs), this)
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this), -rhs)
}

operator fun <T : Ring<T>> T.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(-rhs), this)
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.plus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(listOf(this) + lifted, rhs.constant)
}

operator fun <T : Ring<T>> LinearPolynomial<T>.plus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(lifted + rhs, constant)
}

operator fun <T : Ring<T>> QuadraticMonomial<T>.minus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(-it.coefficient, it.symbol) }
    return QuadraticPolynomial(listOf(this) + lifted, -rhs.constant)
}

operator fun <T : Ring<T>> LinearPolynomial<T>.minus(rhs: QuadraticMonomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(lifted + (-rhs), constant)
}

