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

data class QuadraticMonomialCell internal constructor(
    val cell: Either<QuadraticCellTriple, Flt64>
) : MonomialCell<QuadraticMonomialCell> {
    private val logger = logger()

    data class QuadraticCellTriple(
        val coefficient: Flt64,
        val variable1: AbstractVariableItem<*, *>,
        val variable2: AbstractVariableItem<*, *>?
    ) : Cloneable, Copyable<QuadraticCellTriple> {
        init {
            if (variable2 != null) {
                assert(variable1.identifier <= variable2.identifier)
                if (variable1.identifier == variable2.identifier) {
                    assert(variable1.index <= variable2.index)
                }
            }
        }

        operator fun unaryMinus(): QuadraticCellTriple {
            return QuadraticCellTriple(-coefficient, variable1, variable2)
        }

        @Throws(IllegalArgumentException::class)
        operator fun plus(rhs: QuadraticCellTriple): QuadraticCellTriple {
            if (variable1 != rhs.variable1 || variable2 != rhs.variable2) {
                throw IllegalArgumentException("Invalid argument of QuadraticCellTriple.plus: not same variable.")
            }
            return QuadraticCellTriple(coefficient + rhs.coefficient, variable1, variable2)
        }

        @Throws(IllegalArgumentException::class)
        operator fun minus(rhs: QuadraticCellTriple): QuadraticCellTriple {
            if (variable1 != rhs.variable1 || variable2 != rhs.variable2) {
                throw IllegalArgumentException("Invalid argument of QuadraticCellTriple.minus: not same variable.")
            }
            return QuadraticCellTriple(coefficient - rhs.coefficient, variable1, variable2)
        }

        operator fun times(rhs: Flt64) = QuadraticCellTriple(coefficient * rhs, variable1, variable2)

        @Throws(IllegalArgumentException::class)
        operator fun times(rhs: QuadraticCellTriple): QuadraticCellTriple {
            if (variable2 != null || rhs.variable2 != null) {
                throw IllegalArgumentException("Invalid argument of QuadraticCellTriple.times: over quadratic.")
            }
            return QuadraticCellTriple(coefficient * rhs.coefficient, variable1, rhs.variable1)
        }

        operator fun div(rhs: Flt64) = QuadraticCellTriple(coefficient / rhs, variable1, variable2)

        override fun copy(): QuadraticCellTriple {
            return QuadraticCellTriple(coefficient, variable1, variable2)
        }

        public override fun clone() = copy()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as QuadraticCellTriple

            if (coefficient != other.coefficient) return false
            if (variable1 != other.variable1) return false
            if (variable2 != other.variable2) return false

            return true
        }

        override fun hashCode(): Int {
            var result = coefficient.hashCode()
            result = 31 * result + variable1.hashCode()
            result = 31 * result + (variable2?.hashCode() ?: 0)
            return result
        }
    }

    companion object {
        operator fun invoke(
            linearCell: LinearMonomialCell
        ): QuadraticMonomialCell {
            return when (val cell = linearCell.cell) {
                is Either.Left -> {
                    QuadraticMonomialCell(Either.Left(QuadraticCellTriple(cell.value.coefficient, cell.value.variable, null)))
                }

                is Either.Right -> {
                    QuadraticMonomialCell(Either.Right(cell.value))
                }
            }
        }

        operator fun invoke(
            variable1: AbstractVariableItem<*, *>,
            variable2: AbstractVariableItem<*, *>?
        ): QuadraticMonomialCell {
            return if (variable2 == null) {
                QuadraticMonomialCell(Either.Left(QuadraticCellTriple(Flt64.one, variable1, null)))
            } else {
                if ((variable1.key ord variable2.key) is Order.Greater) {
                    QuadraticMonomialCell(Either.Left(QuadraticCellTriple(Flt64.one, variable2, variable1)))
                } else {
                    QuadraticMonomialCell(Either.Left(QuadraticCellTriple(Flt64.one, variable1, variable2)))
                }
            }
        }

        operator fun invoke(
            coefficient: Flt64,
            variable1: AbstractVariableItem<*, *>,
            variable2: AbstractVariableItem<*, *>?
        ): QuadraticMonomialCell {
            return if (variable2 == null) {
                QuadraticMonomialCell(Either.Left(QuadraticCellTriple(coefficient, variable1, null)))
            } else {
                if ((variable1.key ord variable2.key) is Order.Greater) {
                    QuadraticMonomialCell(Either.Left(QuadraticCellTriple(coefficient, variable2, variable1)))
                } else {
                    QuadraticMonomialCell(Either.Left(QuadraticCellTriple(coefficient, variable1, variable2)))
                }
            }
        }

        operator fun invoke(constant: Flt64) = QuadraticMonomialCell(Either.Right(constant))
    }

    val isTriple by cell::isLeft
    override val isConstant by cell::isRight

    val triple by cell::left
    override val constant by cell::right

    override fun unaryMinus(): QuadraticMonomialCell {
        return when (cell) {
            is Either.Left -> {
                QuadraticMonomialCell(Either.Left(-cell.value))
            }

            is Either.Right -> {
                QuadraticMonomialCell(Either.Right(-cell.value))
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override operator fun plus(rhs: QuadraticMonomialCell): QuadraticMonomialCell {
        return when (cell) {
            is Either.Left -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        QuadraticMonomialCell(
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
                        QuadraticMonomialCell(
                            Either.Right(cell.value + rhs.cell.value)
                        )
                    }
                }
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override operator fun minus(rhs: QuadraticMonomialCell): QuadraticMonomialCell {
        return when (cell) {
            is Either.Left -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        QuadraticMonomialCell(
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
                        QuadraticMonomialCell(
                            Either.Right(cell.value - rhs.cell.value)
                        )
                    }
                }
            }
        }
    }

    override fun times(rhs: Flt64): QuadraticMonomialCell {
        return when (cell) {
            is Either.Left -> {
                QuadraticMonomialCell(
                    Either.Left(cell.value * rhs)
                )
            }

            is Either.Right -> {
                QuadraticMonomialCell(
                    Either.Right(cell.value * rhs)
                )
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: QuadraticMonomialCell): QuadraticMonomialCell {
        return when (cell) {
            is Either.Left -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        QuadraticMonomialCell(
                            Either.Left(cell.value * rhs.cell.value)
                        )
                    }

                    is Either.Right -> {
                        QuadraticMonomialCell(
                            Either.Left(cell.value * rhs.cell.value)
                        )
                    }
                }
            }

            is Either.Right -> {
                when (rhs.cell) {
                    is Either.Left -> {
                        QuadraticMonomialCell(
                            Either.Left(rhs.cell.value * cell.value)
                        )
                    }

                    is Either.Right -> {
                        QuadraticMonomialCell(
                            Either.Right(cell.value * rhs.cell.value)
                        )
                    }
                }
            }
        }
    }

    override fun div(rhs: Flt64): QuadraticMonomialCell {
        return when (cell) {
            is Either.Left -> {
                QuadraticMonomialCell(
                    Either.Left(cell.value / rhs)
                )
            }

            is Either.Right -> {
                QuadraticMonomialCell(
                    Either.Right(cell.value / rhs)
                )
            }
        }
    }

    override fun copy(): QuadraticMonomialCell {
        return when (cell) {
            is Either.Left -> {
                QuadraticMonomialCell(cell.value.coefficient, cell.value.variable1, cell.value.variable2)
            }

            is Either.Right -> {
                QuadraticMonomialCell(cell.value)
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
                if (cell.value.variable2 == null) {
                    "${cell.value.coefficient} * ${cell.value.variable1}"
                } else {
                    "${cell.value.coefficient} * ${cell.value.variable1} * ${cell.value.variable2}"
                }
            }

            is Either.Right -> {
                "${cell.value}"
            }
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return when (cell) {
            is Either.Left -> {
                if (cell.value.variable2 == null) {
                    val token = tokenList.find(cell.value.variable1)
                    if (token != null) {
                        val result = token.result
                        if (result != null) {
                            cell.value.coefficient * result
                        } else {
                            logger.trace { "Unknown result for ${cell.value.variable1}." }
                            if (zeroIfNone) {
                                Flt64.zero
                            } else {
                                null
                            }
                        }
                    } else {
                        logger.trace { "Unknown token for ${cell.value.variable1}." }
                        if (zeroIfNone) {
                            Flt64.zero
                        } else {
                            null
                        }
                    }
                } else {
                    val token1 = tokenList.find(cell.value.variable1)
                    if (token1 != null) {
                        val token2 = tokenList.find(cell.value.variable2!!)
                        if (token2 != null) {
                            val result1 = token1.result
                            if (result1 != null) {
                                val result2 = token2.result
                                if (result2 != null) {
                                    cell.value.coefficient * result1 * result2
                                } else {
                                    logger.trace { "Unknown result for ${cell.value.variable1}." }
                                    if (zeroIfNone) {
                                        Flt64.zero
                                    } else {
                                        null
                                    }
                                }
                            } else {
                                logger.trace { "Unknown result for ${cell.value.variable1}." }
                                if (zeroIfNone) {
                                    Flt64.zero
                                } else {
                                    null
                                }
                            }
                        } else {
                            logger.trace { "Unknown token for ${cell.value.variable1}." }
                            if (zeroIfNone) {
                                Flt64.zero
                            } else {
                                null
                            }
                        }
                    } else {
                        logger.trace { "Unknown token for ${cell.value.variable1}." }
                        if (zeroIfNone) {
                            Flt64.zero
                        } else {
                            null
                        }
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
                if (cell.value.variable2 == null) {
                    val index = tokenList.indexOf(cell.value.variable1)
                    if (index != null) {
                        results[index]
                    } else {
                        logger.trace { "Unknown index for ${cell.value.variable1}." }
                        if (zeroIfNone) {
                            Flt64.zero
                        } else {
                            null
                        }
                    }
                } else {
                    val index = tokenList.indexOf(cell.value.variable1)
                    if (index != null) {
                        val index2 = tokenList.indexOf(cell.value.variable2!!)
                        if (index2 != null) {
                            results[index] * results[index2]
                        } else {
                            logger.trace { "Unknown index for ${cell.value.variable2}." }
                            if (zeroIfNone) {
                                Flt64.zero
                            } else {
                                null
                            }
                        }
                    } else {
                        logger.trace { "Unknown index for ${cell.value.variable1}." }
                        if (zeroIfNone) {
                            Flt64.zero
                        } else {
                            null
                        }
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

typealias QuadraticMonomialSymbolUnit = Variant3<AbstractVariableItem<*, *>, LinearIntermediateSymbol, QuadraticIntermediateSymbol>

data class QuadraticMonomialSymbol(
    val symbol1: QuadraticMonomialSymbolUnit,
    val symbol2: QuadraticMonomialSymbolUnit? = null
) : MonomialSymbol, Eq<QuadraticMonomialSymbol> {
    init {
        assert(symbol2 == null || (symbol1.category == Linear && symbol2.category == Linear))
    }

    companion object {
        private val logger = logger()

        operator fun invoke(variable: AbstractVariableItem<*, *>): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V1(variable))
        }

        operator fun invoke(symbol: LinearIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V2(symbol))
        }

        operator fun invoke(symbol: QuadraticIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V3(symbol))
        }

        operator fun invoke(variable1: AbstractVariableItem<*, *>, variable2: AbstractVariableItem<*, *>): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V1(variable1), Variant3.V1(variable2)
            )
        }

        operator fun invoke(variable: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V1(variable), Variant3.V2(symbol)
            )
        }

        operator fun invoke(variable: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V1(variable), Variant3.V3(symbol)
            )
        }

        operator fun invoke(symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V2(symbol1), Variant3.V2(symbol2)
            )
        }

        operator fun invoke(symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V2(symbol1), Variant3.V3(symbol2)
            )
        }

        operator fun invoke(symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V3(symbol1), Variant3.V3(symbol2)
            )
        }

        operator fun invoke(symbol: LinearMonomialSymbol): QuadraticMonomialSymbol {
            return when (symbol.symbol) {
                is Either.Left -> {
                    QuadraticMonomialSymbol(symbol.symbol.value)
                }

                is Either.Right -> {
                    QuadraticMonomialSymbol(symbol.symbol.value)
                }
            }
        }

        operator fun invoke(symbol1: LinearMonomialSymbol, symbol2: LinearMonomialSymbol): QuadraticMonomialSymbol {
            return when (symbol1.symbol) {
                is Either.Left -> {
                    when (symbol2.symbol) {
                        is Either.Left -> {
                            QuadraticMonomialSymbol(symbol1.symbol.value, symbol2.symbol.value)
                        }

                        is Either.Right -> {
                            QuadraticMonomialSymbol(symbol1.symbol.value, symbol2.symbol.value)
                        }
                    }
                }

                is Either.Right -> {
                    QuadraticMonomialSymbol(symbol1.symbol.value)
                }
            }
        }

        val QuadraticMonomialSymbolUnit.category
            get() = when (this) {
                is Variant3.V1 -> {
                    Linear
                }

                is Variant3.V2 -> {
                    this.value.category
                }

                is Variant3.V3 -> {
                    this.value.category
                }
            }

        val QuadraticMonomialSymbolUnit.name
            get() = when (this) {
                is Variant3.V1 -> {
                    this.value.name
                }

                is Variant3.V2 -> {
                    this.value.name
                }

                is Variant3.V3 -> {
                    this.value.name
                }
            }

        val QuadraticMonomialSymbolUnit.displayName
            get() = when (this) {
                is Variant3.V1 -> {
                    this.value.name
                }

                is Variant3.V2 -> {
                    this.value.displayName
                }

                is Variant3.V3 -> {
                    this.value.displayName
                }
            }

        val QuadraticMonomialSymbolUnit.discrete
            get() = when (this) {
                is Variant3.V1 -> {
                    this.value.type.isIntegerType
                }

                is Variant3.V2 -> {
                    this.value.discrete
                }

                is Variant3.V3 -> {
                    this.value.discrete
                }
            }

        val QuadraticMonomialSymbolUnit.range
            get() = when (this) {
                is Variant3.V1 -> {
                    this.value.range
                }

                is Variant3.V2 -> {
                    this.value.range
                }

                is Variant3.V3 -> {
                    this.value.range
                }
            }

        val QuadraticMonomialSymbolUnit.cells
            get() = when (this) {
                is Variant3.V1 -> {
                    listOf(QuadraticMonomialCell(Flt64.one, this.value, null))
                }

                is Variant3.V2 -> {
                    this.value.cells.map { QuadraticMonomialCell(it) }
                }

                is Variant3.V3 -> {
                    this.value.cells.map { it.copy() }
                }
            }

        val QuadraticMonomialSymbolUnit.cached
            get() = when (this) {
                is Variant3.V1 -> {
                    false
                }

                is Variant3.V2 -> {
                    this.value.cached
                }

                is Variant3.V3 -> {
                    this.value.cached
                }
            }

        val QuadraticMonomialSymbolUnit.hash
            get() = when (this) {
                is Variant3.V1 -> {
                    this.value.hashCode()
                }

                is Variant3.V2 -> {
                    this.value.name.hashCode()
                }

                is Variant3.V3 -> {
                    this.value.name.hashCode()
                }
            }

        infix fun QuadraticMonomialSymbolUnit?.eq(rhs: QuadraticMonomialSymbolUnit?): Boolean {
            return if (this != null && rhs != null) {
                this.v1 == rhs.v1
                        && this.v2 == rhs.v2
                        && this.v3 == rhs.v3
            } else if (this == null && rhs == null) {
                true
            } else {
                false
            }
        }

        fun QuadraticMonomialSymbolUnit.toRawString(unfold: UInt64): String {
            return when (this) {
                is Variant3.V1 -> {
                    this.value.name
                }

                is Variant3.V2 -> {
                    when (val exprSymbol = this.value) {
                        is ExpressionSymbol -> {
                            exprSymbol.toRawString(unfold)
                        }

                        else -> {
                            "$exprSymbol"
                        }
                    }
                }

                is Variant3.V3 -> {
                    when (val exprSymbol = this.value) {
                        is ExpressionSymbol -> {
                            exprSymbol.toRawString(unfold)
                        }

                        else -> {
                            "$exprSymbol"
                        }
                    }
                }
            }
        }

        fun QuadraticMonomialSymbolUnit.value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            return when (this) {
                is Variant3.V1 -> {
                    val token = tokenList.find(this.value)
                    if (token != null) {
                        val result = token.result
                        if (result != null) {
                            result
                        } else {
                            logger.trace { "Unknown result for ${this.value}." }
                            if (zeroIfNone) {
                                Flt64.zero
                            } else {
                                null
                            }
                        }
                    } else {
                        logger.trace { "Unknown token for ${this.value}." }
                        if (zeroIfNone) {
                            Flt64.zero
                        } else {
                            null
                        }
                    }
                }

                is Variant3.V2 -> {
                    this.value.evaluate(tokenList, zeroIfNone)
                }

                is Variant3.V3 -> {
                    this.value.evaluate(tokenList, zeroIfNone)
                }
            }
        }

        fun QuadraticMonomialSymbolUnit.value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
            return when (this) {
                is Variant3.V1 -> {
                    val index = tokenList.indexOf(this.value)
                    if (index != null) {
                        results[index]
                    } else {
                        logger.trace { "Unknown token for ${this.value}." }
                        if (zeroIfNone) {
                            Flt64.zero
                        } else {
                            null
                        }
                    }
                }

                is Variant3.V2 -> {
                    this.value.evaluate(results, tokenList, zeroIfNone)
                }

                is Variant3.V3 -> {
                    this.value.evaluate(results, tokenList, zeroIfNone)
                }
            }
        }

        fun QuadraticMonomialSymbolUnit.value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            return when (this) {
                is Variant3.V1 -> {
                    value(tokenTable.tokenList, zeroIfNone)
                }

                is Variant3.V2 -> {
                    this.value.evaluate(tokenTable, zeroIfNone)
                }

                is Variant3.V3 -> {
                    this.value.evaluate(tokenTable, zeroIfNone)
                }
            }
        }

        fun QuadraticMonomialSymbolUnit.value(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            return when (this) {
                is Variant3.V1 -> {
                    value(results, tokenTable.tokenList, zeroIfNone)
                }

                is Variant3.V2 -> {
                    this.value.evaluate(results, tokenTable, zeroIfNone)
                }

                is Variant3.V3 -> {
                    this.value.evaluate(results, tokenTable, zeroIfNone)
                }
            }
        }
    }

    override val name by lazy {
        if (symbol2 == null) {
            symbol1.name
        } else {
            "${symbol1.name} * ${symbol2.name}"
        }
    }

    override val displayName by lazy {
        if (symbol2 == null) {
            symbol1.displayName
        } else if (symbol1.displayName != null && symbol2.displayName != null) {
            "${symbol1.displayName} * ${symbol2.displayName}"
        } else {
            null
        }
    }

    override val category: Category
        get() = if (symbol2 != null) {
            Quadratic
        } else {
            symbol1.category
        }

    override val discrete by lazy {
        symbol1.discrete && (symbol2?.discrete != false)
    }

    override val range: ExpressionRange<*>
        get() {
            return if (symbol2 == null) {
                symbol1.range
            } else {
                ExpressionRange((symbol1.range.valueRange!! * symbol2.range.valueRange!!)!!)
            }
        }

    override val lowerBound get() = range.lowerBound!!.toFlt64()
    override val upperBound get() = range.upperBound!!.toFlt64()

    val pure by lazy {
        symbol1 is Variant3.V1 && symbol2 is Variant3.V1
    }

    val cells: List<QuadraticMonomialCell>
        get() {
            return if (symbol2 == null) {
                symbol1.cells
            } else {
                val cells1 = symbol1.cells
                val cells2 = symbol2.cells
                val cells = ArrayList<QuadraticMonomialCell>()
                for (cell1 in cells1) {
                    for (cell2 in cells2) {
                        cells.add(cell1 * cell2)
                    }
                }
                cells
            }
        }

    val cached get() = symbol1.cached || (symbol2?.cached == true)

    override fun partialEq(rhs: QuadraticMonomialSymbol): Boolean {
        return symbol1 eq rhs.symbol1
                && symbol2 eq rhs.symbol2
    }

    override fun hashCode(): Int {
        var result = symbol1.hash
        result = 31 * result + (symbol2?.hash ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuadraticMonomialSymbol

        if (!(symbol1 eq other.symbol1)) return false
        if (!(symbol2 eq other.symbol2)) return false

        return true
    }

    override fun toString(): String {
        return if (symbol2 == null) {
            symbol1.name
        } else {
            "${symbol1.name} * ${symbol2.name}"
        }
    }

    override fun toRawString(unfold: UInt64): String {
        return if (symbol2 == null) {
            symbol1.toRawString(unfold)
        } else {
            "${symbol1.toRawString(unfold)} * ${symbol2.toRawString(unfold)}"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (symbol2 == null) {
            symbol1.value(tokenList, zeroIfNone)
        } else {
            symbol1.value(tokenList, zeroIfNone)?.let { value1 ->
                symbol2.value(tokenList, zeroIfNone)?.let { value2 ->
                    value1 * value2
                }
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (symbol2 == null) {
            symbol1.value(results, tokenList, zeroIfNone)
        } else {
            symbol1.value(results, tokenList, zeroIfNone)?.let { value1 ->
                symbol2.value(results, tokenList, zeroIfNone)?.let { value2 ->
                    value1 * value2
                }
            }
        }
    }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (symbol2 == null) {
            symbol1.value(tokenTable, zeroIfNone)
        } else {
            symbol1.value(tokenTable, zeroIfNone)?.let { value1 ->
                symbol2.value(tokenTable, zeroIfNone)?.let { value2 ->
                    value1 * value2
                }
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (symbol2 == null) {
            symbol1.value(results, tokenTable, zeroIfNone)
        } else {
            symbol1.value(results, tokenTable, zeroIfNone)?.let { value1 ->
                symbol2.value(results, tokenTable, zeroIfNone)?.let { value2 ->
                    value1 * value2
                }
            }
        }
    }
}

class QuadraticMonomial(
    override val coefficient: Flt64,
    override val symbol: QuadraticMonomialSymbol,
    override var name: String = "",
    override var displayName: String? = null
) : Monomial<QuadraticMonomial, QuadraticMonomialCell> {
    companion object {
        operator fun invoke(item: AbstractVariableItem<*, *>): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item))
        }

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item))
        }

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item))
        }

        operator fun invoke(coefficient: Flt64, item: AbstractVariableItem<*, *>): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item))
        }

        operator fun invoke(item1: AbstractVariableItem<*, *>, item2: AbstractVariableItem<*, *>): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item1, item2))
        }

        operator fun invoke(coefficient: Int, item1: AbstractVariableItem<*, *>, item2: AbstractVariableItem<*, *>): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item1, item2))
        }

        operator fun invoke(coefficient: Double, item1: AbstractVariableItem<*, *>, item2: AbstractVariableItem<*, *>): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item1, item2))
        }

        operator fun invoke(coefficient: Flt64, item1: AbstractVariableItem<*, *>, item2: AbstractVariableItem<*, *>): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item1, item2))
        }

        operator fun invoke(item: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Flt64, item: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(item: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Flt64, item: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(symbol: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Int, symbol: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Double, symbol: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Flt64, symbol: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Int, symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Double, symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Flt64, symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Int, symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Double, symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Flt64, symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(symbol: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Int, symbol: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Double, symbol: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Flt64, symbol: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Int, symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Double, symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Flt64, symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(monomial: LinearMonomial): QuadraticMonomial {
            return QuadraticMonomial(monomial.coefficient, QuadraticMonomialSymbol(monomial.symbol))
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

    private var _cells: List<QuadraticMonomialCell> = emptyList()
    override val cells: List<QuadraticMonomialCell>
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

    override fun copy(): QuadraticMonomial {
        return QuadraticMonomial(coefficient, symbol.copy())
    }

    override fun unaryMinus() = QuadraticMonomial(-coefficient, symbol.copy())

    override fun times(rhs: Flt64): QuadraticMonomial {
        return QuadraticMonomial(coefficient * rhs, symbol.copy())
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
        }

        assert(this.symbol.symbol2 == null)
        return when (val symbol = this.symbol.symbol1) {
            is Variant3.V1 -> {
                QuadraticMonomial(this.coefficient, symbol.value, rhs)
            }

            is Variant3.V2 -> {
                QuadraticMonomial(this.coefficient, rhs, symbol.value)
            }

            is Variant3.V3 -> {
                QuadraticMonomial(this.coefficient, rhs, symbol.value)
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: LinearIntermediateSymbol): QuadraticMonomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
        }

        assert(this.symbol.symbol2 == null)
        return when (val symbol = this.symbol.symbol1) {
            is Variant3.V1 -> {
                QuadraticMonomial(this.coefficient, symbol.value, rhs)
            }

            is Variant3.V2 -> {
                QuadraticMonomial(this.coefficient, symbol.value, rhs)
            }

            is Variant3.V3 -> {
                QuadraticMonomial(this.coefficient, rhs, symbol.value)
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
        }

        assert(this.symbol.symbol2 == null)
        return when (val symbol1 = this.symbol.symbol1) {
            is Variant3.V1 -> {
                QuadraticMonomial(this.coefficient, symbol1.value, rhs)
            }

            is Variant3.V2 -> {
                QuadraticMonomial(this.coefficient, symbol1.value, rhs)
            }

            is Variant3.V3 -> {
                QuadraticMonomial(this.coefficient, symbol1.value, rhs)
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: LinearMonomial): QuadraticMonomial {
        if (this.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
        }

        assert(this.symbol.symbol2 == null)
        return when (val symbol1 = this.symbol.symbol1) {
            is Variant3.V1 -> {
                when (val symbol2 = rhs.symbol.symbol) {
                    is Either.Left -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }

                    is Either.Right -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }
                }
            }

            is Variant3.V2 -> {
                when (val symbol2 = rhs.symbol.symbol) {
                    is Either.Left -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol2.value, symbol1.value)
                    }

                    is Either.Right -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }
                }
            }

            is Variant3.V3 -> {
                when (val symbol2 = rhs.symbol.symbol) {
                    is Either.Left -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol2.value, symbol1.value)
                    }

                    is Either.Right -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol2.value, symbol1.value)
                    }
                }
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: QuadraticMonomial): QuadraticMonomial {
        if (this.category == Quadratic || rhs.category == Quadratic) {
            throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
        }

        assert(this.symbol.symbol2 == null)
        assert(rhs.symbol.symbol2 == null)
        return when (val symbol1 = this.symbol.symbol1) {
            is Variant3.V1 -> {
                when (val symbol2 = rhs.symbol.symbol1) {
                    is Variant3.V1 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }

                    is Variant3.V2 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }

                    is Variant3.V3 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }
                }
            }

            is Variant3.V2 -> {
                when (val symbol2 = rhs.symbol.symbol1) {
                    is Variant3.V1 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol2.value, symbol1.value)
                    }

                    is Variant3.V2 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }

                    is Variant3.V3 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }
                }
            }

            is Variant3.V3 -> {
                when (val symbol2 = rhs.symbol.symbol1) {
                    is Variant3.V1 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol2.value, symbol1.value)
                    }

                    is Variant3.V2 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol2.value, symbol1.value)
                    }

                    is Variant3.V3 -> {
                        QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                    }
                }
            }
        }
    }

    override fun div(rhs: Flt64): QuadraticMonomial {
        return QuadraticMonomial(coefficient / rhs, symbol.copy())
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

// quantity symbol conversion

@JvmName("quantitySymbolConversion")
fun Quantity<QuadraticIntermediateSymbol>.to(targetUnit: PhysicalUnit): Quantity<QuadraticMonomial>? {
    return this.unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, targetUnit)
    }
}

// quantity monomial conversion

@JvmName("quantityMonomialConversion")
fun Quantity<QuadraticMonomial>.to(targetUnit: PhysicalUnit): Quantity<QuadraticMonomial>? {
    return this.unit.to(targetUnit)?.let {
        Quantity(it.value * this.value, targetUnit)
    }
}

// unary minus symbol

operator fun QuadraticIntermediateSymbol.unaryMinus(): QuadraticMonomial {
    return -Flt64.one * this
}

@JvmName("unaryMinusQuantitySymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.unaryMinus(): Quantity<QuadraticMonomial> {
    return Quantity(-this.value, this.unit)
}

// unary minus monomial

@JvmName("unaryMinusMonomial")
operator fun Quantity<QuadraticMonomial>.unaryMinus(): Quantity<QuadraticMonomial> {
    return Quantity(-this.value, this.unit)
}

// symbol and constant

operator fun QuadraticIntermediateSymbol.times(rhs: Int): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

operator fun QuadraticIntermediateSymbol.times(rhs: Double): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

operator fun QuadraticIntermediateSymbol.times(rhs: Flt64): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

operator fun Int.times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

operator fun Double.times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

operator fun <T : RealNumber<T>> T.times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
    return QuadraticMonomial(this.toFlt64(), rhs)
}

operator fun QuadraticIntermediateSymbol.div(rhs: Int): QuadraticMonomial {
    return this.div(Flt64(rhs))
}

operator fun QuadraticIntermediateSymbol.div(rhs: Double): QuadraticMonomial {
    return this.div(Flt64(rhs))
}

operator fun <T : RealNumber<T>> QuadraticIntermediateSymbol.div(rhs: T): QuadraticMonomial {
    return QuadraticMonomial(rhs.toFlt64().reciprocal(), this)
}

// symbol and quantity

@JvmName("symbolTimesQuantity")
operator fun <T : RealNumber<T>> QuadraticIntermediateSymbol.times(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.value.toFlt64(), this), rhs.unit)
}

@JvmName("quantityTimesSymbol")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: QuadraticIntermediateSymbol): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value.toFlt64(), rhs), this.unit)
}

@JvmName("symbolDivQuantity")
operator fun <T : RealNumber<T>> QuadraticIntermediateSymbol.div(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.value.toFlt64().reciprocal(), this), rhs.unit.reciprocal())
}

// quantity symbol and constant

@JvmName("quantitySymbolTimesInt")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Int): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs, this.value), this.unit)
}

@JvmName("quantitySymbolTimesDouble")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Double): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs, this.value), this.unit)
}

@JvmName("quantitySymbolTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.times(rhs: T): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.toFlt64(), this.value), this.unit)
}

@JvmName("intTimesQuantitySymbol")
operator fun Int.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this, rhs.value), rhs.unit)
}

@JvmName("doubleTimesQuantitySymbol")
operator fun Double.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this, rhs.value), rhs.unit)
}

@JvmName("realNumberTimesQuantitySymbol")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.toFlt64(), rhs.value), rhs.unit)
}

@JvmName("quantitySymbolDivInt")
operator fun Quantity<QuadraticIntermediateSymbol>.div(rhs: Int): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(Flt64(rhs).reciprocal(), this.value), this.unit)
}

@JvmName("quantitySymbolDivDouble")
operator fun Quantity<QuadraticIntermediateSymbol>.div(rhs: Double): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(Flt64(rhs).reciprocal(), this.value), this.unit)
}

@JvmName("quantitySymbolDivRealNumber")
operator fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.div(rhs: T): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.toFlt64().reciprocal(), this.value), this.unit.reciprocal())
}

// quantity symbol and quantity

@JvmName("quantitySymbolTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.times(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.value.toFlt64(), this.value), this.unit * rhs.unit)
}

@JvmName("quantityTimesQuantitySymbol")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value.toFlt64(), rhs.value), this.unit * rhs.unit)
}

@JvmName("quantitySymbolDivQuantity")
operator fun <T : RealNumber<T>> Quantity<QuadraticIntermediateSymbol>.div(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.value.toFlt64().reciprocal(), this.value), this.unit / rhs.unit)
}

// variable and variable

operator fun AbstractVariableItem<*, *>.times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

// quantity variable and variable

@JvmName("quantityVariableTimesVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: AbstractVariableItem<*, *>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs), this.unit)
}

@JvmName("variableTimesQuantityVariable")
operator fun AbstractVariableItem<*, *>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this, rhs.value), rhs.unit)
}

// quantity variable and quantity variable

@JvmName("quantityVariableTimesQuantityVariable")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs.value), this.unit * rhs.unit)
}

// symbol and variable

operator fun AbstractVariableItem<*, *>.times(rhs: LinearIntermediateSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

operator fun LinearIntermediateSymbol.times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

operator fun AbstractVariableItem<*, *>.times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

operator fun QuadraticIntermediateSymbol.times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

// quantity symbol and variable

@JvmName("variableTimesQuantityLinearSymbol")
operator fun AbstractVariableItem<*, *>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this, rhs.value), rhs.unit)
}

@JvmName("quantityLinearSymbolTimesVariable")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: AbstractVariableItem<*, *>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs, this.value), this.unit)
}

@JvmName("variableTimesQuantityQuadraticSymbol")
operator fun AbstractVariableItem<*, *>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this, rhs.value), rhs.unit)
}

@JvmName("quantityQuadraticSymbolTimesVariable")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: AbstractVariableItem<*, *>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs, this.value), this.unit)
}

// symbol and quantity variable

@JvmName("quantityVariableTimesLinearSymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: LinearIntermediateSymbol): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs), this.unit)
}

@JvmName("linearSymbolTimesQuantityVariable")
operator fun LinearIntermediateSymbol.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.value, this), rhs.unit)
}

@JvmName("quantityVariableTimesQuadraticSymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: QuadraticIntermediateSymbol): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs), this.unit)
}

@JvmName("quadraticSymbolTimesQuantityVariable")
operator fun QuadraticIntermediateSymbol.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.value, this), rhs.unit)
}

// quantity symbol and quantity variable

@JvmName("quantityVariableTimesQuantityLinearSymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs.value), this.unit * rhs.unit)
}

@JvmName("quantityLinearSymbolTimesQuantityVariable")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.value, this.value), this.unit * rhs.unit)
}

@JvmName("quantityVariableTimesQuantityQuadraticSymbol")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs.value), this.unit * rhs.unit)
}

@JvmName("quantityQuadraticSymbolTimesQuantityVariable")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(rhs.value, this.value), this.unit * rhs.unit)
}

// symbol and symbol

operator fun LinearIntermediateSymbol.times(rhs: LinearIntermediateSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

@Throws(IllegalArgumentException::class)
operator fun LinearIntermediateSymbol.times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return QuadraticMonomial(this, rhs)
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticIntermediateSymbol.times(rhs: LinearIntermediateSymbol): QuadraticMonomial {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return QuadraticMonomial(rhs, this)
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticIntermediateSymbol.times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
    if (this.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return QuadraticMonomial(this, rhs)
}

// quantity symbol and symbol

@JvmName("quantityLinearSymbolTimesLinearSymbol")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: LinearIntermediateSymbol): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs), this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityLinearSymbolTimesQuadraticSymbol")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: QuadraticIntermediateSymbol): Quantity<QuadraticMonomial> {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(QuadraticMonomial(this.value, rhs), this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticSymbolTimesLinearSymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: LinearIntermediateSymbol): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(QuadraticMonomial(rhs, this.value), this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticSymbolTimesQuadraticSymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: QuadraticIntermediateSymbol): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(QuadraticMonomial(this.value, rhs), this.unit)
}

// quantity symbol and quantity symbol

@JvmName("quantityLinearSymbolTimesQuantityLinearSymbol")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(QuadraticMonomial(this.value, rhs.value), this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityLinearSymbolTimesQuantityQuadraticSymbol")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(QuadraticMonomial(this.value, rhs.value), this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticSymbolTimesQuantityLinearSymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(QuadraticMonomial(rhs.value, this.value), this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticSymbolTimesQuantityQuadraticSymbol")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic || rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(QuadraticMonomial(this.value, rhs.value), this.unit * rhs.unit)
}

// monomial and constant

operator fun Int.times(rhs: QuadraticMonomial): QuadraticMonomial {
    return QuadraticMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

operator fun Double.times(rhs: QuadraticMonomial): QuadraticMonomial {
    return QuadraticMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
}

operator fun <T : RealNumber<T>> T.times(rhs: QuadraticMonomial): QuadraticMonomial {
    return QuadraticMonomial(this.toFlt64() * rhs.coefficient, rhs.symbol)
}

// monomial and unit

operator fun QuadraticMonomial.times(rhs: PhysicalUnit): Quantity<QuadraticMonomial> {
    return Quantity(this, rhs)
}

// monomial and quantity

@JvmName("quantityTimesMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: QuadraticMonomial): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("monomialTimesQuantity")
operator fun <T : RealNumber<T>> QuadraticMonomial.times(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("monomialDivQuantity")
operator fun <T : RealNumber<T>> QuadraticMonomial.div(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(this / rhs.value, rhs.unit.reciprocal())
}

// quantity monomial and constant

@JvmName("intTimesQuantityMonomial")
operator fun Int.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(Flt64(this) * rhs.value, rhs.unit)
}

@JvmName("doubleTimesQuantityMonomial")
operator fun Double.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(Flt64(this) * rhs.value, rhs.unit)
}

@JvmName("realNumberTimesQuantityMonomial")
operator fun <T : RealNumber<T>> T.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(this.toFlt64() * rhs.value, rhs.unit)
}

@JvmName("quantityMonomialTimesInt")
operator fun Quantity<QuadraticMonomial>.times(rhs: Int): Quantity<QuadraticMonomial> {
    return Quantity(this.value * Flt64(rhs), this.unit)
}

@JvmName("quantityMonomialTimesDouble")
operator fun Quantity<QuadraticMonomial>.times(rhs: Double): Quantity<QuadraticMonomial> {
    return Quantity(this.value * Flt64(rhs), this.unit)
}

@JvmName("quantityMonomialTimesRealNumber")
operator fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.times(rhs: T): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs.toFlt64(), this.unit)
}

@JvmName("intTimesQuantityMonomial")
operator fun Quantity<QuadraticMonomial>.div(rhs: Int): Quantity<QuadraticMonomial> {
    return Quantity(this.value / Flt64(rhs), this.unit)
}

@JvmName("doubleTimesQuantityMonomial")
operator fun Quantity<QuadraticMonomial>.div(rhs: Double): Quantity<QuadraticMonomial> {
    return Quantity(this.value / Flt64(rhs), this.unit)
}

@JvmName("realNumberTimesQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.div(rhs: T): Quantity<QuadraticMonomial> {
    return Quantity(this.value / rhs.toFlt64(), this.unit)
}

// quantity monomial and quantity

@JvmName("quantityTimesQuantityMonomial")
operator fun <T : RealNumber<T>> Quantity<T>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityMonomialTimesQuantity")
operator fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.times(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityMonomialDivQuantity")
operator fun <T : RealNumber<T>> Quantity<QuadraticMonomial>.div(rhs: Quantity<T>): Quantity<QuadraticMonomial> {
    return Quantity(this.value / rhs.value, this.unit / rhs.unit)
}

// monomial and variable

operator fun LinearMonomial.times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
    return when (val symbol = this.symbol.symbol) {
        is Either.Left -> {
            QuadraticMonomial(this.coefficient, symbol.value, rhs)
        }

        is Either.Right -> {
            QuadraticMonomial(this.coefficient, rhs, symbol.value)
        }
    }
}

operator fun AbstractVariableItem<*, *>.times(rhs: LinearMonomial): QuadraticMonomial {
    return when (val symbol = rhs.symbol.symbol) {
        is Either.Left -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }

        is Either.Right -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }
    }
}

@Throws(IllegalArgumentException::class)
operator fun AbstractVariableItem<*, *>.times(rhs: QuadraticMonomial): QuadraticMonomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    assert(rhs.symbol.symbol2 == null)
    return when (val symbol = rhs.symbol.symbol1) {
        is Variant3.V1 -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }

        is Variant3.V2 -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }

        is Variant3.V3 -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }
    }
}

// quantity monomial and variable

@JvmName("quantityLinearMonomialTimesVariable")
operator fun Quantity<LinearMonomial>.times(rhs: AbstractVariableItem<*, *>): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("variableTimesQuantityLinearMonomial")
operator fun AbstractVariableItem<*, *>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(rhs.value * this, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("variableTimesQuantityQuadraticMonomial")
operator fun AbstractVariableItem<*, *>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesVariable")
operator fun Quantity<QuadraticMonomial>.times(rhs: AbstractVariableItem<*, *>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

// monomial and quantity variable

@JvmName("linearMonomialTimesQuantityVariable")
operator fun LinearMonomial.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityVariableTimesLinearMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: LinearMonomial): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quadraticMonomialTimesQuantityVariable")
operator fun QuadraticMonomial.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityVariableTimesQuadraticMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: QuadraticMonomial): Quantity<QuadraticMonomial> {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

// quantity monomial and quantity variable

@JvmName("quantityLinearMonomialTimesQuantityVariable")
operator fun Quantity<LinearMonomial>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityVariableTimesQuantityLinearMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesQuantityVariable")
operator fun Quantity<QuadraticMonomial>.times(rhs: Quantity<AbstractVariableItem<*, *>>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityVariableTimesQuantityQuadraticMonomial")
operator fun Quantity<AbstractVariableItem<*, *>>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

// monomial and symbol

operator fun LinearIntermediateSymbol.times(rhs: LinearMonomial): QuadraticMonomial {
    return when (val symbol = rhs.symbol.symbol) {
        is Either.Left -> {
            QuadraticMonomial(rhs.coefficient, symbol.value, this)
        }

        is Either.Right -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }
    }
}

operator fun LinearMonomial.times(rhs: LinearIntermediateSymbol): QuadraticMonomial {
    return when (val symbol = this.symbol.symbol) {
        is Either.Left -> {
            QuadraticMonomial(this.coefficient, symbol.value, rhs)
        }

        is Either.Right -> {
            QuadraticMonomial(this.coefficient, symbol.value, rhs)
        }
    }
}

@Throws(IllegalArgumentException::class)
operator fun LinearMonomial.times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of LinearMonomial.times: over quadratic.")
    }

    return when (val symbol = this.symbol.symbol) {
        is Either.Left -> {
            QuadraticMonomial(this.coefficient, symbol.value, rhs)
        }

        is Either.Right -> {
            QuadraticMonomial(this.coefficient, symbol.value, rhs)
        }
    }
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticIntermediateSymbol.times(rhs: LinearMonomial): QuadraticMonomial {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return when (val symbol = rhs.symbol.symbol) {
        is Either.Left -> {
            QuadraticMonomial(rhs.coefficient, symbol.value, this)
        }

        is Either.Right -> {
            QuadraticMonomial(rhs.coefficient, symbol.value, this)
        }
    }
}

@Throws(IllegalArgumentException::class)
operator fun LinearIntermediateSymbol.times(rhs: QuadraticMonomial): QuadraticMonomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    assert(rhs.symbol.symbol2 == null)
    return when (val symbol = rhs.symbol.symbol1) {
        is Variant3.V1 -> {
            QuadraticMonomial(rhs.coefficient, symbol.value, this)
        }

        is Variant3.V2 -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }

        is Variant3.V3 -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }
    }
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticIntermediateSymbol.times(rhs: QuadraticMonomial): QuadraticMonomial {
    if (this.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    assert(rhs.symbol.symbol2 == null)
    return when (val symbol = rhs.symbol.symbol1) {
        is Variant3.V1 -> {
            QuadraticMonomial(rhs.coefficient, symbol.value, this)
        }

        is Variant3.V2 -> {
            QuadraticMonomial(rhs.coefficient, symbol.value, this)
        }

        is Variant3.V3 -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }
    }
}

// quantity monomial and symbol

@JvmName("quantityLinearMonomialTimesLinearSymbol")
operator fun Quantity<LinearMonomial>.times(rhs: LinearIntermediateSymbol): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearSymbolTimesQuantityLinearMonomial")
operator fun LinearIntermediateSymbol.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(rhs.value * this, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityLinearMonomialTimesQuadraticSymbol")
operator fun Quantity<LinearMonomial>.times(rhs: QuadraticIntermediateSymbol): Quantity<QuadraticMonomial> {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quadraticSymbolTimesQuantityLinearMonomial")
operator fun QuadraticIntermediateSymbol.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesLinearSymbol")
operator fun Quantity<QuadraticMonomial>.times(rhs: LinearIntermediateSymbol): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("linearSymbolTimesQuantityQuadraticMonomial")
operator fun LinearIntermediateSymbol.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesQuadraticSymbol")
operator fun Quantity<QuadraticMonomial>.times(rhs: QuadraticIntermediateSymbol): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quadraticSymbolTimesQuantityQuadraticMonomial")
operator fun QuadraticIntermediateSymbol.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (this.category == Quadratic || rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this, rhs.unit)
}

// monomial and quantity symbol

@JvmName("linearMonomialTimesQuantityLinearSymbol")
operator fun LinearMonomial.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(this * rhs.value, rhs.unit)
}

@JvmName("quantityLinearSymbolTimesLinearMonomial")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: LinearMonomial): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quadraticMonomialTimesQuantityLinearSymbol")
operator fun QuadraticMonomial.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this * rhs.value, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityLinearSymbolTimesQuadraticMonomial")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: QuadraticMonomial): Quantity<QuadraticMonomial> {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("linearMonomialTimesQuantityQuadraticSymbol")
operator fun LinearMonomial.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this * rhs.value, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticSymbolTimesLinearMonomial")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: LinearMonomial): Quantity<QuadraticMonomial> {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quadraticMonomialTimesQuantityQuadraticSymbol")
operator fun QuadraticMonomial.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (this.category == Quadratic || rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this * rhs.value, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticSymbolTimesQuadraticMonomial")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: QuadraticMonomial): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

// quantity monomial and quantity symbol

@JvmName("quantityLinearMonomialTimesQuantityLinearSymbol")
operator fun Quantity<LinearMonomial>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@JvmName("quantityLinearSymbolTimesQuantityLinearMonomial")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityLinearMonomialTimesQuantityQuadraticSymbol")
operator fun Quantity<LinearMonomial>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticSymbolTimesQuantityLinearMonomial")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    if (rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this.value, rhs.unit * this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesQuantityLinearSymbol")
operator fun Quantity<QuadraticMonomial>.times(rhs: Quantity<LinearIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("QuantityLinearSymbolTimesQuantityQuadraticMonomial")
operator fun Quantity<LinearIntermediateSymbol>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this.value, rhs.unit * this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesQuantityQuadraticSymbol")
operator fun Quantity<QuadraticMonomial>.times(rhs: Quantity<QuadraticIntermediateSymbol>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic || rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticSymbolTimesQuantityQuadraticMonomial")
operator fun Quantity<QuadraticIntermediateSymbol>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic || rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this.value, rhs.unit * this.unit)
}

// monomial and monomial

operator fun LinearMonomial.times(rhs: LinearMonomial): QuadraticMonomial {
    return when (val symbol1 = this.symbol.symbol) {
        is Either.Left -> {
            when (val symbol2 = rhs.symbol.symbol) {
                is Either.Left -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                }

                is Either.Right -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                }
            }
        }

        is Either.Right -> {
            when (val symbol2 = rhs.symbol.symbol) {
                is Either.Left -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol2.value, symbol1.value)
                }

                is Either.Right -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                }
            }
        }
    }
}

@Throws(IllegalArgumentException::class)
operator fun LinearMonomial.times(rhs: QuadraticMonomial): QuadraticMonomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    assert(rhs.symbol.symbol2 == null)
    return when (val symbol1 = this.symbol.symbol) {
        is Either.Left -> {
            when (val symbol2 = rhs.symbol.symbol1) {
                is Variant3.V1 -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                }

                is Variant3.V2 -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                }

                is Variant3.V3 -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                }
            }
        }

        is Either.Right -> {
            when (val symbol2 = rhs.symbol.symbol1) {
                is Variant3.V1 -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol2.value, symbol1.value)
                }

                is Variant3.V2 -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                }

                is Variant3.V3 -> {
                    QuadraticMonomial(this.coefficient * rhs.coefficient, symbol1.value, symbol2.value)
                }
            }
        }
    }
}

// quantity monomial and monomial

@JvmName("quantityLinearMonomialTimesLinearMonomial")
operator fun Quantity<LinearMonomial>.times(rhs: LinearMonomial): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs, this.unit)
}

@JvmName("linearMonomialTimesQuantityLinearMonomial")
operator fun LinearMonomial.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(rhs.value * this, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityLinearMonomialTimesQuadraticMonomial")
operator fun Quantity<LinearMonomial>.times(rhs: QuadraticMonomial): Quantity<QuadraticMonomial> {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quadraticMonomialTimesQuantityLinearMonomial")
operator fun QuadraticMonomial.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    if (this.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this, rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesLinearMonomial")
operator fun Quantity<QuadraticMonomial>.times(rhs: LinearMonomial): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("linearMonomialTimesQuantityQuadraticMonomial")
operator fun LinearMonomial.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this, rhs.unit)
}


@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesQuadraticMonomial")
operator fun Quantity<QuadraticMonomial>.times(rhs: QuadraticMonomial): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs, this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quadraticMonomialTimesQuantityQuadraticMonomial")
operator fun QuadraticMonomial.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (this.category == Quadratic || rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this, rhs.unit)
}

// quantity monomial and quantity monomial

@JvmName("quantityLinearMonomialTimesQuantityLinearMonomial")
operator fun Quantity<LinearMonomial>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityLinearMonomialTimesQuantityQuadraticMonomial")
operator fun Quantity<LinearMonomial>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesQuantityLinearMonomial")
operator fun Quantity<QuadraticMonomial>.times(rhs: Quantity<LinearMonomial>): Quantity<QuadraticMonomial> {
    if (rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(rhs.value * this.value, rhs.unit * this.unit)
}

@Throws(IllegalArgumentException::class)
@JvmName("quantityQuadraticMonomialTimesQuantityQuadraticMonomial")
operator fun Quantity<QuadraticMonomial>.times(rhs: Quantity<QuadraticMonomial>): Quantity<QuadraticMonomial> {
    if (this.value.category == Quadratic || rhs.value.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return Quantity(this.value * rhs.value, this.unit * rhs.unit)
}
