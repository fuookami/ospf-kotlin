package fuookami.ospf.kotlin.core.frontend.expression.monomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

data class LinearMonomialCell internal constructor(
    val cell: Either<LinearCellPair, Flt64>
) : MonomialCell<LinearMonomialCell, Linear> {
    data class LinearCellPair(
        val coefficient: Flt64,
        val variable: AbstractVariableItem<*, *>
    ) : Cloneable, Copyable<LinearCellPair> {
        operator fun unaryMinus(): LinearCellPair {
            return LinearCellPair(-coefficient, variable)
        }

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
        operator fun div(rhs: Flt64) = LinearCellPair(coefficient / rhs, variable)

        override fun copy(): LinearCellPair {
            return LinearCellPair(coefficient, variable)
        }

        public override fun clone() = copy()

        override fun hashCode(): Int = variable.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LinearCellPair) return false

            if (coefficient != other.coefficient) return false
            if (variable != other.variable) return false

            return true
        }
    }

    companion object {
        operator fun invoke(coefficient: Flt64, variable: AbstractVariableItem<*, *>) =
            LinearMonomialCell(Either.Left(LinearCellPair(coefficient, variable)))

        operator fun invoke(variable: AbstractVariableItem<*, *>) =
            LinearMonomialCell(Either.Left(LinearCellPair(Flt64.one, variable)))

        operator fun invoke(constant: Flt64) =
            LinearMonomialCell(Either.Right(constant))
    }

    val isPair by cell::isLeft
    override val isConstant by cell::isRight

    val pair by cell::left
    override val constant by cell::right

    override fun unaryMinus(): LinearMonomialCell {
        return when (cell) {
            is Either.Left -> {
                LinearMonomialCell(Either.Left(-cell.value))
            }

            is Either.Right -> {
                LinearMonomialCell(Either.Right(-cell.value))
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override operator fun plus(rhs: LinearMonomialCell): LinearMonomialCell {
        return when (cell) {
            is Either.Left -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        LinearMonomialCell(
                            Either.Left(cell.value + rhs.cell.value)
                        )
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
                        LinearMonomialCell(
                            Either.Right(cell.value + rhs.cell.value)
                        )
                    }
                }
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override operator fun minus(rhs: LinearMonomialCell): LinearMonomialCell {
        return when (cell) {
            is Either.Left -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        LinearMonomialCell(
                            Either.Left(cell.value - rhs.cell.value)
                        )
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
                        LinearMonomialCell(
                            Either.Right(cell.value - rhs.cell.value)
                        )
                    }
                }
            }
        }
    }

    override fun times(rhs: Flt64): LinearMonomialCell {
        return when (cell) {
            is Either.Left -> {
                LinearMonomialCell(
                    Either.Left(cell.value * rhs)
                )
            }

            is Either.Right -> {
                LinearMonomialCell(
                    Either.Right(cell.value * rhs)
                )
            }
        }
    }

    override fun div(rhs: Flt64): LinearMonomialCell {
        return when (cell) {
            is Either.Left -> {
                LinearMonomialCell(
                    Either.Left(cell.value / rhs)
                )
            }

            is Either.Right -> {
                LinearMonomialCell(
                    Either.Right(cell.value / rhs)
                )
            }
        }
    }

    override fun copy(): LinearMonomialCell {
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

    override fun toString(): String {
        return when (cell) {
            is Either.Left -> {
                "${cell.value.coefficient} * ${cell.value.variable}"
            }

            is Either.Right -> {
                "${cell.value}"
            }
        }
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (cell) {
            is Either.Left -> {
                tokenList.find(cell.value.variable)?.result?.let { cell.value.coefficient * it }
            }

            is Either.Right -> {
                cell.value
            }
        } ?: if (zeroIfNone) {
            Flt64.zero
        } else {
            null
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (cell) {
            is Either.Left -> {
                tokenList.indexOf(cell.value.variable)?.let { cell.value.coefficient * results[it] }
            }

            is Either.Right -> {
                cell.value
            }
        } ?: if (zeroIfNone) {
            Flt64.zero
        } else {
            null
        }
    }
}

typealias LinearMonomialSymbolUnit = Either<AbstractVariableItem<*, *>, LinearSymbol>

data class LinearMonomialSymbol(
    val symbol: LinearMonomialSymbolUnit
) : MonomialSymbol<Linear>, Eq<LinearMonomialSymbol> {
    companion object {
        operator fun invoke(variable: AbstractVariableItem<*, *>): LinearMonomialSymbol {
            return LinearMonomialSymbol(Either.Left(variable))
        }

        operator fun invoke(symbol: LinearSymbol): LinearMonomialSymbol {
            return LinearMonomialSymbol(Either.Right(symbol))
        }
    }

    override val name by lazy {
        when (symbol) {
            is Either.Left -> {
                symbol.value.name
            }

            is Either.Right -> {
                symbol.value.name
            }
        }
    }

    override val displayName: String? by lazy {
        when (symbol) {
            is Either.Left -> {
                symbol.value.name
            }

            is Either.Right -> {
                symbol.value.displayName
            }
        }
    }

    override val category by lazy {
        when (symbol) {
            is Either.Left -> {
                Linear
            }

            is Either.Right -> {
                symbol.value.category
            }
        }
    }

    override val discrete: Boolean by lazy {
        when (symbol) {
            is Either.Left -> {
                symbol.value.type.isIntegerType
            }

            is Either.Right -> {
                symbol.value.discrete
            }
        }
    }

    override val range
        get() = when (symbol) {
            is Either.Left -> {
                symbol.value.range
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

    val pure by symbol::isLeft
    val variable by symbol::left
    val exprSymbol by symbol::right

    val cells: List<LinearMonomialCell>
        get() = when (symbol) {
            is Either.Left -> {
                listOf(LinearMonomialCell(Flt64.one, symbol.value))
            }

            is Either.Right -> {
                symbol.value.cells.map { it.copy() }
            }
        }

    val cached: Boolean
        get() = when (symbol) {
            is Either.Left -> {
                false
            }

            is Either.Right -> {
                symbol.value.cached
            }
        }

    override fun partialEq(rhs: LinearMonomialSymbol): Boolean {
        return variable == rhs.variable
                && exprSymbol == rhs.exprSymbol
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

    override fun toString() = when (symbol) {
        is Either.Left -> {
            symbol.value.name
        }

        is Either.Right -> {
            symbol.value.name
        }
    }

    override fun toRawString(unfold: Boolean): String {
        return when (symbol) {
            is Either.Left -> {
                symbol.value.name
            }

            is Either.Right -> {
                when (val exprSymbol = symbol.value) {
                    is ExpressionSymbol<*, *, *, *> -> {
                        "(${exprSymbol.toRawString(unfold)})"
                    }

                    else -> {
                        "$exprSymbol"
                    }
                }
            }
        }
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                tokenList.find(symbol.value)?.result
                    ?: if (zeroIfNone) {
                        Flt64.zero
                    } else {
                        null
                    }
            }

            is Either.Right -> {
                symbol.value.value(tokenList, zeroIfNone)
            }
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                tokenList.indexOf(symbol.value)?.let { results[it] }
                    ?: if (zeroIfNone) {
                        Flt64.zero
                    } else {
                        null
                    }
            }

            is Either.Right -> {
                symbol.value.value(results, tokenList, zeroIfNone)
            }
        }
    }
}

data class LinearMonomial(
    override val coefficient: Flt64,
    override val symbol: LinearMonomialSymbol,
    override var name: String = "",
    override var displayName: String? = null
) : Monomial<LinearMonomial, LinearMonomialCell, Linear> {
    companion object {
        operator fun invoke(item: AbstractVariableItem<*, *>): LinearMonomial {
            return LinearMonomial(Flt64.one, LinearMonomialSymbol(item))
        }

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>): LinearMonomial {
            return LinearMonomial(Flt64(coefficient), LinearMonomialSymbol(item))
        }

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>): LinearMonomial {
            return LinearMonomial(Flt64(coefficient), LinearMonomialSymbol(item))
        }

        operator fun <T : RealNumber<T>> invoke(coefficient: T, item: AbstractVariableItem<*, *>): LinearMonomial {
            return LinearMonomial(coefficient.toFlt64(), LinearMonomialSymbol(item))
        }

        operator fun invoke(symbol: LinearSymbol): LinearMonomial {
            return LinearMonomial(Flt64.one, LinearMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Int, symbol: LinearSymbol): LinearMonomial {
            return LinearMonomial(Flt64(coefficient), LinearMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Double, symbol: LinearSymbol): LinearMonomial {
            return LinearMonomial(Flt64(coefficient), LinearMonomialSymbol(symbol))
        }

        operator fun <T : RealNumber<T>> invoke(coefficient: T, symbol: LinearSymbol): LinearMonomial {
            return LinearMonomial(coefficient.toFlt64(), LinearMonomialSymbol(symbol))
        }
    }

    val pure by symbol::pure

    override val discrete by lazy {
        (coefficient.round() eq coefficient) && symbol.discrete
    }

    private var _range: ExpressionRange<Flt64>? = null
    override val range: ExpressionRange<Flt64>
        get() {
            if (_range == null) {
                _range = ExpressionRange(
                    coefficient * ValueRange(
                        symbol.lowerBound,
                        symbol.upperBound,
                        symbol.range.lowerInterval,
                        symbol.range.upperInterval
                    ),
                    Flt64
                )
            }
            return _range!!
        }

    private var _cells: List<LinearMonomialCell> = emptyList()
    override val cells: List<LinearMonomialCell>
        get() {
            if (_cells.isEmpty()) {
                _cells = symbol.cells.map { it * coefficient }
            }
            return _cells
        }
    override val cached: Boolean = _cells.isNotEmpty()

    override fun flush(force: Boolean) {
        if (force || _range?.set == false) {
            _range = null
        }
        if (force || !symbol.cached) {
            _cells = emptyList()
        }
    }

    override fun copy(): LinearMonomial {
        return LinearMonomial(coefficient, symbol.copy())
    }

    override fun unaryMinus() = LinearMonomial(-coefficient, symbol.copy())

    override fun times(rhs: Flt64): LinearMonomial {
        return LinearMonomial(coefficient * rhs, symbol.copy())
    }

    override fun div(rhs: Flt64): LinearMonomial {
        return LinearMonomial(coefficient / rhs, symbol.copy())
    }

    override fun toString(): String {
        return displayName ?: name.ifEmpty {
            if (coefficient eq Flt64.one) {
                symbol.name
            } else {
                "$coefficient * $symbol"
            }
        }
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return symbol.value(tokenList, zeroIfNone)?.let { coefficient * it }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return symbol.value(results, tokenList, zeroIfNone)?.let { coefficient * it }
    }
}

// variable and constant

operator fun AbstractVariableItem<*, *>.times(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

operator fun AbstractVariableItem<*, *>.times(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.times(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this))
}

operator fun Int.times(rhs: AbstractVariableItem<*, *>): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

operator fun Double.times(rhs: AbstractVariableItem<*, *>): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

operator fun <T : RealNumber<T>> T.times(rhs: AbstractVariableItem<*, *>): LinearMonomial {
    return LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs))
}

operator fun AbstractVariableItem<*, *>.div(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

operator fun AbstractVariableItem<*, *>.div(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.div(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this))
}

// symbol and constant

operator fun LinearSymbol.times(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

operator fun LinearSymbol.times(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

operator fun <T : RealNumber<T>> LinearSymbol.times(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this))
}

operator fun Int.times(rhs: LinearSymbol): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

operator fun Double.times(rhs: LinearSymbol): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

operator fun <T : RealNumber<T>> T.times(rhs: LinearSymbol): LinearMonomial {
    return LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs))
}

operator fun LinearSymbol.div(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

operator fun LinearSymbol.div(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

operator fun <T : RealNumber<T>> LinearSymbol.div(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this))
}

// monomial and constant

operator fun Int.times(rhs: LinearMonomial): LinearMonomial {
    return LinearMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

operator fun Double.times(rhs: LinearMonomial): LinearMonomial {
    return LinearMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

operator fun <T : RealNumber<T>> T.times(rhs: LinearMonomial): LinearMonomial {
    return LinearMonomial(this.toFlt64() * rhs.coefficient, rhs.symbol)
}
