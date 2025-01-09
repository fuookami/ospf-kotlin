package fuookami.ospf.kotlin.core.frontend.expression.monomial

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.physics.unit.*
import fuookami.ospf.kotlin.utils.physics.quantity.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

data class LinearMonomialCell internal constructor(
    val cell: Either<LinearCellPair, Flt64>
) : MonomialCell<LinearMonomialCell> {
    private val logger = logger()

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

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (cell) {
            is Either.Left -> {
                val token = tokenList.find(cell.value.variable)
                if (token != null) {
                    val result = token.result
                    if (result != null) {
                        cell.value.coefficient * result
                    } else {
                        logger.trace { "Unknown result for ${cell.value.variable}" }
                        if (zeroIfNone) {
                            Flt64.zero
                        } else {
                            null
                        }
                    }
                } else {
                    logger.trace { "Unknown token for ${cell.value.variable}" }
                    if (zeroIfNone) {
                        Flt64.zero
                    } else {
                        null
                    }
                }
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

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (cell) {
            is Either.Left -> {
                val index = tokenList.indexOf(cell.value.variable)
                if (index != null) {
                    val result = results[index]
                    cell.value.coefficient * result
                } else {
                    logger.trace { "Unknown result for ${cell.value.variable}" }
                    if (zeroIfNone) {
                        Flt64.zero
                    } else {
                        null
                    }
                }
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

typealias LinearMonomialSymbolUnit = Either<AbstractVariableItem<*, *>, LinearIntermediateSymbol>

data class LinearMonomialSymbol(
    val symbol: LinearMonomialSymbolUnit
) : MonomialSymbol, Eq<LinearMonomialSymbol> {
    private val logger = logger()

    companion object {
        operator fun invoke(variable: AbstractVariableItem<*, *>): LinearMonomialSymbol {
            return LinearMonomialSymbol(Either.Left(variable))
        }

        operator fun invoke(symbol: LinearIntermediateSymbol): LinearMonomialSymbol {
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
                    is ExpressionSymbol -> {
                        "(${exprSymbol.toRawString(unfold)})"
                    }

                    else -> {
                        "$exprSymbol"
                    }
                }
            }
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                val token = tokenList.find(symbol.value)
                if (token != null) {
                    val result = token.result
                    if (result != null) {
                        result
                    } else {
                        logger.trace { "Unknown result for ${symbol.value}" }
                        if (zeroIfNone) {
                            Flt64.zero
                        } else {
                            null
                        }
                    }
                } else {
                    logger.trace { "Unknown token for ${symbol.value}" }
                    if (zeroIfNone) {
                        Flt64.zero
                    } else {
                        null
                    }
                }
            }

            is Either.Right -> {
                symbol.value.evaluate(tokenList, zeroIfNone)
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                val index = tokenList.indexOf(symbol.value)
                if (index != null) {
                    results[index]
                } else {
                    logger.trace { "Unknown result for ${symbol.value}" }
                    if (zeroIfNone) {
                        Flt64.zero
                    } else {
                        null
                    }
                }
            }

            is Either.Right -> {
                symbol.value.evaluate(results, tokenList, zeroIfNone)
            }
        }
    }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                evaluate(tokenTable.tokenList, zeroIfNone)
            }

            is Either.Right -> {
                symbol.value.evaluate(tokenTable, zeroIfNone)
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                evaluate(results, tokenTable.tokenList, zeroIfNone)
            }

            is Either.Right -> {
                symbol.value.evaluate(results, tokenTable, zeroIfNone)
            }
        }
    }
}

data class LinearMonomial(
    override val coefficient: Flt64,
    override val symbol: LinearMonomialSymbol,
    override var name: String = "",
    override var displayName: String? = null
) : Monomial<LinearMonomial, LinearMonomialCell> {
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

        operator fun invoke(symbol: LinearIntermediateSymbol): LinearMonomial {
            return LinearMonomial(Flt64.one, LinearMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Int, symbol: LinearIntermediateSymbol): LinearMonomial {
            return LinearMonomial(Flt64(coefficient), LinearMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Double, symbol: LinearIntermediateSymbol): LinearMonomial {
            return LinearMonomial(Flt64(coefficient), LinearMonomialSymbol(symbol))
        }

        operator fun <T : RealNumber<T>> invoke(coefficient: T, symbol: LinearIntermediateSymbol): LinearMonomial {
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
                _range = if (symbol.range.range != null) {
                    (coefficient * symbol.range.range!!.toFlt64())?.let {
                        ExpressionRange(it, Flt64)
                    } ?: ExpressionRange(null, Flt64)
                } else {
                    ExpressionRange(null, Flt64)
                }
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
}

// quantity variable conversion

@JvmName("quantityVariableConversion")
fun Quantity<AbstractVariableItem<*, *>>.to(targetUnit: PhysicalUnit): Quantity<LinearMonomial>? {
    return unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, this.unit)
    }
}

// quantity symbol conversion

@JvmName("quantitySymbolConversion")
fun Quantity<LinearIntermediateSymbol>.to(targetUnit: PhysicalUnit): Quantity<LinearMonomial>? {
    return unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, this.unit)
    }
}

// quantity monomial conversion

@JvmName("quantityMonomialConversion")
fun Quantity<LinearMonomial>.to(targetUnit: PhysicalUnit): Quantity<LinearMonomial>? {
    return unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, this.unit)
    }
}

// unary minus variable

operator fun AbstractVariableItem<*, *>.unaryMinus(): LinearMonomial {
    return -Flt64.one * this
}

@JvmName("unaryMinusQuantityVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.unaryMinus(): Quantity<LinearMonomial> {
    return Quantity(-this.value, this.unit)
}

// unary minus symbol

operator fun LinearIntermediateSymbol.unaryMinus(): LinearMonomial {
    return -Flt64.one * this
}

@JvmName("unaryMinusQuantitySymbol")
operator fun Quantity<LinearIntermediateSymbol>.unaryMinus(): Quantity<LinearMonomial> {
    return Quantity(-this.value, this.unit)
}

// unary minus monomial

@JvmName("unaryMinusQuantityMonomial")
operator fun Quantity<LinearMonomial>.unaryMinus(): Quantity<LinearMonomial> {
    return Quantity(-this.value, this.unit)
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

// variable and quantity

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), LinearMonomialSymbol(this)), rhs.unit)
}

operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: AbstractVariableItem<*, *>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.value.toFlt64(), LinearMonomialSymbol(rhs)), this.unit)
}

operator fun <T : RealNumber<T>> AbstractVariableItem<*, *>.div(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64().reciprocal(), LinearMonomialSymbol(this)), rhs.unit.reciprocal())
}

// quantity variable and constant

@JvmName("quantityVariableTimesInt")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Int): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("quantityVariableTimesDouble")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Double): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("quantityVariableTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.times(rhs: T): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("intTimesQuantityVariable")
operator fun Int.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@JvmName("doubleTimesQuantityVariable")
operator fun Double.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@JvmName("realNumberTimesQuantityVariable")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@JvmName("quantityVariableDivInt")
operator fun Quantity<AbstractVariableItem<*, *>>.div(rhs: Int): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("quantityVariableDivDouble")
operator fun Quantity<AbstractVariableItem<*, *>>.div(rhs: Double): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("quantityVariableDivRealNumber")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.div(rhs: T): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this.value)), this.unit)
}

// quantity variable and quantity

@JvmName("quantityVariableTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), LinearMonomialSymbol(this.value)), this.unit * rhs.unit)
}

@JvmName("quantityTimesQuantityVariable")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.value.toFlt64(), LinearMonomialSymbol(rhs.value)), this.unit * rhs.unit)
}

@JvmName("quantityVariableDivQuantity")
operator fun <T : RealNumber<T>> Quantity<AbstractVariableItem<*, *>>.div(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64().reciprocal(), LinearMonomialSymbol(this.value)), this.unit / rhs.unit)
}

// symbol and constant

operator fun LinearIntermediateSymbol.times(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

operator fun LinearIntermediateSymbol.times(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this))
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.times(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this))
}

operator fun Int.times(rhs: LinearIntermediateSymbol): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

operator fun Double.times(rhs: LinearIntermediateSymbol): LinearMonomial {
    return LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs))
}

operator fun <T : RealNumber<T>> T.times(rhs: LinearIntermediateSymbol): LinearMonomial {
    return LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs))
}

operator fun LinearIntermediateSymbol.div(rhs: Int): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

operator fun LinearIntermediateSymbol.div(rhs: Double): LinearMonomial {
    return LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this))
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.div(rhs: T): LinearMonomial {
    return LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this))
}

// symbol and quantity

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), LinearMonomialSymbol(this)), rhs.unit)
}

operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: LinearIntermediateSymbol): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.value.toFlt64(), LinearMonomialSymbol(rhs)), this.unit)
}

operator fun <T : RealNumber<T>> LinearIntermediateSymbol.div(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64().reciprocal(), LinearMonomialSymbol(this)), rhs.unit.reciprocal())
}

// quantity symbol and constant

@JvmName("quantitySymbolTimesInt")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Int): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("quantitySymbolTimesDouble")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Double): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("quantitySymbolTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.times(rhs: T): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.toFlt64(), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("intTimesQuantitySymbol")
operator fun Int.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@JvmName("doubleTimesQuantitySymbol")
operator fun Double.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(this), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@JvmName("realNumberTimesQuantitySymbol")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.toFlt64(), LinearMonomialSymbol(rhs.value)), rhs.unit)
}

@JvmName("quantitySymbolDivInt")
operator fun Quantity<LinearIntermediateSymbol>.div(rhs: Int): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("quantitySymbolDivDouble")
operator fun Quantity<LinearIntermediateSymbol>.div(rhs: Double): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(Flt64(rhs).reciprocal(), LinearMonomialSymbol(this.value)), this.unit)
}

@JvmName("quantitySymbolDivRealNumber")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.div(rhs: T): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.toFlt64().reciprocal(), LinearMonomialSymbol(this.value)), this.unit)
}

// quantity symbol and quantity

@JvmName("quantitySymbolTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64(), LinearMonomialSymbol(this.value)), this.unit * rhs.unit)
}

@JvmName("quantityTimesQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(this.value.toFlt64(), LinearMonomialSymbol(rhs.value)), this.unit * rhs.unit)
}

@JvmName("quantitySymbolDivQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearIntermediateSymbol>.div(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(LinearMonomial(rhs.value.toFlt64().reciprocal(), LinearMonomialSymbol(this.value)), this.unit / rhs.unit)
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

// monomial and unit

operator fun LinearMonomial.times(rhs: PhysicalUnit): Quantity<LinearMonomial> {
    return Quantity(this, rhs)
}

// monomial and quantity

@JvmName("quantityTimesMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: LinearMonomial): Quantity<LinearMonomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("monomialTimesQuantity")
operator fun <T : RealNumber<T>> LinearMonomial.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("monomialDivQuantity")
operator fun <T : RealNumber<T>> LinearMonomial.div(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(this / rhs.value, rhs.unit.reciprocal())
}

// quantity monomial and quantity

@JvmName("quantityTimesQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<LinearMonomial>): Quantity<LinearMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityMonomialTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearMonomial>.times(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityMonomialDivQuantity")
operator fun <T : RealNumber<T>> Quantity<LinearMonomial>.div(rhs: Quantity<T>): Quantity<LinearMonomial> {
    return Quantity(this.value / rhs.value, this.unit / rhs.unit)
}
