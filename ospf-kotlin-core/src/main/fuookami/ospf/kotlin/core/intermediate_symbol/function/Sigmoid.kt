@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.geometry.x
import fuookami.ospf.kotlin.math.geometry.y

/**
 * Sigmoid function approximated by piecewise linear interpolation.
 *
 * This is a thin wrapper around [UnivariateLinearPiecewiseFunction].
 * The sigmoid function sigma(v) = 1 / (1 + e^(-v)) is sampled at strategically
 * chosen points, then a piecewise linear approximation is used for MIP encoding.
 *
 * @param x the input linear polynomial
 * @param samplingPoints pre-computed (x, sigmoid(x)) break points
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class SigmoidFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val samplingPoints: List<Point2>,
    override var name: String = "sigmoid",
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val impl: UnivariateLinearPiecewiseFunction<T> = UnivariateLinearPiecewiseFunction(
        x = x,
        points = samplingPoints,
        name = "${name}_impl",
        displayName = displayName
    )

    /**
     * The output polynomial y = sum(y_i * lambda_i) from the piecewise approximation.
     */
    val result: LinearPolynomial<T> by lazy { impl.result }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = impl.helperVariables

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return super.registerAuxiliaryTokens(tokens)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        return impl.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        return impl.register(model)
    }

    companion object {
        enum class Precision { Full, Half }

        /** Compute sigmoid(v) = 1 / (1 + e^(-v)). */
        fun sigmoid(v: Flt64): Flt64 = Flt64(1.0) / (Flt64.one + (-v).exp())

        /** Compute inverse sigmoid: v = -ln((1-y)/y). */
        fun xForY(y: Flt64): Flt64 = -((Flt64(1.0) - y) / y).ln()!!

        /**
         * Generate standard Full precision sampling points.
         *
         * Points: (-1/eps, 0), (x(eps), eps), (-4, sigmoid(-4)), (-2, sigmoid(-2)),
         *         (x(0.2), 0.2), (0, 0.5), (x(0.8), 0.8), (2, sigmoid(2)),
         *         (4, sigmoid(4)), (x(1-eps), 1-eps), (1/eps, 1)
         */
        fun fullPoints(decimalPrecision: Flt64 = Flt64(1e-5)): List<Point2> {
            val eps = decimalPrecision
            val invEps = Flt64(1.0) / eps
            return listOf(
                // Far left tail: asymptote at y=0
                Point2(-invEps, Flt64.zero),
                // Left boundary: y = eps
                Point2(xForY(eps), eps),
                // Standard reference points
                Point2(Flt64(-4.0), sigmoid(Flt64(-4.0))),
                Point2(Flt64(-2.0), sigmoid(Flt64(-2.0))),
                // Lower transition: y = 0.2
                Point2(xForY(Flt64(0.2)), Flt64(0.2)),
                // Center: y = 0.5
                Point2(Flt64.zero, Flt64(0.5)),
                // Upper transition: y = 0.8
                Point2(xForY(Flt64(0.8)), Flt64(0.8)),
                // Standard reference points
                Point2(Flt64(2.0), sigmoid(Flt64(2.0))),
                Point2(Flt64(4.0), sigmoid(Flt64(4.0))),
                // Right boundary: y = 1 - eps
                Point2(xForY(Flt64(1.0) - eps), Flt64(1.0) - eps),
                // Far right tail: asymptote at y=1
                Point2(invEps, Flt64.one)
            )
        }

        /**
         * Generate Half precision sampling points (fewer break points, coarser approximation).
         *
         * Points: (-1/eps, 0), (-4, sigmoid(-4)), (-2, sigmoid(-2)), (0, 0.5),
         *         (2, sigmoid(2)), (4, sigmoid(4)), (1/eps, 1)
         */
        fun halfPoints(decimalPrecision: Flt64 = Flt64(1e-5)): List<Point2> {
            val eps = decimalPrecision
            val invEps = Flt64(1.0) / eps
            return listOf(
                // Far left tail: asymptote at y=0
                Point2(-invEps, Flt64.zero),
                // Standard reference points
                Point2(Flt64(-4.0), sigmoid(Flt64(-4.0))),
                Point2(Flt64(-2.0), sigmoid(Flt64(-2.0))),
                // Center: y = 0.5
                Point2(Flt64.zero, Flt64(0.5)),
                // Standard reference points
                Point2(Flt64(2.0), sigmoid(Flt64(2.0))),
                Point2(Flt64(4.0), sigmoid(Flt64(4.0))),
                // Far right tail: asymptote at y=1
                Point2(invEps, Flt64.one)
            )
        }

        /**
         * Generate sampling points for the specified precision level.
         */
        fun samplingPoints(
            precision: Precision = Precision.Full,
            decimalPrecision: Flt64 = Flt64(1e-5)
        ): List<Point2> = when (precision) {
            Precision.Full -> fullPoints(decimalPrecision)
            Precision.Half -> halfPoints(decimalPrecision)
        }

        /**
         * Factory for Flt64-typed sigmoid function using Full precision.
         */
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            precision: Precision = Precision.Full,
            decimalPrecision: Flt64 = Flt64(1e-5),
            name: String,
            displayName: String? = null
        ): SigmoidFunction<Flt64> = SigmoidFunction(
            x = x,
            samplingPoints = samplingPoints(precision, decimalPrecision),
            name = name,
            displayName = displayName
        )

        /**
         * Factory for Flt64-typed sigmoid function with custom sampling points.
         */
        operator fun invoke(
            x: LinearMonomial<Flt64>,
            precision: Precision = Precision.Full,
            decimalPrecision: Flt64 = Flt64(1e-5),
            name: String,
            displayName: String? = null
        ): SigmoidFunction<Flt64> = SigmoidFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            samplingPoints = samplingPoints(precision, decimalPrecision),
            name = name,
            displayName = displayName
        )

    }
}
