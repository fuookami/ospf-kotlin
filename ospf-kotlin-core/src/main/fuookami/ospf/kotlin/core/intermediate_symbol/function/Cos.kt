@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Cosine function approximated by piecewise linear interpolation.
 *
 * This is a thin wrapper around [UnivariateLinearPiecewiseFunction].
 * The cosine function cos(v) is sampled at strategic points for MIP encoding.
 *
 * @param x the input linear polynomial
 * @param samplingPoints pre-computed (x, cos(x)) break points
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class CosFunction<V>(
    val x: LinearPolynomial<V>,
    val samplingPoints: List<Point2>,
    override var name: String = "cos",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val impl: UnivariateLinearPiecewiseFunction<V> by lazy {
        val breakpoints = samplingPoints.map { it[0] as V }
        val slopes = mutableListOf<V>()
        val intercepts = mutableListOf<V>()
        for (i in 0 until samplingPoints.size - 1) {
            val x0 = samplingPoints[i][0].toDouble()
            val y0 = samplingPoints[i][1].toDouble()
            val x1 = samplingPoints[i + 1][0].toDouble()
            val y1 = samplingPoints[i + 1][1].toDouble()
            @Suppress("UNCHECKED_CAST")
            val slope = Flt64((y1 - y0) / (x1 - x0)) as V
            @Suppress("UNCHECKED_CAST")
            val intercept = Flt64(y0 - slope.asFlt64().toDouble() * x0) as V
            slopes.add(slope)
            intercepts.add(intercept)
        }
        UnivariateLinearPiecewiseFunction(
            x = x,
            breakpoints = breakpoints,
            slopes = slopes,
            intercepts = intercepts,
            name = "${name}_impl",
            displayName = displayName
        )
    }

    val result: LinearPolynomial<V> by lazy { impl.result }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = impl.helperVariables

    override fun evaluate(values: Map<Symbol, V>): V? {
        return impl.evaluate(values)
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        return impl.register(model)
    }

    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            samplingPoints: List<Point2> = defaultPoints(),
            name: String,
            displayName: String? = null
        ): CosFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            CosFunction(x = x, samplingPoints = samplingPoints, name = name, displayName = displayName)

        fun defaultPoints(): List<Point2> {
            val pi = Flt64(kotlin.math.PI)
            val pi2 = pi / Flt64(2.0)
            return listOf(
                Point2(-pi, Flt64(-1.0)),
                Point2(-pi2, Flt64.zero),
                Point2(Flt64.zero, Flt64.one),
                Point2(pi2, Flt64.zero),
                Point2(pi, Flt64(-1.0))
            )
        }

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            name: String,
            displayName: String? = null
        ): CosFunction<Flt64> = CosFunction(
            x = x,
            samplingPoints = defaultPoints(),
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            x: LinearMonomial<Flt64>,
            name: String,
            displayName: String? = null
        ): CosFunction<Flt64> = CosFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            samplingPoints = defaultPoints(),
            name = name,
            displayName = displayName
        )
    }
}