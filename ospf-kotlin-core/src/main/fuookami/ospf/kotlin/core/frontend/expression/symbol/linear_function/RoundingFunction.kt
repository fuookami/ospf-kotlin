package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class RoundingFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val d: Flt64,
    override var name: String = "floor_${x}_${d}",
    override var displayName: String? = "⌊$x/$d⌋"
) : LinearFunctionSymbol {
    private val logger = logger()

    private val q: IntVar by lazy {
        val q = IntVar("${name}_q")
        q.range.set(
            ValueRange(
                possibleRange.lowerBound.value.unwrap().toInt64(),
                possibleRange.upperBound.value.unwrap().toInt64()
            ).value!!
        )
        q
    }

    private val r: URealVar by lazy {
        val r = URealVar("${name}_r")
        r.range.leq(possibleModUpperBound)
        r
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = LinearPolynomial(q, "${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val discrete: Boolean by lazy {
        x.discrete && (d.round() eq d)
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category: Category = Linear

    override val dependencies by x::dependencies
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange
        get() = ValueRange(
            (x.lowerBound!!.value.unwrap() / d).round(),
            (x.upperBound!!.value.unwrap() / d).round()
        ).value!!

    private val possibleModUpperBound
        get() = if (d geq Flt64.zero) {
            d.floor()
        } else {
            d.ceil().abs()
        }
}
