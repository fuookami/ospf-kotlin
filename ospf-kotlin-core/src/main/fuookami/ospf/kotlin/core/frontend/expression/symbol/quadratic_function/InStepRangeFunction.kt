package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.QuadraticFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.expression.symbol.toTidyRawString
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange

class InStepRange(
    private val lb: AbstractQuadraticPolynomial<*>,
    private val ub: AbstractQuadraticPolynomial<*>,
    private val step: AbstractQuadraticPolynomial<*>,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol() {
    companion object {
        operator fun <
                T1 : AbstractQuadraticPolynomial<Poly1>,
                Poly1 : AbstractQuadraticPolynomial<Poly1>,
                T2 : AbstractQuadraticPolynomial<Poly2>,
                Poly2 : AbstractQuadraticPolynomial<Poly2>
                > invoke(
            lb: T1,
            ub: T2,
            step: Int,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): InStepRange {
            return InStepRange(
                lb = lb.toQuadraticPolynomial(),
                ub = ub.toQuadraticPolynomial(),
                step = QuadraticPolynomial(step),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
                T1 : AbstractQuadraticPolynomial<Poly1>,
                Poly1 : AbstractQuadraticPolynomial<Poly1>,
                T2 : AbstractQuadraticPolynomial<Poly2>,
                Poly2 : AbstractQuadraticPolynomial<Poly2>
                > invoke(
            lb: T1,
            ub: T2,
            step: Double,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): InStepRange {
            return InStepRange(
                lb = lb.toQuadraticPolynomial(),
                ub = ub.toQuadraticPolynomial(),
                step = QuadraticPolynomial(step),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
                T1 : AbstractQuadraticPolynomial<Poly1>,
                Poly1 : AbstractQuadraticPolynomial<Poly1>,
                T2 : AbstractQuadraticPolynomial<Poly2>,
                Poly2 : AbstractQuadraticPolynomial<Poly2>,
                T3 : RealNumber<T3>
                > invoke(
            lb: T1,
            ub: T2,
            step: T3,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): InStepRange {
            return InStepRange(
                lb = lb.toQuadraticPolynomial(),
                ub = ub.toQuadraticPolynomial(),
                step = QuadraticPolynomial(step),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
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
            lb: T1,
            ub: T2,
            step: T3,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): InStepRange {
            return InStepRange(
                lb = lb.toQuadraticPolynomial(),
                ub = ub.toQuadraticPolynomial(),
                step = step.toQuadraticPolynomial(),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val stepLinear: LinearFunction by lazy {
        LinearFunction(
            polynomial = step,
            parent = parent ?: this,
            name = "${name}_step"
        )
    }

    private val q: FloorFunction by lazy {
        FloorFunction(
            x = ub - lb,
            d = step,
            parent = parent ?: this,
            name = "${name}_intDiv_$step"
        )
    }

    private val y: AbstractQuadraticPolynomial<*> by lazy {
        val y = QuadraticPolynomial(lb + q * stepLinear, "${name}_y")
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

        return prepareIfNotCached(values, tokenTable) {
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
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = stepLinear.register(tokenTable)) {
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

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        when (val result = stepLinear.register(model)) {
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
        return register(tokenTable)
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(model)
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
        val lbValue = lb.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val stepValue = step.evaluate(tokenList, zeroIfNone) ?: return null
        val qValue = q.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * stepValue
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
        val stepValue = step.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
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
        val lbValue = lb.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val stepValue = step.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * stepValue
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
        val stepValue = step.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val qValue = q.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return lbValue + qValue * stepValue
    }
}





