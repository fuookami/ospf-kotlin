package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_piecewise_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.Function
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class UnivariateLinearPiecewiseFunction(
    val x: LinearPolynomial,
    val size: Int,
    final override var name: String,
    final override var displayName: String? = "${name}(${x.name})"
) : Function<Linear> {

    private val k: PctVariable1 = PctVariable1("${name}_k", Shape1(size))
    private val b: BinVariable1 = BinVariable1("${name}_b", Shape1(size - 1))

    private lateinit var y: LinearSymbol

    val empty: Boolean get() = size == 0
    val fixed: Boolean get() = size == 1
    val piecewise: Boolean get() = size >= 2

    abstract fun pointX(i: Int): Flt64
    abstract fun pointY(i: Int): Flt64

    override val possibleRange: ValueRange<Flt64> get() = y.possibleRange
    override var range: ValueRange<Flt64>
        get() = if (this::y.isInitialized) { y.range } else { ValueRange(Flt64.minimum, Flt64.maximum, Flt64) }
        set(range) { y.range = range }

    override val cells: List<MonomialCell<Linear>> get() = y.cells

    override val lowerBound: Flt64 get() = y.lowerBound
    override val upperBound: Flt64 get() = y.upperBound

    override fun intersectRange(range: ValueRange<Flt64>) = y.intersectRange(range)
    override fun rangeLess(value: Flt64) = y.rangeLess(value)
    override fun rangeLessEqual(value: Flt64) = y.rangeLessEqual(value)
    override fun rangeGreater(value: Flt64) = y.rangeGreater(value)
    override fun rangeGreaterEqual(value: Flt64) = y.rangeGreaterEqual(value)

    override fun toRawString() = displayName ?: name

    override fun register(tokenTable: TokenTable<Linear>) {
        tokenTable.add(k)
        tokenTable.add(b)
        if (!this::y.isInitialized) {
            y = LinearSymbol(polyY(), "${name}_y")
        }
        tokenTable.add(y)
    }

    override fun register(model: Model<Linear>) {
        model.addConstraint(
            x eq polyX(),
            "${name}_x"
        )

        model.addConstraint(
            sum(k) leq Flt64.one,
            "${name}_k"
        )
        model.addConstraint(
            sum(b) leq Flt64.one,
            "${name}_b"
        )

        for (i in 0 until size) {
            val poly = LinearPolynomial()
            if (i != 0) {
                poly += b[i - 1]!!
            }
            if (i != (size - 1)) {
                poly += b[i]!!
            }
            model.addConstraint(
                k[i]!! leq poly,
                "${name}_kb_i"
            )
        }
    }

    private fun polyX(): Polynomial<Linear> {
        assert(!fixed)

        val poly = LinearPolynomial()
        for (i in 0 until size) {
            poly += pointX(i) * k[i]!!
        }
        return poly
    }

    private fun polyY(): Polynomial<Linear> {
        assert(!fixed)

        val poly = LinearPolynomial()
        for (i in 0 until size) {
            poly += pointY(i) * k[i]!!
        }
        return poly
    }
}

class NormalUnivariateLinearPiecewiseFunction(
    x: LinearPolynomial,
    val points: List<Point2>,
    name: String,
    displayName: String? = "${name}(${x.name})"
): UnivariateLinearPiecewiseFunction(x, points.size, name, displayName) {
    override fun pointX(i: Int): Flt64 {
        return points[i].x
    }

    override fun pointY(i: Int): Flt64 {
        return points[i].y
    }
}

abstract class MonotoneUnivariateLinearPiecewiseFunction(
    x: LinearPolynomial,
    size: Int,
    name: String,
    displayName: String? = "${name}(${x.name})"
) : UnivariateLinearPiecewiseFunction(x, size, name, displayName) {

}
