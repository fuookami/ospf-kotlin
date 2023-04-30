package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
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

    class Symbols {
        lateinit var neg: PctVar
        lateinit var zero: PctVar
        lateinit var pos: PctVar

        lateinit var n: BinVar
        lateinit var p: BinVar

        lateinit var y: LinearSymbol
    }

    val symbols = Symbols()
    private val m = max(abs(x.range.lowerBound.toFlt64()), abs(x.range.upperBound.toFlt64()))

    init {
        symbols.neg = PctVar("${x.name}_neg")
        symbols.zero = PctVar("${x.name}_zero")
        symbols.pos = PctVar("${x.name}_pos")
        symbols.n = BinVar("${x.name}_n")
        symbols.p = BinVar("${x.name}_p")
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
        tokenTable.add(symbols.zero)
        tokenTable.add(symbols.pos)
        tokenTable.add(symbols.n)
        tokenTable.add(symbols.p)
        tokenTable.add(symbols.y)
    }

    override fun register(model: Model<Linear>) {
        model.addConstraint(
            x eq (-m * symbols.neg + m * symbols.pos),
            name
        )
        model.addConstraint(
            (symbols.neg + symbols.zero + symbols.pos) eq Flt64.one,
            "${name}_k"
        )
        model.addConstraint(
            (symbols.n + symbols.p) eq Flt64.one,
            "${name}_b"
        )
        model.addConstraint(
            symbols.neg leq symbols.n,
            "${name}_n"
        )
//        Certainly, always stand up
//        model.addConstraint(
//            symbols.zero leq (symbols.n + symbols.p),
//            "${name}_z"
//        )
        model.addConstraint(
            symbols.pos leq symbols.p,
            "${name}_p"
        )
    }

    private fun polyY(): Polynomial<Linear> {
        val poly = LinearPolynomial()
        poly += m * symbols.pos
        poly += m * symbols.neg
        return poly
    }
}
