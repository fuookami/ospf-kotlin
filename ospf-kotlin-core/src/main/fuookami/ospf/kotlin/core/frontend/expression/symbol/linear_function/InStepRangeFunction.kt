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
    companion object {
        operator fun <
            T1 : ToLinearPolynomial<Poly1>,
            Poly1 : AbstractLinearPolynomial<Poly1>,
            T2 : ToLinearPolynomial<Poly2>,
            Poly2 : AbstractLinearPolynomial<Poly2>
        > invoke (
            lowerBound: T1,
            upperBound: T2,
            step: Flt64,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction {
            return InStepRangeFunction(
                lowerBound.toLinearPolynomial(),
                upperBound.toLinearPolynomial(),
                step,
                name,
                displayName
            )
        }
    }

    private val lb = lowerBound
    private val ub = upperBound

    private val q: FloorFunction by lazy {
        FloorFunction(
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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        lb.cells
        ub.cells
        if (values.isNullOrEmpty()) {
            q.prepareAndCache(null, tokenTable)
        } else {
            q.prepareAndCache(values, tokenTable)
        }

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            val lbValue = if (values.isNullOrEmpty()) {
                lb.evaluate(tokenTable)
            } else {
                lb.evaluate(values, tokenTable)
            } ?: return null

            val qValue = if (values.isNullOrEmpty()) {
                q.evaluate(tokenTable)
            } else {
                q.evaluate(values, tokenTable)
            } ?: return null

            lbValue + qValue * step
        } else {
            null
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

    override fun register(
        tokenTable: AbstractMutableTokenTable,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = q.register(tokenTable, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = q.register(model, fixedValues)) {
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

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(tokenList, zeroIfNone) ?: return null
        val qValue = q.evaluate(tokenList, zeroIfNone) ?: return null
        return lbValue + qValue * step
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(results, tokenList, zeroIfNone) ?: return null
        val qValue = q.evaluate(results, tokenList, zeroIfNone) ?: return null
        return lbValue + qValue * step
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(values, tokenList, zeroIfNone) ?: return null
        val qValue = q.evaluate(values, tokenList, zeroIfNone) ?: return null
        return lbValue + qValue * step
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(tokenTable, zeroIfNone) ?: return null
        val qValue = q.evaluate(tokenTable, zeroIfNone) ?: return null
        return lbValue + qValue * step
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val qValue = q.evaluate(results, tokenTable, zeroIfNone) ?: return null
        return lbValue + qValue * step
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(values, tokenTable, zeroIfNone) ?: return null
        val qValue = q.evaluate(values, tokenTable, zeroIfNone) ?: return null
        return lbValue + qValue * step
    }
}
