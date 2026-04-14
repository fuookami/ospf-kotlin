@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * In-Step-Range function: `y = lb + floor((ub - lb) / step) * step`.
 *
 * Finds the largest value `y` such that:
 * - `y >= lb`
 * - `y <= ub`
 * - `y = lb + n * step` for some integer `n >= 0`
 *
 * This is useful for discretizing continuous values to step-aligned grid points.
 *
 * Decomposition:
 * - Create a FloorFunction: `q = floor((ub - lb) / step)`
 * - Result: `y = lb + q * step`
 *
 * @param lb the lower bound linear polynomial
 * @param ub the upper bound linear polynomial
 * @param step the step size (must be positive, default 1)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class InStepRangeFunction<T : Field<T>>(
    val lb: LinearPolynomial<T>,
    val ub: LinearPolynomial<T>,
    val step: Flt64 = Flt64.one,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val diff: LinearPolynomial<T> by lazy {
        LinearPolynomial(ub.monomials + lb.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }, ub.constant - lb.constant)
    }

    private val floorFunc: FloorFunction<T> by lazy {
        FloorFunction(
            x = diff,
            d = step,
            name = "${name}_q"
        )
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = floorFunc.helperVariables

    /**
     * The result: `y = lb + q * step` where `q = floor((ub - lb) / step)`.
     */
    val result: LinearPolynomial<T> by lazy {
        val qResult = floorFunc.q
        val scaledMonomials = qResult.monomials.map {
            @Suppress("UNCHECKED_CAST")
            LinearMonomial((it.coefficient.asFlt64() * step) as T, it.symbol)
        }
        @Suppress("UNCHECKED_CAST")
        val scaledConstant = (qResult.constant.asFlt64() * step) as T
        LinearPolynomial(
            scaledMonomials + lb.monomials,
            scaledConstant + lb.constant
        )
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        val lbValue = lb.evaluate(values) ?: return null
        val ubValue = ub.evaluate(values) ?: return null
        val diffDouble = (ubValue.asFlt64() - lbValue.asFlt64()).toDouble()
        val stepDouble = step.toDouble()
        val qDouble = kotlin.math.floor(diffDouble / stepDouble)
        @Suppress("UNCHECKED_CAST")
        return (lbValue.asFlt64() + Flt64(qDouble * stepDouble)) as T
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        return floorFunc.register(model)
    }

    companion object {
        operator fun invoke(
            lb: LinearPolynomial<Flt64>,
            ub: LinearPolynomial<Flt64>,
            step: Flt64 = Flt64.one,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction<Flt64> = InStepRangeFunction(
            lb = lb,
            ub = ub,
            step = step,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            lb: LinearMonomial<Flt64>,
            ub: LinearMonomial<Flt64>,
            step: Flt64 = Flt64.one,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction<Flt64> = InStepRangeFunction(
            lb = LinearPolynomial(listOf(lb), Flt64.zero),
            ub = LinearPolynomial(listOf(ub), Flt64.zero),
            step = step,
            name = name,
            displayName = displayName
        )
    }
}
