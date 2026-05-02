@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * In-Step-Range function: `y = lb + floor((ub - lb) / step) * step`.
 *
 * Finds the largest value y such that:
 * - y >= lb
 * - y <= ub
 * - y = lb + n * step for some integer n >= 0
 *
 * Delegates to FloorFunction for the quotient computation.
 *
 * @param lb the lower bound linear polynomial
 * @param ub the upper bound linear polynomial
 * @param step the step size (must be positive, default 1)
 * @param m Big-M bound (default 1e6)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class InStepRangeFunction<V>(
    val lb: LinearPolynomial<V>,
    val ub: LinearPolynomial<V>,
    val step: Flt64 = Flt64.one,
    val m: V = Flt64(1e6) as V,
    override var name: String = "inStepRange",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val diff: LinearPolynomial<V> by lazy {
        LinearPolynomial(ub.monomials + lb.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }, ub.constant - lb.constant)
    }

    private val floorFunc: FloorFunction<V> by lazy {
        FloorFunction(
            x = diff,
            bigM = m,
            name = "${name}_q"
        )
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = floorFunc.helperVariables

    val result: LinearPolynomial<V> by lazy {
        val qResult = floorFunc.result
        val scaledMonomials = qResult.monomials.map {
            @Suppress("UNCHECKED_CAST")
            LinearMonomial((it.coefficient.asFlt64() * step) as V, it.symbol)
        }
        @Suppress("UNCHECKED_CAST")
        val scaledConstant = (qResult.constant.asFlt64() * step) as V
        LinearPolynomial(
            scaledMonomials + lb.monomials,
            scaledConstant + lb.constant
        )
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val lbValue = lb.evaluateWith(values) ?: return null
        val ubValue = ub.evaluateWith(values) ?: return null
        val diffDouble = (ubValue.asFlt64() - lbValue.asFlt64()).toDouble()
        val stepDouble = step.toDouble()
        val qDouble = kotlin.math.floor(diffDouble / stepDouble)
        @Suppress("UNCHECKED_CAST")
        return (lbValue.asFlt64() + Flt64(qDouble * stepDouble)) as V
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        return floorFunc.register(model)
    }

    companion object {
        operator fun <V> invoke(
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            step: Flt64 = Flt64.one,
            m: V = Flt64(1e6) as V,
            name: String,
            displayName: String? = null
        ): InStepRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            InStepRangeFunction(lb = lb, ub = ub, step = step, m = m, name = name, displayName = displayName)

        operator fun invoke(
            lb: LinearPolynomial<Flt64>,
            ub: LinearPolynomial<Flt64>,
            step: Flt64 = Flt64.one,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): InStepRangeFunction<Flt64> = InStepRangeFunction(
            lb = lb,
            ub = ub,
            step = step,
            m = m,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            lb: LinearMonomial<Flt64>,
            ub: LinearMonomial<Flt64>,
            step: Flt64 = Flt64.one,
            m: Flt64 = Flt64(1e6),
            name: String,
            displayName: String? = null
        ): InStepRangeFunction<Flt64> = InStepRangeFunction(
            lb = LinearPolynomial(listOf(lb), Flt64.zero),
            ub = LinearPolynomial(listOf(ub), Flt64.zero),
            step = step,
            m = m,
            name = name,
            displayName = displayName
        )
    }
}