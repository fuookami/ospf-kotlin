package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.core.frontend.expression.Expression
import fuookami.ospf.kotlin.core.frontend.expression.ExpressionImpl
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.Polynomial
import fuookami.ospf.kotlin.utils.math.*

interface Symbol<C : Category> : Expression {
    val cells: List<MonomialCell<C>>

    val lowerBound: Flt64
    val upperBound: Flt64

    fun toRawString(): String;
}

class SimpleSymbol<C : Category>(
    val polynomial: Polynomial<C>,
    override var name: String = "",
    override var displayName: String? = null
) : Symbol<C> {

    private val impl = ExpressionImpl { getPossibleValueRange() }

    override val possibleRange: ValueRange<Flt64> by impl::possibleRange
    override var range: ValueRange<Flt64> by impl::range

    override val cells: List<MonomialCell<C>> get() = polynomial.cells
    fun flush() {
        polynomial.flush()
    }

    override val lowerBound: Flt64 get() = range.lowerBound.toFlt64()
    override val upperBound: Flt64 get() = range.upperBound.toFlt64()

    override fun intersectRange(range: ValueRange<Flt64>) = impl.intersectRange(range)
    override fun rangeLess(value: Flt64) = impl.rangeLess(value)
    override fun rangeLessEqual(value: Flt64) = impl.rangeLessEqual(value)
    override fun rangeGreater(value: Flt64) = impl.rangeGreater(value)
    override fun rangeGreaterEqual(value: Flt64) = impl.rangeGreaterEqual(value)

    override fun toString() = polynomial.toString()
    override fun toRawString() = polynomial.toRawString()

    private fun getPossibleValueRange() = polynomial.possibleRange

}

typealias LinearSymbol = SimpleSymbol<Linear>
