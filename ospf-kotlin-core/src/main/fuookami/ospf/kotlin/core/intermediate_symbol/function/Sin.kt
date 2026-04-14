@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Sine function approximated by piecewise linear interpolation.
 *
 * This is a thin wrapper around [UnivariateLinearPiecewiseFunction].
 * The sine function sin(v) is sampled at strategic points for MIP encoding.
 *
 * @param x the input linear polynomial
 * @param samplingPoints pre-computed (x, sin(x)) break points
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class SinFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val samplingPoints: List<Point2>,
    override var name: String = "sin",
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val impl: UnivariateLinearPiecewiseFunction<T> = UnivariateLinearPiecewiseFunction(
        x = x,
        points = samplingPoints,
        name = "${name}_impl",
        displayName = displayName
    )

    val result: LinearPolynomial<T> by lazy { impl.result }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = impl.helperVariables

    override fun evaluate(values: Map<Symbol, T>): T? {
        return impl.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        return impl.register(model)
    }

    companion object {
        /**
         * Generate standard sampling points for sin(x) over [-pi, pi].
         *
         * Points: (-pi, 0), (-pi/2, -1), (-pi/6, -0.5), (0, 0),
         *         (pi/6, 0.5), (pi/2, 1), (pi, 0)
         */
        fun defaultPoints(): List<Point2> {
            val pi = Flt64(kotlin.math.PI)
            val pi2 = pi / Flt64(2.0)
            val pi6 = pi / Flt64(6.0)
            return listOf(
                Point2(-pi, Flt64.zero),
                Point2(-pi2, Flt64(-1.0)),
                Point2(-pi6, Flt64(-0.5)),
                Point2(Flt64.zero, Flt64.zero),
                Point2(pi6, Flt64(0.5)),
                Point2(pi2, Flt64.one),
                Point2(pi, Flt64.zero)
            )
        }

        /**
         * Factory for Flt64-typed sine function.
         */
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            name: String,
            displayName: String? = null
        ): SinFunction<Flt64> = SinFunction(
            x = x,
            samplingPoints = defaultPoints(),
            name = name,
            displayName = displayName
        )

        /**
         * Factory for Flt64-typed sine function with LinearMonomial input.
         */
        operator fun invoke(
            x: LinearMonomial<Flt64>,
            name: String,
            displayName: String? = null
        ): SinFunction<Flt64> = SinFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            samplingPoints = defaultPoints(),
            name = name,
            displayName = displayName
        )
    }
}
