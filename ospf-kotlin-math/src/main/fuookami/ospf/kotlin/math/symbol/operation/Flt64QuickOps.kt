/**
 * Flt64 乘除运算符重载
 * Flt64 Multiplication and Division Operator Overloads
 *
 * 提供 Flt64/Int/Double 与符号和线性单项式之间的乘除运算符重载。
 * Provides multiplication and division operator overloads between
 * Flt64/Int/Double and symbols/linear monomials.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*


// ========== Flt64 arithmetic ==========

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

// ========== Int arithmetic ==========

operator fun Int.times(rhs: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()), rhs)
}

operator fun Symbol.times(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs.toDouble()), this)
}

operator fun Int.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this.toDouble()) * rhs.coefficient, rhs.symbol)
}

operator fun LinearMonomial<Flt64>.times(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient * Flt64(rhs.toDouble()), symbol)
}

operator fun LinearMonomial<Flt64>.div(rhs: Int): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient / Flt64(rhs.toDouble()), symbol)
}

// ========== Double arithmetic ==========

operator fun Double.times(rhs: Symbol): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this), rhs)
}

operator fun Symbol.times(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(rhs), this)
}

operator fun Double.times(rhs: LinearMonomial<Flt64>): LinearMonomial<Flt64> {
    return LinearMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

operator fun LinearMonomial<Flt64>.times(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient * Flt64(rhs), symbol)
}

operator fun LinearMonomial<Flt64>.div(rhs: Double): LinearMonomial<Flt64> {
    return LinearMonomial(coefficient / Flt64(rhs), symbol)
}
