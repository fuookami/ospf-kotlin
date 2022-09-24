package fuookami.ospf.kotlin.core.frontend.expression.polynomial

import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.utils.math.*

sealed interface Polynomial<C : Category> : Expression {
    val monomials: List<Monomial<Linear>>
    val constant: Flt64
    val cells: List<MonomialCell<C>>

    fun flush()
    fun toRawString(): String
}

internal class PolynomialImpl<M : Monomial<C>, C : Category>(
    val monomials: ArrayList<M> = ArrayList(),
    var constant: Flt64 = Flt64.zero,
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
            for (monomial in monomials) {
                monomial.flush()
            }
        }
    }

    fun intersectRange(range: ValueRange<Flt64>) = impl.intersectRange(range)
    fun rangeLess(value: Flt64) = impl.rangeLess(value)
    fun rangeLessEqual(value: Flt64) = impl.rangeLessEqual(value)
    fun rangeGreater(value: Flt64) = impl.rangeGreater(value)
    fun rangeGreaterEqual(value: Flt64) = impl.rangeGreaterEqual(value)

    private fun getPossibleValueRange(): ValueRange<Flt64> {
        return if (monomials.isEmpty()) {
            ValueRange(
                constant,
                constant,
                IntervalType.Closed,
                IntervalType.Closed,
                Flt64
            )
        } else {
            var ret = monomials[0].possibleRange
            for (i in 1 until monomials.size) {
                ret += monomials[i].possibleRange
            }
            ret.plusAssign(constant)
            ret
        }
    }
}
