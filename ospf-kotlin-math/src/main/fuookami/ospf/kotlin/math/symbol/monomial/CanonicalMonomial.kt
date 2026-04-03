package fuookami.ospf.kotlin.math.symbol.monomial

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Nonlinear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol

data class CanonicalMonomial<T : Ring<T>>(
    val coefficient: T,
    val powers: Map<Symbol, Int32> = emptyMap()
) {
    constructor(
        coefficient: T,
        factors: List<Symbol>
    ) : this(
        coefficient = coefficient,
        powers = factors.groupingBy { it }.eachCount().mapValues { Int32(it.value) }
    )

    val factors: List<Symbol>
        get() = powers.entries
            .flatMap { (symbol, exp) -> List(exp.toInt()) { symbol } }

    val degree: Int
        get() = powers.values.sum().toInt()

    val category: Category
        get() = when (degree) {
            0, 1 -> Linear
            2 -> Quadratic
            else -> Nonlinear
        }
}

operator fun <T : Ring<T>> CanonicalMonomial<T>.unaryMinus(): CanonicalMonomial<T> {
    return copy(coefficient = -coefficient)
}

operator fun <T : Ring<T>> CanonicalMonomial<T>.times(rhs: T): CanonicalMonomial<T> {
    return copy(coefficient = coefficient * rhs)
}

operator fun <T : Field<T>> CanonicalMonomial<T>.div(rhs: T): CanonicalMonomial<T> {
    return copy(coefficient = coefficient / rhs)
}

