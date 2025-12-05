package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class SigmoidFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    private val samplingPoints: List<Point2>,
    override val parent: IntermediateSymbol? = null,
    override var name: String = "${x}_sigmoid",
    override var displayName: String? = "Sigmoid($x)"
) : QuadraticFunctionSymbol() {
    companion object {
        enum class Precision {
            Full,
            Half
        }

        fun fullPoints(decimalPrecision: Flt64): List<Point2> {
            return listOf(
                Point(-decimalPrecision.reciprocal(), Flt64(0.0)),
                Point(x(decimalPrecision), decimalPrecision),
                Point(Flt64(-4), sigmoid(Flt64(-4))),
                Point(Flt64(-2), sigmoid(Flt64(-2))),
                Point(x(Flt64(0.2)), Flt64(0.2)),
                Point(Flt64(0), Flt64(0.5)),
                Point(x(Flt64(0.8)), Flt64(0.8)),
                Point(Flt64(2), sigmoid(Flt64(2))),
                Point(Flt64(4), sigmoid(Flt64(4))),
                Point(x(Flt64.one - decimalPrecision), Flt64.one - decimalPrecision),
                Point(decimalPrecision.reciprocal(), Flt64(1.0))
            )
        }

        fun halfPoints(decimalPrecision: Flt64): List<Point2> {
            return listOf(
                Point(-decimalPrecision.reciprocal(), Flt64(0.0)),
                Point(Flt64(-4), sigmoid(Flt64(-4))),
                Point(Flt64(-2), sigmoid(Flt64(-2))),
                Point(Flt64(0), Flt64(0.5)),
                Point(Flt64(2), sigmoid(Flt64(2))),
                Point(Flt64(4), sigmoid(Flt64(4))),
                Point(decimalPrecision.reciprocal(), Flt64(1.0))
            )
        }

        fun sigmoid(x: Flt64) = Flt64(1.0) / (Flt64.one + (-x).exp())
        fun x(y: Flt64) = -((Flt64(1.0) - y) / y).ln()!!

        fun samplingPoints(
            precision: Precision = Precision.Full,
            decimalPrecision: Flt64 = Flt64(1e-5),
        ): List<Point2> {
            assert(decimalPrecision geq Flt64.zero)
            assert(decimalPrecision leq Flt64(1e-2))

            return when (precision) {
               Precision.Full -> fullPoints(
                    decimalPrecision
                )
               Precision.Half -> halfPoints(
                    decimalPrecision
                )
            }
        }

        operator fun <
            T : ToQuadraticPolynomial<Poly>,
            Poly : AbstractQuadraticPolynomial<Poly>
        > invoke(
            x: T,
            samplingPoints: List<Point2>,
            parent: IntermediateSymbol? = null,
            name: String = "${x}_sigmoid",
            displayName: String? = "Sigmoid(${x})"
        ): SigmoidFunction {
            return SigmoidFunction(
                x = x.toQuadraticPolynomial(),
                samplingPoints = samplingPoints,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
        
        operator fun <
            T : ToQuadraticPolynomial<Poly>,
            Poly : AbstractQuadraticPolynomial<Poly>
        > invoke(
            x: T,
            precision: Precision = Precision.Full,
            decimalPrecision: Flt64 = Flt64(1e-5),
            parent: IntermediateSymbol? = null,
            name: String = "${x}_sigmoid",
            displayName: String? = "Sigmoid($x)"
        ): SigmoidFunction {
            return SigmoidFunction(
                x = x.toQuadraticPolynomial(),
                samplingPoints = samplingPoints(precision, decimalPrecision),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    private val impl = UnivariateLinearPiecewiseFunction(
        x = x,
        points = samplingPoints,
        parent = parent,
        name = name,
        displayName = displayName
    )

    override val discrete = false

    override val range get() = impl.range
    override val lowerBound get() = impl.lowerBound
    override val upperBound get() = impl.upperBound

    override val category = Linear

    override val dependencies get() = x.dependencies
    override val cells get() = impl.cells
    override val cached get() = impl.cached

    override fun flush(force: Boolean) {
        impl.flush(force)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        if (values.isNullOrEmpty()) {
            impl.prepareAndCache(null, tokenTable)
        } else {
            impl.prepareAndCache(values, tokenTable)
        }

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            impl.evaluate(tokenTable)
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = impl.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        when (val result = impl.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = impl.register(tokenTable, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = impl.register(model, fixedValues)) {
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
            "sigmoid(${x.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(tokenList, zeroIfNone) ?: return null
        return sigmoid(value)
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(results, tokenList, zeroIfNone) ?: return null
        return sigmoid(value)
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(values, tokenList, zeroIfNone) ?: return null
        return sigmoid(value)
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(tokenTable, zeroIfNone) ?: return null
        return sigmoid(value)
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(results, tokenTable, zeroIfNone) ?: return null
        return sigmoid(value)
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(values, tokenTable, zeroIfNone) ?: return null
        return sigmoid(value)
    }
}
