@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model.monomial

import fuookami.ospf.kotlin.core.intermediate_model.LegacyAbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_model.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.intermediate_model.monomial.MonomialSymbol
import fuookami.ospf.kotlin.core.intermediate_model.toLinearMonomialCells
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractTokenListF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.Bound
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.utils.functional.Eq
import org.apache.logging.log4j.kotlin.logger

typealias LinearMonomialSymbolUnit = Either<AbstractVariableItem<*, *>, LinearIntermediateSymbol<*>>

/**
 * Symbol wrapper for linear monomials.
 * Wraps either a raw AbstractVariableItem or a LinearIntermediateSymbol<*>.
 */
data class LinearMonomialSymbol(
    val symbol: LinearMonomialSymbolUnit
) : MonomialSymbol, Eq<LinearMonomialSymbol> {
    private val logger = logger()

    companion object {
        operator fun invoke(variable: AbstractVariableItem<*, *>): LinearMonomialSymbol {
            return LinearMonomialSymbol(Either.Left(variable))
        }

        operator fun invoke(symbol: LinearIntermediateSymbol<*>): LinearMonomialSymbol {
            return LinearMonomialSymbol(Either.Right(symbol))
        }
    }

    override val name by lazy {
        when (symbol) {
            is Either.Left -> symbol.value.name
            is Either.Right -> symbol.value.name
        }
    }

    override val displayName: String? by lazy {
        when (symbol) {
            is Either.Left -> symbol.value.name
            is Either.Right -> symbol.value.displayName
        }
    }

    override val category by lazy {
        when (symbol) {
            is Either.Left -> Linear
            is Either.Right -> symbol.value.category
        }
    }

    override val discrete: Boolean by lazy {
        when (symbol) {
            is Either.Left -> symbol.value.type.isIntegerType
            is Either.Right -> symbol.value.discrete
        }
    }

    override val range
        get() = when (symbol) {
            is Either.Left -> symbol.value.range
            is Either.Right -> symbol.value.range
        }

    override val lowerBound
        get() = when (symbol) {
            is Either.Left -> symbol.value.lowerBound
            is Either.Right -> symbol.value.lowerBound
        }

    override val upperBound
        get() = when (symbol) {
            is Either.Left -> symbol.value.upperBound
            is Either.Right -> symbol.value.upperBound
        }

    val pure by symbol::isLeft
    val variable by symbol::left
    val exprSymbol by symbol::right

    val flattenedMonomials: LinearFlattenDataF64
        get() = when (symbol) {
            is Either.Left -> {
                LinearFlattenDataF64(
                    monomials = listOf(UtilsLinearMonomial(Flt64.one, symbol.value)),
                    constant = Flt64.zero
                )
            }
            is Either.Right -> symbol.value.flattenedMonomials
        }

    @Deprecated(
        message = "Use flattenedMonomials instead. cells is transitional compatibility layer.",
        level = DeprecationLevel.WARNING
    )
    val cells: List<LinearMonomialCellF64>
        get() = flattenedMonomials.toLinearMonomialCells()

    val cached: Boolean
        get() = when (symbol) {
            is Either.Left -> false
            is Either.Right -> symbol.value.cached
        }

    override fun partialEq(rhs: LinearMonomialSymbol): Boolean {
        return variable == rhs.variable && exprSymbol == rhs.exprSymbol
    }

    override fun hashCode() = when (symbol) {
        is Either.Left -> symbol.value.hashCode()
        is Either.Right -> symbol.value.name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearMonomialSymbol) return false
        if (!this.eq(other)) return false
        return true
    }

    override fun toString() = when (symbol) {
        is Either.Left -> symbol.value.name
        is Either.Right -> symbol.value.name
    }

    override fun toRawString(unfold: UInt64): String {
        return when (symbol) {
            is Either.Left -> symbol.value.name
            is Either.Right -> {
                val exprSymbol = symbol.value
                "$exprSymbol"
            }
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenListF64,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                val token = tokenList.find(symbol.value)
                if (token != null) {
                    val result = token.result
                    if (result != null) result else {
                        logger.trace { "Unknown result for ${symbol.value}" }
                        null
                    }
                } else {
                    logger.trace { "Unknown token for ${symbol.value}" }
                    null
                }
            }
            is Either.Right -> symbol.value.evaluate(tokenList, zeroIfNone)
        } ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        tokenTable: LegacyAbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                val token = tokenTable.find(symbol.value)
                if (token != null) {
                    val result = token.result
                    if (result != null) result else {
                        logger.trace { "Unknown result for ${symbol.value}" }
                        null
                    }
                } else {
                    logger.trace { "Unknown token for ${symbol.value}" }
                    null
                }
            }
            is Either.Right -> symbol.value.evaluate(tokenTable, zeroIfNone)
        } ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenListF64,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                val index = tokenList.indexOf(symbol.value)
                if (index != null && index != -1) results[index] else {
                    logger.trace { "Unknown result for ${symbol.value}" }
                    null
                }
            }
            is Either.Right -> symbol.value.evaluate(results, tokenList, zeroIfNone)
        } ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenTable: LegacyAbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                val index = tokenTable.indexOf(symbol.value)
                if (index != null && index != -1) results[index] else {
                    logger.trace { "Unknown result for ${symbol.value}" }
                    null
                }
            }
            is Either.Right -> symbol.value.evaluate(results, tokenTable, zeroIfNone)
        } ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenListF64?,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                if (values.containsKey(symbol.value)) {
                    values[symbol.value]!!
                } else if (tokenList != null) {
                    val token = tokenList.find(symbol.value)
                    if (token != null) {
                        val result = token.result
                        if (result != null) result else {
                            logger.trace { "Unknown result for ${symbol.value}" }
                            null
                        }
                    } else {
                        logger.trace { "Unknown token for ${symbol.value}" }
                        null
                    }
                } else null
            }
            is Either.Right -> symbol.value.evaluate(values, tokenList, zeroIfNone)
        } ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenTable: LegacyAbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (symbol) {
            is Either.Left -> {
                if (values.containsKey(symbol.value)) {
                    values[symbol.value]!!
                } else if (tokenTable != null) {
                    val token = tokenTable.find(symbol.value)
                    if (token != null) {
                        val result = token.result
                        if (result != null) result else {
                            logger.trace { "Unknown result for ${symbol.value}" }
                            null
                        }
                    } else {
                        logger.trace { "Unknown token for ${symbol.value}" }
                        null
                    }
                } else null
            }
            is Either.Right -> symbol.value.evaluate(values, tokenTable, zeroIfNone)
        } ?: if (zeroIfNone) Flt64.zero else null
    }

    /**
     * Convert to math UtilsLinearMonomial<Flt64>.
     * Replaces the old adapter.toUtilsMonomial() for this type.
     */
    fun toUtilsMonomial(): UtilsLinearMonomial<Flt64> {
        return when (symbol) {
            is Either.Left -> UtilsLinearMonomial(Flt64.one, symbol.value)
            is Either.Right -> {
                val poly = symbol.value.flattenedMonomials
                if (poly.monomials.size == 1 && poly.constant eq Flt64.zero) {
                    poly.monomials[0]
                } else {
                    error("LinearMonomialSymbol with complex polynomial cannot be converted to single UtilsLinearMonomial")
                }
            }
        }
    }
}
