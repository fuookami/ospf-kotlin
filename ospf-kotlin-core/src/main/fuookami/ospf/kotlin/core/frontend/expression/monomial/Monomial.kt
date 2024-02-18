package fuookami.ospf.kotlin.core.frontend.expression.monomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed interface MonomialCell<Self : MonomialCell<Self, C>, C : Category>
    : Cloneable, Copyable<Self>, Neg<Self>, Plus<Self, Self>, Minus<Self, Self>, Times<Flt64, Self>, Div<Flt64, Self> {
    companion object {
        @Suppress("UNCHECKED_CAST")
        operator fun <Cell : MonomialCell<Cell, C>, C : Category> invoke(constant: Flt64, category: C): Cell {
            return when (category) {
                is Linear -> {
                    LinearMonomialCell(constant) as Cell
                }

                else -> {
                    throw IllegalArgumentException("Unknown monomial cell type: ${category::class}")
                }
            }
        }
    }

    val isConstant: Boolean
    val constant: Flt64?

    operator fun <T : RealNumber<T>> times(rhs: T): Self {
        return this.times(rhs.toFlt64())
    }

    operator fun <T : RealNumber<T>> div(rhs: T): Self {
        return this.div(rhs.toFlt64())
    }

    fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
    fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
}

sealed interface MonomialSymbol<C : Category> {
    val name: String
    val displayName: String?
    val discrete: Boolean get() = false
    val range: ExpressionRange<*>
    val lowerBound: Flt64
    val upperBound: Flt64

    fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
    fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?

    fun toRawString(unfold: Boolean = false): String
}

sealed interface Monomial<Self : Monomial<Self, Cell, C>, Cell : MonomialCell<Cell, C>, C : Category>
    : Expression, Cloneable, Copyable<Self>, Neg<Monomial<Self, Cell, C>>, Times<Flt64, Self>, Div<Flt64, Self> {
    val category: C
    val coefficient: Flt64
    val symbol: MonomialSymbol<C>
    override val discrete: Boolean get() = (coefficient.round() eq coefficient) && symbol.discrete
    val cells: List<Cell>
    val cached: Boolean

    operator fun times(rhs: Int): Self {
        return this.times(Flt64(rhs))
    }

    operator fun times(rhs: Double): Self {
        return this.times(Flt64(rhs))
    }

    operator fun <T : RealNumber<T>> times(rhs: T): Self {
        return this.times(rhs.toFlt64())
    }

    operator fun div(rhs: Int): Self {
        return this.div(Flt64(rhs))
    }

    operator fun div(rhs: Double): Self {
        return this.div(Flt64(rhs))
    }

    operator fun <T : RealNumber<T>> div(rhs: T): Self {
        return this.div(rhs.toFlt64())
    }

    fun flush(force: Boolean = false)

    fun toRawString(unfold: Boolean = false): String {
        return if (coefficient eq Flt64.one) {
            symbol.toRawString(unfold)
        } else {
            "$coefficient * ${symbol.toRawString(unfold)}"
        }
    }

    fun value(tokenTable: AbstractTokenTable<Cell, C>, zeroIfNone: Boolean): Flt64? {
        return value(tokenTable.tokenList, zeroIfNone)
    }

    fun value(results: List<Flt64>, tokenTable: AbstractTokenTable<Cell, C>, zeroIfNone: Boolean): Flt64? {
        return value(results, tokenTable.tokenList, zeroIfNone)
    }
}
