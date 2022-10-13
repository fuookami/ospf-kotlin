package fuookami.ospf.kotlin.core.frontend.expression.monomial

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*

sealed interface MonomialCell<C : Category> : Cloneable, Copyable<MonomialCell<C>> {
    @Throws(IllegalArgumentException::class)
    operator fun plusAssign(rhs: MonomialCell<C>)

    @Throws(IllegalArgumentException::class)
    operator fun minusAssign(rhs: MonomialCell<C>)
    operator fun <T : RealNumber<T>> timesAssign(rhs: T)

    fun isConstant(): Boolean
    fun constant(): Flt64?

    fun value(tokenList: TokenList): Flt64
    fun value(results: List<Flt64>, tokenList: TokenList): Flt64
}

sealed interface MonomialSymbol<C : Category> {
    val name: String
    val lowerBound: Flt64
    val upperBound: Flt64
    val range: ValueRange<Flt64>
}

interface Monomial<C : Category> : Expression, Neg<Monomial<C>> {
    val category: Category

    fun flush()
}

sealed interface SimpleMonomial<C : Category> : Monomial<C> {
    var coefficient: Flt64
    val symbol: MonomialSymbol<C>
    val cells: List<MonomialCell<C>>

    fun toRawString() = ""
}

internal class MonomialImpl<S : MonomialSymbol<C>, C : Category>(
    var coefficient: Flt64,
    val symbol: S,
    private val cellsGenerator: () -> MutableList<MonomialCell<C>>
) {
    private val impl = ExpressionImpl { getPossibleValueRange() }

    val possibleRange: ValueRange<Flt64> by impl::possibleRange
    var range: ValueRange<Flt64> by impl::range

    private lateinit var _cells: MutableList<MonomialCell<C>>
    val cells: List<MonomialCell<C>>
        get() {
            if (!this::_cells.isInitialized || _cells.isEmpty()) {
                _cells = cellsGenerator()
            }
            return _cells
        }

    fun flush() {
        if (this::_cells.isInitialized) {
            _cells.clear()
        }
    }

    fun intersectRange(range: ValueRange<Flt64>) = impl.intersectRange(range)
    fun rangeLess(value: Flt64) = impl.rangeLess(value)
    fun rangeLessEqual(value: Flt64) = impl.rangeLessEqual(value)
    fun rangeGreater(value: Flt64) = impl.rangeGreater(value)
    fun rangeGreaterEqual(value: Flt64) = impl.rangeGreaterEqual(value)

    private fun getPossibleValueRange() = coefficient * ValueRange(
        symbol.lowerBound,
        symbol.upperBound,
        symbol.range.lowerInterval,
        symbol.range.upperInterval,
        Flt64
    )
}
