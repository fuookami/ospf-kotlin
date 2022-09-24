package fuookami.ospf.kotlin.core.frontend.expression.monomial

import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.variable.Item
import fuookami.ospf.kotlin.utils.concept.Cloneable
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

private typealias ExprSymbol = Symbol<Linear>

data class LinearCellPair(
    var coefficient: Flt64,
    val variable: Item<*, *>
): Cloneable<LinearCellPair> {
    operator fun plus(rhs: LinearCellPair): LinearCellPair {
        if (variable != rhs.variable) {
            throw IllegalArgumentException("Invalid argument of LinearCellPair.plus: not same variable.")
        }
        return LinearCellPair(coefficient + rhs.coefficient, variable)
    }

    operator fun minus(rhs: LinearCellPair): LinearCellPair {
        if (variable != rhs.variable) {
            throw IllegalArgumentException("Invalid argument of LinearCellPair.minus: not same variable.")
        }
        return LinearCellPair(coefficient - rhs.coefficient, variable)
    }

    operator fun times(rhs: Flt64) = LinearCellPair(coefficient * rhs, variable)

    override fun clone(): LinearCellPair {
        return LinearCellPair(coefficient, variable)
    }

    override fun hashCode(): Int = variable.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearCellPair) return false

        if (coefficient != other.coefficient) return false
        if (variable != other.variable) return false

        return true
    }
}

class LinearMonomialCell internal constructor(
    val cell: Either<LinearCellPair, Flt64>
) : MonomialCell<Linear> {
    companion object {
        operator fun invoke(coefficient: Flt64, variable: Item<*, *>) =
            LinearMonomialCell(Either.Left(LinearCellPair(coefficient, variable)))

        operator fun invoke(constant: Flt64) = LinearMonomialCell(Either.Right(constant))
    }

    fun isPair() = cell.isLeft()
    override fun isConstant() = cell.isRight()

    fun pair() = cell.left()
    override fun constant() = cell.right()

    @Throws(IllegalArgumentException::class)
    override operator fun plusAssign(rhs: MonomialCell<Linear>) {
        if (rhs !is LinearMonomialCell) {
            throw IllegalArgumentException("Invalid argument of LinearMonomialCell.plus: ${rhs.javaClass}.")
        }

        when (cell) {
            is Either.Left -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        cell.value += rhs.cell.value
                    }
                    is Either.Right -> {
                        throw IllegalArgumentException("Invalid argument of LinearMonomialCell.plus: monomial and constant.")
                    }
                }
            }
            is Either.Right -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        throw IllegalArgumentException("Invalid argument of LinearMonomialCell.plus: monomial and constant.")
                    }
                    is Either.Right -> {
                        cell.value += rhs.cell.value
                    }
                }
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override operator fun minusAssign(rhs: MonomialCell<Linear>) {
        if (rhs !is LinearMonomialCell) {
            throw IllegalArgumentException("Invalid argument of LinearMonomialCell.plus: ${rhs.javaClass}.")
        }

        when (cell) {
            is Either.Left -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        cell.value -= rhs.cell.value
                    }
                    is Either.Right -> {
                        throw IllegalArgumentException("Invalid argument of LinearMonomialCell.plus: monomial and constant.")
                    }
                }
            }
            is Either.Right -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        throw IllegalArgumentException("Invalid argument of LinearMonomialCell.plus: monomial and constant.")
                    }
                    is Either.Right -> {
                        cell.value -= rhs.cell.value
                    }
                }
            }
        }
    }

    override operator fun <T : RealNumber<T>> timesAssign(rhs: T) {
        when (cell) {
            is Either.Left -> {
                cell.value *= rhs.toFlt64()
            }
            is Either.Right -> {
                cell.value *= rhs.toFlt64()
            }
        }
    }

    override fun clone(): LinearMonomialCell {
        return when (cell) {
            is Either.Left -> {
                LinearMonomialCell(cell.value.coefficient, cell.value.variable)
            }
            is Either.Right -> {
                LinearMonomialCell(cell.value)
            }
        }
    }

    override fun hashCode(): Int = cell.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearMonomialCell) return false

        if (cell != other.cell) return false

        return true
    }
}

class LinearMonomialSymbol(
    val symbol: Either<Item<*, *>, ExprSymbol>
) : MonomialSymbol<Linear>, Eq<LinearMonomialSymbol> {
    companion object {
        operator fun invoke(variable: Item<*, *>) = LinearMonomialSymbol(Either.Left(variable))
        operator fun invoke(symbol: ExprSymbol) = LinearMonomialSymbol(Either.Right(symbol))
    }

    override val name
        get() = when (symbol) {
            is Either.Left -> {
                symbol.value.name
            }
            is Either.Right -> {
                symbol.value.name
            }
        }

    override val range
        get() = when (symbol) {
            is Either.Left -> {
                symbol.value.range.valueRange
            }
            is Either.Right -> {
                symbol.value.range
            }
        }

    override val lowerBound
        get() = when (symbol) {
            is Either.Left -> {
                symbol.value.lowerBound
            }
            is Either.Right -> {
                symbol.value.lowerBound
            }
        }
    override val upperBound
        get() = when (symbol) {
            is Either.Left -> {
                symbol.value.upperBound
            }
            is Either.Right -> {
                symbol.value.upperBound
            }
        }

    fun pure() = symbol.isLeft()
    fun variable() = symbol.left()
    fun exprSymbol() = symbol.right()

    fun cells(): List<MonomialCell<Linear>> = when (symbol) {
        is Either.Left -> {
            arrayListOf(LinearMonomialCell(Flt64.one, symbol.value))
        }
        is Either.Right -> {
            symbol.value.cells.asSequence().map{ it.clone() }.toList()
        }
    }

    override fun partialEq(rhs: LinearMonomialSymbol) = variable() == rhs.variable()
        && exprSymbol() == rhs.exprSymbol()

    override fun toString() = when (symbol) {
        is Either.Left -> {
            symbol.value.name
        }
        is Either.Right -> {
            "(${symbol.value.toRawString()})"
        }
    }

    override fun hashCode() = when (symbol) {
        is Either.Left -> {
            symbol.value.hashCode()
        }
        is Either.Right -> {
            symbol.value.name.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearMonomialSymbol) return false

        if (!this.eq(other)) return false

        return true
    }
}

class LinearMonomial(
    coefficient: Flt64,
    symbol: LinearMonomialSymbol,
    override var name: String = "",
    override var displayName: String? = null
) : SimpleMonomial<Linear> {
    private val impl = MonomialImpl(coefficient, symbol) { cells() }

    override val category = Linear
    override var coefficient: Flt64 by impl::coefficient
    override val symbol: LinearMonomialSymbol by impl::symbol

    override val possibleRange: ValueRange<Flt64> by impl::possibleRange
    override var range: ValueRange<Flt64> by impl::range

    override val cells: List<MonomialCell<Linear>> get() = impl.cells

    constructor(item: Item<*, *>) : this(Flt64.one, LinearMonomialSymbol(item))
    constructor(symbol: ExprSymbol) : this(Flt64.one, LinearMonomialSymbol(symbol))
    constructor(monomial: LinearMonomial) : this(monomial.coefficient, monomial.symbol)

    override fun flush() = impl.flush()

    override fun intersectRange(range: ValueRange<Flt64>) = impl.intersectRange(range)
    override fun rangeLess(value: Flt64) = impl.rangeLess(value)
    override fun rangeLessEqual(value: Flt64) = impl.rangeLessEqual(value)
    override fun rangeGreater(value: Flt64) = impl.rangeGreater(value)
    override fun rangeGreaterEqual(value: Flt64) = impl.rangeGreaterEqual(value)

    override fun unaryMinus() = LinearMonomial(-coefficient, symbol)

    operator fun <T : RealNumber<T>> timesAssign(rhs: T) {
        coefficient *= rhs.toFlt64()
    }

    operator fun <T : RealNumber<T>> divAssign(rhs: T) {
        coefficient /= rhs.toFlt64()
    }

    override fun toString() = if (coefficient eq Flt64.one) { symbol.name } else { "$coefficient * ${symbol.name}" }
    override fun toRawString() = if (coefficient eq Flt64.one) { symbol.toString() } else { "$coefficient * $symbol" }

    private fun cells(): MutableList<MonomialCell<Linear>> {
        val ret = symbol.cells().toMutableList()
        for (cell in ret) {
            cell *= coefficient
        }
        return ret
    }
}

// variable and constant

operator fun <T : RealNumber<T>> Item<*, *>.times(rhs: T) = LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this))
operator fun <T : RealNumber<T>> T.times(rhs: Item<*, *>) = LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs))
operator fun <T : RealNumber<T>> Item<*, *>.div(rhs: T) = LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this))

// symbol and constant

operator fun <T : RealNumber<T>> ExprSymbol.times(rhs: T) = LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this))
operator fun <T : RealNumber<T>> T.times(rhs: ExprSymbol) = LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs))
operator fun <T : RealNumber<T>> ExprSymbol.div(rhs: T) = LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this))

// monomial and constant

operator fun <T : RealNumber<T>> LinearMonomial.times(rhs: T) = LinearMonomial(this.coefficient * rhs.toFlt64(), this.symbol)
operator fun <T : RealNumber<T>> T.times(rhs: LinearMonomial) = LinearMonomial(this.toFlt64() * rhs.coefficient, rhs.symbol)
operator fun <T : RealNumber<T>> LinearMonomial.div(rhs: T) = LinearMonomial(this.coefficient / rhs.toFlt64(), this.symbol)
