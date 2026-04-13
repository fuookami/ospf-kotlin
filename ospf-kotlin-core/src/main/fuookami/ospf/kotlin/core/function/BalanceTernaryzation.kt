@file:Suppress("unused")

package fuookami.ospf.kotlin.core.function

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
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
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class BalanceTernaryzationFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val epsilon: Flt64 = Flt64(1e-6),
    override var name: String = "bter",
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val impl: UnivariateLinearPiecewiseFunction<T> by lazy {
        val xLower = x.lowerBound?.asFlt64() ?: Flt64(-1e6)
        val xUpper = x.upperBound?.asFlt64() ?: Flt64(1e6)
        val eps = epsilon
        val precision = Flt64(1e-10)
        UnivariateLinearPiecewiseFunction(
            x = x,
            points = listOf(
                Point2(xLower, Flt64(-1.0)),
                Point2(-eps, Flt64(-1.0)),
                Point2(-eps + precision, Flt64.zero),
                Point2(eps - precision, Flt64.zero),
                Point2(eps, Flt64.one),
                Point2(xUpper, Flt64.one)
            ),
            name = "${name}_impl",
            displayName = displayName
        )
    }

    /**
     * The ternary result: y in {-1, 0, 1}.
     */
    val result: LinearPolynomial<T> by lazy { impl.result }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = impl.helperVariables

    override fun evaluate(values: Map<Symbol, T>): T? {
        val xValue = x.evaluate(values) ?: return null
        val xDouble = xValue.asFlt64().toDouble()
        val epsDouble = epsilon.toDouble()
        @Suppress("UNCHECKED_CAST")
        return when {
            xDouble > epsDouble -> Flt64.one as T
            xDouble < -epsDouble -> Flt64(-1.0) as T
            else -> Flt64.zero as T
        }
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        return impl.register(model)
    }

    companion object {
        /**
         * Factory for Flt64-typed balance ternaryzation function.
         */
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): BalanceTernaryzationFunction<Flt64> = BalanceTernaryzationFunction(
            x = x,
            epsilon = epsilon,
            name = name,
            displayName = displayName
        )

        /**
         * Factory for Flt64-typed balance ternaryzation function with LinearMonomial input.
         */
        operator fun invoke(
            x: LinearMonomial<Flt64>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): BalanceTernaryzationFunction<Flt64> = BalanceTernaryzationFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            epsilon = epsilon,
            name = name,
            displayName = displayName
        )
    }
}
