package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.meta_programming.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.Function
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class AndFunction(
    /** all polys must be ∈ (R - R-) */
    val polys: List<LinearPolynomial>,
    override var name: String,
    override var displayName: String? = name
) : Function<Linear> {
    class Symbols {
        lateinit var y: BinVar
        lateinit var min: MinFunction

        val hasMin get() = ::min.isInitialized
    }

    val symbols = Symbols()

    override fun toRawString() = displayName ?: name

    init {
        symbols.y = BinVar("${name}_y")

        if (polys.all { it.range.upperBound.toFlt64() gr Flt64.one }) {
            symbols.min = MinFunction(
                polys = polys.map { it / it.range.upperBound.toFlt64() },
                exact = false,
                name = "${name}_min"
            )
        }
    }

    override val possibleRange: ValueRange<Flt64> by lazy {
        ValueRange(
            if (polys.any { it.range.lowerBound.toFlt64() eq Flt64.zero}) {
                Flt64.zero
            } else {
                Flt64.one
            },
            if (polys.all { it.range.upperBound.toFlt64() eq Flt64.zero }) {
                Flt64.zero
            } else {
                Flt64.one
            },
            Flt64
        )
    }

    override var range: ValueRange<Flt64> by lazyDelegate(AndFunction::possibleRange)

    override val cells: List<MonomialCell<Linear>> get() = listOf(LinearMonomialCell(Flt64.one, symbols.y))

    override val lowerBound: Flt64 by symbols.y::lowerBound
    override val upperBound: Flt64 by symbols.y::upperBound

    override fun intersectRange(range: ValueRange<Flt64>) =
        symbols.y.range.intersectWith(ValueRange(range.lowerBound.toFlt64().toUInt8(), range.upperBound.toFlt64().toUInt8(), UInt8))

    override fun rangeLess(value: Flt64) = symbols.y.range.ls(value.toUInt8())
    override fun rangeLessEqual(value: Flt64) = symbols.y.range.leq(value.toUInt8())
    override fun rangeGreater(value: Flt64) = symbols.y.range.gr(value.toUInt8())
    override fun rangeGreaterEqual(value: Flt64) = symbols.y.range.geq(value.toUInt8())

    override fun register(tokenTable: TokenTable<Linear>) {
        tokenTable.add(symbols.y)
    }

    override fun register(model: Model<Linear>) {
        if (symbols.hasMin) {
            // if all polynomials are not zero, y will be not zero
            model.addConstraint(
                symbols.y geq symbols.min,
                "${name}_lb"
            )
            // if any polynomial is zero, y will be zero
            model.addConstraint(
                symbols.y leq (symbols.min + Flt64.one - Flt64.epsilon),
                "${name}_ub"
            )
        } else {
            // if any polynomial is zero, y will be zero
            for (poly in polys) {
                model.addConstraint(
                    symbols.y leq poly,
                    "${name}_ub_${poly.name}"
                )
            }
            // if all polynomial are not zero, y will be not zero
            val rhs = LinearPolynomial(-Flt64((polys.size - 1).toDouble()))
            for (poly in polys) {
                rhs += poly
            }
            model.addConstraint(
                symbols.y geq rhs,
                "${name}_lb"
            )
        }
    }
}
