package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.Function
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class OrFunction(
    /** all polys must be ∈ {0, 1} */
    val polys: List<LinearPolynomial>,
    override var name: String,
    override var displayName: String? = name
) : Function<Linear> {
    class Symbols {
        lateinit var y: BinVar
    }

    val symbols = Symbols()

    override fun toRawString() = displayName ?: name

    init {
        symbols.y = BinVar("${name}_y")
    }

    override val possibleRange: ValueRange<Flt64> get() = ValueRange(polys.minOf { it.possibleRange.lowerBound }.toFlt64(), polys.maxOf { it.possibleRange.lowerBound }.toFlt64(), Flt64)
    override var range: ValueRange<Flt64> = possibleRange

    override val cells: List<MonomialCell<Linear>> get() = listOf(LinearMonomialCell(Flt64.one, symbols.y))

    override val lowerBound: Flt64 by symbols.y::lowerBound
    override val upperBound: Flt64 by symbols.y::upperBound

    override fun intersectRange(range: ValueRange<Flt64>) = symbols.y.range.intersectWith(ValueRange(range.lowerBound.toFlt64().toUInt8(), range.upperBound.toFlt64().toUInt8(), UInt8))
    override fun rangeLess(value: Flt64) = symbols.y.range.ls(value.toUInt8())
    override fun rangeLessEqual(value: Flt64) = symbols.y.range.leq(value.toUInt8())
    override fun rangeGreater(value: Flt64) = symbols.y.range.gr(value.toUInt8())
    override fun rangeGreaterEqual(value: Flt64) = symbols.y.range.geq(value.toUInt8())

    override fun register(tokenTable: TokenTable<Linear>) {
        tokenTable.add(symbols.y)
    }

    override fun register(model: Model<Linear>) {
        for (poly in polys) {
            model.addConstraint(
                symbols.y geq poly,
                "${name}_ub_${poly.name}"
            )
        }
    }
}
