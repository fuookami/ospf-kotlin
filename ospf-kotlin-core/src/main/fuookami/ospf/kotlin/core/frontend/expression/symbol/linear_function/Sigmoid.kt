package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class Sigmoid(
    private val x: AbstractLinearPolynomial<*>,
    private val precision: Precision = Precision.All,
    private val decimalPrecision: Flt64 = Flt64(1e-5),
    override var name: String = "${x}_sigmoid",
    override var displayName: String? = "Sigmoid($x)"
) : LinearFunctionSymbol {
    init {
        assert(decimalPrecision geq Flt64.zero)
        assert(decimalPrecision leq Flt64(1e-2))
    }

    companion object {
        enum class Precision {
            All,
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
    }

    private val impl = UnivariateLinearPiecewiseFunction(
        x = x,
        points = when (precision) {
            Precision.All -> fullPoints(decimalPrecision)
            Precision.Half -> halfPoints(decimalPrecision)
        },
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

    override fun prepare(tokenTable: AbstractTokenTable) {
        x.cells
        impl.prepare(tokenTable)

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            impl.evaluate(tokenTable)?.let { yValue ->
                tokenTable.cache(this, null, yValue)
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = impl.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = impl.register(model)) {
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
        return "Sigmoid(${x.toRawString(unfold)})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(tokenList, zeroIfNone)
            ?: return null
        return sigmoid(value)
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(results, tokenList, zeroIfNone)
            ?: return null
        return sigmoid(value)
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(tokenTable, zeroIfNone)
            ?: return null
        return sigmoid(value)
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(results, tokenTable, zeroIfNone)
            ?: return null
        return sigmoid(value)
    }
}
