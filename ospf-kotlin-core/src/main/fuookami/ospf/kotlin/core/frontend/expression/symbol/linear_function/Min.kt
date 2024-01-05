package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.utils.meta_programming.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.Function
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class MinFunction(
    val polys: List<LinearPolynomial>,
    val exact: Boolean = false,
    override var name: String,
    override var displayName: String? = name
) : Function<Linear> {
    class Symbols {
        lateinit var y: RealVar

        lateinit var u: BinVariable1
    }

    val symbols = Symbols()
    private val m: Flt64 = polys.maxOf { it.possibleRange.upperBound }.toFlt64()

    init {
        symbols.y = RealVar("${name}_y")

        if (exact) {
            symbols.u = BinVariable1("${name}_u", Shape1(polys.size))
            for ((i, poly) in polys.withIndex()) {
                symbols.u[i]!!.name = "${symbols.u.name}_${poly.name}"
            }
        }
    }

    override val possibleRange: ValueRange<Flt64> by lazy {
        val (min, max) = polys.minMaxOf { it.possibleRange.lowerBound.toFlt64() }
        ValueRange(min, max, Flt64)
    }
    override var range: ValueRange<Flt64> by lazyDelegate(MinFunction::possibleRange)

    override val cells: List<MonomialCell<Linear>> get() = listOf(LinearMonomialCell(Flt64.one, symbols.y))

    override val lowerBound: Flt64 by symbols.y::lowerBound
    override val upperBound: Flt64 by symbols.y::upperBound

    override fun intersectRange(range: ValueRange<Flt64>) = symbols.y.range.intersectWith(range)
    override fun rangeLess(value: Flt64) = symbols.y.range.ls(value)
    override fun rangeLessEqual(value: Flt64) = symbols.y.range.leq(value)
    override fun rangeGreater(value: Flt64) = symbols.y.range.gr(value)
    override fun rangeGreaterEqual(value: Flt64) = symbols.y.range.geq(value)

    override fun toString(): String {
        return "min(${polys.joinToString { it.toString() }})"
    }

    override fun toRawString() = displayName ?: name

    override fun register(tokenTable: TokenTable<Linear>) {
        tokenTable.add(symbols.y)
    }

    override fun register(model: Model<Linear>) {
        for (poly in polys) {
            model.addConstraint(
                symbols.y leq poly,
                "${name}_lb_${poly.name}"
            )
        }

        if (exact) {
            for ((i, poly) in polys.withIndex()) {
                model.addConstraint(
                    symbols.y geq (poly + m * (Flt64.one - symbols.u[i]!!)),
                    "${name}_ub_${poly.name}"
                )
            }

            model.addConstraint(sum(symbols.u) eq Flt64.one, "${name}_u")
        }
    }
}
