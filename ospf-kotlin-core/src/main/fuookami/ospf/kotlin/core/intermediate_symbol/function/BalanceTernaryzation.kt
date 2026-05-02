@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
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
 * Balance Ternaryzation function: maps x to sign(x) in {-1, 0, 1}.
 *
 * Output:
 * - y = 1  when x > epsilon
 * - y = 0  when -epsilon <= x <= epsilon
 * - y = -1 when x < -epsilon
 *
 * Uses piecewise linear approximation via [UnivariateLinearPiecewiseFunction]
 * with breakpoints at the sign transition boundaries.
 *
 * @param x the input linear polynomial
 * @param epsilon zero threshold (default 1e-6)
 * @param extract legacy parameter kept for API compatibility; currently unused (piecewise is always used)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class BalanceTernaryzationFunction<V>(
    val x: LinearPolynomial<V>,
    val epsilon: Flt64 = Flt64(1e-6),
    val extract: Boolean = true,
    override var name: String = "bter",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val impl: UnivariateLinearPiecewiseFunction<V> by lazy {
        val xLower = Flt64(-1e6)
        val xUpper = Flt64(1e6)
        val eps = epsilon
        val precision = Flt64(1e-10)
        val breakpoints = listOf(xLower, -eps, -eps + precision, eps - precision, eps, xUpper).map { it as V }
        val slopes = listOf(
            Flt64.zero,   // segment 0: constant -1
            Flt64(1.0 / precision.toDouble()), // segment 1: rising from -1 to 0
            Flt64.zero,   // segment 2: constant 0
            Flt64(1.0 / precision.toDouble()), // segment 3: rising from 0 to 1
            Flt64.zero    // segment 4: constant 1
        ).map { it as V }
        val intercepts = listOf(
            Flt64(-1.0),
            Flt64(-1.0 - (-eps.toDouble()) / precision.toDouble()),
            Flt64.zero,
            Flt64.zero - Flt64((eps - precision).toDouble() / precision.toDouble()),
            Flt64.one
        ).map { it as V }
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
        val xValue = x.evaluateWith(values) ?: return null
        val xDouble = xValue.asFlt64().toDouble()
        val epsDouble = epsilon.toDouble()
        @Suppress("UNCHECKED_CAST")
        return when {
            xDouble > epsDouble -> Flt64.one as V
            xDouble < -epsDouble -> Flt64(-1.0) as V
            else -> Flt64.zero as V
        }
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return impl.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        return impl.registerConstraints(model)
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        return impl.register(model)
    }

    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            epsilon: Flt64 = Flt64(1e-6),
            extract: Boolean = true,
            name: String,
            displayName: String? = null
        ): BalanceTernaryzationFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BalanceTernaryzationFunction(x = x, epsilon = epsilon, extract = extract, name = name, displayName = displayName)

        operator fun invoke(
            x: LinearMonomial<Flt64>,
            epsilon: Flt64 = Flt64(1e-6),
            extract: Boolean = true,
            name: String,
            displayName: String? = null
        ): BalanceTernaryzationFunction<Flt64> = BalanceTernaryzationFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            epsilon = epsilon,
            extract = extract,
            name = name,
            displayName = displayName
        )
    }
}