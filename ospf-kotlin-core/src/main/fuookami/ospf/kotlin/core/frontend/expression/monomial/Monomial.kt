package fuookami.ospf.kotlin.core.frontend.expression.monomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed interface MonomialCell<Self : MonomialCell<Self>>
    : Cloneable, Copyable<Self>, Neg<Self>, Plus<Self, Self>, Minus<Self, Self>, Times<Flt64, Self>, Div<Flt64, Self> {
    companion object {
        @Suppress("UNCHECKED_CAST")
        operator fun <Cell : MonomialCell<Cell>> invoke(constant: Flt64, category: Category): Cell {
            return when (category) {
                is Linear -> {
                    LinearMonomialCell(constant) as Cell
                }

                is Quadratic -> {
                    QuadraticMonomialCell(constant) as Cell
                }

                else -> {
                    TODO("NOT IMPLEMENT YET")
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

    fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
}

sealed interface MonomialSymbol {
    val name: String
    val displayName: String?
    val category: Category
    val discrete: Boolean get() = false
    val range: ExpressionRange<*>
    val lowerBound: Bound<Flt64>?
    val upperBound: Bound<Flt64>?

    fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64?
    fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64?

    fun toRawString(unfold: Boolean = false): String
}

sealed interface Monomial<Self : Monomial<Self, Cell>, Cell : MonomialCell<Cell>>
    : Expression, Cloneable, Copyable<Self>, Neg<Monomial<Self, Cell>>, Times<Flt64, Self>, Div<Flt64, Self> {
    val coefficient: Flt64
    val symbol: MonomialSymbol
    val category: Category get() = symbol.category
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

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return symbol.evaluate(tokenList, zeroIfNone)?.let { coefficient * it }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return symbol.evaluate(results, tokenList, zeroIfNone)?.let { coefficient * it }
    }

    override fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return symbol.evaluate(tokenTable, zeroIfNone)?.let { coefficient * it }
    }

    override fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return symbol.evaluate(results, tokenTable, zeroIfNone)?.let { coefficient * it }
    }
}
