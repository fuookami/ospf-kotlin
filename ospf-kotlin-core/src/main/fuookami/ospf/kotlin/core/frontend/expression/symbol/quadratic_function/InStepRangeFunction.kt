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

    companion object {
        operator fun <
            T1 : AbstractQuadraticPolynomial<Poly1>,
            Poly1 : AbstractQuadraticPolynomial<Poly1>,
            T2 : AbstractQuadraticPolynomial<Poly2>,
            Poly2 : AbstractQuadraticPolynomial<Poly2>
        > invoke(
            lowerBound: T1,
            upperBound: T2,
            step: Int,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction {
            return InStepRangeFunction(
                lowerBound.toQuadraticPolynomial(),
                upperBound.toQuadraticPolynomial(),
                QuadraticPolynomial(step),
                name,
                displayName
            )
        }

        operator fun <
            T1 : AbstractQuadraticPolynomial<Poly1>,
            Poly1 : AbstractQuadraticPolynomial<Poly1>,
            T2 : AbstractQuadraticPolynomial<Poly2>,
            Poly2 : AbstractQuadraticPolynomial<Poly2>
        > invoke(
            lowerBound: T1,
            upperBound: T2,
            step: Double,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction {
            return InStepRangeFunction(
                lowerBound.toQuadraticPolynomial(),
                upperBound.toQuadraticPolynomial(),
                QuadraticPolynomial(step),
                name,
                displayName
            )
        }

        operator fun <
            T1 : AbstractQuadraticPolynomial<Poly1>,
            Poly1 : AbstractQuadraticPolynomial<Poly1>,
            T2 : AbstractQuadraticPolynomial<Poly2>,
            Poly2 : AbstractQuadraticPolynomial<Poly2>,
            T3 : RealNumber<T3>
        > invoke(
            lowerBound: T1,
            upperBound: T2,
            step: T3,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction {
            return InStepRangeFunction(
                lowerBound.toQuadraticPolynomial(),
                upperBound.toQuadraticPolynomial(),
                QuadraticPolynomial(step),
                name,
                displayName
            )
        }

        operator fun <
            T1 : AbstractQuadraticPolynomial<Poly1>,
            Poly1 : AbstractQuadraticPolynomial<Poly1>,
            T2 : AbstractQuadraticPolynomial<Poly2>,
            Poly2 : AbstractQuadraticPolynomial<Poly2>,
            T3 : AbstractQuadraticPolynomial<Poly3>,
            Poly3 : AbstractQuadraticPolynomial<Poly3>
        > invoke(
            lowerBound: T1,
            upperBound: T2,
            step: T3,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction {
            return InStepRangeFunction(
                lowerBound.toQuadraticPolynomial(),
                upperBound.toQuadraticPolynomial(),
                step.toQuadraticPolynomial(),
                name,
                displayName
            )
        }
    }

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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        lb.cells
        ub.cells
        tokenTable.cache(
            listOf(stepLinear, q).mapNotNull {
                val value = if (values.isNullOrEmpty()) {
                    it.prepare(null, tokenTable)
                } else {
                    it.prepare(values, tokenTable)
                }
                if (value != null) {
                    (it as IntermediateSymbol) to value
                } else {
                    null
                }
            }.toMap()
        )

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

            val stepValue = if (values.isNullOrEmpty()) {
                stepLinear.evaluate(tokenTable)
            } else {
                stepLinear.evaluate(values, tokenTable)
            } ?: return null

            val qValue = if (values.isNullOrEmpty()) {
                q.evaluate(tokenTable)
            } else {
                q.evaluate(values, tokenTable)
            } ?: return null

            lbValue + qValue * stepValue
        } else {
            null
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

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "inStepRange(${lb.toTidyRawString(unfold - UInt64.one)}, ${ub.toTidyRawString(unfold - UInt64.one)}, $${step.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(tokenList, zeroIfNone) ?: return null
        val stepValue = step.evaluate(tokenList, zeroIfNone) ?: return null
        val qValue = q.evaluate(tokenList, zeroIfNone) ?: return null
        return lbValue + qValue * stepValue
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(results, tokenList, zeroIfNone) ?: return null
        val stepValue = step.evaluate(tokenList, zeroIfNone) ?: return null
        val qValue = q.evaluate(results, tokenList, zeroIfNone) ?: return null
        return lbValue + qValue * stepValue
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(values, tokenList, zeroIfNone) ?: return null
        val stepValue = step.evaluate(values, tokenList, zeroIfNone) ?: return null
        val qValue = q.evaluate(values, tokenList, zeroIfNone) ?: return null
        return lbValue + qValue * stepValue
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(tokenTable, zeroIfNone) ?: return null
        val stepValue = step.evaluate(tokenTable, zeroIfNone) ?: return null
        val qValue = q.evaluate(tokenTable, zeroIfNone) ?: return null
        return lbValue + qValue * stepValue
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val stepValue = step.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val qValue = q.evaluate(results, tokenTable, zeroIfNone) ?: return null
        return lbValue + qValue * stepValue
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(values, tokenTable, zeroIfNone) ?: return null
        val stepValue = step.evaluate(values, tokenTable, zeroIfNone) ?: return null
        val qValue = q.evaluate(values, tokenTable, zeroIfNone) ?: return null
        return lbValue + qValue * stepValue
    }
}
