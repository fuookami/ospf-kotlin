package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_piecewise_function

import fuookami.ospf.kotlin.utils.math.*
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

    class Symbols() {
        lateinit var k: PctVariable1
        lateinit var b: BinVariable1

        lateinit var y: LinearSymbol
    }

    val symbols = Symbols()

    val empty: Boolean get() = size == 0
    val fixed: Boolean get() = size == 0
    val piecewise: Boolean get() = size == 0

    abstract fun pointX(i: Int): Flt64
    abstract fun pointY(i: Int): Flt64

    init {
        symbols.k = PctVariable1("${name}_k", Shape1(size))
        symbols.b = BinVariable1("${name}_b", Shape1(size - 1))
        symbols.y = LinearSymbol(polyY(), "${name}_y")
    }

    override val possibleRange: ValueRange<Flt64> by symbols.y::possibleRange
    override var range: ValueRange<Flt64> by symbols.y::range

    override val cells: List<MonomialCell<Linear>> by symbols.y::cells

    override val lowerBound: Flt64 by symbols.y::lowerBound
    override val upperBound: Flt64 by symbols.y::upperBound

    override fun intersectRange(range: ValueRange<Flt64>) = symbols.y.intersectRange(range)
    override fun rangeLess(value: Flt64) = symbols.y.rangeLess(value)
    override fun rangeLessEqual(value: Flt64) = symbols.y.rangeLessEqual(value)
    override fun rangeGreater(value: Flt64) = symbols.y.rangeGreater(value)
    override fun rangeGreaterEqual(value: Flt64) = symbols.y.rangeGreaterEqual(value)

    override fun toRawString() = displayName ?: name

    override fun register(tokenTable: TokenTable<Linear>) {
        tokenTable.add(symbols.k)
        tokenTable.add(symbols.b)
        tokenTable.add(symbols.y)
    }

    override fun register(model: Model<Linear>) {
        model.addConstraint(
            x eq polyX(),
            "${name}_x"
        )

        model.addConstraint(
            sum(symbols.k) leq Flt64.one,
            "${name}_k"
        )
        model.addConstraint(
            sum(symbols.b) leq Flt64.one,
            "${name}_b"
        )

        for (i in 0 until size) {
            val poly = LinearPolynomial()
            if (i != 0) {
                poly += symbols.b[i - 1]!!
            }
            if (i != (size - 1)) {
                poly += symbols.b[i]!!
            }
            model.addConstraint(
                symbols.k[i]!! leq poly,
                "${name}_kb_${i}"
            )
        }
    }

    private fun polyX(): Polynomial<Linear> {
        assert(!fixed)

        val poly = LinearPolynomial()
        for (i in 0 until size) {
            poly += pointX(i) * symbols.k[i]!!
        }
        return poly
    }

    private fun polyY(): Polynomial<Linear> {
        assert(!fixed)

        val poly = LinearPolynomial()
        for (i in 0 until size) {
            poly += pointY(i) * symbols.k[i]!!
        }
        return poly
    }
}

abstract class MonotoneUnivariateLinearPiecewiseFunction(
    x: LinearPolynomial,
    size: Int,
    name: String,
    displayName: String? = "${name}(${x.name})"
) : UnivariateLinearPiecewiseFunction(x, size, name, displayName) {

}
