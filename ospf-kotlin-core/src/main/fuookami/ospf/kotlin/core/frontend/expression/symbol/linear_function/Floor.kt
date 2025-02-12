package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class Floor(
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

    override val category = Linear

    override val dependencies by x::dependencies
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange
        get() = ValueRange(
            (x.lowerBound!!.value.unwrap() / d).floor(),
            (x.upperBound!!.value.unwrap() / d).floor()
        ).value!!

    private val possibleModUpperBound
        get() = if (d geq Flt64.zero) {
            d.floor()
        } else {
            d.ceil().abs()
        }

    override fun flush(force: Boolean) {
        x.flush(force)
        y.flush(force)
        q.range.set(ValueRange(possibleRange.lowerBound.value.unwrap().toInt64(), possibleRange.upperBound.value.unwrap().toInt64()).value!!)
        r.range.set(ValueRange(Flt64.zero, possibleModUpperBound).value!!)
        y.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        x.cells

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                val qValue = (xValue / d).let {
                    if (it geq Flt64.zero) {
                        it.floor()
                    } else {
                        it.ceil()
                    }
                }
                logger.trace { "Setting FloorFunction ${name}.q initial solution: $qValue" }
                tokenTable.find(q)?.let { token -> token._result = qValue }
                val rValue = xValue - qValue * d
                logger.trace { "Setting FloorFunction ${name}.r initial solution: $rValue" }
                tokenTable.find(r)?.let { token -> token._result = rValue }

                tokenTable.cache(this, null, qValue)
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(q)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(r)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            x eq (d * q + r),
            name = name
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "⌊${x.toRawString(unfold)} / $d⌋"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(tokenList, zeroIfNone)?.let { (it / d).floor() }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(results, tokenList, zeroIfNone)?.let { (it / d).floor() }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(tokenTable, zeroIfNone)?.let { (it / d).floor() }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(results, tokenTable, zeroIfNone)?.let { (it / d).floor() }
    }
}
