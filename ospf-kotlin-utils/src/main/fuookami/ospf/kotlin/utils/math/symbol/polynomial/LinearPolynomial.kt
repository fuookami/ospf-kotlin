package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.NumberField

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial

data class LinearPolynomial<T>(
    val monomials: List<LinearMonomial<T>> = emptyList(),
    val constant: T
) {
    val category: Category
        get() = Linear
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.unaryMinus(): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        -constant
    )
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(monomials + rhs.monomials, constant + rhs.constant)
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.minus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        constant - rhs.constant
    )
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.times(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient * rhs, it.symbol) },
        constant * rhs
    )
}

operator fun <T : NumberField<T>> T.times(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return rhs * this
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.div(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(it.coefficient / rhs, it.symbol) },
        constant / rhs
    )
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.plus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(monomials, constant + rhs)
}

operator fun <T : NumberField<T>> T.plus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(rhs.monomials, this + rhs.constant)
}

operator fun <T : NumberField<T>> LinearPolynomial<T>.minus(rhs: T): LinearPolynomial<T> {
    return LinearPolynomial(monomials, constant - rhs)
}

operator fun <T : NumberField<T>> T.minus(rhs: LinearPolynomial<T>): LinearPolynomial<T> {
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        this - rhs.constant
    )
}

