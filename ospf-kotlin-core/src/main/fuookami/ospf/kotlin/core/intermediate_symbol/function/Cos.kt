@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * Cosine function approximated by piecewise linear interpolation.
 *
 * This is a thin wrapper around [UnivariateLinearPiecewiseFunction].
 * The cosine function cos(v) is sampled at strategic points for MIP encoding.
 *
 * @param x the input linear polynomial
 * @param samplingPoints pre-computed (x, cos(x)) break points
 * @param converter value type converter
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class CosFunction<V>(
    val x: LinearPolynomial<V>,
    val samplingPoints: List<Point<Dim2, Flt64>>,
    private val converter: IntoValue<V>,
    override var name: String = "cos",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val impl: UnivariateLinearPiecewiseFunction<V> by lazy {
        val breakpoints = samplingPoints.map { converter.intoValue(it[0]) }
        val slopes = mutableListOf<V>()
        val intercepts = mutableListOf<V>()
        for (i in 0 until samplingPoints.size - 1) {
            val x0 = samplingPoints[i][0].toDouble()
            val y0 = samplingPoints[i][1].toDouble()
            val x1 = samplingPoints[i + 1][0].toDouble()
            val y1 = samplingPoints[i + 1][1].toDouble()
            val slopeVal = Flt64((y1 - y0) / (x1 - x0))
            val slope = converter.intoValue(slopeVal)
            val intercept = converter.intoValue(Flt64(y0 - slopeVal.toDouble() * x0))
            slopes.add(slope)
            intercepts.add(intercept)
        }
        UnivariateLinearPiecewiseFunction(
            x = x,
            breakpoints = breakpoints,
            slopes = slopes,
            intercepts = intercepts,
            converter = converter,
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

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return impl.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return impl.registerConstraints(model)
    }
    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            samplingPoints: List<Point<Dim2, Flt64>> = defaultPoints(),
            converter: IntoValue<V>,
            name: String = "cos",
            displayName: String? = null
        ): CosFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            CosFunction(x = x, samplingPoints = samplingPoints, converter = converter, name = name, displayName = displayName)

        private fun defaultPoints(): List<Point<Dim2, Flt64>> {
            val pi = Flt64(kotlin.math.PI)
            val pi2 = pi / Flt64(2.0)
            return listOf(
                Point<Dim2, Flt64>(-pi, Flt64(-1.0)),
                Point<Dim2, Flt64>(-pi2, Flt64.zero),
                Point<Dim2, Flt64>(Flt64.zero, Flt64.one),
                Point<Dim2, Flt64>(pi2, Flt64.zero),
                Point<Dim2, Flt64>(pi, Flt64(-1.0))
            )
        }

        @kotlin.Deprecated("Use the generic V-typed invoke with an IntoValue<V> converter.", level = DeprecationLevel.WARNING)
        operator fun invoke(
            x: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            name: String = "cos",
            displayName: String? = null
        ): CosFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = CosFunction(
            x = x,
            samplingPoints = defaultPoints(),
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )

        @kotlin.Deprecated("Use the generic V-typed invoke with an IntoValue<V> converter.", level = DeprecationLevel.WARNING)
        operator fun invoke(
            x: LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            name: String = "cos",
            displayName: String? = null
        ): CosFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = CosFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            samplingPoints = defaultPoints(),
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}
