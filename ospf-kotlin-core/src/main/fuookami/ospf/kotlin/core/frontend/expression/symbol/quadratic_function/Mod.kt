package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class ModFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    private val d: AbstractQuadraticPolynomial<*>,
    override var name: String = "${x}_mod_${d}",
    override var displayName: String? = "$x mod $d"
) : QuadraticFunctionSymbol {
    private val logger = logger()

    private val dLinear: LinearFunction by lazy {
        LinearFunction(d, "${name}_d")
    }

    private val q: IntVar by lazy {
        IntVar("${name}_q")
    }

    private val r: URealVar by lazy {
        val r = URealVar("${name}_r")
        r.range.leq(possibleUpperBound)
        r
    }

    private val y: AbstractQuadraticPolynomial<*> by lazy {
        val y = QuadraticPolynomial(r, "${name}_y")
        y.range.set(ValueRange(Flt64.zero, possibleUpperBound).value!!)
        y
    }

    override val discrete by lazy {
        x.discrete && d.discrete
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category = Linear

    override val dependencies get() = x.dependencies + d.dependencies
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleUpperBound
        get() = max(
            if (d.upperBound!!.value.unwrap() geq Flt64.zero) {
                d.upperBound!!.value.unwrap().floor()
            } else {
                d.upperBound!!.value.unwrap().ceil().abs()
            },
            if (d.lowerBound!!.value.unwrap() geq Flt64.zero) {
                d.upperBound!!.value.unwrap().floor()
            } else {
                d.lowerBound!!.value.unwrap().ceil().abs()
            }
        )

    override fun flush(force: Boolean) {
        x.flush(force)
        d.flush(force)
        dLinear.flush(force)
        y.flush(force)
        r.range.set(ValueRange(Flt64.zero, possibleUpperBound).value!!)
        y.range.set(ValueRange(Flt64.zero, possibleUpperBound).value!!)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        x.cells
        d.cells

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                d.evaluate(tokenTable)?.let { dValue ->
                    val qValue = (xValue / dValue).let {
                        if (it geq Flt64.zero) {
                            it.floor()
                        } else {
                            it.ceil()
                        }
                    }
                    logger.trace { "Setting ModFunction ${name}.q initial solution: $qValue" }
                    tokenTable.find(q)?.let { token ->
                        token._result = qValue
                    }
                    val rValue = xValue - dValue * qValue
                    logger.trace { "Setting ModFunction ${name}.r initial solution: $rValue" }
                    tokenTable.find(r)?.let { token ->
                        token._result = rValue
                    }

                    tokenTable.cache(this, null, rValue)
                }
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(dLinear)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

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

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        when (val result = dLinear.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            x eq (dLinear * q + r),
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

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "(${x.toRawString(unfold)} mod $d)"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(tokenList, zeroIfNone)?.let { xValue ->
            d.evaluate(tokenList, zeroIfNone)?.let { dValue ->
                xValue % dValue
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(results, tokenList, zeroIfNone)?.let { xValue ->
            d.evaluate(results, tokenList, zeroIfNone)?.let { dValue ->
                xValue % dValue
            }
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(tokenTable, zeroIfNone)?.let { xValue ->
            d.evaluate(tokenTable, zeroIfNone)?.let { dValue ->
                xValue % dValue
            }
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(results, tokenTable, zeroIfNone)?.let { xValue ->
            d.evaluate(results, tokenTable, zeroIfNone)?.let { dValue ->
                xValue % dValue
            }
        }
    }
}
