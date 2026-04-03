package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.ToQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.QuadraticFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.expression.symbol.toTidyRawString
import fuookami.ospf.kotlin.core.frontend.inequality.eq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.frontend.variable.RealVar
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import org.apache.logging.log4j.kotlin.logger

class LinearFunction(
    val polynomial: AbstractQuadraticPolynomial<*>,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            polynomial: T,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): LinearFunction {
            return LinearFunction(
                polynomial = polynomial.toQuadraticPolynomial(),
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    private val y: RealVar by lazy {
        RealVar("${name}_y")
    }

    private val polyY: AbstractQuadraticPolynomial<*> by lazy {
        if (polynomial.category == Linear) {
            polynomial.copy()
        } else {
            val polyY = QuadraticPolynomial(y)
            polyY.range.set(polynomial.range.valueRange!!)
            polyY
        }
    }

    override val discrete = polynomial.discrete

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol> get() = polynomial.dependencies
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    override fun flush(force: Boolean) {
        polynomial.flush(force)
        polyY.flush(force)
        y.range.set(polynomial.range.valueRange!!)
        polyY.range.set(polynomial.range.valueRange!!)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        polynomial.cells

        return prepareIfNotCached(values, tokenTable) {
            val yValue = if (values.isNullOrEmpty()) {
                polynomial.evaluate(tokenTable)
            } else {
                polynomial.evaluate(values, tokenTable)
            }

            if (polynomial.category != Linear) {
                logger.trace { "Setting LinearFunction ${name}.y initial solution: $yValue" }
                tokenTable.find(y)?.let { token ->
                    token._result = yValue
                }
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        if (polynomial.category != Linear) {
            when (val result = tokenTable.add(y)) {
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
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        if (polynomial.category != Linear) {
            when (val result = model.addConstraint(
                constraint = y eq polynomial,
                name = name,
                from = parent ?: this
            )) {
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
        val xValue = polynomial.evaluate(fixedValues, model.tokens) ?: return register(model)

        if (polynomial.category != Linear) {
            when (val result = model.addConstraint(
                constraint = y eq polynomial,
                name = name,
                from = parent ?: this
            )) {
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

            when (val result = model.addConstraint(
                constraint = y eq xValue,
                name = name,
                from = parent ?: this
            )) {
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

            model.tokens.find(y)?.let { token ->
                token._result = xValue
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
            "linear(${polynomial.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomial.evaluate(tokenList, zeroIfNone)
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomial.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomial.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomial.evaluate(tokenTable, zeroIfNone)
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomial.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        )
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return polynomial.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        )
    }
}




