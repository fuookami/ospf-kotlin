package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class AbsFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val extract: Boolean = true,
    override var name: String = "${x}_abs",
    override var displayName: String? = "|$x|"
) : LinearFunctionSymbol {
    private val logger = logger()

    private val neg: PctVar by lazy {
        PctVar("${name}_neg")
    }

    private val pos: PctVar by lazy {
        PctVar("${name}_pos")
    }

    private val p: BinVar by lazy {
        BinVar("${name}_p")
    }

    private val y: MutableLinearPolynomial by lazy {
        MutableLinearPolynomial(
            (m * pos + m * neg).toMutable(),
            "${name}_abs_y"
        )
    }

    override val discrete by lazy {
        x.discrete
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category = Linear

    override val dependencies by x::dependencies
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleUpperBound get() = max(abs(x.lowerBound!!.value.unwrap()), abs(x.upperBound!!.value.unwrap()))
    private var m = possibleUpperBound

    override fun flush(force: Boolean) {
        x.flush(force)
        y.flush(force)
        val newM = possibleUpperBound
        if (m neq newM) {
            y.range.set(ValueRange(-m, m).value!!)
            y.asMutable() *= m / newM
            m = newM
        }
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                val pValue = xValue geq Flt64.zero
                val yValue = abs(xValue)
                val posValue = if (pValue) {
                    yValue / m
                } else {
                    Flt64.zero
                }
                val negValue = if (!pValue) {
                    yValue / m
                } else {
                    Flt64.zero
                }
                logger.trace { "Setting AbsFunction ${name}.pos initial solution: $posValue" }
                tokenTable.find(pos)?.let { token -> token._result = posValue }
                logger.trace { "Setting AbsFunction ${name}.neg initial solution: $negValue" }
                tokenTable.find(neg)?.let { token -> token._result = negValue }
                logger.trace { "Setting AbsFunction ${name}.p initial solution: $pValue" }
                tokenTable.find(p)?.let { token ->
                    token._result = if (pValue) {
                        Flt64.one
                    } else {
                        Flt64.zero
                    }
                }

                yValue
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(neg)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(pos)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            when (val result = tokenTable.add(p)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        y.range.set(ValueRange(Flt64.zero, m).value!!)

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            x eq (-m * neg + m * pos),
            name
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            when (val result = model.addConstraint(
                neg + pos leq Flt64.one,
                "${name}_b"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                neg leq Flt64.one - p,
                "${name}_n"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
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
            "|${x.toTidyRawString(unfold - UInt64.zero)}|"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(tokenList, zeroIfNone)?.let { abs(it) }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(results, tokenList, zeroIfNone)?.let { abs(it) }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(tokenTable, zeroIfNone)?.let { abs(it) }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(results, tokenTable, zeroIfNone)?.let { abs(it) }
    }
}
