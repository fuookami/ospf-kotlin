package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.ToLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.expression.symbol.toTidyRawString
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange

class InStepRange(
    private val lb: AbstractLinearPolynomial<*>,
    private val ub: AbstractLinearPolynomial<*>,
    private val step: Flt64,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
    companion object {
        operator fun <
                T1 : ToLinearPolynomial<Poly1>,
                Poly1 : AbstractLinearPolynomial<Poly1>,
                T2 : ToLinearPolynomial<Poly2>,
                Poly2 : AbstractLinearPolynomial<Poly2>
                > invoke(
            lb: T1,
            ub: T2,
            step: Flt64,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): InStepRange {
            return InStepRange(
                lb = lb.toLinearPolynomial(),
                ub = ub.toLinearPolynomial(),
                step = step,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val q: FloorFunction by lazy {
        FloorFunction(
            x = ub - lb,
            d = step,
            parent = parent ?: this,
            name = "${name}_intDiv_$step"
        )
    }

    private val y: AbstractLinearPolynomial<*> by lazy {
        val y = LinearPolynomial(lb + q * step, "${name}_y")
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
        q.prepareAndCache(values, tokenTable)

        return prepareIfNotCached(values, tokenTable) {
            val lbValue = if (values.isNullOrEmpty()) {
                lb.evaluate(tokenTable)
            } else {
                lb.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            val qValue = if (values.isNullOrEmpty()) {
                q.evaluate(tokenTable)
            } else {
                q.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            lbValue + qValue * step
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = q.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
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

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = q.register(tokenTable, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
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

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
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
        val lbValue = lb.evaluate(
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * step
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * step
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * step
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * step
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * step
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val lbValue = lb.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * step
    }
}





