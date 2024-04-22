package fuookami.ospf.kotlin.core.frontend.expression.monomial

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
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

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
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

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
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

typealias QuadraticMonomialSymbolUnit = Variant3<AbstractVariableItem<*, *>, LinearSymbol, QuadraticSymbol>

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

        operator fun invoke(symbol: LinearSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V2(symbol))
        }

        operator fun invoke(symbol: QuadraticSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V3(symbol))
        }

        operator fun invoke(variable1: AbstractVariableItem<*, *>, variable2: AbstractVariableItem<*, *>): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V1(variable1), Variant3.V1(variable2)
            )
        }

        operator fun invoke(variable: AbstractVariableItem<*, *>, symbol: LinearSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V1(variable), Variant3.V2(symbol)
            )
        }

        operator fun invoke(variable: AbstractVariableItem<*, *>, symbol: QuadraticSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V1(variable), Variant3.V3(symbol)
            )
        }

        operator fun invoke(symbol1: LinearSymbol, symbol2: LinearSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V2(symbol1), Variant3.V2(symbol2)
            )
        }

        operator fun invoke(symbol1: LinearSymbol, symbol2: QuadraticSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(
                Variant3.V2(symbol1), Variant3.V3(symbol2)
            )
        }

        operator fun invoke(symbol1: QuadraticSymbol, symbol2: QuadraticSymbol): QuadraticMonomialSymbol {
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

        fun QuadraticMonomialSymbolUnit.toRawString(unfold: Boolean): String {
            return when (this) {
                is Variant3.V1 -> {
                    this.value.name
                }

                is Variant3.V2 -> {
                    when (val exprSymbol = this.value) {
                        is ExpressionSymbol -> {
                            "(${exprSymbol.toRawString(unfold)})"
                        }

                        else -> {
                            "$exprSymbol"
                        }
                    }
                }

                is Variant3.V3 -> {
                    when (val exprSymbol = this.value) {
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
                    this.value.value(tokenList, zeroIfNone)
                }

                is Variant3.V3 -> {
                    this.value.value(tokenList, zeroIfNone)
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
                    this.value.value(results, tokenList, zeroIfNone)
                }

                is Variant3.V3 -> {
                    this.value.value(results, tokenList, zeroIfNone)
                }
            }
        }

        fun QuadraticMonomialSymbolUnit.value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            return when (this) {
                is Variant3.V1 -> {
                    value(tokenTable.tokenList, zeroIfNone)
                }

                is Variant3.V2 -> {
                    this.value.value(tokenTable, zeroIfNone)
                }

                is Variant3.V3 -> {
                    this.value.value(tokenTable, zeroIfNone)
                }
            }
        }

        fun QuadraticMonomialSymbolUnit.value(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
            return when (this) {
                is Variant3.V1 -> {
                    value(results, tokenTable.tokenList, zeroIfNone)
                }

                is Variant3.V2 -> {
                    this.value.value(results, tokenTable, zeroIfNone)
                }

                is Variant3.V3 -> {
                    this.value.value(results, tokenTable, zeroIfNone)
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
                ExpressionRange(symbol1.range.valueRange * symbol2.range.valueRange)
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

    override fun toRawString(unfold: Boolean): String {
        return if (symbol2 == null) {
            symbol1.toRawString(unfold)
        } else {
            "${symbol1.toRawString(unfold)} * ${symbol2.toRawString(unfold)}"
        }
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
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

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
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

    override fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
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

    override fun value(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
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

        operator fun invoke(item: AbstractVariableItem<*, *>, symbol: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>, symbol: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>, symbol: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Flt64, item: AbstractVariableItem<*, *>, symbol: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(item: AbstractVariableItem<*, *>, symbol: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>, symbol: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>, symbol: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(coefficient: Flt64, item: AbstractVariableItem<*, *>, symbol: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item, symbol))
        }

        operator fun invoke(symbol: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Int, symbol: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Double, symbol: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Flt64, symbol: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(symbol1: LinearSymbol, symbol2: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Int, symbol1: LinearSymbol, symbol2: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Double, symbol1: LinearSymbol, symbol2: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Flt64, symbol1: LinearSymbol, symbol2: LinearSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(symbol1: LinearSymbol, symbol2: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Int, symbol1: LinearSymbol, symbol2: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Double, symbol1: LinearSymbol, symbol2: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Flt64, symbol1: LinearSymbol, symbol2: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(symbol: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Int, symbol: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Double, symbol: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Flt64, symbol: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol))
        }

        operator fun invoke(symbol1: QuadraticSymbol, symbol2: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Int, symbol1: QuadraticSymbol, symbol2: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Double, symbol1: QuadraticSymbol, symbol2: QuadraticSymbol): QuadraticMonomial {
            return QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))
        }

        operator fun invoke(coefficient: Flt64, symbol1: QuadraticSymbol, symbol2: QuadraticSymbol): QuadraticMonomial {
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
    operator fun times(rhs: LinearSymbol): QuadraticMonomial {
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
    operator fun times(rhs: QuadraticSymbol): QuadraticMonomial {
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

// symbol and constant

operator fun QuadraticSymbol.times(rhs: Int): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

operator fun QuadraticSymbol.times(rhs: Double): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

operator fun QuadraticSymbol.times(rhs: Flt64): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

operator fun Int.times(rhs: QuadraticSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

operator fun Double.times(rhs: QuadraticSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

operator fun <T : RealNumber<T>> T.times(rhs: QuadraticSymbol): QuadraticMonomial {
    return QuadraticMonomial(this.toFlt64(), rhs)
}

operator fun QuadraticSymbol.div(rhs: Int): QuadraticMonomial {
    return this.div(Flt64(rhs))
}

operator fun QuadraticSymbol.div(rhs: Double): QuadraticMonomial {
    return this.div(Flt64(rhs))
}

operator fun <T : RealNumber<T>> QuadraticSymbol.div(rhs: T): QuadraticMonomial {
    return QuadraticMonomial(rhs.toFlt64().reciprocal(), this)
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

// variable and variable

operator fun AbstractVariableItem<*, *>.times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

// symbol and variable

operator fun AbstractVariableItem<*, *>.times(rhs: LinearSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

operator fun LinearSymbol.times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
}

operator fun AbstractVariableItem<*, *>.times(rhs: QuadraticSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

operator fun QuadraticSymbol.times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
    return QuadraticMonomial(rhs, this)
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

// symbol and symbol

operator fun LinearSymbol.times(rhs: LinearSymbol): QuadraticMonomial {
    return QuadraticMonomial(this, rhs)
}

@Throws(IllegalArgumentException::class)
operator fun LinearSymbol.times(rhs: QuadraticSymbol): QuadraticMonomial {
    if (rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return QuadraticMonomial(this, rhs)
}

@Throws(IllegalArgumentException::class)
operator fun QuadraticSymbol.times(rhs: QuadraticSymbol): QuadraticMonomial {
    if (this.category == Quadratic || rhs.category == Quadratic) {
        throw IllegalArgumentException("Invalid argument of QuadraticMonomial.times: over quadratic.")
    }

    return QuadraticMonomial(this, rhs)
}

// monomial and symbol

operator fun LinearSymbol.times(rhs: LinearMonomial): QuadraticMonomial {
    return when (val symbol = rhs.symbol.symbol) {
        is Either.Left -> {
            QuadraticMonomial(rhs.coefficient, symbol.value, this)
        }

        is Either.Right -> {
            QuadraticMonomial(rhs.coefficient, this, symbol.value)
        }
    }
}

operator fun LinearMonomial.times(rhs: LinearSymbol): QuadraticMonomial {
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
operator fun LinearMonomial.times(rhs: QuadraticSymbol): QuadraticMonomial {
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
operator fun QuadraticSymbol.times(rhs: LinearMonomial): QuadraticMonomial {
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
operator fun LinearSymbol.times(rhs: QuadraticMonomial): QuadraticMonomial {
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
operator fun QuadraticSymbol.times(rhs: QuadraticMonomial): QuadraticMonomial {
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
