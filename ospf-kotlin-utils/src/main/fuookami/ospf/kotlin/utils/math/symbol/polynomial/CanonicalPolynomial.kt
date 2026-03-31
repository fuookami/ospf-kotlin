package fuookami.ospf.kotlin.utils.math.symbol.polynomial

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.NumberField

import fuookami.ospf.kotlin.utils.math.symbol.Category
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Nonlinear
import fuookami.ospf.kotlin.utils.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.div
import fuookami.ospf.kotlin.utils.math.symbol.monomial.times
import fuookami.ospf.kotlin.utils.math.symbol.monomial.unaryMinus

data class CanonicalPolynomial<T : NumberField<T>>(
    val monomials: List<CanonicalMonomial<T>> = emptyList(),
    val constant: T
) {
    val category: Category
        get() = when (monomials.maxOfOrNull { it.degree } ?: 0) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.unaryMinus(): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials.map { -it }, -constant)
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.plus(rhs: CanonicalMonomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials + rhs, constant)
}

operator fun <T : NumberField<T>> CanonicalMonomial<T>.plus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(listOf(this) + rhs.monomials, rhs.constant)
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.minus(rhs: CanonicalMonomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials + (-rhs), constant)
}

operator fun <T : NumberField<T>> CanonicalMonomial<T>.minus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(listOf(this) + rhs.monomials.map { -it }, -rhs.constant)
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.plus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials + rhs.monomials, constant + rhs.constant)
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.minus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials + rhs.monomials.map { -it }, constant - rhs.constant)
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.times(rhs: T): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials.map { it * rhs }, constant * rhs)
}

operator fun <T : NumberField<T>> T.times(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return rhs * this
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.div(rhs: T): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials.map { it / rhs }, constant / rhs)
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.plus(rhs: T): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials, constant + rhs)
}

operator fun <T : NumberField<T>> T.plus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(rhs.monomials, this + rhs.constant)
}

operator fun <T : NumberField<T>> CanonicalPolynomial<T>.minus(rhs: T): CanonicalPolynomial<T> {
    return CanonicalPolynomial(monomials, constant - rhs)
}

operator fun <T : NumberField<T>> T.minus(rhs: CanonicalPolynomial<T>): CanonicalPolynomial<T> {
    return CanonicalPolynomial(rhs.monomials.map { -it }, this - rhs.constant)
}

