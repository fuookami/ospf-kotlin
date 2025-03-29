package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class InStepRangeFunction(
    lowerBound: AbstractLinearPolynomial<*>,
    upperBound: AbstractLinearPolynomial<*>,
    private val step: Flt64,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    private val lb = lowerBound
    private val ub = upperBound

    private val q: Floor by lazy {
        Floor(
            upperBound - lowerBound,
            step,
            name = "${name}_intDiv_$step"
        )
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = LinearPolynomial(lowerBound + q * step, "${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val discrete: Boolean by lazy {
        lb.discrete && ub.discrete && (step.round() eq step)
    }

    override val range get() = y.range
    override val lowerBound get() = y.lowerBound
    override val upperBound get() = y.upperBound

    override val category = Linear

    override val dependencies get() = lb.dependencies + ub.dependencies
    override val cells get() = y.cells
    override val cached get() = y.cached

    private val possibleRange
        get() = ValueRange(
            lb.lowerBound!!,
            ub.upperBound!!,
            Flt64
        )

    override fun flush(force: Boolean) {
        lb.flush(force)
        ub.flush(force)
        q.flush(force)
        y.flush(force)
        y.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        lb.cells
        ub.cells
        q.prepare(tokenTable)

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            lb.evaluate(tokenTable)?.let { lbValue ->
                q.evaluate(tokenTable)?.let { qValue ->
                    val yValue = lbValue + qValue * step

                    tokenTable.cache(this, null, yValue)
                }
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

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = q.register(model)) {
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
            "inStepRange(${lb.toTidyRawString(unfold - UInt64.one)}, ${ub.toTidyRawString(unfold - UInt64.one)}, $step)"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return lb.evaluate(tokenList, zeroIfNone)?.let { lbValue ->
            q.evaluate(tokenList, zeroIfNone)?.let { qValue ->
                lbValue + qValue * step
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return lb.evaluate(results, tokenList, zeroIfNone)?.let { lbValue ->
            q.evaluate(results, tokenList, zeroIfNone)?.let { qValue ->
                lbValue + qValue * step
            }
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return lb.evaluate(tokenTable, zeroIfNone)?.let { lbValue ->
            q.evaluate(tokenTable, zeroIfNone)?.let { qValue ->
                lbValue + qValue * step
            }
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return lb.evaluate(results, tokenTable, zeroIfNone)?.let { lbValue ->
            q.evaluate(results, tokenTable, zeroIfNone)?.let { qValue ->
                lbValue + qValue * step
            }
        }
    }
}
