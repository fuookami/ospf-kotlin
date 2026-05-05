@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.adapter.flt64

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial

operator fun Flt64.times(rhs: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(this, rhs)
}

operator fun Symbol.times(rhs: Flt64): LinearMonomial<Flt64> {
    return LinearMonomial(rhs, this)
}

operator fun Flt64.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(this * rhs.coefficient, rhs.symbol)
}

operator fun LinearMonomial<Flt64>.times(rhs: Flt64): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient * rhs, symbol)
}

operator fun LinearMonomial<Flt64>.times(rhs: Symbol): QuadraticMonomial<Flt64> {
    return QuadraticMonomial(coefficient, symbol, rhs)
}

operator fun Symbol.times(rhs: LinearMonomial<Flt64>): QuadraticMonomial<Flt64> {
    return QuadraticMonomial(rhs.coefficient, this, rhs.symbol)
}

operator fun LinearMonomial<Flt64>.div(rhs: Flt64): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient / rhs, symbol)
}

operator fun LinearMonomial<Flt64>.plus(rhs: Flt64): LinearMonomial<Flt64> {
    return this
}

operator fun LinearMonomial<Flt64>.minus(rhs: Flt64): LinearMonomial<Flt64> {
    return this
}
