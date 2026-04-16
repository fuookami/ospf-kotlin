@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model.monomial

import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_model.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenData
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenData
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.ToQuadraticPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.intermediate_model.toQuadraticMonomialCells
import fuookami.ospf.kotlin.core.intermediate_symbol.ExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.utils.functional.Variant3
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.Bound
import fuookami.ospf.kotlin.math.algebra.value_range.times
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.utils.functional.Order
import org.apache.logging.log4j.kotlin.logger

/**
 * Cell representation for quadratic monomials - phantom type parameter V.
 * Either a (coefficient, variable1, variable2?) triple or a constant value.
 * Internal implementation uses Flt64.
 */
data class QuadraticMonomialCellOf<V> internal constructor(
    val cell: Either<QuadraticCellTripleOf<V>, Flt64>
) : MonomialCell<QuadraticMonomialCellOf<V>> {
    private val logger = logger()

    data class QuadraticCellTripleOf<V>(
        val coefficient: Flt64,
        val variable1: AbstractVariableItem<*, *>,
        val variable2: AbstractVariableItem<*, *>?
    ) : Cloneable, Copyable<QuadraticCellTripleOf<V>> {
        init {
            if (variable2 != null) {
                assert(variable1.identifier <= variable2.identifier)
                if (variable1.identifier == variable2.identifier) {
                    assert(variable1.index <= variable2.index)
                }
            }
        }

        operator fun unaryMinus(): QuadraticCellTripleOf<V> {
            return QuadraticCellTripleOf<V>(-coefficient, variable1, variable2)
        }

        @Throws(IllegalArgumentException::class)
        operator fun plus(rhs: QuadraticCellTripleOf<V>): QuadraticCellTripleOf<V> {
            if (variable1 != rhs.variable1 || variable2 != rhs.variable2) {
                throw IllegalArgumentException("Invalid argument of QuadraticCellTripleOf.plus: not same variable.")
            }
            return QuadraticCellTripleOf<V>(coefficient + rhs.coefficient, variable1, variable2)
        }

        @Throws(IllegalArgumentException::class)
        operator fun minus(rhs: QuadraticCellTripleOf<V>): QuadraticCellTripleOf<V> {
            if (variable1 != rhs.variable1 || variable2 != rhs.variable2) {
                throw IllegalArgumentException("Invalid argument of QuadraticCellTripleOf.minus: not same variable.")
            }
            return QuadraticCellTripleOf<V>(coefficient - rhs.coefficient, variable1, variable2)
        }

        operator fun times(rhs: Flt64) = QuadraticCellTripleOf<V>(coefficient * rhs, variable1, variable2)

        @Throws(IllegalArgumentException::class)
        operator fun times(rhs: QuadraticCellTripleOf<V>): QuadraticCellTripleOf<V> {
            if (variable2 != null || rhs.variable2 != null) {
                throw IllegalArgumentException("Invalid argument of QuadraticCellTripleOf.times: over quadratic.")
            }
            return if ((variable1.key ord rhs.variable1.key) is Order.Greater) {
                QuadraticCellTripleOf<V>(coefficient * rhs.coefficient, rhs.variable1, variable1)
            } else {
                QuadraticCellTripleOf<V>(coefficient * rhs.coefficient, variable1, rhs.variable1)
            }
        }

        operator fun div(rhs: Flt64) = QuadraticCellTripleOf<V>(coefficient / rhs, variable1, variable2)

        override fun copy(): QuadraticCellTripleOf<V> = QuadraticCellTripleOf<V>(coefficient, variable1, variable2)
        public override fun clone() = copy()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as QuadraticCellTripleOf<*>
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
        operator fun <V> invoke(linearCell: LinearMonomialCellOf<V>): QuadraticMonomialCellOf<V> {
            return when (val cell = linearCell.cell) {
                is Either.Left -> QuadraticMonomialCellOf<V>(
                    Either.Left(QuadraticCellTripleOf<V>(cell.value.coefficient, cell.value.variable, null))
                )
                is Either.Right -> QuadraticMonomialCellOf<V>(Either.Right(cell.value))
            }
        }

        operator fun <V> invoke(
            variable1: AbstractVariableItem<*, *>,
            variable2: AbstractVariableItem<*, *>?
        ): QuadraticMonomialCellOf<V> {
            return if (variable2 == null) {
                QuadraticMonomialCellOf<V>(
                    Either.Left(QuadraticCellTripleOf<V>(Flt64.one, variable1, null))
                )
            } else {
                if ((variable1.key ord variable2.key) is Order.Greater) {
                    QuadraticMonomialCellOf<V>(
                        Either.Left(QuadraticCellTripleOf<V>(Flt64.one, variable2, variable1))
                    )
                } else {
                    QuadraticMonomialCellOf<V>(
                        Either.Left(QuadraticCellTripleOf<V>(Flt64.one, variable1, variable2))
                    )
                }
            }
        }

        operator fun <V> invoke(
            coefficient: Flt64,
            variable1: AbstractVariableItem<*, *>,
            variable2: AbstractVariableItem<*, *>?
        ): QuadraticMonomialCellOf<V> {
            return if (variable2 == null) {
                QuadraticMonomialCellOf<V>(
                    Either.Left(QuadraticCellTripleOf<V>(coefficient, variable1, null))
                )
            } else {
                if ((variable1.key ord variable2.key) is Order.Greater) {
                    QuadraticMonomialCellOf<V>(
                        Either.Left(QuadraticCellTripleOf<V>(coefficient, variable2, variable1))
                    )
                } else {
                    QuadraticMonomialCellOf<V>(
                        Either.Left(QuadraticCellTripleOf<V>(coefficient, variable1, variable2))
                    )
                }
            }
        }

        operator fun <V> invoke(constant: Flt64): QuadraticMonomialCellOf<V> = QuadraticMonomialCellOf<V>(Either.Right(constant))
    }

    val isTriple by cell::isLeft
    override val isConstant by cell::isRight
    val triple by cell::left
    override val constant by cell::right

    override operator fun unaryMinus(): QuadraticMonomialCellOf<V> {
        return when (cell) {
            is Either.Left -> QuadraticMonomialCellOf<V>(Either.Left(-cell.value))
            is Either.Right -> QuadraticMonomialCellOf<V>(Either.Right(-cell.value))
        }
    }

    @Throws(IllegalArgumentException::class)
    override operator fun plus(rhs: QuadraticMonomialCellOf<V>): QuadraticMonomialCellOf<V> {
        return when (cell) {
            is Either.Left -> when (rhs.cell) {
                is Either.Left -> QuadraticMonomialCellOf<V>(Either.Left(cell.value + rhs.cell.value))
                is Either.Right -> throw IllegalArgumentException("Cannot add monomial and constant")
            }
            is Either.Right -> when (rhs.cell) {
                is Either.Left -> throw IllegalArgumentException("Cannot add monomial and constant")
                is Either.Right -> QuadraticMonomialCellOf<V>(Either.Right(cell.value + rhs.cell.value))
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override operator fun minus(rhs: QuadraticMonomialCellOf<V>): QuadraticMonomialCellOf<V> {
        return when (cell) {
            is Either.Left -> when (rhs.cell) {
                is Either.Left -> QuadraticMonomialCellOf<V>(Either.Left(cell.value - rhs.cell.value))
                is Either.Right -> throw IllegalArgumentException("Cannot subtract monomial and constant")
            }
            is Either.Right -> when (rhs.cell) {
                is Either.Left -> throw IllegalArgumentException("Cannot subtract monomial and constant")
                is Either.Right -> QuadraticMonomialCellOf<V>(Either.Right(cell.value - rhs.cell.value))
            }
        }
    }

    override fun times(rhs: Flt64): QuadraticMonomialCellOf<V> {
        return when (cell) {
            is Either.Left -> QuadraticMonomialCellOf<V>(Either.Left(cell.value * rhs))
            is Either.Right -> QuadraticMonomialCellOf<V>(Either.Right(cell.value * rhs))
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: QuadraticMonomialCellOf<V>): QuadraticMonomialCellOf<V> {
        return when (cell) {
            is Either.Left -> when (rhs.cell) {
                is Either.Left -> QuadraticMonomialCellOf<V>(Either.Left(cell.value * rhs.cell.value))
                is Either.Right -> QuadraticMonomialCellOf<V>(Either.Left(cell.value * rhs.cell.value))
            }
            is Either.Right -> when (rhs.cell) {
                is Either.Left -> QuadraticMonomialCellOf<V>(Either.Left(rhs.cell.value * cell.value))
                is Either.Right -> QuadraticMonomialCellOf<V>(Either.Right(cell.value * rhs.cell.value))
            }
        }
    }

    override fun div(rhs: Flt64): QuadraticMonomialCellOf<V> {
        return when (cell) {
            is Either.Left -> QuadraticMonomialCellOf<V>(Either.Left(cell.value / rhs))
            is Either.Right -> QuadraticMonomialCellOf<V>(Either.Right(cell.value / rhs))
        }
    }

    override fun copy(): QuadraticMonomialCellOf<V> {
        return when (cell) {
            is Either.Left -> QuadraticMonomialCellOf<V>(cell.value.coefficient, cell.value.variable1, cell.value.variable2)
            is Either.Right -> QuadraticMonomialCellOf<V>(cell.value)
        }
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
            is Either.Right -> "${cell.value}"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val result = when (val c = cell) {
            is Either.Left -> evaluateFromTriple(c.value, tokenList, null)
            is Either.Right -> c.value
        }
        return result ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val result = when (val c = cell) {
            is Either.Left -> evaluateFromTripleByIndex(c.value, results, tokenList)
            is Either.Right -> c.value
        }
        return result ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Flt64? {
        val result = when (val c = cell) {
            is Either.Left -> evaluateFromTripleByValues(c.value, values, tokenList)
            is Either.Right -> c.value
        }
        return result ?: if (zeroIfNone) Flt64.zero else null
    }

    private fun evaluateFromTriple(triple: QuadraticCellTripleOf<V>, tokenList: AbstractTokenList, unused: Any?): Flt64? {
        val token1 = tokenList.find(triple.variable1) ?: return null
        val r1 = token1.result ?: return null
        if (triple.variable2 == null) return triple.coefficient * r1
        val token2 = tokenList.find(triple.variable2) ?: return null
        val r2 = token2.result ?: return null
        return triple.coefficient * r1 * r2
    }

    private fun evaluateFromTripleByIndex(triple: QuadraticCellTripleOf<V>, results: List<Flt64>, tokenList: AbstractTokenList): Flt64? {
        val idx1 = tokenList.indexOf(triple.variable1) ?: return null
        if (idx1 == -1) return null
        if (triple.variable2 == null) return triple.coefficient * results[idx1]
        val idx2 = tokenList.indexOf(triple.variable2) ?: return null
        return triple.coefficient * results[idx1] * results[idx2]
    }

    private fun evaluateFromTripleByValues(triple: QuadraticCellTripleOf<V>, values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?): Flt64? {
        val v1 = values[triple.variable1] ?: tokenList?.find(triple.variable1)?.result ?: return null
        if (triple.variable2 == null) return triple.coefficient * v1
        val v2 = values[triple.variable2] ?: tokenList?.find(triple.variable2)?.result ?: return null
        return triple.coefficient * v1 * v2
    }
}

/**
 * Legacy typealias for Flt64-specific QuadraticMonomialCell.
 */
typealias QuadraticMonomialCell = QuadraticMonomialCellOf<Flt64>

// ========== QuadraticMonomialSymbol ==========

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
            return QuadraticMonomialSymbol(Variant3.V1(variable1), Variant3.V1(variable2))
        }

        operator fun invoke(variable: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V1(variable), Variant3.V2(symbol))
        }

        operator fun invoke(variable: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V1(variable), Variant3.V3(symbol))
        }

        operator fun invoke(symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V2(symbol1), Variant3.V2(symbol2))
        }

        operator fun invoke(symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V2(symbol1), Variant3.V3(symbol2))
        }

        operator fun invoke(symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomialSymbol {
            return QuadraticMonomialSymbol(Variant3.V3(symbol1), Variant3.V3(symbol2))
        }

        operator fun invoke(symbol: LinearMonomialSymbol): QuadraticMonomialSymbol {
            return when (symbol.symbol) {
                is Either.Left -> QuadraticMonomialSymbol(symbol.symbol.value)
                is Either.Right -> QuadraticMonomialSymbol(symbol.symbol.value)
            }
        }

        operator fun invoke(symbol1: LinearMonomialSymbol, symbol2: LinearMonomialSymbol): QuadraticMonomialSymbol {
            return when (symbol1.symbol) {
                is Either.Left -> when (symbol2.symbol) {
                    is Either.Left -> QuadraticMonomialSymbol(symbol1.symbol.value, symbol2.symbol.value)
                    is Either.Right -> QuadraticMonomialSymbol(symbol1.symbol.value, symbol2.symbol.value)
                }
                is Either.Right -> QuadraticMonomialSymbol(symbol1.symbol.value)
            }
        }

        val QuadraticMonomialSymbolUnit.category
            get() = when (this) {
                is Variant3.V1 -> Linear
                is Variant3.V2 -> this.value.category
                is Variant3.V3 -> this.value.category
            }

        val QuadraticMonomialSymbolUnit.name
            get() = when (this) {
                is Variant3.V1 -> this.value.name
                is Variant3.V2 -> this.value.name
                is Variant3.V3 -> this.value.name
            }

        val QuadraticMonomialSymbolUnit.displayName
            get() = when (this) {
                is Variant3.V1 -> this.value.name
                is Variant3.V2 -> this.value.displayName
                is Variant3.V3 -> this.value.displayName
            }

        val QuadraticMonomialSymbolUnit.discrete
            get() = when (this) {
                is Variant3.V1 -> this.value.type.isIntegerType
                is Variant3.V2 -> this.value.discrete
                is Variant3.V3 -> this.value.discrete
            }

        val QuadraticMonomialSymbolUnit.range
            get() = when (this) {
                is Variant3.V1 -> this.value.range
                is Variant3.V2 -> this.value.range
                is Variant3.V3 -> this.value.range
            }

        val QuadraticMonomialSymbolUnit.flattenedMonomials: QuadraticFlattenData
            get() = when (this) {
                is Variant3.V1 -> QuadraticFlattenData(
                    monomials = listOf(UtilsQuadraticMonomial(Flt64.one, this.value, null)),
                    constant = Flt64.zero
                )
                is Variant3.V2 -> this.value.flattenedMonomials.toQuadraticFlattenData()
                is Variant3.V3 -> this.value.flattenedMonomials
            }

        @Deprecated(
            message = "Use flattenedMonomials instead. cells is transitional.",
            level = DeprecationLevel.WARNING
        )
        val QuadraticMonomialSymbolUnit.cells
            get() = flattenedMonomials.toQuadraticMonomialCells()

        val QuadraticMonomialSymbolUnit.cached
            get() = when (this) {
                is Variant3.V1 -> false
                is Variant3.V2 -> this.value.cached
                is Variant3.V3 -> this.value.cached
            }

        val QuadraticMonomialSymbolUnit.hash
            get() = when (this) {
                is Variant3.V1 -> this.value.hashCode()
                is Variant3.V2 -> this.value.name.hashCode()
                is Variant3.V3 -> this.value.name.hashCode()
            }

        infix fun QuadraticMonomialSymbolUnit?.eq(rhs: QuadraticMonomialSymbolUnit?): Boolean {
            return if (this != null && rhs != null) {
                this.v1 == rhs.v1 && this.v2 == rhs.v2 && this.v3 == rhs.v3
            } else this == null && rhs == null
        }

        fun QuadraticMonomialSymbolUnit.toRawString(unfold: UInt64): String {
            return when (this) {
                is Variant3.V1 -> this.value.name
                is Variant3.V2 -> "$this.value"
                is Variant3.V3 -> "$this.value"
            }
        }

        fun QuadraticMonomialSymbolUnit.value(
            tokenList: AbstractTokenList, zeroIfNone: Boolean
        ): Flt64? {
            val result: Flt64? = when (this) {
                is Variant3.V1 -> {
                    val token = tokenList.find(this.value)
                    token?.result
                }
                is Variant3.V2 -> this.value.evaluate(tokenList, zeroIfNone)
                is Variant3.V3 -> this.value.evaluate(tokenList, zeroIfNone)
            }
            if (result == null) logger.trace { "Unknown token for ${this.toRawString(UInt64.zero)}." }
            return result ?: if (zeroIfNone) Flt64.zero else null
        }

        fun QuadraticMonomialSymbolUnit.value(
            tokenTable: AbstractTokenTable, zeroIfNone: Boolean
        ): Flt64? {
            val result: Flt64? = when (this) {
                is Variant3.V1 -> {
                    val token = tokenTable.find(this.value)
                    token?.result
                }
                is Variant3.V2 -> this.value.evaluate(tokenTable, zeroIfNone)
                is Variant3.V3 -> this.value.evaluate(tokenTable, zeroIfNone)
            }
            if (result == null) logger.trace { "Unknown token for ${this.toRawString(UInt64.zero)}." }
            return result ?: if (zeroIfNone) Flt64.zero else null
        }

        fun QuadraticMonomialSymbolUnit.value(
            results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean
        ): Flt64? {
            val result: Flt64? = when (this) {
                is Variant3.V1 -> {
                    val index = tokenList.indexOf(this.value)
                    if (index != null && index != -1) results[index]
                    else null
                }
                is Variant3.V2 -> this.value.evaluate(results, tokenList, zeroIfNone)
                is Variant3.V3 -> this.value.evaluate(results, tokenList, zeroIfNone)
            }
            if (result == null) logger.trace { "Unknown token for ${this.toRawString(UInt64.zero)}." }
            return result ?: if (zeroIfNone) Flt64.zero else null
        }

        fun QuadraticMonomialSymbolUnit.value(
            results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean
        ): Flt64? {
            val result: Flt64? = when (this) {
                is Variant3.V1 -> {
                    val index = tokenTable.indexOf(this.value)
                    if (index != null && index != -1) results[index]
                    else null
                }
                is Variant3.V2 -> this.value.evaluate(results, tokenTable, zeroIfNone)
                is Variant3.V3 -> this.value.evaluate(results, tokenTable, zeroIfNone)
            }
            if (result == null) logger.trace { "Unknown token for ${this.toRawString(UInt64.zero)}." }
            return result ?: if (zeroIfNone) Flt64.zero else null
        }

        fun QuadraticMonomialSymbolUnit.value(
            values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean
        ): Flt64? {
            val result: Flt64? = when (this) {
                is Variant3.V1 -> {
                    values[this.value] ?: tokenList?.find(this.value)?.result
                }
                is Variant3.V2 -> values[this.value] ?: this.value.evaluate(values, tokenList, zeroIfNone)
                is Variant3.V3 -> values[this.value] ?: this.value.evaluate(values, tokenList, zeroIfNone)
            }
            if (result == null) logger.trace { "Unknown token for ${this.toRawString(UInt64.zero)}." }
            return result ?: if (zeroIfNone) Flt64.zero else null
        }

        fun QuadraticMonomialSymbolUnit.value(
            values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean
        ): Flt64? {
            val result: Flt64? = when (this) {
                is Variant3.V1 -> {
                    values[this.value] ?: tokenTable?.find(this.value)?.result
                }
                is Variant3.V2 -> values[this.value] ?: this.value.evaluate(values, tokenTable, zeroIfNone)
                is Variant3.V3 -> values[this.value] ?: this.value.evaluate(values, tokenTable, zeroIfNone)
            }
            if (result == null) logger.trace { "Unknown token for ${this.toRawString(UInt64.zero)}." }
            return result ?: if (zeroIfNone) Flt64.zero else null
        }
    }

    override val name by lazy {
        if (symbol2 == null) symbol1.name else "${symbol1.name} * ${symbol2.name}"
    }

    override val displayName by lazy {
        if (symbol2 == null) symbol1.displayName
        else if (symbol1.displayName != null && symbol2.displayName != null) "${symbol1.displayName} * ${symbol2.displayName}"
        else null
    }

    override val category: Category
        get() = if (symbol2 != null) Quadratic else symbol1.category

    override val discrete by lazy { symbol1.discrete && (symbol2?.discrete != false) }

    override val range: ExpressionRange<*>
        get() = if (symbol2 == null) symbol1.range
        else ExpressionRange((symbol1.range.valueRange!! * symbol2.range.valueRange!!)!!)

    override val lowerBound get() = range.lowerBound?.toFlt64()
    override val upperBound get() = range.upperBound?.toFlt64()

    val pure by lazy { symbol1 is Variant3.V1 && symbol2 is Variant3.V1 }

    val flattenedMonomials: QuadraticFlattenData
        get() = if (symbol2 == null) symbol1.flattenedMonomials
        else {
            val flatten1 = symbol1.flattenedMonomials
            val flatten2 = symbol2.flattenedMonomials
            val monomials = ArrayList<UtilsQuadraticMonomial<Flt64>>()
            // m1 * m2 terms
            for (m1 in flatten1.monomials) {
                for (m2 in flatten2.monomials) {
                    monomials.add(UtilsQuadraticMonomial(
                        m1.coefficient * m2.coefficient,
                        m1.symbol1 as AbstractVariableItem<*, *>,
                        m2.symbol1 as AbstractVariableItem<*, *>?
                    ))
                }
            }
            // m1 * c2 terms
            if (flatten2.constant neq Flt64.zero) {
                for (m1 in flatten1.monomials) {
                    monomials.add(UtilsQuadraticMonomial(
                        m1.coefficient * flatten2.constant,
                        m1.symbol1 as AbstractVariableItem<*, *>,
                        null
                    ))
                }
            }
            // c1 * m2 terms
            if (flatten1.constant neq Flt64.zero) {
                for (m2 in flatten2.monomials) {
                    monomials.add(UtilsQuadraticMonomial(
                        flatten1.constant * m2.coefficient,
                        m2.symbol1 as AbstractVariableItem<*, *>,
                        null
                    ))
                }
            }
            QuadraticFlattenData(monomials, flatten1.constant * flatten2.constant)
        }

    @Deprecated(
        message = "Use flattenedMonomials instead. cells is transitional.",
        level = DeprecationLevel.WARNING
    )
    val cells: List<QuadraticMonomialCell>
        get() = flattenedMonomials.toQuadraticMonomialCells()

    val cached get() = symbol1.cached || (symbol2?.cached == true)

    override fun partialEq(rhs: QuadraticMonomialSymbol): Boolean =
        symbol1 eq rhs.symbol1 && symbol2 eq rhs.symbol2

    override fun hashCode(): Int {
        var result = symbol1.hash
        result = 31 * result + (symbol2?.hash ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as QuadraticMonomialSymbol
        return (symbol1 eq other.symbol1) && (symbol2 eq other.symbol2)
    }

    override fun toString() = if (symbol2 == null) symbol1.name else "${symbol1.name} * ${symbol2.name}"

    override fun toRawString(unfold: UInt64): String =
        if (symbol2 == null) symbol1.toRawString(unfold) else "${symbol1.toRawString(unfold)} * ${symbol2.toRawString(unfold)}"

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? =
        if (symbol2 == null) symbol1.value(tokenList, zeroIfNone)
        else symbol1.value(tokenList, zeroIfNone)?.let { v1 -> symbol2.value(tokenList, zeroIfNone)?.let { v2 -> v1 * v2 } }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? =
        if (symbol2 == null) symbol1.value(tokenTable, zeroIfNone)
        else symbol1.value(tokenTable, zeroIfNone)?.let { v1 -> symbol2.value(tokenTable, zeroIfNone)?.let { v2 -> v1 * v2 } }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? =
        if (symbol2 == null) symbol1.value(results, tokenList, zeroIfNone)
        else symbol1.value(results, tokenList, zeroIfNone)?.let { v1 -> symbol2.value(results, tokenList, zeroIfNone)?.let { v2 -> v1 * v2 } }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? =
        if (symbol2 == null) symbol1.value(results, tokenTable, zeroIfNone)
        else symbol1.value(results, tokenTable, zeroIfNone)?.let { v1 -> symbol2.value(results, tokenTable, zeroIfNone)?.let { v2 -> v1 * v2 } }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList?, zeroIfNone: Boolean): Flt64? =
        if (symbol2 == null) symbol1.value(values, tokenList, zeroIfNone)
        else symbol1.value(values, tokenList, zeroIfNone)?.let { v1 -> symbol2.value(values, tokenList, zeroIfNone)?.let { v2 -> v1 * v2 } }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable?, zeroIfNone: Boolean): Flt64? =
        if (symbol2 == null) symbol1.value(values, tokenTable, zeroIfNone)
        else symbol1.value(values, tokenTable, zeroIfNone)?.let { v1 -> symbol2.value(values, tokenTable, zeroIfNone)?.let { v2 -> v1 * v2 } }
}

// ========== QuadraticMonomial ==========

class QuadraticMonomial(
    override val coefficient: Flt64,
    override val symbol: QuadraticMonomialSymbol,
    override var name: String = "",
    override var displayName: String? = null
) : Monomial<QuadraticMonomial, QuadraticMonomialCell>, Eq<QuadraticMonomial>,
    ToQuadraticPolynomial {
    override val range: ExpressionRange<Flt64>
        get() = symbol.range.valueRange?.let { ExpressionRange(it) } ?: ExpressionRange(null, Flt64)
    companion object {
        operator fun invoke(item: AbstractVariableItem<*, *>): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item))

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item))

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item))

        operator fun invoke(coefficient: Flt64, item: AbstractVariableItem<*, *>): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item))

        operator fun invoke(item1: AbstractVariableItem<*, *>, item2: AbstractVariableItem<*, *>): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item1, item2))

        operator fun invoke(coefficient: Int, item1: AbstractVariableItem<*, *>, item2: AbstractVariableItem<*, *>): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item1, item2))

        operator fun invoke(coefficient: Double, item1: AbstractVariableItem<*, *>, item2: AbstractVariableItem<*, *>): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item1, item2))

        operator fun invoke(coefficient: Flt64, item1: AbstractVariableItem<*, *>, item2: AbstractVariableItem<*, *>): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item1, item2))

        operator fun invoke(item: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item, symbol))

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))

        operator fun invoke(coefficient: Flt64, item: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item, symbol))

        operator fun invoke(item: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(item, symbol))

        operator fun invoke(coefficient: Int, item: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))

        operator fun invoke(coefficient: Double, item: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(item, symbol))

        operator fun invoke(coefficient: Flt64, item: AbstractVariableItem<*, *>, symbol: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(item, symbol))

        operator fun invoke(symbol: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol))

        operator fun invoke(coefficient: Int, symbol: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))

        operator fun invoke(coefficient: Double, symbol: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))

        operator fun invoke(coefficient: Flt64, symbol: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol))

        operator fun invoke(symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Int, symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Double, symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Flt64, symbol1: LinearIntermediateSymbol, symbol2: LinearIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Int, symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Double, symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Flt64, symbol1: LinearIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(symbol: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol))

        operator fun invoke(coefficient: Int, symbol: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))

        operator fun invoke(coefficient: Double, symbol: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol))

        operator fun invoke(coefficient: Flt64, symbol: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol))

        operator fun invoke(symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64.one, QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Int, symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Double, symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(Flt64(coefficient), QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(coefficient: Flt64, symbol1: QuadraticIntermediateSymbol, symbol2: QuadraticIntermediateSymbol): QuadraticMonomial =
            QuadraticMonomial(coefficient, QuadraticMonomialSymbol(symbol1, symbol2))

        operator fun invoke(monomial: LinearMonomial): QuadraticMonomial =
            QuadraticMonomial(monomial.coefficient, QuadraticMonomialSymbol(monomial.symbol))
    }

    val pure by symbol::pure

    override val discrete by lazy { (coefficient.round() eq coefficient) && symbol.discrete }

    override val cells: List<QuadraticMonomialCell>
        get() = flattenedMonomials.toQuadraticMonomialCells()

    override val cached: Boolean get() = symbol.cached

    override fun flush(force: Boolean) {
        // QuadraticMonomial has no internal cache to flush
    }

    override fun copy(): QuadraticMonomial = QuadraticMonomial(coefficient, symbol.copy())

    override operator fun unaryMinus() = QuadraticMonomial(-coefficient, symbol.copy())

    override fun times(rhs: Flt64): QuadraticMonomial = QuadraticMonomial(coefficient * rhs, symbol.copy())

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: AbstractVariableItem<*, *>): QuadraticMonomial {
        if (this.category == Quadratic) throw IllegalArgumentException("Over quadratic.")
        assert(this.symbol.symbol2 == null)
        return when (val s = this.symbol.symbol1) {
            is Variant3.V1 -> QuadraticMonomial(coefficient, s.value, rhs)
            is Variant3.V2 -> QuadraticMonomial(coefficient, rhs, s.value)
            is Variant3.V3 -> QuadraticMonomial(coefficient, rhs, s.value)
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: LinearIntermediateSymbol): QuadraticMonomial {
        if (this.category == Quadratic) throw IllegalArgumentException("Over quadratic.")
        assert(this.symbol.symbol2 == null)
        return when (val s = this.symbol.symbol1) {
            is Variant3.V1 -> QuadraticMonomial(coefficient, s.value, rhs)
            is Variant3.V2 -> QuadraticMonomial(coefficient, s.value, rhs)
            is Variant3.V3 -> QuadraticMonomial(coefficient, rhs, s.value)
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: QuadraticIntermediateSymbol): QuadraticMonomial {
        if (this.category == Quadratic || rhs.category == Quadratic) throw IllegalArgumentException("Over quadratic.")
        assert(this.symbol.symbol2 == null)
        return when (val s = this.symbol.symbol1) {
            is Variant3.V1 -> QuadraticMonomial(coefficient, s.value, rhs)
            is Variant3.V2 -> QuadraticMonomial(coefficient, s.value, rhs)
            is Variant3.V3 -> QuadraticMonomial(coefficient, s.value, rhs)
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: LinearMonomial): QuadraticMonomial {
        if (this.category == Quadratic) throw IllegalArgumentException("Over quadratic.")
        assert(this.symbol.symbol2 == null)
        return when (val s1 = this.symbol.symbol1) {
            is Variant3.V1 -> when (val s2 = rhs.symbol.symbol) {
                is Either.Left -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
                is Either.Right -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
            }
            is Variant3.V2 -> when (val s2 = rhs.symbol.symbol) {
                is Either.Left -> QuadraticMonomial(coefficient * rhs.coefficient, s2.value, s1.value)
                is Either.Right -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
            }
            is Variant3.V3 -> when (val s2 = rhs.symbol.symbol) {
                is Either.Left -> QuadraticMonomial(coefficient * rhs.coefficient, s2.value, s1.value)
                is Either.Right -> QuadraticMonomial(coefficient * rhs.coefficient, s2.value, s1.value)
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    operator fun times(rhs: QuadraticMonomial): QuadraticMonomial {
        if (this.category == Quadratic || rhs.category == Quadratic) throw IllegalArgumentException("Over quadratic.")
        assert(this.symbol.symbol2 == null)
        assert(rhs.symbol.symbol2 == null)
        return when (val s1 = this.symbol.symbol1) {
            is Variant3.V1 -> when (val s2 = rhs.symbol.symbol1) {
                is Variant3.V1 -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
                is Variant3.V2 -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
                is Variant3.V3 -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
            }
            is Variant3.V2 -> when (val s2 = rhs.symbol.symbol1) {
                is Variant3.V1 -> QuadraticMonomial(coefficient * rhs.coefficient, s2.value, s1.value)
                is Variant3.V2 -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
                is Variant3.V3 -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
            }
            is Variant3.V3 -> when (val s2 = rhs.symbol.symbol1) {
                is Variant3.V1 -> QuadraticMonomial(coefficient * rhs.coefficient, s2.value, s1.value)
                is Variant3.V2 -> QuadraticMonomial(coefficient * rhs.coefficient, s2.value, s1.value)
                is Variant3.V3 -> QuadraticMonomial(coefficient * rhs.coefficient, s1.value, s2.value)
            }
        }
    }

    override fun div(rhs: Flt64): QuadraticMonomial = QuadraticMonomial(coefficient / rhs, symbol.copy())

    val flattenedMonomials: QuadraticFlattenData
        get() {
            val symFlatten = symbol.flattenedMonomials
            return QuadraticFlattenData(
                monomials = symFlatten.monomials.map {
                    UtilsQuadraticMonomial(it.coefficient * coefficient, it.symbol1, it.symbol2)
                },
                constant = symFlatten.constant * coefficient
            )
        }

    override fun partialEq(rhs: QuadraticMonomial): Boolean =
        coefficient == rhs.coefficient && symbol == rhs.symbol

    override fun hashCode(): Int {
        var result = coefficient.hashCode()
        result = 31 * result + symbol.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuadraticMonomial) return false
        return coefficient == other.coefficient && symbol == other.symbol
    }

    fun toUtilsMonomial(): UtilsQuadraticMonomial<Flt64> {
        val flatten = flattenedMonomials
        if (flatten.monomials.size == 1 && flatten.constant eq Flt64.zero) {
            return flatten.monomials[0]
        }
        error("Cannot convert complex quadratic monomial to single UtilsQuadraticMonomial")
    }

    override fun toQuadraticPolynomial(): UtilsQuadraticPolynomial<Flt64> {
        return toUtilsPolynomial()
    }

    fun toUtilsPolynomial(): UtilsQuadraticPolynomial<Flt64> {
        val utilsMonomials = flattenedMonomials.monomials.map { m ->
            UtilsQuadraticMonomial(m.coefficient, m.symbol1, m.symbol2)
        }
        return UtilsQuadraticPolynomial(
            monomials = utilsMonomials,
            constant = flattenedMonomials.constant
        )
    }
}

// ========== Scalar * QuadraticMonomial operators ==========

operator fun Int.times(rhs: QuadraticMonomial): QuadraticMonomial =
    QuadraticMonomial(Flt64(this) * rhs.coefficient, rhs.symbol.copy())

operator fun Double.times(rhs: QuadraticMonomial): QuadraticMonomial =
    QuadraticMonomial(Flt64(this) * rhs.coefficient, rhs.symbol.copy())

operator fun <T : RealNumber<T>> T.times(rhs: QuadraticMonomial): QuadraticMonomial =
    QuadraticMonomial(this.toFlt64() * rhs.coefficient, rhs.symbol.copy())
