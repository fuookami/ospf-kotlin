/**
 * 多项式快捷 DSL
 * Polynomial Quick DSL
 *
 * 提供多项式构建的 DSL 扩展函数，用于快速创建和操作多项式。
 * Provides DSL extension functions for polynomial construction, enabling quick creation and manipulation of polynomials.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64 as F64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial

import kotlin.jvm.JvmName

@JvmName("quickLinearPolynomialZero")
fun LinearPolynomial(): LinearPolynomial<F64> {
    return LinearPolynomial(emptyList(), F64.zero)
}

@JvmName("quickLinearPolynomialFromConstant")
fun LinearPolynomial(constant: F64): LinearPolynomial<F64> {
    return LinearPolynomial(emptyList(), constant)
}

@JvmName("quickLinearPolynomialFromMonomial")
fun LinearPolynomial(monomial: LinearMonomial<F64>): LinearPolynomial<F64> {
    return LinearPolynomial(listOf(monomial), F64.zero)
}

@JvmName("quickLinearPolynomialFromMutable")
fun <T : NumberField<T>> LinearPolynomial(poly: MutableLinearPolynomial<T>): LinearPolynomial<T> {
    return poly.toLinearPolynomial()
}

@JvmName("quickLinearPolynomialFromSymbol")
fun LinearPolynomial(symbol: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(listOf(LinearMonomial(F64.one, symbol)), F64.zero)
}

@JvmName("quickMutableLinearPolynomialZero")
fun MutableLinearPolynomial(): MutableLinearPolynomial<F64> {
    return MutableLinearPolynomial(emptyList(), F64.zero)
}

@JvmName("quickMutableLinearPolynomialFromMonomial")
fun MutableLinearPolynomial(monomial: LinearMonomial<F64>): MutableLinearPolynomial<F64> {
    return MutableLinearPolynomial(listOf(monomial), F64.zero)
}

@JvmName("quickSumByNullableMonomial")
fun <T : Ring<T>, E> sum(
    items: Iterable<E>,
    transform: (E) -> LinearMonomial<T>?
): LinearPolynomial<T> {
    val list = items.mapNotNull(transform)
    if (list.isEmpty()) {
        throw IllegalArgumentException("sum(items, transform): empty monomial list")
    }
    return sum(list)
}

@JvmName("quickFlatSumNullable")
fun <T : Ring<T>, E> flatSum(
    list: Iterable<E>,
    transform: (E) -> Iterable<LinearMonomial<T>?>
): LinearPolynomial<T> {
    val monomials = mutableListOf<LinearMonomial<T>>()
    for (e in list) {
        monomials += transform(e).filterNotNull()
    }
    if (monomials.isEmpty()) {
        throw IllegalArgumentException("flatSum(list, transform): empty monomial list")
    }
    return sum(monomials)
}

@JvmName("quickSumVars")
fun <E> sumVars(
    items: Iterable<E>,
    selector: (E) -> Symbol?
): LinearPolynomial<F64> {
    val monomials = items.mapNotNull(selector).map { LinearMonomial(F64.one, it) }
    return LinearPolynomial(monomials, F64.zero)
}

@JvmName("quickSumSymbols")
fun sum(symbols: Iterable<Symbol>): LinearPolynomial<F64> {
    return LinearPolynomial(
        symbols.map { LinearMonomial(F64.one, it) },
        F64.zero
    )
}

operator fun Symbol.unaryMinus(): LinearMonomial<F64> {
    return LinearMonomial(-F64.one, this)
}

operator fun Symbol.plus(rhs: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(LinearMonomial(F64.one, this), LinearMonomial(F64.one, rhs)),
        F64.zero
    )
}

operator fun Symbol.minus(rhs: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(LinearMonomial(F64.one, this), LinearMonomial(-F64.one, rhs)),
        F64.zero
    )
}

operator fun Symbol.plus(rhs: LinearMonomial<F64>): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(LinearMonomial(F64.one, this), rhs),
        F64.zero
    )
}

operator fun Symbol.minus(rhs: LinearMonomial<F64>): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(LinearMonomial(F64.one, this), LinearMonomial(-rhs.coefficient, rhs.symbol)),
        F64.zero
    )
}

operator fun LinearMonomial<F64>.plus(rhs: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(this, LinearMonomial(F64.one, rhs)),
        F64.zero
    )
}

operator fun LinearMonomial<F64>.minus(rhs: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(this, LinearMonomial(-F64.one, rhs)),
        F64.zero
    )
}

operator fun Symbol.plus(rhs: LinearPolynomial<F64>): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(LinearMonomial(F64.one, this)) + rhs.monomials,
        rhs.constant
    )
}

operator fun Symbol.minus(rhs: LinearPolynomial<F64>): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(LinearMonomial(F64.one, this)) + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        -rhs.constant
    )
}

operator fun LinearPolynomial<F64>.plus(rhs: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        monomials + LinearMonomial(F64.one, rhs),
        constant
    )
}

operator fun LinearPolynomial<F64>.minus(rhs: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        monomials + LinearMonomial(-F64.one, rhs),
        constant
    )
}

operator fun UInt64.minus(rhs: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-F64.one, rhs)),
        this.toFlt64()
    )
}

operator fun Int.minus(rhs: Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        listOf(LinearMonomial(-F64.one, rhs)),
        F64(this.toDouble())
    )
}

operator fun UInt64.times(rhs: LinearPolynomial<F64>): LinearPolynomial<F64> {
    return rhs * this.toFlt64()
}

operator fun UInt64.minus(rhs: LinearPolynomial<F64>): LinearPolynomial<F64> {
    return this.toFlt64() - rhs
}
