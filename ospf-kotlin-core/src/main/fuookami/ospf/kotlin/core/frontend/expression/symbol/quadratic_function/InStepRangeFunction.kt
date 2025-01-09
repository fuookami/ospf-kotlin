package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

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
    lowerBound: AbstractQuadraticPolynomial<*>,
    upperBound: AbstractQuadraticPolynomial<*>,
    private val step: AbstractQuadraticPolynomial<*>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    private val lb = lowerBound
    private val ub = upperBound

    private val stepLinear: LinearFunction by lazy {
        LinearFunction(
            step,
            name = "${name}_step"
        )
    }

    private val q: FloorFunction by lazy {
        FloorFunction(
            upperBound - lowerBound,
            step,
            name = "${name}_intDiv_$step"
        )
    }

    private val y: AbstractQuadraticPolynomial<*> by lazy {
        val y = QuadraticPolynomial(lowerBound + q * stepLinear, "${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val discrete: Boolean by lazy {
        lb.discrete && ub.discrete && step.discrete
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
        stepLinear.flush(force)
        q.flush(force)
        y.flush(force)
        y.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        lb.cells
        ub.cells
        stepLinear.prepare(tokenTable)
        q.prepare(tokenTable)

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            lb.evaluate(tokenTable)?.let { lbValue ->
                step.evaluate(tokenTable)?.let { stepValue ->
                    q.evaluate(tokenTable)?.let { qValue ->
                        val yValue = lbValue + qValue * stepValue

                        tokenTable.cache(this, null, yValue)
                    }
                }
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(stepLinear)) {
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

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        when (val result = stepLinear.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

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

    override fun toRawString(unfold: Boolean): String {
        return "inStepRange(${lb.toRawString(unfold)}, ${ub.toRawString(unfold)}, $${step.toRawString(unfold)})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return lb.evaluate(tokenList, zeroIfNone)?.let { lbValue ->
            step.evaluate(tokenList, zeroIfNone)?.let { stepValue ->
                q.evaluate(tokenList, zeroIfNone)?.let { qValue ->
                    lbValue + qValue * stepValue
                }
            }
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return lb.evaluate(results, tokenList, zeroIfNone)?.let { lbValue ->
            step.evaluate(results, tokenList, zeroIfNone)?.let { stepValue ->
                q.evaluate(results, tokenList, zeroIfNone)?.let { qValue ->
                    lbValue + qValue * stepValue
                }
            }
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return lb.evaluate(tokenTable, zeroIfNone)?.let { lbValue ->
            step.evaluate(tokenTable, zeroIfNone)?.let { stepValue ->
                q.evaluate(tokenTable, zeroIfNone)?.let { qValue ->
                    lbValue + qValue * stepValue
                }
            }
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return lb.evaluate(results, tokenTable, zeroIfNone)?.let { lbValue ->
            step.evaluate(tokenTable, zeroIfNone)?.let { stepValue ->
                q.evaluate(results, tokenTable, zeroIfNone)?.let { qValue ->
                    lbValue + qValue * stepValue
                }
            }
        }
    }
}
