@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model.monomial

import fuookami.ospf.kotlin.core.intermediate_model.LegacyAbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_model.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.intermediate_model.ToLinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.ToQuadraticPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.Monomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.MonomialCell
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractTokenListF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.algebra.value_range.times as vr_times
import fuookami.ospf.kotlin.math.operator.Abs
import fuookami.ospf.kotlin.math.operator.abs

/**
 * CellF64 representation for linear monomials with type parameter V.
 * Either a (coefficient, variable) pair or a constant value.
 * Internal implementation uses Flt64.
 */
data class LinearMonomialCell<V> internal constructor(
    val cell: Either<LinearCellPair<V>, Flt64>
) : Copyable<LinearMonomialCell<V>>, MonomialCell<LinearMonomialCell<V>> {

    data class LinearCellPair<V>(
        val coefficient: Flt64,
        val variable: AbstractVariableItem<*, *>
    ) : Copyable<LinearCellPair<V>> {
        operator fun unaryMinus() = LinearCellPair<V>(-coefficient, variable)
        operator fun plus(rhs: LinearCellPair<V>) = LinearCellPair<V>(coefficient + rhs.coefficient, variable)
        operator fun minus(rhs: LinearCellPair<V>) = LinearCellPair<V>(coefficient - rhs.coefficient, variable)
        operator fun times(rhs: Flt64) = LinearCellPair<V>(coefficient * rhs, variable)
        operator fun div(rhs: Flt64) = LinearCellPair<V>(coefficient / rhs, variable)
        override fun copy() = LinearCellPair<V>(coefficient, variable)
        override fun hashCode(): Int = variable.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LinearCellPair<*>) return false
            if (coefficient != other.coefficient) return false
            if (variable != other.variable) return false
            return true
        }
    }

    companion object {
        operator fun <V> invoke(coefficient: Flt64, variable: AbstractVariableItem<*, *>): LinearMonomialCell<V> =
            LinearMonomialCell(Either.Left(LinearCellPair<V>(coefficient, variable)))
        operator fun <V> invoke(variable: AbstractVariableItem<*, *>): LinearMonomialCell<V> =
            LinearMonomialCell(Either.Left(LinearCellPair<V>(Flt64.one, variable)))
        operator fun <V> invoke(constant: Flt64): LinearMonomialCell<V> =
            LinearMonomialCell(Either.Right(constant))
    }

    val isPair by cell::isLeft
    override val isConstant by cell::isRight
    val pair by cell::left
    override val constant by cell::right

    override operator fun unaryMinus(): LinearMonomialCell<V> = when (cell) {
        is Either.Left -> LinearMonomialCell(Either.Left(-cell.value))
        is Either.Right -> LinearMonomialCell(Either.Right(-cell.value))
    }

    override operator fun plus(rhs: LinearMonomialCell<V>): LinearMonomialCell<V> {
        return when {
            cell.isLeft && rhs.cell.isLeft -> LinearMonomialCell(Either.Left(cell.left!! + rhs.cell.left!!))
            cell.isRight && rhs.cell.isRight -> LinearMonomialCell(Either.Right(cell.right!! + rhs.cell.right!!))
            else -> throw IllegalArgumentException("Cannot add monomial and constant")
        }
    }

    override operator fun minus(rhs: LinearMonomialCell<V>): LinearMonomialCell<V> {
        return when {
            cell.isLeft && rhs.cell.isLeft -> LinearMonomialCell(Either.Left(cell.left!! - rhs.cell.left!!))
            cell.isRight && rhs.cell.isRight -> LinearMonomialCell(Either.Right(cell.right!! - rhs.cell.right!!))
            else -> throw IllegalArgumentException("Cannot subtract monomial and constant")
        }
    }

    override operator fun times(rhs: Flt64): LinearMonomialCell<V> = when (cell) {
        is Either.Left -> LinearMonomialCell(Either.Left(cell.value * rhs))
        is Either.Right -> LinearMonomialCell(Either.Right(cell.value * rhs))
    }

    override operator fun div(rhs: Flt64): LinearMonomialCell<V> = when (cell) {
        is Either.Left -> LinearMonomialCell(Either.Left(cell.value / rhs))
        is Either.Right -> LinearMonomialCell(Either.Right(cell.value / rhs))
    }

    override fun copy(): LinearMonomialCell<V> {
        return when (cell) {
            is Either.Left -> LinearMonomialCell(cell.value.coefficient, cell.value.variable)
            is Either.Right -> LinearMonomialCell(cell.value)
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenListF64,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (cell) {
            is Either.Left -> {
                val token = tokenList.find(cell.value.variable)
                token?.result?.let { cell.value.coefficient * it }
            }
            is Either.Right -> cell.value
        } ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenListF64,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (cell) {
            is Either.Left -> {
                val index = tokenList.indexOf(cell.value.variable)
                if (index != null && index != -1) cell.value.coefficient * results[index] else null
            }
            is Either.Right -> cell.value
        } ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenListF64?,
        zeroIfNone: Boolean
    ): Flt64? {
        return when (cell) {
            is Either.Left -> {
                values[cell.value.variable]?.let { cell.value.coefficient * it }
                    ?: tokenList?.let {
                        it.find(cell.value.variable)?.result?.let { r -> cell.value.coefficient * r }
                    }
            }
            is Either.Right -> cell.value
        } ?: if (zeroIfNone) Flt64.zero else null
    }
}

/**
 * Legacy typealias for Flt64-specific LinearMonomialCellF64.
 */
typealias LinearMonomialCellF64 = LinearMonomialCell<Flt64>

/**
 * A linear monomial: coefficient * symbol, where symbol is either a variable or an intermediate symbol.
 * Implements the core contract that was previously in expression.monomial.LinearMonomial.
 */
data class LinearMonomial(
    override val coefficient: Flt64,
    override val symbol: LinearMonomialSymbol
) : Monomial<LinearMonomial, LinearMonomialCellF64>, Eq<LinearMonomial>,
    ToLinearPolynomial, ToQuadraticPolynomial {

    override val cached: Boolean get() = symbol.cached

    companion object {
        operator fun invoke(coefficient: Int, variable: AbstractVariableItem<*, *>): LinearMonomial {
            return LinearMonomial(Flt64(coefficient), LinearMonomialSymbol(variable))
        }

        operator fun invoke(coefficient: Int, symbol: LinearIntermediateSymbol<*>): LinearMonomial {
            return LinearMonomial(Flt64(coefficient), LinearMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Flt64, variable: AbstractVariableItem<*, *>): LinearMonomial {
            return LinearMonomial(coefficient, LinearMonomialSymbol(variable))
        }

        operator fun invoke(variable: AbstractVariableItem<*, *>): LinearMonomial {
            return LinearMonomial(Flt64.one, LinearMonomialSymbol(variable))
        }

        operator fun invoke(symbol: LinearIntermediateSymbol<*>): LinearMonomial {
            return LinearMonomial(Flt64.one, LinearMonomialSymbol(symbol))
        }

        operator fun invoke(coefficient: Flt64, symbol: LinearIntermediateSymbol<*>): LinearMonomial {
            return LinearMonomial(coefficient, LinearMonomialSymbol(symbol))
        }

        operator fun invoke(variable: AbstractVariableItem<*, *>, symbol: LinearIntermediateSymbol<*>): LinearMonomial {
            return LinearMonomial(Flt64.one, LinearMonomialSymbol(symbol))
        }

        /** Construct from coefficient and raw Symbol (convenience) */
        operator fun invoke(coefficient: Flt64, symbol: Symbol): LinearMonomial {
            return when (symbol) {
                is AbstractVariableItem<*, *> -> LinearMonomial(coefficient, symbol)
                is LinearIntermediateSymbol<*> -> LinearMonomial(coefficient, symbol)
                else -> error("Unsupported symbol type: ${symbol::class}")
            }
        }

        operator fun invoke(symbol: Symbol): LinearMonomial {
            return LinearMonomial(Flt64.one, symbol)
        }
    }

    override var name: String = ""
    override var displayName: String? = null
    override val range: ExpressionRange<Flt64>
        get() = when (val sym = symbol.symbol) {
            is Either.Left -> {
                val symRange = sym.value.range.valueRange
                if (symRange != null) {
                    val scaled = coefficient.vr_times(symRange)
                    if (scaled != null) ExpressionRange(scaled)
                    else ExpressionRange(Flt64)
                } else {
                    ExpressionRange(Flt64)
                }
            }
            is Either.Right -> {
                val vr = sym.value.range.valueRange
                if (vr != null) ExpressionRange(vr)
                else ExpressionRange(Flt64)
            }
        }
    override val category: Category get() = symbol.category
    override val discrete: Boolean get() = (coefficient.round() eq coefficient) && symbol.discrete

    override val cells: List<LinearMonomialCellF64>
        get() = when (val sym = symbol.symbol) {
            is Either.Left -> listOf(
                LinearMonomialCell<Flt64>(
                    Either.Left(LinearMonomialCell.LinearCellPair<Flt64>(coefficient, sym.value))
                )
            )
            is Either.Right -> {
                sym.value.flattenedMonomials.monomials.map {
                    LinearMonomialCell<Flt64>(it.coefficient * coefficient, it.symbol as AbstractVariableItem<*, *>)
                } + LinearMonomialCell<Flt64>(sym.value.flattenedMonomials.constant * coefficient)
            }
        }

    val flattenedMonomials: LinearFlattenDataF64
        get() = when (val sym = symbol.symbol) {
            is Either.Left -> {
                LinearFlattenDataF64(
                    monomials = listOf(UtilsLinearMonomial(coefficient, sym.value)),
                    constant = Flt64.zero
                )
            }
            is Either.Right -> {
                val subFlatten = sym.value.flattenedMonomials
                LinearFlattenDataF64(
                    monomials = subFlatten.monomials.map {
                        UtilsLinearMonomial(it.coefficient * coefficient, it.symbol)
                    },
                    constant = subFlatten.constant * coefficient
                )
            }
        }

    override operator fun unaryMinus() = LinearMonomial(-coefficient, symbol)
    override operator fun times(rhs: Flt64) = LinearMonomial(coefficient * rhs, symbol)
    override operator fun div(rhs: Flt64) = LinearMonomial(coefficient / rhs, symbol)
    override operator fun <T : RealNumber<T>> times(rhs: T) = LinearMonomial(coefficient * rhs.toFlt64(), symbol)
    override operator fun <T : RealNumber<T>> div(rhs: T) = LinearMonomial(coefficient / rhs.toFlt64(), symbol)

    override fun toRawString(unfold: UInt64): String {
        val symStr = symbol.toRawString(unfold)
        return if (coefficient eq Flt64.one) symStr
        else if (coefficient eq -Flt64.one) "-$symStr"
        else "$coefficient * $symStr"
    }

    override fun evaluate(
        tokenList: AbstractTokenListF64,
        zeroIfNone: Boolean
    ): Flt64? {
        return symbol.evaluate(tokenList, zeroIfNone)?.let { coefficient * it }
            ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        tokenTable: LegacyAbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return symbol.evaluate(tokenTable, zeroIfNone)?.let { coefficient * it }
            ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenListF64,
        zeroIfNone: Boolean
    ): Flt64? {
        return symbol.evaluate(results, tokenList, zeroIfNone)?.let { coefficient * it }
            ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenTable: LegacyAbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return symbol.evaluate(results, tokenTable, zeroIfNone)?.let { coefficient * it }
            ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenListF64?,
        zeroIfNone: Boolean
    ): Flt64? {
        return symbol.evaluate(values, tokenList, zeroIfNone)?.let { coefficient * it }
            ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenTable: LegacyAbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return symbol.evaluate(values, tokenTable, zeroIfNone)?.let { coefficient * it }
            ?: if (zeroIfNone) Flt64.zero else null
    }

    override fun partialEq(rhs: LinearMonomial): Boolean {
        return symbol == rhs.symbol && coefficient == rhs.coefficient
    }

    override fun flush(force: Boolean) {
        // LinearMonomial has no cache to flush
    }

    override fun copy(): LinearMonomial = LinearMonomial(coefficient, symbol)

    override fun toLinearPolynomial(): UtilsLinearPolynomial<Flt64> {
        val utilsMono = toUtilsMonomial()
        return UtilsLinearPolynomial(monomials = listOf(utilsMono), constant = Flt64.zero)
    }

    override fun toQuadraticPolynomial(): UtilsQuadraticPolynomial<Flt64> {
        val utilsMono = toUtilsMonomial()
        val qMono = UtilsQuadraticMonomial(coefficient, utilsMono.symbol, utilsMono.symbol)
        return UtilsQuadraticPolynomial(monomials = listOf(qMono), constant = Flt64.zero)
    }

    /**
     * Convert to math UtilsLinearMonomial<Flt64>.
     * Only works for pure variable monomials (not wrapping intermediate symbols).
     */
    fun toUtilsMonomial(): UtilsLinearMonomial<Flt64> {
        return when (val sym = symbol.symbol) {
            is Either.Left -> UtilsLinearMonomial(coefficient, sym.value)
            is Either.Right -> {
                // For symbol-based monomials, we need to expand
                val poly = sym.value.flattenedMonomials
                if (poly.monomials.size == 1 && poly.constant eq Flt64.zero) {
                    UtilsLinearMonomial(coefficient * poly.monomials[0].coefficient, poly.monomials[0].symbol)
                } else {
                    error("Cannot convert complex symbol-based monomial to single UtilsLinearMonomial")
                }
            }
        }
    }

    fun abs(): LinearMonomial {
        return LinearMonomial(abs(coefficient as Abs<Flt64>), symbol)
    }
}

// ========== Scalar * LinearMonomial operators ==========

operator fun Int.times(rhs: LinearMonomial): LinearMonomial = LinearMonomial(Flt64(this) * rhs.coefficient, rhs.symbol)
operator fun <T : RealNumber<T>> T.times(rhs: LinearMonomial): LinearMonomial = LinearMonomial(this.toFlt64() * rhs.coefficient, rhs.symbol)

// ========== LinearMonomial + LinearMonomial = list of monomials (used by polynomial constructors) ==========

operator fun LinearMonomial.plus(rhs: LinearMonomial): List<LinearMonomial> = listOf(this, rhs)