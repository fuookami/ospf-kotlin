package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_piecewise_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.Function
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class AbsFunction(
    val x: LinearPolynomial,
    override var name: String = "${x.name}_abs",
    override var displayName: String? = "|${x.name}|"
) : Function<Linear> {

    class Symbols() {
        lateinit var neg: URealVar
        lateinit var pos: URealVar

        lateinit var y: LinearSymbol
    }

    val symbols = Symbols()

    init {
        symbols.neg = URealVar("${x.name}_neg")
        symbols.pos = URealVar("${x.name}_pos")
        symbols.y = LinearSymbol(polyY(), "${x.name}_abs_y")
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
        tokenTable.add(symbols.neg)
        tokenTable.add(symbols.pos)
        tokenTable.add(symbols.y)
    }

    override fun register(model: Model<Linear>) {
        model.addConstraint(
            symbols.pos geq x,
            "${name}_pos"
        )
        model.addConstraint(
            symbols.neg geq (-Flt64.one * x),
            "${name}_neg"
        )
    }

    private fun polyY(): Polynomial<Linear> {
        val poly = LinearPolynomial()
        poly += symbols.pos
        poly += symbols.neg
        return poly
    }
}
