package fuookami.ospf.kotlin.core.frontend.expression.polynomial

import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.utils.math.*

private typealias ExprSymbol = Symbol<Linear>
private typealias ExprSymbols = SymbolCombination<Linear, *>
private typealias ExprSymbolView = SymbolView<Linear>

class LinearPolynomial(
    constant: Flt64 = Flt64.zero,
    override var name: String = "",
    override var displayName: String? = null
) : Polynomial<Linear> {
    private val impl = PolynomialImpl<LinearMonomial, Linear>(constant = constant) { cells() }

    override val monomials: ArrayList<LinearMonomial> by impl::monomials
    override var constant: Flt64 by impl::constant

    override val possibleRange: ValueRange<Flt64> by impl::possibleRange
    override var range: ValueRange<Flt64> by impl::range

    override val cells: List<MonomialCell<Linear>> by impl::cells

    constructor(item: Item<*, *>) : this() {
        monomials.add(LinearMonomial(item))
        name = item.name
    }

    constructor(symbol: ExprSymbol) : this() {
        monomials.add(LinearMonomial(symbol))
        name = symbol.name
        displayName = symbol.displayName
    }

    constructor(monomial: LinearMonomial) : this() {
        monomials.add(monomial)
    }

    constructor(polynomial: LinearPolynomial) : this(polynomial.constant) {
        monomials.addAll(polynomial.monomials.asIterable().map { LinearMonomial(it) })
        name = polynomial.name
        displayName = polynomial.displayName
    }

    override fun flush() = impl.flush()

    override fun intersectRange(range: ValueRange<Flt64>) = impl.intersectRange(range)
    override fun rangeLess(value: Flt64) = impl.rangeLess(value)
    override fun rangeLessEqual(value: Flt64) = impl.rangeLessEqual(value)
    override fun rangeGreater(value: Flt64) = impl.rangeGreater(value)
    override fun rangeGreaterEqual(value: Flt64) = impl.rangeGreaterEqual(value)

    operator fun <T : RealNumber<T>> plusAssign(rhs: T) {
        constant += rhs.toFlt64()
    }

    operator fun plusAssign(rhs: Item<*, *>) {
        monomials.add(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs)))
    }

    operator fun plusAssign(rhs: ExprSymbol) {
        monomials.add(LinearMonomial(Flt64.one, LinearMonomialSymbol(rhs)))
    }

    operator fun plusAssign(rhs: LinearMonomial) {
        monomials.add(rhs)
    }

    operator fun plusAssign(rhs: LinearPolynomial) {
        monomials.addAll(rhs.monomials.asIterable().map { LinearMonomial(it) })
        constant += rhs.constant
    }

    operator fun <T : RealNumber<T>> minusAssign(rhs: T) {
        constant -= rhs.toFlt64()
    }

    operator fun minusAssign(rhs: Item<*, *>) {
        monomials.add(LinearMonomial(-Flt64.one, LinearMonomialSymbol(rhs)))
    }

    operator fun minusAssign(rhs: ExprSymbol) {
        monomials.add(LinearMonomial(-Flt64.one, LinearMonomialSymbol(rhs)))
    }

    operator fun minusAssign(rhs: LinearMonomial) {
        monomials.add(-rhs)
    }

    operator fun minusAssign(rhs: LinearPolynomial) {
        monomials.addAll(rhs.monomials.asIterable().map { -it })
        constant -= rhs.constant
    }

    operator fun <T : RealNumber<T>> timesAssign(rhs: T) {
        for (monomial in monomials) {
            monomial *= rhs
        }
        constant *= rhs.toFlt64()
    }

    operator fun <T : RealNumber<T>> divAssign(rhs: T) {
        for (monomial in monomials) {
            monomial /= rhs
        }
        constant /= rhs.toFlt64()
    }

    override fun toString(): String {
        return if (monomials.isEmpty()) {
            "$constant"
        } else if (constant eq Flt64.zero) {
            monomials.joinToString(" + ") { it.toString() }
        } else {
            "${monomials.joinToString(" + ") { it.toString() }} + $constant"
        }
    }

    override fun toRawString(): String {
        return if (monomials.isEmpty()) {
            "$constant"
        } else if (constant eq Flt64.zero) {
            monomials.joinToString(" + ") { it.toRawString() }
        } else {
            "${monomials.joinToString(" + ") { it.toRawString() }} + $constant"
        }
    }

    private fun cells(): MutableList<MonomialCell<Linear>> {
        val cells = ArrayList<MonomialCell<Linear>>()
        var constant = this.constant
        for (monomial in monomials) {
            val thisCells = monomial.cells
            for (cell in thisCells) {
                if (cell.isConstant()) {
                    constant += cell.constant()!!
                } else {
                    val sameCell = cells.find { it == cell }
                    if (sameCell != null) {
                        sameCell += cell
                    } else {
                        cells.add(cell.copy())
                    }
                }
            }
        }
        cells.sortBy { it.hashCode() }
        cells.add(LinearMonomialCell(constant))
        return cells
    }
}

operator fun <T : RealNumber<T>> LinearPolynomial.times(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly *= rhs
    return poly
}

operator fun <T : RealNumber<T>> LinearPolynomial.div(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly /= rhs
    return poly
}

operator fun <T : RealNumber<T>> T.times(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(rhs)
    poly *= this
    return poly
}

// variable and constant

operator fun <T : RealNumber<T>> Item<*, *>.plus(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun <T : RealNumber<T>> Item<*, *>.minus(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun <T : RealNumber<T>> T.plus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this.toFlt64())
    poly += rhs
    return poly
}

operator fun <T : RealNumber<T>> T.minus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this.toFlt64())
    poly -= rhs
    return poly
}

// symbol and constant

operator fun <T : RealNumber<T>> ExprSymbol.plus(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun <T : RealNumber<T>> ExprSymbol.minus(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun <T : RealNumber<T>> T.plus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this.toFlt64())
    poly += rhs
    return poly
}

operator fun <T : RealNumber<T>> T.minus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this.toFlt64())
    poly -= rhs
    return poly
}

// monomial and constant

operator fun <T : RealNumber<T>> LinearMonomial.plus(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs.toFlt64()
    return poly
}

operator fun <T : RealNumber<T>> LinearMonomial.minus(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs.toFlt64()
    return poly
}

operator fun <T : RealNumber<T>> T.plus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this.toFlt64())
    poly += rhs
    return poly
}

operator fun <T : RealNumber<T>> T.minus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this.toFlt64())
    poly -= rhs
    return poly
}

// polynomial and constant

operator fun <T : RealNumber<T>> LinearPolynomial.plus(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun <T : RealNumber<T>> LinearPolynomial.minus(rhs: T): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun <T : RealNumber<T>> T.plus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this.toFlt64())
    poly += rhs
    return poly
}

operator fun <T : RealNumber<T>> T.minus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this.toFlt64())
    poly -= rhs
    return poly
}

// variable and variable

operator fun Item<*, *>.plus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun Item<*, *>.minus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// symbol and variable

operator fun ExprSymbol.plus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun ExprSymbol.minus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun Item<*, *>.plus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun Item<*, *>.minus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// monomial and variable

operator fun LinearMonomial.plus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun LinearMonomial.minus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun Item<*, *>.plus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun Item<*, *>.minus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// polynomial and variable

operator fun LinearPolynomial.plus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun LinearPolynomial.minus(rhs: Item<*, *>): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun Item<*, *>.plus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun Item<*, *>.minus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// symbol and symbol

operator fun ExprSymbol.plus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun ExprSymbol.minus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// monomial and symbol

operator fun LinearMonomial.plus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun LinearMonomial.minus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun ExprSymbol.plus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun ExprSymbol.minus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// polynomial and symbol

operator fun LinearPolynomial.plus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun LinearPolynomial.minus(rhs: ExprSymbol): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun ExprSymbol.plus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun ExprSymbol.minus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// monomial and monomial

operator fun LinearMonomial.plus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun LinearMonomial.minus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// polynomial and monomial

operator fun LinearPolynomial.plus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun LinearPolynomial.minus(rhs: LinearMonomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

operator fun LinearMonomial.plus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun LinearMonomial.minus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// polynomial and polynomial

operator fun LinearPolynomial.plus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly += rhs
    return poly
}

operator fun LinearPolynomial.minus(rhs: LinearPolynomial): LinearPolynomial {
    val poly = LinearPolynomial(this)
    poly -= rhs
    return poly
}

// sigma

fun sum(items: Combination<*, *, *>): LinearPolynomial {
    val poly = LinearPolynomial()
    for (item in items) {
        poly += item!!
    }
    return poly
}

@JvmName("variableViewSum")
fun sum(items: CombinationView<*, *>): LinearPolynomial {
    val poly = LinearPolynomial()
    for (item in items) {
        poly += item!!
    }
    return poly
}

fun sum(symbols: ExprSymbols): LinearPolynomial {
    val poly = LinearPolynomial()
    for (symbol in symbols) {
        poly += symbol!!
    }
    return poly
}

@JvmName("symbolViewSum")
fun sum(symbols: ExprSymbolView): LinearPolynomial {
    val poly = LinearPolynomial()
    for (symbol in symbols) {
        poly += symbol!!
    }
    return poly
}
