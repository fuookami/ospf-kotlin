@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.adapter.flt64

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial

import kotlin.jvm.JvmName

@JvmName("quickLinearPolynomialZero")
fun LinearPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), Flt64.zero)
}

@JvmName("quickLinearPolynomialFromConstant")
fun LinearPolynomial(constant: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), constant)
}

@JvmName("quickLinearPolynomialFromMonomial")
fun LinearPolynomial(monomial: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(monomial), Flt64.zero)
}

@JvmName("quickLinearPolynomialFromSymbol")
fun LinearPolynomial(symbol: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, symbol)), Flt64.zero)
}

@JvmName("quickMutableLinearPolynomialZero")
fun MutableLinearPolynomial(): MutableLinearPolynomial<Flt64> {
    return MutableLinearPolynomial(emptyList(), Flt64.zero)
}

@JvmName("quickMutableLinearPolynomialFromMonomial")
fun MutableLinearPolynomial(monomial: LinearMonomial<Flt64>): MutableLinearPolynomial<Flt64> {
    return MutableLinearPolynomial(listOf(monomial), Flt64.zero)
}

@JvmName("quickSumVars")
fun <E> sumVars(
    items: Iterable<E>,
    selector: (E) -> Symbol?
): LinearPolynomial<Flt64> {
    val monomials = items.mapNotNull(selector).map { LinearMonomial(Flt64.one, it) }
    return LinearPolynomial(monomials, Flt64.zero)
}

@JvmName("quickSumSymbols")
fun sum(symbols: Iterable<Symbol>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        symbols.map { LinearMonomial(Flt64.one, it) },
        Flt64.zero
    )
}

operator fun Symbol.unaryMinus(): LinearMonomial<Flt64> {
    return LinearMonomial(-Flt64.one, this)
}

operator fun Symbol.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(Flt64.one, rhs)),
        Flt64.zero
    )
}

operator fun Symbol.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-Flt64.one, rhs)),
        Flt64.zero
    )
}

operator fun Symbol.plus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), rhs),
        Flt64.zero
    )
}

operator fun Symbol.minus(rhs: LinearMonomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this), LinearMonomial(-rhs.coefficient, rhs.symbol)),
        Flt64.zero
    )
}

operator fun LinearMonomial<Flt64>.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(this, LinearMonomial(Flt64.one, rhs)),
        Flt64.zero
    )
}

operator fun LinearMonomial<Flt64>.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(this, LinearMonomial(-Flt64.one, rhs)),
        Flt64.zero
    )
}

operator fun Symbol.plus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials,
        rhs.constant
    )
}

operator fun Symbol.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, this)) + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        -rhs.constant
    )
}

operator fun LinearPolynomial<Flt64>.plus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        monomials + LinearMonomial(Flt64.one, rhs),
        constant
    )
}

operator fun LinearPolynomial<Flt64>.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        monomials + LinearMonomial(-Flt64.one, rhs),
        constant
    )
}

operator fun UInt64.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-Flt64.one, rhs)),
        this.toFlt64()
    )
}

operator fun Int.minus(rhs: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-Flt64.one, rhs)),
        Flt64(this.toDouble())
    )
}

operator fun UInt64.times(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = this.toFlt64()
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(it.coefficient * scalar, it.symbol) },
        rhs.constant * scalar
    )
}

operator fun UInt64.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    val scalar = this.toFlt64()
    return LinearPolynomial(
        rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        scalar - rhs.constant
    )
}