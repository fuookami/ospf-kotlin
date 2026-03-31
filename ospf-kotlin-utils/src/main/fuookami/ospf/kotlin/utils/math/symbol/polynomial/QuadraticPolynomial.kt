package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.NumberField

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial

data class QuadraticPolynomial<T>(
    val monomials: List<QuadraticMonomial<T>> = emptyList(),
    val constant: T
) {
    val category: Category
        get() = if (monomials.any { it.isQuadratic }) Quadratic else Linear
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.unaryMinus(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        -constant
    )
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.plus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + QuadraticMonomial.linear(rhs.coefficient, rhs.symbol), constant)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(QuadraticMonomial.linear(coefficient, symbol)) + rhs.monomials, rhs.constant)
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.minus(rhs: LinearMonomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + QuadraticMonomial.linear(-rhs.coefficient, rhs.symbol), constant)
}

operator fun <T : NumberField<T>> LinearMonomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        listOf(QuadraticMonomial.linear(coefficient, symbol)) +
                rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        -rhs.constant
    )
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials + rhs.monomials, constant + rhs.constant)
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials + rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        constant - rhs.constant
    )
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.plus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(monomials + lifted, constant + rhs.constant)
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(lifted + rhs.monomials, constant + rhs.constant)
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.minus(rhs: LinearPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = rhs.monomials.map { QuadraticMonomial.linear(-it.coefficient, it.symbol) }
    return QuadraticPolynomial(monomials + lifted, constant - rhs.constant)
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    val lifted = monomials.map { QuadraticMonomial.linear(it.coefficient, it.symbol) }
    return QuadraticPolynomial(
        lifted + rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        constant - rhs.constant
    )
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.times(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(it.coefficient * rhs, it.symbol1, it.symbol2) },
        constant * rhs
    )
}

operator fun <T : NumberField<T>> T.times(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return rhs * this
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.div(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(it.coefficient / rhs, it.symbol1, it.symbol2) },
        constant / rhs
    )
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.plus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials, constant + rhs)
}

operator fun <T : NumberField<T>> T.plus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(rhs.monomials, this + rhs.constant)
}

operator fun <T : NumberField<T>> QuadraticPolynomial<T>.minus(rhs: T): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials, constant - rhs)
}

operator fun <T : NumberField<T>> T.minus(rhs: QuadraticPolynomial<T>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        rhs.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
        this - rhs.constant
    )
}

